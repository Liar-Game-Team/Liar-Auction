package liarauction.client.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import liarauction.common.Message;

/**
 * 떠 있는 채팅창 (#39). 게임 화면 위에 떠 있고 제목줄을 드래그해 옮긴다 (화면 밖으로는 못 나감).
 * 접으면 최근 메시지 한 줄과 안 읽은 개수만 보이는 막대가 되어 화면을 가리지 않는다.
 */
public final class FloatingChat extends JPanel {

    /** 펼쳤을 때 크기 */
    static final Dimension EXPANDED = new Dimension(430, 250);
    /** 접었을 때 크기 */
    static final Dimension COLLAPSED = new Dimension(430, 40);

    private final ChatPanel chat;
    private final JLabel titleLabel = new JLabel("협상 채팅");
    private final JLabel previewLabel = new JLabel(" ");
    private final JLabel unreadLabel = new JLabel();
    private final JButton toggleButton = new JButton("펼치기");
    private boolean expanded;
    private int unread;

    public FloatingChat(Consumer<Message> sender) {
        super(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        chat = new ChatPanel(sender, null);
        chat.setOpaque(false);
        chat.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        chat.setOnAppend(this::onNewLine);

        JPanel bar = Theme.clear(new JPanel(new BorderLayout(8, 0)));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        bar.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        titleLabel.setFont(Theme.BOLD);
        titleLabel.setForeground(Theme.GOLD);
        previewLabel.setFont(Theme.SMALL);
        previewLabel.setForeground(Theme.TEXT_DIM);
        unreadLabel.setFont(Theme.BOLD);
        unreadLabel.setForeground(Theme.WALL);
        unreadLabel.setOpaque(true);
        unreadLabel.setBackground(Theme.GOLD);
        unreadLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        unreadLabel.setVisible(false);
        toggleButton.setFont(Theme.SMALL);
        toggleButton.addActionListener(e -> toggle());

        JPanel left = Theme.clear(new JPanel(new BorderLayout(8, 0)));
        left.add(titleLabel, BorderLayout.WEST);
        left.add(previewLabel, BorderLayout.CENTER);
        JPanel right = Theme.clear(new JPanel(new BorderLayout(6, 0)));
        right.add(unreadLabel, BorderLayout.WEST);
        right.add(toggleButton, BorderLayout.EAST);
        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);
        add(chat, BorderLayout.CENTER);

        // 제목줄 드래그로 옮기기, 두 번 누르면 접기/펼치기
        MouseAdapter drag = new MouseAdapter() {
            private Point grab;

            @Override
            public void mousePressed(MouseEvent e) {
                grab = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (grab != null) {
                    Point at = getLocation();
                    moveTo(at.x + e.getX() - grab.x, at.y + e.getY() - grab.y);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggle();
                }
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        previewLabel.addMouseListener(drag);
        previewLabel.addMouseMotionListener(drag);

        collapse();
    }

    public ChatPanel chat() {
        return chat;
    }

    public boolean isExpanded() {
        return expanded;
    }

    /** 접혀 있을 때 쌓인 안 읽은 메시지 수 */
    public int unread() {
        return unread;
    }

    /** 접힌 막대에 보이는 최근 메시지 */
    public String previewText() {
        return previewLabel.getText();
    }

    public void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    public void expand() {
        expanded = true;
        unread = 0;
        chat.setVisible(true);
        updateBar();
        resizeKeepingBottom(EXPANDED);
        chat.focusInput();
    }

    public void collapse() {
        expanded = false;
        chat.setVisible(false);
        updateBar();
        resizeKeepingBottom(COLLAPSED);
    }

    /** 새 게임: 기록을 비우고 접는다 */
    public void reset() {
        chat.clear();
        unread = 0;
        previewLabel.setText(" ");
        collapse();
    }

    /** 부모 안에서만 움직인다 */
    public void moveTo(int x, int y) {
        Container parent = getParent();
        if (parent != null && parent.getWidth() > 0) {
            x = Math.max(0, Math.min(x, parent.getWidth() - getWidth()));
            y = Math.max(0, Math.min(y, parent.getHeight() - getHeight()));
        }
        setLocation(x, y);
    }

    /** 부모 크기가 바뀌어도 화면 안에 남게 */
    void keepInside() {
        moveTo(getX(), getY());
    }

    private void onNewLine() {
        previewLabel.setText(shorten(chat.lastLine(), 34));
        if (!expanded) {
            unread++;
        }
        updateBar();
    }

    private void updateBar() {
        toggleButton.setText(expanded ? "접기" : "펼치기");
        previewLabel.setVisible(!expanded);
        unreadLabel.setText(String.valueOf(unread));
        unreadLabel.setVisible(!expanded && unread > 0);
    }

    /** 펼치고 접을 때 아래쪽 위치를 유지해서 막대가 제자리에 있는 것처럼 */
    private void resizeKeepingBottom(Dimension size) {
        int bottom = getY() + getHeight();
        setSize(size);
        setPreferredSize(size);
        if (getParent() != null && getHeight() > 0) {
            moveTo(getX(), bottom - size.height);
        }
        revalidate();
        repaint();
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(Theme.CARD.getRed(), Theme.CARD.getGreen(), Theme.CARD.getBlue(), 238));
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.setColor(Theme.GOLD);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.dispose();
    }
}
