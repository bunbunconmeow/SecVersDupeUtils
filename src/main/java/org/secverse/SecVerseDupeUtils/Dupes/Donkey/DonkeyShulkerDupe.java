package org.secverse.SecVerseDupeUtils.Dupes.Donkey;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.secverse.SecVerseDupeUtils.Helper.EventsKeys;
import org.secverse.SecVerseDupeUtils.Helper.CleanShulker;

import java.util.*;

public class DonkeyShulkerDupe implements Listener {
    private final Plugin plugin;
    private final EventsKeys ek;
    private final HashMap<UUID, Long> openTimes = new HashMap<>();
    private final HashMap<UUID, UUID> donkeyOwners = new HashMap<>();
    private final List<DonkeyRank> ranks = new ArrayList<>();

    private boolean plEnabled;
    private long defaultMin;
    private long defaultMax;

    public DonkeyShulkerDupe(Plugin plugin) {
        this.plugin = plugin;
        this.ek = new EventsKeys(plugin);
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        String basePath = "OtherDupes.DonkeyDupe";

        // Load default settings
        plEnabled = config.getBoolean(basePath + ".Enabled", false);
        defaultMin = config.getLong(basePath + ".MinTiming", 100);
        defaultMax = config.getLong(basePath + ".MaxTiming", 5000);

        // Load ranks
        ranks.clear();
        ConfigurationSection ranksSection = config.getConfigurationSection(basePath + ".Ranks");

        if (ranksSection != null) {
            for (String rankName : ranksSection.getKeys(false)) {
                String rankPath = basePath + ".Ranks." + rankName;

                String permission = config.getString(rankPath + ".Permission");
                boolean rankEnabled = config.getBoolean(rankPath + ".Enabled", false);
                long rankMin = config.getLong(rankPath + ".MinTiming", defaultMin);
                long rankMax = config.getLong(rankPath + ".MaxTiming", defaultMax);

                if (permission != null) {
                    DonkeyRank rank = new DonkeyRank(rankName, permission, rankEnabled, rankMin, rankMax);
                    ranks.add(rank);

                    plugin.getLogger().info("[DonkeyDupe] Loaded rank '" + rankName + "': " +
                            "permission=" + permission + ", enabled=" + rankEnabled +
                            ", timing=" + rankMin + "-" + rankMax + "ms");
                }
            }

            // Sort ranks by priority (shorter timing windows = higher priority)
            ranks.sort((r1, r2) -> {
                long diff1 = r1.maxTiming - r1.minTiming;
                long diff2 = r2.maxTiming - r2.minTiming;
                return Long.compare(diff1, diff2);
            });
        }

        ek.reload();

        plugin.getLogger().info("[DonkeyDupe] Config loaded: enabled=" + plEnabled
                + ", default timing=" + defaultMin + "-" + defaultMax + "ms"
                + ", ranks loaded=" + ranks.size());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDonkeyOpen(PlayerInteractEntityEvent event) {
        if (!plEnabled) return;
        if (event.isCancelled()) return;

        if (!(event.getRightClicked() instanceof Donkey donkey)) return;
        if (!donkey.isCarryingChest()) return;

        Player player = event.getPlayer();
        UUID donkeyId = donkey.getUniqueId();

        // Check if player has any valid rank or base access
        DonkeyRank playerRank = getPlayerRank(player);

        if (playerRank == null && !plEnabled) {
            // No rank and base not enabled
            return;
        }

        openTimes.put(donkeyId, System.currentTimeMillis());
        donkeyOwners.put(donkeyId, player.getUniqueId());

        plugin.getLogger().fine("[DonkeyDupe] Player " + player.getName() + " opened donkey " + donkeyId +
                (playerRank != null ? " with rank " + playerRank.name : " (default)"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDonkeyDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.DONKEY) return;

        Donkey donkey = (Donkey) event.getEntity();
        UUID donkeyId = donkey.getUniqueId();

        Long openTime = openTimes.get(donkeyId);
        UUID playerId = donkeyOwners.get(donkeyId);

        if (openTime == null || playerId == null) {
            return;
        }

        long diff = System.currentTimeMillis() - openTime;

        // Get the player who opened the donkey
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            plugin.getLogger().warning("[DonkeyDupe] Player not found for donkey dupe");
            openTimes.remove(donkeyId);
            donkeyOwners.remove(donkeyId);
            return;
        }

        // Get player's rank and check timing
        DonkeyRank playerRank = getPlayerRank(player);
        boolean isValidTiming = false;
        String timingSource = "none";
        long usedMin = 0;
        long usedMax = 0;

        if (playerRank != null && playerRank.enabled) {
            // Check rank timing
            if (diff >= playerRank.minTiming && diff <= playerRank.maxTiming) {
                isValidTiming = true;
                timingSource = "rank:" + playerRank.name;
                usedMin = playerRank.minTiming;
                usedMax = playerRank.maxTiming;
            }
        } else if (plEnabled) {
            // Check default timing
            if (diff >= defaultMin && diff <= defaultMax) {
                isValidTiming = true;
                timingSource = "default";
                usedMin = defaultMin;
                usedMax = defaultMax;
            }
        }

        if (!isValidTiming) {
            plugin.getLogger().fine("[DonkeyDupe] Timing outside window for " + player.getName() +
                    ": " + diff + "ms (expected " + usedMin + "-" + usedMax + "ms, source: " + timingSource + ")");
            openTimes.remove(donkeyId);
            donkeyOwners.remove(donkeyId);
            return;
        }

        plugin.getLogger().info("[DonkeyDupe] Duping donkey inventory for " + player.getName() +
                " (timing=" + diff + "ms/" + usedMin + "-" + usedMax + "ms, source=" + timingSource + ")");

        // Process donkey inventory
        Inventory inv = donkey.getInventory();


        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            // Clone item before processing
            ItemStack processedItem = item.clone();

            // Check if item is blacklisted
            if (ek.isBlockedItem(processedItem)) {
                continue;
            }

            // Clean shulkers and bundles recursively
            CleanShulker.cleanShulker(processedItem, ek, ek.getIllegalItemValidator());

            // Validate item after cleaning
            if (ek.getIllegalItemValidator() != null) {
                ItemStack validatedItem = ek.getIllegalItemValidator().validateAndClean(processedItem);
                if (validatedItem == null) {
                    continue;
                }
                processedItem = validatedItem;
            }

            ItemStack duplicatedItem = processedItem.clone();
            donkey.getWorld().dropItemNaturally(donkey.getLocation(), duplicatedItem);
        }

        // Cleanup
        openTimes.remove(donkeyId);
        donkeyOwners.remove(donkeyId);
    }


