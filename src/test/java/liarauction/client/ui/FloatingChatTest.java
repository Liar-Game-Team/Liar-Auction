package liarauction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import liarauction.common.Message;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;

class FloatingChatTest {

    @Test
    void collapsedByDefaultAndCountsUnread() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FloatingChat chat = new FloatingChat(m -> { });
            assertFalse(chat.isExpanded(), "처음에는 접힌 막대");
            assertEquals(FloatingChat.COLLAPSED, chat.getSize());

            chat.chat().addChat("민수", "저건 가짜 같은데요?");
            chat.chat().addSystem("히든 미션을 받았습니다");
            assertEquals(2, chat.unread());
            assertEquals("* 히든 미션을 받았습니다", chat.previewText(), "막대에 최근 메시지");

            chat.expand();
            assertTrue(chat.isExpanded());
            assertEquals(0, chat.unread(), "펼치면 다 읽음");
            assertEquals(FloatingChat.EXPANDED, chat.getSize());
            chat.chat().addChat("민수", "펼쳐 있을 때");
            assertEquals(0, chat.unread(), "펼쳐 있으면 안 읽은 개수가 늘지 않음");

            chat.toggle();
            assertFalse(chat.isExpanded());
            chat.reset();
            assertEquals("", chat.chat().lastLine());
            assertEquals(0, chat.unread());
        });
    }

    @Test
    void staysInsideTheScreenWhenDragged() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel screen = new JPanel(null);
            screen.setSize(800, 600);
            FloatingChat chat = new FloatingChat(m -> { });
            screen.add(chat);
            chat.setBounds(10, 500, FloatingChat.COLLAPSED.width, FloatingChat.COLLAPSED.height);

            chat.moveTo(-100, 5000);
            assertEquals(0, chat.getX());
            assertEquals(600 - chat.getHeight(), chat.getY());
            chat.moveTo(5000, -20);
            assertEquals(800 - chat.getWidth(), chat.getX());
            assertEquals(0, chat.getY());

            // 펼치면 아래쪽을 기준으로 위로 커지고, 화면 안에 남는다
            chat.moveTo(0, 560);
            chat.expand();
            assertEquals(600, chat.getY() + chat.getHeight());
        });
    }

    @Test
    void opensForNegotiationAndClosesForAuction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel game = new GamePanel(m -> { });
            game.startGame("권이건", List.of("권이건", "주성진"), 1000, 8);
            assertFalse(game.floatingChat().isExpanded());
            game.handle(Message.of(Protocol.PHASE, "NEGOTIATION", 30));
            assertTrue(game.floatingChat().isExpanded(), "협상 때는 펼침");
            game.handle(Message.of(Protocol.PHASE, "NEGOTIATION", 29));
            game.floatingChat().collapse();
            game.handle(Message.of(Protocol.PHASE, "NEGOTIATION", 28));
            assertFalse(game.floatingChat().isExpanded(), "같은 단계 갱신에는 다시 펼치지 않음 (직접 접은 것 유지)");
            game.handle(Message.of(Protocol.PHASE, "AUCTION", 20));
            assertFalse(game.floatingChat().isExpanded(), "경매 때는 접힘");

            game.handle(Message.of(Protocol.BROADCAST_CHAT, "권이건", "내 메시지"));
            assertEquals("권이건: 내 메시지", game.floatingChat().chat().lastLine());
        });
    }
}
