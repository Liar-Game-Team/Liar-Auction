package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import liarauction.common.Artwork;
import liarauction.common.Grade;
import org.junit.jupiter.api.Test;

class InspectionGeneratorTest {

    private static final int TRIALS = 20_000;

    @Test
    void officialAccuracyAbout80Percent() {
        assertAccuracy(true, InspectionGenerator.OFFICIAL_ACCURACY);
    }

    @Test
    void rumorAccuracyAbout60Percent() {
        assertAccuracy(false, InspectionGenerator.RUMOR_ACCURACY);
    }

    @Test
    void wrongAnswersCoverBothOtherGrades() {
        InspectionGenerator gen = new InspectionGenerator(new Random(1));
        Artwork fake = new Artwork(1, "t", "a", Grade.FAKE, 0);
        Set<Grade> wrong = EnumSet.noneOf(Grade.class);
        for (int i = 0; i < 1000; i++) {
            Grade g = gen.generate(fake, false).claimedGrade();
            if (g != Grade.FAKE) {
                wrong.add(g);
            }
        }
        assertEquals(EnumSet.of(Grade.MASTERPIECE, Grade.NORMAL), wrong);
    }

    @Test
    void jammedRumorIsFalseAndDiffersFromTheOrdinaryRumor() {
        InspectionGenerator generator = new InspectionGenerator(new Random(5));
        for (Grade actual : Grade.values()) {
            Artwork artwork = new Artwork(1, "t", "a", actual, actual.minValue());
            for (Grade ordinaryClaim : Grade.values()) {
                Inspection jammed = generator.generateJammedRumor(artwork, ordinaryClaim);
                assertFalse(jammed.official());
                assertNotEquals(actual, jammed.claimedGrade());
                assertNotEquals(ordinaryClaim, jammed.claimedGrade());
            }
        }
    }

    @Test
    void textNamesTheArtworkAndNeverShowsTheRealValue() {
        InspectionGenerator gen = new InspectionGenerator(new Random(7));
        Artwork art = new Artwork(1, "해바라기 밭", "김화백", Grade.NORMAL, 237);
        for (int i = 0; i < 500; i++) {
            Inspection insp = gen.generate(art, i % 2 == 0);
            assertEquals(i % 2 == 0, insp.official());
            assertFalse(insp.text().contains("237"), "실제 가치가 문장에 나오면 안 됨: " + insp.text());
            assertFalse(insp.text().contains("{"), "채워지지 않은 자리 표시: " + insp.text());
            assertFalse(insp.text().contains("|"));
        }
    }

    @Test
    void everyTemplateRendersForEveryGrade() {
        Artwork[] samples = {
            new Artwork(1, "별밤", "김화백", Grade.MASTERPIECE, 512),      // 받침 있음
            new Artwork(2, "작품 3", "작가 2", Grade.NORMAL, 199),          // 숫자로 끝남
            new Artwork(3, "Moon", "Monet", Grade.FAKE, 0)                 // 영문
        };
        for (Artwork art : samples) {
            for (Grade claimed : Grade.values()) {
                for (String t : InspectionTexts.RUMORS.get(claimed)) {
                    String text = InspectionTexts.render(t, art, 0, 0);
                    assertFalse(text.contains("{") || text.contains("}"), text);
                    assertFalse(text.isBlank());
                }
                for (String t : InspectionTexts.CERTIFICATES.get(claimed)) {
                    String text = InspectionTexts.render(t, art, 430, 490);
                    assertFalse(text.contains("{") || text.contains("}"), text);
                    assertTrue(text.startsWith("감정 결과:"), text);
                    assertEquals(claimed != Grade.FAKE, text.contains("430~490만원"), text);
                }
            }
        }
        assertTrue(InspectionTexts.RUMORS.values().stream().allMatch(l -> l.size() >= 5), "등급별 소문 5개 이상");
    }

    @Test
    void correctCertificateEstimateContainsTheRealValue() {
        InspectionGenerator gen = new InspectionGenerator(new Random(3));
        for (Grade grade : List.of(Grade.MASTERPIECE, Grade.NORMAL)) {
            for (int value = grade.minValue(); value <= grade.maxValue(); value += 7) {
                Artwork art = new Artwork(1, "t", "a", grade, value);
                int[] r = gen.estimate(art, grade);
                assertEquals(InspectionGenerator.ESTIMATE_WIDTH, r[1] - r[0]);
                assertTrue(r[0] >= grade.minValue() && r[1] <= grade.maxValue(), "등급 범위 안");
                assertTrue(r[0] <= value && value <= r[1], value + " in " + r[0] + "~" + r[1]);
                if (value != grade.minValue() && value != grade.maxValue()) {
                    assertTrue(r[0] != value && r[1] != value, "끝값이 실제 가치와 같으면 안 됨");
                }
            }
        }
        // 가짜 판정은 범위 없음, 틀린 판정도 주장한 등급 범위 안
        Artwork fake = new Artwork(1, "t", "a", Grade.FAKE, 0);
        assertArrayEquals(new int[] {0, 0}, gen.estimate(fake, Grade.FAKE));
        int[] wrong = gen.estimate(fake, Grade.MASTERPIECE);
        assertTrue(wrong[0] >= Grade.MASTERPIECE.minValue() && wrong[1] <= Grade.MASTERPIECE.maxValue());
    }

    @Test
    void josaFollowsTheLastSyllable() {
        assertEquals("김화백이", Josa.attach("김화백", "이/가"));
        assertEquals("피카소가", Josa.attach("피카소", "이/가"));
        assertEquals("별밤을", Josa.attach("별밤", "을/를"));
        assertEquals("해바라기를", Josa.attach("해바라기", "을/를"));
        assertEquals("작품 3을", Josa.attach("작품 3", "을/를"));
        assertEquals("작가 2는", Josa.attach("작가 2", "은/는"));
    }

    private static void assertAccuracy(boolean official, double expected) {
        InspectionGenerator gen = new InspectionGenerator(new Random(42));
        for (Grade grade : Grade.values()) {
            Artwork art = new Artwork(1, "t", "a", grade, grade.minValue());
            int correct = 0;
            for (int i = 0; i < TRIALS; i++) {
                if (gen.generate(art, official).claimedGrade() == grade) {
                    correct++;
                }
            }
            double rate = (double) correct / TRIALS;
            assertTrue(Math.abs(rate - expected) < 0.02, grade + " 정확도 " + rate + ", 기대 " + expected);
        }
    }
}
