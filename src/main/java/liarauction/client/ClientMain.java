package liarauction.client;

import javax.swing.SwingUtilities;
import liarauction.client.ui.MainFrame;
import liarauction.client.ui.Theme;

/** 클라이언트 실행: {@code ./gradlew runClient} */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
