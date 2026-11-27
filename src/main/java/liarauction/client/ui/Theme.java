package liarauction.client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * 경매장 테마 (#37): 고급 미술품 경매장. 어두운 벽, 금색 몰딩, 버건디 카펫, 따뜻한 조명.
 *
 * <p>색·글꼴·테두리는 모두 여기서 가져다 쓴다 (다른 화면 작업도 이 클래스를 쓰면 모양이 맞는다).
 * {@link #install()}은 클라이언트 시작 때 한 번 부른다. 외부 라이브러리 없이 Swing 기본(Metal) 위에 색을 입힌다.
 */
public final class Theme {

    private Theme() {
    }

    // ===== 색 =====

    /** 벽 (가장 어두운 배경) */
    public static final Color WALL = new Color(0x16, 0x13, 0x10);
    /** 벽 위쪽 (조명이 닿는 쪽) */
    public static final Color WALL_LIGHT = new Color(0x2B, 0x22, 0x1A);
    /** 패널·카드 배경 */
    public static final Color CARD = new Color(0x1F, 0x1B, 0x17);
    /** 입력창·표 배경 */
    public static final Color FIELD = new Color(0x12, 0x10, 0x0E);
    /** 금색 강조 (테두리, 제목) */
    public static final Color GOLD = new Color(0xC9, 0xA5, 0x4B);
    /** 밝은 금색 (마우스를 올렸을 때, 중요한 숫자) */
    public static final Color GOLD_LIGHT = new Color(0xE8, 0xCC, 0x7A);
    /** 어두운 금색 (흐린 테두리) */
    public static final Color GOLD_DIM = new Color(0x7A, 0x63, 0x2E);
    /** 버건디 카펫 */
    public static final Color CARPET = new Color(0x5E, 0x14, 0x22);
    /** 카펫 어두운 쪽 */
    public static final Color CARPET_DARK = new Color(0x33, 0x0A, 0x12);
    /** 본문 글자 (아이보리) */
    public static final Color TEXT = new Color(0xEF, 0xE6, 0xD2);
    /** 흐린 글자 (안내, 비활성) */
    public static final Color TEXT_DIM = new Color(0x9C, 0x92, 0x80);
    /** 선택된 줄 */
    public static final Color SELECTION = new Color(0x4A, 0x3A, 0x1C);
    /** 좋은 소식 (성공, 준비 완료) */
    public static final Color GOOD = new Color(0x9F, 0xC9, 0x8A);
    /** 나쁜 소식 (실패, 오류) */
    public static final Color BAD = new Color(0xD9, 0x7B, 0x6C);

    // ===== 글꼴 =====

    private static final String SANS = pick("Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "NanumGothic", Font.DIALOG);
    private static final String SERIF = pick("Batang", "바탕", "AppleMyungjo", "NanumMyeongjo", Font.SERIF);

    /** 본문 */
    public static final Font BODY = new Font(SANS, Font.PLAIN, 14);
    /** 작은 글자 (안내) */
    public static final Font SMALL = new Font(SANS, Font.PLAIN, 12);
    /** 굵은 본문 */
    public static final Font BOLD = new Font(SANS, Font.BOLD, 14);
    /** 화면 제목 (명조) */
    public static final Font TITLE = new Font(SERIF, Font.BOLD, 26);
    /** 큰 제목 (로그인 화면 등) */
    public static final Font DISPLAY = new Font(SERIF, Font.BOLD, 40);
    /** 작품 제목 (명조) */
    public static final Font ARTWORK = new Font(SERIF, Font.BOLD, 28);
    /** 큰 숫자 (현재가 등) */
    public static final Font NUMBER = new Font(SANS, Font.BOLD, 22);

    /**
     * 전체 화면에 테마를 입힌다. 클라이언트 시작 때 Swing 스레드에서 한 번 부른다.
     * (테스트는 부르지 않아도 된다: 화면은 이 값이 없어도 동작한다.)
     */
    public static void install() {
        try {
            // Metal 기본 색(스크롤바·체크박스·포커스 등)도 어둡게
            MetalLookAndFeel.setCurrentTheme(new HallMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // 기본 모양으로도 동작한다
        }
        // 모든 글꼴을 한글 글꼴로
        FontUIResource body = new FontUIResource(BODY);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String k && k.endsWith(".font")) {
                UIManager.put(k, body);
            }
        }

        put(WALL, "Panel.background", "OptionPane.background", "Viewport.background",
                "ScrollPane.background", "RootPane.background", "control");
        put(TEXT, "Label.foreground", "OptionPane.messageForeground", "CheckBox.foreground",
                "RadioButton.foreground", "TitledBorder.titleColor", "Button.foreground", "ToolTip.foreground");
        put(FIELD, "TextField.background", "PasswordField.background", "TextArea.background",
                "List.background", "Table.background", "ComboBox.background", "FormattedTextField.background");
        put(TEXT, "TextField.foreground", "PasswordField.foreground", "TextArea.foreground",
                "List.foreground", "Table.foreground", "ComboBox.foreground", "FormattedTextField.foreground",
                "TextField.caretForeground", "PasswordField.caretForeground", "TextArea.caretForeground");
        put(TEXT_DIM, "TextField.inactiveForeground", "Label.disabledForeground", "Button.disabledText",
                "TextField.disabledForeground");
        put(SELECTION, "List.selectionBackground", "Table.selectionBackground", "ComboBox.selectionBackground",
                "TextField.selectionBackground", "TextArea.selectionBackground");
        put(GOLD_LIGHT, "List.selectionForeground", "Table.selectionForeground", "ComboBox.selectionForeground");
        put(GOLD_DIM, "Table.gridColor", "Separator.foreground");
        put(CARD, "TableHeader.background", "CheckBox.background", "ToolTip.background", "Button.background");
        put(GOLD, "TableHeader.foreground", "Button.focus");
        UIManager.put("TitledBorder.border", BorderFactory.createLineBorder(GOLD_DIM));
        UIManager.put("TitledBorder.font", new FontUIResource(BOLD));
        UIManager.put("TextField.border", fieldBorder());
        UIManager.put("PasswordField.border", fieldBorder());
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(GOLD_DIM));
        UIManager.put("ButtonUI", GoldButtonUI.class.getName());
        UIManager.put("TableHeader.cellBorder", BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, GOLD_DIM), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(1, 4, 1, 4));
    }

    // ===== 꾸미기 도우미 =====

    /** 금색 선 테두리 + 제목 */
    public static Border goldBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GOLD_DIM), title);
        border.setTitleColor(GOLD);
        border.setTitleFont(BOLD);
        return BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(4, 6, 6, 6));
    }

    /** 금박 액자 테두리 (작품 카드) */
    public static Border frameBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD_DIM, 2),
                        BorderFactory.createLineBorder(GOLD, 3)),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD_DIM, 1),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    }

    /** 카드 모양: 불투명한 어두운 바탕 + 금색 테두리 */
    public static <T extends JComponent> T card(T c, String title) {
        c.setOpaque(true);
        c.setBackground(CARD);
        c.setBorder(title == null
                ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD_DIM),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8))
                : goldBorder(title));
        return c;
    }

    /** 경매장 배경이 비치도록 투명하게 */
    public static <T extends JComponent> T clear(T c) {
        c.setOpaque(false);
        return c;
    }

    /** 화면 제목 (금색 명조) */
    public static JLabel title(JLabel label) {
        label.setFont(TITLE);
        label.setForeground(GOLD);
        return label;
    }

    /** 흐린 안내 글자 */
    public static JLabel hint(JLabel label) {
        label.setFont(SMALL);
        label.setForeground(TEXT_DIM);
        return label;
    }

    /** 기록 칸 (소문·채팅 기록 등) */
    public static JTextArea log(JTextArea area) {
        area.setBackground(FIELD);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setFont(BODY);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return area;
    }

    /** 표 모양 */
    public static JTable table(JTable table) {
        table.setBackground(FIELD);
        table.setForeground(TEXT);
        table.setGridColor(GOLD_DIM);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(GOLD_LIGHT);
        table.setFont(BODY);
        table.getTableHeader().setBackground(CARD);
        table.getTableHeader().setForeground(GOLD);
        table.getTableHeader().setFont(BOLD);
        return table;
    }

    /** 스크롤 칸 테두리·바탕 */
    public static JScrollPane scroll(JScrollPane scroll) {
        scroll.getViewport().setBackground(FIELD);
        scroll.setBackground(CARD);
        return scroll;
    }

    /** Metal 기본 색: 어두운 바탕에 금색 */
    private static final class HallMetalTheme extends DefaultMetalTheme {
        @Override
        public String getName() {
            return "Liar Auction Hall";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(GOLD);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(GOLD_DIM);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(SELECTION);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(GOLD_DIM);
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(new Color(0x3A, 0x31, 0x24));
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(WALL);
        }

        @Override
        protected ColorUIResource getBlack() {
            return new ColorUIResource(TEXT);
        }

        @Override
        protected ColorUIResource getWhite() {
            return new ColorUIResource(FIELD);
        }
    }

    private static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_DIM), BorderFactory.createEmptyBorder(4, 6, 4, 6));
    }

    private static void put(Color color, String... keys) {
        ColorUIResource c = new ColorUIResource(color);
        for (String key : keys) {
            UIManager.put(key, c);
        }
    }

    /** 설치된 글꼴 중 처음 있는 것 (없으면 마지막 값) */
    private static String pick(String... families) {
        Set<String> installed;
        try {
            installed = Set.copyOf(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        } catch (RuntimeException e) {
            return families[families.length - 1];
        }
        for (String f : families) {
            if (installed.contains(f)) {
                return f;
            }
        }
        return families[families.length - 1];
    }
}
