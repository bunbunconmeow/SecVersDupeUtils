package org.secverse.SecVerseDupeUtils.Dupes.Dropper;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Dropper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.secverse.SecVerseDupeUtils.helper.EventsKeys;
import org.secverse.SecVerseDupeUtils.helper.CleanShulker;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DropperDupe implements Listener {

    private final Map<UUID, Long> exploitTimings = new HashMap<>();
    private final Plugin plugin;
    private final Logger log;

    private boolean enabled;
    private boolean debug;
    private int multiplier;

    public DropperDupe(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        loadConfig();
    }

    // -------------- Config --------------

    private void loadConfig() {
        String path = "OtherDupes.DropperDupe";
        enabled = plugin.getConfig().getBoolean(path + ".Enabled", false);
        debug = false;
        multiplier = plugin.getConfig().getInt(path + ".Multiplier", 2);

        info("Config loaded: enabled=" + enabled
                + ", debug=" + debug
                + ", multiplier=" + multiplier);
    }

    public void reload() {
        info("Reload requested...");
        loadConfig();
        exploitTimings.clear();
        info("Reload complete. Cleared timing entries.");
    }

    // -------------- Events --------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!enabled) return;

        Block block = event.getBlock();
        if (block.getType() != Material.DROPPER) return;

        Player nearest = getNearestPlayer(block, 32.0);
        if (nearest == null) {
            debug("No player nearby for dropper event");
            return;
        }

        executeExploit(event, block, nearest);
        debug("Exploit executed.");
    }

    // -------------- Exploit logic --------------

    private void executeExploit(BlockDispenseEvent event, Block dropperBlock, Player p) {
        try {
            // Get the item that was actually dispensed
            ItemStack original = event.getItem();
            if (original == null || original.getType() == Material.AIR) {
                debug("No item to duplicate in dropper");
                return;
            }

            // Check if item is blacklisted
            EventsKeys ek = new EventsKeys(plugin);
            if (ek.isBlockedItem(original)) {
                debug("Item is blacklisted, skipping duplication: " + itemToStr(original));
                return;
            }

            // Clean shulkers that might contain blacklisted items
            CleanShulker.cleanShulker(original, ek);

            debug("Execute: duplicating " + itemToStr(original));

            // Drop duplicates based on multiplier
            var dropLoc = dropperBlock.getLocation().add(0.5, 0.5, 0.5);
            for (int i = 0; i < multiplier - 1; i++) { // -1 because we already have the original
                ItemStack dupe = original.clone();
                dropperBlock.getWorld().dropItemNaturally(dropLoc, dupe);
            }

            // Play sound effect
            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0F, 0.5F);
            debug("Duplication complete and SFX played.");

        } catch (Throwable t) {
            error("Error in executeExploit: " + t.getMessage(), t);
        }
    }

    // -------------- Helpers --------------

    private Player getNearestPlayer(Block block, double maxDist) {
        Player nearest = null;
        double minDistSq = maxDist * maxDist;

        for (Player p : block.getWorld().getPlayers()) {
            double distSq = p.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5));
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = p;
            }
        }
        if (nearest != null) {
            debug("Nearest player: " + playerToStr(nearest) + ", dist=" + String.format(Locale.US, "%.2f", Math.sqrt(minDistSq)));
        }
        return nearest;
    }

    private String itemToStr(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : "";
        return item.getType().name() + "x" + item.getAmount() + (name.isEmpty() ? "" : ("(\"" + name + "\")"));
    }

    private String playerToStr(Player p) {
        return p.getName() + "@" + p.getWorld().getName()
                + "(" + String.format(Locale.US, "%.1f", p.getLocation().getX())
                + "," + String.format(Locale.US, "%.1f", p.getLocation().getY())
                + "," + String.format(Locale.US, "%.1f", p.getLocation().getZ()) + ")";
    }

    // -------------- Logging --------------

    private void info(String msg) {
        log.log(Level.INFO, "[DropperDupe] " + msg);
    }

    private void debug(String msg) {
        if (debug) {
            log.log(Level.INFO, "[DropperDupe][DEBUG] " + msg);
        }
    }

    private void error(String msg, Throwable t) {
        log.log(Level.SEVERE, "[DropperDupe][ERROR] " + msg, t);
    }
}