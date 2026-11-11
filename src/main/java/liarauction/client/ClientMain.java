package liarauction.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import liarauction.client.ui.MainFrame;

/** 클라이언트 실행: {@code ./gradlew runClient} */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 기본 모양 사용
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
