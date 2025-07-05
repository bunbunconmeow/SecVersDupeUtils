package org.secverse.SecVerseDupeUtils;

import org.bukkit.Bukkit;
import org.secverse.SecVerseDupeUtils.cummon.ChestBoatDupeManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.secverse.SecVerseDupeUtils.cummon.Events;

public final class FDupe extends JavaPlugin {
    private int mcVersion;

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(new Events(this).new FrameAll(), this);
        getServer().getPluginManager().registerEvents(new ChestBoatDupeManager(this), this);
        if (mcVersion >= 17) {
            getServer().getPluginManager().registerEvents(new Events(this).new FrameSpecific(), this);
        }

        saveDefaultConfig();
    }




    @Override
    public void onDisable() {

    }
}
