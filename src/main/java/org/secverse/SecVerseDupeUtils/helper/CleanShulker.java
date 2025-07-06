package org.secverse.SecVerseDupeUtils.helper;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class CleanShulker {
    public static void cleanShulker(ItemStack item, EventsKeys ek) {
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulkerBox)) return;

        Inventory shulkerInv = shulkerBox.getInventory();
        boolean changed = false;

        for (int i = 0; i < shulkerInv.getSize(); i++) {
            ItemStack stack = shulkerInv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;

            if (ek.isBlockedItem(stack)) {
                shulkerInv.setItem(i, null);
                changed = true;
            }
        }

        if (changed) {
            shulkerBox.update();
            meta.setBlockState(shulkerBox);
            item.setItemMeta(meta);
        }
    }
}
