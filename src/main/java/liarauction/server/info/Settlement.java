package liarauction.server.info;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import liarauction.common.Artwork;
import liarauction.common.GameState;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;

/**
 * 최종 정산 (#19). 최종 점수 = 현금 + 보유 작품 실제 가치 + 히든 미션 보너스.
 * 점수가 높은 순으로 순위를 매기고, 같은 점수는 같은 순위(1, 1, 3 …)로 둔다.
 */
final class Settlement {

    /** 플레이어 한 명의 결과 */
    record PlayerResult(int rank, String nickname, int cash, int artworkValue, int artworkCount,
            int bonus, int total, String mission, boolean missionSuccess) {
    }

    /** 작품 한 점의 진위 공개 */
    record ArtworkResult(int round, String title, String artist, String grade, int value, String owner) {
    }

    /** 유찰된 작품의 보유자 표시 */
    static final String UNSOLD = "유찰";

    private Settlement() {
    }

    /**
     * @param bonusOf 플레이어별 미션 보너스
     * @param missionOf 플레이어별 미션 설명, 미션이 없으면 빈 문자열
     */
    static List<PlayerResult> rank(List<PlayerState> players, ToIntFunction<PlayerState> bonusOf,
            Function<PlayerState, String> missionOf) {
        List<PlayerResult> unranked = new ArrayList<>();
        for (PlayerState p : players) {
            int artworkValue = p.totalAssets() - p.cash();
            int bonus = bonusOf.applyAsInt(p);
            unranked.add(new PlayerResult(0, p.nickname(), p.cash(), artworkValue, p.artworks().size(),
                    bonus, p.totalAssets() + bonus, missionOf.apply(p), bonus > 0));
        }
        // 점수 높은 순, 같으면 입장 순서 유지 (stable sort)
        unranked.sort(Comparator.comparingInt(PlayerResult::total).reversed());

        List<PlayerResult> ranked = new ArrayList<>();
        for (int i = 0; i < unranked.size(); i++) {
            PlayerResult r = unranked.get(i);
            int rank = (i > 0 && r.total() == unranked.get(i - 1).total()) ? ranked.get(i - 1).rank() : i + 1;
            ranked.add(new PlayerResult(rank, r.nickname(), r.cash(), r.artworkValue(), r.artworkCount(),
                    r.bonus(), r.total(), r.mission(), r.missionSuccess()));
        }
        return ranked;
    }

    /** 1등이 한 명이면 그 닉네임, 여러 명이면 {@link Protocol#DRAW} */
    static String winner(List<PlayerResult> ranked) {
        if (ranked.isEmpty()) {
            return Protocol.DRAW;
        }
        long firsts = ranked.stream().filter(r -> r.rank() == 1).count();
        return firsts == 1 ? ranked.get(0).nickname() : Protocol.DRAW;
    }

    /** 라운드 순서대로 모든 작품의 진위·실제 가치·낙찰자 */
    static List<ArtworkResult> artworks(GameState game) {
        List<ArtworkResult> out = new ArrayList<>();
        List<PlayerState> players = game.players();
        for (int i = 0; i < game.deck().size(); i++) {
            Artwork a = game.deck().get(i);
            String owner = UNSOLD;
            for (PlayerState p : players) {
                if (p.owns(a)) {
                    owner = p.nickname();
                }
            }
            out.add(new ArtworkResult(i + 1, a.title(), a.artist(), a.grade().label(), a.realValue(), owner));
        }
        return out;
    }
}
