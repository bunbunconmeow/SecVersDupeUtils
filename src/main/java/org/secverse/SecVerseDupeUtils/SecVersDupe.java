package org.secverse.SecVerseDupeUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
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
        getServer().getPluginManager().registerEvents(this, this);

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
        } else if (command.getName().equalsIgnoreCase("configdupes")) {
            if (!sender.hasPermission("dupeutils.configdupes")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            // Open the GUI for toggling dupes allowed
            openConfigDupesGUI((Player) sender);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Configure Dupes")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null) return;

            boolean isLeftClick = event.getClick().isLeftClick();
            boolean isRightClick = event.getClick().isRightClick();

            switch (clickedItem.getType()) {
                case ITEM_FRAME:
                    if (isLeftClick) {
                        // Toggle Item Frame Dupe
                        boolean frameEnabled = !getConfig().getBoolean("FrameDupe.Enabled");
                        getConfig().set("FrameDupe.Enabled", frameEnabled);
                        saveConfig();
                        player.sendMessage((frameEnabled ? "§a" : "§c") + "Item Frame Dupe " + (frameEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        frameDupe.reload();
                    } else if (isRightClick) {
                        // Cycle probability
                        int currentProb = getConfig().getInt("FrameDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110; // Cycle 0, 10, 20, ..., 100
                        getConfig().set("FrameDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aItem Frame Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        frameDupe.reload();
                    }
                    break;
                case GLOW_ITEM_FRAME:
                    if (isLeftClick) {
                        // Toggle Glowing Item Frame Dupe
                        boolean glowFrameEnabled = !getConfig().getBoolean("GLOW_FrameDupe.Enabled");
                        getConfig().set("GLOW_FrameDupe.Enabled", glowFrameEnabled);
                        saveConfig();
                        player.sendMessage((glowFrameEnabled ? "§a" : "§c") + "Glowing Item Frame Dupe " + (glowFrameEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        frameDupe.reload();
                    } else if (isRightClick) {
                        // Cycle probability
                        int currentProb = getConfig().getInt("GLOW_FrameDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110; // Cycle 0, 10, 20, ..., 100
                        getConfig().set("GLOW_FrameDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aGlowing Item Frame Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        frameDupe.reload();
                    }
                    break;
                case DONKEY_SPAWN_EGG:
                    if (isLeftClick) {
                        // Toggle Donkey Dupe
                        boolean donkeyEnabled = !getConfig().getBoolean("OtherDupes.DonkeyDupe.Enabled");
                        getConfig().set("OtherDupes.DonkeyDupe.Enabled", donkeyEnabled);
                        saveConfig();
                        player.sendMessage((donkeyEnabled ? "§a" : "§c") + "Donkey Dupe " + (donkeyEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        donkeyDupe.reload();
                    } else if (isRightClick) {
                        // Cycle probability
                        int currentProb = getConfig().getInt("OtherDupes.DonkeyDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110; // Cycle 0, 10, 20, ..., 100
                        getConfig().set("OtherDupes.DonkeyDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aDonkey Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        donkeyDupe.reload();
                    }
                    break;
                case GRINDSTONE:
                    if (isLeftClick) {
                        // Toggle Grindstone Dupe
                        boolean grindstoneEnabled = !getConfig().getBoolean("OtherDupes.GrindStone");
                        getConfig().set("OtherDupes.GrindStone", grindstoneEnabled);
                        saveConfig();
                        player.sendMessage((grindstoneEnabled ? "§a" : "§c") + "Grindstone Dupe " + (grindstoneEnabled ? "enabled" : "disabled"));
                        if (grindstoneEnabled) {
                            player.sendMessage("§e[Warning] Grindstone Dupe is still experimental and doesn't currently work.");
                        }
                        this.reloadConfig();
                        grindstoneDupe.reload();
                    } else if (isRightClick) {
                        // Cycle probability
                        int currentProb = getConfig().getInt("OtherDupes.GrindStone.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110; // Cycle 0, 10, 20, ..., 100
                        getConfig().set("OtherDupes.GrindStone.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aGrindstone Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        grindstoneDupe.reload();
                    }
                    break;
            }

            // Update the GUI to reflect the new state
            openConfigDupesGUI(player);
        }
    }

    private void openConfigDupesGUI(Player player) {
        // Create a new inventory for the GUI
        Inventory gui = Bukkit.createInventory(null, 27, "Configure Dupes");

        // Create background glass panes
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);

        // Fill background with glass panes
        for (int i = 0; i < 27; i++) {
            if (i != 10 && i != 12 && i != 14 && i != 16) {
                gui.setItem(i, glassPane);
            }
        }

        // Create items for the GUI
        ItemStack frameDupeItem = new ItemStack(Material.ITEM_FRAME);
        ItemMeta frameMeta = frameDupeItem.getItemMeta();
        boolean frameEnabled = getConfig().getBoolean("FrameDupe.Enabled");
        int frameProbability = getConfig().getInt("FrameDupe.Probability-percentage", 100);
        frameMeta.setDisplayName((frameEnabled ? "§a" : "§c") + "Item Frame Dupe: " + (frameEnabled ? "Enabled" : "Disabled"));
        frameMeta.setLore(java.util.Arrays.asList(
            "Probability: " + frameProbability + "%",
            "Left-click to toggle, Right-click to change probability"
        ));
        frameDupeItem.setItemMeta(frameMeta);

        ItemStack glowFrameDupeItem = new ItemStack(Material.GLOW_ITEM_FRAME);
        ItemMeta glowFrameMeta = glowFrameDupeItem.getItemMeta();
        boolean glowFrameEnabled = getConfig().getBoolean("GLOW_FrameDupe.Enabled");
        int glowFrameProbability = getConfig().getInt("GLOW_FrameDupe.Probability-percentage", 100);
        glowFrameMeta.setDisplayName((glowFrameEnabled ? "§a" : "§c") + "Glowing Item Frame Dupe: " + (glowFrameEnabled ? "Enabled" : "Disabled"));
        glowFrameMeta.setLore(java.util.Arrays.asList(
            "Probability: " + glowFrameProbability + "%",
            "Left-click to toggle, Right-click to change probability"
        ));
        glowFrameDupeItem.setItemMeta(glowFrameMeta);

        ItemStack donkeyDupeItem = new ItemStack(Material.DONKEY_SPAWN_EGG);
        ItemMeta donkeyMeta = donkeyDupeItem.getItemMeta();
        boolean donkeyEnabled = getConfig().getBoolean("OtherDupes.DonkeyDupe.Enabled");
        int donkeyProbability = getConfig().getInt("OtherDupes.DonkeyDupe.Probability-percentage", 100);
        donkeyMeta.setDisplayName((donkeyEnabled ? "§a" : "§c") + "Donkey Dupe: " + (donkeyEnabled ? "Enabled" : "Disabled"));
        donkeyMeta.setLore(java.util.Arrays.asList(
            "Probability: " + donkeyProbability + "%",
            "Left-click to toggle, Right-click to change probability"
        ));
        donkeyDupeItem.setItemMeta(donkeyMeta);

        ItemStack grindstoneDupeItem = new ItemStack(Material.GRINDSTONE);
        ItemMeta grindstoneMeta = grindstoneDupeItem.getItemMeta();
        boolean grindstoneEnabled = getConfig().getBoolean("OtherDupes.GrindStone");
        int grindstoneProbability = getConfig().getInt("OtherDupes.GrindStone.Probability-percentage", 100);
        grindstoneMeta.setDisplayName((grindstoneEnabled ? "§a" : "§c") + "Grindstone Dupe: " + (grindstoneEnabled ? "Enabled" : "Disabled"));
        grindstoneMeta.setLore(java.util.Arrays.asList(
            "Probability: " + grindstoneProbability + "%",
            "Left-click to toggle, Right-click to change probability"
        ));
        grindstoneDupeItem.setItemMeta(grindstoneMeta);

        // Add items to the inventory
        gui.setItem(10, frameDupeItem);
        gui.setItem(12, glowFrameDupeItem);
        gui.setItem(14, donkeyDupeItem);
        gui.setItem(16, grindstoneDupeItem);

        // Open the inventory for the player
        player.openInventory(gui);
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
