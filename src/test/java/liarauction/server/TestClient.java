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
