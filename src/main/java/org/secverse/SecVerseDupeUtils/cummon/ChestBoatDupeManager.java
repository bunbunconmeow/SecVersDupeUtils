package org.secverse.SecVerseDupeUtils.cummon;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.secverse.SecVerseDupeUtils.FDupe;

import java.util.*;

public class ChestBoatDupeManager implements Listener {

    private final Map<String, ChestBoatStorage> storedBoats = new HashMap<>();
    private final Queue<UUID> pendingSpawns = new LinkedList<>();
    private final FDupe plugin;

    public ChestBoatDupeManager(FDupe plugin) {
        this.plugin = plugin;
        startSpawnLoop();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();

        if (!(quitter.getVehicle() instanceof ChestBoat chestBoat)) return;
        if (chestBoat.getBoatType() != Boat.Type.ACACIA) return;

        ItemStack[] inv = chestBoat.getInventory().getContents();
        ItemStack[] copy = new ItemStack[inv.length];

        for (int i = 0; i < inv.length; i++) {
            ItemStack item = inv[i];
            copy[i] = (item == null || item.getType() == Material.AIR) ? null : item.clone();


            if (copy[i] != null && isShulkerBox(copy[i])) {
                cleanShulker(copy[i]);
            }
        }

        storedBoats.put(quitter.getUniqueId().toString(),
                new ChestBoatStorage(chestBoat.getLocation(), copy, chestBoat.getBoatType()));

        chestBoat.remove(); // Boot im Welt-State bleibt unverändert bis zum Respawn
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (storedBoats.containsKey(uuid.toString())) {
            pendingSpawns.add(uuid);
        }
    }

    private void startSpawnLoop() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (pendingSpawns.isEmpty()) return;

            UUID uuid = pendingSpawns.poll();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;

            String id = uuid.toString();
            if (!storedBoats.containsKey(id)) return;

            ChestBoatStorage storage = storedBoats.remove(id);
            if (storage.boatType != Boat.Type.ACACIA) return;

            Entity entity = player.getWorld().spawnEntity(storage.location, EntityType.ACACIA_CHEST_BOAT);
            if (!(entity instanceof ChestBoat newBoat)) return;

            newBoat.setBoatType(storage.boatType);
            newBoat.getInventory().setContents(storage.contents);
            newBoat.addPassenger(player);

        }, 0L, 10L);

    }

    private boolean isShulkerBox(ItemStack item) {
        return item.getType().toString().endsWith("_SHULKER_BOX");
    }

    private void cleanShulker(ItemStack shulkerItem) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

        Inventory shulkerInv = shulker.getInventory();

        for (int slot = 0; slot < shulkerInv.getSize(); slot++) {
            ItemStack content = shulkerInv.getItem(slot);
            if (content == null || content.getType() == Material.AIR) continue;

            if (isBlockedItem(content)) {
                shulkerInv.setItem(slot, null);
            } else if (isShulkerBox(content)) {
                ItemStack nested = content.clone();
                cleanShulker(nested);
                shulkerInv.setItem(slot, nested);
            }
        }

        shulker.update();
        meta.setBlockState(shulker);
        shulkerItem.setItemMeta(meta);
    }

    private boolean isBlockedItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(EventsKeys.CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING)) {
            String type = container.get(EventsKeys.CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING);
            return type != null && (type.equalsIgnoreCase("heart") || type.equalsIgnoreCase("revive"));
        }

        return false;
    }
}
