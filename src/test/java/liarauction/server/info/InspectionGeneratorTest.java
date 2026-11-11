package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
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
    void textMatchesClaimAndHidesNothingExtra() {
        InspectionGenerator gen = new InspectionGenerator(new Random(7));
        Artwork art = new Artwork(1, "해바라기", "고흐", Grade.NORMAL, 237);
        for (int i = 0; i < 200; i++) {
            Inspection insp = gen.generate(art, i % 2 == 0);
            assertEquals(i % 2 == 0, insp.official());
            assertEquals(InspectionGenerator.describe(insp.official(), insp.claimedGrade()), insp.text());
            assertFalse(insp.text().contains("237"), "실제 가치가 문장에 나오면 안 됨");
            assertFalse(insp.text().contains("|"));
        }
    }

    @Test
    void everyGradeHasText() {
        for (Grade g : Grade.values()) {
            assertFalse(InspectionGenerator.describe(true, g).isBlank());
            assertFalse(InspectionGenerator.describe(false, g).isBlank());
        }
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
