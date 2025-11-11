package org.secverse.SecVerseDupeUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.secverse.SecVerseDupeUtils.Crafter.CrafterDupe;
import org.secverse.SecVerseDupeUtils.Death.DeathDupe;
import org.secverse.SecVerseDupeUtils.Interface.Interface;
import org.secverse.SecVerseDupeUtils.ItemFrame.ItemFrameDupe;
import org.secverse.SecVerseDupeUtils.Donkey.DonkeyShulkerDupe;
import org.secverse.SecVerseDupeUtils.GrindStone.GrindStoneDupe;
import org.secverse.SecVerseDupeUtils.SecVersCom.Telemetry;
import org.secverse.SecVerseDupeUtils.SecVersCom.UpdateChecker;

import java.util.HashMap;
import java.util.Map;

public final class SecVersDupe extends JavaPlugin implements Listener {
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private GrindStoneDupe grindstoneDupe;
    private CrafterDupe crafterDupe;
    private DeathDupe deathDupe;
    private Interface dupeInterface;
    private UpdateChecker updateChecker;
    private Telemetry telemetry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialisiere Dupe-Module
        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        crafterDupe = new CrafterDupe(this);
        deathDupe = new DeathDupe(this);

        // Initialisiere Interface
        // Initialisiere Interface
        dupeInterface = new Interface(this, frameDupe, donkeyDupe, grindstoneDupe, crafterDupe, deathDupe);

        // Registriere Events
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);
        getServer().getPluginManager().registerEvents(crafterDupe, this);
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

                if (!sender.hasPermission("dupeutils.configdupes")) {
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
    public void onDisable() {
        if(telemetry != null) {
            Map<String, Object> add = new HashMap<>();
            add.put("event", "plugin_disable");
            telemetry.sendTelemetryAsync(add);
        }
    }
}
