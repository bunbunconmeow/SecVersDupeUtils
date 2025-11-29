package org.secvers.DupeUtility.EconomyFix;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive scanner that recursively scans all inventories including:
 * - Player inventories
 * - Ender chests
 * - World containers (Chests, Barrels, Hoppers, etc.)
 * - Shulker Boxes (nested in containers)
 * - Bundles (nested in containers or shulkers)
 * - Deep nesting (Shulker in Chest, Bundle in Shulker, etc.)
 */
public class ItemScanner {

    private static final int MAX_SCAN_DEPTH = 5;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;

    private final JavaPlugin plugin;
    private final ItemFixer itemFixer;
    private final ExecutorService executorService;

    // Configuration
    private final int chunksPerBatch;
    private final int containersPerTick;
    private final boolean verboseLogging;

    // Statistics
    private final AtomicInteger totalItemsScanned = new AtomicInteger(0);
    private final AtomicInteger totalItemsFixed = new AtomicInteger(0);
    private final AtomicInteger totalContainersScanned = new AtomicInteger(0);
    private final AtomicInteger totalNestedContainersScanned = new AtomicInteger(0);

    public ItemScanner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemFixer = new ItemFixer(plugin);

        // Thread pool configuration
        int threadPoolSize = plugin.getConfig().getInt("EconomyFix.ThreadPoolSize", -1);
        if (threadPoolSize <= 0) {
            threadPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        }

        this.executorService = Executors.newFixedThreadPool(
                threadPoolSize,
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "EconomyFix-Scanner-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );

        // Performance configuration
        this.chunksPerBatch = plugin.getConfig().getInt("EconomyFix.ChunksPerBatch", 5);
        this.containersPerTick = plugin.getConfig().getInt("EconomyFix.ContainersPerTick", 50);
        this.verboseLogging = plugin.getConfig().getBoolean("EconomyFix.VerboseLogging", false);

        plugin.getLogger().info(String.format(
                "ItemScanner initialized with %d threads (Batch: %d chunks, %d containers/tick)",
                threadPoolSize, chunksPerBatch, containersPerTick
        ));
    }

