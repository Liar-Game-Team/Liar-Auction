package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 채팅 (방 목록·방·게임 공용). 내 메시지는 금색, 다른 사람은 아이보리, 시스템 안내는 흐린 기울임으로 구분한다.
 */
public final class ChatPanel extends JPanel {

    private final JTextPane logPane = new JTextPane();
    private final JTextField inputField = new JTextField();
    private String myName = "";
    private String lastLine = "";
    private Runnable onAppend = () -> { };

    public ChatPanel(Consumer<Message> sender) {
        this(sender, "협상 채팅");
    }

    /** @param title 테두리 제목 (방 목록 채팅, 방 채팅 등), null이면 제목 없음 */
    public ChatPanel(Consumer<Message> sender, String title) {
        super(new BorderLayout(4, 4));
        Theme.card(this, title);
        setPreferredSize(new Dimension(0, 180));

        logPane.setEditable(false);
        logPane.setBackground(Theme.FIELD);
        logPane.setForeground(Theme.TEXT);
        logPane.setFont(Theme.BODY);
        logPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(Theme.scroll(new JScrollPane(logPane)), BorderLayout.CENTER);

        JButton sendButton = new JButton("보내기");
        JPanel input = Theme.clear(new JPanel(new BorderLayout(6, 0)));
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

    /** 내 닉네임 (내 메시지를 금색으로) */
    public void setMyName(String myName) {
        this.myName = myName == null ? "" : myName;
    }

    /** 새 줄이 붙을 때마다 부를 일 (떠 있는 채팅창의 안 읽은 개수 등) */
    public void setOnAppend(Runnable onAppend) {
        this.onAppend = onAppend == null ? () -> { } : onAppend;
    }

    public void addChat(String sender, String text) {
        boolean mine = sender.equals(myName);
        append(sender + ": " + text, mine ? Theme.GOLD_LIGHT : Theme.TEXT, mine, false);
    }

    /** 시스템 알림 (입장, 오류 등) */
    public void addSystem(String text) {
        append("* " + text, Theme.TEXT_DIM, false, true);
    }

    public void clear() {
        logPane.setText("");
        lastLine = "";
    }

    /** 마지막 줄 (미리보기용) */
    public String lastLine() {
        return lastLine;
    }

    /** 지금까지의 기록 전체 */
    String logText() {
        return logPane.getText();
    }

    /** 입력칸에 바로 쓸 수 있게 */
    public void focusInput() {
        inputField.requestFocusInWindow();
    }

    private void append(String line, Color color, boolean bold, boolean italic) {
        StyledDocument doc = logPane.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italic);
        try {
            doc.insertString(doc.getLength(), line + "\n", style);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        logPane.setCaretPosition(doc.getLength());
        lastLine = line;
        onAppend.run();
    }
}
