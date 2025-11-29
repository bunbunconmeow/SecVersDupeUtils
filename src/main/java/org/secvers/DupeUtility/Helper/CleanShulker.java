package org.secvers.DupeUtility.Helper;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.ArrayList;
import java.util.List;

public class CleanShulker {

    /**
     * Cleans a shulker box (or any container) from blacklisted and illegal items
     * Also recursively cleans nested shulkers and bundles
     */
    public static void cleanShulker(ItemStack item, EventsKeys ek, IllegalItemValidator validator) {
        if (item == null || item.getType() == Material.AIR) return;
        if (!item.hasItemMeta()) return;

        // Check if the item itself is a shulker box
        if (item.getItemMeta() instanceof BlockStateMeta) {
            cleanShulkerBox(item, ek, validator);
        }

        // Check if the item is a bundle
        if (item.getItemMeta() instanceof BundleMeta) {
            cleanBundle(item, ek, validator);
        }
    }


    private static void cleanShulkerBox(ItemStack item, EventsKeys ek, IllegalItemValidator validator) {
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) return;

        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
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

            // Validate illegal items if enabled
            if (validator != null) {
                ItemStack validatedItem = validator.validateAndClean(stack);
                if (validatedItem == null) {
                    // Item was removed
                    shulkerInv.setItem(i, null);
                    changed = true;
                    continue;
                } else if (!validatedItem.equals(stack)) {
                    // Item was modified
                    stack = validatedItem;
                    shulkerInv.setItem(i, stack);
                    changed = true;
                }
            }

            // Recursively check nested shulkers
            if (stack.hasItemMeta() && stack.getItemMeta() instanceof BlockStateMeta) {
                cleanShulkerBox(stack, ek, validator);
                // Update the item in inventory after cleaning
                shulkerInv.setItem(i, stack);
                changed = true;
            }

            // Check bundles recursively
            if (stack.hasItemMeta() && stack.getItemMeta() instanceof BundleMeta) {
                if (cleanBundle(stack, ek, validator)) {
                    // Update the item in inventory after cleaning
                    shulkerInv.setItem(i, stack);
                    changed = true;
                }
            }
        }

        // Apply changes back to the item
        if (changed) {
            shulkerBox.update();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
        }
    }

    /**
     * Clean a bundle item
     * Returns true if bundle was modified
     */
    public static boolean cleanBundle(ItemStack bundleItem, EventsKeys ek, IllegalItemValidator validator) {
        if (bundleItem == null || !bundleItem.hasItemMeta()) return false;
        if (!(bundleItem.getItemMeta() instanceof BundleMeta)) return false;

        BundleMeta bundleMeta = (BundleMeta) bundleItem.getItemMeta();
        if (!bundleMeta.hasItems()) return false;

        boolean changed = false;
        List<ItemStack> cleanedItems = new ArrayList<>();

        for (ItemStack bundleContent : bundleMeta.getItems()) {
            if (bundleContent == null || bundleContent.getType() == Material.AIR) continue;

            // Check for blocked items
            if (ek.isBlockedItem(bundleContent)) {
                changed = true;
                continue; // Skip this item
            }

            // Validate illegal items
            if (validator != null) {
                ItemStack validatedItem = validator.validateAndClean(bundleContent);
                if (validatedItem == null) {
                    changed = true;
                    continue; // Skip removed items
                } else if (!validatedItem.equals(bundleContent)) {
                    bundleContent = validatedItem;
                    changed = true;
                }
            }

            // Recursively clean nested shulkers in bundle
            if (bundleContent.hasItemMeta() && bundleContent.getItemMeta() instanceof BlockStateMeta) {
                cleanShulkerBox(bundleContent, ek, validator);
                changed = true;
            }

            // Recursively clean nested bundles
            if (bundleContent.hasItemMeta() && bundleContent.getItemMeta() instanceof BundleMeta) {
                if (cleanBundle(bundleContent, ek, validator)) {
                    changed = true;
                }
            }

            cleanedItems.add(bundleContent);
        }

        // Update bundle contents if changed
        if (changed) {
            bundleMeta.setItems(cleanedItems);
            bundleItem.setItemMeta(bundleMeta);
        }

        return changed;
    }
}
