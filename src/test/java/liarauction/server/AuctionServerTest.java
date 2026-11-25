package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import liarauction.common.GameConfig;
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

    @BeforeEach
    void setUp() throws IOException {
        server = new AuctionServer(0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void twoPlayersLoginAndChat() throws IOException {
        try (TestClient a = connect(); TestClient b = connect()) {
            a.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            assertEquals(Message.of(Protocol.LOGIN_OK, "철수"), a.read());

            b.send(Message.of(Protocol.REQ_LOGIN, " 영희 "));
            assertEquals(Message.of(Protocol.LOGIN_OK, "영희"), b.read());

            assertEquals(Message.of(Protocol.GAME_START, "영희", GameConfig.START_CASH, GameConfig.TOTAL_ROUNDS), a.read());
            assertEquals(Message.of(Protocol.GAME_START, "철수", GameConfig.START_CASH, GameConfig.TOTAL_ROUNDS), b.read());

            String text = "100|200? 줄\n바꿈";
            a.send(Message.of(Protocol.REQ_CHAT, text));
            Message expected = Message.of(Protocol.BROADCAST_CHAT, "철수", text);
            assertEquals(expected, readUntil(a, Protocol.BROADCAST_CHAT));
            assertEquals(expected, readUntil(b, Protocol.BROADCAST_CHAT));
        }
    }

    @Test
    void rejectsDuplicateNicknameAndFullRoom() throws IOException {
        try (TestClient a = connect(); TestClient b = connect(); TestClient c = connect()) {
            a.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            a.read();

            b.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            assertEquals(Protocol.LOGIN_FAIL, b.read().command());
            b.send(Message.of(Protocol.REQ_LOGIN, ""));
            assertEquals(Protocol.LOGIN_FAIL, b.read().command());
            b.send(Message.of(Protocol.REQ_LOGIN, Protocol.NONE));
            assertEquals(Protocol.LOGIN_FAIL, b.read().command());
            b.send(Message.of(Protocol.REQ_LOGIN, "영희"));
            assertEquals(Protocol.LOGIN_OK, b.read().command());

            c.send(Message.of(Protocol.REQ_LOGIN, "민수"));
            assertEquals(Message.of(Protocol.LOGIN_FAIL, "방이 가득 찼습니다"), c.read());
        }
    }

    @Test
    void errorsForBadMessages() throws IOException {
        try (TestClient a = connect()) {
            a.send(Message.of(Protocol.REQ_CHAT, "로그인 전"));
            assertEquals(Protocol.ERROR, a.read().command());

            a.sendRaw("HELLO");
            assertEquals(Protocol.ERROR, a.read().command());

            a.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            a.read();
            a.send(Message.of(Protocol.REQ_BID, "abc"));
            assertEquals(Protocol.ERROR, a.read().command());
            a.send(Message.of(Protocol.UPDATE_BID, "철수", 100));
            assertEquals(Protocol.ERROR, a.read().command());
        }
    }

    @Test
    void opponentLeftThenNewPlayerCanJoin() throws IOException {
        try (TestClient a = connect()) {
            a.send(Message.of(Protocol.REQ_LOGIN, "철수"));
            a.read();

            try (TestClient b = connect()) {
                b.send(Message.of(Protocol.REQ_LOGIN, "영희"));
                b.read();
                b.read();
                assertEquals(Protocol.GAME_START, a.read().command());
            }
            assertEquals(Message.of(Protocol.OPPONENT_LEFT, "영희"), readUntil(a, Protocol.OPPONENT_LEFT));

            try (TestClient c = connect()) {
                c.send(Message.of(Protocol.REQ_LOGIN, "영희"));
                assertEquals(Protocol.LOGIN_OK, c.read().command());
                assertEquals(
                        Message.of(Protocol.GAME_START, "영희", GameConfig.START_CASH, GameConfig.TOTAL_ROUNDS),
                        readUntil(a, Protocol.GAME_START));
            }
        }
    }

    @Test
    void completesEightRoundsWithBidsAndAnUnbidAuction() throws Exception {
        server.stop();
        server = new AuctionServer(0, new AuctionTiming(
                Duration.ofMillis(25),
                Duration.ofMillis(25),
                Duration.ofMillis(25),
                Duration.ofMillis(500),
                Duration.ofMillis(25),
                Duration.ofMillis(40)));
        server.start();

        try (TestClient alice = connect().login("alice");
             TestClient bob = connect().login("bob")) {
            assertEquals(Protocol.GAME_START, readUntil(alice, Protocol.GAME_START).command());
            assertEquals(Protocol.GAME_START, readUntil(bob, Protocol.GAME_START).command());

            for (int round = 1; round <= GameConfig.TOTAL_ROUNDS; round++) {
                Message startA = readUntil(alice, Protocol.START_ROUND);
                Message startB = readUntil(bob, Protocol.START_ROUND);
                assertEquals(String.valueOf(round), startA.arg(0));
                assertEquals(String.valueOf(round), startB.arg(0));
                assertEquals("100", startA.arg(3));

                List<Phase> phases = new ArrayList<>();
                readUntilAuctionPhase(alice, phases);
                readUntilAuctionPhase(bob, null);
                assertEquals(
                        List.of(Phase.REVEAL, Phase.INSPECTION, Phase.NEGOTIATION, Phase.AUCTION),
                        phases);

                if (round < GameConfig.TOTAL_ROUNDS) {
                    alice.send(Message.of(Protocol.REQ_BID, 10));
                    assertEquals(Message.of(Protocol.UPDATE_BID, "alice", 110),
                            readUntil(alice, Protocol.UPDATE_BID));
                    assertEquals(Message.of(
                                    Protocol.UPDATE_CASH, "alice", 890 - ((round - 1) * 110)),
                            readUntil(alice, Protocol.UPDATE_CASH));
                    assertEquals(Message.of(Protocol.ROUND_END, "alice", 110),
                            readUntil(alice, Protocol.ROUND_END));

                    assertEquals(Message.of(Protocol.UPDATE_BID, "alice", 110),
                            readUntil(bob, Protocol.UPDATE_BID));
                    readUntil(bob, Protocol.UPDATE_CASH);
                    assertEquals(Message.of(Protocol.ROUND_END, "alice", 110),
                            readUntil(bob, Protocol.ROUND_END));
                } else {
                    assertEquals(Message.of(Protocol.ROUND_END, Protocol.NONE, 0),
                            readUntil(alice, Protocol.ROUND_END));
                    assertEquals(Message.of(Protocol.ROUND_END, Protocol.NONE, 0),
                            readUntil(bob, Protocol.ROUND_END));
                }
            }

            long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (server.room().state().phase() != Phase.GAME_OVER
                    && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(Phase.GAME_OVER, server.room().state().phase());
            assertEquals(230, server.room().state().findPlayer("alice").cash());
            assertEquals(7, server.room().state().findPlayer("alice").artworks().size());
            assertEquals(GameConfig.START_CASH, server.room().state().findPlayer("bob").cash());
        }
    }

    private static Message readUntil(TestClient client, String command) throws IOException {
        Message message;
        do {
            message = client.read();
        } while (!command.equals(message.command()));
        return message;
    }

    private TestClient connect() throws IOException {
        return new TestClient(server.getPort());
    }

    private static void readUntilAuctionPhase(TestClient client, List<Phase> observedPhases)
            throws IOException {
        while (true) {
            Message message = client.read();
            if (!Protocol.PHASE.equals(message.command())) {
                continue;
            }
            Phase phase = Phase.valueOf(message.arg(0));
            if (observedPhases != null) {
                observedPhases.add(phase);
            }
            if (phase == Phase.AUCTION) {
                return;
            }
        }
    }
}
