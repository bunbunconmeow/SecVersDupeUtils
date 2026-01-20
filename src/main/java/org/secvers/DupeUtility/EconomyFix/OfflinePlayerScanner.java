package org.secvers.DupeUtility.EconomyFix;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.*;

public class OfflinePlayerScanner {

    private final JavaPlugin plugin;
    private final ItemFixer itemFixer;
    private final ItemScanner itemScanner;
    private final EconomyFixManager manager;

    private final int maxPlayersPerScan;
    private final int chunkRadius;
    private final int chunkLoadTimeout;
    private final boolean unloadAfterScan;

    // Track loaded chunks for cleanup
    private final Set<ChunkPosition> loadedChunks = ConcurrentHashMap.newKeySet();

    // Track last scan times to avoid re-scanning
    private final Map<UUID, Long> lastScanTimes = new ConcurrentHashMap<>();
    private final long SCAN_COOLDOWN_MS;

    public OfflinePlayerScanner(JavaPlugin plugin, ItemScanner scanner,
                                EconomyFixManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.itemScanner = scanner;
        this.itemFixer = new ItemFixer(plugin);

        this.maxPlayersPerScan = plugin.getConfig().getInt("EconomyFix.MaxOfflinePlayersPerScan", 50);
        this.chunkRadius = plugin.getConfig().getInt("EconomyFix.ChunkRadiusAroundLogout", 2);
        this.chunkLoadTimeout = plugin.getConfig().getInt("EconomyFix.ChunkLoadTimeoutSeconds", 10);
        this.unloadAfterScan = plugin.getConfig().getBoolean("EconomyFix.UnloadChunksAfterScan", true);
        this.SCAN_COOLDOWN_MS = plugin.getConfig().getInt("EconomyFix.OfflineScanIntervalSeconds", 3600) * 1000L;
    }

