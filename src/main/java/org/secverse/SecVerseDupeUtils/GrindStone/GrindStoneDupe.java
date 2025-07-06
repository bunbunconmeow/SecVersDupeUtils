package org.secverse.SecVerseDupeUtils.GrindStone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.secverse.SecVerseDupeUtils.helper.EventsKeys;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

import static org.secverse.SecVerseDupeUtils.helper.CleanShulker.cleanShulker;

public class GrindStoneDupe implements Listener {
    private final HashMap<UUID, Long> insertTimes = new HashMap<>();
    private Plugin plugin;
    private EventsKeys ek;
    private boolean isEnbaled;

    public GrindStoneDupe(Plugin lplugin) {
        this.plugin = lplugin;
        this.ek = new EventsKeys(lplugin);

        isEnbaled = plugin.getConfig().getBoolean("OtherDupes.GrindStone");
    }

    public void reload() {
        isEnbaled = plugin.getConfig().getBoolean("OtherDupes.GrindStone");
    }

    @EventHandler
    public void onGrindstoneInsert(InventoryClickEvent event) {
        if (!isEnbaled) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && currentItem.getType().toString().endsWith("_SHULKER_BOX")) {
            insertTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onGrindstoneTake(InventoryClickEvent event) {
        if (!isEnbaled) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() > 2) return; // Nur Grindstone Slots

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && currentItem.getType().toString().endsWith("_SHULKER_BOX")) {
            Long insertTime = insertTimes.get(player.getUniqueId());
            if (insertTime == null) return;

            long diff = System.currentTimeMillis() - insertTime;
            if (diff >= 1200 && diff <= 2000) {
                if (ek.isBlockedItem(currentItem)) {
                    cleanShulker(currentItem, ek);
                }

                ItemStack duped = currentItem.clone();
                player.getWorld().dropItemNaturally(player.getLocation(), duped);
            }

            insertTimes.remove(player.getUniqueId());
        }
    }
}
