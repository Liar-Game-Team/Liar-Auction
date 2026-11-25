package liarauction.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;

class AuctionPanelTest {

    @Test
    void showsArtworkPriceBidderAndTimeAndOnlyEnablesBidsDuringAuction() throws Exception {
        AtomicReference<Message> sent = new AtomicReference<>();
        AuctionPanel[] panelRef = new AuctionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            AuctionPanel panel = new AuctionPanel(sent::set);
            panel.showArtwork("별이 빛나는 밤", "고흐", 100);
            panel.updateBid("alice", 150);
            panel.setAuctionPhase(Phase.REVEAL, 3);
            panelRef[0] = panel;
        });

        SwingUtilities.invokeAndWait(() -> {
            List<JLabel> labels = new ArrayList<>();
            List<JButton> buttons = new ArrayList<>();
            collectComponents(panelRef[0], labels, buttons);

            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("별이 빛나는 밤")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("현재가: 150")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("alice")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("3초")));
            assertTrue(buttons.stream().noneMatch(JButton::isEnabled));

            panelRef[0].setAuctionPhase(Phase.AUCTION, 12);
            JButton bidButton = buttons.stream()
                    .filter(button -> button.getText().equals("+50"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(bidButton.isEnabled());
            bidButton.doClick();
            assertEquals(Message.of(Protocol.REQ_BID, 50), sent.get());

            panelRef[0].showRoundEnd("alice", 150);
            assertFalse(bidButton.isEnabled());
        });
    }

    private static void collectComponents(
            Container parent, List<JLabel> labels, List<JButton> buttons) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label) {
                labels.add(label);
            } else if (component instanceof JButton button) {
                buttons.add(button);
            } else if (component instanceof Container container) {
                collectComponents(container, labels, buttons);
            }
        }
    }
}
