package liarauction.server.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Item;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.PlayerState;
import liarauction.common.Protocol;
import liarauction.server.GameRoom;

/** Part B: private items, player-specific rumors, and final settlement. */
public final class InfoController {

    private final GameRoom room;
    private final InspectionGenerator inspections;
    private final Map<String, Inspection> roundRumors = new HashMap<>();
    private final Set<String> jammerUsersForThisRound = new HashSet<>();
    private final Set<String> jammerUsersForNextRound = new HashSet<>();
    private boolean finalized;

    public InfoController(GameRoom room) {
        this(room, new InspectionGenerator());
    }

    public InfoController(GameRoom room, InspectionGenerator inspections) {
        this.room = Objects.requireNonNull(room, "room");
        this.inspections = Objects.requireNonNull(inspections, "inspections");
    }

    /** Called after a round starts; jammer use from the previous round targets this round. */
    public void onRoundStart(Artwork artwork) {
        GameState game = room.state();
        if (game.phase() != Phase.REVEAL || game.round() < 1) {
            throw new IllegalStateException("공개 단계가 아닙니다");
        }
        requireCurrentArtwork(game, artwork);
        roundRumors.clear();
        jammerUsersForThisRound.clear();
        jammerUsersForThisRound.addAll(jammerUsersForNextRound);
        jammerUsersForNextRound.clear();
        for (PlayerState player : game.players()) {
            sendItems(player);
        }
    }

    /** Called during the reveal phase by {@code REQ_USE_ITEM}. */
    public void onUseItem(PlayerState player, Item item) {
        if (player == null) {
            throw new IllegalStateException("아이템을 사용할 플레이어가 없습니다");
        }
        Objects.requireNonNull(item, "item");
        GameState game = room.state();
        if (game.findPlayer(player.nickname()) != player) {
            throw new IllegalArgumentException("게임에 없는 플레이어: " + player.nickname());
        }
        if (game.phase() != Phase.REVEAL) {
            throw new IllegalStateException("아이템은 공개 단계에서만 사용할 수 있습니다");
        }
        if (item == Item.JAMMER && game.round() == GameConfig.TOTAL_ROUNDS) {
            throw new IllegalStateException("교란 아이템은 마지막 라운드에서 사용할 수 없습니다");
        }
        if (!player.consumeItem(item)) {
            throw new IllegalStateException("사용할 아이템이 없습니다");
        }

        if (item == Item.CERTIFICATE) {
            Inspection certificate = inspections.generate(game.currentArtwork(), true);
            room.sendTo(player.nickname(),
                    Message.of(Protocol.ITEM_RESULT, item.name(), certificate.text()));
        } else {
            jammerUsersForNextRound.add(player.nickname());
            room.sendTo(player.nickname(), Message.of(
                    Protocol.ITEM_RESULT, item.name(), "다음 라운드 상대에게 거짓 소문을 전달합니다."));
        }
        sendItems(player);
    }

    /**
     * 소문 단계: 기본은 모두 같은 소문을 받고, 지난 라운드에 교란당한 플레이어만
     * 기본 소문과도 실제 등급과도 다른 거짓 소문을 받는다 (12주차 회의).
     */
    public void onRumorPhase(Artwork artwork) {
        GameState game = room.state();
        if (game.phase() != Phase.RUMOR) {
            throw new IllegalStateException("소문 단계가 아닙니다");
        }
        requireCurrentArtwork(game, artwork);
        roundRumors.clear();
        Inspection common = inspections.generate(artwork, false);
        for (PlayerState player : game.players()) {
            Inspection rumor = common;
            PlayerState opponent = game.opponentOf(player.nickname());
            if (opponent != null && jammerUsersForThisRound.contains(opponent.nickname())) {
                rumor = inspections.generateJammedRumor(artwork, common.claimedGrade());
            }
            roundRumors.put(player.nickname(), rumor);
            room.sendTo(player.nickname(), Message.of(Protocol.RUMOR, rumor.text()));
        }
    }

    /** The current rumor for a player, or null until the rumor phase. */
    public Inspection rumorOf(String nickname) {
        return roundRumors.get(nickname);
    }

    /** Reveals actual artwork details and broadcasts the final asset comparison once. */
    public void onGameOver() {
        GameState game = room.state();
        if (!game.isOver()) {
            throw new IllegalStateException("게임이 종료되지 않았습니다");
        }
        if (finalized) {
            return;
        }

        List<PlayerState> players = game.players();
        int highestAssets = players.stream().mapToInt(PlayerState::totalAssets).max().orElse(0);
        List<PlayerState> winners = players.stream()
                .filter(player -> player.totalAssets() == highestAssets)
                .toList();
        String winner = winners.size() == 1 ? winners.get(0).nickname() : Protocol.DRAW;

        StringBuilder details = new StringBuilder("최종 자산\n");
        for (PlayerState player : players) {
            details.append(player.nickname())
                    .append(": 현금 ")
                    .append(player.cash())
                    .append(", 총 자산 ")
                    .append(player.totalAssets())
                    .append('\n');
        }
        details.append("\n작품 공개\n");
        for (int i = 0; i < game.deck().size(); i++) {
            Artwork artwork = game.deck().get(i);
            details.append(i + 1)
                    .append(". ")
                    .append(artwork.title())
                    .append(" - ")
                    .append(artwork.grade().label())
                    .append(", 실제 가치 ")
                    .append(artwork.realValue())
                    .append(", 보유자 ")
                    .append(ownerOf(players, artwork))
                    .append('\n');
        }

        room.broadcast(Message.of(Protocol.GAME_OVER, winner, details.toString().stripTrailing()));
        finalized = true;
    }

    /** Clears transient state when a player leaves and the room is reset. */
    public void onGameStopped() {
        roundRumors.clear();
        jammerUsersForThisRound.clear();
        jammerUsersForNextRound.clear();
        finalized = false;
    }

    private void sendItems(PlayerState player) {
        room.sendTo(player.nickname(), Message.of(
                Protocol.ITEMS,
                player.itemCount(Item.CERTIFICATE),
                player.itemCount(Item.JAMMER)));
    }

    private static String ownerOf(List<PlayerState> players, Artwork artwork) {
        List<String> owners = new ArrayList<>();
        for (PlayerState player : players) {
            if (player.owns(artwork)) {
                owners.add(player.nickname());
            }
        }
        return owners.isEmpty() ? Protocol.NONE : String.join(", ", owners);
    }

    private static void requireCurrentArtwork(GameState game, Artwork artwork) {
        Objects.requireNonNull(artwork, "artwork");
        if (!artwork.equals(game.currentArtwork())) {
            throw new IllegalArgumentException("현재 라운드 작품이 아닙니다");
        }
    }
}
