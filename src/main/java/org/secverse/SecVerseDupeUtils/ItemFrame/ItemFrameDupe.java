package org.secverse.SecVerseDupeUtils.ItemFrame;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.secverse.SecVerseDupeUtils.SecVersDupe;
import org.secverse.SecVerseDupeUtils.helper.CleanShulker;
import org.secverse.SecVerseDupeUtils.helper.EventsKeys;

public class ItemFrameDupe {
    private final SecVersDupe plugin;
    private EventsKeys ek;

    private boolean ITEM_FRAME_Enabled, GLOW_ITEM_FRAME_Enabled;
    private int ITEM_FRAME_Multiplier, GLOW_ITEM_FRAME_Multiplier;
    private int ITEM_FRAME_Probability, GLOW_ITEM_FRAME_Probability;

    public ItemFrameDupe(SecVersDupe plugin) {
        this.plugin = plugin;
        this.ek = new EventsKeys(plugin);
        reload(); // Initial load
    }

    public void reload() {
        // Reload EventsKeys with updated config
        this.ek.reload();

        // Reload FrameDupe settings
        this.ITEM_FRAME_Enabled = plugin.getConfig().getBoolean("FrameDupe.Enabled", false);
        this.ITEM_FRAME_Multiplier = plugin.getConfig().getInt("FrameDupe.Multiplier", 1);
        this.ITEM_FRAME_Probability = plugin.getConfig().getInt("FrameDupe.Probability-percentage", 0);

        // Reload GLOW_FrameDupe settings
        this.GLOW_ITEM_FRAME_Enabled = plugin.getConfig().getBoolean("GLOW_FrameDupe.Enabled", false);
        this.GLOW_ITEM_FRAME_Multiplier = plugin.getConfig().getInt("GLOW_FrameDupe.Multiplier", 1);
        this.GLOW_ITEM_FRAME_Probability = plugin.getConfig().getInt("GLOW_FrameDupe.Probability-percentage", 0);
    }

    public class FrameAll implements Listener {
        @EventHandler
        private void onFrameBreak(EntityDamageByEntityEvent event) {
            handleFrameBreak(event, EntityType.ITEM_FRAME, ITEM_FRAME_Enabled, ITEM_FRAME_Multiplier, ITEM_FRAME_Probability);
        }
    }

    public class FrameSpecific implements Listener {
        @EventHandler
        private void onFrameBreak(EntityDamageByEntityEvent event) {
            handleFrameBreak(event, EntityType.GLOW_ITEM_FRAME, GLOW_ITEM_FRAME_Enabled, GLOW_ITEM_FRAME_Multiplier, GLOW_ITEM_FRAME_Probability);
        }
    }

    private void handleFrameBreak(EntityDamageByEntityEvent event, EntityType expectedType, boolean enabled, int multiplier, int probability) {
        if (!enabled) return;
        if (event.getEntityType() != expectedType) return;

        ItemStack item;
        if (expectedType == EntityType.ITEM_FRAME) {
            item = ((ItemFrame) event.getEntity()).getItem();
        } else if (expectedType == EntityType.GLOW_ITEM_FRAME) {
            item = ((GlowItemFrame) event.getEntity()).getItem();
        } else {
            return;
        }

        // Blocked item check
        if (ek.isBlockedItem(item)) return;

        // Clean blocked contents in shulkers
        CleanShulker.cleanShulker(item, ek);

        // Calculate duplication chance
        int rng = (int) (Math.random() * 100);
        if (rng < probability) {
            for (int i = 0; i < multiplier; i++) {
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), item.clone());
            }
        }
    }
}