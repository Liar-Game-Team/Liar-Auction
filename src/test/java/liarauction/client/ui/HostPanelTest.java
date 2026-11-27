package liarauction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class HostPanelTest {

    @Test
    void typesThenShowsTheWholeLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            HostPanel host = new HostPanel();
            host.say("작품을 공개합니다.");
            assertEquals("작품을 공개합니다.", host.currentLine());
            assertTrue(host.visibleText().length() < "작품을 공개합니다.".length(), "처음에는 한 글자씩");
            host.finishTyping();
            assertEquals("작품을 공개합니다.", host.visibleText());
        });
    }

    @Test
    void queuesLinesAndKeepsOnlyTheLatest() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            HostPanel host = new HostPanel();
            host.say("첫째");
            host.say("둘째");
            host.say("셋째");
            host.say("넷째");
            assertEquals("첫째", host.currentLine(), "말하는 중에는 덮지 않음");
            assertEquals(HostPanel.MAX_QUEUE, host.queued(), "밀린 문장은 최근 것만");
            host.next();
            assertEquals("셋째", host.currentLine());
            host.next();
            assertEquals("넷째", host.currentLine());
            host.next();
            host.say("다섯째");
            assertEquals("다섯째", host.currentLine(), "쉬고 있으면 바로 말함");
        });
    }

    @Test
    void hammerFallsOnSale() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            HostPanel host = new HostPanel();
            host.say("260만원, 주성진 님께 낙찰! 탕!");
            assertTrue(host.isStriking());
            host.clear();
            assertFalse(host.isStriking());
            assertEquals("", host.visibleText());
        });
    }
}
