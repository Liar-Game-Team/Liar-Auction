package liarauction.client.ui;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JPanel;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;

/**
 * 게임 화면. 서버 메시지를 받아 각 영역으로 나눠 준다.
 * <pre>
 * ┌──────────── 상태 (라운드, 단계, 시간, 현금) ────────────┐
 * │         경매 (파트 A)          │  감정 정보 (파트 B)   │
 * ├──────────────────── 협상 채팅 ─────────────────────────┤
 * </pre>
 */
public final class GamePanel extends JPanel {

    private final StatusPanel status = new StatusPanel();
    private final AuctionPanel auction;
    private final InfoPanel info;
    private final ChatPanel chat;

    public GamePanel(Consumer<Message> sender) {
        super(new BorderLayout(8, 8));
        auction = new AuctionPanel(sender);
        info = new InfoPanel(sender);
        chat = new ChatPanel(sender);

        add(status, BorderLayout.NORTH);
        add(auction, BorderLayout.CENTER);
        add(info, BorderLayout.EAST);
        add(chat, BorderLayout.SOUTH);
    }

    public void startGame(String myName, String opponent, int startCash, int totalRounds) {
        chat.clear();
        info.clear();
        status.startGame(myName, opponent, startCash, totalRounds);
        chat.addSystem(opponent + " 님과 게임을 시작합니다");
    }

    public void stop() {
        status.stop();
    }

    /** {@code GAME_START} 이후 받는 메시지 처리 */
    public void handle(Message msg) {
        try {
            switch (msg.command()) {
                case Protocol.BROADCAST_CHAT -> chat.addChat(msg.arg(0), msg.arg(1));
                case Protocol.START_ROUND -> {
                    status.setRound(msg.intArg(0));
                    auction.showArtwork(msg.arg(1), msg.arg(2), msg.intArg(3));
                    info.addLine("--- 라운드 " + msg.arg(0) + " ---");
                }
                case Protocol.PHASE -> status.setPhase(Phase.valueOf(msg.arg(0)), msg.intArg(1));
                case Protocol.INSPECTION -> info.showInspection(msg.boolArg(0), msg.arg(1));
                case Protocol.APPRAISE_RESULT -> info.showAppraisal(msg.arg(0));
                case Protocol.UPDATE_BID -> auction.updateBid(msg.arg(0), msg.intArg(1));
                case Protocol.BID_REJECT -> auction.showNotice("입찰 거부: " + msg.arg(0));
                case Protocol.ROUND_END -> auction.showRoundEnd(msg.arg(0), msg.intArg(1));
                // TODO(#13): 되팔기 창
                case Protocol.RESALE_OFFER -> info.addLine(msg.arg(0) + " 님의 되팔기 제안: " + msg.arg(1));
                case Protocol.RESALE_RESULT -> info.addLine("되팔기 " + msg.arg(0) + " (" + msg.arg(1) + ")");
                case Protocol.UPDATE_CASH -> status.setCash(msg.arg(0), msg.intArg(1));
                // TODO(#19): 결과 창
                case Protocol.GAME_OVER -> {
                    status.stop();
                    chat.addSystem("게임 종료, 승자: " + msg.arg(0));
                    info.addLine(msg.arg(1));
                }
                case Protocol.ERROR -> chat.addSystem("오류: " + msg.arg(0));
                default -> System.err.println("[클라이언트] 처리하지 않은 메시지: " + msg);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[클라이언트] 메시지 처리 실패: " + msg + " (" + e.getMessage() + ")");
        }
    }
}
