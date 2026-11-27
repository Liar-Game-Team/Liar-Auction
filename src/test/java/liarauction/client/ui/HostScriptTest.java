package liarauction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import liarauction.common.Message;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;

class HostScriptTest {

    private final HostScript script = new HostScript(new Random(1));

    @Test
    void announcesRoundWithArtworkAndStartPrice() {
        String line = script.lineFor(Message.of(Protocol.START_ROUND, 3, "해질녘의 정원", "한서윤", 100));
        assertEquals("3라운드! 이번 작품은 한서윤의 「해질녘의 정원」입니다. 시작가는 100만원.", line);
    }

    @Test
    void speaksOnlyWhenThePhaseChanges() {
        assertNotNull(script.lineFor(Message.of(Protocol.PHASE, "REVEAL", 10)));
        assertNull(script.lineFor(Message.of(Protocol.PHASE, "REVEAL", 9)), "남은 시간 갱신에는 말하지 않음");
        assertNull(script.lineFor(Message.of(Protocol.PHASE, "RUMOR", 5)), "소문 단계는 RUMOR로 말함");
        assertNotNull(script.lineFor(Message.of(Protocol.PHASE, "NEGOTIATION", 30)));
        assertTrue(script.lineFor(Message.of(Protocol.PHASE, "AUCTION", 20)).contains("입찰"));
        assertNotNull(script.lineFor(Message.of(Protocol.PHASE, "REVEAL", 10)), "다음 라운드 공개는 다시 말함");
    }

    @Test
    void whispersTheRumor() {
        String line = script.lineFor(Message.of(Protocol.RUMOR, "가짜라는 말이 있어요."));
        assertTrue(line.endsWith("가짜라는 말이 있어요."), line);
    }

    @Test
    void announcesBidsAndHammer() {
        assertTrue(script.lineFor(Message.of(Protocol.UPDATE_BID, "주성진", 260)).startsWith("260만원! 주성진"));
        String sold = script.lineFor(Message.of(Protocol.ROUND_END, "주성진", 260));
        assertEquals("260만원, 주성진 님께 낙찰! 탕!", sold);
        assertTrue(script.lineFor(Message.of(Protocol.ROUND_END, Protocol.NONE, 0)).contains("유찰"));
    }

    @Test
    void otherEventsAndUnknownMessages() {
        assertTrue(script.welcome(3, 8).contains("8점"));
        assertNotNull(script.lineFor(Message.of(Protocol.MISSION, "[큰손] ...")));
        assertTrue(script.lineFor(Message.of(Protocol.PLAYER_LEFT, "민수")).startsWith("민수 님이"));
        assertNotNull(script.lineFor(Message.of(Protocol.GAME_OVER, "철수", "")));
        assertNull(script.lineFor(Message.of(Protocol.UPDATE_CASH, "철수", 100)), "할 말 없는 메시지");
        assertNull(script.lineFor(Message.of(Protocol.UPDATE_BID, "철수", "abc")), "잘못된 숫자는 무시");
    }
}
