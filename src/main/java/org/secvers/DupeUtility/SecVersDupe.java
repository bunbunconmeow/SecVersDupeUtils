package org.secvers.DupeUtility;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.secvers.DupeUtility.Config.ConfigVersionChecker;
import org.secvers.DupeUtility.Dupes.Crafter.CrafterDupe;
import org.secvers.DupeUtility.Dupes.Death.DeathDupe;
import org.secvers.DupeUtility.Dupes.GrindStone.GrindStoneDupe;
import org.secvers.DupeUtility.Helper.LoggerColor;
import org.secvers.DupeUtility.Interface.Interface;
import org.secvers.DupeUtility.Dupes.ItemFrame.ItemFrameDupe;
import org.secvers.DupeUtility.Dupes.Donkey.DonkeyShulkerDupe;
import org.secvers.DupeUtility.Dupes.Dropper.DropperDupe;
import org.secvers.DupeUtility.EconomyFix.EconomyFixManager;
import org.secvers.DupeUtility.EconomyFix.ItemScanner;
import org.secvers.DupeUtility.EconomyFix.OfflinePlayerScanner;
import org.secvers.DupeUtility.SecVersCom.Telemetry;
import org.secvers.DupeUtility.SecVersCom.UpdateChecker;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.secvers.DupeUtility.Translation.TranslationWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SecVersDupe extends JavaPlugin implements Listener {
    // Dupe Prevention Modules
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private GrindStoneDupe grindstoneDupe;
    private CrafterDupe crafterDupe;
    private DropperDupe dropperDupe;
    private DeathDupe deathDupe;

    // Interface & Utilities
    private Interface dupeInterface;
    private UpdateChecker updateChecker;
    private Telemetry telemetry;
    private TranslationWorker translationWorker;

    // EconomyFix Module
    private EconomyFixManager economyFixManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        getLogger().info(LoggerColor.CYAN + "╔═══════════════════════════════════════╗" + LoggerColor.RESET);
        getLogger().info(LoggerColor.CYAN + "║    " + LoggerColor.BRIGHT_YELLOW + "SecVers DupeUtility Starting..." + LoggerColor.CYAN + "    ║" + LoggerColor.RESET);
        getLogger().info(LoggerColor.CYAN + "╚═══════════════════════════════════════╝" + LoggerColor.RESET);

        // Check config version
        ConfigVersionChecker versionChecker = new ConfigVersionChecker(this);
        versionChecker.deleteInvalidConfig();
        saveDefaultConfig();

        // Initialize Dupe Prevention Modules
        getLogger().info(LoggerColor.YELLOW + "→ Initializing dupe modules..." + LoggerColor.RESET);
        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        crafterDupe = new CrafterDupe(this);
        dropperDupe = new DropperDupe(this);
        deathDupe = new DeathDupe(this);
        translationWorker = new TranslationWorker(this);
        getLogger().info(LoggerColor.GREEN + "✓ Dupe modules loaded" + LoggerColor.RESET);

        // Initialize Interface
        dupeInterface = new Interface(this, frameDupe, donkeyDupe, grindstoneDupe,
                crafterDupe, dropperDupe, deathDupe, translationWorker);

        // Register Events
        getLogger().info(LoggerColor.YELLOW + "→ Registering event listeners..." + LoggerColor.RESET);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);
        getServer().getPluginManager().registerEvents(crafterDupe, this);
        getServer().getPluginManager().registerEvents(dropperDupe, this);
        getServer().getPluginManager().registerEvents(deathDupe, this);
        getServer().getPluginManager().registerEvents(dupeInterface, this);
        getLogger().info(LoggerColor.GREEN + "✓ Event listeners registered" + LoggerColor.RESET);

        // Initialize EconomyFix
        if (getConfig().getBoolean("EconomyFix.Enabled", true)) {
            getLogger().info(LoggerColor.YELLOW + "→ Initializing EconomyFix module..." + LoggerColor.RESET);
            try {
                economyFixManager = new EconomyFixManager(this);
                economyFixManager.startScanning();
                getLogger().info(LoggerColor.GREEN + "✓ EconomyFix loaded and scanning started" + LoggerColor.RESET);
            } catch (Exception e) {
                getLogger().severe(LoggerColor.RED + "✗ Failed to initialize EconomyFix: " + e.getMessage() + LoggerColor.RESET);
                e.printStackTrace();
            }
        } else {
            getLogger().info(LoggerColor.YELLOW + "⊗ EconomyFix disabled in config" + LoggerColor.RESET);
        }

        // Update Checker
        updateChecker = new UpdateChecker(this);
        if (getConfig().getBoolean("checkUpdate", true)) {
            getLogger().info(LoggerColor.YELLOW + "→ Checking for updates..." + LoggerColor.RESET);
            updateChecker.checkNowAsync();
        }

        // Telemetry
        telemetry = new Telemetry(this);
        if (getConfig().getBoolean("telemetry.enabled", true)) {
            Map<String, Object> enableData = new HashMap<>();
            enableData.put("event", "plugin_enable");
            enableData.put("version", getDescription().getVersion());
            enableData.put("economy_fix_enabled", economyFixManager != null);
            telemetry.sendTelemetryAsync(enableData);

            int interval = getConfig().getInt("telemetry.send_interval_seconds", 9000);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<String, Object> periodicData = new HashMap<>();
                    periodicData.put("event", "periodic_ping");

                    if (economyFixManager != null) {
                        ItemScanner.ScanStatistics stats = economyFixManager.getStatistics();
                        periodicData.put("items_fixed", stats.totalItemsFixed);
                        periodicData.put("containers_scanned", stats.totalContainersScanned);
                    }

                    telemetry.sendTelemetryAsync(periodicData);
                }
            }.runTaskTimerAsynchronously(this, interval * 20L, interval * 20L);
        }

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info(LoggerColor.BRIGHT_MAGENTA + "💬 We got a Discord Server! " + LoggerColor.BLUE + "https://discord.gg/Th95ea32EG" + LoggerColor.RESET);

        getLogger().info(LoggerColor.GREEN + "╔═══════════════════════════════════════╗" + LoggerColor.RESET);
        getLogger().info(LoggerColor.GREEN + "║ " + LoggerColor.BRIGHT_GREEN + "SecVers DupeUtility Enabled!" + LoggerColor.GREEN + " (" + LoggerColor.YELLOW + loadTime + "ms" + LoggerColor.GREEN + ") ║" + LoggerColor.RESET);
        getLogger().info(LoggerColor.GREEN + "╚═══════════════════════════════════════╝" + LoggerColor.RESET);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle /dupe command
        if (command.getName().equalsIgnoreCase("dupe")) {
            if (!sender.hasPermission("DupeUtility.command")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§6§l╔═══════════════════════════════════╗");
                sender.sendMessage("§6§l║  §e§lSecVers Dupe Utils Help    §6§l║");
                sender.sendMessage("§6§l╚═══════════════════════════════════╝");
                sender.sendMessage("§e/dupe reload §7- Reload configuration");
                sender.sendMessage("§e/dupe config §7- Open config GUI");
                sender.sendMessage("§e/economyfix §7- Illegal item scanner");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("DupeUtility.reload")) {
                    sender.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }

                reloadConfig();
                frameDupe.reload();
                donkeyDupe.reload();
                grindstoneDupe.reload();
                crafterDupe.reload();
                dropperDupe.reload();
                deathDupe.reload();

                if (economyFixManager != null) {
                    economyFixManager.reload();
                }

                sender.sendMessage("§a✓ SecVers Dupe Utils config reloaded.");
                return true;
            }

            if (args[0].equalsIgnoreCase("config")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }

                if (!sender.hasPermission("DupeUtility.configdupes")) {
                    sender.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }

                dupeInterface.openConfigDupesGUI((Player) sender);
                return true;
            }

            sender.sendMessage("§cUnknown subcommand. Use §e/dupe§c for help.");
            return true;
        }

        // Handle /economyfix command
        if (command.getName().equalsIgnoreCase("economyfix")) {
            if (!sender.hasPermission("economyfix.admin")) {
                sender.sendMessage("§cYou don't have permission to use EconomyFix commands!");
                return true;
            }

            if (economyFixManager == null) {
                sender.sendMessage("§cEconomyFix is not enabled! Enable it in config.yml");
                return true;
            }

            if (args.length == 0) {
                sendEconomyFixHelp(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "scan" -> {
                    return handleScanCommand(sender, args);
                }
                case "stats" -> {
                    return handleStatsCommand(sender);
                }
                case "toggle" -> {
                    return handleToggleCommand(sender, args);
                }
                default -> {
                    sender.sendMessage("§cUnknown subcommand! Use §e/economyfix§c for help.");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Send EconomyFix help message
     */
    private void sendEconomyFixHelp(CommandSender sender) {
        sender.sendMessage("§6§l╔════════════════════════════════════════╗");
        sender.sendMessage("§6§l║   §e§lEconomyFix - Illegal Item Scanner §6§l║");
        sender.sendMessage("§6§l╚════════════════════════════════════════╝");
        sender.sendMessage("§e/economyfix scan <type> §7- Start scan");
        sender.sendMessage("  §7Types: §fplayers, enderchests, world, offline, all");
        sender.sendMessage("§e/economyfix stats §7- View statistics");
        sender.sendMessage("§e/economyfix toggle <type> §7- Toggle scan");
        sender.sendMessage("§6§l════════════════════════════════════════");
    }

    /**
     * Handle scan command
     */
    private boolean handleScanCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /economyfix scan <players|enderchests|world|offline|all>");
            return true;
        }

        String scanType = args[1].toLowerCase();
        sender.sendMessage("§a§l⚡ Starting " + scanType + " scan...");

        switch (scanType) {
            case "players" -> {
                if (!sender.hasPermission("economyfix.scan.players")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                economyFixManager.scanOnlinePlayers().thenAccept(result ->
                        notifyScanComplete(sender, "Player Inventory", result)
                );
            }
            case "enderchests" -> {
                if (!sender.hasPermission("economyfix.scan.enderchests")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                economyFixManager.scanEnderChests().thenAccept(result ->
                        notifyScanComplete(sender, "Ender Chest", result)
                );
            }
            case "world" -> {
                if (!sender.hasPermission("economyfix.scan.world")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                economyFixManager.scanWorldContainers().thenAccept(result ->
                        notifyScanComplete(sender, "World Container", result)
                );
            }
            case "all" -> {
                if (!sender.hasPermission("economyfix.scan")) {
                    sender.sendMessage("§cNo permission!");
                    return true;
                }
                sender.sendMessage("§e⚠ Warning: Full scan may take several minutes!");
                economyFixManager.scanAll().thenAccept(results -> {
                    int totalFixed = results.stream().mapToInt(r -> r.itemsFixed).sum();
                    int totalContainers = results.stream().mapToInt(r -> r.containersScanned).sum();

                    Bukkit.getScheduler().runTask(this, () -> {
                        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        sender.sendMessage("§e§lComplete Scan Results:");
                        sender.sendMessage("§7Total Items Fixed: §a" + totalFixed);
                        sender.sendMessage("§7Total Containers: §a" + totalContainers);
                        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

                        if (sender instanceof Player player) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        }
                    });
                });
            }
            default -> {
                sender.sendMessage("§cInvalid scan type!");
                return true;
            }
        }

        return true;
    }

    /**
     * Handle stats command
     */
    private boolean handleStatsCommand(CommandSender sender) {
        if (!sender.hasPermission("economyfix.stats")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        ItemScanner.ScanStatistics stats = economyFixManager.getStatistics();

        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§e§lEconomyFix Statistics:");
        sender.sendMessage("§7Total Items Scanned: §a" + String.format("%,d", stats.totalItemsScanned));
        sender.sendMessage("§7Total Items Fixed: §c" + String.format("%,d", stats.totalItemsFixed));
        sender.sendMessage("§7Total Containers: §b" + String.format("%,d", stats.totalContainersScanned));
        sender.sendMessage("§7Nested Containers: §d" + String.format("%,d", stats.totalNestedContainersScanned));

        if (stats.totalItemsScanned > 0) {
            double fixPercentage = (stats.totalItemsFixed * 100.0) / stats.totalItemsScanned;
            sender.sendMessage("§7Fix Rate: §e" + String.format("%.2f%%", fixPercentage));
        }

        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return true;
    }

    /**
     * Handle toggle command
     */
    private boolean handleToggleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economyfix.toggle")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /economyfix toggle <players|enderchests|world|offline>");
            return true;
        }

        String setting = args[1].toLowerCase();
        String configPath = switch (setting) {
            case "players" -> "EconomyFix.PlayerInventoryScan";
            case "enderchests" -> "EconomyFix.EnderChestScan";
            case "world" -> "EconomyFix.WorldContainerScan";
            case "offline" -> "EconomyFix.OfflinePlayerScan";
            default -> null;
        };

        if (configPath == null) {
            sender.sendMessage("§cInvalid setting!");
            return true;
        }

        boolean currentValue = getConfig().getBoolean(configPath);
        getConfig().set(configPath, !currentValue);
        saveConfig();

        String status = !currentValue ? "§a§lENABLED" : "§c§lDISABLED";
        String emoji = !currentValue ? "✓" : "✗";

        sender.sendMessage(String.format(
                "§e%s §7scanning is now %s %s",
                setting.substring(0, 1).toUpperCase() + setting.substring(1),
                status,
                emoji
        ));

        return true;
    }

    /**
     * Notify scan complete
     */
    private void notifyScanComplete(CommandSender sender, String scanType, ItemScanner.ScanResult result) {
        Bukkit.getScheduler().runTask(this, () -> {
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§e§l" + scanType + " Scan Complete!");
            sender.sendMessage("§7Items Fixed: §a" + result.itemsFixed);
            sender.sendMessage("§7Containers Scanned: §b" + result.containersScanned);
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            if (result.itemsFixed > 0) {
                notifyAdmins(scanType, result);
            }

            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        });
    }

    /**
     * Notify offline scan complete
     */
    private void notifyOfflineScanComplete(CommandSender sender, OfflinePlayerScanner result) {
        Bukkit.getScheduler().runTask(this, () -> {
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage(result.toString());
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        });
    }

    /**
     * Notify all admins about scan results
     */
    private void notifyAdmins(String scanType, ItemScanner.ScanResult result) {
        String message = String.format(
                "§e[EconomyFix] §7%s scan found §c%d §7illegal items!",
                scanType,
                result.itemsFixed
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("economyfix.notify")) {
                player.sendMessage(message);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("dupe")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                if (sender.hasPermission("DupeUtility.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("DupeUtility.configdupes")) {
                    completions.add("config");
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (command.getName().equalsIgnoreCase("economyfix")) {
            if (args.length == 1) {
                return List.of("scan", "stats", "toggle").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("scan")) {
                return List.of("players", "enderchests", "world", "all").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                return List.of("players", "enderchests", "world").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return null;
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down SecVers DupeUtility...");

        // Stop EconomyFix
        if (economyFixManager != null) {
            economyFixManager.stopScanning();
            getLogger().info("✓ EconomyFix stopped");
        }

        // Send telemetry
        if (telemetry != null) {
            Map<String, Object> disableData = new HashMap<>();
            disableData.put("event", "plugin_disable");

            if (economyFixManager != null) {
                ItemScanner.ScanStatistics stats = economyFixManager.getStatistics();
                disableData.put("total_items_fixed", stats.totalItemsFixed);
                disableData.put("total_containers_scanned", stats.totalContainersScanned);
            }

            try {
                telemetry.sendTelemetrySync(disableData);
            } catch (IllegalPluginAccessException e) {
                getLogger().warning("Failed to send disable telemetry: " + e.getMessage());
            }
        }

        getLogger().info("SecVers DupeUtility disabled successfully!");
    }

    /**
     * Get EconomyFix manager
     */
    public EconomyFixManager getEconomyFixManager() {
        return economyFixManager;
    }
}
