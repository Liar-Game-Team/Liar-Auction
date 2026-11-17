package liarauction.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 클라이언트 한 명을 맡는 스레드. 받은 줄을 {@link Message}로 바꿔 {@link GameRoom}에 넘긴다.
 */
public final class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameRoom room;
    private final BufferedReader in;
    private final PrintWriter out;
    /** 로그인 전에는 null */
    private volatile String nickname;

    ClientHandler(Socket socket, GameRoom room) throws IOException {
        this.socket = socket;
        this.room = room;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg;
                try {
                    msg = Message.parse(line);
                } catch (IllegalArgumentException e) {
                    send(Message.of(Protocol.ERROR, e.getMessage()));
                    continue;
                }
                room.handle(this, msg);
            }
        } catch (IOException e) {
            // 접속 끊김
        } finally {
            AuctionServer.log("접속 종료: " + (nickname != null ? nickname : socket.getRemoteSocketAddress()));
            room.leave(this);
            close();
        }
    }

    /** 메시지 전송. 여러 스레드에서 불러도 줄이 섞이지 않게 동기화한다. */
    public synchronized void send(Message msg) {
        out.println(msg.encode());
    }

    public String nickname() {
        return nickname;
    }

    void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isLoggedIn() {
        return nickname != null;
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 무시
        }
    }
}
