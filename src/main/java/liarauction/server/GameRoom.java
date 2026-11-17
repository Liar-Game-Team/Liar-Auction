package liarauction.server;

import java.util.ArrayList;
import java.util.List;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import liarauction.server.auction.AuctionController;
import liarauction.server.info.InfoController;

/**
 * 1:1 게임 방. 로그인, 채팅, 접속 끊김을 처리하고 게임 메시지는 파트별 컨트롤러로 넘긴다.
 * <ul>
 *   <li>파트 A (경매 진행): {@link AuctionController}</li>
 *   <li>파트 B (정보·거래): {@link InfoController}</li>
 * </ul>
 *
 * <p>동시성: 이 클래스의 public 메서드는 모두 방({@code this})으로 동기화된다.
 * 타이머 스레드처럼 다른 스레드에서 게임 상태를 바꿀 때도 {@code synchronized (room) { ... }}
 * 안에서 해야 입찰·감정 처리와 섞이지 않는다.
 */
public final class GameRoom {

    private final List<ClientHandler> sessions = new ArrayList<>();
    private GameState state = new GameState();
    private final AuctionController auction = new AuctionController(this);
    private final InfoController info = new InfoController(this);

    // ===== 컨트롤러가 쓰는 메서드 =====

    public synchronized GameState state() {
        return state;
    }

    public AuctionController auction() {
        return auction;
    }

    public InfoController info() {
        return info;
    }

    /** 로그인한 모두에게 전송 */
    public synchronized void broadcast(Message msg) {
        for (ClientHandler s : sessions) {
            s.send(msg);
        }
    }

    /** 한 명에게만 전송 (감정 정보처럼 플레이어별로 다른 정보) */
    public synchronized void sendTo(String nickname, Message msg) {
        for (ClientHandler s : sessions) {
            if (nickname.equals(s.nickname())) {
                s.send(msg);
                return;
            }
        }
    }

    // ===== ClientHandler가 부르는 메서드 =====

    synchronized void handle(ClientHandler from, Message msg) {
        if (!from.isLoggedIn()) {
            if (Protocol.REQ_LOGIN.equals(msg.command())) {
                login(from, msg.arg(0));
            } else {
                from.send(Message.of(Protocol.ERROR, "먼저 로그인하세요"));
            }
            return;
        }

        String nickname = from.nickname();
        PlayerState player = state.findPlayer(nickname);
        try {
            switch (msg.command()) {
                case Protocol.REQ_LOGIN -> from.send(Message.of(Protocol.ERROR, "이미 로그인했습니다"));
                case Protocol.REQ_CHAT -> broadcast(Message.of(Protocol.BROADCAST_CHAT, nickname, msg.arg(0)));
                case Protocol.REQ_BID -> auction.onBid(player, msg.intArg(0));
                case Protocol.REQ_APPRAISE -> info.onAppraise(player);
                case Protocol.REQ_RESALE -> info.onResaleOffer(player, msg.intArg(0));
                case Protocol.RESALE_ANS -> info.onResaleAnswer(player, parseAnswer(msg.arg(0)));
                default -> from.send(Message.of(Protocol.ERROR, "클라이언트가 보낼 수 없는 메시지: " + msg.command()));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            from.send(Message.of(Protocol.ERROR, e.getMessage()));
        }
    }

    synchronized void leave(ClientHandler handler) {
        if (!sessions.remove(handler)) {
            return;
        }
        String nickname = handler.nickname();
        boolean wasPlaying = state.phase() != Phase.WAITING;
        if (wasPlaying) {
            auction.onGameStopped();
            info.onGameStopped();
        }
        broadcast(Message.of(Protocol.OPPONENT_LEFT, nickname));

        // 남은 사람은 대기 상태로 돌려 새 상대를 기다린다
        state = new GameState();
        for (ClientHandler s : sessions) {
            state.addPlayer(s.nickname());
        }
    }

    synchronized void closeAll() {
        for (ClientHandler s : new ArrayList<>(sessions)) {
            s.close();
        }
    }

    // ===== 내부 =====

    private void login(ClientHandler from, String rawNickname) {
        String nickname = rawNickname.trim();
        String error = validateNickname(nickname);
        if (error == null && state.isFull()) {
            error = "방이 가득 찼습니다";
        }
        if (error == null && state.findPlayer(nickname) != null) {
            error = "이미 사용 중인 닉네임입니다";
        }
        if (error != null) {
            from.send(Message.of(Protocol.LOGIN_FAIL, error));
            return;
        }

        state.addPlayer(nickname);
        from.setNickname(nickname);
        sessions.add(from);
        from.send(Message.of(Protocol.LOGIN_OK, nickname));
        AuctionServer.log("로그인: " + nickname);

        if (state.isFull()) {
            startGame();
        }
    }

    private void startGame() {
        for (ClientHandler s : sessions) {
            PlayerState opponent = state.opponentOf(s.nickname());
            s.send(Message.of(Protocol.GAME_START, opponent.nickname(), GameConfig.START_CASH, GameConfig.TOTAL_ROUNDS));
        }
        AuctionServer.log("게임 시작: " + state.players());
        // 연결: 덱 생성·라운드 진행은 파트 A가 시작한다
        auction.onGameStart();
    }

    static String validateNickname(String nickname) {
        if (nickname.isEmpty()) {
            return "닉네임을 입력하세요";
        }
        if (nickname.length() > GameConfig.MAX_NICKNAME_LENGTH) {
            return "닉네임은 " + GameConfig.MAX_NICKNAME_LENGTH + "자 이하로 입력하세요";
        }
        if (nickname.equals(Protocol.NONE) || nickname.equals(Protocol.DRAW)) {
            return "사용할 수 없는 닉네임입니다";
        }
        return null;
    }

    private static boolean parseAnswer(String answer) {
        return switch (answer) {
            case Protocol.ACCEPT -> true;
            case Protocol.REJECT -> false;
            default -> throw new IllegalArgumentException("ACCEPT 또는 REJECT만 가능: " + answer);
        };
    }
}
