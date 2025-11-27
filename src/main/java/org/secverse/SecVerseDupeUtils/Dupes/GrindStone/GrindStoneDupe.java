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
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

import static org.secverse.SecVerseDupeUtils.Helper.CleanShulker.cleanShulker;

public class GrindStoneDupe implements Listener {
    private final HashMap<UUID, Long> insertTimes = new HashMap<>();
    private final HashMap<UUID, ItemStack> storedShulkers = new HashMap<>();
    private Plugin plugin;
    private EventsKeys ek;
    private boolean isEnabled;
    private long minTiming;
    private long maxTiming;
    private boolean dropNaturally;
    private boolean addToInventory;

    public GrindStoneDupe(Plugin lplugin) {
        this.plugin = lplugin;
        this.ek = new EventsKeys(lplugin);
        loadConfig();
    }

    private void loadConfig() {
        String basePath = "OtherDupes.GrindStone";

        isEnabled = plugin.getConfig().getBoolean(basePath + ".Enabled", false);
        minTiming = plugin.getConfig().getLong(basePath + ".MinTiming", 1200L);
        maxTiming = plugin.getConfig().getLong(basePath + ".MaxTiming", 2200L);
        dropNaturally = plugin.getConfig().getBoolean(basePath + ".dropNaturally", true);
        addToInventory = plugin.getConfig().getBoolean(basePath + ".addToInventory", false);

    }

    public void reload() {
        loadConfig();
        insertTimes.clear();
        storedShulkers.clear();
        plugin.getLogger().info("[GrindStone Exploit] Reloaded | Status: " + (isEnabled ? "§aEnabled" : "§cDisabled"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGrindstoneClick(InventoryClickEvent event) {
        if (!isEnabled) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Shulker Box Detection
        if (isShulkerBox(clickedItem) || isShulkerBox(cursorItem)) {
            event.setCancelled(false);
            if (isShulkerBox(cursorItem) && event.getRawSlot() < 2) {
                handleShulkerInsert(player, cursorItem);
            }
            else if (isShulkerBox(clickedItem) && event.getRawSlot() < 3) {
                handleShulkerTake(player, clickedItem, event);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onGrindstoneInteract(InventoryClickEvent event) {
        if (!isEnabled) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;

        Player player = (Player) event.getWhoClicked();

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
        ItemStack toDupe = shulker.clone();

        if (ek.isBlockedItem(toDupe)) {
            cleanShulker(toDupe, ek, ek.getIllegalItemValidator());
        }

        ItemStack duped = toDupe.clone();

        if (dropNaturally) player.getWorld().dropItemNaturally(player.getLocation(), duped);

        if (addToInventory) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(duped.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String type = item.getType().toString();
        return type.endsWith("_SHULKER_BOX") || type.equals("SHULKER_BOX");
    }
}
