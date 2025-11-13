package org.secverse.SecVerseDupeUtils.Dupes.Donkey;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.secverse.SecVerseDupeUtils.Helper.EventsKeys;

import java.util.HashMap;
import java.util.UUID;

import static org.secverse.SecVerseDupeUtils.Helper.CleanShulker.cleanShulker;

public class DonkeyShulkerDupe implements Listener {
    private final Plugin plugin;
    private final EventsKeys ek;
    private final HashMap<UUID, Long> openTimes = new HashMap<>();

    private Boolean plEnabled;
    private long min, max;

    public DonkeyShulkerDupe(Plugin plugin) {
        this.plugin = plugin;
        this.ek = new EventsKeys(plugin);
        reload();

    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        plEnabled = config.getBoolean("OtherDupes.DonkeyDupe.Enabled");
        min = config.getLong("OtherDupes.DonkeyDupe.MinTiming", 100);
        max = config.getLong("OtherDupes.DonkeyDupe.MaxTiming", 800);

        ek.reload();
    }

    @EventHandler
    public void onDonkeyOpen(PlayerInteractEntityEvent event) {
        if (!plEnabled) return;
        if (!(event.getRightClicked() instanceof Donkey donkey)) return;
        if (!donkey.isCarryingChest()) return;

        openTimes.put(donkey.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onDonkeyDeath(EntityDeathEvent event) {
        if (!plEnabled) return;
        if (event.getEntityType() != EntityType.DONKEY) return;

        Donkey donkey = (Donkey) event.getEntity();
        Long openTime = openTimes.get(donkey.getUniqueId());
        if (openTime == null) return;

        long diff = System.currentTimeMillis() - openTime;
        if (diff >= min && diff <= max) { // Timing window

            Inventory inv = donkey.getInventory();
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;

                if (ek.isBlockedItem(item)) {
                    item.setType(Material.AIR);
                    continue;
                }

                cleanShulker(item, ek);
                donkey.getWorld().dropItemNaturally(donkey.getLocation(), item.clone());
                donkey.getWorld().dropItemNaturally(donkey.getLocation(), item.clone());
            }
        }

        openTimes.remove(donkey.getUniqueId());
    }
}
