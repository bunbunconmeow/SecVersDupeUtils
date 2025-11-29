package org.secvers.DupeUtility.Permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PermissionLoader {
    private final Plugin plugin;
    private final Map<String, List<RankConfig>> dupeRanks;

    public PermissionLoader(Plugin plugin) {
        this.plugin = plugin;
        this.dupeRanks = new HashMap<>();
        loadRanks();
    }

    public void loadRanks() {
        dupeRanks.clear();

        // Load FrameDupe ranks
        loadRanksForDupe("FrameDupe");

        // Load GLOW_FrameDupe ranks
        loadRanksForDupe("GLOW_FrameDupe");

        // Load GrindStoneDupe ranks
        loadRanksForDupe("GrindStoneDupe");

        // Load DropperDupe ranks
        loadRanksForDupe("DropperDupe");

        plugin.getLogger().info("Loaded permission ranks for dupes");
    }

    private void loadRanksForDupe(String dupeType) {
        String configPath;
        switch (dupeType) {
            case "FrameDupe":
            case "GLOW_FrameDupe":
                configPath = dupeType + ".Ranks";
                break;
            default:
                configPath = "OtherDupes." + dupeType + ".Ranks";
                break;
        }

        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection(configPath);

        if (ranksSection == null) {
            dupeRanks.put(dupeType, new ArrayList<>());
            plugin.getLogger().info("No ranks found for " + dupeType + " at path: " + configPath);
            return;
        }

        List<RankConfig> ranks = new ArrayList<>();

        for (String rankName : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);

            if (rankSection == null) continue;

            String permission = rankSection.getString("Permission");
            int multiplier = rankSection.getInt("Multiplier", 1);
            int probability = rankSection.getInt("Probability-percentage", 100);

            if (permission != null) {
                ranks.add(new RankConfig(rankName, permission, multiplier, probability));
                plugin.getLogger().info("Loaded rank '" + rankName + "' for " + dupeType +
                        " with permission: " + permission + ", multiplier: " + multiplier);
            }
        }

        dupeRanks.put(dupeType, ranks);
    }

    public List<RankConfig> getRanksForDupe(String dupeType) {
        return dupeRanks.getOrDefault(dupeType, new ArrayList<>());
    }

    public RankConfig getHighestRank(Player player, String dupeType) {
        List<RankConfig> ranks = getRanksForDupe(dupeType);
        RankConfig highestRank = null;
        int highestMultiplier = 0;

        for (RankConfig rank : ranks) {
            if (player.hasPermission(rank.getPermission())) {
                if (rank.getMultiplier() > highestMultiplier) {
                    highestMultiplier = rank.getMultiplier();
                    highestRank = rank;
                }
            }
        }

        return highestRank;
    }
}
