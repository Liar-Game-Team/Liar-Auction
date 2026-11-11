package liarauction.server.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.GameState;
import liarauction.common.Grade;
import liarauction.common.Message;
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
    void officialHolderAlternatesEachRound() {
        for (int round = 1; round <= GameConfig.TOTAL_ROUNDS; round++) {
            boolean first = InfoController.isOfficialHolder(0, round);
            boolean second = InfoController.isOfficialHolder(1, round);
            assertNotEquals(first, second, "라운드 " + round + ": 한 명만 감정서");
            assertEquals(round % 2 == 1, first, "홀수 라운드는 0번이 감정서");
        }
    }

    @Test
    void eachPlayerGetsOnlyOwnInspectionAndHolderRotates() throws IOException {
        try (TestClient a = new TestClient(server.getPort()).login("철수");
             TestClient b = new TestClient(server.getPort()).login("영희")) {
            assertEquals(Protocol.GAME_START, a.read().command());
            assertEquals(Protocol.GAME_START, b.read().command());

            GameRoom room = server.room();
            synchronized (room) {
                room.state().setDeck(deck());
            }

            for (int round = 1; round <= GameConfig.TOTAL_ROUNDS; round++) {
                synchronized (room) {
                    GameState state = room.state();
                    Artwork art = state.startNextRound();
                    room.info().onRoundStart(art);
                }
                Message ma = a.read();
                Message mb = b.read();
                assertEquals(Protocol.INSPECTION, ma.command());
                assertEquals(Protocol.INSPECTION, mb.command());

                boolean aOfficial = ma.boolArg(0);
                assertEquals(round % 2 == 1, aOfficial, "라운드 " + round + " 철수 감정서 여부");
                assertEquals(!aOfficial, mb.boolArg(0), "라운드 " + round + " 영희는 반대");

                synchronized (room) {
                    assertEquals(room.info().inspectionOf("철수").text(), ma.arg(1));
                    assertEquals(room.info().inspectionOf("영희").text(), mb.arg(1));
                }
            }

            // 감정 정보는 각자 하나씩만: 다음 메시지는 채팅이어야 함
            a.send(Message.of(Protocol.REQ_CHAT, "끝"));
            assertEquals(Protocol.BROADCAST_CHAT, a.read().command());
            assertEquals(Protocol.BROADCAST_CHAT, b.read().command());
        }
    }

    @Test
    void textNeverRevealsRealValue() throws IOException {
        try (TestClient a = new TestClient(server.getPort()).login("철수");
             TestClient b = new TestClient(server.getPort()).login("영희")) {
            a.read();
            b.read();
            GameRoom room = server.room();
            List<Artwork> deck = deck();
            synchronized (room) {
                room.state().setDeck(deck);
                room.info().onRoundStart(room.state().startNextRound());
            }
            String value = String.valueOf(deck.get(0).realValue());
            assertFalse(a.read().arg(1).contains(value));
            assertFalse(b.read().arg(1).contains(value));
        }
    }

    @Test
    void rejectsBeforeFirstRound() throws IOException {
        try (TestClient ignored = new TestClient(server.getPort()).login("철수")) {
            GameRoom room = server.room();
            synchronized (room) {
                assertThrows(IllegalStateException.class,
                        () -> room.info().onRoundStart(new Artwork(1, "t", "a", Grade.FAKE, 0)));
            }
        }
    }

    /** 명작 2, 보통 3, 가짜 3. 가치는 다른 숫자와 헷갈리지 않게 특이한 값 */
    private static List<Artwork> deck() {
        List<Artwork> deck = new ArrayList<>();
        int id = 0;
        for (Grade g : Grade.values()) {
            for (int i = 0; i < g.count(); i++) {
                int value = g == Grade.FAKE ? 0 : g.minValue() + 37 + i;
                deck.add(new Artwork(id++, "작품" + id, "작가", g, value));
            }
        }
        return deck;
    }
}
