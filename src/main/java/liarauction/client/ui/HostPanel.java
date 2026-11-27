package liarauction.client.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 * 진행자 화면 (#40): 경매사 그림 + 말풍선. 말풍선 글자는 한 글자씩 나오고, 누르면 바로 다 보인다.
 * 낙찰 대사가 나오면 의사봉을 내려친다.
 *
 * <p>메시지가 몰려 오면(라운드 시작 직후 단계 안내 등) 대사가 바로 덮이지 않게 줄을 세운다.
 * 한 문장을 다 말하면 잠깐 보여 준 뒤 다음 문장으로 넘어가고, 밀린 문장이 많으면 최근 것만 남긴다.
 */
public final class HostPanel extends JPanel {

    /** 글자 하나가 나오는 간격 (ms) */
    static final int TYPE_DELAY_MS = 28;
    /** 의사봉을 내려친 모습을 보여 주는 시간 (ms) */
    static final int STRIKE_MS = 450;
    /** 다 말한 문장을 보여 두는 시간 (ms) */
    static final int HOLD_MS = 1100;
    /** 밀린 문장을 최대 몇 개까지 남길지 */
    static final int MAX_QUEUE = 2;

    private final AuctioneerFigure figure = new AuctioneerFigure();
    private final JTextArea speech = new JTextArea();
    private final Timer typer;
    private final Timer strikeTimer;
    private final Timer holdTimer;
    private final java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
    private String fullText = "";
    private int shown;
    /** 말하는 중이거나 다 말한 문장을 보여 두는 중 */
    private boolean busy;

    public HostPanel() {
        super(new BorderLayout(4, 0));
        Theme.clear(this);
        setPreferredSize(new Dimension(0, 150));

        add(figure, BorderLayout.WEST);

        Bubble bubble = new Bubble();
        JLabel name = new JLabel("경매사");
        name.setFont(Theme.BOLD);
        name.setForeground(Theme.GOLD);
        speech.setOpaque(false);
        speech.setEditable(false);
        speech.setFocusable(false);
        speech.setLineWrap(true);
        speech.setWrapStyleWord(true);
        speech.setFont(Theme.BODY.deriveFont(17f));
        speech.setForeground(Theme.TEXT);
        bubble.add(name, BorderLayout.NORTH);
        bubble.add(speech, BorderLayout.CENTER);
        add(bubble, BorderLayout.CENTER);

        typer = new Timer(TYPE_DELAY_MS, e -> typeNext());
        strikeTimer = new Timer(STRIKE_MS, e -> figure.setStriking(false));
        strikeTimer.setRepeats(false);
        holdTimer = new Timer(HOLD_MS, e -> next());
        holdTimer.setRepeats(false);

        MouseAdapter skip = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                finishTyping();
            }
        };
        speech.addMouseListener(skip);
        bubble.addMouseListener(skip);
    }

    /** 진행자가 말한다 (한 글자씩). 말하는 중이면 줄을 세운다 */
    public void say(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (!busy) {
            start(line);
            return;
        }
        queue.addLast(line);
        while (queue.size() > MAX_QUEUE) {
            queue.removeFirst();
        }
    }

    /** 말풍선 글자를 바로 다 보여 준다 (다음 문장은 잠깐 뒤에) */
    public void finishTyping() {
        typer.stop();
        shown = fullText.length();
        speech.setText(fullText);
        if (busy) {
            holdTimer.restart();
        }
    }

    /** 밀린 문장으로 넘어간다 (없으면 쉼) */
    void next() {
        holdTimer.stop();
        String line = queue.pollFirst();
        if (line == null) {
            busy = false;
        } else {
            start(line);
        }
    }

    /** 밀린 문장 수 */
    int queued() {
        return queue.size();
    }

    private void start(String line) {
        busy = true;
        fullText = line;
        shown = 0;
        speech.setText("");
        if (line.contains("낙찰!")) {
            strike();
        }
        typer.restart();
    }

    /** 지금 말풍선에 다 나올 문장 */
    public String currentLine() {
        return fullText;
    }

    /** 지금 말풍선에 보이는 글자 */
    String visibleText() {
        return speech.getText();
    }

    boolean isStriking() {
        return figure.isStriking();
    }

    public void clear() {
        typer.stop();
        holdTimer.stop();
        queue.clear();
        busy = false;
        fullText = "";
        shown = 0;
        speech.setText("");
        figure.setStriking(false);
    }

    private void strike() {
        figure.setStriking(true);
        strikeTimer.restart();
    }

    private void typeNext() {
        if (shown >= fullText.length()) {
            typer.stop();
            holdTimer.restart();
            return;
        }
        shown++;
        speech.setText(fullText.substring(0, shown));
    }

    /** 경매사 쪽으로 꼬리가 달린 말풍선 */
    private static final class Bubble extends JPanel {

        private static final int TAIL = 16;

        Bubble() {
            super(new BorderLayout(0, 4));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 14 + TAIL, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D box = new RoundRectangle2D.Double(TAIL, 4, w - TAIL - 4, h - 10, 22, 22);
            GeneralPath tail = new GeneralPath();
            tail.moveTo(TAIL + 1, h * 0.42);
            tail.lineTo(2, h * 0.52);
            tail.lineTo(TAIL + 1, h * 0.60);
            tail.closePath();
            g.setColor(Theme.CARD);
            g.fill(box);
            g.fill(tail);
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(box);
            g.drawLine(TAIL, (int) (h * 0.42), 2, (int) (h * 0.52));
            g.drawLine(2, (int) (h * 0.52), TAIL, (int) (h * 0.60));
            g.setColor(Theme.CARD);
            g.setStroke(new BasicStroke(2.4f));
            g.drawLine(TAIL + 1, (int) (h * 0.43), TAIL + 1, (int) (h * 0.59));
            g.dispose();
        }
    }
}
