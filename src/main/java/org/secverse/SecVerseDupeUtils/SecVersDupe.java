package org.secverse.SecVerseDupeUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.secverse.SecVerseDupeUtils.Crafter.CrafterDupe;
import org.secverse.SecVerseDupeUtils.Interface.Interface;
import org.secverse.SecVerseDupeUtils.ItemFrame.ItemFrameDupe;
import org.secverse.SecVerseDupeUtils.Donkey.DonkeyShulkerDupe;
import org.secverse.SecVerseDupeUtils.GrindStone.GrindStoneDupe;
import org.secverse.SecVerseDupeUtils.SecVersCom.Telemetry;
import org.secverse.SecVerseDupeUtils.SecVersCom.UpdateChecker;
import org.secverse.SecVerseDupeUtils.Death.DeathDupe;

import java.util.HashMap;
import java.util.Map;

public final class SecVersDupe extends JavaPlugin implements Listener {
    private ItemFrameDupe frameDupe;
    private DonkeyShulkerDupe donkeyDupe;
    private GrindStoneDupe grindstoneDupe;
    private DeathDupe deathDupe;
    private UpdateChecker updateChecker;
    private Telemetry telemetry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialisiere Dupe-Module
        frameDupe = new ItemFrameDupe(this);
        donkeyDupe = new DonkeyShulkerDupe(this);
        grindstoneDupe = new GrindStoneDupe(this);
        deathDupe = new DeathDupe(this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameAll(), this);
        getServer().getPluginManager().registerEvents(frameDupe.new FrameSpecific(), this);
        getServer().getPluginManager().registerEvents(donkeyDupe, this);
        getServer().getPluginManager().registerEvents(grindstoneDupe, this);
        getServer().getPluginManager().registerEvents(deathDupe, this);
        getServer().getPluginManager().registerEvents(this, this);

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
        if (command.getName().equalsIgnoreCase("duperealod")) {
            if (!sender.hasPermission("dupeutils.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            this.reloadConfig();
            frameDupe.reload();
            donkeyDupe.reload();
            grindstoneDupe.reload();
            deathDupe.reload();

            sender.sendMessage("§aSecVers Dupe Utils config reloaded.");
            return true;
        } else if (command.getName().equalsIgnoreCase("configdupes")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            if (!sender.hasPermission("dupeutils.configdupes")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

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

                        boolean frameEnabled = !getConfig().getBoolean("FrameDupe.Enabled");
                        getConfig().set("FrameDupe.Enabled", frameEnabled);
                        saveConfig();
                        player.sendMessage((frameEnabled ? "§a" : "§c") + "Item Frame Dupe " + (frameEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        frameDupe.reload();
                    } else if (isRightClick) {

                        int currentProb = getConfig().getInt("FrameDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110;
                        getConfig().set("FrameDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aItem Frame Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        frameDupe.reload();
                    }
                    break;
                case GLOW_ITEM_FRAME:
                    if (isLeftClick) {

                        boolean glowFrameEnabled = !getConfig().getBoolean("GLOW_FrameDupe.Enabled");
                        getConfig().set("GLOW_FrameDupe.Enabled", glowFrameEnabled);
                        saveConfig();
                        player.sendMessage((glowFrameEnabled ? "§a" : "§c") + "Glowing Item Frame Dupe " + (glowFrameEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        frameDupe.reload();
                    } else if (isRightClick) {

                        int currentProb = getConfig().getInt("GLOW_FrameDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110;
                        getConfig().set("GLOW_FrameDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aGlowing Item Frame Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        frameDupe.reload();
                    }
                    break;
                case DONKEY_SPAWN_EGG:
                    if (isLeftClick) {

                        boolean donkeyEnabled = !getConfig().getBoolean("OtherDupes.DonkeyDupe.Enabled");
                        getConfig().set("OtherDupes.DonkeyDupe.Enabled", donkeyEnabled);
                        saveConfig();
                        player.sendMessage((donkeyEnabled ? "§a" : "§c") + "Donkey Dupe " + (donkeyEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        donkeyDupe.reload();
                    } else if (isRightClick) {

                        int currentProb = getConfig().getInt("OtherDupes.DonkeyDupe.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110;
                        getConfig().set("OtherDupes.DonkeyDupe.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aDonkey Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        donkeyDupe.reload();
                    }
                    break;
                case GRINDSTONE:
                    if (isLeftClick) {

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

                        int currentProb = getConfig().getInt("OtherDupes.GrindStone.Probability-percentage", 100);
                        int newProb = (currentProb + 10) % 110;
                        getConfig().set("OtherDupes.GrindStone.Probability-percentage", newProb);
                        saveConfig();
                        player.sendMessage("§aGrindstone Dupe probability set to " + newProb + "%");
                        this.reloadConfig();
                        grindstoneDupe.reload();
                    }
                    break;
                case BONE:
                    if (isLeftClick) {
                        boolean deathEnabled = !getConfig().getBoolean("OtherDupes.DeathDupe.Enabled");
                        getConfig().set("OtherDupes.DeathDupe.Enabled", deathEnabled);
                        saveConfig();
                        player.sendMessage((deathEnabled ? "§a" : "§c") + "Death Dupe " + (deathEnabled ? "enabled" : "disabled"));
                        this.reloadConfig();
                        deathDupe.reload();
                    }
                    break;
            }

            openConfigDupesGUI(player);
        }
    }

    private void openConfigDupesGUI(Player player) {

        Inventory gui = Bukkit.createInventory(null, 27, "Configure Dupes");

        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (i != 10 && i != 12 && i != 14 && i != 16 && i != 18) {
                gui.setItem(i, glassPane);
            }
        }

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

        ItemStack deathDupeItem = new ItemStack(Material.BONE);
        ItemMeta deathMeta = deathDupeItem.getItemMeta();
        boolean deathEnabled = getConfig().getBoolean("OtherDupes.DeathDupe.Enabled");
        deathMeta.setDisplayName((deathEnabled ? "§a" : "§c") + "Death Dupe: " + (deathEnabled ? "Enabled" : "Disabled"));
        deathMeta.setLore(java.util.Arrays.asList(
            "Left-click to toggle"
        ));
        deathDupeItem.setItemMeta(deathMeta);

        gui.setItem(10, frameDupeItem);
        gui.setItem(12, glowFrameDupeItem);
        gui.setItem(14, donkeyDupeItem);
        gui.setItem(16, grindstoneDupeItem);
        gui.setItem(18, deathDupeItem);

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
