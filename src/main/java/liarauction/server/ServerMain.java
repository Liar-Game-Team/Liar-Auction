package liarauction.server;

import liarauction.common.GameConfig;

/** 서버 실행: {@code ./gradlew runServer} 또는 {@code java liarauction.server.ServerMain [port]} */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : GameConfig.DEFAULT_PORT;
        AuctionServer server = new AuctionServer(port);
        server.start();
        server.awaitTermination();
    }
}
