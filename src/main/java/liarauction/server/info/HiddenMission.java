package liarauction.server.info;

import liarauction.common.Artwork;
import liarauction.common.GameConfig;
import liarauction.common.Grade;
import liarauction.common.PlayerState;

/**
 * 히든 미션 (#49). 게임 중간에 1인당 1개씩 본인에게만 주고, 게임이 끝날 때 달성하면 보너스(만원)를 더한다.
 * {@link #needsTarget()}인 미션은 다른 플레이어 한 명을 지정한다 ("플레이어 지정 방식").
 * 판정은 게임이 끝난 시점의 현금·보유 작품으로 한다.
 */
public enum HiddenMission {

    MASTERPIECE_COLLECTOR("명작 수집가", "명작을 1점 이상 가지고 끝내세요", 100, false) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return count(me, Grade.MASTERPIECE) >= 1;
        }
    },
    NO_FAKES("눈썰미", "작품을 2점 이상 가지면서 가짜는 하나도 없이 끝내세요", 100, false) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return me.artworks().size() >= 2 && count(me, Grade.FAKE) == 0;
        }
    },
    BIG_SPENDER("큰손", "작품을 4점 이상 모으세요", 100, false) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return me.artworks().size() >= 4;
        }
    },
    SAVER("알뜰살뜰", "현금을 500만원 이상 남기고 끝내세요", 80, false) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return me.cash() >= 500;
        }
    },
    STEADY("수수한 취향", "보통 작품을 2점 이상 가지고 끝내세요", 80, false) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return count(me, Grade.NORMAL) >= 2;
        }
    },
    RIVAL_COLLECTION("라이벌", "{target}보다 작품을 더 많이 가지고 끝내세요", 100, true) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return me.artworks().size() > target.artworks().size();
        }
    },
    RIVAL_CASH("견제", "{target}보다 현금을 더 많이 남기고 끝내세요", 80, true) {
        @Override
        boolean achieved(PlayerState me, PlayerState target) {
            return me.cash() > target.cash();
        }
    };

    private final String title;
    private final String goal;
    private final int bonus;
    private final boolean needsTarget;

    HiddenMission(String title, String goal, int bonus, boolean needsTarget) {
        this.title = title;
        this.goal = goal;
        this.bonus = bonus;
        this.needsTarget = needsTarget;
    }

    public String title() {
        return title;
    }

    /** 보너스 (만원) */
    public int bonus() {
        return bonus;
    }

    /** 다른 플레이어 한 명을 지정하는 미션인지 */
    public boolean needsTarget() {
        return needsTarget;
    }

    /** 본인에게 보낼 미션 설명. 지정 미션이면 대상 닉네임을 넣는다 */
    public String describe(String target) {
        String text = needsTarget ? goal.replace("{target}", target) : goal;
        return "[" + title + "] " + text + " (성공하면 +" + GameConfig.money(bonus) + ")";
    }

    /**
     * 게임 종료 시점에 달성했는지.
     *
     * @param target 지정 미션의 대상, 지정 미션이 아니면 null
     */
    abstract boolean achieved(PlayerState me, PlayerState target);

    static long count(PlayerState player, Grade grade) {
        return player.artworks().stream().map(Artwork::grade).filter(g -> g == grade).count();
    }
}
