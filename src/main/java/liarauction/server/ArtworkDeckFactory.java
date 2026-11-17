package liarauction.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.Grade;

/** Creates the server-side artwork deck for one game. */
public final class ArtworkDeckFactory {

    private ArtworkDeckFactory() {
    }

    public static List<Artwork> createDeck() {
        List<Artwork> deck = new ArrayList<>(GameConfig.TOTAL_ROUNDS);
        int id = 1;
        for (Grade grade : Grade.values()) {
            for (int i = 0; i < grade.count(); i++) {
                int value = randomValue(grade);
                deck.add(new Artwork(id, "작품 " + id, "작가 " + id, grade, value));
                id++;
            }
        }
        Collections.shuffle(deck);
        return List.copyOf(deck);
    }

    private static int randomValue(Grade grade) {
        int range = grade.maxValue() - grade.minValue() + 1;
        return grade.minValue() + ThreadLocalRandom.current().nextInt(range);
    }
}
