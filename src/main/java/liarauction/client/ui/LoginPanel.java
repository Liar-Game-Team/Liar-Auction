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
        Theme.clear(this);

        JLabel title = new JLabel("라이어 경매", SwingConstants.CENTER);
        title.setFont(Theme.DISPLAY);
        title.setForeground(Theme.GOLD);
        JLabel subtitle = Theme.hint(new JLabel("LIAR AUCTION  ·  진품인가, 가짜인가", SwingConstants.CENTER));
        subtitle.setFont(Theme.BODY);

        JPanel form = Theme.card(new JPanel(new GridBagLayout()), "입장");
        form.setBorder(BorderFactory.createCompoundBorder(form.getBorder(),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 6, 24, 6);
        add(subtitle, c);
        c.insets = new Insets(6, 6, 6, 6);

        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(6, 6, 6, 6);
        f.fill = GridBagConstraints.HORIZONTAL;
        f.gridwidth = 1;
        addRow(form, f, 0, "서버", hostField);
        addRow(form, f, 1, "포트", portField);
        addRow(form, f, 2, "닉네임", nicknameField);
        f.gridx = 0;
        f.gridy = 3;
        f.gridwidth = 2;
        f.insets = new Insets(14, 6, 6, 6);
        form.add(connectButton, f);
        f.gridy = 4;
        f.insets = new Insets(4, 6, 2, 6);
        statusLabel.setForeground(Theme.BAD);
        form.add(statusLabel, f);

        c.gridy = 2;
        add(form, c);

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

    private static void addRow(JPanel form, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT_DIM);
        form.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
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
