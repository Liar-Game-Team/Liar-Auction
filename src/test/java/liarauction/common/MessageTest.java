package liarauction.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void encodeAndParse() {
        Message m = Message.of(Protocol.START_ROUND, 3, "별이 빛나는 밤", "고흐", 200);
        assertEquals("START_ROUND|3|별이 빛나는 밤|고흐|200", m.encode());

        Message parsed = Message.parse(m.encode());
        assertEquals(m, parsed);
        assertEquals(3, parsed.intArg(0));
        assertEquals(200, parsed.intArg(3));
    }

    @Test
    void noArgs() {
        Message m = Message.parse("REQ_APPRAISE");
        assertEquals(Protocol.REQ_APPRAISE, m.command());
        assertTrue(m.args().isEmpty());
    }

    @Test
    void specialCharactersInChat() {
        String text = "가격은 100|200 사이? \\ 백슬래시\n둘째 줄";
        Message m = Message.of(Protocol.BROADCAST_CHAT, "철수", text);
        String line = m.encode();

        assertTrue(line.indexOf('\n') < 0, "한 줄이어야 함");
        assertEquals(List.of("철수", text), Message.parse(line).args());
    }

    @Test
    void emptyArg() {
        Message m = Message.of(Protocol.REQ_CHAT, "");
        assertEquals("", Message.parse(m.encode()).arg(0));
    }

    @Test
    void boolArg() {
        assertTrue(Message.parse("INSPECTION|true|진품 같음").boolArg(0));
    }

    @Test
    void rejectsUnknownCommand() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse("HELLO|x"));
    }

    @Test
    void rejectsWrongArgCount() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse("REQ_BID"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("REQ_BID|10|20"));
    }

    @Test
    void rejectsBadEscape() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse("REQ_CHAT|a\\x"));
        assertThrows(IllegalArgumentException.class, () -> Message.parse("REQ_CHAT|a\\"));
    }

    @Test
    void rejectsEmptyLine() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse(""));
    }

    @Test
    void intArgNotNumber() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse("REQ_BID|abc").intArg(0));
    }
}
