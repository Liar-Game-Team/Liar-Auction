package liarauction.server;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Phase;

/** Advances a game through its round phases using a server-side timer. */
public final class RoundPhaseTimer implements AutoCloseable {

    private final GameState game;
    private final AuctionTiming timing;
    private final Listener listener;
    private final ScheduledExecutorService scheduler;

    private ScheduledFuture<?> phaseTimeout;
    private ScheduledFuture<?> countdown;
    private ScheduledFuture<?> resultTimeout;
    private long generation;
    private long deadlineNanos;
    private int reportedSeconds;
    private boolean started;
    private boolean resultPending;
    private volatile boolean closed;

    public RoundPhaseTimer(GameState game, AuctionTiming timing, Listener listener) {
        this.game = Objects.requireNonNull(game, "game");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.listener = Objects.requireNonNull(listener, "listener");
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "liar-round-timer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public RoundPhaseTimer(GameState game, Listener listener) {
        this(game, AuctionTiming.defaults(), listener);
    }

    public synchronized void start() {
        ensureOpen();
        if (started) {
            throw new IllegalStateException("라운드 타이머가 이미 시작되었습니다");
        }
        if (game.phase() != Phase.WAITING || game.players().size() != GameConfig.MAX_PLAYERS) {
            throw new IllegalStateException("두 플레이어가 대기 중일 때만 게임을 시작할 수 있습니다");
        }
        if (game.deck().size() != GameConfig.TOTAL_ROUNDS) {
            throw new IllegalStateException("게임을 시작하려면 작품 덱이 필요합니다");
        }

        started = true;
        startNextRound();
    }

    /** Ends the auction early, for example after the last bid has been idle long enough. */
    public synchronized boolean closeAuction() {
        ensureOpen();
        if (!started || resultPending || game.phase() != Phase.AUCTION) {
            return false;
        }
        finishAuctionPhase();
        return true;
    }

    /** Requests an early close without blocking a caller that may hold another game lock. */
    public void requestAuctionClose() {
        if (closed) {
            return;
        }
        try {
            scheduler.execute(() -> {
                synchronized (RoundPhaseTimer.this) {
                    if (!closed && started && !resultPending && game.phase() == Phase.AUCTION) {
                        finishAuctionPhase();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            if (!closed) {
                throw e;
            }
        }
    }

    private void startNextRound() {
        Artwork artwork = game.startNextRound();
        listener.onRoundStarted(game.round(), artwork.title(), artwork.artist());
        enterPhase(Phase.REVEAL);
    }

    private void enterPhase(Phase phase) {
        game.setPhase(phase);
        armTimer(timing.durationFor(phase));
        listener.onPhaseChanged(phase, reportedSeconds);
    }

    private void armTimer(Duration duration) {
        cancelTimers();
        long currentGeneration = ++generation;
        long durationNanos = duration.toNanos();
        deadlineNanos = System.nanoTime() + durationNanos;
        reportedSeconds = secondsRemaining(deadlineNanos);
        phaseTimeout = scheduler.schedule(
                () -> phaseTimedOut(currentGeneration), durationNanos, TimeUnit.NANOSECONDS);
        countdown = scheduler.scheduleAtFixedRate(
                () -> reportTimeRemaining(currentGeneration), 1, 1, TimeUnit.SECONDS);
    }

    private synchronized void reportTimeRemaining(long currentGeneration) {
        if (closed || currentGeneration != generation) {
            return;
        }
        int remaining = secondsRemaining(deadlineNanos);
        if (remaining != reportedSeconds) {
            reportedSeconds = remaining;
            listener.onTimeRemaining(remaining);
        }
    }

    private synchronized void phaseTimedOut(long currentGeneration) {
        if (closed || currentGeneration != generation) {
            return;
        }
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos > 0) {
            phaseTimeout = scheduler.schedule(
                    () -> phaseTimedOut(currentGeneration), remainingNanos, TimeUnit.NANOSECONDS);
            return;
        }

        switch (game.phase()) {
            case REVEAL -> enterPhase(Phase.RUMOR);
            case RUMOR -> enterPhase(Phase.NEGOTIATION);
            case NEGOTIATION -> enterPhase(Phase.AUCTION);
            case AUCTION -> finishAuctionPhase();
            default -> throw new IllegalStateException("예상하지 못한 단계: " + game.phase());
        }
    }

    private void finishAuctionPhase() {
        if (resultPending || game.phase() != Phase.AUCTION) {
            return;
        }
        cancelTimers();
        resultPending = true;
        listener.onAuctionClosed(game.currentArtwork());
        resultTimeout = scheduler.schedule(this::finishResultPhase,
                timing.result().toNanos(), TimeUnit.NANOSECONDS);
    }

    private synchronized void finishResultPhase() {
        if (closed || !resultPending) {
            return;
        }
        resultPending = false;
        resultTimeout = null;
        if (game.hasNextRound()) {
            startNextRound();
        } else {
            game.finish();
            generation++;
            cancelTimers();
            listener.onGameFinished();
        }
    }

    private void cancelTimers() {
        if (phaseTimeout != null) {
            phaseTimeout.cancel(false);
            phaseTimeout = null;
        }
        if (countdown != null) {
            countdown.cancel(false);
            countdown = null;
        }
        if (resultTimeout != null) {
            resultTimeout.cancel(false);
            resultTimeout = null;
        }
    }

    private static int secondsRemaining(long deadline) {
        long remainingNanos = Math.max(0, deadline - System.nanoTime());
        long secondNanos = TimeUnit.SECONDS.toNanos(1);
        return (int) Math.min(Integer.MAX_VALUE, (remainingNanos + secondNanos - 1) / secondNanos);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("라운드 타이머가 종료되었습니다");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            generation++;
            cancelTimers();
            scheduler.shutdownNow();
        }
    }

    public interface Listener {
        void onRoundStarted(int round, String title, String artist);

        void onPhaseChanged(Phase phase, int secondsRemaining);

        void onTimeRemaining(int secondsRemaining);

        /** Called after the auction closes so the result can be settled and displayed. */
        void onAuctionClosed(Artwork artwork);

        void onGameFinished();
    }
}
