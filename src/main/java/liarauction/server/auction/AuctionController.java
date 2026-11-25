package liarauction.server.auction;

import java.util.Objects;
import liarauction.common.Artwork;
import liarauction.common.GameState;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import liarauction.server.ArtworkDeckFactory;
import liarauction.server.AuctionBidManager;
import liarauction.server.AuctionTiming;
import liarauction.server.GameRoom;
import liarauction.server.RoundPhaseTimer;

/**
 * 파트 A: 경매 진행 (작품 덱 생성, 라운드 단계 타이머, 실시간 입찰 판정, 낙찰/유찰).
 *
 * <p>{@link GameRoom}이 요청을 전달하고, 타이머 콜백은 방 lock을 사용해 상태와 메시지를 갱신한다.
 *
 * <p>파트 B로 넘기는 연결 지점
 * <ol>
 *   <li>라운드 시작 → {@code room.info().onRoundStart(artwork)}</li>
 *   <li>소문 단계 → {@code room.info().onRumorPhase(artwork)}</li>
 *   <li>게임 종료 → {@code room.info().onGameOver()}</li>
 * </ol>
 */
public final class AuctionController {

    private static final int STARTING_PRICE = 100;

    private final GameRoom room;
    private final AuctionTiming timing;
    private RoundPhaseTimer roundTimer;
    private AuctionBidManager bidManager;

    public AuctionController(GameRoom room) {
        this(room, AuctionTiming.defaults());
    }

    public AuctionController(GameRoom room, AuctionTiming timing) {
        this.room = Objects.requireNonNull(room, "room");
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    /** 두 명이 모여 게임이 시작됐을 때 (GAME_START 전송 직후) */
    public void onGameStart() {
        GameState game = room.state();
        game.setDeck(ArtworkDeckFactory.createDeck());
        bidManager = new AuctionBidManager(game, timing.bidIdle(), new AuctionBidManager.Listener() {
            @Override
            public void onAuctionStarted(String title, String artist, int startingPrice) {
            }

            @Override
            public void onBidUpdated(String highestBidder, int currentPrice) {
                room.broadcast(Message.of(Protocol.UPDATE_BID, highestBidder, currentPrice));
            }

            @Override
            public void onBidRejected(String nickname, String reason) {
                room.sendTo(nickname, Message.of(Protocol.BID_REJECT, reason));
            }

            @Override
            public void onAuctionEnded(String winner, int price) {
                synchronized (room) {
                    room.broadcast(Message.of(Protocol.ROUND_END, winner, price));
                }
                roundTimer.requestAuctionClose();
            }

            @Override
            public void onCashChanged(String nickname, int cash) {
                room.broadcast(Message.of(Protocol.UPDATE_CASH, nickname, cash));
            }

            @Override
            public void onOpponentLeft(String nickname) {
                room.broadcast(Message.of(Protocol.OPPONENT_LEFT, nickname));
            }

            @Override
            public void onGameFinished() {
            }
        });
        roundTimer = new RoundPhaseTimer(game, timing, new RoundPhaseTimer.Listener() {
            @Override
            public void onRoundStarted(int round, String title, String artist) {
                synchronized (room) {
                    room.broadcast(Message.of(
                            Protocol.START_ROUND, round, title, artist, STARTING_PRICE));
                    room.info().onRoundStart(game.currentArtwork());
                }
            }

            @Override
            public void onPhaseChanged(Phase phase, int secondsRemaining) {
                synchronized (room) {
                    if (phase == Phase.AUCTION) {
                        bidManager.openAuction(game.currentArtwork(), STARTING_PRICE);
                    }
                    room.broadcast(Message.of(Protocol.PHASE, phase.name(), secondsRemaining));
                    if (phase == Phase.RUMOR) {
                        room.info().onRumorPhase(game.currentArtwork());
                    }
                }
            }

            @Override
            public void onTimeRemaining(int secondsRemaining) {
                synchronized (room) {
                    room.broadcast(Message.of(Protocol.PHASE, game.phase().name(), secondsRemaining));
                }
            }

            @Override
            public void onAuctionClosed(Artwork artwork) {
                bidManager.closeAuction();
            }

            @Override
            public void onGameFinished() {
                synchronized (room) {
                    room.info().onGameOver();
                }
                bidManager.close();
            }
        });
        roundTimer.start();
    }

    /** {@code REQ_BID|increment} */
    public void onBid(PlayerState player, int increment) {
        if (player == null || bidManager == null) {
            throw new IllegalStateException("입찰할 수 있는 게임이 없습니다");
        }
        bidManager.placeBid(player.nickname(), increment);
    }

    /** 상대가 나가서 게임이 중단됐을 때. 타이머를 멈춘다. */
    public void onGameStopped() {
        if (roundTimer != null) {
            roundTimer.close();
            roundTimer = null;
        }
        if (bidManager != null) {
            bidManager.close();
            bidManager = null;
        }
    }
}
