package liarauction.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 서버 전체: 로그인한 사람과 방 목록. 방 안의 일(대기·게임)은 각 {@link GameRoom}이 맡는다.
 *
 * <p>흐름: 로그인 → 방 목록 화면 (방 만들기 / 들어가기 / 빠른 입장, 방 목록 채팅)
 * → 방 (준비·시작·게임) → 방 나가기 → 방 목록 화면.
 *
 * <p>동시성: {@code this} lock은 사람·방 목록(맵)을 바꿀 때만 짧게 잡는다.
 * 방 lock을 잡은 채로 이 클래스를 부르지 않고, 이 lock을 잡은 채로 방을 부르지 않는다
 * (방 목록을 만들 때도 목록을 복사한 뒤 lock 밖에서 각 방에 묻는다).
 */
public final class Lobby {

    private final AuctionTiming timing;
    /** 로그인한 모든 사람 (닉네임 → 접속) */
    private final Map<String, ClientHandler> users = new LinkedHashMap<>();
    /** 방 번호 → 방 (만든 순서) */
    private final Map<Integer, GameRoom> rooms = new LinkedHashMap<>();
    private int nextRoomId = 1;

    Lobby(AuctionTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    /** 방 번호로 찾기, 없으면 null */
    public synchronized GameRoom room(int id) {
        return rooms.get(id);
    }

    /** 지금 있는 방들 (만든 순서) */
    public synchronized List<GameRoom> rooms() {
        return List.copyOf(rooms.values());
    }

    // ===== ClientHandler가 부르는 메서드 =====

    void handle(ClientHandler from, Message msg) {
        if (!from.isLoggedIn()) {
            if (Protocol.REQ_LOGIN.equals(msg.command())) {
                login(from, msg.arg(0));
            } else {
                from.send(Message.of(Protocol.ERROR, "먼저 로그인하세요"));
            }
            return;
        }

        GameRoom room = from.room();
        if (room != null) {
            if (Protocol.REQ_LEAVE_ROOM.equals(msg.command())) {
                leaveRoom(from, false);
            } else {
                room.handle(from, msg);
            }
            return;
        }

        try {
            switch (msg.command()) {
                case Protocol.REQ_LOGIN -> from.send(Message.of(Protocol.ERROR, "이미 로그인했습니다"));
                case Protocol.REQ_CHAT -> lobbyChat(from.nickname(), msg.arg(0));
                case Protocol.REQ_ROOM_LIST -> from.send(roomListMessage());
                case Protocol.REQ_CREATE_ROOM -> createRoom(from, msg.arg(0), msg.intArg(1), msg.arg(2));
                case Protocol.REQ_JOIN_ROOM -> joinRoom(from, msg.intArg(0), msg.arg(1));
                case Protocol.REQ_QUICK_JOIN -> quickJoin(from);
                case Protocol.REQ_LEAVE_ROOM, Protocol.REQ_READY, Protocol.REQ_START,
                        Protocol.REQ_BID, Protocol.REQ_USE_ITEM ->
                        from.send(Message.of(Protocol.ERROR, "방에 들어가야 할 수 있습니다"));
                default -> from.send(Message.of(Protocol.ERROR, "클라이언트가 보낼 수 없는 메시지: " + msg.command()));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            from.send(Message.of(Protocol.ERROR, e.getMessage()));
        }
    }

    /** 접속이 끊겼을 때 */
    void disconnect(ClientHandler handler) {
        if (handler.room() != null) {
            leaveRoom(handler, true);
        }
        synchronized (this) {
            if (handler.isLoggedIn()) {
                users.remove(handler.nickname(), handler);
            }
        }
    }

    /** 서버 종료 시 모든 접속을 끊는다 */
    void closeAll() {
        List<ClientHandler> all;
        synchronized (this) {
            all = new ArrayList<>(users.values());
        }
        for (ClientHandler h : all) {
            h.close();
        }
    }

    // ===== GameRoom이 부르는 메서드 (방 lock 밖에서) =====

    /** 방 상태가 바뀌었을 때 (게임 시작·종료 등) 방 목록을 다시 보낸다 */
    void roomChanged() {
        broadcastRoomList();
    }

    // ===== 내부 =====

    private void login(ClientHandler from, String rawNickname) {
        String nickname = rawNickname.trim();
        String error = validateNickname(nickname);
        synchronized (this) {
            if (error == null && users.containsKey(nickname)) {
                error = "이미 사용 중인 닉네임입니다";
            }
            if (error == null) {
                from.setNickname(nickname);
                users.put(nickname, from);
            }
        }
        if (error != null) {
            from.send(Message.of(Protocol.LOGIN_FAIL, error));
            return;
        }
        from.send(Message.of(Protocol.LOGIN_OK, nickname));
        from.send(roomListMessage());
        AuctionServer.log("로그인: " + nickname);
    }

    private void createRoom(ClientHandler from, String rawName, int maxPlayers, String password) {
        String name = rawName.trim();
        String error = validateRoomName(name);
        if (error == null && (maxPlayers < GameConfig.MIN_PLAYERS || maxPlayers > GameConfig.MAX_PLAYERS)) {
            error = "최대 인원은 " + GameConfig.MIN_PLAYERS + "~" + GameConfig.MAX_PLAYERS + "명입니다";
        }
        if (error == null && password.length() > GameConfig.MAX_PASSWORD_LENGTH) {
            error = "비밀번호는 " + GameConfig.MAX_PASSWORD_LENGTH + "자 이하로 입력하세요";
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        GameRoom room;
        synchronized (this) {
            int id = nextRoomId++;
            room = new GameRoom(id, name, maxPlayers, password, timing, this);
            rooms.put(id, room);
        }
        AuctionServer.log("방 만들기: #" + room.id() + " " + name + " (최대 " + maxPlayers + "명"
                + (password.isEmpty() ? "" : ", 비밀방") + ") by " + from.nickname());
        String joinError = room.join(from, password);
        if (joinError != null) {
            // 만들자마자 들어가지 못하는 경우는 없지만, 생기면 빈 방을 지운다
            removeIfEmpty(room);
            throw new IllegalStateException(joinError);
        }
        broadcastRoomList();
    }

    private void joinRoom(ClientHandler from, int roomId, String password) {
        GameRoom room = room(roomId);
        if (room == null) {
            throw new IllegalStateException("없는 방입니다");
        }
        String error = room.join(from, password);
        if (error != null) {
            throw new IllegalStateException(error);
        }
        broadcastRoomList();
    }

    private void quickJoin(ClientHandler from) {
        for (GameRoom room : rooms()) {
            if (!room.isLocked() && room.join(from, "") == null) {
                broadcastRoomList();
                return;
            }
        }
        throw new IllegalStateException("바로 들어갈 수 있는 방이 없습니다. 방을 만들어 보세요");
    }

    private void leaveRoom(ClientHandler handler, boolean disconnected) {
        GameRoom room = handler.room();
        if (room == null) {
            return;
        }
        room.leave(handler);
        handler.setRoom(null);
        removeIfEmpty(room);
        if (!disconnected) {
            handler.send(Message.of(Protocol.ROOM_LEFT));
        }
        broadcastRoomList();
    }

    private void removeIfEmpty(GameRoom room) {
        if (room.isEmpty()) {
            synchronized (this) {
                rooms.remove(room.id(), room);
            }
            AuctionServer.log("방 삭제: #" + room.id() + " " + room.name());
        }
    }

    private void lobbyChat(String sender, String text) {
        Message chat = Message.of(Protocol.BROADCAST_CHAT, sender, text);
        for (ClientHandler h : usersInLobby()) {
            h.send(chat);
        }
    }

    /** 방 목록 화면에 있는 사람 모두에게 방 목록 */
    private void broadcastRoomList() {
        Message list = roomListMessage();
        for (ClientHandler h : usersInLobby()) {
            h.send(list);
        }
    }

    private synchronized List<ClientHandler> usersInLobby() {
        List<ClientHandler> out = new ArrayList<>();
        for (ClientHandler h : users.values()) {
            if (h.room() == null) {
                out.add(h);
            }
        }
        return out;
    }

    /** 방 목록 메시지. 방 목록을 복사한 뒤 lock 밖에서 각 방 상태를 읽는다 */
    private Message roomListMessage() {
        List<String> entries = new ArrayList<>();
        for (GameRoom room : rooms()) {
            GameRoom.Summary s = room.summary();
            if (s.count() == 0) {
                continue;
            }
            entries.add(String.join(Protocol.LIST_SEPARATOR,
                    String.valueOf(s.id()), s.name(), String.valueOf(s.count()),
                    String.valueOf(s.maxPlayers()), String.valueOf(s.locked()), String.valueOf(s.playing())));
        }
        return Message.of(Protocol.ROOM_LIST, String.join(Protocol.ROOM_SEPARATOR, entries));
    }

    static String validateNickname(String nickname) {
        if (nickname.isEmpty()) {
            return "닉네임을 입력하세요";
        }
        if (nickname.length() > GameConfig.MAX_NICKNAME_LENGTH) {
            return "닉네임은 " + GameConfig.MAX_NICKNAME_LENGTH + "자 이하로 입력하세요";
        }
        if (nickname.contains(Protocol.LIST_SEPARATOR) || nickname.contains(Protocol.ROOM_SEPARATOR)) {
            return "닉네임에 쉼표(,)나 세미콜론(;)은 쓸 수 없습니다";
        }
        if (nickname.equals(Protocol.NONE) || nickname.equals(Protocol.DRAW)) {
            return "사용할 수 없는 닉네임입니다";
        }
        return null;
    }

    static String validateRoomName(String name) {
        if (name.isEmpty()) {
            return "방 이름을 입력하세요";
        }
        if (name.length() > GameConfig.MAX_ROOM_NAME_LENGTH) {
            return "방 이름은 " + GameConfig.MAX_ROOM_NAME_LENGTH + "자 이하로 입력하세요";
        }
        if (name.contains(Protocol.LIST_SEPARATOR) || name.contains(Protocol.ROOM_SEPARATOR)) {
            return "방 이름에 쉼표(,)나 세미콜론(;)은 쓸 수 없습니다";
        }
        return null;
    }
}
