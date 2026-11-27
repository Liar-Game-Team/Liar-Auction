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
    private final JButton certificateButton = new JButton();
    private final JButton jammerButton = new JButton();
    private int certificates;
    private int jammers;
    private int round;
    private int totalRounds = GameConfig.BASE_ROUNDS;
    private Phase phase = Phase.WAITING;

    public InfoPanel(Consumer<Message> sender) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("소문 및 아이템"));
        setPreferredSize(new Dimension(260, 0));

        infoArea.setFont(certificateButton.getFont());
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        add(new JScrollPane(infoArea), BorderLayout.CENTER);

        JPanel itemPanel = new JPanel(new BorderLayout(2, 2));
        itemPanel.setBorder(BorderFactory.createTitledBorder("공개 단계 사용"));
        itemPanel.add(inventoryLabel, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
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
