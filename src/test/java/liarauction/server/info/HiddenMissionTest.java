package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import liarauction.common.Artwork;
import liarauction.common.Grade;
import liarauction.common.PlayerState;
import org.junit.jupiter.api.Test;

class HiddenMissionTest {

    private int nextId = 1;

    @Test
    void masterpieceCollector() {
        PlayerState me = player("나", 1000);
        assertFalse(HiddenMission.MASTERPIECE_COLLECTOR.achieved(me, null));
        give(me, Grade.MASTERPIECE);
        assertTrue(HiddenMission.MASTERPIECE_COLLECTOR.achieved(me, null));
    }

    @Test
    void noFakesNeedsTwoArtworksAndNoFake() {
        PlayerState me = player("나", 1000);
        give(me, Grade.NORMAL);
        assertFalse(HiddenMission.NO_FAKES.achieved(me, null), "작품 1점이면 실패");
        give(me, Grade.MASTERPIECE);
        assertTrue(HiddenMission.NO_FAKES.achieved(me, null));
        give(me, Grade.FAKE);
        assertFalse(HiddenMission.NO_FAKES.achieved(me, null), "가짜가 있으면 실패");
    }

    @Test
    void bigSpenderSaverAndSteady() {
        PlayerState me = player("나", 1000);
        assertTrue(HiddenMission.SAVER.achieved(me, null));
        me.pay(501);
        assertFalse(HiddenMission.SAVER.achieved(me, null), "499만원이면 실패");

        give(me, Grade.NORMAL);
        assertFalse(HiddenMission.STEADY.achieved(me, null));
        give(me, Grade.NORMAL);
        assertTrue(HiddenMission.STEADY.achieved(me, null));

        give(me, Grade.FAKE);
        assertFalse(HiddenMission.BIG_SPENDER.achieved(me, null));
        give(me, Grade.FAKE);
        assertTrue(HiddenMission.BIG_SPENDER.achieved(me, null));
    }

    @Test
    void rivalMissionsCompareWithTheTarget() {
        PlayerState me = player("나", 600);
        PlayerState rival = player("상대", 600);
        assertFalse(HiddenMission.RIVAL_COLLECTION.achieved(me, rival), "같으면 실패");
        assertFalse(HiddenMission.RIVAL_CASH.achieved(me, rival), "같으면 실패");

        give(me, Grade.FAKE);
        assertTrue(HiddenMission.RIVAL_COLLECTION.achieved(me, rival));
        rival.pay(100);
        assertTrue(HiddenMission.RIVAL_CASH.achieved(me, rival));
    }

    @Test
    void describeShowsTargetAndBonus() {
        assertEquals("[라이벌] 영희보다 작품을 더 많이 가지고 끝내세요 (성공하면 +100만원)",
                HiddenMission.RIVAL_COLLECTION.describe("영희"));
        assertEquals("[명작 수집가] 명작을 1점 이상 가지고 끝내세요 (성공하면 +100만원)",
                HiddenMission.MASTERPIECE_COLLECTOR.describe(null));
        assertTrue(HiddenMission.values().length >= 6, "최대 6명이 서로 다른 미션을 받을 수 있어야 함");
    }

    private static PlayerState player(String name, int cash) {
        return new PlayerState(name, cash);
    }

    private void give(PlayerState p, Grade grade) {
        int id = nextId++;
        p.addArtwork(new Artwork(id, "t" + id, "a", grade, grade.minValue()));
    }
}
