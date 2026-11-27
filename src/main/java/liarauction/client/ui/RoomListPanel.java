package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 방 목록 화면: 방 만들기 / 들어가기 / 빠른 입장, 방 목록에 있는 사람끼리 채팅.
 * 기본 동작만 있는 화면이고 꾸미기는 #46.
 */
public final class RoomListPanel extends JPanel {

    /** 방 목록 한 줄 ({@code ROOM_LIST}의 방 하나) */
    public record RoomRow(int id, String name, int count, int maxPlayers, boolean locked, boolean playing) {

        boolean isFull() {
            return count >= maxPlayers;
        }
    }

    private static final String[] COLUMNS = {"번호", "방 이름", "인원", "상태", "공개"};

    private final Consumer<Message> sender;
    private final JLabel welcomeLabel = new JLabel(" ", SwingConstants.CENTER);
    private final RoomTableModel model = new RoomTableModel();
    private final JTable table = new JTable(model);
    private final JLabel emptyLabel = new JLabel(" ", SwingConstants.CENTER);
    private final ChatPanel chat;
    private String myName = "";

    public RoomListPanel(Consumer<Message> sender) {
        super(new BorderLayout(8, 8));
        this.sender = sender;
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(20f));
        add(welcomeLabel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMaxWidth(80);
        table.getColumnModel().getColumn(3).setMaxWidth(90);
        table.getColumnModel().getColumn(4).setMaxWidth(70);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    joinSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("방 목록"));

        JButton createButton = new JButton("방 만들기");
        JButton joinButton = new JButton("들어가기");
        JButton quickButton = new JButton("빠른 입장");
        JButton refreshButton = new JButton("새로고침");
        createButton.addActionListener(e -> showCreateDialog());
        joinButton.addActionListener(e -> joinSelected());
        quickButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_QUICK_JOIN)));
        refreshButton.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_ROOM_LIST)));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        buttons.add(createButton);
        buttons.add(joinButton);
        buttons.add(quickButton);
        buttons.add(refreshButton);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(scroll, BorderLayout.CENTER);
        center.add(emptyLabel, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        chat = new ChatPanel(sender, "방 목록 채팅");
        add(chat, BorderLayout.SOUTH);
    }

    /** 로그인 직후 */
    public void enter(String myName) {
        this.myName = myName;
        welcomeLabel.setText(myName + " 님, 방을 만들거나 골라 들어가세요");
    }

    /** 방에서 나와 돌아왔을 때 */
    public void returned() {
        chat.addSystem("방 목록으로 돌아왔습니다");
    }

    /** {@code ROOM_LIST|rooms} */
    public void update(List<RoomRow> rooms) {
        model.setRows(rooms);
        emptyLabel.setText(rooms.isEmpty() ? "아직 방이 없습니다. 방을 만들어 보세요" : " ");
    }

    public ChatPanel chat() {
        return chat;
    }

    /** {@code ROOM_LIST}의 인자를 방 목록으로 */
    public static List<RoomRow> parse(String rooms) {
        List<RoomRow> out = new ArrayList<>();
        if (rooms.isEmpty()) {
            return out;
        }
        for (String entry : rooms.split(Protocol.ROOM_SEPARATOR)) {
            String[] f = entry.split(Protocol.LIST_SEPARATOR, -1);
            if (f.length != 6) {
                throw new IllegalArgumentException("잘못된 방 정보: " + entry);
            }
            out.add(new RoomRow(Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]),
                    Integer.parseInt(f[3]), Boolean.parseBoolean(f[4]), Boolean.parseBoolean(f[5])));
        }
        return out;
    }

    private void joinSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "들어갈 방을 고르세요", "방 들어가기", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        RoomRow room = model.rows.get(table.convertRowIndexToModel(row));
        if (room.playing()) {
            JOptionPane.showMessageDialog(this, "게임 중인 방입니다", "방 들어가기", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (room.isFull()) {
            JOptionPane.showMessageDialog(this, "방이 가득 찼습니다", "방 들어가기", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String password = "";
        if (room.locked()) {
            JPasswordField field = new JPasswordField(12);
            int ok = JOptionPane.showConfirmDialog(this, field, "비밀번호 입력", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) {
                return;
            }
            password = new String(field.getPassword());
        }
        sender.accept(Message.of(Protocol.REQ_JOIN_ROOM, room.id(), password));
    }

    private void showCreateDialog() {
        JTextField nameField = new JTextField(myName + "의 방", 16);
        Integer[] sizes = new Integer[GameConfig.MAX_PLAYERS - GameConfig.MIN_PLAYERS + 1];
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = GameConfig.MIN_PLAYERS + i;
        }
        JComboBox<Integer> maxBox = new JComboBox<>(sizes);
        maxBox.setSelectedItem(GameConfig.MAX_PLAYERS);
        JCheckBox lockBox = new JCheckBox("비밀방");
        JPasswordField passwordField = new JPasswordField(12);
        passwordField.setEnabled(false);
        lockBox.addActionListener(e -> passwordField.setEnabled(lockBox.isSelected()));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        addRow(form, c, 0, "방 이름", nameField);
        addRow(form, c, 1, "최대 인원", maxBox);
        addRow(form, c, 2, lockBox, passwordField);

        int ok = JOptionPane.showConfirmDialog(this, form, "방 만들기", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        String password = lockBox.isSelected() ? new String(passwordField.getPassword()) : "";
        if (lockBox.isSelected() && password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "비밀번호를 입력하세요", "방 만들기", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sender.accept(Message.of(Protocol.REQ_CREATE_ROOM,
                nameField.getText().trim(), maxBox.getSelectedItem(), password));
    }

    private static void addRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
        addRow(form, c, row, new JLabel(label), field);
    }

    private static void addRow(JPanel form, GridBagConstraints c, int row, java.awt.Component label,
            java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        form.add(label, c);
        c.gridx = 1;
        form.add(field, c);
    }

    /** 방 목록 표 */
    private static final class RoomTableModel extends AbstractTableModel {

        private List<RoomRow> rows = List.of();

        void setRows(List<RoomRow> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            RoomRow r = rows.get(row);
            return switch (column) {
                case 0 -> r.id();
                case 1 -> r.name();
                case 2 -> r.count() + " / " + r.maxPlayers();
                case 3 -> r.playing() ? "게임 중" : (r.isFull() ? "가득 참" : "대기 중");
                default -> r.locked() ? "비밀방" : "공개";
            };
        }
    }
}
