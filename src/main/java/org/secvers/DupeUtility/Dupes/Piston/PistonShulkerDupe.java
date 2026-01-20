package org.secvers.DupeUtility.Dupes.Piston;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.Plugin;
import org.secvers.DupeUtility.Helper.EventsKeys;

import java.util.HashMap;
import java.util.Map;

import static org.secvers.DupeUtility.Helper.CleanShulker.cleanShulker;

public class PistonShulkerDupe implements Listener {
    private final Plugin plugin;
    private final EventsKeys ek;
    private final Map<Location, Long> lastDupeTimes = new HashMap<>();
    private boolean enabled;
    private int multiplier;
    private long cooldownMs;

    public PistonShulkerDupe(Plugin plugin) {
        this.plugin = plugin;
        this.ek = new EventsKeys(plugin);
        loadConfig();
    }

    public void reload() {
        loadConfig();
        ek.reload();
        lastDupeTimes.clear();
    }

    private void loadConfig() {
        String basePath = "OtherDupes.PistonShulkerDupe";
        this.enabled = plugin.getConfig().getBoolean(basePath + ".Enabled", false);
        this.multiplier = Math.max(1, plugin.getConfig().getInt(basePath + ".Multiplier", 1));
        this.cooldownMs = plugin.getConfig().getLong(basePath + ".CooldownMs", 1000L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePistonMove(event.getBlock(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePistonMove(event.getBlock(), event.getDirection());
    }

    private void handlePistonMove(Block piston, org.bukkit.block.BlockFace direction) {
        if (!enabled) {
            return;
        }

        if (!isPistonFacing(piston, direction)) {
            return;
        }

        Block gapBlock = piston.getRelative(direction);
        Block oppositePiston = gapBlock.getRelative(direction);

        if (!isPiston(oppositePiston) || !isPistonFacing(oppositePiston, direction.getOppositeFace())) {
            return;
        }

        if (!isShulkerBox(gapBlock)) {
            return;
        }

        Location gapLocation = gapBlock.getLocation();
        long now = System.currentTimeMillis();
        long lastDupe = lastDupeTimes.getOrDefault(gapLocation, 0L);
        if (now - lastDupe < cooldownMs) {
            return;
        }
        lastDupeTimes.put(gapLocation, now);

        ItemStack shulkerItem = toShulkerItem(gapBlock);
        if (shulkerItem == null) {
            return;
        }

        cleanShulker(shulkerItem, ek, ek.getIllegalItemValidator());

        for (int i = 0; i < multiplier; i++) {
            gapBlock.getWorld().dropItemNaturally(gapLocation.clone().add(0.5, 0.5, 0.5), shulkerItem.clone());
        }
    }

    private boolean isPiston(Block block) {
        Material type = block.getType();
        return type == Material.PISTON || type == Material.STICKY_PISTON;
    }

    private boolean isPistonFacing(Block block, org.bukkit.block.BlockFace face) {
        if (!isPiston(block)) {
            return false;
        }
        if (!(block.getBlockData() instanceof Directional)) {
            return false;
        }
        Directional directional = (Directional) block.getBlockData();
        return directional.getFacing() == face;
    }

    private boolean isShulkerBox(Block block) {
        Material type = block.getType();
        return type == Material.SHULKER_BOX || type.name().endsWith("_SHULKER_BOX");
    }

    private ItemStack toShulkerItem(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof ShulkerBox)) {
            return null;
        }

        ItemStack item = new ItemStack(block.getType());
        if (!(item.getItemMeta() instanceof BlockStateMeta)) {
            return item;
        }

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        meta.setBlockState(state);
        item.setItemMeta(meta);
        return item;
    }
}