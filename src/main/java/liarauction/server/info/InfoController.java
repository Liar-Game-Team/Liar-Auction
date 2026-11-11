package liarauction.server.info;

import liarauction.common.Artwork;
import liarauction.common.PlayerState;
import liarauction.server.GameRoom;

/**
 * 파트 B: 정보·거래 (감정서·소문, 정밀 감정, 되팔기, 최종 정산).
 *
 * <p>{@link GameRoom}과 파트 A({@code AuctionController})가 아래 메서드를 방 lock 안에서 부른다.
 * 감정 정보는 {@code room.sendTo(nickname, ...)}로 플레이어별로 따로 보낸다 (진위는 서버만 보관).
 */
public final class InfoController {

    private final GameRoom room;

    public InfoController(GameRoom room) {
        this.room = room;
    }

    /** 연결 지점 1: 라운드 시작 (파트 A가 부름) */
    public void onRoundStart(Artwork artwork) {
        // TODO(#7, #8): 감정서·소문 생성, 담당 교대, INSPECTION 개별 전송
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
        // 필요하면 진행 중인 되팔기 등 정리
    }
}
