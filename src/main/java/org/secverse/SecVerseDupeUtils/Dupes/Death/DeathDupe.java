package org.secverse.SecVerseDupeUtils.Dupes.Death;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.secverse.SecVerseDupeUtils.Helper.EventsKeys;
import org.secverse.SecVerseDupeUtils.Helper.CleanShulker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathDupe implements Listener {
    private final Plugin plugin;
    private final Map<UUID, DeathData> playerDeathData = new HashMap<>();
    private final Map<UUID, Boolean> disconnectedDuringDeath = new HashMap<>();
    private boolean isEnabled;

    public DeathDupe(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        isEnabled = config.getBoolean("OtherDupes.DeathDupe.Enabled", true);
        Bukkit.getLogger().info("[DeathDupe] Config reloaded. Enabled: " + isEnabled);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled) return;

        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        Bukkit.getLogger().info("[DeathDupe] Player died: " + player.getName());

        // Store the player's inventory and death location
        ItemStack[] inventory = player.getInventory().getContents().clone();
        Location deathLocation = player.getLocation();

        // Filter out blacklisted items and clean shulkers before storing
        EventsKeys ek = new EventsKeys(plugin);
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                if (ek.isBlockedItem(inventory[i])) {
                    inventory[i] = null;
                    Bukkit.getLogger().info("[DeathDupe] Removed blacklisted item from inventory for: " + player.getName());
                } else {
                    // Clean shulkers that might contain blacklisted items
                    CleanShulker.cleanShulker(inventory[i], ek);
                }
            }
        }

        // Store the death data
        playerDeathData.put(playerId, new DeathData(inventory, deathLocation));

        // Mark that they haven't disconnected during death yet
        disconnectedDuringDeath.put(playerId, false);

        Bukkit.getLogger().info("[DeathDupe] Stored death data for player: " + player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isEnabled) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Bukkit.getLogger().info("[DeathDupe] Player quit: " + player.getName());

        // Check if this player has died recently (has death data)
        if (playerDeathData.containsKey(playerId)) {
            // Mark that they disconnected during death
            disconnectedDuringDeath.put(playerId, true);
            Bukkit.getLogger().info("[DeathDupe] Player disconnected during death: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!isEnabled) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Bukkit.getLogger().info("[DeathDupe] Player respawned: " + player.getName());

        // Only process if the player didn't disconnect during death
        if (disconnectedDuringDeath.getOrDefault(playerId, false)) {
            Bukkit.getLogger().info("[DeathDupe] Player had disconnected during death, skipping respawn processing: " + player.getName());
            return;
        }

        // Check if we have death data for this player
        DeathData deathData = playerDeathData.get(playerId);
        if (deathData != null) {
            // Clear the death data since they respawning normally
            playerDeathData.remove(playerId);
            disconnectedDuringDeath.remove(playerId);
            Bukkit.getLogger().info("[DeathDupe] Cleared death data for normally respawning player: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Bukkit.getLogger().info("[DeathDupe] Player joined: " + player.getName());

        // Check if we have death data for this player and they disconnected during death
        DeathData deathData = playerDeathData.get(playerId);
        if (deathData != null && disconnectedDuringDeath.getOrDefault(playerId, false)) {
            Bukkit.getLogger().info("[DeathDupe] Processing death dupe for player: " + player.getName());

            // Restore inventory on join
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().setContents(deathData.getInventory());

                // Remove the death data after processing
                playerDeathData.remove(playerId);
                disconnectedDuringDeath.remove(playerId);

                Bukkit.getLogger().info("[DeathDupe] Completed death dupe for player: " + player.getName());
            }, 1L); // Delay to ensure player is fully joined
        }
    }

    // Helper class to store death data
    private static class DeathData {
        private final ItemStack[] inventory;
        private final Location deathLocation;

        public DeathData(ItemStack[] inventory, Location deathLocation) {
            this.inventory = inventory;
            this.deathLocation = deathLocation;
        }

        public ItemStack[] getInventory() {
            return inventory;
        }

    }
}