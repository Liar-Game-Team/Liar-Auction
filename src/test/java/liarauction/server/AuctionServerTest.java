package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class AuctionServerTest {

    private AuctionServer server;
    private final List<TestClient> extraClients = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new AuctionServer(0);
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        for (TestClient c : extraClients) {
            c.close();
        }
        server.stop();
    }

    // ===== 방 목록 =====

    @Test
    void loginShowsRoomListAndCreatedRoomAppears() throws IOException {
        try (TestClient a = connect(); TestClient b = connect()) {
            a.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            assertEquals(Message.of(Protocol.LOGIN_OK, "철수"), a.read());
            assertEquals(Message.of(Protocol.ROOM_LIST, ""), a.read());

            a.send(Message.of(Protocol.REQ_CREATE_ROOM, " 초보만 ", 4, ""));
            assertEquals(Message.of(Protocol.ROOM_JOINED, 1, "초보만", 4, false), a.read());
            assertEquals(Message.of(Protocol.ROOM_STATE, "철수", "철수", ""), a.read());

            b.send(Message.of(Protocol.REQ_LOGIN, " 영희 "));
            assertEquals(Message.of(Protocol.LOGIN_OK, "영희"), b.read());
            assertEquals(Message.of(Protocol.ROOM_LIST, "1,초보만,1,4,false,false"), b.read());

            b.joinRoom(1);
            Message state = Message.of(Protocol.ROOM_STATE, "철수", "철수,영희", "");
            assertEquals(state, a.readUntil(Protocol.ROOM_STATE));
            assertEquals(state, b.readUntil(Protocol.ROOM_STATE));
        }
    }

    @Test
    void lobbyChatAndRoomChatAreSeparate() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희");
             TestClient c = connect().login("민수")) {
            int room = a.createRoom("방");
            b.joinRoom(room);

            String text = "100|200? 줄\n바꿈";
            a.send(Message.of(Protocol.REQ_CHAT, text));
            Message roomChat = Message.of(Protocol.BROADCAST_CHAT, "철수", text);
            assertEquals(roomChat, a.readUntil(Protocol.BROADCAST_CHAT));
            assertEquals(roomChat, b.readUntil(Protocol.BROADCAST_CHAT));

            c.send(Message.of(Protocol.REQ_CHAT, "방 목록에서 안녕"));
            // 방 목록에 있는 민수는 방 채팅 대신 자기 채팅만 받는다
            assertEquals(Message.of(Protocol.BROADCAST_CHAT, "민수", "방 목록에서 안녕"),
                    c.readUntil(Protocol.BROADCAST_CHAT));

            // 방 안의 두 사람은 민수의 채팅을 받지 않는다: 다음 채팅이 방 채팅이어야 함
            b.send(Message.of(Protocol.REQ_CHAT, "끝"));
            assertEquals(Message.of(Protocol.BROADCAST_CHAT, "영희", "끝"), a.readUntil(Protocol.BROADCAST_CHAT));
        }
    }

    @Test
    void leavingRoomReturnsToListAndEmptyRoomDisappears() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희")) {
            int room = a.createRoom("방");
            b.joinRoom(room);

            // 방장이 나가면 다음 사람이 방장
            a.send(Message.of(Protocol.REQ_LEAVE_ROOM));
            assertEquals(Message.of(Protocol.ROOM_LEFT), a.readUntil(Protocol.ROOM_LEFT));
            assertEquals(Message.of(Protocol.ROOM_LIST, room + ",방,1,6,false,false"), a.readUntil(Protocol.ROOM_LIST));
            awaitMessage(b, Message.of(Protocol.ROOM_STATE, "영희", "영희", ""));

            // 마지막 사람이 나가면 방이 사라진다
            b.send(Message.of(Protocol.REQ_LEAVE_ROOM));
            assertEquals(Message.of(Protocol.ROOM_LEFT), b.readUntil(Protocol.ROOM_LEFT));
            assertEquals(Message.of(Protocol.ROOM_LIST, ""), b.readUntil(Protocol.ROOM_LIST));
            assertNull(server.lobby().room(room));

            a.send(Message.of(Protocol.REQ_JOIN_ROOM, room, ""));
            assertEquals(Message.of(Protocol.ERROR, "없는 방입니다"), a.readUntil(Protocol.ERROR));
        }
    }

    @Test
    void privateRoomNeedsPassword() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희")) {
            int room = a.createRoom("비밀", 4, "1234");
            awaitMessage(b, Message.of(Protocol.ROOM_LIST, room + ",비밀,1,4,true,false"));

            b.send(Message.of(Protocol.REQ_JOIN_ROOM, room, "0000"));
            assertEquals(Message.of(Protocol.ERROR, "비밀번호가 틀렸습니다"), b.readUntil(Protocol.ERROR));

            b.send(Message.of(Protocol.REQ_QUICK_JOIN));
            assertEquals(Message.of(Protocol.ERROR, "바로 들어갈 수 있는 방이 없습니다. 방을 만들어 보세요"),
                    b.readUntil(Protocol.ERROR), "빠른 입장은 비밀방에 들어가지 않는다");

            b.joinRoom(room, "1234");
            assertEquals(Message.of(Protocol.ROOM_STATE, "철수", "철수,영희", ""), b.readUntil(Protocol.ROOM_STATE));
        }
    }

    @Test
    void roomMaxPlayersIsRespected() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희");
             TestClient c = connect().login("민수")) {
            int room = a.createRoom("둘이서", 2, "");
            b.joinRoom(room);
            c.send(Message.of(Protocol.REQ_JOIN_ROOM, room, ""));
            assertEquals(Message.of(Protocol.ERROR, "방이 가득 찼습니다 (최대 2명)"), c.readUntil(Protocol.ERROR));
        }
    }

    @Test
    void quickJoinPicksAWaitingPublicRoom() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희");
             TestClient c = connect().login("민수"); TestClient d = connect().login("지수")) {
            c.send(Message.of(Protocol.REQ_QUICK_JOIN));
            assertEquals(Protocol.ERROR, c.readUntil(Protocol.ERROR).command(), "방이 없으면 실패");

            int playing = a.createRoom("게임 중");
            b.joinRoom(playing);
            TestClient.startGame(a, b);
            int open = d.createRoom("자리 있음");

            c.send(Message.of(Protocol.REQ_QUICK_JOIN));
            assertEquals(open, c.readUntil(Protocol.ROOM_JOINED).intArg(0), "게임 중인 방은 건너뛴다");
        }
    }

    @Test
    void rejectsBadNicknamesRoomNamesAndRoomSizes() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect()) {
            for (String bad : List.of("철수", "", Protocol.NONE, "a,b", "a;b")) {
                b.send(Message.of(Protocol.REQ_LOGIN, bad));
                assertEquals(Protocol.LOGIN_FAIL, b.read().command(), "닉네임 '" + bad + "'");
            }
            for (String bad : List.of("", "a,b", "a;b", "a".repeat(GameConfig.MAX_ROOM_NAME_LENGTH + 1))) {
                a.send(Message.of(Protocol.REQ_CREATE_ROOM, bad, 4, ""));
                assertEquals(Protocol.ERROR, a.readUntil(Protocol.ERROR).command(), "방 이름 '" + bad + "'");
            }
            for (int size : List.of(1, 7)) {
                a.send(Message.of(Protocol.REQ_CREATE_ROOM, "방", size, ""));
                assertEquals(Message.of(Protocol.ERROR, "최대 인원은 2~6명입니다"), a.readUntil(Protocol.ERROR));
            }
        }
    }

    @Test
    void errorsForBadMessages() throws IOException {
        try (TestClient a = connect()) {
            a.send(Message.of(Protocol.REQ_CHAT, "로그인 전"));
            assertEquals(Protocol.ERROR, a.read().command());

            a.sendRaw("HELLO");
            assertEquals(Protocol.ERROR, a.read().command());

            a.login("철수");
            a.send(Message.of(Protocol.REQ_BID, 10));
            assertEquals(Message.of(Protocol.ERROR, "방에 들어가야 할 수 있습니다"), a.readUntil(Protocol.ERROR));
            a.send(Message.of(Protocol.UPDATE_BID, "철수", 100));
            assertEquals(Protocol.ERROR, a.readUntil(Protocol.ERROR).command());

            a.createRoom("방");
            a.send(Message.of(Protocol.REQ_BID, "abc"));
            assertEquals(Protocol.ERROR, a.readUntil(Protocol.ERROR).command());
            a.send(Message.of(Protocol.REQ_CREATE_ROOM, "또", 4, ""));
            assertEquals(Message.of(Protocol.ERROR, "방에서 나간 뒤에 할 수 있습니다"), a.readUntil(Protocol.ERROR));
        }
    }

    // ===== 방 안: 준비·시작·퇴장 =====

    @Test
    void onlyHostStartsAndOnlyWhenEveryoneIsReady() throws IOException {
        try (TestClient host = connect().login("철수");
             TestClient b = connect().login("영희");
             TestClient c = connect().login("민수")) {
            int room = host.createRoom("방");
            host.send(Message.of(Protocol.REQ_START));
            assertEquals(Message.of(Protocol.ERROR, "2명 이상 모여야 시작할 수 있습니다"), host.readUntil(Protocol.ERROR));
            b.joinRoom(room);
            c.joinRoom(room);

            host.send(Message.of(Protocol.REQ_START));
            assertTrue(host.readUntil(Protocol.ERROR).arg(0).contains("준비하지 않은 사람: 영희, 민수"));

            b.send(Message.of(Protocol.REQ_START));
            assertEquals(Message.of(Protocol.ERROR, "방장만 시작할 수 있습니다"), b.readUntil(Protocol.ERROR));

            b.send(Message.of(Protocol.REQ_READY, true));
            awaitMessage(c, Message.of(Protocol.ROOM_STATE, "철수", "철수,영희,민수", "영희"));
            b.send(Message.of(Protocol.REQ_READY, false));
            awaitMessage(c, Message.of(Protocol.ROOM_STATE, "철수", "철수,영희,민수", ""));

            Message start = TestClient.startGame(host, b, c);
            assertEquals(Message.of(Protocol.GAME_START, "철수,영희,민수", GameConfig.START_CASH, 8), start);

            b.send(Message.of(Protocol.REQ_READY, true));
            assertEquals(Message.of(Protocol.ERROR, "게임 시작 전에만 준비할 수 있습니다"), b.readUntil(Protocol.ERROR));
        }
    }

    @Test
    void cannotJoinRoomInGameAndListShowsIt() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희");
             TestClient late = connect().login("민수")) {
            int room = a.createRoom("방");
            b.joinRoom(room);
            TestClient.startGame(a, b);
            awaitMessage(late, Message.of(Protocol.ROOM_LIST, room + ",방,2,6,false,true"));

            late.send(Message.of(Protocol.REQ_JOIN_ROOM, room, ""));
            assertEquals(Message.of(Protocol.ERROR, "게임 중인 방입니다"), late.readUntil(Protocol.ERROR));
        }
    }

    @Test
    void gameContinuesWhenOneOfThreeLeaves() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient b = connect().login("영희");
             TestClient c = connect().login("민수")) {
            int room = a.createRoom("방");
            b.joinRoom(room);
            c.joinRoom(room);
            TestClient.startGame(a, b, c);

            c.send(Message.of(Protocol.REQ_LEAVE_ROOM));
            assertEquals(Message.of(Protocol.ROOM_LEFT), c.readUntil(Protocol.ROOM_LEFT));
            assertEquals(Message.of(Protocol.PLAYER_LEFT, "민수"), a.readUntil(Protocol.PLAYER_LEFT));
            assertEquals(Message.of(Protocol.PLAYER_LEFT, "민수"), b.readUntil(Protocol.PLAYER_LEFT));
            GameRoom r = server.lobby().room(room);
            synchronized (r) {
                GameState game = r.state();
                assertNotEquals(Phase.WAITING, game.phase(), "2명이 남았으니 게임 계속");
                assertEquals(List.of("철수", "영희"), game.players().stream().map(p -> p.nickname()).toList());
            }
        }
    }

    @Test
    void lastOpponentLeavingReturnsToWaitingAndNewPlayerCanJoin() throws IOException {
        try (TestClient a = connect().login("철수"); TestClient c = connect().login("민수")) {
            int room = a.createRoom("방");
            try (TestClient b = connect().login("영희")) {
                b.joinRoom(room);
                TestClient.startGame(a, b);
            }
            assertEquals(Message.of(Protocol.PLAYER_LEFT, "영희"), a.readUntil(Protocol.PLAYER_LEFT));
            assertEquals(Message.of(Protocol.ROOM_STATE, "철수", "철수", ""), a.readUntil(Protocol.ROOM_STATE));
            awaitMessage(c, Message.of(Protocol.ROOM_LIST, room + ",방,1,6,false,false"));

            c.joinRoom(room);
            assertEquals(Message.of(Protocol.ROOM_STATE, "철수", "철수,민수", ""), a.readUntil(Protocol.ROOM_STATE));
            assertEquals(Message.of(Protocol.GAME_START, "철수,민수", GameConfig.START_CASH, 8),
                    TestClient.startGame(a, c));
        }
    }

    @Test
    void twoRoomsPlayAtTheSameTime() throws IOException {
        try (TestClient a = connect().login("a"); TestClient b = connect().login("b");
             TestClient c = connect().login("c"); TestClient d = connect().login("d")) {
            int room1 = a.createRoom("1번");
            b.joinRoom(room1);
            int room2 = c.createRoom("2번");
            d.joinRoom(room2);
            assertEquals(Message.of(Protocol.GAME_START, "a,b", GameConfig.START_CASH, 8), TestClient.startGame(a, b));
            assertEquals(Message.of(Protocol.GAME_START, "c,d", GameConfig.START_CASH, 8), TestClient.startGame(c, d));

            GameState g1 = server.lobby().room(room1).state();
            GameState g2 = server.lobby().room(room2).state();
            assertNotSame(g1, g2);
            assertEquals(List.of("a", "b"), g1.players().stream().map(p -> p.nickname()).toList());
            assertEquals(List.of("c", "d"), g2.players().stream().map(p -> p.nickname()).toList());

            // 다른 방 채팅은 섞이지 않는다
            c.send(Message.of(Protocol.REQ_CHAT, "2번 방"));
            a.send(Message.of(Protocol.REQ_CHAT, "1번 방"));
            assertEquals(Message.of(Protocol.BROADCAST_CHAT, "a", "1번 방"), b.readUntil(Protocol.BROADCAST_CHAT));
            assertEquals(Message.of(Protocol.BROADCAST_CHAT, "c", "2번 방"), d.readUntil(Protocol.BROADCAST_CHAT));
        }
    }

    @Test
    void fivePlayersPlayTenRoundsWithBreakAfterFive() throws IOException {
        TestClient host = track(connect().login("p1"));
        int room = host.createRoom("다섯");
        List<TestClient> others = new ArrayList<>();
        for (int i = 2; i <= 5; i++) {
            others.add(track(connect().login("p" + i)).joinRoom(room));
        }
        Message start = TestClient.startGame(host, others.toArray(new TestClient[0]));
        assertEquals(Message.of(Protocol.GAME_START, "p1,p2,p3,p4,p5", GameConfig.START_CASH, 10), start);
        GameRoom r = server.lobby().room(room);
        synchronized (r) {
            assertEquals(10, r.state().totalRounds());
            assertEquals(5, r.state().breakAfterRound());
        }
    }

    @Test
    void completesEightRoundsThenReturnsToRoom() throws Exception {
        server.stop();
        server = new AuctionServer(0, new AuctionTiming(
                Duration.ofMillis(400),
                Duration.ofMillis(100),
                Duration.ofMillis(60),
                Duration.ofMillis(500),
                Duration.ofMillis(50),
                Duration.ofMillis(40)));
        server.start();

        try (TestClient alice = connect().login("alice");
             TestClient bob = connect().login("bob")) {
            int roomId = alice.createRoom("방");
            bob.joinRoom(roomId);
            TestClient.startGame(alice, bob);
            GameRoom room = server.lobby().room(roomId);
            // 게임이 끝나면 방은 새 상태로 바뀌므로 이번 판 상태를 잡아 둔다
            GameState game = room.state();

            for (int round = 1; round <= GameConfig.BASE_ROUNDS; round++) {
                Message startA = alice.readUntil(Protocol.START_ROUND);
                Message startB = bob.readUntil(Protocol.START_ROUND);
                assertEquals(String.valueOf(round), startA.arg(0));
                assertEquals(String.valueOf(round), startB.arg(0));
                assertEquals("100", startA.arg(3));

                Message itemsA = alice.readUntil(Protocol.ITEMS);
                Message itemsB = bob.readUntil(Protocol.ITEMS);
                assertEquals(round == 1 ? "1" : "0", itemsA.arg(0));
                assertEquals("1", itemsA.arg(1));
                assertEquals("1", itemsB.arg(0));
                assertEquals(round == 1 ? "1" : "0", itemsB.arg(1));

                if (round == 1) {
                    alice.send(Message.of(Protocol.REQ_USE_ITEM, "CERTIFICATE"));
                    bob.send(Message.of(Protocol.REQ_USE_ITEM, "JAMMER"));
                    assertEquals("CERTIFICATE", alice.readUntil(Protocol.ITEM_RESULT).arg(0));
                    assertEquals("JAMMER", bob.readUntil(Protocol.ITEM_RESULT).arg(0));
                    assertEquals(Message.of(Protocol.ITEMS, 0, 1), alice.readUntil(Protocol.ITEMS));
                    assertEquals(Message.of(Protocol.ITEMS, 1, 0), bob.readUntil(Protocol.ITEMS));
                }

                alice.readUntil(Protocol.RUMOR);
                bob.readUntil(Protocol.RUMOR);
                if (round == 2) {
                    synchronized (room) {
                        assertNotEquals(
                                game.currentArtwork().grade(),
                                room.info().rumorOf("alice").claimedGrade());
                    }
                }
                readUntilAuctionPhase(bob);
                readUntilAuctionPhase(alice);

                if (round < GameConfig.BASE_ROUNDS) {
                    alice.send(Message.of(Protocol.REQ_BID, 10));
                    assertEquals(Message.of(Protocol.UPDATE_BID, "alice", 110),
                            alice.readUntil(Protocol.UPDATE_BID));
                    assertEquals(Message.of(
                                    Protocol.UPDATE_CASH, "alice", 890 - ((round - 1) * 110)),
                            alice.readUntil(Protocol.UPDATE_CASH));
                    assertEquals(Message.of(Protocol.ROUND_END, "alice", 110),
                            alice.readUntil(Protocol.ROUND_END));

                    assertEquals(Message.of(Protocol.UPDATE_BID, "alice", 110),
                            bob.readUntil(Protocol.UPDATE_BID));
                    bob.readUntil(Protocol.UPDATE_CASH);
                    assertEquals(Message.of(Protocol.ROUND_END, "alice", 110),
                            bob.readUntil(Protocol.ROUND_END));
                } else {
                    assertEquals(Message.of(Protocol.ROUND_END, Protocol.NONE, 0),
                            alice.readUntil(Protocol.ROUND_END));
                    assertEquals(Message.of(Protocol.ROUND_END, Protocol.NONE, 0),
                            bob.readUntil(Protocol.ROUND_END));
                }
            }

            Message resultA = alice.readUntil(Protocol.GAME_OVER);
            Message resultB = bob.readUntil(Protocol.GAME_OVER);
            assertEquals(resultA, resultB);
            assertEquals(Phase.GAME_OVER, game.phase());
            assertEquals(230, game.findPlayer("alice").cash());
            assertEquals(7, game.findPlayer("alice").artworks().size());
            assertEquals(GameConfig.START_CASH, game.findPlayer("bob").cash());
            int aliceAssets = game.findPlayer("alice").totalAssets();
            int bobAssets = game.findPlayer("bob").totalAssets();
            String expectedWinner = aliceAssets == bobAssets
                    ? Protocol.DRAW
                    : aliceAssets > bobAssets ? "alice" : "bob";
            assertEquals(expectedWinner, resultA.arg(0));
            assertTrue(resultA.arg(1).contains("작품 공개"));
            assertTrue(resultA.arg(1).contains("230만원"), "돈은 만원 단위로 표시");
            for (var artwork : game.deck()) {
                assertTrue(resultA.arg(1).contains(artwork.title()));
                assertTrue(resultA.arg(1).contains("실제 가치 " + artwork.realValue() + "만원"));
            }

            // 끝나면 같은 사람들로 방 대기 상태, 다시 시작 가능
            Message waiting = Message.of(Protocol.ROOM_STATE, "alice", "alice,bob", "");
            assertEquals(waiting, alice.readUntil(Protocol.ROOM_STATE));
            assertEquals(waiting, bob.readUntil(Protocol.ROOM_STATE));
            assertEquals(Phase.WAITING, room.state().phase());
            assertEquals(Message.of(Protocol.GAME_START, "alice,bob", GameConfig.START_CASH, 8),
                    TestClient.startGame(alice, bob));
        }
    }

    // ===== 도우미 =====

    /** 기대한 메시지가 올 때까지 같은 종류의 메시지를 읽는다 (안 오면 @Timeout으로 실패) */
    private static void awaitMessage(TestClient client, Message expected) throws IOException {
        Message m;
        do {
            m = client.readUntil(expected.command());
        } while (!m.equals(expected));
    }

    private TestClient connect() throws IOException {
        return new TestClient(server.getPort());
    }

    private TestClient track(TestClient client) {
        extraClients.add(client);
        return client;
    }

    private static void readUntilAuctionPhase(TestClient client) throws IOException {
        while (true) {
            Message message = client.readUntil(Protocol.PHASE);
            if (Phase.valueOf(message.arg(0)) == Phase.AUCTION) {
                return;
            }
        }
    }
}
