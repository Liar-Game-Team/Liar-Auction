package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;

/**
 * 게임 화면. 서버 메시지를 받아 각 영역으로 나눠 준다.
 * <pre>
 * ┌──────────── 상태 (라운드, 단계, 시간, 내 현금) ──────────┐
 * │  진행자 (말풍선)               │                        │
 * │  경매 작품 카드 (파트 A)        │  소문·아이템·미션 (B)   │
 * └────────────────────────────────┴────────────────────────┘
 *   + 위에 떠 있는 채팅창 (#39, 드래그·접기)
 * </pre>
 */
public final class GamePanel extends JPanel {

    private final StatusPanel status = new StatusPanel();
    private final AuctionPanel auction;
    private final InfoPanel info;
    private final ChatPanel chat;
    /** 떠 있는 채팅창 (#39) */
    private final FloatingChat floatingChat;
    private Phase lastPhase;
    /** 진행자 (#40) */
    private final HostPanel host = new HostPanel();
    private final HostScript script = new HostScript();
    /** 게임 결과 줄 (GAME_OVER가 오면 결과 창으로) */
    private final List<ResultDialog.PlayerRow> resultPlayers = new ArrayList<>();
    private final List<ResultDialog.ArtworkRow> resultArtworks = new ArrayList<>();
    private String myName = "";

    public GamePanel(Consumer<Message> sender) {
        super(new BorderLayout());
        Theme.clear(this);
        auction = new AuctionPanel(sender);
        info = new InfoPanel(sender);
        floatingChat = new FloatingChat(sender);
        chat = floatingChat.chat();

        JPanel board = Theme.clear(new JPanel(new BorderLayout(10, 10)));
        board.setBorder(javax.swing.BorderFactory.createEmptyBorder(64, 14, 58, 14));
        board.add(status, BorderLayout.NORTH);
        // 가운데: 위에는 진행자, 아래에는 경매 작품 카드
        JPanel stage = Theme.clear(new JPanel(new BorderLayout(0, 8)));
        stage.add(host, BorderLayout.NORTH);
        stage.add(auction, BorderLayout.CENTER);
        board.add(stage, BorderLayout.CENTER);
        board.add(info, BorderLayout.EAST);

        // 아래층에 게임 화면, 위층에 떠 있는 채팅창
        JLayeredPane layers = new JLayeredPane();
        layers.setLayout(new FloatLayout(board, floatingChat));
        layers.add(board, JLayeredPane.DEFAULT_LAYER);
        layers.add(floatingChat, JLayeredPane.PALETTE_LAYER);
        add(layers, BorderLayout.CENTER);
    }

    /** 게임 화면은 꽉 채우고, 채팅창은 처음엔 왼쪽 아래에 두고 그 뒤로는 사용자가 옮긴 자리 (화면 안으로만) */
    private static final class FloatLayout implements LayoutManager {
        private final JPanel board;
        private final FloatingChat chat;
        private boolean placed;

        FloatLayout(JPanel board, FloatingChat chat) {
            this.board = board;
            this.chat = chat;
        }

        @Override
        public void layoutContainer(Container parent) {
            int w = parent.getWidth();
            int h = parent.getHeight();
            board.setBounds(0, 0, w, h);
            Dimension size = chat.getPreferredSize();
            if (!placed && w > 0 && h > 0) {
                chat.setBounds(14, h - size.height - 10, size.width, size.height);
                placed = true;
            } else {
                chat.setSize(size);
                chat.keepInside();
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return board.getPreferredSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return board.getMinimumSize();
        }

        @Override
        public void addLayoutComponent(String name, java.awt.Component comp) {
        }

        @Override
        public void removeLayoutComponent(java.awt.Component comp) {
        }
    }

    /** 떠 있는 채팅창 (테스트·미리보기용) */
    FloatingChat floatingChat() {
        return floatingChat;
    }

    public void startGame(String myName, List<String> players, int startCash, int totalRounds) {
        this.myName = myName;
        resultPlayers.clear();
        resultArtworks.clear();
        auction.clear();
        floatingChat.reset();
        chat.setMyName(myName);
        lastPhase = null;
        info.clear();
        info.setTotalRounds(totalRounds);
        status.startGame(myName, players, startCash, totalRounds);
        host.clear();
        host.say(script.welcome(players.size(), totalRounds));
        chat.addSystem(players.size() + "명 게임 시작 (" + String.join(", ", players) + "), 총 "
                + totalRounds + "라운드, 브레이크타임은 "
                + GameConfig.breakAfterRound(totalRounds) + "라운드 뒤");
    }

    public void stop() {
        status.stop();
    }

    /** {@code GAME_START} 이후 받는 메시지 처리 */
    public void handle(Message msg) {
        host.say(script.lineFor(msg));
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
                    // 협상 때는 채팅창을 펼치고, 경매가 시작되면 입찰 버튼을 가리지 않게 접는다
                    if (phase != lastPhase) {
                        if (phase == Phase.NEGOTIATION) {
                            floatingChat.expand();
                        } else if (phase == Phase.AUCTION) {
                            floatingChat.collapse();
                        }
                        lastPhase = phase;
                    }
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
