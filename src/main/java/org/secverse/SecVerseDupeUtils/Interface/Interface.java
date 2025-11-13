package org.secverse.SecVerseDupeUtils.Interface;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.secverse.SecVerseDupeUtils.Dupes.Crafter.CrafterDupe;
import org.secverse.SecVerseDupeUtils.Dupes.Death.DeathDupe;
import org.secverse.SecVerseDupeUtils.Dupes.Donkey.DonkeyShulkerDupe;
import org.secverse.SecVerseDupeUtils.Dupes.Dropper.DropperDupe;
import org.secverse.SecVerseDupeUtils.Dupes.GrindStone.GrindStoneDupe;
import org.secverse.SecVerseDupeUtils.Dupes.ItemFrame.ItemFrameDupe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interface implements Listener {
    private final Plugin plugin;
    private final ItemFrameDupe frameDupe;
    private final DonkeyShulkerDupe donkeyDupe;
    private final GrindStoneDupe grindstoneDupe;
    private final CrafterDupe crafterDupe;
    private final DropperDupe dropperDupe;
    private final DeathDupe deathDupe;


    // Language strings - will be moved to language file later
    private final Map<String, String> lang = new HashMap<>();

    // GUI Type enum for navigation
    private enum GUIType {
        MAIN,
        ITEMFRAME_SETTINGS,
        GLOWFRAME_SETTINGS,
        DONKEY_SETTINGS,
        GRINDSTONE_SETTINGS,
        CRAFTER_SETTINGS,
        DROPPER_SETTINGS,
        DEATH_SETTINGS,
        BLACKLIST_SETTINGS
    }

    public Interface(Plugin plugin, ItemFrameDupe frameDupe, DonkeyShulkerDupe donkeyDupe,
                     GrindStoneDupe grindstoneDupe, CrafterDupe crafterDupe, DropperDupe dropperDupe, DeathDupe deathDupe) {
        this.plugin = plugin;
        this.frameDupe = frameDupe;
        this.donkeyDupe = donkeyDupe;
        this.grindstoneDupe = grindstoneDupe;
        this.crafterDupe = crafterDupe;
        this.dropperDupe = dropperDupe;
        this.deathDupe = deathDupe;

        initializeLanguage();
    }

    private void initializeLanguage() {
        // GUI
        lang.put("gui.title", "Configure Dupes");
        lang.put("gui.title.settings", "{0} Settings");
        lang.put("gui.enabled", "Enabled");
        lang.put("gui.disabled", "Disabled");
        lang.put("gui.probability", "Probability");
        lang.put("gui.multiplier", "Multiplier");
        lang.put("gui.min_timing", "Min Timing");
        lang.put("gui.max_timing", "Max Timing");
        lang.put("gui.drop_naturally", "Drop Naturally");
        lang.put("gui.add_to_inventory", "Add to Inventory");
        lang.put("gui.destroy_crafter", "Destroy Crafter");
        lang.put("gui.drop_originals", "Drop Originals");
        lang.put("gui.yes", "Yes");
        lang.put("gui.no", "No");
        lang.put("gui.left_click", "Left-click");
        lang.put("gui.right_click", "Right-click");
        lang.put("gui.shift_left_click", "Shift+Left-click");
        lang.put("gui.shift_right_click", "Shift+Right-click");
        lang.put("gui.to_toggle", "to toggle");
        lang.put("gui.to_change_probability", "to change probability");
        lang.put("gui.to_change_multiplier", "to change multiplier");
        lang.put("gui.to_cycle_mode", "to cycle mode");
        lang.put("gui.to_toggle_option", "to toggle option");
        lang.put("gui.to_increase", "to increase");
        lang.put("gui.to_decrease", "to decrease");
        lang.put("gui.to_open_settings", "to open settings");
        lang.put("gui.reload", "Reload Configuration");
        lang.put("gui.reload_desc", "Reloads all dupe configurations");
        lang.put("gui.back", "Back to Main Menu");
        lang.put("gui.increase_small", "Increase by +10");
        lang.put("gui.decrease_small", "Decrease by -10");
        lang.put("gui.increase_large", "Increase by +100");
        lang.put("gui.decrease_large", "Decrease by -100");
        lang.put("gui.click_to_adjust", "Click to adjust");

        // Dupe Names
        lang.put("dupe.itemframe.name", "Item Frame Dupe");
        lang.put("dupe.glowframe.name", "Glowing Item Frame Dupe");
        lang.put("dupe.donkey.name", "Donkey Dupe");
        lang.put("dupe.grindstone.name", "Grindstone Dupe");
        lang.put("dupe.crafter.name", "Crafter Dupe");
        lang.put("dupe.dropper.name", "Dropper Dupe");
        lang.put("dupe.death.name", "Death Dupe");

        // Messages
        lang.put("msg.enabled", "enabled");
        lang.put("msg.disabled", "disabled");
        lang.put("msg.probability_set", "probability set to");
        lang.put("msg.multiplier_set", "multiplier set to");
        lang.put("msg.mode_drop_only", "Mode: Drop Only");
        lang.put("msg.mode_inventory_only", "Mode: Inventory Only");
        lang.put("msg.mode_both", "Mode: Drop + Inventory");
        lang.put("msg.destroy_crafter", "Destroy Crafter");
        lang.put("msg.drop_originals", "Drop Originals");
        lang.put("msg.timing_set", "Timing set to");
        lang.put("msg.value_set", "Value set to");
        lang.put("msg.config_reloaded", "Configuration reloaded successfully!");
        lang.put("msg.min_cannot_exceed_max", "Min timing cannot exceed max timing!");
        lang.put("msg.max_cannot_below_min", "Max timing cannot be below min timing!");
        lang.put("msg.value_too_low", "Value cannot be set below 0!");

        // Colors
        lang.put("color.success", "§a");
        lang.put("color.error", "§c");
        lang.put("color.info", "§e");
        lang.put("color.value", "§e");
        lang.put("color.description", "§7");
        lang.put("color.reset", "§r");
    }

    public String getLang(String key) {
        return lang.getOrDefault(key, key);
    }

    public String getLang(String key, Object... args) {
        String message = lang.getOrDefault(key, key);
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return message;
    }

    public void openConfigDupesGUI(Player player) {
        openGUI(player, GUIType.MAIN);
    }

    private void openGUI(Player player, GUIType type) {
        switch (type) {
            case MAIN:
                openMainGUI(player);
                break;
            case ITEMFRAME_SETTINGS:
                openItemFrameSettings(player);
                break;
            case GLOWFRAME_SETTINGS:
                openGlowFrameSettings(player);
                break;
            case DONKEY_SETTINGS:
                openDonkeySettings(player);
                break;
            case GRINDSTONE_SETTINGS:
                openGrindstoneSettings(player);
                break;
            case CRAFTER_SETTINGS:
                openCrafterSettings(player);
                break;
            case DROPPER_SETTINGS:
                openDropperSettings(player);
                break;
            case DEATH_SETTINGS:
                openDeathSettings(player);
                break;
            case BLACKLIST_SETTINGS:
                openBlacklistSettings(player);
                break;
        }
    }

    private void openMainGUI(Player player) {
        openMainGUI(player, 1); // Start with page 1
    }

    private void openMainGUI(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 45, getLang("gui.title"));

        // Fill with glass panes
        fillWithGlass(gui);

        // Create a list of all dupe items
        ItemStack[] dupeItems = new ItemStack[]{
            createMainMenuItem(Material.ITEM_FRAME, "FrameDupe.Enabled", "dupe.itemframe.name"),
            createMainMenuItem(Material.GLOW_ITEM_FRAME, "GLOW_FrameDupe.Enabled", "dupe.glowframe.name"),
            createMainMenuItem(Material.DONKEY_SPAWN_EGG, "OtherDupes.DonkeyDupe.Enabled", "dupe.donkey.name"),
            createMainMenuItem(Material.GRINDSTONE, "OtherDupes.GrindStone.Enabled", "dupe.grindstone.name"),
            createMainMenuItem(Material.CRAFTER, "OtherDupes.CrafterDupe.Enabled", "dupe.crafter.name"),
            createMainMenuItem(Material.DROPPER, "OtherDupes.DropperDupe.Enabled", "dupe.dropper.name"),
            createMainMenuItem(Material.SKELETON_SPAWN_EGG, "OtherDupes.DeathDupe.Enabled", "dupe.death.name")
        };

        

        // Add other items in default positions
        int[] defaultSlots = {10, 12, 14, 16, 28, 30, 32};
        for (int i = 0; i < 7; i++) {
            
            gui.setItem(defaultSlots[i], dupeItems[i]);
            
        }

        // Reload Button (Slot 44)
        gui.setItem(44, createReloadButton());

        // Blacklist Button (Slot 36 - bottom left)
        gui.setItem(36, createBlacklistButton());

        player.openInventory(gui);
    }

    private ItemStack createMainMenuItem(Material material, String enabledPath, String nameKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean enabled = plugin.getConfig().getBoolean(enabledPath, false);

        String statusColor = enabled ? getLang("color.success") : getLang("color.error");
        String status = enabled ? getLang("gui.enabled") : getLang("gui.disabled");

        meta.setDisplayName(statusColor + getLang(nameKey) + ": " + status);
        meta.setLore(Arrays.asList(
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + getLang("gui.to_toggle"),
                getLang("color.info") + getLang("gui.right_click") + " " +
                        getLang("color.description") + getLang("gui.to_open_settings")
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReloadButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang("color.info") + getLang("gui.reload"));
        meta.setLore(Arrays.asList(
                getLang("color.description") + getLang("gui.reload_desc"),
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + "to reload"
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang("color.info") + getLang("gui.back"));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBlacklistButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang("color.info") + "Item Blacklist");
        meta.setLore(Arrays.asList(
                getLang("color.description") + "Manage blacklisted items",
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + "to open blacklist settings"
        ));

        item.setItemMeta(meta);
        return item;
    }


    // ==================== ITEM FRAME SETTINGS ====================
    private void openItemFrameSettings(Player player) {
        String basePath = "FrameDupe";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.itemframe.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.itemframe.name"));

        // Probability Settings (Slots 20-24)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -1, "Decrease by -1"));
        gui.setItem(13, createValueDisplay(Material.PAPER, "gui.probability",
                plugin.getConfig().getInt(basePath + ".Probability-percentage", 100) + "%"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +1, "Increase by +1"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +10, "gui.increase_small"));

        // Multiplier Settings (Slots 29-33)
        gui.setItem(20, createAdjustButton(Material.RED_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(21, createAdjustButton(Material.ORANGE_CONCRETE, -1, "Decrease by -1"));
        gui.setItem(22, createValueDisplay(Material.PAPER, "gui.multiplier",
                plugin.getConfig().getInt(basePath + ".Multiplier", 1) + "x"));
        gui.setItem(23, createAdjustButton(Material.LIME_CONCRETE, +1, "Increase by +1"));
        gui.setItem(24, createAdjustButton(Material.GREEN_CONCRETE, +10, "gui.increase_small"));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== GLOW FRAME SETTINGS ====================
    private void openGlowFrameSettings(Player player) {
        String basePath = "GLOW_FrameDupe";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.glowframe.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.glowframe.name"));

        // Probability Settings (Slots 20-24)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -1, "Decrease by -1"));
        gui.setItem(13, createValueDisplay(Material.PAPER, "gui.probability",
                plugin.getConfig().getInt(basePath + ".Probability-percentage", 100) + "%"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +1, "Increase by +1"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +10, "gui.increase_small"));

        // Multiplier Settings (Slots 29-33)
        gui.setItem(20, createAdjustButton(Material.RED_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(21, createAdjustButton(Material.ORANGE_CONCRETE, -1, "Decrease by -1"));
        gui.setItem(22, createValueDisplay(Material.PAPER, "gui.multiplier",
                plugin.getConfig().getInt(basePath + ".Multiplier", 1) + "x"));
        gui.setItem(23, createAdjustButton(Material.LIME_CONCRETE, +1, "Increase by +1"));
        gui.setItem(24, createAdjustButton(Material.GREEN_CONCRETE, +10, "gui.increase_small"));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== DONKEY SETTINGS ====================
    private void openDonkeySettings(Player player) {
        String basePath = "OtherDupes.DonkeyDupe";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.donkey.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.donkey.name"));

        // Min Timing Settings (Slots 19-23)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(13, createValueDisplay(Material.CLOCK, "gui.min_timing",
                plugin.getConfig().getLong(basePath + ".MinTiming", 100L) + "ms"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Max Timing Settings (Slots 28-32)
        gui.setItem(20, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(21, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(22, createValueDisplay(Material.CLOCK, "gui.max_timing",
                plugin.getConfig().getLong(basePath + ".MaxTiming", 5000L) + "ms"));
        gui.setItem(23, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(24, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== GRINDSTONE SETTINGS ====================
    private void openGrindstoneSettings(Player player) {
        String basePath = "OtherDupes.GrindStone";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.grindstone.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.grindstone.name"));

        // Min Timing Settings (Slots 10-14)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(13, createValueDisplay(Material.CLOCK, "gui.min_timing",
                plugin.getConfig().getLong(basePath + ".MinTiming", 1200L) + "ms"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Max Timing Settings (Slots 19-23)
        gui.setItem(20, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(21, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(22, createValueDisplay(Material.CLOCK, "gui.max_timing",
                plugin.getConfig().getLong(basePath + ".MaxTiming", 2200L) + "ms"));
        gui.setItem(23, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(24, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Drop Naturally Toggle (Slot 29)
        gui.setItem(29, createBooleanToggle(basePath + ".dropNaturally",
                "gui.drop_naturally", Material.DROPPER));

        // Add to Inventory Toggle (Slot 33)
        gui.setItem(33, createBooleanToggle(basePath + ".addToInventory",
                "gui.add_to_inventory", Material.CHEST));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== CRAFTER SETTINGS ====================
    private void openCrafterSettings(Player player) {
        String basePath = "OtherDupes.CrafterDupe";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.crafter.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.crafter.name"));

        // Min Timing Settings (Slots 11-15)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(13, createValueDisplay(Material.CLOCK, "gui.min_timing",
                plugin.getConfig().getLong(basePath + ".MinTiming", 100L) + "ms"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Max Timing Settings (Slots 20-24)
        gui.setItem(20, createAdjustButton(Material.RED_CONCRETE, -100, "gui.decrease_large"));
        gui.setItem(21, createAdjustButton(Material.ORANGE_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(22, createValueDisplay(Material.CLOCK, "gui.max_timing",
                plugin.getConfig().getLong(basePath + ".MaxTiming", 1000L) + "ms"));
        gui.setItem(23, createAdjustButton(Material.LIME_CONCRETE, +10, "gui.increase_small"));
        gui.setItem(24, createAdjustButton(Material.GREEN_CONCRETE, +100, "gui.increase_large"));

        // Destroy Crafter Toggle (Slot 29)
        gui.setItem(29, createBooleanToggle(basePath + ".destroyCrafter",
                "gui.destroy_crafter", Material.TNT));

        // Drop Originals Toggle (Slot 33)
        gui.setItem(33, createBooleanToggle(basePath + ".dropOriginals",
                "gui.drop_originals", Material.DROPPER));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== DROPPER SETTINGS ====================
    private void openDropperSettings(Player player) {
        String basePath = "OtherDupes.DropperDupe";
        Inventory gui = Bukkit.createInventory(null, 45,
                getLang("gui.title.settings", getLang("dupe.dropper.name")));

        fillWithGlass(gui);

        // Enable/Disable Toggle (Slot 4)
        gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.dropper.name"));

        // Multiplier Settings (Slots 11-15)
        gui.setItem(11, createAdjustButton(Material.RED_CONCRETE, -10, "gui.decrease_small"));
        gui.setItem(12, createAdjustButton(Material.ORANGE_CONCRETE, -1, "Decrease by -1"));
        gui.setItem(13, createValueDisplay(Material.PAPER, "gui.multiplier",
                plugin.getConfig().getInt(basePath + ".Multiplier", 2) + "x"));
        gui.setItem(14, createAdjustButton(Material.LIME_CONCRETE, +1, "Increase by +1"));
        gui.setItem(15, createAdjustButton(Material.GREEN_CONCRETE, +10, "gui.increase_small"));

        // Back Button (Slot 40)
        gui.setItem(40, createBackButton());

        player.openInventory(gui);
    }

    // ==================== HELPER METHODS ====================

    private void fillWithGlass(Inventory gui) {
        ItemStack glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.setDisplayName(" ");
        glassPane.setItemMeta(glassMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glassPane);
            }
        }
    }

    private ItemStack createToggleItem(String configPath, String nameKey) {
        boolean enabled = plugin.getConfig().getBoolean(configPath, false);

        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String statusColor = enabled ? getLang("color.success") : getLang("color.error");
        String status = enabled ? getLang("gui.enabled") : getLang("gui.disabled");

        meta.setDisplayName(statusColor + getLang(nameKey));
        meta.setLore(Arrays.asList(
                getLang("color.description") + "Status: " + statusColor + status,
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + getLang("gui.to_toggle")
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createValueDisplay(Material material, String labelKey, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang("color.info") + getLang(labelKey));
        meta.setLore(Arrays.asList(
                getLang("color.description") + "Current: " + getLang("color.value") + value,
                "",
                getLang("color.description") + "Use buttons to adjust"
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdjustButton(Material material, int adjustment, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String color = adjustment > 0 ? getLang("color.success") : getLang("color.error");
        String sign = adjustment > 0 ? "+" : "";

        meta.setDisplayName(color + sign + adjustment);
        meta.setLore(Arrays.asList(
                getLang("color.description") + (label.startsWith("gui.") ? getLang(label) : label),
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + getLang("gui.click_to_adjust")
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBooleanToggle(String configPath, String labelKey, Material material) {
        boolean value = plugin.getConfig().getBoolean(configPath, false);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String statusColor = value ? getLang("color.success") : getLang("color.error");
        String status = value ? getLang("gui.yes") : getLang("gui.no");

        meta.setDisplayName(getLang("color.info") + getLang(labelKey));
        meta.setLore(Arrays.asList(
                getLang("color.description") + "Status: " + statusColor + status,
                "",
                getLang("color.info") + getLang("gui.left_click") + " " +
                        getLang("color.description") + getLang("gui.to_toggle")
        ));

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        plugin.getLogger().info("[DEBUG] InventoryDragEvent triggered. Title: '" + title + "', Slots: " + event.getInventorySlots() + ", Raw slots: " + event.getRawSlots());

        if (!title.equals("Item Blacklist")) {
            plugin.getLogger().info("[DEBUG] Title does not match 'Item Blacklist', ignoring drag event.");
            return;
        }

        // Check if dragging into slot 22
        if (event.getInventorySlots().contains(22)) {
            plugin.getLogger().info("[DEBUG] Dragging into slot 22 detected.");
            event.setCancelled(false); // Ensure the drag is allowed
            // Allow the drag, then handle after
            Player player = (Player) event.getWhoClicked();
            Inventory gui = event.getInventory();

            // Schedule task to handle after the drag completes
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[DEBUG] Scheduled task running for drag handling.");
                ItemStack item = gui.getItem(22);
                plugin.getLogger().info("[DEBUG] Item in slot 22: " + (item != null ? item.getType().name() : "null"));
                if (item != null && item.getType() != Material.AIR) {
                    plugin.getLogger().info("[DEBUG] Adding item to blacklist: " + item.getType().name());
                    addItemToBlacklist(item);
                    gui.setItem(22, null); // Reset slot to empty
                    player.sendMessage(getLang("color.success") + item.getType().name() + " added to blacklist!");
                } else {
                    plugin.getLogger().info("[DEBUG] No valid item found in slot 22 after drag.");
                }
            });
        } else {
            plugin.getLogger().info("[DEBUG] Not dragging into slot 22, slots: " + event.getInventorySlots());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Check if it's one of our GUIs
        if (!title.equals(getLang("gui.title")) &&
                !title.equals("Item Blacklist") &&
                !title.contains(getLang("dupe.itemframe.name")) &&
                !title.contains(getLang("dupe.glowframe.name")) &&
                !title.contains(getLang("dupe.donkey.name")) &&
                !title.contains(getLang("dupe.grindstone.name")) &&
                !title.contains(getLang("dupe.crafter.name")) &&
                !title.contains(getLang("dupe.dropper.name")) &&
                !title.contains(getLang("dupe.death.name"))) {
            return;
        }

        // For blacklist GUI, handle specially
        if (title.equals("Item Blacklist")) {
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            ItemStack clickedItem = event.getCurrentItem();

            // Only cancel clicks in the GUI inventory, allow player inventory for picking up items
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (slot == 40 && clickedItem != null && clickedItem.getType() == Material.ARROW) {
                    // Back button
                    openMainGUI(player);
                    event.setCancelled(true);
                    return;
                }
                if (slot == 22) {
                    // Handle adding item to blacklist by clicking on slot 22
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        plugin.getLogger().info("[DEBUG] Adding item to blacklist via click: " + cursor.getType().name());
                        addItemToBlacklist(cursor.clone());
                        // Give the item back to the player
                        player.getInventory().addItem(cursor.clone());
                        event.getView().setCursor(null);
                        player.sendMessage(getLang("color.success") + cursor.getType().name() + " added to blacklist!");
                        event.setCancelled(true);
                        return;
                    }
                }
                // Cancel the event to prevent picking up items from GUI
                event.setCancelled(true);
            }
            return;
        }

        // Cancel for other GUIs
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Handle Main GUI
        if (title.equals(getLang("gui.title"))) {
            handleMainGUIClick(player, clickedItem, event.getClick().isLeftClick());
            return;
        }

        // Handle Settings GUIs
        handleSettingsGUIClick(player, clickedItem, title, event.getSlot());
    }

    private void handleMainGUIClick(Player player, ItemStack clickedItem, boolean isLeftClick) {
        switch (clickedItem.getType()) {
            case ITEM_FRAME:
                if (isLeftClick) {
                    toggleEnabled(player, "FrameDupe", "dupe.itemframe.name");
                    frameDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.ITEMFRAME_SETTINGS);
                }
                break;

            case GLOW_ITEM_FRAME:
                if (isLeftClick) {
                    toggleEnabled(player, "GLOW_FrameDupe", "dupe.glowframe.name");
                    frameDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.GLOWFRAME_SETTINGS);
                }
                break;

            case DONKEY_SPAWN_EGG:
                if (isLeftClick) {
                    toggleEnabled(player, "OtherDupes.DonkeyDupe", "dupe.donkey.name");
                    donkeyDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.DONKEY_SETTINGS);
                }
                break;

            case GRINDSTONE:
                if (isLeftClick) {
                    toggleEnabled(player, "OtherDupes.GrindStone", "dupe.grindstone.name");
                    grindstoneDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.GRINDSTONE_SETTINGS);
                }
                break;

            case CRAFTER:
                if (isLeftClick) {
                    toggleEnabled(player, "OtherDupes.CrafterDupe", "dupe.crafter.name");
                    crafterDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.CRAFTER_SETTINGS);
                }
                break;

            case DROPPER:
                if (isLeftClick) {
                    toggleEnabled(player, "OtherDupes.DropperDupe", "dupe.dropper.name");
                    dropperDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.DROPPER_SETTINGS);
                }
                break;

            case BARRIER:
                // Reload button
                reloadAllConfigs(player);
                openMainGUI(player);
                break;
            case ANVIL:
                // Blacklist button
                openGUI(player, GUIType.BLACKLIST_SETTINGS);
                break;
            case SKELETON_SPAWN_EGG:
                if (isLeftClick) {
                    toggleEnabled(player, "OtherDupes.DeathDupe", "dupe.death.name");
                    deathDupe.reload();
                    openMainGUI(player);
                } else {
                    openGUI(player, GUIType.DEATH_SETTINGS);
                }
                break;
        }
    }

// ==================== DEATH SETTINGS ====================
private void openDeathSettings(Player player) {
    String basePath = "OtherDupes.DeathDupe";
    Inventory gui = Bukkit.createInventory(null, 45,
            getLang("gui.title.settings", getLang("dupe.death.name")));

    fillWithGlass(gui);

    // Enable/Disable Toggle (Slot 4)
    gui.setItem(4, createToggleItem(basePath + ".Enabled", "dupe.death.name"));

    // Back Button (Slot 40)
    gui.setItem(40, createBackButton());

    player.openInventory(gui);
}

// ==================== BLACKLIST SETTINGS ====================
private void openBlacklistSettings(Player player) {
    Inventory gui = Bukkit.createInventory(null, 45, "Item Blacklist");

    fillWithGlass(gui);

    // Center slot for adding items (Slot 22) - hopper as visual indicator
    ItemStack hopper = new ItemStack(Material.HOPPER);
    ItemMeta hopperMeta = hopper.getItemMeta();
    hopperMeta.setDisplayName(getLang("color.info") + "Drop items here to blacklist");
    hopperMeta.setLore(Arrays.asList(getLang("color.description") + "Click here with an item to add it to the blacklist"));
    hopper.setItemMeta(hopperMeta);
    gui.setItem(22, hopper);
    plugin.getLogger().info("[DEBUG] Blacklist GUI opened. Slot 22 item: " + (gui.getItem(22) != null ? gui.getItem(22).getType().name() : "null"));

    // Back Button (Slot 40)
    gui.setItem(40, createBackButton());

    player.openInventory(gui);
}

private void handleDeathSettingsClick(Player player, int slot, ItemStack item) {
    String basePath = "OtherDupes.DeathDupe";

    if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
        toggleEnabled(player, basePath, "dupe.death.name");
        deathDupe.reload();
        openDeathSettings(player);
    }
}


    private void handleSettingsGUIClick(Player player, ItemStack clickedItem, String title, int slot) {
        // Back button
        if (clickedItem.getType() == Material.ARROW) {
            openMainGUI(player);
            return;
        }

        // Determine which settings GUI we're in
        if (title.contains(getLang("dupe.itemframe.name"))) {
            handleItemFrameSettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.glowframe.name"))) {
            handleGlowFrameSettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.donkey.name"))) {
            handleDonkeySettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.grindstone.name"))) {
            handleGrindstoneSettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.crafter.name"))) {
            handleCrafterSettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.dropper.name"))) {
            handleDropperSettingsClick(player, slot, clickedItem);
        } else if (title.contains(getLang("dupe.death.name"))) {
            handleDeathSettingsClick(player, slot, clickedItem);
        }
    }

    private void handleItemFrameSettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "FrameDupe";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.itemframe.name");
            frameDupe.reload();
            openItemFrameSettings(player);
        }
        // Probability adjustments (slots 11-15)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            int adjustment = getAdjustmentValue(slot, 11);
            adjustIntValue(player, basePath + ".Probability-percentage", adjustment, 0, 100);
            frameDupe.reload();
            openItemFrameSettings(player);
        }
        // Multiplier adjustments (slots 20-24)
        else if (slot >= 20 && slot <= 24 && item.getType().name().contains("CONCRETE")) {
            int adjustment = getAdjustmentValue(slot, 20);
            adjustIntValue(player, basePath + ".Multiplier", adjustment, 1, 100);
            frameDupe.reload();
            openItemFrameSettings(player);
        }
    }

    private void handleGlowFrameSettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "GLOW_FrameDupe";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.glowframe.name");
            frameDupe.reload();
            openGlowFrameSettings(player);
        }
        // Probability adjustments (slots 20-24)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            int adjustment = getAdjustmentValue(slot, 11);
            adjustIntValue(player, basePath + ".Probability-percentage", adjustment, 0, 100);
            frameDupe.reload();
            openGlowFrameSettings(player);
        }
        // Multiplier adjustments (slots 29-33)
        else if (slot >= 20 && slot <= 24 && item.getType().name().contains("CONCRETE")) {
            int adjustment = getAdjustmentValue(slot, 20);
            adjustIntValue(player, basePath + ".Multiplier", adjustment, 1, 100);
            frameDupe.reload();
            openGlowFrameSettings(player);
        }
    }

    private void handleDonkeySettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "OtherDupes.DonkeyDupe";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.donkey.name");
            donkeyDupe.reload();
            openDonkeySettings(player);
        }
        // Min Timing adjustments (slots 11-15)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 11);
            adjustLongValue(player, basePath + ".MinTiming", adjustment, 0,
                    plugin.getConfig().getLong(basePath + ".MaxTiming"));
            donkeyDupe.reload();
            openDonkeySettings(player);
        }
        // Max Timing adjustments (slots 20-24)
        else if (slot >= 20 && slot <= 24 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 20);
            adjustLongValue(player, basePath + ".MaxTiming", adjustment,
                    plugin.getConfig().getLong(basePath + ".MinTiming"), Long.MAX_VALUE);
            donkeyDupe.reload();
            openDonkeySettings(player);
        }
    }

    private void handleGrindstoneSettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "OtherDupes.GrindStone";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.grindstone.name");
            grindstoneDupe.reload();
            openGrindstoneSettings(player);
        }
        // Min Timing adjustments (slots 10-14)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 11);
            adjustLongValue(player, basePath + ".MinTiming", adjustment, 0,
                    plugin.getConfig().getLong(basePath + ".MaxTiming"));
            grindstoneDupe.reload();
            openGrindstoneSettings(player);
        }
        // Max Timing adjustments (slots 19-23)
        else if (slot >= 20 && slot <= 24 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 20);
            adjustLongValue(player, basePath + ".MaxTiming", adjustment,
                    plugin.getConfig().getLong(basePath + ".MinTiming"), Long.MAX_VALUE);
            grindstoneDupe.reload();
            openGrindstoneSettings(player);
        }
        // Drop Naturally toggle (slot 29)
        else if (slot == 29) {
            toggleBoolean(player, basePath + ".dropNaturally");
            grindstoneDupe.reload();
            openGrindstoneSettings(player);
        }
        // Add to Inventory toggle (slot 33)
        else if (slot == 33) {
            toggleBoolean(player, basePath + ".addToInventory");
            grindstoneDupe.reload();
            openGrindstoneSettings(player);
        }
    }

    private void handleCrafterSettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "OtherDupes.CrafterDupe";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.crafter.name");
            crafterDupe.reload();
            openCrafterSettings(player);
        }
        // Min Timing adjustments (slots 11-15)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 11);
            adjustLongValue(player, basePath + ".MinTiming", adjustment, 0,
                    plugin.getConfig().getLong(basePath + ".MaxTiming"));
            crafterDupe.reload();
            openCrafterSettings(player);
        }
        // Max Timing adjustments (slots 20-24)
        else if (slot >= 20 && slot <= 24 && item.getType().name().contains("CONCRETE")) {
            long adjustment = getTimingAdjustment(slot, 20);
            adjustLongValue(player, basePath + ".MaxTiming", adjustment,
                    plugin.getConfig().getLong(basePath + ".MinTiming"), Long.MAX_VALUE);
            crafterDupe.reload();
            openCrafterSettings(player);
        }
        // Destroy Crafter toggle (slot 29)
        else if (slot == 29) {
            toggleBoolean(player, basePath + ".destroyCrafter");
            crafterDupe.reload();
            openCrafterSettings(player);
        }
        // Drop Originals toggle (slot 33)
        else if (slot == 33) {
            toggleBoolean(player, basePath + ".dropOriginals");
            crafterDupe.reload();
            openCrafterSettings(player);
        }
    }

    private void handleDropperSettingsClick(Player player, int slot, ItemStack item) {
        String basePath = "OtherDupes.DropperDupe";

        if (slot == 4 && (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE)) {
            toggleEnabled(player, basePath, "dupe.dropper.name");
            dropperDupe.reload();
            openDropperSettings(player);
        }
        // Multiplier adjustments (slots 11-15)
        else if (slot >= 11 && slot <= 15 && item.getType().name().contains("CONCRETE")) {
            int adjustment = getAdjustmentValue(slot, 11);
            adjustIntValue(player, basePath + ".Multiplier", adjustment, 1, 100);
            dropperDupe.reload();
            openDropperSettings(player);
        }
    }

    // ==================== ADJUSTMENT HELPER METHODS ====================

    private int getAdjustmentValue(int slot, int baseSlot) {
        int offset = slot - baseSlot;
        switch (offset) {
            case 0: return -10;
            case 1: return -1;
            case 2: return 0; // Display slot
            case 3: return 1;
            case 4: return 10;
            default: return 0;
        }
    }

    private long getTimingAdjustment(int slot, int baseSlot) {
        int offset = slot - baseSlot;
        switch (offset) {
            case 0: return -100;
            case 1: return -10;
            case 2: return 0; // Display slot
            case 3: return 10;
            case 4: return 100;
            default: return 0;
        }
    }

    private void adjustIntValue(Player player, String path, int adjustment, int min, int max) {
        int current = plugin.getConfig().getInt(path, 0);
        int newValue = Math.max(min, Math.min(max, current + adjustment));

        if (newValue == current) {
            if (adjustment < 0) {
                player.sendMessage(getLang("color.error") + getLang("msg.value_too_low"));
            } else {
                player.sendMessage(getLang("color.error") + "Value cannot exceed " + max);
            }
            return;
        }

        plugin.getConfig().set(path, newValue);
        plugin.saveConfig();
        player.sendMessage(getLang("color.success") + getLang("msg.value_set") + " " +
                getLang("color.value") + newValue);
        plugin.reloadConfig();
    }

    private void adjustLongValue(Player player, String path, long adjustment, long min, long max) {
        long current = plugin.getConfig().getLong(path, 0L);
        long newValue = Math.max(min, Math.min(max, current + adjustment));

        if (newValue == current) {
            if (adjustment < 0) {
                player.sendMessage(getLang("color.error") + getLang("msg.value_too_low"));
            } else {
                player.sendMessage(getLang("color.error") + "Value at maximum");
            }
            return;
        }

        plugin.getConfig().set(path, newValue);
        plugin.saveConfig();
        player.sendMessage(getLang("color.success") + getLang("msg.timing_set") + " " +
                getLang("color.value") + newValue + "ms");
        plugin.reloadConfig();
    }

    private void toggleEnabled(Player player, String basePath, String nameKey) {
        boolean enabled = !plugin.getConfig().getBoolean(basePath + ".Enabled");
        plugin.getConfig().set(basePath + ".Enabled", enabled);
        plugin.saveConfig();

        String statusColor = enabled ? getLang("color.success") : getLang("color.error");
        String status = enabled ? getLang("msg.enabled") : getLang("msg.disabled");
        player.sendMessage(statusColor + getLang(nameKey) + " " + status);

        plugin.reloadConfig();
    }

    private void toggleBoolean(Player player, String path) {
        boolean current = plugin.getConfig().getBoolean(path, false);
        plugin.getConfig().set(path, !current);
        plugin.saveConfig();
        plugin.reloadConfig();
    }

    private void addItemToBlacklist(ItemStack item) {
        List<Map<String, Object>> blacklist = new ArrayList<>();
        for (Map<?, ?> map : plugin.getConfig().getMapList("ItemBlacklist")) {
            Map<String, Object> newMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                newMap.put((String) entry.getKey(), entry.getValue());
            }
            blacklist.add(newMap);
        }

        // Check if already blacklisted
        String materialName = item.getType().name();
        String lowerMaterialName = materialName.toLowerCase();
        for (Map<String, Object> entry : blacklist) {
            if ("minecraft".equals(entry.get("Namespace")) && lowerMaterialName.equals(entry.get("Key"))) {
                return; // Already blacklisted
            }
        }

        // Add new entry
        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("Namespace", "minecraft");
        newEntry.put("Key", lowerMaterialName);
        newEntry.put("Names", new ArrayList<String>());

        blacklist.add(newEntry);
        plugin.getConfig().set("ItemBlacklist", blacklist);
        plugin.saveConfig();
        plugin.reloadConfig();
    }

    private void reloadAllConfigs(Player player) {
        plugin.reloadConfig();
        frameDupe.reload();
        donkeyDupe.reload();
        grindstoneDupe.reload();
        crafterDupe.reload();
        dropperDupe.reload();
        deathDupe.reload();

        player.sendMessage(getLang("color.success") + getLang("msg.config_reloaded"));
    }

    // Method to reload language (for future language file support)
    public void reloadLanguage() {
        // TODO: Load from language file
        initializeLanguage();
    }
}
