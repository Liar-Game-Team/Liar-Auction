package liarauction.client.ui;

import java.util.List;
import java.util.Random;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;

/**
 * 진행자 대사 (#40). 서버가 보내는 게임 메시지를 진행자 말로 바꾼다 (서버는 바꾸지 않는다).
 *
 * <p>소문({@code RUMOR})도 진행자가 흘리는 말로 보여 준다. 교란당한 플레이어는 서버가 다른 소문을
 * 보내므로 그 사람의 진행자는 다른 말을 하게 된다.
 * {@code PHASE}는 남은 시간 갱신으로 매초 오므로 단계가 바뀔 때만 말한다.
 * 대사를 바꾸거나 늘릴 때는 이 클래스만 고치면 된다.
 */
public final class HostScript {

    private final Random random;
    private Phase lastPhase;

    public HostScript() {
        this(new Random());
    }

    HostScript(Random random) {
        this.random = random;
    }

    /** 게임 시작 인사 */
    public String welcome(int players, int totalRounds) {
        lastPhase = null;
        return pick(
                "신사 숙녀 여러분, 라이어 경매에 오신 것을 환영합니다. 오늘 " + players + "분과 함께 "
                        + totalRounds + "점의 작품을 모십니다.",
                "환영합니다. 오늘 밤 " + totalRounds + "점의 작품이 주인을 기다리고 있습니다. "
                        + "진품과 가짜를 잘 가려내 보시죠.");
    }

    /** 이 메시지에 진행자가 할 말, 할 말이 없으면 null */
    public String lineFor(Message msg) {
        try {
            return switch (msg.command()) {
                case Protocol.START_ROUND -> msg.arg(0) + "라운드! 이번 작품은 " + msg.arg(2) + "의 「" + msg.arg(1)
                        + "」입니다. 시작가는 " + GameConfig.money(msg.intArg(3)) + ".";
                case Protocol.PHASE -> phaseLine(Phase.valueOf(msg.arg(0)));
                case Protocol.RUMOR -> pick("(목소리를 낮추며) 들리는 말로는… ", "여기서만 하는 얘기입니다만… ",
                        "(작게) 소문이 하나 있는데요… ") + msg.arg(0);
                case Protocol.UPDATE_BID -> GameConfig.money(msg.intArg(1)) + "! " + msg.arg(0)
                        + pick(" 님, 더 없습니까?", " 님이 부르셨습니다. 더 계십니까?", " 님! 다음 분?");
                case Protocol.ROUND_END -> Protocol.NONE.equals(msg.arg(0))
                        ? pick("아무도 손을 들지 않는군요. 유찰입니다.", "이 작품은 주인을 찾지 못했습니다. 유찰!")
                        : GameConfig.money(msg.intArg(1)) + ", " + msg.arg(0) + " 님께 낙찰! 탕!";
                case Protocol.MISSION -> "지금부터 여러분께 비밀 임무를 하나씩 드립니다. 다른 분께는 비밀입니다.";
                case Protocol.PLAYER_LEFT -> msg.arg(0) + " 님이 자리를 떠나셨습니다. 경매는 계속됩니다.";
                case Protocol.GAME_OVER -> "오늘의 경매는 여기까지입니다. 모든 작품의 진위를 공개합니다!";
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 단계가 바뀔 때만 말한다 */
    private String phaseLine(Phase phase) {
        if (phase == lastPhase) {
            return null;
        }
        lastPhase = phase;
        return switch (phase) {
            case REVEAL -> "작품을 공개합니다. 감정서와 교란은 지금만 쓰실 수 있습니다.";
            case RUMOR -> null; // 소문 단계는 RUMOR 메시지로 말한다
            case NEGOTIATION -> pick("협상 시간입니다. 서로 떠보셔도 좋습니다.", "잠시 이야기를 나눠 보시죠. 누가 진실을 말하고 있을까요?");
            case AUCTION -> pick("경매를 시작합니다! 입찰해 주십시오.", "자, 입찰 받겠습니다!");
            default -> null;
        };
    }

    private String pick(String... options) {
        return List.of(options).get(random.nextInt(options.length));
    }
}
