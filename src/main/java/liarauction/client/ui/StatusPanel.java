package liarauction.client.ui;

import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import liarauction.common.GameConfig;
import liarauction.common.Phase;

/** 상단 상태 표시: 라운드, 단계, 남은 시간, 내 현금, 함께하는 사람 (다른 사람 현금은 비공개) */
public final class StatusPanel extends JPanel {

    private final JLabel roundLabel = new JLabel();
    private final JLabel phaseLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel cashLabel = new JLabel();

    /** 입장 순서대로 닉네임 → 현금 */
    private final Map<String, Integer> cash = new LinkedHashMap<>();
    private String myName = "";
    private int totalRounds;
    private int secondsLeft;
    private final Timer countdown = new Timer(1000, e -> tick());

    public StatusPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 22, 6));
        Theme.card(this, null);
        roundLabel.setFont(Theme.TITLE.deriveFont(20f));
        roundLabel.setForeground(Theme.GOLD);
        phaseLabel.setFont(Theme.BOLD.deriveFont(16f));
        phaseLabel.setForeground(Theme.TEXT);
        timeLabel.setFont(Theme.NUMBER.deriveFont(18f));
        timeLabel.setForeground(Theme.GOLD_LIGHT);
        cashLabel.setForeground(Theme.TEXT_DIM);
        add(roundLabel);
        add(phaseLabel);
        add(timeLabel);
        add(cashLabel);
    }

    public void startGame(String myName, List<String> players, int startCash, int totalRounds) {
        this.myName = myName;
        this.totalRounds = totalRounds;
        cash.clear();
        for (String name : players) {
            cash.put(name, startCash);
        }
        setRound(0);
        setPhase(Phase.WAITING, 0);
        updateCash();
    }

    public void setRound(int round) {
        roundLabel.setText("라운드 " + round + " / " + totalRounds);
    }

    /** 서버 PHASE 메시지 기준으로 남은 시간을 다시 맞춘다. */
    public void setPhase(Phase phase, int seconds) {
        phaseLabel.setText("단계: " + phase.label());
        secondsLeft = seconds;
        updateTime();
        if (seconds > 0) {
            countdown.restart();
        } else {
            countdown.stop();
        }
    }

    public void setCash(String nickname, int amount) {
        if (cash.containsKey(nickname)) {
            cash.put(nickname, amount);
            updateCash();
        }
    }

    /** 게임 중 나간 플레이어는 목록에서 뺀다 */
    public void removePlayer(String nickname) {
        cash.remove(nickname);
        updateCash();
    }

    public void stop() {
        countdown.stop();
    }

    private void updateCash() {
        StringBuilder sb = new StringBuilder("내 현금 ");
        Integer mine = cash.get(myName);
        sb.append(mine == null ? "-" : GameConfig.money(mine));
        StringBuilder others = new StringBuilder();
        for (String name : cash.keySet()) {
            if (!name.equals(myName)) {
                others.append(others.length() == 0 ? "" : ", ").append(name);
            }
        }
        if (others.length() > 0) {
            sb.append("   ·   함께하는 사람: ").append(others);
        }
        cashLabel.setText(sb.toString());
    }

    private void tick() {
        if (secondsLeft > 0) {
            secondsLeft--;
        }
        updateTime();
        if (secondsLeft == 0) {
            countdown.stop();
        }
    }

    private void updateTime() {
        timeLabel.setText("남은 시간: " + secondsLeft + "초");
    }
}
