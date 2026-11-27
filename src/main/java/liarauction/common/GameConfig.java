package liarauction.common;

import java.util.List;

/** 게임 설정값. 밸런스 조정은 여기서 한다. */
public final class GameConfig {

    private GameConfig() {
    }

    /** 기본 서버 포트 */
    public static final int DEFAULT_PORT = 5000;
    /** 게임 시작에 필요한 최소 인원 */
    public static final int MIN_PLAYERS = 2;
    /** 한 방 최대 인원 (방을 만들 때 2~6명 중 고른다) */
    public static final int MAX_PLAYERS = 6;
    /** 기본 라운드 수. 2~4명은 이 값, 5명부터는 인원 × 2 ({@link #roundsFor}) */
    public static final int BASE_ROUNDS = 8;
    /** 시작 현금 (단위: 만원) */
    public static final int START_CASH = 1000;
    /** 돈 단위. 숫자는 그대로 두고 화면에만 붙인다 (1000 → 1000만원) */
    public static final String MONEY_UNIT = "만원";
    /** 입찰 버튼 금액 */
    public static final List<Integer> BID_INCREMENTS = List.of(10, 50, 100);
    /** 이 시간 동안 추가 입찰이 없으면 낙찰 (입찰 시 리셋) */
    public static final int BID_IDLE_SECONDS = 5;
    /** 게임 시작 시 플레이어별 감정서 수 */
    public static final int START_CERTIFICATES = 1;
    /** 게임 시작 시 플레이어별 교란 아이템 수 */
    public static final int START_JAMMERS = 1;
    /** 낙찰 결과를 보여 주고 다음 라운드로 넘어가기 전 대기 시간 */
    public static final int AUCTION_RESULT_SECONDS = 3;
    /** 닉네임 최대 길이 */
    public static final int MAX_NICKNAME_LENGTH = 12;
    /** 방 이름 최대 길이 */
    public static final int MAX_ROOM_NAME_LENGTH = 16;
    /** 비밀방 비밀번호 최대 길이 */
    public static final int MAX_PASSWORD_LENGTH = 12;

    public static boolean isValidBidIncrement(int increment) {
        return BID_INCREMENTS.contains(increment);
    }

    /**
     * 인원별 총 라운드 수 (= 작품 수). 2~4명 8라운드, 5명 10라운드, 6명 12라운드.
     *
     * @throws IllegalArgumentException 인원이 범위를 벗어날 때
     */
    public static int roundsFor(int players) {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "인원은 " + MIN_PLAYERS + "~" + MAX_PLAYERS + "명: " + players);
        }
        return Math.max(BASE_ROUNDS, players * 2);
    }

    /** 브레이크타임이 들어가는 라운드: 이 라운드가 끝난 뒤 (총 라운드의 절반) */
    public static int breakAfterRound(int totalRounds) {
        return totalRounds / 2;
    }

    /** 화면 표시용 금액 (예: 1000 → "1000만원") */
    public static String money(int amount) {
        return amount + MONEY_UNIT;
    }
}