    /**
     * Scan all online player inventories
     *
     * @return CompletableFuture with scan results
     */
    public CompletableFuture<ScanResult> scanOnlinePlayers() {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            long startTime = System.currentTimeMillis();

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            if (verboseLogging) {
                plugin.getLogger().info("Starting scan of " + players.size() + " online players");
            }

            for (Player player : players) {
                try {
                    // Scan main inventory + armor + offhand
                    int fixed = scanInventoryRecursive(player.getInventory(), 0);
                    result.itemsFixed += fixed;
                    result.containersScanned++;

                    if (verboseLogging && fixed > 0) {
                        plugin.getLogger().info(String.format(
                                "Fixed %d items in %s's inventory",
                                fixed, player.getName()
                        ));
                    }

                } catch (Exception e) {
                    plugin.getLogger().warning(String.format(
                            "Error scanning player %s: %s",
                            player.getName(), e.getMessage()
                    ));
                    if (verboseLogging) {
                        e.printStackTrace();
                    }
                }
            }

            result.scanDuration = System.currentTimeMillis() - startTime;
            updateStatistics(result);

            return result;

        }, executorService);
    }

    /**
     * Scan all online player ender chests
     *
     * @return CompletableFuture with scan results
     */
    public CompletableFuture<ScanResult> scanEnderChests() {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            long startTime = System.currentTimeMillis();

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            if (verboseLogging) {
                plugin.getLogger().info("Starting ender chest scan for " + players.size() + " players");
            }

            for (Player player : players) {
                try {
                    // Scan ender chest
                    int fixed = scanInventoryRecursive(player.getEnderChest(), 0);
                    result.itemsFixed += fixed;
                    result.containersScanned++;

                    if (verboseLogging && fixed > 0) {
                        plugin.getLogger().info(String.format(
                                "Fixed %d items in %s's ender chest",
                                fixed, player.getName()
                        ));
                    }

                } catch (Exception e) {
                    plugin.getLogger().warning(String.format(
                            "Error scanning ender chest of %s: %s",
                            player.getName(), e.getMessage()
                    ));
                    if (verboseLogging) {
                        e.printStackTrace();
                    }
                }
            }

            result.scanDuration = System.currentTimeMillis() - startTime;
            updateStatistics(result);

            return result;

        }, executorService);
    }

    /**
     * Scan all world containers (chests, barrels, hoppers, etc.)
     *
     * @return CompletableFuture with scan results
     */
    public CompletableFuture<ScanResult> scanWorldContainers() {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            long startTime = System.currentTimeMillis();

            for (World world : Bukkit.getWorlds()) {
                if (verboseLogging) {
                    plugin.getLogger().info("Scanning containers in world: " + world.getName());
                }

                Chunk[] loadedChunks = world.getLoadedChunks();
                result.chunksScanned += loadedChunks.length;

                // Process chunks in batches
                List<Chunk> chunkBatch = new ArrayList<>();

                for (Chunk chunk : loadedChunks) {
                    chunkBatch.add(chunk);

                    if (chunkBatch.size() >= chunksPerBatch) {
                        result.itemsFixed += processBatch(chunkBatch);
                        chunkBatch.clear();
                    }
                }

                // Process remaining chunks
                if (!chunkBatch.isEmpty()) {
                    result.itemsFixed += processBatch(chunkBatch);
                }
            }

            result.scanDuration = System.currentTimeMillis() - startTime;
            updateStatistics(result);

            if (verboseLogging) {
                plugin.getLogger().info(String.format(
                        "World container scan completed in %dms",
                        result.scanDuration
                ));
            }

            return result;

        }, executorService);
    }

    /**
     * Process a batch of chunks on the main thread
     *
     * @param chunks Chunks to process
     * @return Number of items fixed
     */
    private int processBatch(List<Chunk> chunks) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        // Must run on main thread for Bukkit API
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int totalFixed = 0;

                for (Chunk chunk : chunks) {
                    for (BlockState blockState : chunk.getTileEntities()) {
                        if (blockState instanceof Container container) {
                            try {
                                int fixed = scanInventoryRecursive(container.getInventory(), 0);
                                totalFixed += fixed;
                                totalContainersScanned.incrementAndGet();

                                if (verboseLogging && fixed > 0) {
                                    plugin.getLogger().info(String.format(
                                            "Fixed %d items in %s at %s",
                                            fixed,
                                            container.getType().name(),
                                            container.getLocation()
                                    ));
                                }

                            } catch (Exception e) {
                                plugin.getLogger().warning(String.format(
                                        "Error scanning container at %s: %s",
                                        container.getLocation(), e.getMessage()
                                ));
                            }
                        }
                    }
                }

                future.complete(totalFixed);

            } catch (Exception e) {
                plugin.getLogger().severe("Error processing chunk batch: " + e.getMessage());
                future.complete(0);
            }
        });

        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.getLogger().warning("Batch processing timed out");
            return 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Error waiting for batch: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Recursively scan an inventory including nested containers
     *
     * @param inventory Inventory to scan
     * @param depth Current recursion depth
     * @return Number of items fixed
     */
    private int scanInventoryRecursive(Inventory inventory, int depth) {
        if (inventory == null || depth > MAX_SCAN_DEPTH) {
            if (depth > MAX_SCAN_DEPTH && verboseLogging) {
                plugin.getLogger().warning("Max scan depth reached, stopping recursion");
            }
            return 0;
        }

        int totalFixed = 0;
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            totalItemsScanned.incrementAndGet();

            // Fix the item itself
            ItemFixer.FixResult result = itemFixer.fixItem(item);
            if (result.wasFixed) {
                inventory.setItem(i, result.replacementItem);
                totalFixed++;
                totalItemsFixed.incrementAndGet();

                if (verboseLogging) {
                    plugin.getLogger().info(String.format(
                            "Fixed %s at slot %d (depth %d): %s",
                            item.getType().name(),
                            i,
                            depth,
                            String.join(", ", result.violations)
                    ));
                }
            }

            // Check for nested Shulker Boxes
            if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                BlockState blockState = blockStateMeta.getBlockState();

                if (blockState instanceof ShulkerBox shulkerBox) {
                    totalNestedContainersScanned.incrementAndGet();

                    int nestedFixed = scanInventoryRecursive(shulkerBox.getInventory(), depth + 1);

                    if (nestedFixed > 0) {
                        // Update the shulker box with fixed contents
                        blockStateMeta.setBlockState(shulkerBox);
                        item.setItemMeta(blockStateMeta);
                        inventory.setItem(i, item);
                        totalFixed += nestedFixed;

                        if (verboseLogging) {
                            plugin.getLogger().info(String.format(
                                    "Fixed %d items in Shulker Box at slot %d (depth %d)",
                                    nestedFixed, i, depth
                            ));
                        }
                    }
                }
            }

            // Check for nested Bundles
            if (item.getItemMeta() instanceof BundleMeta bundleMeta) {
                totalNestedContainersScanned.incrementAndGet();

                int bundleFixed = scanBundle(bundleMeta, inventory, i, depth + 1);

                if (bundleFixed > 0) {
                    totalFixed += bundleFixed;

                    if (verboseLogging) {
                        plugin.getLogger().info(String.format(
                                "Fixed %d items in Bundle at slot %d (depth %d)",
                                bundleFixed, i, depth
                        ));
                    }
                }
            }
        }

        return totalFixed;
    }

    /**
     * Scan and fix items inside a bundle
     *
     * @param bundleMeta Bundle metadata
     * @param parentInventory Parent inventory containing the bundle
     * @param slot Slot of the bundle in parent inventory
     * @param depth Current recursion depth
     * @return Number of items fixed
     */
    private int scanBundle(BundleMeta bundleMeta, Inventory parentInventory, int slot, int depth) {
        if (depth > MAX_SCAN_DEPTH) {
            return 0;
        }

        int totalFixed = 0;
        List<ItemStack> bundleItems = new ArrayList<>(bundleMeta.getItems());
        boolean bundleModified = false;

        for (int i = 0; i < bundleItems.size(); i++) {
            ItemStack bundleItem = bundleItems.get(i);

            if (bundleItem == null || bundleItem.getType() == Material.AIR) {
                continue;
            }

            totalItemsScanned.incrementAndGet();

            // Fix the item
            ItemFixer.FixResult result = itemFixer.fixItem(bundleItem);
            if (result.wasFixed) {
                bundleItems.set(i, result.replacementItem);
                totalFixed++;
                totalItemsFixed.incrementAndGet();
                bundleModified = true;

                if (verboseLogging) {
                    plugin.getLogger().info(String.format(
                            "Fixed %s in bundle slot %d (depth %d): %s",
                            bundleItem.getType().name(),
                            i,
                            depth,
                            String.join(", ", result.violations)
                    ));
                }
            }

            // Check for nested Shulker Boxes inside the bundle
            if (bundleItem.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                BlockState blockState = blockStateMeta.getBlockState();

                if (blockState instanceof ShulkerBox shulkerBox) {
                    totalNestedContainersScanned.incrementAndGet();

                    int nestedFixed = scanInventoryRecursive(shulkerBox.getInventory(), depth + 1);

                    if (nestedFixed > 0) {
                        blockStateMeta.setBlockState(shulkerBox);
                        bundleItem.setItemMeta(blockStateMeta);
                        bundleItems.set(i, bundleItem);
                        totalFixed += nestedFixed;
                        bundleModified = true;

                        if (verboseLogging) {
                            plugin.getLogger().info(String.format(
                                    "Fixed %d items in Shulker Box inside Bundle (depth %d)",
                                    nestedFixed, depth
                            ));
                        }
                    }
                }
            }
        }

        // Update the bundle if any items were modified
        if (bundleModified) {
            bundleMeta.setItems(bundleItems);
            ItemStack parentItem = parentInventory.getItem(slot);

            if (parentItem != null) {
                parentItem.setItemMeta(bundleMeta);
                parentInventory.setItem(slot, parentItem);
            }

            if (verboseLogging) {
                plugin.getLogger().info(String.format(
                        "Updated Bundle at slot %d with %d fixed items",
                        slot, totalFixed
                ));
            }
        }

        return totalFixed;
    }

    /**
     * Update global statistics
     *
     * @param result Scan result to add to statistics
     */
    private void updateStatistics(ScanResult result) {
        // Statistics are already updated in real-time via AtomicInteger
        // This method can be used for additional processing if needed

        if (verboseLogging) {
            plugin.getLogger().info(String.format(
                    "Scan completed: %d items fixed in %d containers (Duration: %dms)",
                    result.itemsFixed,
                    result.containersScanned,
                    result.scanDuration
            ));
        }
    }

    /**
     * Get current statistics
     *
     * @return Current scan statistics
     */
    public ScanStatistics getStatistics() {
        return new ScanStatistics(
                totalItemsScanned.get(),
                totalItemsFixed.get(),
                totalContainersScanned.get(),
                totalNestedContainersScanned.get()
        );
    }

    /**
     * Reset all statistics
     */
    public void resetStatistics() {
        totalItemsScanned.set(0);
        totalItemsFixed.set(0);
        totalContainersScanned.set(0);
        totalNestedContainersScanned.set(0);

        plugin.getLogger().info("ItemScanner statistics reset");
    }

    /**
     * Shutdown the scanner and cleanup resources
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down ItemScanner...");

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Scanner did not terminate gracefully, forcing shutdown");
                List<Runnable> pendingTasks = executorService.shutdownNow();

                if (!pendingTasks.isEmpty()) {
                    plugin.getLogger().warning(pendingTasks.size() + " pending tasks were cancelled");
                }

                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    plugin.getLogger().severe("Scanner could not be terminated");
                }
            } else {
                plugin.getLogger().info("ItemScanner shutdown successfully");
            }
        } catch (InterruptedException e) {
            plugin.getLogger().warning("Scanner shutdown interrupted");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if scanner is currently running
     *
     * @return true if scanner is active
     */
    public boolean isActive() {
        return !executorService.isShutdown() && !executorService.isTerminated();
    }

    // ==========================================
    // Data Classes
    // ==========================================

    /**
     * Result of a scan operation
     */
    public static class ScanResult {
        public int itemsFixed = 0;
        public int containersScanned = 0;
        public int chunksScanned = 0;
        public long scanDuration = 0;

        /**
         * Merge another result into this one
         *
         * @param other Result to merge
         */
        public void merge(ScanResult other) {
            this.itemsFixed += other.itemsFixed;
            this.containersScanned += other.containersScanned;
            this.chunksScanned += other.chunksScanned;
            this.scanDuration += other.scanDuration;
        }

        @Override
        public String toString() {
            return String.format(
                    "ScanResult{fixed=%d, containers=%d, chunks=%d, duration=%dms}",
                    itemsFixed, containersScanned, chunksScanned, scanDuration
            );
        }
    }

    /**
     * Global statistics for all scans
     */
    public static class ScanStatistics {
        public final int totalItemsScanned;
        public final int totalItemsFixed;
        public final int totalContainersScanned;
        public final int totalNestedContainersScanned;

        public ScanStatistics(int scanned, int fixed, int containers, int nested) {
            this.totalItemsScanned = scanned;
            this.totalItemsFixed = fixed;
            this.totalContainersScanned = containers;
            this.totalNestedContainersScanned = nested;
        }

        /**
         * Calculate fix rate percentage
         *
         * @return Percentage of items that were fixed (0-100)
         */
        public double getFixRate() {
            if (totalItemsScanned == 0) {
                return 0.0;
            }
            return (totalItemsFixed * 100.0) / totalItemsScanned;
        }

        @Override
        public String toString() {
            return String.format(
                    "Items Scanned: %,d | Items Fixed: %,d (%.2f%%) | Containers: %,d | Nested: %,d",
                    totalItemsScanned,
                    totalItemsFixed,
                    getFixRate(),
                    totalContainersScanned,
                    totalNestedContainersScanned
            );
        }


        public String toFormattedString() {
            return String.format(
                    "Total Items Scanned: %,d\n" +
                            "Total Items Fixed: %,d\n" +
                            "Fix Rate: %.2f%%\n" +
                            "Containers Scanned: %,d\n" +
                            "Nested Containers: %,d",
                    totalItemsScanned,
                    totalItemsFixed,
                    getFixRate(),
                    totalContainersScanned,
                    totalNestedContainersScanned
            );
        }
    }
}
