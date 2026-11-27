package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Grade;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;

class SettlementTest {

    @Test
    void ranksByCashPlusArtworksPlusBonusWithSharedRanks() {
        PlayerState a = new PlayerState("a", 500);   // 500 + 0 + 100 = 600
        PlayerState b = new PlayerState("b", 400);   // 400 + 200 + 0 = 600
        PlayerState c = new PlayerState("c", 700);   // 700
        PlayerState d = new PlayerState("d", 100);   // 100
        b.addArtwork(new Artwork(1, "t", "x", Grade.NORMAL, 200));
        Map<String, Integer> bonus = Map.of("a", 100);

        List<Settlement.PlayerResult> r = Settlement.rank(List.of(a, b, c, d),
                p -> bonus.getOrDefault(p.nickname(), 0), p -> p.nickname().equals("a") ? "미션" : "");

        assertEquals(List.of("c", "a", "b", "d"), r.stream().map(Settlement.PlayerResult::nickname).toList());
        assertEquals(List.of(1, 2, 2, 4), r.stream().map(Settlement.PlayerResult::rank).toList(), "동점은 같은 순위");
        assertEquals(600, r.get(1).total());
        assertEquals(100, r.get(1).bonus());
        assertTrue(r.get(1).missionSuccess());
        assertEquals(200, r.get(2).artworkValue());
        assertEquals(1, r.get(2).artworkCount());
        assertFalse(r.get(2).missionSuccess());
        assertEquals("c", Settlement.winner(r));
    }

    @Test
    void tiedFirstPlaceIsADraw() {
        PlayerState a = new PlayerState("a", 500);
        PlayerState b = new PlayerState("b", 500);
        List<Settlement.PlayerResult> r = Settlement.rank(List.of(a, b), p -> 0, p -> "");
        assertEquals(List.of(1, 1), r.stream().map(Settlement.PlayerResult::rank).toList());
        assertEquals(Protocol.DRAW, Settlement.winner(r));
    }

    @Test
    void artworksShowOwnerOrUnsold() {
        GameState game = new GameState();
        game.addPlayer("a");
        game.addPlayer("b");
        List<Artwork> deck = new ArrayList<>();
        for (int i = 0; i < GameConfig.BASE_ROUNDS; i++) {
            deck.add(new Artwork(i, "작품" + i, "작가" + i, Grade.FAKE, 0));
        }
        game.setDeck(deck);
        game.findPlayer("b").addArtwork(deck.get(2));

        List<Settlement.ArtworkResult> arts = Settlement.artworks(game);
        assertEquals(GameConfig.BASE_ROUNDS, arts.size());
        assertEquals(new Settlement.ArtworkResult(3, "작품2", "작가2", "가짜", 0, "b"), arts.get(2));
        assertEquals(Settlement.UNSOLD, arts.get(0).owner());
    }
}