    /**
     * Scan offline players and their logout locations
     */
    public CompletableFuture<ScanResult> scanOfflinePlayers() {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            long startTime = System.currentTimeMillis();

            List<OfflinePlayer> offlinePlayers = getOfflinePlayersToScan();

            plugin.getLogger().info(String.format(
                    "Starting offline scan for %d players...", offlinePlayers.size()
            ));

            for (OfflinePlayer offlinePlayer : offlinePlayers) {
                try {
                    ScanResult playerResult = scanOfflinePlayer(offlinePlayer).get(30, TimeUnit.SECONDS);
                    result.merge(playerResult);

                    // Mark as scanned
                    lastScanTimes.put(offlinePlayer.getUniqueId(), System.currentTimeMillis());

                } catch (Exception e) {
                    plugin.getLogger().warning(String.format(
                            "Failed to scan offline player %s: %s",
                            offlinePlayer.getName(), e.getMessage()
                    ));
                }

                // Small delay between players to prevent lag
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Cleanup loaded chunks
            cleanupLoadedChunks();

            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info(String.format(
                    "Offline scan completed: %d players, %d items fixed, %d chunks scanned (%.2fs)",
                    offlinePlayers.size(), result.fixedItems, result.scannedChunks, duration / 1000.0
            ));

            return result;
        });
    }

    /**
     * Get list of offline players that need scanning
     */
    private List<OfflinePlayer> getOfflinePlayersToScan() {
        OfflinePlayer[] allOffline = Bukkit.getOfflinePlayers();
        List<OfflinePlayer> toScan = new ArrayList<>();

        long currentTime = System.currentTimeMillis();

        for (OfflinePlayer player : allOffline) {
            // Skip online players
            if (player.isOnline()) {
                continue;
            }

            // Skip if never played
            if (!player.hasPlayedBefore()) {
                continue;
            }

            // Check cooldown
            UUID uuid = player.getUniqueId();
            Long lastScan = lastScanTimes.get(uuid);
            if (lastScan != null && (currentTime - lastScan) < SCAN_COOLDOWN_MS) {
                continue;
            }

            toScan.add(player);

            // Limit per scan
            if (toScan.size() >= maxPlayersPerScan) {
                break;
            }
        }

        return toScan;
    }

    /**
     * Scan a single offline player
     */
    private CompletableFuture<ScanResult> scanOfflinePlayer(OfflinePlayer offlinePlayer) {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();

            // Load player data on main thread
            CompletableFuture<PlayerData> dataFuture = CompletableFuture.supplyAsync(() -> {
                return loadPlayerData(offlinePlayer);
            }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));

            PlayerData data;
            try {
                data = dataFuture.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data for " + offlinePlayer.getName());
                return result;
            }

            // Scan inventory
            if (manager.isOfflinePlayerScanEnabled() && data.inventory != null) {
                result.fixedItems += itemFixer.fixInventory(data.inventory);
                result.scannedContainers++;
            }

            // Scan ender chest
            if (manager.isOfflineEChestScanEnabled() && data.enderChest != null) {
                result.fixedItems += itemFixer.fixInventory(data.enderChest);
                result.scannedContainers++;
            }

            // Save if modified
            if (result.fixedItems > 0) {
                savePlayerData(offlinePlayer, data);
            }

            // Scan logout location chunks
            if (manager.isChunkLoadingEnabled() && data.lastLocation != null) {
                ScanResult chunkResult = scanLogoutChunks(data.lastLocation).join();
                result.merge(chunkResult);
            }

            return result;
        });
    }

    /**
     * Load player data from disk
     */
    private PlayerData loadPlayerData(OfflinePlayer offlinePlayer) {
        PlayerData data = new PlayerData();

        try {
            // Try to get from online player first (if they just logged in)
            Player player = offlinePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                data.inventory = player.getInventory();
                data.enderChest = player.getEnderChest();
                data.lastLocation = player.getLocation();
                return data;
            }

            // Get last location from bedspawn or world spawn
            data.lastLocation = offlinePlayer.getBedSpawnLocation();
            if (data.lastLocation == null) {
                World world = Bukkit.getWorlds().get(0);
                data.lastLocation = world.getSpawnLocation();
            }

            // Note: Full offline inventory loading requires NBT parsing
            // For now, we'll only scan chunks around logout location
            // To implement full offline inventory access, see NBTPlayerDataLoader

        } catch (Exception e) {
            plugin.getLogger().warning("Error loading player data: " + e.getMessage());
        }

        return data;
    }

    /**
     * Save modified player data back to disk
     */
    private void savePlayerData(OfflinePlayer offlinePlayer, PlayerData data) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // If player is online, save directly
                Player player = offlinePlayer.getPlayer();
                if (player != null && player.isOnline()) {
                    player.saveData();
                    plugin.getLogger().info("Saved fixed items for " + offlinePlayer.getName());
                }
                // For offline players, NBT writing would be needed
            } catch (Exception e) {
                plugin.getLogger().warning("Error saving player data: " + e.getMessage());
            }
        });
    }

    /**
     * Scan chunks around logout location (Paper synchronous loading)
     */
    private CompletableFuture<ScanResult> scanLogoutChunks(Location location) {
        CompletableFuture<ScanResult> future = new CompletableFuture<>();

        // Must run on main thread for Paper
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                ScanResult result = new ScanResult();

                if (location == null || location.getWorld() == null) {
                    future.complete(result);
                    return;
                }

                World world = location.getWorld();
                int centerX = location.getChunk().getX();
                int centerZ = location.getChunk().getZ();

                // Load and scan chunks in radius
                for (int x = -chunkRadius; x <= chunkRadius; x++) {
                    for (int z = -chunkRadius; z <= chunkRadius; z++) {
                        int chunkX = centerX + x;
                        int chunkZ = centerZ + z;

                        try {
                            Chunk chunk = loadChunkSync(world, chunkX, chunkZ);

                            if (chunk != null) {
                                ScanResult chunkResult = scanChunkSync(chunk);
                                result.merge(chunkResult);
                                result.scannedChunks++;

                                // Track for cleanup
                                loadedChunks.add(new ChunkPosition(world, chunkX, chunkZ));
                            }

                        } catch (Exception e) {
                            plugin.getLogger().warning(String.format(
                                    "Error scanning chunk [%d, %d]: %s",
                                    chunkX, chunkZ, e.getMessage()
                            ));
                        }
                    }
                }

                future.complete(result);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Load chunk synchronously on main thread (Paper)
     */
    private Chunk loadChunkSync(World world, int x, int z) {
        try {
            // Check if already loaded
            if (world.isChunkLoaded(x, z)) {
                return world.getChunkAt(x, z);
            }

            // Load chunk - Paper will handle this efficiently
            return world.getChunkAt(x, z);

        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                    "Failed to load chunk [%d, %d]: %s", x, z, e.getMessage()
            ));
            return null;
        }
    }

    /**
     * Scan all containers in a chunk synchronously
     */
    private ScanResult scanChunkSync(Chunk chunk) {
        ScanResult result = new ScanResult();

        try {
            for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                if (state instanceof org.bukkit.block.Container container) {
                    result.fixedItems += itemFixer.fixInventory(container.getInventory());
                    result.scannedContainers++;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error scanning chunk containers: " + e.getMessage());
        }

        return result;
    }

    /**
     * Cleanup chunks that were loaded for scanning
     */
    private void cleanupLoadedChunks() {
        if (!unloadAfterScan || loadedChunks.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            int unloaded = 0;

            for (ChunkPosition pos : loadedChunks) {
                try {
                    if (pos.world.isChunkLoaded(pos.x, pos.z)) {
                        // Check if chunk has players nearby
                        Location chunkCenter = new Location(
                                pos.world,
                                (pos.x << 4) + 8,
                                64,
                                (pos.z << 4) + 8
                        );

                        boolean hasNearbyPlayers = pos.world.getNearbyEntities(
                                chunkCenter, 128, 128, 128
                        ).stream().anyMatch(e -> e instanceof Player);

                        if (!hasNearbyPlayers) {
                            pos.world.unloadChunk(pos.x, pos.z, true);
                            unloaded++;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to unload chunk: " + e.getMessage());
                }
            }

            loadedChunks.clear();

            if (unloaded > 0) {
                plugin.getLogger().info("Unloaded " + unloaded + " chunks after scanning");
            }
        });
    }

    /**
     * Shutdown and cleanup
     */
    public void shutdown() {
        cleanupLoadedChunks();
        lastScanTimes.clear();
    }

    // Helper classes

    private static class PlayerData {
        org.bukkit.inventory.Inventory inventory;
        org.bukkit.inventory.Inventory enderChest;
        Location lastLocation;
    }

    private static class ChunkPosition {
        final World world;
        final int x;
        final int z;

        ChunkPosition(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkPosition that)) return false;
            return x == that.x && z == that.z && world.equals(that.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }
    }

    static class ScanResult {
        int fixedItems = 0;
        int scannedContainers = 0;
        int scannedChunks = 0;

        void merge(ScanResult other) {
            this.fixedItems += other.fixedItems;
            this.scannedContainers += other.scannedContainers;
            this.scannedChunks += other.scannedChunks;
        }
    }
}
