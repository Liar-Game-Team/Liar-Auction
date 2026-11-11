package liarauction.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommonClassesTest {

    @Test
    void gradeCountsMatchRounds() {
        int sum = 0;
        for (Grade g : Grade.values()) {
            sum += g.count();
        }
        assertEquals(GameConfig.TOTAL_ROUNDS, sum);
        assertFalse(Grade.FAKE.isGenuine());
        assertTrue(Grade.MASTERPIECE.isGenuine());
    }

    @Test
    void artworkValueMustFitGrade() {
        assertThrows(IllegalArgumentException.class, () -> new Artwork(1, "t", "a", Grade.FAKE, 100));
        assertThrows(IllegalArgumentException.class, () -> new Artwork(1, "t", "a", Grade.MASTERPIECE, 399));
        Artwork a = new Artwork(1, "모나리자", "다빈치", Grade.MASTERPIECE, 500);
        assertFalse(a.toString().contains("500"), "toString에 실제 가치가 나오면 안 됨");
    }

    @Test
    void playerCash() {
        PlayerState p = new PlayerState("철수");
        assertEquals(GameConfig.START_CASH, p.cash());
        assertTrue(p.canAfford(1000));
        assertFalse(p.canAfford(1001));

        p.pay(300);
        p.earn(50);
        assertEquals(750, p.cash());
        assertThrows(IllegalStateException.class, () -> p.pay(751));
        assertThrows(IllegalArgumentException.class, () -> p.pay(-1));
        assertEquals(750, p.cash());
    }

    @Test
    void playerArtworksAndAssets() {
        PlayerState p = new PlayerState("영희", 100);
        Artwork real = new Artwork(1, "a", "x", Grade.NORMAL, 200);
        Artwork fake = new Artwork(2, "b", "y", Grade.FAKE, 0);
        p.addArtwork(real);
        p.addArtwork(fake);
        assertEquals(300, p.totalAssets());

        assertTrue(p.removeArtwork(fake));
        assertFalse(p.removeArtwork(fake));
        assertEquals(List.of(real), p.artworks());
    }

    @Test
    void phaseOrder() {
        List<Phase> order = new ArrayList<>();
        for (Phase p = Phase.REVEAL; p != null; p = p.nextInRound()) {
            order.add(p);
        }
        assertEquals(List.of(Phase.REVEAL, Phase.INSPECTION, Phase.NEGOTIATION, Phase.AUCTION, Phase.RESALE), order);
        assertEquals(30, Phase.NEGOTIATION.seconds());
        assertEquals(20, Phase.AUCTION.seconds());
    }

    @Test
    void gameStatePlayers() {
        GameState g = new GameState();
        g.addPlayer("철수");
        assertThrows(IllegalStateException.class, () -> g.addPlayer("철수"));
        g.addPlayer("영희");
        assertTrue(g.isFull());
        assertThrows(IllegalStateException.class, () -> g.addPlayer("민수"));

        assertEquals("영희", g.opponentOf("철수").nickname());
        g.removePlayer("영희");
        assertNull(g.opponentOf("철수"));
    }

    @Test
    void gameStateRounds() {
        GameState g = new GameState();
        assertEquals(Phase.WAITING, g.phase());
        assertThrows(IllegalStateException.class, g::startNextRound);
        assertThrows(IllegalArgumentException.class, () -> g.setDeck(List.of()));

        List<Artwork> deck = new ArrayList<>();
        for (int i = 0; i < GameConfig.TOTAL_ROUNDS; i++) {
            deck.add(new Artwork(i, "t" + i, "a", Grade.FAKE, 0));
        }
        g.setDeck(deck);

        for (int i = 0; i < GameConfig.TOTAL_ROUNDS; i++) {
            Artwork a = g.startNextRound();
            assertSame(deck.get(i), a);
            assertSame(a, g.currentArtwork());
            assertEquals(i + 1, g.round());
            assertEquals(Phase.REVEAL, g.phase());
        }
        assertFalse(g.hasNextRound());
        assertThrows(IllegalStateException.class, g::startNextRound);

        g.finish();
        assertTrue(g.isOver());
    }
}
