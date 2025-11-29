package org.secvers.DupeUtility.Config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigVersionChecker {
    private final JavaPlugin plugin;
    private final File configFile;

    public ConfigVersionChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public boolean isConfigValid() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig(); // Create default config if missing
            return true; // Assume new config is valid
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        double configVersion = config.getDouble("version", 0.0);

        if (configVersion != 3.0) {
            plugin.getLogger().warning("Invalid config version (" + configVersion + "). Deleting...");
            return false; // Config is invalid
        }

        return true; // Config is valid
    }

    public void deleteInvalidConfig() {
        if (!isConfigValid()) {
            if (configFile.delete()) {
                plugin.getLogger().info("Deleted invalid config. A new one will be generated.");
                plugin.saveDefaultConfig(); // Regenerate default config
            } else {
                plugin.getLogger().severe("Failed to delete invalid config!");
            }
        }
    }

}
