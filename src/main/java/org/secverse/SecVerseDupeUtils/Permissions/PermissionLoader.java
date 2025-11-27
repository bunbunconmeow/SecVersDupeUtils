package org.secverse.SecVerseDupeUtils.Permissions;

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

        plugin.getLogger().info("Loaded permission ranks for dupes");
    }

    private void loadRanksForDupe(String dupeType) {
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection(dupeType + ".Ranks");

        if (ranksSection == null) {
            dupeRanks.put(dupeType, new ArrayList<>());
            return;
        }

        List<RankConfig> ranks = new ArrayList<>();

        for (String rankName : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);

            if (rankSection == null) continue;

            String permission = rankSection.getString("Permission");
            int multiplier = rankSection.getInt("Multiplier", 1);
            int probability = rankSection.getInt("Probability-percentage", 0);

            if (permission != null && !permission.isEmpty()) {
                ranks.add(new RankConfig(rankName, permission, multiplier, probability));
            }
        }

        // Sort ranks by probability (highest first) to give priority to better ranks
        ranks.sort((a, b) -> Integer.compare(b.getProbability(), a.getProbability()));

        dupeRanks.put(dupeType, ranks);
    }

    public RankConfig getBestRank(Player player, String dupeType) {
        List<RankConfig> ranks = dupeRanks.get(dupeType);

        if (ranks == null || ranks.isEmpty()) {
            return null;
        }

        // Return first rank that player has permission for (already sorted by priority)
        for (RankConfig rank : ranks) {
            if (player.hasPermission(rank.getPermission())) {
                return rank;
            }
        }

        return null;
    }

    public int getDefaultMultiplier(String dupeType) {
        return plugin.getConfig().getInt(dupeType + ".Multiplier", 1);
    }

    public int getDefaultProbability(String dupeType) {
        return plugin.getConfig().getInt(dupeType + ".Probability-percentage", 0);
    }

    public void reload() {
        loadRanks();
    }
}
