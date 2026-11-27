package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Grade;
import liarauction.common.Item;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;
import liarauction.server.AuctionServer;
import liarauction.server.GameRoom;
import liarauction.server.TestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class InfoControllerTest {

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
    void eachPlayerStartsWithItemsAndCertificateIsPrivate() throws IOException {
        try (TestClient alice = new TestClient(server.getPort()).login("alice");
             TestClient bob = new TestClient(server.getPort()).login("bob")) {
            readUntil(alice, Protocol.GAME_START);
            readUntil(bob, Protocol.GAME_START);

            assertEquals(Message.of(Protocol.ITEMS, 1, 1), readUntil(alice, Protocol.ITEMS));
            assertEquals(Message.of(Protocol.ITEMS, 1, 1), readUntil(bob, Protocol.ITEMS));

            alice.send(Message.of(Protocol.REQ_USE_ITEM, Item.CERTIFICATE.name()));
            Message result = readUntil(alice, Protocol.ITEM_RESULT);
            assertEquals(Item.CERTIFICATE.name(), result.arg(0));
            assertEquals(Message.of(Protocol.ITEMS, 0, 1), readUntil(alice, Protocol.ITEMS));
        }
    }

    @Test
    void jammerMakesTheOpponentsNextRoundRumorFalse() throws IOException {
        try (TestClient alice = new TestClient(server.getPort()).login("alice");
             TestClient bob = new TestClient(server.getPort()).login("bob")) {
            readUntil(alice, Protocol.GAME_START);
            readUntil(bob, Protocol.GAME_START);
            readUntil(alice, Protocol.ITEMS);
            readUntil(bob, Protocol.ITEMS);

            GameRoom room = server.room();
            Artwork nextArtwork;
            synchronized (room) {
                GameState game = room.state();
                game.setDeck(deck());
                game.setPhase(Phase.REVEAL);
                room.info().onUseItem(game.findPlayer("bob"), Item.JAMMER);
                nextArtwork = game.startNextRound();
                room.info().onRoundStart(nextArtwork);
                game.setPhase(Phase.RUMOR);
                room.info().onRumorPhase(nextArtwork);
            }

            Message jammerResult = readUntil(bob, Protocol.ITEM_RESULT);
            assertEquals(Item.JAMMER.name(), jammerResult.arg(0));
            assertEquals(Protocol.RUMOR, readUntil(alice, Protocol.RUMOR).command());
            assertEquals(Protocol.RUMOR, readUntil(bob, Protocol.RUMOR).command());
            synchronized (room) {
                Grade jammed = room.info().rumorOf("alice").claimedGrade();
                assertNotEquals(nextArtwork.grade(), jammed, "교란 소문은 항상 거짓");
                assertNotEquals(room.info().rumorOf("bob").claimedGrade(), jammed,
                        "교란 소문은 기본 소문과도 달라야 함");
            }
        }
    }

    @Test
    void everyoneHearsTheSameRumorWithoutJammer() throws IOException {
        try (TestClient alice = new TestClient(server.getPort()).login("alice");
             TestClient bob = new TestClient(server.getPort()).login("bob")) {
            readUntil(alice, Protocol.GAME_START);
            readUntil(bob, Protocol.GAME_START);

            GameRoom room = server.room();
            for (int i = 0; i < 20; i++) {
                synchronized (room) {
                    GameState game = room.state();
                    game.setPhase(Phase.RUMOR);
                    room.info().onRumorPhase(game.currentArtwork());
                }
                assertEquals(readUntil(alice, Protocol.RUMOR), readUntil(bob, Protocol.RUMOR),
                        "교란이 없으면 모두 같은 소문");
            }
        }
    }

    @Test
    void rejectsItemsOutsideRevealAndJammerOnTheLastRound() throws IOException {
        try (TestClient alice = new TestClient(server.getPort()).login("alice");
             TestClient bob = new TestClient(server.getPort()).login("bob")) {
            readUntil(alice, Protocol.GAME_START);
            readUntil(bob, Protocol.GAME_START);

            GameRoom room = server.room();
            synchronized (room) {
                GameState game = room.state();
                game.setPhase(Phase.RUMOR);
                assertThrows(IllegalStateException.class,
                        () -> room.info().onUseItem(game.findPlayer("alice"), Item.CERTIFICATE));
                assertEquals(1, game.findPlayer("alice").itemCount(Item.CERTIFICATE));

                game.setDeck(deck());
                for (int round = 1; round < GameConfig.TOTAL_ROUNDS; round++) {
                    game.startNextRound();
                }
                assertThrows(IllegalStateException.class,
                        () -> room.info().onUseItem(game.findPlayer("alice"), Item.JAMMER));
                assertEquals(1, game.findPlayer("alice").itemCount(Item.JAMMER));
            }
        }
    }

    private static List<Artwork> deck() {
        List<Artwork> deck = new ArrayList<>();
        int id = 0;
        for (Grade grade : Grade.values()) {
            for (int i = 0; i < grade.count(); i++) {
                int value = grade == Grade.FAKE ? 0 : grade.minValue() + i;
                deck.add(new Artwork(id++, "작품" + id, "작가", grade, value));
            }
        }
        return deck;
    }

    private static Message readUntil(TestClient client, String command) throws IOException {
        Message message;
        do {
            message = client.read();
        } while (!command.equals(message.command()));
        return message;
    }
}
