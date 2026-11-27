package liarauction.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * 경매장 버튼 (#37): 어두운 바탕에 금색 테두리, 마우스를 올리면 밝아지고 누르면 금색으로 채워진다.
 * {@link Theme#install()}이 모든 버튼에 적용한다.
 */
public final class GoldButtonUI extends BasicButtonUI {

    private static final GoldButtonUI INSTANCE = new GoldButtonUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setForeground(Theme.TEXT);
        b.setFont(Theme.BOLD);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        return new Dimension(d.width + 28, Math.max(d.height + 12, 34));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth();
        int h = c.getHeight();
        int arc = 10;

        boolean enabled = m.isEnabled();
        boolean pressed = enabled && m.isPressed() && m.isArmed();
        boolean hover = enabled && m.isRollover();

        Color top = pressed ? Theme.GOLD : hover ? new Color(0x3A, 0x2F, 0x1E) : new Color(0x2A, 0x23, 0x1A);
        Color bottom = pressed ? Theme.GOLD_DIM : hover ? new Color(0x26, 0x1E, 0x14) : new Color(0x1A, 0x16, 0x12);
        if (!enabled) {
            top = new Color(0x22, 0x1F, 0x1B);
            bottom = top;
        }
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setStroke(new BasicStroke(hover ? 1.6f : 1.2f));
        g2.setColor(!enabled ? Theme.GOLD_DIM.darker() : hover ? Theme.GOLD_LIGHT : Theme.GOLD);
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        g2.dispose();
        super.paint(g, c);
    }

    /** 글자는 상태에 맞는 색으로 직접 그린다 (기본 비활성 글자는 음각이라 어두운 바탕에서 안 보임) */
    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel m = b.getModel();
        boolean enabled = m.isEnabled();
        boolean pressed = enabled && m.isPressed() && m.isArmed();
        boolean hover = enabled && m.isRollover();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(b.getFont());
        g2.setColor(!enabled ? Theme.TEXT_DIM : pressed ? Theme.WALL : hover ? Theme.GOLD_LIGHT : Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, textRect.x, textRect.y + fm.getAscent());
        g2.dispose();
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // 눌림은 paint에서 그린다
    }
}
