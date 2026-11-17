package liarauction.server;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Phase;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;

/** Validates live bids and settles the auction winner on the server. */
public final class AuctionBidManager implements AutoCloseable {

    private final GameState game;
    private final Duration idleTimeout;
    private final Listener listener;
    private final ScheduledExecutorService scheduler;

    private ScheduledFuture<?> idleTask;
    private Artwork artwork;
    private PlayerState highestBidder;
    private int currentPrice;
    private long idleDeadlineNanos;
    private boolean active;
    private boolean closed;

    public AuctionBidManager(GameState game, Duration idleTimeout, Listener listener) {
        this.game = Objects.requireNonNull(game, "game");
        this.idleTimeout = Objects.requireNonNull(idleTimeout, "idleTimeout");
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("입찰 대기 시간은 양수여야 합니다");
        }
        this.listener = Objects.requireNonNull(listener, "listener");
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "liar-auction-bid-timer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public AuctionBidManager(GameState game, Listener listener) {
        this(game, Duration.ofSeconds(GameConfig.BID_IDLE_SECONDS), listener);
    }

    public synchronized void openAuction(Artwork artwork, int startingPrice) {
        ensureOpen();
        Objects.requireNonNull(artwork, "artwork");
        if (active) {
            throw new IllegalStateException("이미 진행 중인 경매가 있습니다");
        }
        if (game.phase() != Phase.AUCTION || !artwork.equals(game.currentArtwork())) {
            throw new IllegalStateException("현재 라운드의 경매 단계에서만 시작할 수 있습니다");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("시작가는 양수여야 합니다");
        }

        this.artwork = artwork;
        currentPrice = startingPrice;
        highestBidder = null;
        active = true;
        listener.onAuctionStarted(artwork.title(), artwork.artist(), startingPrice);
    }

    /** @return true if the bid was accepted */
    public synchronized boolean placeBid(String nickname, int increment) {
        Objects.requireNonNull(nickname, "nickname");
        ensureOpen();
        if (!active || game.phase() != Phase.AUCTION) {
            return reject(nickname, "INVALID_PHASE");
        }
        if (System.nanoTime() >= idleDeadlineNanos && idleTask != null) {
            finishAuction();
            return reject(nickname, "AUCTION_CLOSED");
        }

        PlayerState bidder = game.findPlayer(nickname);
        if (bidder == null) {
            return reject(nickname, "UNKNOWN_PLAYER");
        }
        if (!GameConfig.isValidBidIncrement(increment)) {
            return reject(nickname, "INVALID_INCREMENT");
        }
        if (currentPrice > Integer.MAX_VALUE - increment) {
            return reject(nickname, "PRICE_OVERFLOW");
        }

        int nextPrice = currentPrice + increment;
        if (!bidder.canAfford(nextPrice)) {
            return reject(nickname, "INSUFFICIENT_CASH");
        }

        highestBidder = bidder;
        currentPrice = nextPrice;
        armIdleTimeout();
        listener.onBidUpdated(bidder.nickname(), currentPrice);
        return true;
    }

    /** Closes the current auction, for example when its maximum phase time expires. */
    public synchronized boolean closeAuction() {
        ensureOpen();
        if (!active) {
            return false;
        }
        finishAuction();
        return true;
    }

    public synchronized void playerDisconnected(String nickname) {
        Objects.requireNonNull(nickname, "nickname");
        ensureOpen();
        if (game.findPlayer(nickname) == null) {
            throw new IllegalArgumentException("게임에 없는 플레이어: " + nickname);
        }

        game.removePlayer(nickname);
        active = false;
        cancelIdleTimeout();
        game.finish();
        listener.onOpponentLeft(nickname);
        listener.onGameFinished();
    }

    private boolean reject(String nickname, String reason) {
        listener.onBidRejected(nickname, reason);
        return false;
    }

    private void armIdleTimeout() {
        cancelIdleTimeout();
        idleDeadlineNanos = System.nanoTime() + idleTimeout.toNanos();
        idleTask = scheduler.schedule(this::idleTimeoutReached,
                idleTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    private synchronized void idleTimeoutReached() {
        if (closed || !active) {
            return;
        }
        long remainingNanos = idleDeadlineNanos - System.nanoTime();
        if (remainingNanos > 0) {
            idleTask = scheduler.schedule(this::idleTimeoutReached,
                    remainingNanos, TimeUnit.NANOSECONDS);
            return;
        }
        finishAuction();
    }

    private void finishAuction() {
        if (!active) {
            return;
        }
        active = false;
        cancelIdleTimeout();

        String winner = Protocol.NONE;
        int finalPrice = 0;
        if (highestBidder != null) {
            highestBidder.pay(currentPrice);
            highestBidder.addArtwork(artwork);
            winner = highestBidder.nickname();
            finalPrice = currentPrice;
            listener.onCashChanged(winner, highestBidder.cash());
        }
        listener.onAuctionEnded(winner, finalPrice);
    }

    private void cancelIdleTimeout() {
        if (idleTask != null) {
            idleTask.cancel(false);
            idleTask = null;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("입찰 관리자가 종료되었습니다");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cancelIdleTimeout();
            scheduler.shutdownNow();
        }
    }

    public interface Listener {
        void onAuctionStarted(String title, String artist, int startingPrice);

        void onBidUpdated(String highestBidder, int currentPrice);

        void onBidRejected(String nickname, String reason);

        void onAuctionEnded(String winner, int price);

        void onCashChanged(String nickname, int cash);

        void onOpponentLeft(String nickname);

        void onGameFinished();
    }
}
