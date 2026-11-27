package liarauction.server.info;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.Grade;

/**
 * 감정서·소문 문장 모음 (#36). 소문은 진행자가 흘리는 이야기처럼, 감정서는 공식 문서처럼 쓴다.
 *
 * <p>문장 안의 자리 표시:
 * <ul>
 *   <li>{@code {title}}, {@code {artist}}: 작품 제목, 작가</li>
 *   <li>{@code {title:을/를}}, {@code {artist:이/가}} 등: 받침에 맞춰 조사를 붙인 제목·작가</li>
 *   <li>{@code {lo}}, {@code {hi}}: 감정서 예상 가치 범위 (만원 단위가 붙는다)</li>
 * </ul>
 * 문장에는 실제 가치를 직접 쓰지 않는다. 문장을 늘릴 때는 등급별 목록에 줄만 추가하면 된다.
 */
final class InspectionTexts {

    private InspectionTexts() {
    }

    /** 소문: 이 등급이라고 주장하는 문장들 */
    static final Map<Grade, List<String>> RUMORS = Map.of(
            Grade.MASTERPIECE, List.of(
                    "{artist:이/가} 세상을 떠나기 직전에 그린 마지막 작품이라는 소문이 돌아요.",
                    "{title}, 경매 전날 유명 미술관 관장이 몰래 보러 왔다던데요?",
                    "{artist}의 화실 바닥 밑에서 발견됐다는 얘기가 있어요. 진짜라면 대박이죠.",
                    "{title}의 붓 터치가 {artist}의 전성기 작품이랑 똑같다는 말이 있어요.",
                    "어느 집안이 대대로 숨겨 온 작품이라는 소문이 있습니다.",
                    "{artist:이/가} 가장 친한 친구에게만 보여 줬다는 비밀 작품이라던데요.",
                    "해외 수집가들이 {title:을/를} 노리고 일부러 왔다는 얘기가 들려요.",
                    "{title:을/를} 처음 본 감정사가 손을 떨었다는 소문이 있어요."),
            Grade.NORMAL, List.of(
                    "{artist}의 작품은 맞는데, 연습 삼아 그린 습작에 가깝다는 얘기가 있어요.",
                    "{title}, 진품이긴 해도 특별할 건 없다는 평이 많아요.",
                    "{artist}의 제자가 같이 그렸다는 소문이 있어요.",
                    "전시회에 몇 번 걸렸던 평범한 진품이라던데요.",
                    "{artist:이/가} 생활비를 벌려고 빠르게 그린 그림이라는 얘기가 있어요.",
                    "보관 상태는 좋은데 작품성은 그저 그렇다는 말이 돌아요.",
                    "{title}, 수집가들 사이에서는 무난한 작품으로 통한대요."),
            Grade.FAKE, List.of(
                    "{title}의 캔버스가 {artist:이/가} 살던 시대에는 없던 재료라는 소문이 있어요.",
                    "유명한 위작 화가가 만든 작품이라는 얘기가 돌아요.",
                    "{artist}의 서명 획이 어딘가 어색하다는 말이 있어요.",
                    "{title}, 몇 년 전 다른 경매에서 가짜로 판정받았다던데요?",
                    "물감이 너무 새것 같다는 소문이 있어요.",
                    "{artist:은/는} 이런 색을 쓴 적이 없다는 얘기가 있어요.",
                    "뒷골목 화방에서 똑같은 그림을 봤다는 사람이 있어요."));

    /** 감정서: 이 등급으로 판정하는 문장들 */
    static final Map<Grade, List<String>> CERTIFICATES = Map.of(
            Grade.MASTERPIECE, List.of(
                    "감정 결과: 진품. {artist}의 대표작 수준으로 판단됩니다. 예상 가치 {lo}~{hi}.",
                    "감정 결과: 진품. 붓 터치와 재료가 {artist}의 전성기와 일치합니다. 예상 가치 {lo}~{hi}.",
                    "감정 결과: 진품, 명작급. 보존 상태가 뛰어납니다. 예상 가치 {lo}~{hi}."),
            Grade.NORMAL, List.of(
                    "감정 결과: 진품. 작품성은 평범한 수준입니다. 예상 가치 {lo}~{hi}.",
                    "감정 결과: 진품. {artist}의 작품이 맞으나 대표작은 아닙니다. 예상 가치 {lo}~{hi}.",
                    "감정 결과: 진품. 시장에서 흔히 거래되는 수준입니다. 예상 가치 {lo}~{hi}."),
            Grade.FAKE, List.of(
                    "감정 결과: 위작. 서명과 재료에서 위조 흔적이 발견되었습니다. 가치 없음.",
                    "감정 결과: 위작. 캔버스가 {artist}의 활동 시기보다 최근 것입니다. 가치 없음.",
                    "감정 결과: 위작. 물감 성분이 당시 것과 다릅니다. 가치 없음."));

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(title|artist|lo|hi)(?::([^}]+))?}");

    /** 자리 표시를 채운다. 예상 가치가 없는 문장이면 lo·hi는 쓰이지 않는다. */
    static String render(String template, Artwork artwork, int lo, int hi) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = switch (m.group(1)) {
                case "title" -> artwork.title();
                case "artist" -> artwork.artist();
                case "lo" -> String.valueOf(lo);
                default -> GameConfig.money(hi);
            };
            if (m.group(2) != null) {
                value = Josa.attach(value, m.group(2));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
