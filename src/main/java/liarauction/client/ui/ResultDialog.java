package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import liarauction.common.GameConfig;
import liarauction.common.Message;

/**
 * 게임 결과 창 (#19): 승자, 순위표, 히든 미션 결과, 작품 진위 공개.
 * 게임이 끝나면 방 화면으로 돌아가므로 다른 화면을 막지 않게(모달 아님) 띄운다.
 */
public final class ResultDialog extends JDialog {

    /** {@code RESULT_PLAYER} 한 줄 */
    public record PlayerRow(int rank, String nickname, int cash, int artworkValue, int artworkCount,
            int bonus, int total, String mission, boolean missionSuccess) {

        static PlayerRow from(Message m) {
            return new PlayerRow(m.intArg(0), m.arg(1), m.intArg(2), m.intArg(3), m.intArg(4),
                    m.intArg(5), m.intArg(6), m.arg(7), m.boolArg(8));
        }
    }

    /** {@code RESULT_ARTWORK} 한 줄 */
    public record ArtworkRow(int round, String title, String artist, String grade, int value, String owner) {

        static ArtworkRow from(Message m) {
            return new ArtworkRow(m.intArg(0), m.arg(1), m.arg(2), m.arg(3), m.intArg(4), m.arg(5));
        }
    }

    final JTable rankingTable;
    final JTable artworkTable;
    final JLabel winnerLabel;

    public ResultDialog(Window owner, String myName, List<PlayerRow> players, List<ArtworkRow> artworks) {
        super(owner, "게임 결과", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        winnerLabel = new JLabel(winnerText(players), SwingConstants.CENTER);
        winnerLabel.setFont(winnerLabel.getFont().deriveFont(Font.BOLD, 22f));
        winnerLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        rankingTable = table(new String[] {"순위", "이름", "현금", "작품 가치", "미션 보너스", "최종"});
        DefaultTableModel ranking = (DefaultTableModel) rankingTable.getModel();
        for (PlayerRow p : players) {
            ranking.addRow(new Object[] {
                p.rank() + "위",
                p.nickname() + (p.nickname().equals(myName) ? " (나)" : ""),
                GameConfig.money(p.cash()),
                GameConfig.money(p.artworkValue()) + " (" + p.artworkCount() + "점)",
                "+" + GameConfig.money(p.bonus()),
                GameConfig.money(p.total())
            });
        }
        highlightRow(rankingTable, players.stream().map(PlayerRow::nickname).toList().indexOf(myName));

        JPanel missions = new JPanel();
        missions.setLayout(new BoxLayout(missions, BoxLayout.Y_AXIS));
        missions.setBorder(BorderFactory.createTitledBorder("히든 미션"));
        for (PlayerRow p : players) {
            if (!p.mission().isEmpty()) {
                missions.add(new JLabel(p.nickname() + ": " + p.mission()
                        + (p.missionSuccess() ? "  → 성공" : "  → 실패")));
            }
        }

        artworkTable = table(new String[] {"라운드", "작품", "작가", "등급", "실제 가치", "낙찰자"});
        DefaultTableModel art = (DefaultTableModel) artworkTable.getModel();
        for (ArtworkRow a : artworks) {
            art.addRow(new Object[] {a.round(), a.title(), a.artist(), a.grade(), GameConfig.money(a.value()), a.owner()});
        }

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(titled(rankingTable, "순위"));
        if (missions.getComponentCount() > 0) {
            center.add(missions);
        }
        center.add(titled(artworkTable, "작품 진위 공개"));

        JButton close = new JButton("닫기");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.add(close);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        content.add(winnerLabel, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(640, 560);
        setLocationRelativeTo(owner);
    }

    /** 1위가 한 명이면 "우승: 이름", 여러 명이면 "공동 1위: 이름, 이름" */
    static String winnerText(List<PlayerRow> players) {
        List<String> firsts = players.stream().filter(p -> p.rank() == 1).map(PlayerRow::nickname).toList();
        if (firsts.isEmpty()) {
            return "게임 종료";
        }
        return (firsts.size() == 1 ? "우승: " : "공동 1위: ") + String.join(", ", firsts);
    }

    private static JTable table(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable t = new JTable(model);
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        return t;
    }

    private static JScrollPane titled(JTable table, String title) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        scroll.setPreferredSize(new Dimension(600, 40 + table.getRowHeight() * Math.max(2, table.getRowCount())));
        return scroll;
    }

    /** 내 줄은 굵게 */
    private static void highlightRow(JTable table, int row) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, value, selected, focus, r, c);
                comp.setFont(comp.getFont().deriveFont(r == row ? Font.BOLD : Font.PLAIN));
                return comp;
            }
        };
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(renderer);
        }
    }
}
