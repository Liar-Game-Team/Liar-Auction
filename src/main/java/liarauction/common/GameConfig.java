package liarauction.common;

import java.util.List;

/** 게임 설정값. 밸런스 조정은 여기서 한다. */
public final class GameConfig {

    private GameConfig() {
    }

    /** 기본 서버 포트 */
    public static final int DEFAULT_PORT = 5000;
    /** 한 게임 인원 (1:1) */
    public static final int MAX_PLAYERS = 2;
    /** 총 라운드 수 (= 작품 수) */
    public static final int TOTAL_ROUNDS = 8;
    /** 시작 현금 */
    public static final int START_CASH = 1000;
    /** 입찰 버튼 금액 */
    public static final List<Integer> BID_INCREMENTS = List.of(10, 50, 100);
    /** 이 시간 동안 추가 입찰이 없으면 낙찰 (입찰 시 리셋) */
    public static final int BID_IDLE_SECONDS = 5;
    /** 닉네임 최대 길이 */
    public static final int MAX_NICKNAME_LENGTH = 12;

    public static boolean isValidBidIncrement(int increment) {
        return BID_INCREMENTS.contains(increment);
    }
}
