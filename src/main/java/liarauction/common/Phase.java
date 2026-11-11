package liarauction.common;

/**
 * 라운드 단계. 순서: 공개 → 감정 → 협상 → 경매 → 되팔기.
 * {@code PHASE} 메시지의 {@code phaseName}은 이 enum의 {@link #name()}이다.
 */
public enum Phase {
    /** 게임 시작 전 (상대 대기) */
    WAITING("대기", 0),
    REVEAL("공개", 3),
    INSPECTION("감정", 5),
    NEGOTIATION("협상", 30),
    /** 최대 시간. 입찰이 끊기면 {@link GameConfig#BID_IDLE_SECONDS} 뒤 낙찰 */
    AUCTION("경매", 20),
    RESALE("되팔기", 15),
    /** 게임 종료 */
    GAME_OVER("종료", 0);

    private final String label;
    private final int seconds;

    Phase(String label, int seconds) {
        this.label = label;
        this.seconds = seconds;
    }

    /** 화면 표시용 이름 */
    public String label() {
        return label;
    }

    /** 기본 진행 시간(초) */
    public int seconds() {
        return seconds;
    }

    /** 라운드 안에서 다음 단계. 되팔기 다음은 null (다음 라운드 또는 종료). */
    public Phase nextInRound() {
        return switch (this) {
            case REVEAL -> INSPECTION;
            case INSPECTION -> NEGOTIATION;
            case NEGOTIATION -> AUCTION;
            case AUCTION -> RESALE;
            default -> null;
        };
    }
}
