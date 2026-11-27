package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Grade;
import liarauction.common.Phase;
import org.junit.jupiter.api.Test;

class RoundPhaseTimerTest {

    @Test
    void advancesAllRoundsAndFinishesTheGame() throws Exception {
        GameState game = newGame();
        RecordingListener listener = new RecordingListener();
        AuctionTiming timing = timing(Duration.ofMillis(15));

        try (RoundPhaseTimer timer = new RoundPhaseTimer(game, timing, listener)) {
            timer.start();

            assertTrue(listener.gameFinished.await(3, TimeUnit.SECONDS));
            assertEquals(Phase.GAME_OVER, game.phase());
            assertEquals(GameConfig.BASE_ROUNDS, listener.rounds.size());
            assertEquals(
                    java.util.stream.IntStream.rangeClosed(1, GameConfig.BASE_ROUNDS).boxed().toList(),
                    listener.rounds);
            assertEquals(GameConfig.BASE_ROUNDS, listener.auctionsClosed.size());
            for (int round = 1; round <= GameConfig.BASE_ROUNDS; round++) {
                assertEquals(List.of(
                                Phase.REVEAL,
                                Phase.RUMOR,
                                Phase.NEGOTIATION,
                                Phase.AUCTION),
                        listener.phasesByRound.get(round - 1));
            }
        }
    }

    @Test
    void allowsAuctionToCloseBeforeItsDeadline() throws Exception {
        GameState game = newGame();
        RecordingListener listener = new RecordingListener();
        AuctionTiming timing = timing(
                Duration.ofMillis(20), Duration.ofSeconds(2), Duration.ofSeconds(2));

        try (RoundPhaseTimer timer = new RoundPhaseTimer(game, timing, listener)) {
            timer.start();
            assertTrue(listener.auctionStarted.await(2, TimeUnit.SECONDS));

            assertTrue(timer.closeAuction());
            assertFalse(timer.closeAuction());
            assertEquals(Phase.AUCTION, game.phase());
            assertEquals(1, listener.auctionsClosed.size());
            Thread.sleep(100);
            assertEquals(1, game.round(), "결과 표시 시간이 끝나기 전에는 다음 라운드로 넘어가지 않음");
        }
    }

    private static AuctionTiming timing(Duration phaseDuration) {
        return timing(phaseDuration, phaseDuration);
    }

    private static AuctionTiming timing(Duration shortPhase, Duration auction) {
        return timing(shortPhase, auction, shortPhase);
    }

    private static AuctionTiming timing(Duration shortPhase, Duration auction, Duration result) {
        return new AuctionTiming(
                shortPhase, shortPhase, shortPhase, auction, result, Duration.ofMillis(5));
    }

    private static GameState newGame() {
        GameState game = new GameState();
        game.addPlayer("alice");
        game.addPlayer("bob");
        List<Artwork> deck = new ArrayList<>();
        for (int i = 0; i < GameConfig.BASE_ROUNDS; i++) {
            deck.add(new Artwork(i + 1, "artwork-" + i, "artist", Grade.FAKE, 0));
        }
        game.setDeck(deck);
        return game;
    }

    private static final class RecordingListener implements RoundPhaseTimer.Listener {
        private final CountDownLatch auctionStarted = new CountDownLatch(1);
        private final CountDownLatch gameFinished = new CountDownLatch(1);
        private final List<Integer> rounds = new ArrayList<>();
        private final List<List<Phase>> phasesByRound = new ArrayList<>();
        private final List<Artwork> auctionsClosed = new ArrayList<>();

        private RecordingListener() {
        }

        @Override
        public synchronized void onRoundStarted(int round, String title, String artist) {
            rounds.add(round);
            phasesByRound.add(new ArrayList<>());
        }

        @Override
        public synchronized void onPhaseChanged(Phase phase, int secondsRemaining) {
            if (!phasesByRound.isEmpty()) {
                phasesByRound.get(phasesByRound.size() - 1).add(phase);
            }
            if (phase == Phase.AUCTION) {
                auctionStarted.countDown();
            }
        }

        @Override
        public void onTimeRemaining(int secondsRemaining) {
        }

        @Override
        public synchronized void onAuctionClosed(Artwork artwork) {
            auctionsClosed.add(artwork);
        }

        @Override
        public void onGameFinished() {
            gameFinished.countDown();
        }
    }
}
