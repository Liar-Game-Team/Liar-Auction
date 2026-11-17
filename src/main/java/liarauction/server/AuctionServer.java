package liarauction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * 멀티스레드 소켓 서버. 접속마다 {@link ClientHandler} 스레드를 하나씩 만든다.
 * 1:1 게임이라 방은 {@link GameRoom} 하나만 둔다.
 */
public final class AuctionServer {

    private final int port;
    private final GameRoom room = new GameRoom();
    private ServerSocket serverSocket;
    private Thread acceptThread;

    /** @param port 0이면 비어 있는 포트를 자동으로 고른다 (테스트용) */
    public AuctionServer(int port) {
        this.port = port;
    }

    /** 포트를 열고 접속 받기를 시작한다 (바로 반환). */
    public synchronized void start() throws IOException {
        if (serverSocket != null) {
            throw new IllegalStateException("이미 시작됨");
        }
        serverSocket = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "accept");
        acceptThread.start();
        log("서버 시작, 포트 " + getPort());
    }

    /** 실제로 열린 포트 */
    public synchronized int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    public GameRoom room() {
        return room;
    }

    /** 서버 종료, 접속 중인 클라이언트도 끊는다. */
    public void stop() {
        ServerSocket ss;
        synchronized (this) {
            ss = serverSocket;
        }
        if (ss == null) {
            return;
        }
        try {
            ss.close();
        } catch (IOException ignored) {
            // 닫는 중 오류는 무시
        }
        room.closeAll();
    }

    /** {@link #stop()}이 불릴 때까지 기다린다. */
    public void awaitTermination() throws InterruptedException {
        Thread t;
        synchronized (this) {
            t = acceptThread;
        }
        if (t != null) {
            t.join();
        }
    }

    private void acceptLoop() {
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                log("서버 종료");
                return;
            } catch (IOException e) {
                log("접속 받기 오류: " + e.getMessage());
                continue;
            }
            log("접속: " + socket.getRemoteSocketAddress());
            try {
                ClientHandler handler = new ClientHandler(socket, room);
                Thread t = new Thread(handler, "client-" + socket.getPort());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                log("클라이언트 준비 실패: " + e.getMessage());
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // 무시
                }
            }
        }
    }

    static void log(String msg) {
        System.out.println("[서버] " + msg);
    }
}
