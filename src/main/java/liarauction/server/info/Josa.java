package liarauction.server.info;

/**
 * 한국어 조사 붙이기. 작품 제목·작가 이름이 바뀌어도 "이/가", "을/를" 등이 자연스럽게 붙도록 한다.
 * 받침이 있으면 앞의 것(이·을·은·과), 없으면 뒤의 것(가·를·는·와)을 붙인다.
 */
final class Josa {

    private Josa() {
    }

    /**
     * @param word 앞 단어
     * @param pair "이/가", "을/를", "은/는", "과/와" 형태
     * @return 단어 + 조사
     */
    static String attach(String word, String pair) {
        String[] options = pair.split("/");
        if (options.length != 2) {
            throw new IllegalArgumentException("조사는 '받침 있음/없음' 형태: " + pair);
        }
        return word + (hasFinalConsonant(word) ? options[0] : options[1]);
    }

    /** 마지막 글자에 받침이 있는지 (한글은 글자로, 숫자는 읽는 소리로 판단) */
    static boolean hasFinalConsonant(String word) {
        if (word.isEmpty()) {
            return false;
        }
        char last = word.charAt(word.length() - 1);
        if (last >= '가' && last <= '힣') {
            return (last - '가') % 28 != 0;
        }
        if (last >= '0' && last <= '9') {
            // 영 일 삼 육 칠 팔 → 받침 있음 / 이 사 오 구 → 없음
            return "013678".indexOf(last) >= 0;
        }
        // 영문 등은 대부분 받침 없이 읽힌다고 본다 (l, m, n은 받침으로 읽힘)
        return "lmnLMN".indexOf(last) >= 0;
    }
}
