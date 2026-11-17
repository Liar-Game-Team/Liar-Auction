package liarauction.client.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import liarauction.common.GameConfig;

/** 첫 화면: 서버 주소, 포트, 닉네임 입력 */
public final class LoginPanel extends JPanel {

    /** 접속 버튼을 눌렀을 때 */
    public interface ConnectAction {
        void connect(String host, int port, String nickname);
    }

    private final JTextField hostField = new JTextField("localhost", 15);
    private final JTextField portField = new JTextField(String.valueOf(GameConfig.DEFAULT_PORT), 15);
    private final JTextField nicknameField = new JTextField(15);
    private final JButton connectButton = new JButton("접속");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public LoginPanel(ConnectAction action) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("라이어 경매", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(28f));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);

        c.gridwidth = 1;
        addRow(c, 1, "서버", hostField);
        addRow(c, 2, "포트", portField);
        addRow(c, 3, "닉네임", nicknameField);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        add(connectButton, c);
        c.gridy = 5;
        add(statusLabel, c);

        Runnable submit = () -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                showError("포트는 숫자로 입력하세요");
                return;
            }
            setBusy(true);
            statusLabel.setText("접속 중...");
            action.connect(hostField.getText().trim(), port, nicknameField.getText().trim());
        };
        connectButton.addActionListener(e -> submit.run());
        nicknameField.addActionListener(e -> submit.run());
    }

    private void addRow(GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    public void showError(String message) {
        setBusy(false);
        statusLabel.setText(message);
    }

    public void reset() {
        setBusy(false);
        statusLabel.setText(" ");
    }

    private void setBusy(boolean busy) {
        connectButton.setEnabled(!busy);
        hostField.setEnabled(!busy);
        portField.setEnabled(!busy);
        nicknameField.setEnabled(!busy);
    }
}
