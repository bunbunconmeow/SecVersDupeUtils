package org.secvers.DupeUtility.EconomyFix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Main manager for EconomyFix system.
 * Scans and fixes illegal items in player inventories and storage.
 */
public class EconomyFixManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private BukkitTask scanTask;
    private ItemScanner scanner;
    private OfflinePlayerScanner offlineScanner;

    private boolean playerScanEnabled;
    private boolean enderChestScanEnabled;
    private boolean containerScanEnabled;
    private boolean offlinePlayerScanEnabled;
    private boolean offlineEChestScanEnabled;
    private boolean ChunkLoadingEnabled;
    private int scanIntervalTicks;

    public EconomyFixManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadConfiguration();
        initializeComponents();
    }

    /**
     * Load configuration from config.yml
     */
    private void loadConfiguration() {
        this.playerScanEnabled = config.getBoolean("EconomyFix.PlayerInventoryScan", true);
        this.enderChestScanEnabled = config.getBoolean("EconomyFix.EnderChestScan", true);
        this.containerScanEnabled = config.getBoolean("EconomyFix.WorldContainerScan", true);
        this.offlinePlayerScanEnabled = config.getBoolean("EconomyFix.OfflinePlayerScan", false);
        this.offlineEChestScanEnabled = config.getBoolean("EconomyFix.OfflineEnderChestScan", false);
        this.ChunkLoadingEnabled  = config.getBoolean("EconomyFix.ChunkLoadingEnabled", false);
        int intervalMinutes = config.getInt("EconomyFix.PlayerScanInterval", 30);
        this.scanIntervalTicks = intervalMinutes * 60 * 20; // Convert to ticks
    }

    /**
     * Initialize scanner components
     */
    private void initializeComponents() {
        scanner = new ItemScanner(plugin);

        offlineScanner = new OfflinePlayerScanner(plugin, scanner, this);

        plugin.getLogger().info("EconomyFix initialized successfully");
    }

    /**
     * Start automatic scanning
     */
    public void startScanning() {
        if (scanTask != null) {
            scanTask.cancel();
        }

        if (!config.getBoolean("EconomyFix.Enabled", true)) {
            plugin.getLogger().info("EconomyFix is disabled in config");
            return;
        }

        scanTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                if (playerScanEnabled) {
                    scanOnlinePlayers();
                }
                if (enderChestScanEnabled) {
                    scanEnderChests();
                }
                if (containerScanEnabled) {
                    scanWorldContainers();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error during scheduled scan: " + e.getMessage());
                e.printStackTrace();
            }
        }, 100L, scanIntervalTicks);

        plugin.getLogger().info("EconomyFix automatic scanning started");
    }

    /**
     * Scan online player inventories
     */
    public CompletableFuture<ItemScanner.ScanResult> scanOnlinePlayers() {
        if (!playerScanEnabled) {
            return CompletableFuture.completedFuture(new ItemScanner.ScanResult());
        }
        return scanner.scanOnlinePlayers();
    }

    /**
     * Scan ender chests
     */
    public CompletableFuture<ItemScanner.ScanResult> scanEnderChests() {
        if (!enderChestScanEnabled) {
            return CompletableFuture.completedFuture(new ItemScanner.ScanResult());
        }
        return scanner.scanEnderChests();
    }



    /**
     * Scan world containers
     */
    public CompletableFuture<ItemScanner.ScanResult> scanWorldContainers() {
        if (!containerScanEnabled) {
            return CompletableFuture.completedFuture(new ItemScanner.ScanResult());
        }
        return scanner.scanWorldContainers();
    }

    /**
     * Scan offline players
     */
    public CompletableFuture<OfflinePlayerScanner.ScanResult> scanOfflinePlayers() {
        if (!offlinePlayerScanEnabled) {
            return CompletableFuture.completedFuture(new OfflinePlayerScanner.ScanResult());
        }
        return offlineScanner.scanOfflinePlayers();
    }

    /**
     * Scan all locations
     */
    public CompletableFuture<List<ItemScanner.ScanResult>> scanAll() {
        return CompletableFuture.allOf(
                scanOnlinePlayers(),
                scanEnderChests(),
                scanWorldContainers()
        ).thenApply(v -> List.of(
                scanOnlinePlayers().join(),
                scanEnderChests().join(),
                scanWorldContainers().join()
        ));
    }

    /**
     * Get current statistics
     */
    public ItemScanner.ScanStatistics getStatistics() {
        return scanner.getStatistics();
    }

    /**
     * Stop all scanning
     */
    public void stopScanning() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }

        if (scanner != null) {
            scanner.shutdown();
        }

        if (offlineScanner != null) {
            offlineScanner.shutdown();
        }

        plugin.getLogger().info("EconomyFix scanning stopped");
    }

    /**
     * Reload configuration and restart scanning
     */
    public void reload() {
        stopScanning();
        plugin.reloadConfig();
        loadConfiguration();
        initializeComponents();
        startScanning();
        plugin.getLogger().info("EconomyFix configuration reloaded");
    }

    // Getters
    public boolean isOfflinePlayerScanEnabled() { return offlinePlayerScanEnabled; }
    public boolean isOfflineEChestScanEnabled() { return offlineEChestScanEnabled; }
    public boolean isChunkLoadingEnabled() { return ChunkLoadingEnabled; }
    public FileConfiguration getConfig() { return config; }
}
