package liarauction.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import liarauction.common.GameConfig;
import liarauction.common.Phase;

/** Swing view for the current artwork and live auction state. */
public final class AuctionPanel extends JPanel {

    private final JLabel artworkLabel = new JLabel("작품: -");
    private final JLabel artistLabel = new JLabel("작가: -");
    private final JLabel phaseLabel = new JLabel("단계: -");
    private final JLabel priceLabel = new JLabel("현재가: 0");
    private final JLabel bidderLabel = new JLabel("최고 입찰자: 없음");
    private final JLabel timeLabel = new JLabel("남은 시간: 0초");
    private final Map<Integer, JButton> bidButtons = new LinkedHashMap<>();

    public AuctionPanel(IntConsumer bidAction) {
        Objects.requireNonNull(bidAction, "bidAction");
        setLayout(new BorderLayout(8, 8));

        JPanel details = new JPanel(new GridLayout(0, 1, 4, 4));
        details.add(artworkLabel);
        details.add(artistLabel);
        details.add(phaseLabel);
        details.add(priceLabel);
        details.add(bidderLabel);
        details.add(timeLabel);
        add(details, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        for (int increment : GameConfig.BID_INCREMENTS) {
            JButton button = new JButton("+" + increment);
            button.addActionListener(event -> bidAction.accept(increment));
            bidButtons.put(increment, button);
            buttons.add(button);
        }
        add(buttons, BorderLayout.SOUTH);
        setBiddingEnabled(false);
    }

    public void updateAuction(
            String title,
            String artist,
            Phase phase,
            String highestBidder,
            int currentPrice,
            int secondsRemaining) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(artist, "artist");
        Objects.requireNonNull(phase, "phase");
        if (currentPrice < 0 || secondsRemaining < 0) {
            throw new IllegalArgumentException("가격과 남은 시간은 음수일 수 없습니다");
        }

        runOnEventDispatchThread(() -> {
            artworkLabel.setText("작품: " + title);
            artistLabel.setText("작가: " + artist);
            phaseLabel.setText("단계: " + phase.label());
            priceLabel.setText("현재가: " + currentPrice);
            bidderLabel.setText("최고 입찰자: "
                    + (highestBidder == null ? "없음" : highestBidder));
            timeLabel.setText("남은 시간: " + secondsRemaining + "초");
        });
    }

    public void setBiddingEnabled(boolean enabled) {
        runOnEventDispatchThread(() ->
                bidButtons.values().forEach(button -> button.setEnabled(enabled)));
    }

    private static void runOnEventDispatchThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
