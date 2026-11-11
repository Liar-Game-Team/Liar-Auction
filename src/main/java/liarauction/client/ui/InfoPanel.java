package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 파트 B 화면: 감정 정보 (나만 보임), 정밀 감정.
 * TODO(#13): 되팔기 창, TODO(#19): 결과 창
 */
public final class InfoPanel extends JPanel {

    private final JTextArea infoArea = new JTextArea();
    private final JButton appraiseButton = new JButton("정밀 감정 (1회)");

    public InfoPanel(Consumer<Message> sender) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("감정 정보 (나만 보임)"));
        setPreferredSize(new Dimension(260, 0));

        infoArea.setFont(appraiseButton.getFont());
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        add(new JScrollPane(infoArea), BorderLayout.CENTER);

        appraiseButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_APPRAISE)));
        add(appraiseButton, BorderLayout.SOUTH);
    }

    /** {@code INSPECTION} */
    public void showInspection(boolean official, String text) {
        append((official ? "[감정서] " : "[소문] ") + text);
    }

    /** {@code APPRAISE_RESULT} */
    public void showAppraisal(String text) {
        append("[정밀 감정] " + text);
    }

    public void addLine(String text) {
        append(text);
    }

    public void clear() {
        infoArea.setText("");
    }

    private void append(String line) {
        infoArea.append(line + "\n");
        infoArea.setCaretPosition(infoArea.getDocument().getLength());
    }
}
