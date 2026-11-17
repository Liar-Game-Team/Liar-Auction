package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import liarauction.client.ServerConnection;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 메인 창. 화면 전환: 로그인 → 대기 → 게임.
 * 로그인·접속 관련 메시지는 여기서, 나머지 게임 메시지는 {@link GamePanel}에서 처리한다.
 */
public final class MainFrame extends JFrame {

    private static final String LOGIN = "login";
    private static final String WAITING = "waiting";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel loginPanel = new LoginPanel(this::connect);
    private final JLabel waitingLabel = new JLabel("", SwingConstants.CENTER);
    private final GamePanel gamePanel = new GamePanel(this::send);

    private ServerConnection connection;
    private String myName;

    public MainFrame() {
        super("라이어 경매");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 640);
        setLocationByPlatform(true);

        waitingLabel.setFont(waitingLabel.getFont().deriveFont(Font.PLAIN, 20f));
        JPanel waitingPanel = new JPanel(new BorderLayout());
        waitingPanel.add(waitingLabel, BorderLayout.CENTER);

        root.add(loginPanel, LOGIN);
        root.add(waitingPanel, WAITING);
        root.add(gamePanel, GAME);
        setContentPane(root);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void connect(String host, int port, String nickname) {
        // 접속은 시간이 걸릴 수 있어 Swing 스레드 밖에서
        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.connect(host, port, this::onMessage, this::onDisconnect);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    connection = conn;
                    send(Message.of(Protocol.REQ_LOGIN, nickname));
                });
            } catch (IOException e) {
                javax.swing.SwingUtilities.invokeLater(
                        () -> loginPanel.showError("접속 실패: " + e.getMessage()));
            }
        }, "connect").start();
    }

    private void send(Message msg) {
        if (connection != null) {
            connection.send(msg);
        }
    }

    private void onMessage(Message msg) {
        switch (msg.command()) {
            case Protocol.LOGIN_OK -> {
                myName = msg.arg(0);
                showWaiting(myName + " 님, 상대를 기다리는 중...");
            }
            case Protocol.LOGIN_FAIL -> {
                disconnect();
                loginPanel.showError(msg.arg(0));
            }
            case Protocol.GAME_START -> {
                gamePanel.startGame(myName, msg.arg(0), msg.intArg(1), msg.intArg(2));
                cards.show(root, GAME);
            }
            case Protocol.OPPONENT_LEFT -> {
                gamePanel.stop();
                showWaiting(msg.arg(0) + " 님이 나갔습니다. 새 상대를 기다리는 중...");
            }
            default -> gamePanel.handle(msg);
        }
    }

    private void onDisconnect() {
        connection = null;
        gamePanel.stop();
        JOptionPane.showMessageDialog(this, "서버와 연결이 끊어졌습니다.", "연결 끊김", JOptionPane.WARNING_MESSAGE);
        loginPanel.reset();
        cards.show(root, LOGIN);
    }

    private void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private void showWaiting(String text) {
        waitingLabel.setText(text);
        cards.show(root, WAITING);
    }
}
