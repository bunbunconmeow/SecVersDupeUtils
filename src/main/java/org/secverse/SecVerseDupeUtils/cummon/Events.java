package org.secverse.SecVerseDupeUtils.cummon;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.secverse.SecVerseDupeUtils.FDupe;

import java.util.List;

public class Events {
    private final FDupe plugin;

    public Events(FDupe plugin) {
        this.plugin = plugin;
    }

    public class FrameAll implements Listener {

        @EventHandler
        private void onFrameBreak(EntityDamageByEntityEvent event) {
            if (plugin.getConfig().getBoolean("FrameDupe.Enabled") && event.getEntityType() == EntityType.ITEM_FRAME) {
                ItemFrame itemFrame = (ItemFrame) event.getEntity();
                ItemStack item = itemFrame.getItem();

                // Check if permission is needed
                if (plugin.getConfig().getBoolean("FrameDupe.Need-Permissions")) {
                    String permission = plugin.getConfig().getString("FrameDupe.Permission", "framedupe.use"); // Permiso configurable
                    if (!event.getEntity().getWorld().getPlayers().get(0).hasPermission(permission)) {
                        return;
                    }
                }

                // Whitelist and Blacklist Verification
                if (!isItemAllowed(item, "FrameDupe")) return;

                // ShulkerBox Verification
                if (plugin.getConfig().getBoolean("FrameDupe.Fix-Shulkers")) {
                    removeItems(item, "FrameDupe");
                }

                // Probability of duplicate
                int rng = (int) (Math.random() * 100);
                if (rng < plugin.getConfig().getInt("FrameDupe.Probability-percentage")) {
                    int multiplier = plugin.getConfig().getInt("FrameDupe.Multiplier", 1);
                    for (int i = 0; i < multiplier; i++) {
                        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), item);
                    }
                }
            }
        }
    }


    public class FrameSpecific implements Listener {

        @EventHandler
        private void onFrameBreak(EntityDamageByEntityEvent event) {
            if (plugin.getConfig().getBoolean("GLOW_FrameDupe.Enabled") && event.getEntityType() == EntityType.GLOW_ITEM_FRAME) {
                GlowItemFrame itemFrame = (GlowItemFrame) event.getEntity();
                ItemStack item = itemFrame.getItem();

                // Check if permission is needed
                if (plugin.getConfig().getBoolean("GLOW_FrameDupe.Need-Permissions")) {
                    String permission = plugin.getConfig().getString("GLOW_FrameDupe.Permission", "glowframedupe.use"); // Permiso configurable
                    if (!event.getEntity().getWorld().getPlayers().get(0).hasPermission(permission)) {
                        return;
                    }
                }

                // Whitelist and Blacklist Verification
                if (!isItemAllowed(item, "GLOW_FrameDupe")) return;

                // ShulkerBox Verification
                if (plugin.getConfig().getBoolean("GLOW_FrameDupe.Fix-Shulkers")) {
                    removeItems(item, "GLOW_FrameDupe");
                }

                // Probability of duplicate
                int rng = (int) (Math.random() * 100);
                if (rng < plugin.getConfig().getInt("GLOW_FrameDupe.Probability-percentage")) {
                    int multiplier = plugin.getConfig().getInt("GLOW_FrameDupe.Multiplier", 1);
                    for (int i = 0; i < multiplier; i++) {
                        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), item);
                    }
                }
            }
        }
    }

    private boolean isItemAllowed(ItemStack item, String path) {
        if (plugin.getConfig().getBoolean(path + ".Whitelist.Enabled")) {
            List<String> whitelist = plugin.getConfig().getStringList(path + ".Whitelist.Items");
            if (!whitelist.contains(item.getType().toString())) return false;
        }

        if (plugin.getConfig().getBoolean(path + ".Blacklist.Enabled")) {
            List<String> blacklist = plugin.getConfig().getStringList(path + ".Blacklist.Items");
            if (blacklist.contains(item.getType().toString())) return false;
        }
        return true;
    }

    private void removeItems(ItemStack item, String path) {
        if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                Inventory shulkerInventory = shulkerBox.getInventory();

                for (int i = 0; i < shulkerInventory.getSize(); i++) {
                    ItemStack stack = shulkerInventory.getItem(i);
                    if (stack == null || stack.getType() == Material.AIR) continue;

                    ItemMeta meta = stack.getItemMeta();
                    if (meta == null) continue;

                    PersistentDataContainer container = meta.getPersistentDataContainer();

                    if (container.has(EventsKeys.CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING)) {
                        String type = container.get(EventsKeys.CUSTOM_ITEM_TYPE_KEY, PersistentDataType.STRING);

                        if (type != null && (type.equalsIgnoreCase("heart") || type.equalsIgnoreCase("revive"))) {
                            shulkerInventory.setItem(i, null);
                        }
                    }
                }

                shulkerBox.update();
                blockStateMeta.setBlockState(shulkerBox);
                item.setItemMeta(blockStateMeta);
            }
        }
    }
}

