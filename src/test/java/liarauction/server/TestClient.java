package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import liarauction.common.Message;
import liarauction.common.Protocol;

/** 테스트용 소켓 클라이언트 */
public final class TestClient implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public TestClient(int port) throws IOException {
        this.socket = new Socket("localhost", port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public void send(Message msg) {
        sendRaw(msg.encode());
    }

    public void sendRaw(String line) {
        out.println(line);
    }

    public Message read() throws IOException {
        String line = in.readLine();
        assertNotNull(line, "연결이 끊김");
        return Message.parse(line);
    }

    /** 해당 메시지가 올 때까지 읽고, 그 메시지를 돌려준다 (중간 메시지는 버림). */
    public Message readUntil(String command) throws IOException {
        Message message;
        do {
            message = read();
        } while (!command.equals(message.command()));
        return message;
    }

    /** 공개방을 만들고 (최대 6명) 들어간 방 번호를 돌려준다. */
    public int createRoom(String name) throws IOException {
        return createRoom(name, 6, "");
    }

    /** 방을 만들고 ROOM_JOINED까지 읽어 방 번호를 돌려준다. */
    public int createRoom(String name, int maxPlayers, String password) throws IOException {
        send(Message.of(Protocol.REQ_CREATE_ROOM, name, maxPlayers, password));
        return readUntil(Protocol.ROOM_JOINED).intArg(0);
    }

    /** 방에 들어가고 ROOM_JOINED까지 읽는다. */
    public TestClient joinRoom(int roomId) throws IOException {
        return joinRoom(roomId, "");
    }

    public TestClient joinRoom(int roomId, String password) throws IOException {
        send(Message.of(Protocol.REQ_JOIN_ROOM, roomId, password));
        readUntil(Protocol.ROOM_JOINED);
        return this;
    }

    /**
     * 방에서 게임 시작: 나머지가 준비하고 방장이 시작한 뒤, 모두 GAME_START까지 읽는다.
     *
     * @return 방장이 받은 GAME_START
     */
    public static Message startGame(TestClient host, TestClient... others) throws IOException {
        for (TestClient other : others) {
            other.send(Message.of(Protocol.REQ_READY, true));
        }
        // 준비 요청은 다른 소켓·스레드로 처리되므로, 방장이 모두 준비된 ROOM_STATE를 받은 뒤 시작한다
        if (others.length > 0) {
            Message lobby;
            do {
                lobby = host.readUntil(Protocol.ROOM_STATE);
            } while (lobby.arg(2).isEmpty() || lobby.arg(2).split(",").length < others.length);
        }
        host.send(Message.of(Protocol.REQ_START));
        Message start = host.readUntil(Protocol.GAME_START);
        for (TestClient other : others) {
            other.readUntil(Protocol.GAME_START);
        }
        return start;
    }

    /** 로그인하고 LOGIN_OK까지 읽는다. */
    public TestClient login(String nickname) throws IOException {
        send(Message.of(Protocol.REQ_LOGIN, nickname));
        Message reply = read();
        if (!Protocol.LOGIN_OK.equals(reply.command())) {
            throw new AssertionError("로그인 실패: " + reply);
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
