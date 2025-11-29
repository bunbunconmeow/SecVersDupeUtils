package org.secvers.DupeUtility;

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
import org.secvers.DupeUtility.Interface.Interface;
import org.secvers.DupeUtility.Dupes.ItemFrame.ItemFrameDupe;
import org.secvers.DupeUtility.Dupes.Donkey.DonkeyShulkerDupe;
import org.secvers.DupeUtility.Dupes.Dropper.DropperDupe;
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
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private GrindStoneDupe grindstoneDupe;
    private CrafterDupe crafterDupe;
    private DropperDupe dropperDupe;
    private DeathDupe deathDupe;
    private Interface dupeInterface;
    private UpdateChecker updateChecker;
    private Telemetry telemetry;
    private TranslationWorker translationWorker;

    @Override
    public void onEnable() {
        // Check of config Version is the new one or delete
        ConfigVersionChecker versionChecker = new ConfigVersionChecker(this);
        versionChecker.deleteInvalidConfig();
        saveDefaultConfig();

        // Initialisiere Dupe-Module
        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        crafterDupe = new CrafterDupe(this);
        dropperDupe = new DropperDupe(this);
        deathDupe = new DeathDupe(this);
        translationWorker = new TranslationWorker(this);

        // Initialisiere Interface
        dupeInterface = new Interface(this, frameDupe, donkeyDupe, grindstoneDupe, crafterDupe, dropperDupe, deathDupe, translationWorker);

        // Registriere Events
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);
        getServer().getPluginManager().registerEvents(crafterDupe, this);
        getServer().getPluginManager().registerEvents(dropperDupe, this);
        getServer().getPluginManager().registerEvents(deathDupe, this);
        getServer().getPluginManager().registerEvents(dupeInterface, this);

        // Update Checker
        updateChecker = new UpdateChecker(this);
        boolean checkUpdate = getConfig().getBoolean("checkUpdate", true);
        if(checkUpdate) {
            updateChecker.checkNowAsync();
        }

        // Telemetry
        telemetry = new Telemetry(this);
        Map<String, Object> add = new HashMap<>();
        add.put("event", "plugin_enable");
        telemetry.sendTelemetryAsync(add);

        int interval = getConfig().getInt("telemetry.send_interval_seconds", 3600);
        if (getConfig().getBoolean("telemetry.enabled", true)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<String, Object> periodic = new HashMap<>();
                    periodic.put("event", "periodic_ping");
                    telemetry.sendTelemetryAsync(periodic);
                }
            }.runTaskTimerAsynchronously(this, interval * 20L, interval * 20L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dupe")) {

            // Base-Permission Check
            if (!sender.hasPermission("dupeutils.command")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            // No Args -> Help
            if (args.length == 0) {
                sender.sendMessage("§6=== SecVers Dupe Utils ===");
                sender.sendMessage("§e/dupe reload §7- Reload the config");
                sender.sendMessage("§e/dupe config §7- Open config GUI");
                return true;
            }

            // Subcommand: reload
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("dupeutils.reload")) {
                    sender.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }

                this.reloadConfig();
                frameDupe.reload();
                donkeyDupe.reload();
                grindstoneDupe.reload();
                crafterDupe.reload();
                dropperDupe.reload();
                deathDupe.reload();

                sender.sendMessage("§aSecVers Dupe Utils config reloaded.");
                return true;
            }

            // Subcommand: config
            else if (args[0].equalsIgnoreCase("config")) {
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

            // unknown Subcommand
            else {
                sender.sendMessage("§cUnknown subcommand. Use §e/dupe§c for help.");
                return true;
            }
        }

        return false;
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
        return null;
    }

    @Override
    public void onDisable() {
        if(telemetry != null) {
            Map<String, Object> add = new HashMap<>();
            add.put("event", "plugin_disable");
            try {
                telemetry.sendTelemetrySync(add);
            } catch (IllegalPluginAccessException e) {
                getLogger().warning("Failed to send disable telemetry: " + e.getMessage());
            }
        }
    }
}
