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
    /** 인자 하나에 값 여러 개를 담을 때 쓰는 구분자 (닉네임·방 이름에는 쓸 수 없다) */
    public static final String LIST_SEPARATOR = ",";
    /** {@link #ROOM_LIST}에서 방과 방을 나누는 구분자 (방 이름에는 쓸 수 없다) */
    public static final String ROOM_SEPARATOR = ";";

    // ===== Client -> Server =====

    /** {@code REQ_LOGIN|nickname} 대기실 입장, 닉네임 등록 */
    public static final String REQ_LOGIN = "REQ_LOGIN";
    /** {@code REQ_CHAT|message} 협상 채팅 */
    public static final String REQ_CHAT = "REQ_CHAT";
    /** {@code REQ_BID|increment} 현재가에 더할 금액 (10 / 50 / 100) */
    public static final String REQ_BID = "REQ_BID";
    /** {@code REQ_USE_ITEM|item} 공개 단계에서 아이템 사용 */
    public static final String REQ_USE_ITEM = "REQ_USE_ITEM";
    /** {@code REQ_ROOM_LIST} 방 목록 다시 받기 (목록은 바뀔 때마다 자동으로도 온다) */
    public static final String REQ_ROOM_LIST = "REQ_ROOM_LIST";
    /** {@code REQ_CREATE_ROOM|name|maxPlayers|password} 방 만들기, 만든 사람이 방장. 비밀번호가 빈 문자열이면 공개방 */
    public static final String REQ_CREATE_ROOM = "REQ_CREATE_ROOM";
    /** {@code REQ_JOIN_ROOM|roomId|password} 방 들어가기 (공개방이면 비밀번호는 빈 문자열) */
    public static final String REQ_JOIN_ROOM = "REQ_JOIN_ROOM";
    /** {@code REQ_QUICK_JOIN} 자리가 남은 대기 중 공개방에 바로 들어가기 */
    public static final String REQ_QUICK_JOIN = "REQ_QUICK_JOIN";
    /** {@code REQ_LEAVE_ROOM} 방 나가기, 방 목록으로 */
    public static final String REQ_LEAVE_ROOM = "REQ_LEAVE_ROOM";
    /** {@code REQ_READY|true or false} 방에서 준비 / 준비 취소 (방장은 필요 없음) */
    public static final String REQ_READY = "REQ_READY";
    /** {@code REQ_START} 방장만, 2명 이상이고 나머지가 모두 준비했을 때 게임 시작 */
    public static final String REQ_START = "REQ_START";

    // ===== Server -> Client =====

    /** {@code LOGIN_OK|nickname} 로그인 성공, 방 목록 화면으로 */
    public static final String LOGIN_OK = "LOGIN_OK";
    /** {@code LOGIN_FAIL|reason} 로그인 실패 (닉네임 중복, 방 가득 참, 게임 진행 중 등) */
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    /**
     * {@code ROOM_LIST|rooms} 방 목록 (방 목록 화면에 있는 사람에게). 방마다
     * {@code id,name,count,maxPlayers,locked,playing}을 {@link #LIST_SEPARATOR}로 잇고,
     * 방과 방은 {@link #ROOM_SEPARATOR}로 잇는다. 방이 없으면 빈 문자열
     */
    public static final String ROOM_LIST = "ROOM_LIST";
    /** {@code ROOM_JOINED|roomId|name|maxPlayers|locked} 방에 들어감 (만들거나 들어가거나 빠른 입장) */
    public static final String ROOM_JOINED = "ROOM_JOINED";
    /** {@code ROOM_LEFT} 방에서 나와 방 목록 화면으로 */
    public static final String ROOM_LEFT = "ROOM_LEFT";
    /**
     * {@code ROOM_STATE|host|players|readyPlayers} 방 대기 상태. 입장·퇴장·준비가 바뀔 때마다,
     * 게임이 끝나 방으로 돌아올 때 전송. players·readyPlayers는 입장 순서대로
     * {@link #LIST_SEPARATOR}로 이은 닉네임 (준비한 사람이 없으면 빈 문자열)
     */
    public static final String ROOM_STATE = "ROOM_STATE";
    /** {@code GAME_START|players|startCash|totalRounds} 게임 시작. players는 입장 순서대로 쉼표로 이은 닉네임 */
    public static final String GAME_START = "GAME_START";
    /** {@code BROADCAST_CHAT|sender|message} 채팅. 방 목록 화면이면 방 목록에 있는 사람끼리, 방 안이면 그 방 사람끼리 */
    public static final String BROADCAST_CHAT = "BROADCAST_CHAT";
    /** {@code START_ROUND|roundNum|title|artist|startPrice} 라운드 시작, 작품 공개 */
    public static final String START_ROUND = "START_ROUND";
    /** {@code PHASE|phaseName|seconds} 단계 전환 ({@link Phase} 이름), 남은 시간 */
    public static final String PHASE = "PHASE";
    /** {@code ITEMS|certificates|jammers} 아이템 보유 수 */
    public static final String ITEMS = "ITEMS";
    /** {@code ITEM_RESULT|item|infoText} 개인 아이템 사용 결과 */
    public static final String ITEM_RESULT = "ITEM_RESULT";
    /** {@code MISSION|text} 히든 미션, 본인에게만 (후반 첫 라운드에 1인당 1개) */
    public static final String MISSION = "MISSION";
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
    /** {@code PLAYER_LEFT|nickname} 게임 중 플레이어 접속 끊김. 2명 이상 남으면 게임은 계속된다 */
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    /**
     * {@code RESULT_PLAYER|rank|nickname|cash|artworkValue|artworkCount|bonus|total|mission|missionSuccess}
     * 최종 순위 한 줄 (순위 순서대로, 같은 점수는 같은 순위). 모두 보낸 뒤 {@link #GAME_OVER}
     */
    public static final String RESULT_PLAYER = "RESULT_PLAYER";
    /** {@code RESULT_ARTWORK|round|title|artist|grade|value|owner} 작품 진위 공개 한 줄 (유찰이면 owner = 유찰) */
    public static final String RESULT_ARTWORK = "RESULT_ARTWORK";
    /** {@code GAME_OVER|winner|details} 최종 결과 (결과 줄을 다 보낸 뒤), 1등이 여러 명이면 winner = {@link #DRAW} */
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
            Map.entry(REQ_ROOM_LIST, 0),
            Map.entry(REQ_CREATE_ROOM, 3),
            Map.entry(REQ_JOIN_ROOM, 2),
            Map.entry(REQ_QUICK_JOIN, 0),
            Map.entry(REQ_LEAVE_ROOM, 0),
            Map.entry(REQ_READY, 1),
            Map.entry(REQ_START, 0),
            Map.entry(LOGIN_OK, 1),
            Map.entry(LOGIN_FAIL, 1),
            Map.entry(ROOM_LIST, 1),
            Map.entry(ROOM_JOINED, 4),
            Map.entry(ROOM_LEFT, 0),
            Map.entry(ROOM_STATE, 3),
            Map.entry(GAME_START, 3),
            Map.entry(BROADCAST_CHAT, 2),
            Map.entry(START_ROUND, 4),
            Map.entry(PHASE, 2),
            Map.entry(ITEMS, 2),
            Map.entry(ITEM_RESULT, 2),
            Map.entry(RUMOR, 1),
            Map.entry(MISSION, 1),
            Map.entry(UPDATE_BID, 2),
            Map.entry(BID_REJECT, 1),
            Map.entry(ROUND_END, 2),
            Map.entry(UPDATE_CASH, 2),
            Map.entry(PLAYER_LEFT, 1),
            Map.entry(RESULT_PLAYER, 9),
            Map.entry(RESULT_ARTWORK, 6),
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
