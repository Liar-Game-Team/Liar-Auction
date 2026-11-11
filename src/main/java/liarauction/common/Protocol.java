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
    /** {@code REQ_APPRAISE} 정밀 감정 요청 (게임당 1회) */
    public static final String REQ_APPRAISE = "REQ_APPRAISE";
    /** {@code REQ_RESALE|price} 낙찰 작품 되팔기 제안 */
    public static final String REQ_RESALE = "REQ_RESALE";
    /** {@code RESALE_ANS|ACCEPT or REJECT} 되팔기 제안 응답 */
    public static final String RESALE_ANS = "RESALE_ANS";

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
    /** {@code INSPECTION|isOfficial|infoText} 감정서(true)/소문(false), 플레이어별 개별 전송 */
    public static final String INSPECTION = "INSPECTION";
    /** {@code APPRAISE_RESULT|infoText} 정밀 감정 결과, 요청자에게만 */
    public static final String APPRAISE_RESULT = "APPRAISE_RESULT";
    /** {@code UPDATE_BID|highestBidder|currentPrice} 실시간 입찰 갱신 */
    public static final String UPDATE_BID = "UPDATE_BID";
    /** {@code BID_REJECT|reason} 입찰 거부 (현금 부족 등) */
    public static final String BID_REJECT = "BID_REJECT";
    /** {@code ROUND_END|winner|price} 낙찰 결과, 유찰이면 winner = {@link #NONE} */
    public static final String ROUND_END = "ROUND_END";
    /** {@code RESALE_OFFER|from|price} 상대의 되팔기 제안 */
    public static final String RESALE_OFFER = "RESALE_OFFER";
    /** {@code RESALE_RESULT|ACCEPT or REJECT|price} 되팔기 성사 여부 */
    public static final String RESALE_RESULT = "RESALE_RESULT";
    /** {@code UPDATE_CASH|nickname|cash} 플레이어 현금 변경 */
    public static final String UPDATE_CASH = "UPDATE_CASH";
    /** {@code OPPONENT_LEFT|nickname} 상대 접속 끊김 */
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";
    /** {@code GAME_OVER|winner|details} 최종 결과, 무승부면 winner = {@link #DRAW} */
    public static final String GAME_OVER = "GAME_OVER";
    /** {@code ERROR|reason} 잘못된 요청 등 */
    public static final String ERROR = "ERROR";

    // ===== 인자 값 =====

    public static final String ACCEPT = "ACCEPT";
    public static final String REJECT = "REJECT";
    /** 유찰 (낙찰자 없음) */
    public static final String NONE = "NONE";
    /** 무승부 */
    public static final String DRAW = "DRAW";

    private static final Map<String, Integer> ARG_COUNTS = Map.ofEntries(
            Map.entry(REQ_LOGIN, 1),
            Map.entry(REQ_CHAT, 1),
            Map.entry(REQ_BID, 1),
            Map.entry(REQ_APPRAISE, 0),
            Map.entry(REQ_RESALE, 1),
            Map.entry(RESALE_ANS, 1),
            Map.entry(LOGIN_OK, 1),
            Map.entry(LOGIN_FAIL, 1),
            Map.entry(GAME_START, 3),
            Map.entry(BROADCAST_CHAT, 2),
            Map.entry(START_ROUND, 4),
            Map.entry(PHASE, 2),
            Map.entry(INSPECTION, 2),
            Map.entry(APPRAISE_RESULT, 1),
            Map.entry(UPDATE_BID, 2),
            Map.entry(BID_REJECT, 1),
            Map.entry(ROUND_END, 2),
            Map.entry(RESALE_OFFER, 2),
            Map.entry(RESALE_RESULT, 2),
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
