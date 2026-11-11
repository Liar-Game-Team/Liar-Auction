package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import liarauction.common.Message;
import liarauction.common.Protocol;

/** 협상 채팅 */
public final class ChatPanel extends JPanel {

    private final JTextArea logArea = new JTextArea();
    private final JTextField inputField = new JTextField();

    public ChatPanel(Consumer<Message> sender) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("협상 채팅"));
        setPreferredSize(new Dimension(0, 180));

        logArea.setFont(inputField.getFont());
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton sendButton = new JButton("보내기");
        JPanel input = new JPanel(new BorderLayout(4, 0));
        input.add(inputField, BorderLayout.CENTER);
        input.add(sendButton, BorderLayout.EAST);
        add(input, BorderLayout.SOUTH);

        Runnable send = () -> {
            String text = inputField.getText();
            if (!text.isBlank()) {
                sender.accept(Message.of(Protocol.REQ_CHAT, text));
                inputField.setText("");
            }
        };
        sendButton.addActionListener(e -> send.run());
        inputField.addActionListener(e -> send.run());
    }

    public void addChat(String sender, String text) {
        append(sender + ": " + text);
    }

    /** 시스템 알림 (입장, 오류 등) */
    public void addSystem(String text) {
        append("* " + text);
    }

    public void clear() {
        logArea.setText("");
    }

    private void append(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
