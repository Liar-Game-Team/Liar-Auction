package liarauction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import liarauction.common.Message;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;

class RoomPanelTest {

    @Test
    void hostCanStartOnlyWhenOthersAreReady() throws Exception {
        AtomicReference<Message> sent = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            RoomPanel panel = new RoomPanel(sent::set);
            panel.enter(1, "방", 6, false);
            JButton start = button(panel, "게임 시작");
            JButton ready = button(panel, "준비");

            // 방장 혼자
            panel.update("철수", "철수", List.of("철수"), List.of());
            assertTrue(start.isVisible());
            assertFalse(start.isEnabled());
            assertFalse(ready.isVisible());

            // 2명, 영희 아직 준비 안 함
            panel.update("철수", "철수", List.of("철수", "영희"), List.of());
            assertFalse(start.isEnabled());

            // 영희 준비 → 방장 시작 가능
            panel.update("철수", "철수", List.of("철수", "영희"), List.of("영희"));
            assertTrue(start.isEnabled());
            start.doClick();
            assertEquals(Message.of(Protocol.REQ_START), sent.get());
        });
    }

    @Test
    void otherPlayersToggleReady() throws Exception {
        AtomicReference<Message> sent = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            RoomPanel panel = new RoomPanel(sent::set);
            panel.enter(1, "방", 6, false);
            JButton ready = button(panel, "준비");

            panel.update("영희", "철수", List.of("철수", "영희"), List.of());
            assertTrue(ready.isVisible());
            assertFalse(button(panel, "게임 시작").isVisible());
            ready.doClick();
            assertEquals(Message.of(Protocol.REQ_READY, true), sent.get());

            panel.update("영희", "철수", List.of("철수", "영희"), List.of("영희"));
            assertEquals("준비 취소", ready.getText());
            ready.doClick();
            assertEquals(Message.of(Protocol.REQ_READY, false), sent.get());
        });
    }

    @Test
    void leaveButtonSendsLeaveRoom() throws Exception {
        AtomicReference<Message> sent = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            RoomPanel panel = new RoomPanel(sent::set);
            panel.enter(3, "초보만", 4, true);
            assertTrue(labels(panel).contains("[비밀방] 3번 방 - 초보만"));
            button(panel, "방 나가기").doClick();
            assertEquals(Message.of(Protocol.REQ_LEAVE_ROOM), sent.get());
        });
    }

    @Test
    void parsesRoomList() {
        assertEquals(List.of(), RoomListPanel.parse(""));
        assertEquals(List.of(
                        new RoomListPanel.RoomRow(1, "초보만", 2, 4, false, false),
                        new RoomListPanel.RoomRow(3, "비밀", 6, 6, true, true)),
                RoomListPanel.parse("1,초보만,2,4,false,false;3,비밀,6,6,true,true"));
    }

    @Test
    void resultRowsParseAndWinnerText() {
        ResultDialog.PlayerRow first = ResultDialog.PlayerRow.from(
                Message.of(Protocol.RESULT_PLAYER, 1, "철수", 230, 900, 7, 100, 1230, "[큰손] 미션", true));
        assertEquals(new ResultDialog.PlayerRow(1, "철수", 230, 900, 7, 100, 1230, "[큰손] 미션", true), first);
        ResultDialog.PlayerRow tie = new ResultDialog.PlayerRow(1, "영희", 1230, 0, 0, 0, 1230, "", false);
        ResultDialog.PlayerRow third = new ResultDialog.PlayerRow(3, "민수", 500, 0, 0, 0, 500, "", false);
        assertEquals("우승: 철수", ResultDialog.winnerText(List.of(first, third)));
        assertEquals("공동 1위: 철수, 영희", ResultDialog.winnerText(List.of(first, tie, third)));

        assertEquals(new ResultDialog.ArtworkRow(2, "별밤", "김화백", "명작", 512, "유찰"),
                ResultDialog.ArtworkRow.from(Message.of(Protocol.RESULT_ARTWORK, 2, "별밤", "김화백", "명작", 512, "유찰")));
    }

    @Test
    void statusShowsEveryPlayersCashInManwon() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StatusPanel status = new StatusPanel();
            status.startGame("철수", List.of("철수", "영희", "민수"), 1000, 8);
            status.setCash("영희", 890);
            status.removePlayer("민수");
            String text = labels(status).stream().filter(t -> t.startsWith("현금")).findFirst().orElseThrow();
            assertEquals("현금 철수(나) 1000만원 · 영희 890만원", text);
        });
    }

    private static JButton button(Container parent, String text) {
        for (Component c : all(parent)) {
            if (c instanceof JButton b && b.getText().equals(text)) {
                return b;
            }
        }
        throw new AssertionError("버튼 없음: " + text);
    }

    private static List<String> labels(Container parent) {
        List<String> texts = new ArrayList<>();
        for (Component c : all(parent)) {
            if (c instanceof JLabel l) {
                texts.add(l.getText());
            }
        }
        return texts;
    }

    private static List<Component> all(Container parent) {
        List<Component> out = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            out.add(c);
            if (c instanceof Container inner) {
                out.addAll(all(inner));
            }
        }
        return out;
    }
}
