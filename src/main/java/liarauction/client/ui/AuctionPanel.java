package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;

/**
 * 파트 A 화면: 작품 카드, 현재가, 최고 입찰자, 입찰 버튼.
 */
public final class AuctionPanel extends JPanel {

    private final JLabel titleLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel artistLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel priceLabel = new JLabel("현재가: -", SwingConstants.CENTER);
    private final JLabel bidderLabel = new JLabel("최고 입찰자: -", SwingConstants.CENTER);
    private final JLabel timeLabel = new JLabel("남은 시간: -", SwingConstants.CENTER);
    private final JLabel noticeLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Map<Integer, JButton> bidButtons = new LinkedHashMap<>();

    public AuctionPanel(Consumer<Message> sender) {
        super(new BorderLayout(8, 8));
        Objects.requireNonNull(sender, "sender");
        setBorder(BorderFactory.createTitledBorder("경매"));

        titleLabel.setFont(titleLabel.getFont().deriveFont(22f));
        priceLabel.setFont(priceLabel.getFont().deriveFont(18f));

        JPanel card = new JPanel(new GridLayout(6, 1, 4, 4));
        card.add(titleLabel);
        card.add(artistLabel);
        card.add(priceLabel);
        card.add(bidderLabel);
        card.add(timeLabel);
        card.add(noticeLabel);
        add(card, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        for (int inc : GameConfig.BID_INCREMENTS) {
            JButton b = new JButton("+" + inc);
            b.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_BID, inc)));
            b.setEnabled(false);
            bidButtons.put(inc, b);
            buttons.add(b);
        }
        add(buttons, BorderLayout.SOUTH);
    }

    /** {@code START_ROUND} */
    public void showArtwork(String title, String artist, int startPrice) {
        titleLabel.setText(title);
        artistLabel.setText(artist);
        priceLabel.setText("현재가: " + startPrice);
        bidderLabel.setText("최고 입찰자: -");
        timeLabel.setText("남은 시간: -");
        noticeLabel.setText(" ");
        setBiddingEnabled(false);
    }

    /** {@code UPDATE_BID} */
    public void updateBid(String bidder, int price) {
        priceLabel.setText("현재가: " + price);
        bidderLabel.setText("최고 입찰자: " + bidder);
        noticeLabel.setText(" ");
    }

    /** {@code PHASE} */
    public void setAuctionPhase(Phase phase, int secondsRemaining) {
        timeLabel.setText("남은 시간: " + secondsRemaining + "초");
        setBiddingEnabled(phase == Phase.AUCTION);
    }

    /** {@code BID_REJECT} 등 안내 문구 */
    public void showNotice(String text) {
        noticeLabel.setText(text);
    }

    /** {@code ROUND_END} */
    public void showRoundEnd(String winner, int price) {
        showNotice(Protocol.NONE.equals(winner) ? "유찰" : winner + " 낙찰 (" + price + ")");
        setBiddingEnabled(false);
    }

    public void clear() {
        titleLabel.setText("-");
        artistLabel.setText("-");
        priceLabel.setText("현재가: -");
        bidderLabel.setText("최고 입찰자: -");
        timeLabel.setText("남은 시간: -");
        noticeLabel.setText(" ");
        setBiddingEnabled(false);
    }

    private void setBiddingEnabled(boolean enabled) {
        bidButtons.values().forEach(button -> button.setEnabled(enabled));
    }
}
