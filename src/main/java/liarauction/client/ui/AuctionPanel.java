package liarauction.client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Protocol;

/**
 * 파트 A 화면: 작품 카드, 현재가, 최고 입찰자, 입찰 버튼.
 * TODO(#11): 작품 카드 꾸미기, 경매 단계에서만 버튼 활성화
 */
public final class AuctionPanel extends JPanel {

    private final JLabel titleLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel artistLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel priceLabel = new JLabel("현재가: -", SwingConstants.CENTER);
    private final JLabel bidderLabel = new JLabel("최고 입찰자: -", SwingConstants.CENTER);
    private final JLabel noticeLabel = new JLabel(" ", SwingConstants.CENTER);

    public AuctionPanel(Consumer<Message> sender) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("경매"));

        titleLabel.setFont(titleLabel.getFont().deriveFont(22f));
        priceLabel.setFont(priceLabel.getFont().deriveFont(18f));

        JPanel card = new JPanel(new GridLayout(5, 1, 4, 4));
        card.add(titleLabel);
        card.add(artistLabel);
        card.add(priceLabel);
        card.add(bidderLabel);
        card.add(noticeLabel);
        add(card, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        for (int inc : GameConfig.BID_INCREMENTS) {
            JButton b = new JButton("+" + inc);
            b.addActionListener(e -> sender.accept(Message.of(Protocol.REQ_BID, inc)));
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
        noticeLabel.setText(" ");
    }

    /** {@code UPDATE_BID} */
    public void updateBid(String bidder, int price) {
        priceLabel.setText("현재가: " + price);
        bidderLabel.setText("최고 입찰자: " + bidder);
        noticeLabel.setText(" ");
    }

    /** {@code BID_REJECT} 등 안내 문구 */
    public void showNotice(String text) {
        noticeLabel.setText(text);
    }

    /** {@code ROUND_END} */
    public void showRoundEnd(String winner, int price) {
        showNotice(Protocol.NONE.equals(winner) ? "유찰" : winner + " 낙찰 (" + price + ")");
    }
}
