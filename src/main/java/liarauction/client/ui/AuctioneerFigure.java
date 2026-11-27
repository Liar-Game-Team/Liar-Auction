package liarauction.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * 진행자 그림 (#40): 금장 연단 뒤에 선 경매사.
 * 턱시도(새틴 라펠), 버건디 나비넥타이, 금색 포켓치프, 단정하게 넘긴 머리, 콧수염과 금테 모노클, 의사봉.
 * 외부 그림 없이 200×200 좌표로 그리고 크기에 맞춰 늘린다. {@link #setStriking}이면 의사봉을 내려친 모습.
 */
public final class AuctioneerFigure extends JComponent {

    private static final Color SKIN = new Color(0xE6, 0xC0, 0x9C);
    private static final Color SKIN_SHADE = new Color(0xC9, 0x9E, 0x7C);
    private static final Color HAIR = new Color(0x2A, 0x1D, 0x14);
    private static final Color JACKET = new Color(0x15, 0x14, 0x16);
    private static final Color SATIN = new Color(0x34, 0x32, 0x38);
    private static final Color SHIRT = new Color(0xF4, 0xEF, 0xE6);
    private static final Color TIE = new Color(0x7A, 0x1E, 0x2C);
    private static final Color WOOD = new Color(0x4A, 0x2C, 0x1A);
    private static final Color WOOD_DARK = new Color(0x2C, 0x19, 0x0E);

    private boolean striking;

    public AuctioneerFigure() {
        setOpaque(false);
        setPreferredSize(new Dimension(150, 150));
    }

    /** 의사봉을 내려친 모습인지 */
    public void setStriking(boolean striking) {
        this.striking = striking;
        repaint();
    }

    public boolean isStriking() {
        return striking;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = Math.min(getWidth(), getHeight()) / 200.0;
        g.translate((getWidth() - 200 * s) / 2, (getHeight() - 200 * s) / 2);
        g.scale(s, s);
        paintFigure(g, striking);
        g.dispose();
    }

    /** 200×200 좌표에 경매사를 그린다 */
    static void paintFigure(Graphics2D g, boolean striking) {
        // 뒤쪽 조명
        g.setPaint(new RadialGradientPaint(new Point2D.Float(100, 80), 110, new float[] {0f, 1f},
                new Color[] {new Color(255, 214, 150, 70), new Color(0, 0, 0, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, 200, 200);

        // 어깨·재킷
        GeneralPath body = new GeneralPath();
        body.moveTo(34, 150);
        body.curveTo(38, 116, 56, 104, 80, 101);
        body.lineTo(120, 101);
        body.curveTo(144, 104, 162, 116, 166, 150);
        body.closePath();
        g.setPaint(new GradientPaint(0, 100, new Color(0x26, 0x24, 0x28), 0, 150, JACKET));
        g.fill(body);

        // 셔츠 (V자)
        GeneralPath shirt = new GeneralPath();
        shirt.moveTo(88, 101);
        shirt.lineTo(112, 101);
        shirt.lineTo(100, 140);
        shirt.closePath();
        g.setColor(SHIRT);
        g.fill(shirt);

        // 새틴 라펠
        g.setPaint(new GradientPaint(80, 100, SATIN, 90, 140, JACKET));
        GeneralPath leftLapel = new GeneralPath();
        leftLapel.moveTo(86, 101);
        leftLapel.lineTo(76, 104);
        leftLapel.lineTo(84, 120);
        leftLapel.lineTo(78, 124);
        leftLapel.lineTo(98, 146);
        leftLapel.closePath();
        g.fill(leftLapel);
        GeneralPath rightLapel = new GeneralPath();
        rightLapel.moveTo(114, 101);
        rightLapel.lineTo(124, 104);
        rightLapel.lineTo(116, 120);
        rightLapel.lineTo(122, 124);
        rightLapel.lineTo(102, 146);
        rightLapel.closePath();
        g.fill(rightLapel);

        // 금색 포켓치프
        g.setColor(Theme.GOLD);
        GeneralPath pocket = new GeneralPath();
        pocket.moveTo(56, 128);
        pocket.lineTo(60, 120);
        pocket.lineTo(63, 126);
        pocket.lineTo(66, 119);
        pocket.lineTo(69, 128);
        pocket.closePath();
        g.fill(pocket);
        g.setColor(new Color(0x0A, 0x0A, 0x0B));
        g.fillRect(54, 128, 17, 2);

        // 목
        g.setColor(SKIN_SHADE);
        g.fillRoundRect(92, 88, 16, 16, 6, 6);

        // 나비넥타이
        g.setColor(TIE);
        GeneralPath bow = new GeneralPath();
        bow.moveTo(100, 106);
        bow.lineTo(86, 99);
        bow.lineTo(86, 113);
        bow.closePath();
        bow.moveTo(100, 106);
        bow.lineTo(114, 99);
        bow.lineTo(114, 113);
        bow.closePath();
        g.fill(bow);
        g.setColor(Theme.GOLD);
        g.fill(new Ellipse2D.Double(97, 103, 6, 6));

        // 귀
        g.setColor(SKIN_SHADE);
        g.fill(new Ellipse2D.Double(73, 64, 8, 13));
        g.fill(new Ellipse2D.Double(119, 64, 8, 13));
        // 얼굴
        g.setPaint(new GradientPaint(80, 50, SKIN, 120, 95, SKIN_SHADE));
        g.fill(new Ellipse2D.Double(77, 42, 46, 54));

        // 단정하게 넘긴 머리 (가르마 + 윤기)
        GeneralPath hair = new GeneralPath();
        hair.moveTo(76, 68);
        hair.curveTo(72, 40, 96, 30, 112, 35);
        hair.curveTo(128, 40, 130, 58, 124, 68);
        hair.curveTo(120, 56, 112, 50, 104, 49);
        hair.curveTo(94, 48, 84, 52, 80, 62);
        hair.closePath();
        g.setColor(HAIR);
        g.fill(hair);
        g.setColor(new Color(255, 240, 210, 70));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(86, 37, 30, 14, 30, 110);
        g.drawArc(96, 40, 24, 12, 10, 80);

        // 눈썹·눈
        g.setColor(HAIR);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(84, 62, 94, 60);
        g.drawLine(106, 60, 116, 62);
        g.setColor(new Color(0x1E, 0x16, 0x10));
        g.fill(new Ellipse2D.Double(87, 65, 5, 4));
        g.fill(new Ellipse2D.Double(108, 65, 5, 4));

        // 금테 모노클과 줄
        g.setColor(Theme.GOLD);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(103.5, 60.5, 14, 14));
        g.setStroke(new BasicStroke(0.9f));
        GeneralPath chain = new GeneralPath();
        chain.moveTo(117, 71);
        chain.curveTo(126, 86, 124, 104, 120, 122);
        g.draw(chain);

        // 코·콧수염·미소
        g.setColor(SKIN_SHADE.darker());
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(100, 68, 98, 77);
        g.setColor(HAIR);
        GeneralPath mustache = new GeneralPath();
        mustache.moveTo(100, 80);
        mustache.curveTo(95, 77, 88, 79, 85, 84);
        mustache.curveTo(91, 82, 96, 83, 100, 83);
        mustache.curveTo(104, 83, 109, 82, 115, 84);
        mustache.curveTo(112, 79, 105, 77, 100, 80);
        g.fill(mustache);
        g.setColor(new Color(0x8A, 0x4E, 0x3C));
        g.drawArc(93, 82, 14, 7, 200, 140);

        // 의사봉을 든 오른팔 (내려칠 때는 연단 위로)
        double hx = striking ? 150 : 168;
        double hy = striking ? 128 : 92;
        g.setColor(JACKET);
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath arm = new GeneralPath();
        arm.moveTo(146, 116);
        arm.quadTo(striking ? 156 : 162, striking ? 126 : 110, hx, hy);
        g.draw(arm);
        g.setColor(SHIRT);
        g.setStroke(new BasicStroke(3f));
        g.drawLine((int) hx - 5, (int) hy + 4, (int) hx + 3, (int) hy - 3);
        // 의사봉
        double angle = striking ? Math.toRadians(-118) : Math.toRadians(-55);
        Graphics2D gv = (Graphics2D) g.create();
        gv.translate(hx, hy);
        gv.rotate(angle);
        gv.setColor(WOOD);
        gv.fill(new RoundRectangle2D.Double(-2, -26, 4, 30, 2, 2));
        gv.setPaint(new GradientPaint(0, -40, new Color(0x6B, 0x41, 0x26), 0, -26, WOOD_DARK));
        gv.fill(new RoundRectangle2D.Double(-13, -38, 26, 12, 4, 4));
        gv.setColor(Theme.GOLD);
        gv.fillRect(-9, -38, 2, 12);
        gv.fillRect(7, -38, 2, 12);
        gv.dispose();
        if (striking) {
            // 내려친 자리의 울림
            g.setColor(Theme.GOLD_LIGHT);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(104, 132, 98, 124);
            g.drawLine(112, 128, 110, 118);
            g.drawLine(120, 130, 124, 121);
        }
        // 손
        g.setColor(SKIN);
        g.fill(new Ellipse2D.Double(hx - 6, hy - 6, 12, 12));

        // 금장 연단
        GeneralPath podium = new GeneralPath();
        podium.moveTo(34, 146);
        podium.lineTo(166, 146);
        podium.lineTo(156, 200);
        podium.lineTo(44, 200);
        podium.closePath();
        g.setPaint(new GradientPaint(0, 146, WOOD, 0, 200, WOOD_DARK));
        g.fill(podium);
        g.setPaint(new GradientPaint(0, 138, new Color(0x5E, 0x39, 0x22), 0, 148, WOOD_DARK));
        g.fill(new RoundRectangle2D.Double(26, 138, 148, 10, 4, 4));
        g.setColor(Theme.GOLD);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(28, 148, 172, 148);
        g.drawLine(40, 156, 160, 156);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(46, 196, 154, 196);
        // 앞면 문장
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(88, 162, 24, 24));
        g.fill(new Ellipse2D.Double(97, 171, 6, 6));
        g.drawLine(100, 164, 100, 169);
        g.drawLine(100, 179, 100, 184);
        g.drawLine(90, 174, 95, 174);
        g.drawLine(105, 174, 110, 174);
    }
}
