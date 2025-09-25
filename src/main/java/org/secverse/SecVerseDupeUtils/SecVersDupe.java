package org.secverse.SecVerseDupeUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.secverse.SecVerseDupeUtils.ItemFrame.ItemFrameDupe;
import org.secverse.SecVerseDupeUtils.Donkey.DonkeyShulkerDupe;
import org.secverse.SecVerseDupeUtils.GrindStone.GrindStoneDupe;
import org.secverse.SecVerseDupeUtils.SecVersCom.Telemetry;
import org.secverse.SecVerseDupeUtils.SecVersCom.UpdateChecker;

import java.util.HashMap;
import java.util.Map;

public final class SecVersDupe extends JavaPlugin {
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private  GrindStoneDupe grindstoneDupe;
    private UpdateChecker updateChecker;
    private Telemetry telemetry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);

        UpdateChecker checker = new UpdateChecker(this);
        boolean checkUpdate = getConfig().getBoolean("checkUpdate", true);
        if(checkUpdate) {
            checker.checkNowAsync();
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
            }.runTaskTimerAsynchronously(this, interval * 20L, interval * 20L); // seconds -> ticks
        }


    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dupeutls reload")) {
            if (!sender.hasPermission("dupeutils.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            this.reloadConfig();
            frameDupe.reload();
            donkeyDupe.reload();
            grindstoneDupe.reload();

            sender.sendMessage("§SecVers Dupe Utils config reloaded.");
            return true;
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
