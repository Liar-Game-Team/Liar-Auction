package liarauction.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 게임 진행 뼈대: 플레이어, 덱, 현재 라운드와 단계.
 *
 * <p>서버가 게임 하나당 하나씩 들고 있는 상태다. 덱 생성과 단계 타이머(파트 A),
 * 아이템·소문·정산(파트 B)은 이 상태를 읽고 바꾼다.
 * 여러 스레드(클라이언트 핸들러, 타이머)가 접근하므로 메서드를 동기화한다.
 */
public final class GameState {

    private final List<PlayerState> players = new ArrayList<>();
    private List<Artwork> deck = List.of();
    /** 0이면 시작 전, 1부터 {@link #totalRounds()}까지 */
    private int round;
    private Phase phase = Phase.WAITING;

    /**
     * @throws IllegalStateException 인원이 다 찼거나 닉네임이 겹칠 때
     */
    public synchronized PlayerState addPlayer(String nickname) {
        if (players.size() >= GameConfig.MAX_PLAYERS) {
            throw new IllegalStateException("인원이 다 찼습니다");
        }
        if (findPlayer(nickname) != null) {
            throw new IllegalStateException("이미 있는 닉네임: " + nickname);
        }
        PlayerState p = new PlayerState(nickname);
        players.add(p);
        return p;
    }

    public synchronized void removePlayer(String nickname) {
        players.removeIf(p -> p.nickname().equals(nickname));
    }

    public synchronized List<PlayerState> players() {
        return List.copyOf(players);
    }

    public synchronized boolean isFull() {
        return players.size() >= GameConfig.MAX_PLAYERS;
    }

    /** 게임을 시작할 수 있는 인원인지 ({@link GameConfig#MIN_PLAYERS}명 이상) */
    public synchronized boolean hasEnoughPlayers() {
        return players.size() >= GameConfig.MIN_PLAYERS;
    }

    /** 없으면 null */
    public synchronized PlayerState findPlayer(String nickname) {
        for (PlayerState p : players) {
            if (p.nickname().equals(nickname)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 나 말고 첫 번째 플레이어, 없으면 null.
     * 2인 기준으로 만든 메서드라 3명 이상에서는 "상대"가 하나로 정해지지 않는다 (교란 대상 선택은 #35).
     */
    public synchronized PlayerState opponentOf(String nickname) {
        for (PlayerState p : players) {
            if (!p.nickname().equals(nickname)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 덱 설정 (파트 A의 덱 생성 결과). 작품 수 = 총 라운드 수.
     *
     * @throws IllegalArgumentException 작품 수가 지금 인원의 라운드 수와 다를 때
     */
    public synchronized void setDeck(List<Artwork> deck) {
        Objects.requireNonNull(deck, "deck");
        int expected = players.size() >= GameConfig.MIN_PLAYERS
                ? GameConfig.roundsFor(players.size())
                : GameConfig.BASE_ROUNDS;
        if (deck.size() != expected) {
            throw new IllegalArgumentException(
                    "작품 수 " + expected + "개 필요, 받은 개수 " + deck.size());
        }
        this.deck = List.copyOf(deck);
    }

    /** 총 라운드 수 (덱의 작품 수), 덱이 없으면 0 */
    public synchronized int totalRounds() {
        return deck.size();
    }

    /** 브레이크타임이 들어가는 라운드 (이 라운드가 끝난 뒤), 덱이 없으면 0 */
    public synchronized int breakAfterRound() {
        return GameConfig.breakAfterRound(deck.size());
    }

    public synchronized List<Artwork> deck() {
        return deck;
    }

    public synchronized int round() {
        return round;
    }

    public synchronized Phase phase() {
        return phase;
    }

    public synchronized void setPhase(Phase phase) {
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    /** 현재 라운드 작품, 시작 전이면 null */
    public synchronized Artwork currentArtwork() {
        if (round < 1 || round > deck.size()) {
            return null;
        }
        return deck.get(round - 1);
    }

    public synchronized boolean hasNextRound() {
        return round < deck.size();
    }

    /**
     * 다음 라운드로 넘어가고 단계를 {@link Phase#REVEAL}로 바꾼다.
     *
     * @return 이번 라운드 작품
     * @throws IllegalStateException 덱이 없거나 마지막 라운드가 끝났을 때
     */
    public synchronized Artwork startNextRound() {
        if (deck.isEmpty()) {
            throw new IllegalStateException("덱이 없습니다");
        }
        if (!hasNextRound()) {
            throw new IllegalStateException("마지막 라운드가 끝났습니다");
        }
        round++;
        phase = Phase.REVEAL;
        return deck.get(round - 1);
    }

    public synchronized void finish() {
        phase = Phase.GAME_OVER;
    }

    public synchronized boolean isOver() {
        return phase == Phase.GAME_OVER;
    }
}
