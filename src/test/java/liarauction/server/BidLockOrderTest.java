package liarauction.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import liarauction.common.Message;
import liarauction.common.Phase;
import liarauction.common.Protocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * #44 회귀 테스트: 경매 단계가 열리는 순간 입찰이 들어와도 서버가 멈추지 않아야 한다.
 * 단계를 아주 짧게 두고 한 명이 입찰을 계속 보내서 단계 전환과 입찰이 겹치게 만든다.
 */
class BidLockOrderTest {

    private static final int GAMES = 10;

    @Test
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void spammingBidsDuringPhaseChangesNeverDeadlocks() throws Exception {
        for (int game = 1; game <= GAMES; game++) {
            AuctionServer server = new AuctionServer(0, new AuctionTiming(
                    Duration.ofMillis(5), Duration.ofMillis(5), Duration.ofMillis(5),
                    Duration.ofMillis(30), Duration.ofMillis(5), Duration.ofMillis(10)));
            server.start();
            AtomicBoolean stop = new AtomicBoolean();
            try (TestClient alice = new TestClient(server.getPort()).login("alice");
                 TestClient bob = new TestClient(server.getPort()).login("bob")) {
                int roomId = alice.createRoom("방");
                bob.joinRoom(roomId);
                TestClient.startGame(alice, bob);
                drain(alice, stop);
                drain(bob, stop);
                Thread spammer = new Thread(() -> {
                    while (!stop.get()) {
                        alice.send(Message.of(Protocol.REQ_BID, 10));
                    }
                });
                spammer.setDaemon(true);
                spammer.start();

                // 교착 상태면 state()에서 멈추고 @Timeout이 테스트를 실패시킨다.
                // 게임이 끝나면 대기실(WAITING)로 돌아온다
                while (server.lobby().room(roomId).state().phase() != Phase.WAITING) {
                    assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads(),
                            game + "번째 게임에서 교착 상태");
                    Thread.sleep(5);
                }
                assertEquals(Phase.WAITING, server.lobby().room(roomId).state().phase());
            } finally {
                stop.set(true);
                server.stop();
            }
        }
    }

    private static void drain(TestClient client, AtomicBoolean stop) {
        Thread reader = new Thread(() -> {
            try {
                while (!stop.get()) {
                    client.read();
                }
            } catch (IOException | AssertionError ignored) {
                // 연결 종료
            }
        });
        reader.setDaemon(true);
        reader.start();
    }
}
