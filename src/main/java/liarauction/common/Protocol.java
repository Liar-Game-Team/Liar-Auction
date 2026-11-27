package liarauction.common;

import java.util.Map;

/**
 * 클라이언트·서버가 주고받는 메시지 이름과 인자 개수.
 *
 * <p>한 줄에 메시지 하나, {@code COMMAND|ARG1|ARG2...} 형식이다.
 * 인자 안의 {@code |}, {@code \}, 줄바꿈은 {@link Message}가 이스케이프한다.
 * 메시지를 추가·변경할 때는 README 프로토콜 표도 같이 고친다.
 */
public final class Protocol {

    private Protocol() {
    }

    /** 구분자. */
    public static final char DELIMITER = '|';

    // ===== Client -> Server =====

    /** {@code REQ_LOGIN|nickname} 대기실 입장, 닉네임 등록 */
    public static final String REQ_LOGIN = "REQ_LOGIN";
    /** {@code REQ_CHAT|message} 협상 채팅 */
    public static final String REQ_CHAT = "REQ_CHAT";
    /** {@code REQ_BID|increment} 현재가에 더할 금액 (10 / 50 / 100) */
    public static final String REQ_BID = "REQ_BID";
    /** {@code REQ_USE_ITEM|item} 공개 단계에서 아이템 사용 */
    public static final String REQ_USE_ITEM = "REQ_USE_ITEM";

    // ===== Server -> Client =====

    /** {@code LOGIN_OK|nickname} 로그인 성공 (상대가 들어올 때까지 대기) */
    public static final String LOGIN_OK = "LOGIN_OK";
    /** {@code LOGIN_FAIL|reason} 로그인 실패 (닉네임 중복, 방 가득 참 등) */
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    /** {@code GAME_START|opponent|startCash|totalRounds} 두 명이 모여 게임 시작 */
    public static final String GAME_START = "GAME_START";
    /** {@code BROADCAST_CHAT|sender|message} 채팅 브로드캐스트 */
    public static final String BROADCAST_CHAT = "BROADCAST_CHAT";
    /** {@code START_ROUND|roundNum|title|artist|startPrice} 라운드 시작, 작품 공개 */
    public static final String START_ROUND = "START_ROUND";
    /** {@code PHASE|phaseName|seconds} 단계 전환 ({@link Phase} 이름), 남은 시간 */
    public static final String PHASE = "PHASE";
    /** {@code ITEMS|certificates|jammers} 아이템 보유 수 */
    public static final String ITEMS = "ITEMS";
    /** {@code ITEM_RESULT|item|infoText} 개인 아이템 사용 결과 */
    public static final String ITEM_RESULT = "ITEM_RESULT";
    /** {@code RUMOR|infoText} 플레이어별 소문 */
    public static final String RUMOR = "RUMOR";
    /** {@code UPDATE_BID|highestBidder|currentPrice} 실시간 입찰 갱신 */
    public static final String UPDATE_BID = "UPDATE_BID";
    /** {@code BID_REJECT|reason} 입찰 거부 (현금 부족 등) */
    public static final String BID_REJECT = "BID_REJECT";
    /** {@code ROUND_END|winner|price} 낙찰 결과, 유찰이면 winner = {@link #NONE} */
    public static final String ROUND_END = "ROUND_END";
    /** {@code UPDATE_CASH|nickname|cash} 플레이어 현금 변경 */
    public static final String UPDATE_CASH = "UPDATE_CASH";
    /** {@code OPPONENT_LEFT|nickname} 상대 접속 끊김 */
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";
    /** {@code GAME_OVER|winner|details} 최종 결과, 무승부면 winner = {@link #DRAW} */
    public static final String GAME_OVER = "GAME_OVER";
    /** {@code ERROR|reason} 잘못된 요청 등 */
    public static final String ERROR = "ERROR";

    // ===== 인자 값 =====

    /** 유찰 (낙찰자 없음) */
    public static final String NONE = "NONE";
    /** 무승부 */
    public static final String DRAW = "DRAW";

    private static final Map<String, Integer> ARG_COUNTS = Map.ofEntries(
            Map.entry(REQ_LOGIN, 1),
            Map.entry(REQ_CHAT, 1),
            Map.entry(REQ_BID, 1),
            Map.entry(REQ_USE_ITEM, 1),
            Map.entry(LOGIN_OK, 1),
            Map.entry(LOGIN_FAIL, 1),
            Map.entry(GAME_START, 3),
            Map.entry(BROADCAST_CHAT, 2),
            Map.entry(START_ROUND, 4),
            Map.entry(PHASE, 2),
            Map.entry(ITEMS, 2),
            Map.entry(ITEM_RESULT, 2),
            Map.entry(RUMOR, 1),
            Map.entry(UPDATE_BID, 2),
            Map.entry(BID_REJECT, 1),
            Map.entry(ROUND_END, 2),
            Map.entry(UPDATE_CASH, 2),
            Map.entry(OPPONENT_LEFT, 1),
            Map.entry(GAME_OVER, 2),
            Map.entry(ERROR, 1));

    /** 정의된 메시지인지. */
    public static boolean isKnown(String command) {
        return ARG_COUNTS.containsKey(command);
    }

    /** 메시지의 인자 개수, 모르는 메시지면 -1. */
    public static int argCount(String command) {
        return ARG_COUNTS.getOrDefault(command, -1);
    }
}
