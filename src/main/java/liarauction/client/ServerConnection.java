package liarauction.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import liarauction.common.Message;

/**
 * 서버 연결. 받는 스레드를 따로 두고, 받은 메시지는 Swing 스레드(EDT)에서 리스너로 넘긴다.
 * 그래서 리스너 안에서는 바로 화면을 바꿔도 된다.
 */
public final class ServerConnection {

    private static final int CONNECT_TIMEOUT_MS = 3000;

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile boolean closed;

    private ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
    }

    /**
     * 접속하고 받기 스레드를 시작한다.
     *
     * @param onMessage 메시지를 받을 때 (EDT)
     * @param onDisconnect 서버와 연결이 끊겼을 때 (EDT), 직접 {@link #close()}한 경우는 부르지 않음
     */
    public static ServerConnection connect(String host, int port,
                                           Consumer<Message> onMessage, Runnable onDisconnect) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        ServerConnection conn = new ServerConnection(socket);
        Thread reader = new Thread(() -> conn.readLoop(onMessage, onDisconnect), "server-reader");
        reader.setDaemon(true);
        reader.start();
        return conn;
    }

    public synchronized void send(Message msg) {
        if (!closed) {
            out.println(msg.encode());
        }
    }

    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // 무시
        }
    }

    private void readLoop(Consumer<Message> onMessage, Runnable onDisconnect) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg;
                try {
                    msg = Message.parse(line);
                } catch (IllegalArgumentException e) {
                    System.err.println("[클라이언트] 잘못된 메시지 무시: " + line + " (" + e.getMessage() + ")");
                    continue;
                }
                SwingUtilities.invokeLater(() -> onMessage.accept(msg));
            }
        } catch (IOException e) {
            // 연결 끊김
        }
        if (!closed) {
            closed = true;
            SwingUtilities.invokeLater(onDisconnect);
        }
    }
}
