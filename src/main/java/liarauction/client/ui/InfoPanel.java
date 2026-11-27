package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import liarauction.common.GameConfig;
import liarauction.common.Item;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;

/** Private rumors, consumable items, and final game details. */
public final class InfoPanel extends JPanel {

    private final JTextArea infoArea = new JTextArea();
    private final JLabel inventoryLabel = new JLabel();
    /** 히든 미션 (나만 보임), 받기 전에는 숨김 */
    private final JTextArea missionArea = new JTextArea();
    private final JScrollPane missionBox = new JScrollPane(missionArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final JButton certificateButton = new JButton();
    private final JButton jammerButton = new JButton();
    private int certificates;
    private int jammers;
    private int round;
    private int totalRounds = GameConfig.BASE_ROUNDS;
    private Phase phase = Phase.WAITING;

    public InfoPanel(Consumer<Message> sender) {
        super(new BorderLayout(4, 6));
        Theme.card(this, "소문 및 아이템 (나만 보임)");
        setPreferredSize(new Dimension(300, 0));

        Theme.log(infoArea);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        add(Theme.scroll(new JScrollPane(infoArea)), BorderLayout.CENTER);

        Theme.log(missionArea);
        missionArea.setForeground(Theme.GOLD_LIGHT);
        missionArea.setEditable(false);
        missionArea.setLineWrap(true);
        missionArea.setWrapStyleWord(true);
        Theme.scroll(missionBox);
        missionBox.setBorder(Theme.goldBorder("히든 미션"));
        missionBox.setPreferredSize(new Dimension(260, 96));
        missionBox.setVisible(false);
        add(missionBox, BorderLayout.NORTH);

        JPanel itemPanel = Theme.clear(new JPanel(new BorderLayout(2, 4)));
        itemPanel.setBorder(Theme.goldBorder("아이템 (작품 공개 때만)"));
        inventoryLabel.setForeground(Theme.TEXT_DIM);
        itemPanel.add(inventoryLabel, BorderLayout.NORTH);
        JPanel buttons = Theme.clear(new JPanel(new GridLayout(1, 2, 6, 0)));
        certificateButton.addActionListener(
                e -> sender.accept(Message.of(Protocol.REQ_USE_ITEM, Item.CERTIFICATE.name())));
        jammerButton.addActionListener(
                e -> sender.accept(Message.of(Protocol.REQ_USE_ITEM, Item.JAMMER.name())));
        buttons.add(certificateButton);
        buttons.add(jammerButton);
        itemPanel.add(buttons, BorderLayout.CENTER);
        add(itemPanel, BorderLayout.SOUTH);
        updateInventory();
    }

    /** {@code MISSION}: 위쪽에 계속 보이게 두고 기록에도 남긴다 */
    public void showMission(String text) {
        missionArea.setText(text);
        missionArea.setCaretPosition(0);
        missionBox.setVisible(true);
        revalidate();
        append("[히든 미션] " + text);
    }

    public void showRumor(String text) {
        append("[소문] " + text);
    }

    public void showItemResult(String item, String text) {
        String label = switch (Item.valueOf(item)) {
            case CERTIFICATE -> "감정서";
            case JAMMER -> "교란";
        };
        append("[" + label + "] " + text);
    }

    public void updateItems(int certificates, int jammers) {
        if (certificates < 0 || jammers < 0) {
            throw new IllegalArgumentException("아이템 수는 음수일 수 없습니다");
        }
        this.certificates = certificates;
        this.jammers = jammers;
        updateInventory();
    }

    public void setRound(int round) {
        this.round = round;
        updateInventory();
    }

    public void setTotalRounds(int totalRounds) {
        if (totalRounds < 1) {
            throw new IllegalArgumentException("총 라운드는 양수여야 합니다");
        }
        this.totalRounds = totalRounds;
        updateInventory();
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        updateInventory();
    }

    public void addLine(String text) {
        append(text);
    }

    public void clear() {
        infoArea.setText("");
        missionArea.setText("");
        missionBox.setVisible(false);
        certificates = 0;
        jammers = 0;
        round = 0;
        phase = Phase.WAITING;
        updateInventory();
    }

    private void updateInventory() {
        inventoryLabel.setText("감정서 " + certificates + " · 교란 " + jammers);
        boolean revealPhase = phase == Phase.REVEAL;
        certificateButton.setText("감정서 (" + certificates + ")");
        certificateButton.setEnabled(revealPhase && certificates > 0);
        jammerButton.setText("교란 (" + jammers + ")");
        jammerButton.setEnabled(
                revealPhase && jammers > 0 && round > 0 && round < totalRounds);
    }

    private void append(String line) {
        infoArea.append(line + "\n");
        infoArea.setCaretPosition(infoArea.getDocument().getLength());
    }
}
