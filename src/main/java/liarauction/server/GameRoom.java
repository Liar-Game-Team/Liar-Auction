package liarauction.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Item;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import liarauction.server.auction.AuctionController;
import liarauction.server.info.InfoController;

/**
 * 게임 방 하나 (2~6명). 방 대기(준비·시작)·채팅·퇴장을 처리하고 게임 메시지는 파트별 컨트롤러로 넘긴다.
 * <ul>
 *   <li>파트 A (경매 진행): {@link AuctionController}</li>
 *   <li>파트 B (정보·거래): {@link InfoController}</li>
 * </ul>
 * 방 목록·로그인은 {@link Lobby}가 맡는다.
 *
 * <p>방 규칙: 방을 만든 사람이 방장 (방장이 나가면 다음 사람). 방장을 뺀 나머지가 모두 준비하면 방장이 시작한다.
 * 게임 중 누가 나가면 남은 사람끼리 계속하고, 2명 미만이 되면 방 대기 상태로 돌아간다.
 * 게임이 끝나도 같은 사람들로 방 대기 상태로 돌아간다. 아무도 없으면 방은 닫힌다.
 *
 * <p>동시성: 방 상태는 {@code this}로 동기화한다. 입찰 판정은 별도 동기화 경로로 처리해
 * 타이머의 낙찰 콜백과 lock 순서가 뒤집히지 않게 한다. 컨트롤러를 멈추는 호출과
 * {@link Lobby}에 알리는 호출은 방 lock 밖에서 한다.
 */
public final class GameRoom {

    /** 방 목록 한 줄 */
    public record Summary(int id, String name, int count, int maxPlayers, boolean locked, boolean playing) {
    }

    private final int id;
    private final String name;
    private final int maxPlayers;
    /** 빈 문자열이면 공개방 */
    private final String password;
    private final Lobby lobby;

    /** 입장 순서. 첫 번째가 방장 */
    private final List<ClientHandler> sessions = new ArrayList<>();
    /** 방에서 준비한 사람 */
    private final Set<String> ready = new LinkedHashSet<>();
    private GameState state = new GameState();
    private final AuctionController auction;
    private final InfoController info;
    /** 게임을 멈추고 방 대기 상태로 돌리는 중 (그동안 입장·시작 거부) */
    private boolean resetting;
    /** 방금 게임이 시작돼 Lobby에 알려야 함 (방 lock 밖에서 알림) */
    private boolean startedSinceLastNotify;
    /** 마지막 사람이 나가 닫힌 방 (다시 들어올 수 없음) */
    private boolean closed;

    GameRoom(int id, String name, int maxPlayers, String password, AuctionTiming timing, Lobby lobby) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.maxPlayers = maxPlayers;
        this.password = Objects.requireNonNull(password, "password");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        auction = new AuctionController(this, timing);
        info = new InfoController(this);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public boolean isLocked() {
        return !password.isEmpty();
    }

    public synchronized boolean isEmpty() {
        return closed;
    }

    public synchronized Summary summary() {
        return new Summary(id, name, sessions.size(), maxPlayers, isLocked(), inGame() || resetting);
    }

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

    /** 방 안의 모두에게 전송 */
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

    /** 방장 닉네임, 아무도 없으면 null */
    public synchronized String host() {
        return sessions.isEmpty() ? null : sessions.get(0).nickname();
    }

    /**
     * 게임이 끝난 뒤 같은 사람들로 방 대기 상태로 돌아간다 (파트 A가 게임 종료 처리 마지막에 부름).
     * 방 lock을 잡지 않은 상태에서 불러야 한다.
     */
    public void returnToWaitingRoom() {
        synchronized (this) {
            if (state.phase() != Phase.GAME_OVER || resetting) {
                return;
            }
            resetting = true;
        }
        stopControllers();
        synchronized (this) {
            resetToWaiting();
            resetting = false;
        }
        lobby.roomChanged();
    }

    // ===== Lobby가 부르는 메서드 =====

    /**
     * 방에 들어간다. 성공하면 {@code ROOM_JOINED}와 {@code ROOM_STATE}를 보낸다.
     *
     * @return 실패 이유, 성공하면 null
     */
    synchronized String join(ClientHandler handler, String givenPassword) {
        if (closed) {
            return "없는 방입니다";
        }
        if (inGame() || resetting) {
            return "게임 중인 방입니다";
        }
        if (sessions.size() >= maxPlayers) {
            return "방이 가득 찼습니다 (최대 " + maxPlayers + "명)";
        }
        if (isLocked() && !password.equals(givenPassword)) {
            return "비밀번호가 틀렸습니다";
        }
        state.addPlayer(handler.nickname());
        sessions.add(handler);
        handler.setRoom(this);
        handler.send(Message.of(Protocol.ROOM_JOINED, id, name, maxPlayers, isLocked()));
        AuctionServer.log("방 입장: #" + id + " " + handler.nickname() + " (" + sessions.size() + "/" + maxPlayers + ")");
        broadcastRoomState();
        return null;
    }

    void handle(ClientHandler from, Message msg) {
        if (Protocol.REQ_BID.equals(msg.command())) {
            PlayerState player;
            synchronized (this) {
                player = state.findPlayer(from.nickname());
            }
            try {
                auction.onBid(player, msg.intArg(0));
            } catch (IllegalArgumentException | IllegalStateException e) {
                from.send(Message.of(Protocol.ERROR, e.getMessage()));
            }
            return;
        }

        boolean started;
        synchronized (this) {
            handleLocked(from, msg);
            started = startedSinceLastNotify;
            startedSinceLastNotify = false;
        }
        if (started) {
            lobby.roomChanged();
        }
    }

