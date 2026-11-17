package liarauction.common;

import java.util.Objects;

/**
 * 작품. {@link #grade()}와 {@link #realValue()}는 서버만 보관하는 정보라서
 * 클라이언트로 보낼 때는 제목·작가만 보낸다 ({@code START_ROUND}).
 */
public final class Artwork {

    private final int id;
    private final String title;
    private final String artist;
    private final Grade grade;
    private final int realValue;

    /**
     * @param id 게임 안에서 겹치지 않는 번호
     * @throws IllegalArgumentException 실제 가치가 등급 범위를 벗어날 때
     */
    public Artwork(int id, String title, String artist, Grade grade, int realValue) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title");
        this.artist = Objects.requireNonNull(artist, "artist");
        this.grade = Objects.requireNonNull(grade, "grade");
        if (!grade.isValidValue(realValue)) {
            throw new IllegalArgumentException(
                    grade + " 가치 범위 " + grade.minValue() + "~" + grade.maxValue() + " 밖: " + realValue);
        }
        this.realValue = realValue;
    }

    public int id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String artist() {
        return artist;
    }

    /** 서버 전용 */
    public Grade grade() {
        return grade;
    }

    /** 서버 전용 */
    public int realValue() {
        return realValue;
    }

    /** 서버 전용 */
    public boolean isGenuine() {
        return grade.isGenuine();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Artwork a && id == a.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** 로그용. 등급·가치는 넣지 않는다 (실수로 클라이언트에 보내지 않도록). */
    @Override
    public String toString() {
        return "Artwork#" + id + "(" + title + " / " + artist + ")";
    }
}
