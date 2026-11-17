package liarauction.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 플레이어 상태 (현금, 보유 작품).
 * 입찰 스레드와 타이머 스레드가 같이 건드릴 수 있어서 메서드를 동기화한다.
 */
public final class PlayerState {

    private final String nickname;
    private int cash;
    private final List<Artwork> artworks = new ArrayList<>();

    public PlayerState(String nickname, int startCash) {
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        if (startCash < 0) {
            throw new IllegalArgumentException("시작 현금이 음수: " + startCash);
        }
        this.cash = startCash;
    }

    public PlayerState(String nickname) {
        this(nickname, GameConfig.START_CASH);
    }

    public String nickname() {
        return nickname;
    }

    public synchronized int cash() {
        return cash;
    }

    public synchronized boolean canAfford(int amount) {
        return amount >= 0 && amount <= cash;
    }

    /**
     * 현금 지불.
     *
     * @throws IllegalStateException 현금이 부족할 때
     */
    public synchronized void pay(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("음수 금액: " + amount);
        }
        if (amount > cash) {
            throw new IllegalStateException(nickname + " 현금 부족: 보유 " + cash + ", 필요 " + amount);
        }
        cash -= amount;
    }

    public synchronized void earn(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("음수 금액: " + amount);
        }
        cash += amount;
    }

    public synchronized void addArtwork(Artwork artwork) {
        artworks.add(Objects.requireNonNull(artwork, "artwork"));
    }

    /** @return 가지고 있어서 뺐으면 true */
    public synchronized boolean removeArtwork(Artwork artwork) {
        return artworks.remove(artwork);
    }

    public synchronized boolean owns(Artwork artwork) {
        return artworks.contains(artwork);
    }

    /** 보유 작품 목록 (복사본) */
    public synchronized List<Artwork> artworks() {
        return List.copyOf(artworks);
    }

    /** 최종 자산 = 현금 + 보유 작품 실제 가치 합 (서버 전용) */
    public synchronized int totalAssets() {
        int sum = cash;
        for (Artwork a : artworks) {
            sum += a.realValue();
        }
        return sum;
    }

    @Override
    public synchronized String toString() {
        return nickname + "(현금 " + cash + ", 작품 " + artworks.size() + "점)";
    }
}