    /** 방을 나간다 (방 나가기 또는 접속 끊김). 마지막 사람이 나가면 방이 닫힌다. */
    void leave(ClientHandler handler) {
        String nickname;
        synchronized (this) {
            if (!sessions.remove(handler)) {
                return;
            }
            nickname = handler.nickname();
            ready.remove(nickname);
            state.removePlayer(nickname);
            if (sessions.isEmpty()) {
                closed = true;
            }
            if (!inGame()) {
                broadcastRoomState();
                return;
            }
            // 게임 중: 나간 사람만 빼고 계속, 2명 미만이면 방 대기 상태로
            broadcast(Message.of(Protocol.PLAYER_LEFT, nickname));
            AuctionServer.log("게임 중 퇴장: #" + id + " " + nickname + ", 남은 인원 " + sessions.size());
            if (sessions.size() >= GameConfig.MIN_PLAYERS || resetting) {
                return;
            }
            resetting = true;
        }
        stopControllers();
        synchronized (this) {
            resetToWaiting();
            resetting = false;
        }
    }

    // ===== 내부 (방 lock 안에서 호출) =====

    private void handleLocked(ClientHandler from, Message msg) {
        String nickname = from.nickname();
        PlayerState player = state.findPlayer(nickname);
        try {
            switch (msg.command()) {
                case Protocol.REQ_LOGIN -> from.send(Message.of(Protocol.ERROR, "이미 로그인했습니다"));
                case Protocol.REQ_CHAT -> broadcast(Message.of(Protocol.BROADCAST_CHAT, nickname, msg.arg(0)));
                case Protocol.REQ_READY -> setReady(nickname, msg.boolArg(0));
                case Protocol.REQ_START -> requestStart(nickname);
                case Protocol.REQ_USE_ITEM -> info.onUseItem(player, Item.parse(msg.arg(0)));
                case Protocol.REQ_ROOM_LIST, Protocol.REQ_CREATE_ROOM, Protocol.REQ_JOIN_ROOM,
                        Protocol.REQ_QUICK_JOIN ->
                        from.send(Message.of(Protocol.ERROR, "방에서 나간 뒤에 할 수 있습니다"));
                default -> from.send(Message.of(Protocol.ERROR, "클라이언트가 보낼 수 없는 메시지: " + msg.command()));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            from.send(Message.of(Protocol.ERROR, e.getMessage()));
        }
    }

    private void setReady(String nickname, boolean isReady) {
        if (inGame() || resetting) {
            throw new IllegalStateException("게임 시작 전에만 준비할 수 있습니다");
        }
        if (isReady) {
            ready.add(nickname);
        } else {
            ready.remove(nickname);
        }
        broadcastRoomState();
    }

    private void requestStart(String nickname) {
        if (inGame() || resetting) {
            throw new IllegalStateException("이미 게임이 진행 중입니다");
        }
        if (!nickname.equals(host())) {
            throw new IllegalStateException("방장만 시작할 수 있습니다");
        }
        if (!state.hasEnoughPlayers()) {
            throw new IllegalStateException(GameConfig.MIN_PLAYERS + "명 이상 모여야 시작할 수 있습니다");
        }
        List<String> notReady = new ArrayList<>();
        for (String name : nicknames()) {
            if (!name.equals(nickname) && !ready.contains(name)) {
                notReady.add(name);
            }
        }
        if (!notReady.isEmpty()) {
            throw new IllegalStateException("아직 준비하지 않은 사람: " + String.join(", ", notReady));
        }
        startGame();
    }

    private void startGame() {
        List<String> names = nicknames();
        int totalRounds = GameConfig.roundsFor(names.size());
        broadcast(Message.of(Protocol.GAME_START,
                String.join(Protocol.LIST_SEPARATOR, names), GameConfig.START_CASH, totalRounds));
        AuctionServer.log("게임 시작: #" + id + " " + state.players() + ", " + totalRounds + "라운드");
        ready.clear();
        startedSinceLastNotify = true;
        // 연결: 덱 생성·라운드 진행은 파트 A가 시작한다
        auction.onGameStart();
    }

    /** 같은 사람들로 새 방 대기 상태를 만든다. 준비는 다시 해야 한다. */
    private void resetToWaiting() {
        state = new GameState();
        for (ClientHandler session : sessions) {
            state.addPlayer(session.nickname());
        }
        ready.clear();
        broadcastRoomState();
    }

    private void broadcastRoomState() {
        String host = host();
        List<String> readyInOrder = new ArrayList<>();
        for (String n : nicknames()) {
            if (ready.contains(n)) {
                readyInOrder.add(n);
            }
        }
        broadcast(Message.of(Protocol.ROOM_STATE,
                host == null ? "" : host,
                String.join(Protocol.LIST_SEPARATOR, nicknames()),
                String.join(Protocol.LIST_SEPARATOR, readyInOrder)));
    }

    private List<String> nicknames() {
        List<String> names = new ArrayList<>(sessions.size());
        for (ClientHandler s : sessions) {
            names.add(s.nickname());
        }
        return names;
    }

    private boolean inGame() {
        return state.phase() != Phase.WAITING;
    }

    /** 타이머·입찰·정보 컨트롤러를 멈춘다. 방 lock 밖에서 부른다. */
    private void stopControllers() {
        auction.onGameStopped();
        info.onGameStopped();
    }
}
