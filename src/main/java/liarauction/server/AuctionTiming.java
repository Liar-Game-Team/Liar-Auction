package liarauction.server;

import java.time.Duration;
import java.util.Objects;
import liarauction.common.GameConfig;
import liarauction.common.Phase;

/** Durations for round phases and the auction idle timeout. */
public record AuctionTiming(
        Duration reveal,
        Duration rumor,
        Duration negotiation,
        Duration auction,
        Duration result,
        Duration bidIdle) {

    public AuctionTiming {
        requirePositive(reveal, "reveal");
        requirePositive(rumor, "rumor");
        requirePositive(negotiation, "negotiation");
        requirePositive(auction, "auction");
        requirePositive(result, "result");
        requirePositive(bidIdle, "bidIdle");
    }

    public static AuctionTiming defaults() {
        return new AuctionTiming(
                Duration.ofSeconds(Phase.REVEAL.seconds()),
                Duration.ofSeconds(Phase.RUMOR.seconds()),
                Duration.ofSeconds(Phase.NEGOTIATION.seconds()),
                Duration.ofSeconds(Phase.AUCTION.seconds()),
                Duration.ofSeconds(GameConfig.AUCTION_RESULT_SECONDS),
                Duration.ofSeconds(GameConfig.BID_IDLE_SECONDS));
    }

    Duration durationFor(Phase phase) {
        return switch (phase) {
            case REVEAL -> reveal;
            case RUMOR -> rumor;
            case NEGOTIATION -> negotiation;
            case AUCTION -> auction;
            default -> throw new IllegalArgumentException("타이머가 없는 단계: " + phase);
        };
    }

    private static void requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " 시간은 양수여야 합니다");
        }
    }
}
