package liarauction.client.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import liarauction.common.GameConfig;
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
    /** 게임 결과 줄 (GAME_OVER가 오면 결과 창으로) */
    private final List<ResultDialog.PlayerRow> resultPlayers = new ArrayList<>();
    private final List<ResultDialog.ArtworkRow> resultArtworks = new ArrayList<>();
    private String myName = "";

    public GamePanel(Consumer<Message> sender) {
        super(new BorderLayout(10, 10));
        Theme.clear(this);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(64, 14, 12, 14));
        auction = new AuctionPanel(sender);
        info = new InfoPanel(sender);
        chat = new ChatPanel(sender);

        add(status, BorderLayout.NORTH);
        add(auction, BorderLayout.CENTER);
        add(info, BorderLayout.EAST);
        add(chat, BorderLayout.SOUTH);
    }

    public void startGame(String myName, List<String> players, int startCash, int totalRounds) {
        this.myName = myName;
        resultPlayers.clear();
        resultArtworks.clear();
        auction.clear();
        chat.clear();
        info.clear();
        info.setTotalRounds(totalRounds);
        status.startGame(myName, players, startCash, totalRounds);
        chat.addSystem(players.size() + "명 게임 시작 (" + String.join(", ", players) + "), 총 "
                + totalRounds + "라운드, 브레이크타임은 "
                + GameConfig.breakAfterRound(totalRounds) + "라운드 뒤");
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
                    info.setRound(msg.intArg(0));
                    auction.showArtwork(msg.arg(1), msg.arg(2), msg.intArg(3));
                    info.addLine("--- 라운드 " + msg.arg(0) + " ---");
                }
                case Protocol.PHASE -> {
                    Phase phase = Phase.valueOf(msg.arg(0));
                    int secondsRemaining = msg.intArg(1);
                    status.setPhase(phase, secondsRemaining);
                    info.setPhase(phase);
                    auction.setAuctionPhase(phase, secondsRemaining);
                }
                case Protocol.ITEMS -> info.updateItems(msg.intArg(0), msg.intArg(1));
                case Protocol.ITEM_RESULT -> info.showItemResult(msg.arg(0), msg.arg(1));
                case Protocol.RUMOR -> info.showRumor(msg.arg(0));
                case Protocol.MISSION -> {
                    info.showMission(msg.arg(0));
                    chat.addSystem("히든 미션을 받았습니다. 오른쪽 위에서 확인하세요 (나만 보임)");
                }
                case Protocol.UPDATE_BID -> auction.updateBid(msg.arg(0), msg.intArg(1));
                case Protocol.BID_REJECT -> auction.showNotice("입찰 거부: " + msg.arg(0));
                case Protocol.ROUND_END -> auction.showRoundEnd(msg.arg(0), msg.intArg(1));
                case Protocol.UPDATE_CASH -> status.setCash(msg.arg(0), msg.intArg(1));
                case Protocol.PLAYER_LEFT -> {
                    status.removePlayer(msg.arg(0));
                    chat.addSystem(msg.arg(0) + " 님이 나갔습니다. 남은 사람끼리 계속합니다");
                }
                case Protocol.RESULT_PLAYER -> resultPlayers.add(ResultDialog.PlayerRow.from(msg));
                case Protocol.RESULT_ARTWORK -> resultArtworks.add(ResultDialog.ArtworkRow.from(msg));
                case Protocol.GAME_OVER -> {
                    status.stop();
                    chat.addSystem("게임 종료, " + ResultDialog.winnerText(resultPlayers));
                    info.addLine(msg.arg(1));
                    showResult();
                }
                case Protocol.ERROR -> chat.addSystem("오류: " + msg.arg(0));
                default -> System.err.println("[클라이언트] 처리하지 않은 메시지: " + msg);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[클라이언트] 메시지 처리 실패: " + msg + " (" + e.getMessage() + ")");
        }
    }

    /** 결과 창 (모달 아님). 방 화면으로 돌아가도 그대로 볼 수 있다 */
    private void showResult() {
        ResultDialog dialog = new ResultDialog(SwingUtilities.getWindowAncestor(this), myName,
                List.copyOf(resultPlayers), List.copyOf(resultArtworks));
        resultPlayers.clear();
        resultArtworks.clear();
        dialog.setVisible(true);
    }
}
