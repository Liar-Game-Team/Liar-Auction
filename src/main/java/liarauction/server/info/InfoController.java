package liarauction.server.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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

/** Part B: private items, player-specific rumors, hidden missions, and final settlement. */
public final class InfoController {

    /** 한 플레이어가 받은 히든 미션. 지정 미션이 아니면 target은 null */
    public record AssignedMission(HiddenMission mission, String target) {

        /** 본인에게 보내는 설명 */
        public String text() {
            return mission.describe(target);
        }
    }

    private final GameRoom room;
    private final InspectionGenerator inspections;
    private final Random random;
    /** 닉네임 → 히든 미션 (#49) */
    private final Map<String, AssignedMission> missions = new LinkedHashMap<>();
    private boolean missionsAssigned;
    private final Map<String, Inspection> roundRumors = new HashMap<>();
    private final Set<String> jammerUsersForThisRound = new HashSet<>();
    private final Set<String> jammerUsersForNextRound = new HashSet<>();
    private boolean finalized;

    public InfoController(GameRoom room) {
        this(room, new InspectionGenerator());
    }

    public InfoController(GameRoom room, InspectionGenerator inspections) {
        this(room, inspections, new Random());
    }

    public InfoController(GameRoom room, InspectionGenerator inspections, Random random) {
        this.room = Objects.requireNonNull(room, "room");
        this.inspections = Objects.requireNonNull(inspections, "inspections");
        this.random = Objects.requireNonNull(random, "random");
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
        // 후반 첫 라운드에 "갑자기" 히든 미션을 준다. 브레이크타임 단계(#34)가 생기면 거기서 assignMissions()를 불러도 된다
        if (game.round() == game.breakAfterRound() + 1) {
            assignMissions();
        }
    }

    /**
     * 히든 미션을 1인당 1개씩, 서로 다른 미션으로 나눠 본인에게만 보낸다 ({@code MISSION}).
     * 한 게임에 한 번만 나눈다 (이미 나눴으면 아무것도 하지 않는다).
     */
    public void assignMissions() {
        if (missionsAssigned) {
            return;
        }
        missionsAssigned = true;
        List<PlayerState> players = room.state().players();
        List<HiddenMission> pool = new ArrayList<>(List.of(HiddenMission.values()));
        Collections.shuffle(pool, random);
        for (int i = 0; i < players.size(); i++) {
            String nickname = players.get(i).nickname();
            HiddenMission mission = pool.get(i % pool.size());
            String target = null;
            if (mission.needsTarget()) {
                List<String> others = new ArrayList<>();
                for (PlayerState p : players) {
                    if (!p.nickname().equals(nickname)) {
                        others.add(p.nickname());
                    }
                }
                target = others.get(random.nextInt(others.size()));
            }
            AssignedMission assigned = new AssignedMission(mission, target);
            missions.put(nickname, assigned);
            room.sendTo(nickname, Message.of(Protocol.MISSION, assigned.text()));
        }
    }

    /** 그 플레이어의 히든 미션, 아직 없으면 null */
    public AssignedMission missionOf(String nickname) {
        return missions.get(nickname);
    }

    /**
     * 게임 종료 시점의 미션 보너스 (만원). 미션이 없거나, 실패했거나, 지정 대상이 게임 중 나갔으면 0.
     */
    public int missionBonus(PlayerState player) {
        AssignedMission assigned = missions.get(player.nickname());
        if (assigned == null) {
            return 0;
        }
        PlayerState target = null;
        if (assigned.mission().needsTarget()) {
            target = room.state().findPlayer(assigned.target());
            if (target == null) {
                return 0;
            }
        }
        return assigned.mission().achieved(player, target) ? assigned.mission().bonus() : 0;
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
        if (item == Item.JAMMER && game.round() == game.totalRounds()) {
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

    /**
     * 최종 정산 (#19): 순위(현금 + 작품 실제 가치 + 미션 보너스), 작품 진위 공개, 미션 공개.
     * {@code RESULT_PLAYER}·{@code RESULT_ARTWORK}를 보낸 뒤 {@code GAME_OVER}를 한 번 보낸다.
     */
    public void onGameOver() {
        GameState game = room.state();
        if (!game.isOver()) {
            throw new IllegalStateException("게임이 종료되지 않았습니다");
        }
        if (finalized) {
            return;
        }

        List<Settlement.PlayerResult> ranking = Settlement.rank(game.players(), this::missionBonus, p -> {
            AssignedMission m = missions.get(p.nickname());
            return m == null ? "" : m.text();
        });
        List<Settlement.ArtworkResult> artworks = Settlement.artworks(game);
        String winner = Settlement.winner(ranking);

        // 결과 창용: 플레이어별·작품별 한 줄씩 보낸 뒤 마지막에 GAME_OVER
        for (Settlement.PlayerResult r : ranking) {
            room.broadcast(Message.of(Protocol.RESULT_PLAYER, r.rank(), r.nickname(), r.cash(),
                    r.artworkValue(), r.artworkCount(), r.bonus(), r.total(), r.mission(), r.missionSuccess()));
        }
        for (Settlement.ArtworkResult a : artworks) {
            room.broadcast(Message.of(Protocol.RESULT_ARTWORK,
                    a.round(), a.title(), a.artist(), a.grade(), a.value(), a.owner()));
        }

        // 기록·로그용 요약 문장
        StringBuilder details = new StringBuilder("순위\n");
        for (Settlement.PlayerResult r : ranking) {
            details.append(r.rank()).append("위 ")
                    .append(r.nickname())
                    .append(": 현금 ").append(GameConfig.money(r.cash()))
                    .append(", 작품 ").append(GameConfig.money(r.artworkValue()))
                    .append(" (").append(r.artworkCount()).append("점)")
                    .append(", 미션 보너스 +").append(GameConfig.money(r.bonus()))
                    .append(", 최종 ").append(GameConfig.money(r.total()))
                    .append('\n');
        }
        if (!missions.isEmpty()) {
            details.append("\n히든 미션\n");
            for (Settlement.PlayerResult r : ranking) {
                if (!r.mission().isEmpty()) {
                    details.append(r.nickname()).append(": ").append(r.mission())
                            .append(r.missionSuccess() ? " → 성공" : " → 실패").append('\n');
                }
            }
        }
        details.append("\n작품 공개\n");
        for (Settlement.ArtworkResult a : artworks) {
            details.append(a.round()).append(". ")
                    .append(a.title()).append(" - ").append(a.grade())
                    .append(", 실제 가치 ").append(GameConfig.money(a.value()))
                    .append(", 보유자 ").append(a.owner())
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
        missions.clear();
        missionsAssigned = false;
        finalized = false;
    }

    private void sendItems(PlayerState player) {
        room.sendTo(player.nickname(), Message.of(
                Protocol.ITEMS,
                player.itemCount(Item.CERTIFICATE),
                player.itemCount(Item.JAMMER)));
    }

    private static void requireCurrentArtwork(GameState game, Artwork artwork) {
        Objects.requireNonNull(artwork, "artwork");
        if (!artwork.equals(game.currentArtwork())) {
            throw new IllegalArgumentException("현재 라운드 작품이 아닙니다");
        }
    }
}
