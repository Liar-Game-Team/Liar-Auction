package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 방 대기 화면: 방 이름, 참가자, 방장, 준비 상태, 방 채팅. 방장은 시작, 나머지는 준비.
 * 기본 동작만 있는 화면이고 꾸미기·캐릭터는 #46.
 */
public final class RoomPanel extends JPanel {

    private final JLabel titleLabel = new JLabel("방", SwingConstants.CENTER);
    private final DefaultListModel<String> playerModel = new DefaultListModel<>();
    private final JScrollPane playerScroll;
    private final JLabel hintLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton readyButton = new JButton("준비");
    private final JButton startButton = new JButton("게임 시작");
    private final JButton leaveButton = new JButton("방 나가기");
    private final ChatPanel chat;

    private int maxPlayers = GameConfig.MAX_PLAYERS;
    private boolean amHost;
    private boolean amReady;

    public RoomPanel(Consumer<Message> sender) {
        super(new BorderLayout(8, 8));
        Theme.clear(this);
        setBorder(BorderFactory.createEmptyBorder(70, 60, 16, 60));

        Theme.title(titleLabel);
        add(titleLabel, BorderLayout.NORTH);

        JList<String> playerList = new JList<>(playerModel);
        playerList.setFont(Theme.BODY.deriveFont(17f));
        playerList.setBackground(Theme.FIELD);
        playerList.setForeground(Theme.TEXT);
        playerList.setFixedCellHeight(34);
        playerScroll = Theme.scroll(new JScrollPane(playerList));
        Theme.hint(hintLabel);
        hintLabel.setFont(Theme.BODY);
        hintLabel.setForeground(Theme.GOLD_LIGHT);

        JPanel center = Theme.clear(new JPanel(new BorderLayout(4, 8)));
        center.add(playerScroll, BorderLayout.CENTER);
        center.add(hintLabel, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        readyButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_READY, !amReady)));
        startButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_START)));
        leaveButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_LEAVE_ROOM)));
        JPanel buttons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4)));
        buttons.add(readyButton);
        buttons.add(startButton);
        buttons.add(leaveButton);

        chat = new ChatPanel(sender, "방 채팅");
        JPanel south = Theme.clear(new JPanel(new BorderLayout(4, 4)));
        south.add(buttons, BorderLayout.NORTH);
        south.add(chat, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    /** {@code ROOM_JOINED|roomId|name|maxPlayers|locked} */
    public void enter(int roomId, String name, int maxPlayers, boolean locked) {
        this.maxPlayers = maxPlayers;
        titleLabel.setText((locked ? "[비밀방] " : "") + roomId + "번 방 - " + name);
        chat.clear();
        chat.addSystem(name + " 방에 들어왔습니다");
        playerModel.clear();
        hintLabel.setText(" ");
        readyButton.setVisible(false);
        startButton.setVisible(false);
    }

    /** {@code ROOM_STATE|host|players|readyPlayers} */
    public void update(String myName, String host, List<String> players, List<String> readyPlayers) {
        amHost = myName.equals(host);
        amReady = readyPlayers.contains(myName);

        playerScroll.setBorder(Theme.goldBorder("참가자 " + players.size() + " / " + maxPlayers + "명"));
        playerModel.clear();
        int notReady = 0;
        for (String name : players) {
            boolean isHost = name.equals(host);
            boolean isReady = readyPlayers.contains(name);
            if (!isHost && !isReady) {
                notReady++;
            }
            String status = isHost ? "방장" : (isReady ? "준비 완료" : "대기 중");
            playerModel.addElement(name + (name.equals(myName) ? " (나)" : "") + "  -  " + status);
        }

        boolean enough = players.size() >= GameConfig.MIN_PLAYERS;
        readyButton.setVisible(!amHost);
        readyButton.setText(amReady ? "준비 취소" : "준비");
        startButton.setVisible(amHost);
        startButton.setEnabled(amHost && enough && notReady == 0);

        if (!enough) {
            hintLabel.setText(GameConfig.MIN_PLAYERS + "명 이상 모이면 시작할 수 있습니다");
        } else if (notReady > 0) {
            hintLabel.setText(amHost
                    ? "모두 준비하면 시작할 수 있습니다 (" + notReady + "명 대기 중)"
                    : "모두 준비하면 방장이 시작합니다");
        } else {
            hintLabel.setText(amHost ? "모두 준비했습니다. 게임을 시작하세요" : "방장이 시작하기를 기다리는 중...");
        }
    }

    public ChatPanel chat() {
        return chat;
    }
}
