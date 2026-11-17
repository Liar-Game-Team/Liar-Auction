package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import liarauction.common.GameConfig;
import liarauction.common.Message;
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
            assertEquals(expected, a.read());
            assertEquals(expected, b.read());
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
            assertEquals(Message.of(Protocol.OPPONENT_LEFT, "영희"), a.read());

            try (TestClient c = connect()) {
                c.send(Message.of(Protocol.REQ_LOGIN, "영희"));
                assertEquals(Protocol.LOGIN_OK, c.read().command());
                assertEquals(Message.of(Protocol.GAME_START, "영희", GameConfig.START_CASH, GameConfig.TOTAL_ROUNDS), a.read());
            }
        }
    }

    private TestClient connect() throws IOException {
        return new TestClient(server.getPort());
    }
}
