package liarauction.server.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import liarauction.common.Artwork;
import liarauction.common.Grade;

/**
 * 감정서·소문 생성 (#7).
 *
 * <p>정확도만큼의 확률로 실제 등급을 알려 주고, 틀릴 때는 나머지 등급 중 하나를 고른다.
 * 실제 진위·등급은 서버만 알고, 클라이언트는 이 문장만 받는다.
 */
public final class InspectionGenerator {

    /** 감정서 정확도 */
    public static final double OFFICIAL_ACCURACY = 0.8;
    /** 소문 정확도 */
    public static final double RUMOR_ACCURACY = 0.6;

    private final Random random;

    public InspectionGenerator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public InspectionGenerator() {
        this(new Random());
    }

    /** @param official true면 감정서(80%), false면 소문(60%) */
    public Inspection generate(Artwork artwork, boolean official) {
        double accuracy = official ? OFFICIAL_ACCURACY : RUMOR_ACCURACY;
        Grade claimed = random.nextDouble() < accuracy ? artwork.grade() : wrongGrade(artwork.grade());
        return new Inspection(official, claimed, describe(official, claimed));
    }

    private Grade wrongGrade(Grade actual) {
        List<Grade> others = new ArrayList<>(List.of(Grade.values()));
        others.remove(actual);
        return others.get(random.nextInt(others.size()));
    }

    /** 등급별 문장. 감정서는 단정적으로, 소문은 흐릿하게. */
    static String describe(boolean official, Grade claimed) {
        if (official) {
            return switch (claimed) {
                case MASTERPIECE -> "진품, 명작급으로 판정합니다. (예상 가치 "
                        + Grade.MASTERPIECE.minValue() + "~" + Grade.MASTERPIECE.maxValue() + ")";
                case NORMAL -> "진품, 보통 작품으로 판정합니다. (예상 가치 "
                        + Grade.NORMAL.minValue() + "~" + Grade.NORMAL.maxValue() + ")";
                case FAKE -> "위작으로 판정합니다. (가치 없음)";
            };
        }
        return switch (claimed) {
            case MASTERPIECE -> "숨겨진 명작이라는 얘기가 돌아요.";
            case NORMAL -> "평범한 진품이라던데요.";
            case FAKE -> "가짜라는 말이 있어요.";
        };
    }
}
