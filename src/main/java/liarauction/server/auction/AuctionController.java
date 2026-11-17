package liarauction.server.auction;

import liarauction.common.PlayerState;
import liarauction.server.GameRoom;

/**
 * 파트 A: 경매 진행 (작품 덱 생성, 라운드 단계 타이머, 실시간 입찰 판정, 낙찰/유찰).
 *
 * <p>{@link GameRoom}이 아래 메서드를 방 lock 안에서 부른다.
 * 타이머 스레드에서 상태를 바꿀 때는 {@code synchronized (room) { ... }}로 감싼다.
 *
 * <p>파트 B로 넘기는 연결 지점
 * <ol>
 *   <li>라운드 시작 → {@code room.info().onRoundStart(artwork)}</li>
 *   <li>낙찰 → {@code room.info().onAuctionWon(winner, artwork, price)}</li>
 *   <li>게임 종료 → {@code room.info().onGameOver()}</li>
 * </ol>
 */
public final class AuctionController {

    private final GameRoom room;

    public AuctionController(GameRoom room) {
        this.room = room;
    }

    /** 두 명이 모여 게임이 시작됐을 때 (GAME_START 전송 직후) */
    public void onGameStart() {
        // TODO(#5): 덱 생성 → room.state().setDeck(...)
        // TODO(#6): 라운드 단계 타이머 시작, START_ROUND / PHASE 전송
    }

    /** {@code REQ_BID|increment} */
    public void onBid(PlayerState player, int increment) {
        // TODO(#10): 입찰 판정, UPDATE_BID / BID_REJECT
        throw new IllegalStateException("입찰은 아직 구현되지 않았습니다");
    }

    /** 상대가 나가서 게임이 중단됐을 때. 타이머를 멈춘다. */
    public void onGameStopped() {
        // TODO(#6): 타이머 정지
    }
}
