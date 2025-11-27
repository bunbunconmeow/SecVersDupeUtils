package org.secverse.SecVerseDupeUtils.Permissions;

public class RankConfig {
    private final String rankName;
    private final String permission;
    private final int multiplier;
    private final int probability;

    public RankConfig(String rankName, String permission, int multiplier, int probability) {
        this.rankName = rankName;
        this.permission = permission;
        this.multiplier = multiplier;
        this.probability = probability;
    }

    public String getRankName() {
        return rankName;
    }

    public String getPermission() {
        return permission;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public int getProbability() {
        return probability;
    }
}
