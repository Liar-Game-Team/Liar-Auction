package liarauction.common;

/** Consumable items available to each player during the reveal phase. */
public enum Item {
    CERTIFICATE,
    JAMMER;

    public static Item parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 아이템: " + value, e);
        }
    }
}
