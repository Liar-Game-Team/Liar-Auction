package liarauction.client.ui;

import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import liarauction.client.ServerConnection;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 메인 창. 화면 전환: 로그인 → 방 목록 → 방 → 게임 → (끝나면) 방 → (나가기) 방 목록.
 * 로그인·방 목록·방 메시지는 여기서, 나머지 게임 메시지는 {@link GamePanel}에서 처리한다.
 * 채팅과 오류는 지금 보고 있는 화면의 채팅 칸에 보여 준다.
 */
public final class MainFrame extends JFrame {

    private static final String LOGIN = "login";
    private static final String ROOM_LIST = "roomList";
    private static final String ROOM = "room";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = Theme.clear(new JPanel(cards));
    private final LoginPanel loginPanel = new LoginPanel(this::connect);
    private final RoomListPanel roomListPanel = new RoomListPanel(this::send);
    private final RoomPanel roomPanel = new RoomPanel(this::send);
    private final GamePanel gamePanel = new GamePanel(this::send);

    private ServerConnection connection;
    private String myName;
    /** 지금 보이는 화면 (채팅·오류를 어디에 보여 줄지) */
    private String screen = LOGIN;

    public MainFrame() {
        super("라이어 경매");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1100, 760);
        setMinimumSize(new java.awt.Dimension(960, 680));
        setLocationByPlatform(true);

        root.add(loginPanel, LOGIN);
        root.add(roomListPanel, ROOM_LIST);
        root.add(roomPanel, ROOM);
        root.add(gamePanel, GAME);
        HallBackground hall = new HallBackground(new java.awt.BorderLayout());
        hall.add(root, java.awt.BorderLayout.CENTER);
        setContentPane(hall);

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
                roomListPanel.enter(myName);
                show(ROOM_LIST);
            }
            case Protocol.LOGIN_FAIL -> {
                disconnect();
                loginPanel.showError(msg.arg(0));
            }
            case Protocol.ROOM_LIST -> roomListPanel.update(RoomListPanel.parse(msg.arg(0)));
            case Protocol.ROOM_JOINED -> {
                roomPanel.enter(msg.intArg(0), msg.arg(1), msg.intArg(2), msg.boolArg(3));
                show(ROOM);
            }
            case Protocol.ROOM_LEFT -> {
                gamePanel.stop();
                roomListPanel.returned();
                show(ROOM_LIST);
            }
            case Protocol.ROOM_STATE -> {
                gamePanel.stop();
                roomPanel.update(myName, msg.arg(0), splitNames(msg.arg(1)), splitNames(msg.arg(2)));
                show(ROOM);
            }
            case Protocol.GAME_START -> {
                gamePanel.startGame(myName, splitNames(msg.arg(0)), msg.intArg(1), msg.intArg(2));
                show(GAME);
            }
            case Protocol.BROADCAST_CHAT -> {
                switch (screen) {
                    case ROOM_LIST -> roomListPanel.chat().addChat(msg.arg(0), msg.arg(1));
                    case ROOM -> roomPanel.chat().addChat(msg.arg(0), msg.arg(1));
                    default -> gamePanel.handle(msg);
                }
            }
            case Protocol.ERROR -> {
                switch (screen) {
                    case ROOM_LIST -> JOptionPane.showMessageDialog(
                            this, msg.arg(0), "알림", JOptionPane.INFORMATION_MESSAGE);
                    case ROOM -> roomPanel.chat().addSystem("오류: " + msg.arg(0));
                    default -> gamePanel.handle(msg);
                }
            }
            default -> gamePanel.handle(msg);
        }
    }

    private void show(String name) {
        screen = name;
        cards.show(root, name);
    }

    private void onDisconnect() {
        connection = null;
        gamePanel.stop();
        JOptionPane.showMessageDialog(this, "서버와 연결이 끊어졌습니다.", "연결 끊김", JOptionPane.WARNING_MESSAGE);
        loginPanel.reset();
        show(LOGIN);
    }

    private void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /** 쉼표로 이은 닉네임 목록 (빈 문자열이면 빈 목록) */
    static List<String> splitNames(String joined) {
        if (joined.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(joined.split(Protocol.LIST_SEPARATOR));
    }
}
