package liarauction.client.ui;

import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import liarauction.common.Phase;

/** 상단 상태 표시: 라운드, 단계, 남은 시간, 양쪽 현금 */
public final class StatusPanel extends JPanel {

    private final JLabel roundLabel = new JLabel();
    private final JLabel phaseLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel myCashLabel = new JLabel();
    private final JLabel opponentCashLabel = new JLabel();

    private String myName = "";
    private String opponentName = "";
    private int totalRounds;
    private int secondsLeft;
    private final Timer countdown = new Timer(1000, e -> tick());

    public StatusPanel() {
        super(new FlowLayout(FlowLayout.LEFT, 16, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(roundLabel);
        add(phaseLabel);
        add(timeLabel);
        add(myCashLabel);
        add(opponentCashLabel);
    }

    public void startGame(String myName, String opponentName, int startCash, int totalRounds) {
        this.myName = myName;
        this.opponentName = opponentName;
        this.totalRounds = totalRounds;
        setRound(0);
        setPhase(Phase.WAITING, 0);
        setCash(myName, startCash);
        setCash(opponentName, startCash);
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

    public void setCash(String nickname, int cash) {
        if (nickname.equals(myName)) {
            myCashLabel.setText("내 현금(" + myName + "): " + cash);
        } else if (nickname.equals(opponentName)) {
            opponentCashLabel.setText("상대 현금(" + opponentName + "): " + cash);
        }
    }

    public void stop() {
        countdown.stop();
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
