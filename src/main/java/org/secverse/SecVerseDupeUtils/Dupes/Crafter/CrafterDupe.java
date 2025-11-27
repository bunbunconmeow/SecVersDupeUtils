package org.secverse.SecVerseDupeUtils.Dupes.Crafter;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.secverse.SecVerseDupeUtils.Helper.CleanShulker;
import org.secverse.SecVerseDupeUtils.Helper.EventsKeys;
import org.secverse.SecVerseDupeUtils.Helper.IllegalItemValidator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrafterDupe implements Listener {
    private final EventsKeys eventsKeys;
    private final IllegalItemValidator itemValidator;
    private final Map<UUID, Long> exploitTimings = new HashMap<>();
    private final Plugin plugin;
    private final Logger log;

    private boolean enabled;
    private boolean debug;
    private long minTiming;
    private long maxTiming;
    private boolean destroyCrafter;

    public CrafterDupe(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.eventsKeys = new EventsKeys(plugin);
        this.itemValidator = new IllegalItemValidator(plugin);
        loadConfig();
    }

    private void loadConfig() {
        String path = "OtherDupes.CrafterDupe";
        enabled = plugin.getConfig().getBoolean(path + ".Enabled", false);
        debug = false;
        minTiming = plugin.getConfig().getLong(path + ".MinTiming", 100L);
        maxTiming = plugin.getConfig().getLong(path + ".MaxTiming", 1000L);
        destroyCrafter = plugin.getConfig().getBoolean(path + ".destroyCrafter", true);
    }

    public void reload() {
        loadConfig();
        exploitTimings.clear();
    }

    // -------------- Events --------------

    // MONITOR + ignoreCancelled, then read after 1 tick to see final inventory content
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryInteract(InventoryClickEvent e) {
        if (!enabled) return;

        Inventory top = e.getView() != null ? e.getView().getTopInventory() : null;
        if (top == null || top.getType() != InventoryType.CRAFTER) return;

        Player p = (Player) e.getWhoClicked();
        debug("InventoryClick: topInvImpl=" + top.getClass().getName()
                + ", action=" + e.getAction()
                + ", click=" + e.getClick()
                + ", rawSlot=" + e.getRawSlot() + ", slot=" + e.getSlot()
                + ", player=" + playerToStr(p));

        // Wait one tick so the click has been applied
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                Optional<String> err = patternError(top);
                if (err.isEmpty()) {
                    exploitTimings.put(p.getUniqueId(), System.currentTimeMillis());
                    debug("Pattern OK after click. Timing set. Grid:\n");
                } else {
                    debug("Pattern NOT met after click: " + err.get() + "\nGrid:\n");
                }
            } catch (Throwable t) {
                error("Error during delayed pattern check: " + t.getMessage(), t);
            }
        });
    }

    // Redstone change: look around for a crafter (rising edge)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRedstoneSignal(BlockRedstoneEvent e) {
        if (!enabled) return;

        try {
            Block src = e.getBlock();
            debug("RedstoneEvent: src=" + blockToStr(src)
                    + ", old=" + e.getOldCurrent() + ", new=" + e.getNewCurrent());

            // Rising edge only
            if (e.getOldCurrent() > 0 || e.getNewCurrent() == 0) {
                debug("Skip: not a rising edge.");
                return;
            }

            Block crafterBlock = findTargetCrafter(src, 2); // BFS up to 2 blocks
            if (crafterBlock == null) {
                debug("No crafter nearby (<=2 blocks).");
                return;
            }

            Crafter crafterState = (Crafter) crafterBlock.getState();
            Inventory inv = crafterState.getInventory(); // Do NOT cast to CrafterInventory
            debug("Crafter found: " + blockToStr(crafterBlock)
                    + ", stateImpl=" + crafterState.getClass().getName()
                    + ", invImpl=" + inv.getClass().getName()
                    + ", powered=" + crafterBlock.isBlockPowered());

            Optional<String> err = patternError(inv);
            if (err.isPresent()) {
                debug("Pattern NOT met on redstone trigger: " + err.get() + "\nGrid:\n");
                return;
            }
            debug("Pattern OK on redstone trigger.\nGrid:\n");

            Player target = getNearestPlayer(crafterBlock, 32.0); // 32 blocks radius
            if (target == null) {
                debug("Skip: no player in range.");
                return;
            }

            UUID uuid = target.getUniqueId();
            Long setupTime = exploitTimings.get(uuid);
            if (setupTime != null) {
                long delta = System.currentTimeMillis() - setupTime;
                debug("Timing check: delta=" + delta + "ms, window=[" + minTiming + "," + maxTiming + "]ms");
                if (delta < minTiming || delta > maxTiming) {
                    exploitTimings.remove(uuid);
                    debug("Skip: delta outside window. Timing removed.");
                    return;
                }
            } else {
                debug("Note: no timing entry for " + target.getName() + " present.");
            }

            executeExploit(inv, crafterBlock, target);
            exploitTimings.remove(uuid);
            debug("Exploit executed; timing removed.");

        } catch (Throwable t) {
            error("Error in redstone handler: " + t.getMessage(), t);
        }
    }

    // -------------- Exploit logic --------------

    // 3x3 slots: 0 1 2 / 3 4 5 / 6 7 8
    // Example pattern:
    // - Slot 2: Shulker box
    // - Slot 6: GOLDEN_APPLE
    // - Slot 7: NETHERITE_BLOCK
    // - Slot 8: REDSTONE_TORCH x64
    // - All others empty
    private Optional<String> patternError(Inventory inv) {
        if (inv.getSize() < 9) return Optional.of("Inventory size < 9: " + inv.getSize());

        ItemStack s2 = inv.getItem(2);
        if (!isShulkerBox(s2)) return Optional.of("Slot 2: not a shulker box (" + itemToStr(s2) + ")");

        ItemStack s6 = inv.getItem(6);
        if (s6 == null || s6.getType() != Material.GOLDEN_APPLE)
            return Optional.of("Slot 6: expected GOLDEN_APPLE, got " + itemToStr(s6));

        ItemStack s7 = inv.getItem(7);
        if (s7 == null || s7.getType() != Material.NETHERITE_BLOCK)
            return Optional.of("Slot 7: expected NETHERITE_BLOCK, got " + itemToStr(s7));

        ItemStack s8 = inv.getItem(8);
        if (s8 == null || s8.getType() != Material.REDSTONE_TORCH)
            return Optional.of("Slot 8: expected REDSTONE_TORCH, got " + itemToStr(s8));
        if (s8.getAmount() != 64)
            return Optional.of("Slot 8: expected amount 64, got " + (s8 == null ? "null" : s8.getAmount()));

        for (int i = 0; i < 9; i++) {
            if (i == 2 || i == 6 || i == 7 || i == 8) continue;
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) {
                return Optional.of("Slot " + i + " is not empty: " + itemToStr(it));
            }
        }
        return Optional.empty();
    }

    private void executeExploit(Inventory inv, Block crafterBlock, Player p) {
        try {
            ItemStack shulkerOriginal    = safeClone(inv.getItem(2));
            ItemStack sacrificeGapple    = safeClone(inv.getItem(6));
            ItemStack sacrificeNetherite = safeClone(inv.getItem(7));
            ItemStack sacrificeTorches   = safeClone(inv.getItem(8));

            debug("Execute: shulker=" + itemToStr(shulkerOriginal)
                    + ", gapple=" + itemToStr(sacrificeGapple)
                    + ", netherite=" + itemToStr(sacrificeNetherite)
                    + ", torches=" + itemToStr(sacrificeTorches));

            CleanShulker.cleanShulker(shulkerOriginal, eventsKeys, itemValidator);

            // Drop 3 duplicates
            ItemStack dupe1 = shulkerOriginal.clone();
            ItemStack dupe2 = shulkerOriginal.clone();
            ItemStack dupe3 = shulkerOriginal.clone();

            var dropLoc = crafterBlock.getLocation().add(0.5, 1.2, 0.5);
            crafterBlock.getWorld().dropItemNaturally(dropLoc, dupe1);
            crafterBlock.getWorld().dropItemNaturally(dropLoc, dupe2);
            crafterBlock.getWorld().dropItemNaturally(dropLoc, dupe3);

            // Optionally drop the original "sacrifice" items too
            if (plugin.getConfig().getBoolean("OtherDupes.CrafterDupe.dropOriginals", true)) {
                crafterBlock.getWorld().dropItemNaturally(dropLoc, sacrificeGapple);
                crafterBlock.getWorld().dropItemNaturally(dropLoc, sacrificeNetherite);
                crafterBlock.getWorld().dropItemNaturally(dropLoc, sacrificeTorches);
            }

            if (destroyCrafter) {
                crafterBlock.setType(Material.AIR);
                crafterBlock.getWorld().createExplosion(
                        crafterBlock.getLocation().add(0.5, 0.5, 0.5),
                        0F, false, false
                );
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0F, 0.5F);
                debug("Crafter destroyed and SFX played.");
            } else {
                inv.clear();
                debug("Cleared crafter inventory (destroyCrafter=false).");
            }
        } catch (Throwable t) {
            error("Error in executeExploit: " + t.getMessage(), t);
        }
    }

    // -------------- Helpers --------------

    private Block findTargetCrafter(Block src, int maxSteps) {
        if (src == null) return null;
        if (src.getType() == Material.CRAFTER) return src;

        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        Set<Block> visited = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        Map<Block, Integer> depth = new HashMap<>();

        q.add(src);
        depth.put(src, 0);
        visited.add(src);

        while (!q.isEmpty()) {
            Block cur = q.poll();
            int d = depth.getOrDefault(cur, 0);
            if (d >= maxSteps) continue;

            for (BlockFace f : faces) {
                Block nb = cur.getRelative(f);
                if (!visited.add(nb)) continue;

                if (nb.getType() == Material.CRAFTER) {
                    debug("findTargetCrafter: crafter at " + blockToStr(nb) + " dist=" + (d + 1));
                    return nb;
                }
                depth.put(nb, d + 1);
                q.add(nb);
            }
        }
        return null;
    }

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

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String type = item.getType().name();
        return type.equals("SHULKER_BOX") || type.endsWith("_SHULKER_BOX");
    }

    private ItemStack safeClone(ItemStack item) {
        if (item == null) return new ItemStack(Material.AIR);
        return item.clone();
    }

    private String itemToStr(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : "";
        return item.getType().name() + "x" + item.getAmount() + (name.isEmpty() ? "" : ("(\"" + name + "\")"));
    }

    private String blockToStr(Block b) {
        return b.getType() + "@" + b.getWorld().getName()
                + "(" + b.getX() + "," + b.getY() + "," + b.getZ() + ")";
    }

    private String playerToStr(Player p) {
        return p.getName() + "@" + p.getWorld().getName()
                + "(" + String.format(Locale.US, "%.1f", p.getLocation().getX())
                + "," + String.format(Locale.US, "%.1f", p.getLocation().getY())
                + "," + String.format(Locale.US, "%.1f", p.getLocation().getZ()) + ")";
    }

    // -------------- Logging --------------

    private void info(String msg) {
        log.log(Level.INFO, "[CrafterDupe] " + msg);
    }

    private void debug(String msg) {
        if (debug) {
            // Level.FINE is often suppressed -> INFO with [DEBUG]
            log.log(Level.INFO, "[CrafterDupe][DEBUG] " + msg);
        }
    }

    private void error(String msg, Throwable t) {
        log.log(Level.SEVERE, "[CrafterDupe][ERROR] " + msg, t);
    }
}
