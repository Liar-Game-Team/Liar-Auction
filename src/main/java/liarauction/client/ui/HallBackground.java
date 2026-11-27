package liarauction.client.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * 경매장 배경 (#37): 나무 벽 패널, 금색 몰딩, 천장 조명, 버건디 카펫.
 * 외부 그림 없이 {@code Graphics2D}로 그리고, 크기가 바뀔 때만 다시 그려 둔다.
 * 위에 올리는 패널은 {@link Theme#clear}로 투명하게 해야 배경이 보인다.
 */
public class HallBackground extends JPanel {

    private BufferedImage cache;

    public HallBackground(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Theme.WALL);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = cache.createGraphics();
            paintHall(g2, w, h);
            g2.dispose();
        }
        g.drawImage(cache, 0, 0, null);
    }

    /** 경매장 한 장면을 그린다 (다른 화면에서도 쓸 수 있게 static) */
    static void paintHall(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int floorY = (int) (h * 0.80);

        // 벽: 위쪽이 조금 밝은 나무색
        g.setPaint(new GradientPaint(0, 0, Theme.WALL_LIGHT, 0, floorY, Theme.WALL));
        g.fillRect(0, 0, w, floorY);

        // 벽 패널 (세로 홈)
        int panels = Math.max(4, w / 180);
        int pw = w / panels;
        for (int i = 0; i < panels; i++) {
            int x = i * pw;
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(x, 60, 3, floorY - 60);
            g.setColor(new Color(255, 230, 180, 14));
            g.fillRect(x + 3, 60, 1, floorY - 60);
            // 패널 안쪽 사각 몰딩
            g.setColor(new Color(201, 165, 75, 38));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(x + 18, 90, pw - 36, (int) ((floorY - 90) * 0.55));
            g.drawRect(x + 18, 90 + (int) ((floorY - 90) * 0.62), pw - 36, (int) ((floorY - 90) * 0.30));
        }

        // 천장 몰딩 (금색 띠)
        g.setPaint(new GradientPaint(0, 0, new Color(0x0E, 0x0C, 0x0A), 0, 56, new Color(0x24, 0x1C, 0x14)));
        g.fillRect(0, 0, w, 56);
        g.setColor(Theme.GOLD_DIM);
        g.fillRect(0, 52, w, 2);
        g.setColor(Theme.GOLD);
        g.fillRect(0, 56, w, 2);
        g.setColor(new Color(201, 165, 75, 60));
        g.fillRect(0, 62, w, 1);

        // 천장 조명 (따뜻한 빛 원뿔)
        int lights = Math.max(2, w / 360);
        for (int i = 0; i < lights; i++) {
            float cx = w * (i + 0.5f) / lights;
            float radius = Math.max(w / (float) lights, h * 0.6f);
            Composite old = Composite.of(g);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, 58), radius,
                    new float[] {0f, 0.35f, 1f},
                    new Color[] {new Color(255, 214, 150, 90), new Color(255, 190, 120, 28), new Color(0, 0, 0, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fillRect(0, 58, w, floorY - 58);
            old.restore(g);
            // 조명 기구
            g.setColor(Theme.GOLD);
            g.fillRoundRect((int) cx - 22, 50, 44, 10, 6, 6);
            g.setColor(new Color(255, 236, 190));
            g.fillOval((int) cx - 8, 56, 16, 6);
        }

        // 걸레받이 (금색 선 + 어두운 나무)
        g.setColor(new Color(0x10, 0x0C, 0x09));
        g.fillRect(0, floorY - 14, w, 14);
        g.setColor(Theme.GOLD_DIM);
        g.fillRect(0, floorY - 15, w, 1);

        // 바닥: 원근이 있는 버건디 카펫
        g.setPaint(new GradientPaint(0, floorY, Theme.CARPET, 0, h, Theme.CARPET_DARK));
        g.fillRect(0, floorY, w, h - floorY);
        Polygon runner = new Polygon(
                new int[] {(int) (w * 0.30), (int) (w * 0.70), w + w / 10, -w / 10},
                new int[] {floorY, floorY, h, h}, 4);
        g.setPaint(new GradientPaint(0, floorY, new Color(0x7A, 0x1E, 0x2C), 0, h, new Color(0x45, 0x0D, 0x18)));
        g.fillPolygon(runner);
        g.setColor(Theme.GOLD);
        g.setStroke(new BasicStroke(2f));
        g.drawLine((int) (w * 0.30), floorY, -w / 10, h);
        g.drawLine((int) (w * 0.70), floorY, w + w / 10, h);
        g.setColor(new Color(201, 165, 75, 90));
        g.drawLine((int) (w * 0.33), floorY, 0, h);
        g.drawLine((int) (w * 0.67), floorY, w, h);

        // 가장자리를 어둡게 (비네트)
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.45f), Math.max(w, h) * 0.75f,
                new float[] {0.55f, 1f}, new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)}));
        g.fillRect(0, 0, w, h);
    }

    /** AlphaComposite를 잠깐 바꿨다가 되돌리기 */
    private record Composite(java.awt.Composite saved) {
        static Composite of(Graphics2D g) {
            return new Composite(g.getComposite());
        }

        void restore(Graphics2D g) {
            g.setComposite(saved);
        }
    }
}
