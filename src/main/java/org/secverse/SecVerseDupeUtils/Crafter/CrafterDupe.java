package org.secverse.SecVerseDupeUtils.Crafter;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CrafterInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

public class CrafterDupe implements Listener {
    private final HashMap<UUID, Long> exploitTimings = new HashMap<>();
    private Plugin plugin;
    private boolean enabled;
    private long minTiming;
    private long maxTiming;
    private boolean destroyCrafter;

    public CrafterDupe(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        String path = "OtherDupes.CrafterDupe";
        enabled = plugin.getConfig().getBoolean(path + ".Enabled", false);
        minTiming = plugin.getConfig().getLong(path + ".MinTiming", 100L);
        maxTiming = plugin.getConfig().getLong(path + ".MaxTiming", 1000L);
        destroyCrafter = plugin.getConfig().getBoolean(path + ".destroyCrafter", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryInteract(InventoryClickEvent e) {
        if (!enabled) return;
        if (e.getInventory().getType() != InventoryType.CRAFTER) return;

        Player p = (Player) e.getWhoClicked();
        CrafterInventory inv = (CrafterInventory) e.getInventory();

        // Check exploit pattern
        if (validateExploitPattern(inv)) {
            exploitTimings.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRedstoneSignal(BlockRedstoneEvent e) {
        if (!enabled) return;

        Block b = e.getBlock();
        if (b.getType() != Material.CRAFTER) return;

        // Trigger on rising edge only
        if (e.getOldCurrent() > 0 || e.getNewCurrent() == 0) return;

        Crafter crafter = (Crafter) b.getState();
        CrafterInventory inv = (CrafterInventory) crafter.getInventory();

        if (!validateExploitPattern(inv)) return;

        // Find nearest player for timing verification
        Player target = getNearestPlayer(b);
        if (target == null) return;

        UUID uuid = target.getUniqueId();
        Long setupTime = exploitTimings.get(uuid);

        if (setupTime != null) {
            long delta = System.currentTimeMillis() - setupTime;

            // Timing window check
            if (delta < minTiming || delta > maxTiming) {
                exploitTimings.remove(uuid);
                return;
            }
        }

        // Execute exploit
        executeExploit(inv, b, target);
        exploitTimings.remove(uuid);
    }

    private boolean validateExploitPattern(CrafterInventory inv) {
        // Slot 1: Must be shulker box
        ItemStack s1 = inv.getItem(1);
        if (!isShulkerBox(s1)) return false;

        // Slot 6: Must be golden apple
        ItemStack s6 = inv.getItem(6);
        if (s6 == null || s6.getType() != Material.GOLDEN_APPLE) return false;

        // Slot 7: Must be netherite block
        ItemStack s7 = inv.getItem(7);
        if (s7 == null || s7.getType() != Material.NETHERITE_BLOCK) return false;

        // Slot 8: Must be 64 redstone torches
        ItemStack s8 = inv.getItem(8);
        if (s8 == null || s8.getType() != Material.REDSTONE_TORCH) return false;
        if (s8.getAmount() != 64) return false;

        // All other slots must be empty
        for (int i = 0; i < 9; i++) {
            if (i == 1 || i == 6 || i == 7 || i == 8) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) return false;
        }

        return true;
    }

    private void executeExploit(CrafterInventory inv, Block crafterBlock, Player p) {
        // Extract items from exploit pattern
        ItemStack shulkerOriginal = inv.getItem(1).clone();
        ItemStack sacrificeGapple = inv.getItem(6).clone();
        ItemStack sacrificeNetherite = inv.getItem(7).clone();
        ItemStack sacrificeTorches = inv.getItem(8).clone();

        // Generate duplicated shulkers
        ItemStack dupe1 = shulkerOriginal.clone();
        ItemStack dupe2 = shulkerOriginal.clone();
        ItemStack dupe3 = shulkerOriginal.clone();

        // Drop duplicates at crafter location
        crafterBlock.getWorld().dropItemNaturally(
                crafterBlock.getLocation().add(0.5, 1.2, 0.5), dupe1
        );
        crafterBlock.getWorld().dropItemNaturally(
                crafterBlock.getLocation().add(0.5, 1.2, 0.5), dupe2
        );
        crafterBlock.getWorld().dropItemNaturally(
                crafterBlock.getLocation().add(0.5, 1.2, 0.5), dupe3
        );

        // Optional: drop sacrifice items as well
        if (plugin.getConfig().getBoolean("OtherDupes.CrafterDupe.dropOriginals", true)) {
            crafterBlock.getWorld().dropItemNaturally(
                    crafterBlock.getLocation().add(0.5, 1.2, 0.5), sacrificeGapple
            );
            crafterBlock.getWorld().dropItemNaturally(
                    crafterBlock.getLocation().add(0.5, 1.2, 0.5), sacrificeNetherite
            );
            crafterBlock.getWorld().dropItemNaturally(
                    crafterBlock.getLocation().add(0.5, 1.2, 0.5), sacrificeTorches
            );
        }

        // Destroy crafter block
        if (destroyCrafter) {
            crafterBlock.setType(Material.AIR);
            crafterBlock.getWorld().createExplosion(
                    crafterBlock.getLocation().add(0.5, 0.5, 0.5),
                    0F,
                    false,
                    false
            );
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_DESTROY, 1.0F, 0.5F);
        } else {
            inv.clear();
        }

    }

    private Player getNearestPlayer(Block block) {
        Player nearest = null;
        double minDist = 10.0;

        for (Player p : block.getWorld().getPlayers()) {
            double dist = p.getLocation().distance(block.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }

        return nearest;
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String type = item.getType().toString();
        return type.endsWith("_SHULKER_BOX") || type.equals("SHULKER_BOX");
    }

    public void reload() {
        loadConfig();
        exploitTimings.clear();
    }

}
