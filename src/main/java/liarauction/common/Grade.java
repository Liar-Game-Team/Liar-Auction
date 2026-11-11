package liarauction.common;

/**
 * 작품 등급. 진위·등급은 서버만 알고, 클라이언트에는 게임 종료 때 공개한다.
 * 등급별 개수의 합은 {@link GameConfig#TOTAL_ROUNDS}와 같아야 한다.
 */
public enum Grade {
    MASTERPIECE("명작", 2, 400, 600),
    NORMAL("보통", 3, 150, 250),
    FAKE("가짜", 3, 0, 0);

    private final String label;
    private final int count;
    private final int minValue;
    private final int maxValue;

    Grade(String label, int count, int minValue, int maxValue) {
        this.label = label;
        this.count = count;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /** 화면 표시용 이름 */
    public String label() {
        return label;
    }

    /** 덱에 들어가는 개수 */
    public int count() {
        return count;
    }

    public int minValue() {
        return minValue;
    }

    public int maxValue() {
        return maxValue;
    }

    /** 진품 여부 (가짜만 false) */
    public boolean isGenuine() {
        return this != FAKE;
    }

    public boolean isValidValue(int value) {
        return value >= minValue && value <= maxValue;
    }
}
