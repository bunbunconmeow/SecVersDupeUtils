package org.secverse.SecVerseDupeUtils.Helper;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.Plugin;

public class CleanShulker {
    public static void cleanShulker(ItemStack item, EventsKeys ek, IllegalItemValidator validator) {
        Plugin plugin = ek.getPlugin();
        
        if (!plugin.getConfig().getBoolean("Settings.EnableItemCheck", true)) return;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulkerBox)) return;

        Inventory shulkerInv = shulkerBox.getInventory();
        boolean changed = false;

        for (int i = 0; i < shulkerInv.getSize(); i++) {
            ItemStack stack = shulkerInv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;

            // Check for blocked items (from blacklist)
            if (ek.isBlockedItem(stack)) {
                shulkerInv.setItem(i, null);
                changed = true;
                continue;
            }

            // Check for illegal items if enabled
            if (validator != null) {
                ItemStack cleaned = validator.validateAndClean(stack);
                if (cleaned == null) {
                    // Item was removed
                    shulkerInv.setItem(i, null);
                    changed = true;
                } else if (cleaned != stack) {
                    // Item was modified
                    shulkerInv.setItem(i, cleaned);
                    changed = true;
                }
            }

            // Recursively clean nested shulkers
            if (stack.getItemMeta() instanceof BlockStateMeta) {
                cleanShulker(stack, ek, validator);
                changed = true;
            }

            // Clean bundles
            if (stack.getItemMeta() instanceof BundleMeta) {
                if (cleanBundle(stack, ek, validator)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            shulkerBox.update();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
        }
    }

    public static boolean cleanBundle(ItemStack bundleItem, EventsKeys ek, IllegalItemValidator validator) {
        if (!(bundleItem.getItemMeta() instanceof BundleMeta bundleMeta)) return false;

        boolean changed = false;

        if (bundleMeta.hasItems()) {
            java.util.List<ItemStack> items = bundleMeta.getItems();
            java.util.List<ItemStack> cleanedItems = new java.util.ArrayList<>();

            for (ItemStack stack : items) {
                if (stack == null || stack.getType() == Material.AIR) continue;

                // Check for blocked items
                if (ek.isBlockedItem(stack)) {
                    changed = true;
                    continue;
                }

                // Check for illegal items
                if (validator != null) {
                    ItemStack cleaned = validator.validateAndClean(stack);
                    if (cleaned == null) {
                        changed = true;
                        continue;
                    } else if (cleaned != stack) {
                        cleanedItems.add(cleaned);
                        changed = true;
                        continue;
                    }
                }

                cleanedItems.add(stack);
            }

            if (changed) {
                bundleMeta.setItems(cleanedItems);
                bundleItem.setItemMeta(bundleMeta);
            }
        }

        return changed;
    }

    public static void cleanShulker(ItemStack item, EventsKeys ek) {
        cleanShulker(item, ek, null);
    }
}
