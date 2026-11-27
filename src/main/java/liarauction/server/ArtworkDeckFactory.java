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

    /** 기본 8장 덱 (2~4명) */
    public static List<Artwork> createDeck() {
        return createDeck(GameConfig.BASE_ROUNDS);
    }

    /**
     * 라운드 수만큼의 덱. 8장은 등급별 개수(명작 2, 보통 3, 가짜 3) 그대로,
     * 5~6명용으로 더 필요한 장수는 보통 → 가짜 → 명작 순서로 채운다.
     * TODO(#47): 작품 16개 목록과 인원별 등급 구성으로 교체
     *
     * @throws IllegalArgumentException 기본 라운드 수보다 적을 때
     */
    public static List<Artwork> createDeck(int rounds) {
        if (rounds < GameConfig.BASE_ROUNDS) {
            throw new IllegalArgumentException("라운드 수는 " + GameConfig.BASE_ROUNDS + " 이상: " + rounds);
        }
        List<Grade> grades = new ArrayList<>(rounds);
        for (Grade grade : Grade.values()) {
            for (int i = 0; i < grade.count(); i++) {
                grades.add(grade);
            }
        }
        Grade[] extra = {Grade.NORMAL, Grade.FAKE, Grade.MASTERPIECE};
        for (int i = 0; grades.size() < rounds; i++) {
            grades.add(extra[i % extra.length]);
        }

        List<Artwork> deck = new ArrayList<>(rounds);
        int id = 1;
        for (Grade grade : grades) {
            deck.add(new Artwork(id, "작품 " + id, "작가 " + id, grade, randomValue(grade)));
            id++;
        }
        Collections.shuffle(deck);
        return List.copyOf(deck);
    }

    private static int randomValue(Grade grade) {
        int range = grade.maxValue() - grade.minValue() + 1;
        return grade.minValue() + ThreadLocalRandom.current().nextInt(range);
    }
}
