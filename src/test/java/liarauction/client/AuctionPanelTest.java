package liarauction.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import liarauction.common.Phase;
import org.junit.jupiter.api.Test;

class AuctionPanelTest {

    @Test
    void showsAuctionStateAndSendsSelectedBidIncrement() throws Exception {
        AtomicInteger selectedIncrement = new AtomicInteger();
        AuctionPanel[] panelRef = new AuctionPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            AuctionPanel panel = new AuctionPanel(selectedIncrement::set);
            panel.updateAuction("별이 빛나는 밤", "고흐", Phase.AUCTION, "alice", 250, 12);
            panel.setBiddingEnabled(true);
            panelRef[0] = panel;
        });

        SwingUtilities.invokeAndWait(() -> {
            List<JLabel> labels = new ArrayList<>();
            List<JButton> buttons = new ArrayList<>();
            collectComponents(panelRef[0], labels, buttons);

            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("별이 빛나는 밤")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("현재가: 250")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("alice")));
            assertTrue(labels.stream().anyMatch(label -> label.getText().contains("12")));

            JButton bidButton = buttons.stream()
                    .filter(button -> button.getText().equals("+50"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(bidButton.isEnabled());
            bidButton.doClick();
        });

        assertEquals(50, selectedIncrement.get());
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
