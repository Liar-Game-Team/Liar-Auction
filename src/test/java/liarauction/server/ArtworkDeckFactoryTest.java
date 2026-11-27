package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.Grade;
import org.junit.jupiter.api.Test;

class ArtworkDeckFactoryTest {

    @Test
    void createsDeckWithConfiguredGradeCountsAndValidValues() {
        List<Artwork> deck = ArtworkDeckFactory.createDeck();
        Map<Grade, Integer> counts = new EnumMap<>(Grade.class);

        assertEquals(GameConfig.BASE_ROUNDS, deck.size());
        for (Artwork artwork : deck) {
            counts.merge(artwork.grade(), 1, Integer::sum);
            assertTrue(artwork.grade().isValidValue(artwork.realValue()));
        }
        for (Grade grade : Grade.values()) {
            assertEquals(grade.count(), counts.getOrDefault(grade, 0));
        }
        assertEquals(GameConfig.BASE_ROUNDS,
                deck.stream().map(Artwork::id).distinct().count());
    }

    @Test
    void createsLargerDecksForFiveAndSixPlayers() {
        for (int players = 5; players <= 6; players++) {
            int rounds = GameConfig.roundsFor(players);
            List<Artwork> deck = ArtworkDeckFactory.createDeck(rounds);
            assertEquals(rounds, deck.size());
            assertEquals(rounds, deck.stream().map(Artwork::id).distinct().count());
            for (Artwork artwork : deck) {
                assertTrue(artwork.grade().isValidValue(artwork.realValue()));
            }
            for (Grade grade : Grade.values()) {
                assertTrue(deck.stream().filter(a -> a.grade() == grade).count() >= grade.count(),
                        players + "명 덱에 " + grade + "가 기본 개수 이상");
            }
        }
        assertThrows(IllegalArgumentException.class, () -> ArtworkDeckFactory.createDeck(7));
    }

    @Test
    void returnedDeckCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> ArtworkDeckFactory.createDeck().add(null));
    }
}
