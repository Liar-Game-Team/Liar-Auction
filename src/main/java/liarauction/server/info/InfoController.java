package liarauction.server.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.Message;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import liarauction.server.GameRoom;

/**
 * 파트 B: 정보·거래 (감정서·소문, 정밀 감정, 되팔기, 최종 정산).
 *
 * <p>{@link GameRoom}과 파트 A({@code AuctionController})가 아래 메서드를 방 lock 안에서 부른다.
 * 감정 정보는 {@code room.sendTo(nickname, ...)}로 플레이어별로 따로 보낸다 (진위는 서버만 보관).
 */
public final class InfoController {

    private final GameRoom room;
    private final InspectionGenerator inspections;
    /** 이번 라운드에 각 플레이어가 받은 감정 정보 (닉네임 → 정보) */
    private final Map<String, Inspection> roundInspections = new HashMap<>();

    public InfoController(GameRoom room) {
        this(room, new InspectionGenerator());
    }

    public InfoController(GameRoom room, InspectionGenerator inspections) {
        this.room = room;
        this.inspections = inspections;
    }

    /**
     * 연결 지점 1: 라운드 시작 (파트 A가 {@code state.startNextRound()} 다음에 부름).
     * 감정서·소문을 만들어 각 플레이어에게 자기 것만 보낸다 (#8).
     */
    public void onRoundStart(Artwork artwork) {
        int round = room.state().round();
        if (round < 1) {
            throw new IllegalStateException("라운드 시작 전입니다");
        }
        List<PlayerState> players = room.state().players();
        roundInspections.clear();
        for (int i = 0; i < players.size(); i++) {
            String nickname = players.get(i).nickname();
            Inspection insp = inspections.generate(artwork, isOfficialHolder(i, round));
            roundInspections.put(nickname, insp);
            room.sendTo(nickname, Message.of(Protocol.INSPECTION, insp.official(), insp.text()));
        }
    }

    /**
     * 감정서 담당 교대: 1라운드는 먼저 들어온 플레이어(0번)가 감정서, 매 라운드 바뀐다.
     *
     * @param playerIndex 입장 순서 (0, 1)
     * @param round 1부터
     */
    static boolean isOfficialHolder(int playerIndex, int round) {
        return playerIndex == (round - 1) % GameConfig.MAX_PLAYERS;
    }

    /** 이번 라운드에 그 플레이어가 받은 감정 정보, 없으면 null */
    public Inspection inspectionOf(String nickname) {
        return roundInspections.get(nickname);
    }

    /** 연결 지점 2: 낙찰 (파트 A가 부름). 되팔기 단계 준비 */
    public void onAuctionWon(PlayerState winner, Artwork artwork, int price) {
        // TODO(#12): 되팔기 단계 준비
    }

    /** 연결 지점 3: 마지막 라운드 종료 (파트 A가 부름) */
    public void onGameOver() {
        // TODO(#19): 최종 정산, GAME_OVER 전송
    }

    /** {@code REQ_APPRAISE} */
    public void onAppraise(PlayerState player) {
        // TODO(#18): 정밀 감정 (게임당 1회), APPRAISE_RESULT
        throw new IllegalStateException("정밀 감정은 아직 구현되지 않았습니다");
    }

    /** {@code REQ_RESALE|price} */
    public void onResaleOffer(PlayerState player, int price) {
        // TODO(#12): 되팔기 제안 → 상대에게 RESALE_OFFER
        throw new IllegalStateException("되팔기는 아직 구현되지 않았습니다");
    }

    /** {@code RESALE_ANS|ACCEPT or REJECT} */
    public void onResaleAnswer(PlayerState player, boolean accept) {
        // TODO(#12): 수락 시 현금·작품 이동, RESALE_RESULT / UPDATE_CASH
        throw new IllegalStateException("되팔기는 아직 구현되지 않았습니다");
    }

    /** 상대가 나가서 게임이 중단됐을 때 */
    public void onGameStopped() {
        roundInspections.clear();
    }
}
