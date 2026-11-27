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

class AuctionBidManagerTest {

    @Test
    void acceptsValidBidAndSettlesWinnerAfterIdleTimeout() throws Exception {
        GameState game = gameInAuction();
        RecordingListener listener = new RecordingListener();

        try (AuctionBidManager manager =
                new AuctionBidManager(game, Duration.ofMillis(100), listener)) {
            manager.openAuction(game.currentArtwork(), 100);

            assertTrue(manager.placeBid("alice", 50));
            assertEquals("alice", listener.highestBidder);
            assertEquals(150, listener.currentPrice);
            assertFalse(manager.placeBid("bob", 25));
            assertEquals("INVALID_INCREMENT", listener.lastRejection);

            assertTrue(listener.auctionEnded.await(2, TimeUnit.SECONDS));
            assertEquals("alice", listener.winner);
            assertEquals(150, listener.finalPrice);
            assertEquals(GameConfig.START_CASH - 150, game.findPlayer("alice").cash());
            assertEquals(List.of(game.currentArtwork()), game.findPlayer("alice").artworks());
        }
    }

    @Test
    void rejectsBidsThatExceedAvailableCash() {
        GameState game = gameInAuction();
        RecordingListener listener = new RecordingListener();

        try (AuctionBidManager manager =
                new AuctionBidManager(game, Duration.ofSeconds(1), listener)) {
            manager.openAuction(game.currentArtwork(), 950);

            assertFalse(manager.placeBid("alice", 100));
            assertEquals("INSUFFICIENT_CASH", listener.lastRejection);
            assertEquals(GameConfig.START_CASH, game.findPlayer("alice").cash());
        }
    }

    @Test
    void closesUnbidAuctionWithoutChargingPlayers() throws Exception {
        GameState game = gameInAuction();
        RecordingListener listener = new RecordingListener();

        try (AuctionBidManager manager =
                new AuctionBidManager(game, Duration.ofSeconds(1), listener)) {
            manager.openAuction(game.currentArtwork(), 100);

            assertTrue(manager.closeAuction());
            assertFalse(manager.closeAuction());
            assertTrue(listener.auctionEnded.await(1, TimeUnit.SECONDS));
            assertEquals("NONE", listener.winner);
            assertEquals(0, listener.finalPrice);
            assertEquals(GameConfig.START_CASH, game.findPlayer("alice").cash());
            assertTrue(game.findPlayer("alice").artworks().isEmpty());
        }
    }

    private static GameState gameInAuction() {
        GameState game = new GameState();
        game.addPlayer("alice");
        game.addPlayer("bob");
        List<Artwork> deck = new ArrayList<>();
        for (int i = 0; i < GameConfig.BASE_ROUNDS; i++) {
            deck.add(new Artwork(i + 1, "artwork-" + i, "artist", Grade.FAKE, 0));
        }
        game.setDeck(deck);
        game.startNextRound();
        game.setPhase(Phase.AUCTION);
        return game;
    }

    private static final class RecordingListener implements AuctionBidManager.Listener {
        private final CountDownLatch auctionEnded = new CountDownLatch(1);
        private String highestBidder;
        private int currentPrice;
        private String winner;
        private int finalPrice;
        private String lastRejection;

        @Override
        public void onAuctionStarted(String title, String artist, int startingPrice) {
        }

        @Override
        public void onBidUpdated(String highestBidder, int currentPrice) {
            this.highestBidder = highestBidder;
            this.currentPrice = currentPrice;
        }

        @Override
        public void onBidRejected(String nickname, String reason) {
            lastRejection = reason;
        }

        @Override
        public void onAuctionEnded(String winner, int price) {
            this.winner = winner;
            finalPrice = price;
            auctionEnded.countDown();
        }

        @Override
        public void onCashChanged(String nickname, int cash) {
        }

        @Override
        public void onOpponentLeft(String nickname) {
        }

        @Override
        public void onGameFinished() {
        }
    }
}
