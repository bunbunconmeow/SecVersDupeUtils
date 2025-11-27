package org.secverse.SecVerseDupeUtils.Dupes.GrindStone;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.secverse.SecVerseDupeUtils.Helper.EventsKeys;
import org.secverse.SecVerseDupeUtils.Permissions.PermissionLoader;
import org.secverse.SecVerseDupeUtils.Permissions.RankConfig;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.secverse.SecVerseDupeUtils.Helper.CleanShulker.cleanShulker;

public class GrindStoneDupe implements Listener {
    private final HashMap<UUID, Long> insertTimes = new HashMap<>();
    private final HashMap<UUID, ItemStack> storedShulkers = new HashMap<>();
    private Plugin plugin;
    private EventsKeys ek;
    private PermissionLoader permissionLoader;
    private boolean isEnabled;
    private long minTiming;
    private long maxTiming;
    private boolean dropNaturally;
    private boolean addToInventory;
    private int baseMultiplier;

    public GrindStoneDupe(Plugin lplugin) {
        this.plugin = lplugin;
        this.ek = new EventsKeys(lplugin);
        this.permissionLoader = new PermissionLoader(lplugin);
        loadConfig();
    }

    private void loadConfig() {
        String basePath = "OtherDupes.GrindStone";
        this.isEnabled = plugin.getConfig().getBoolean(basePath + ".Enabled", false);
        this.minTiming = plugin.getConfig().getLong(basePath + ".MinTiming", 1200);
        this.maxTiming = plugin.getConfig().getLong(basePath + ".MaxTiming", 2200);
        this.dropNaturally = plugin.getConfig().getBoolean(basePath + ".dropNaturally", true);
        this.addToInventory = plugin.getConfig().getBoolean(basePath + ".addToInventory", false);
        this.baseMultiplier = plugin.getConfig().getInt(basePath + ".Multiplier", 1);
    }

    public void reload() {
        loadConfig();
        permissionLoader.loadRanks();
        insertTimes.clear();
        storedShulkers.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrindstoneClick(InventoryClickEvent event) {
        if (!isEnabled) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Check base permission
        if (!player.hasPermission("dupeutils.grindstonedupe")) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getType() == InventoryType.GRINDSTONE) {
                    for (int i = 0; i < 2; i++) {
                        ItemStack item = player.getOpenInventory().getItem(i);
                        if (isShulkerBox(item)) {
                            UUID uuid = player.getUniqueId();
                            if (!insertTimes.containsKey(uuid)) {
                                insertTimes.put(uuid, System.currentTimeMillis());
                                storedShulkers.put(uuid, item.clone());
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void handleShulkerInsert(Player player, ItemStack shulker) {
        long currentTime = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        insertTimes.put(uuid, currentTime);
        storedShulkers.put(uuid, shulker.clone());
    }

    private void handleShulkerTake(Player player, ItemStack shulker, InventoryClickEvent event) {
        UUID uuid = player.getUniqueId();
        Long insertTime = insertTimes.get(uuid);

        if (insertTime == null) {
            insertTime = System.currentTimeMillis() - minTiming;
        }

        long timeDiff = System.currentTimeMillis() - insertTime;
        if (timeDiff >= minTiming && timeDiff <= maxTiming) {
            performDupe(player, shulker, timeDiff);
        }

        insertTimes.remove(uuid);
        storedShulkers.remove(uuid);
    }

    private void performDupe(Player player, ItemStack shulker, long timeDiff) {
        // Get multiplier based on player's rank
        int multiplier = getPlayerMultiplier(player);

        ItemStack toDupe = shulker.clone();

        // Clean the shulker from blocked/illegal items
        if (ek.isBlockedItem(toDupe)) {
            cleanShulker(toDupe, ek, ek.getIllegalItemValidator());
        } else {
            // Also clean non-blocked shulkers for illegal items inside
            cleanShulker(toDupe, ek, ek.getIllegalItemValidator());
        }

        // Duplicate based on multiplier
        for (int i = 0; i < multiplier; i++) {
            ItemStack duped = toDupe.clone();

            if (dropNaturally) {
                player.getWorld().dropItemNaturally(player.getLocation(), duped);
            }

            if (addToInventory) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(duped.clone());
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
        }
    }

    /**
     * Gets the multiplier for a player based on their highest rank
     */
    private int getPlayerMultiplier(Player player) {
        List<RankConfig> ranks = permissionLoader.getRanksForDupe("GrindStoneDupe");

        int highestMultiplier = baseMultiplier;

        for (RankConfig rank : ranks) {
            if (player.hasPermission(rank.getPermission())) {
                if (rank.getMultiplier() > highestMultiplier) {
                    highestMultiplier = rank.getMultiplier();
                }
            }
        }

        return highestMultiplier;
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String type = item.getType().toString();
        return type.endsWith("_SHULKER_BOX") || type.equals("SHULKER_BOX");
    }
}