    private DonkeyRank getPlayerRank(Player player) {
        DonkeyRank highestRank = null;

        // Ranks are already sorted by priority (shorter timing = higher priority)
        for (DonkeyRank rank : ranks) {
            if (player.hasPermission(rank.permission)) {
                if (highestRank == null) {
                    highestRank = rank;
                } else {
                    // Compare timing windows - shorter is higher priority
                    long currentWindow = highestRank.maxTiming - highestRank.minTiming;
                    long newWindow = rank.maxTiming - rank.minTiming;

                    if (newWindow < currentWindow) {
                        highestRank = rank;
                    }
                }
            }
        }

        return highestRank;
    }

    private static class DonkeyRank {
        final String name;
        final String permission;
        final boolean enabled;
        final long minTiming;
        final long maxTiming;

        DonkeyRank(String name, String permission, boolean enabled, long minTiming, long maxTiming) {
            this.name = name;
            this.permission = permission;
            this.enabled = enabled;
            this.minTiming = minTiming;
            this.maxTiming = maxTiming;
        }

        @Override
        public String toString() {
            return "DonkeyRank{" +
                    "name='" + name + '\'' +
                    ", permission='" + permission + '\'' +
                    ", enabled=" + enabled +
                    ", timing=" + minTiming + "-" + maxTiming + "ms" +
                    '}';
        }
    }
}
