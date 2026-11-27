package liarauction.server.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import liarauction.common.Artwork;
import liarauction.common.Grade;

/**
 * 감정서·소문 생성 (#35/#36).
 *
 * <p>정확도만큼의 확률로 실제 등급을 알려 주고, 틀릴 때는 나머지 등급 중 하나를 고른다.
 * 실제 진위·등급은 서버만 알고, 클라이언트는 이 문장만 받는다.
 * 문장은 {@link InspectionTexts}에서 등급별로 무작위로 고른다.
 *
 * <p>감정서의 예상 가치는 {@link #ESTIMATE_WIDTH}만원 폭으로 좁혀서 알려 준다.
 * 판정이 맞으면 실제 가치가 그 범위 안에 있고, 틀리면 주장한 등급 범위 안에서 임의로 정해서
 * 겉으로는 맞는 감정서와 구별되지 않는다. 범위의 양 끝은 실제 가치와 겹치지 않게 해서
 * 숫자로 실제 가치가 드러나지 않게 한다.
 */
public final class InspectionGenerator {

    /** 감정서 정확도 */
    public static final double OFFICIAL_ACCURACY = 0.8;
    /** 소문 정확도 */
    public static final double RUMOR_ACCURACY = 0.6;
    /** 감정서 예상 가치 범위의 폭 (만원) */
    public static final int ESTIMATE_WIDTH = 60;

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
        String text = official ? certificateText(artwork, claimed) : rumorText(artwork, claimed);
        return new Inspection(official, claimed, text);
    }

    /** 교란당한 플레이어용: 실제 등급과도, 기본 소문과도 다른 100% 거짓 소문 */
    public Inspection generateJammedRumor(Artwork artwork, Grade ordinaryClaim) {
        Objects.requireNonNull(artwork, "artwork");
        Objects.requireNonNull(ordinaryClaim, "ordinaryClaim");
        List<Grade> candidates = new ArrayList<>(List.of(Grade.values()));
        candidates.remove(artwork.grade());
        candidates.remove(ordinaryClaim);
        Grade claimed = candidates.isEmpty()
                ? wrongGrade(artwork.grade())
                : candidates.get(random.nextInt(candidates.size()));
        return new Inspection(false, claimed, rumorText(artwork, claimed));
    }

    /** 이 등급이라고 주장하는 소문 한 문장 */
    String rumorText(Artwork artwork, Grade claimed) {
        return InspectionTexts.render(pick(InspectionTexts.RUMORS.get(claimed)), artwork, 0, 0);
    }

    /** 이 등급으로 판정하는 감정서 한 문장 (진품 판정이면 예상 가치 범위 포함) */
    String certificateText(Artwork artwork, Grade claimed) {
        int[] range = estimate(artwork, claimed);
        return InspectionTexts.render(pick(InspectionTexts.CERTIFICATES.get(claimed)), artwork, range[0], range[1]);
    }

    /**
     * 예상 가치 범위 {lo, hi}. 가짜 판정이면 {0, 0}.
     * 판정이 맞으면 실제 가치를, 틀리면 주장한 등급 범위의 임의 값을 기준으로 잡고
     * 기준이 범위 안쪽(양 끝에서 5 이상 떨어진 곳)에 오도록 폭 {@link #ESTIMATE_WIDTH}의 범위를 만든다.
     */
    int[] estimate(Artwork artwork, Grade claimed) {
        if (claimed == Grade.FAKE) {
            return new int[] {0, 0};
        }
        int base = claimed == artwork.grade()
                ? artwork.realValue()
                : claimed.minValue() + random.nextInt(claimed.maxValue() - claimed.minValue() + 1);
        int offset = 5 + random.nextInt(ESTIMATE_WIDTH - 9);
        int lo = base - offset;
        int hi = lo + ESTIMATE_WIDTH;
        if (lo < claimed.minValue()) {
            lo = claimed.minValue();
            hi = lo + ESTIMATE_WIDTH;
        }
        if (hi > claimed.maxValue()) {
            hi = claimed.maxValue();
            lo = hi - ESTIMATE_WIDTH;
        }
        return new int[] {lo, hi};
    }

    private String pick(List<String> options) {
        return options.get(random.nextInt(options.size()));
    }

    private Grade wrongGrade(Grade actual) {
        List<Grade> others = new ArrayList<>(List.of(Grade.values()));
        others.remove(actual);
        return others.get(random.nextInt(others.size()));
    }
}
