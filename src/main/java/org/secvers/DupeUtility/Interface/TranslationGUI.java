package org.secvers.DupeUtility.Interface;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.secvers.DupeUtility.Helper.LanguageGUICloseEvent;
import org.secvers.DupeUtility.Translation.TranslationWorker;

import java.util.ArrayList;
import java.util.List;

public class TranslationGUI implements Listener {
    private final Plugin plugin;
    private final TranslationWorker translator;
    private static final String GUI_TITLE_KEY = "gui.language.title";

    public TranslationGUI(Plugin plugin, TranslationWorker translator) {
        this.plugin = plugin;
        this.translator = translator;
    }

    /**
     * Open the language selection GUI
     */
    public void open(Player player) {
        if (!translator.isEnabled()) {
            player.sendMessage("§cTranslation system is disabled!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, getLang(player, GUI_TITLE_KEY));

        fillWithGlass(gui);

        List<String> languages = translator.getAvailableLanguages();
        String currentLang = translator.getPlayerLanguage(player);

        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < Math.min(languages.size(), slots.length); i++) {
            String lang = languages.get(i);
            gui.setItem(slots[i], createLanguageItem(player, lang, lang.equalsIgnoreCase(currentLang)));
        }

        // Info item in center top (Slot 4)
        gui.setItem(4, createInfoItem(player));

        // Close Button (Slot 44)
        gui.setItem(44, createCloseButton(player));

        player.openInventory(gui);
    }

    private ItemStack createLanguageItem(Player player, String language, boolean isCurrent) {
        TranslationWorker.LanguageMetadata meta = translator.getLanguageMetadata(language);

        Material material;
        if (isCurrent) {
            material = Material.LIME_BANNER;
        } else {
            material = Material.WHITE_BANNER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        String displayName = meta != null ? meta.getNativeName() : language;
        String color = isCurrent ? "§a§l" : "§e";

        itemMeta.setDisplayName(color + displayName);

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (meta != null) {
            lore.add(getLang(player, "color.description") + "Language: " +
                    getLang(player, "color.value") + meta.getLanguage());
            lore.add(getLang(player, "color.description") + getLang(player, "gui.language.version") + ": " +
                    getLang(player, "color.value") + meta.getVersion());
            lore.add(getLang(player, "color.description") + getLang(player, "gui.language.author") + ": " +
                    getLang(player, "color.value") + meta.getAuthor());
            lore.add("");
            lore.add(getLang(player, "color.description") + meta.getDescription());

            if (!meta.getCountryCodes().isEmpty()) {
                lore.add("");
                lore.add(getLang(player, "color.description") + "Countries: " +
                        getLang(player, "color.value") + String.join(", ", meta.getCountryCodes()));
            }

            lore.add("");
        }

        if (isCurrent) {
            lore.add(getLang(player, "color.success") + "✓ " + getLang(player, "gui.language.current"));
        } else {
            lore.add(getLang(player, "color.info") + getLang(player, "gui.left_click") + " " +
                    getLang(player, "color.description") + getLang(player, "gui.language.click_to_select"));
        }

        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return item;
    }

    /**
     * Create info item
     */
    private ItemStack createInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang(player, "color.info") + getLang(player, "gui.language"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(getLang(player, "color.description") + getLang(player, "gui.language.desc"));
        lore.add("");
        lore.add(getLang(player, "color.description") + "Available: " +
                getLang(player, "color.value") + translator.getAvailableLanguages().size() + " languages");
        lore.add("");

        String currentLang = translator.getPlayerLanguage(player);
        TranslationWorker.LanguageMetadata currentMeta = translator.getLanguageMetadata(currentLang);
        String displayName = currentMeta != null ? currentMeta.getNativeName() : currentLang;

        lore.add(getLang(player, "color.success") + getLang(player, "gui.language.current") + ":");
        lore.add(getLang(player, "color.value") + "  " + displayName);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create back button
     */
    private ItemStack createBackButton(Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang(player, "color.info") + getLang(player, "gui.back"));
        meta.setLore(List.of(
                "",
                getLang(player, "color.description") + "Return to main menu"
        ));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create close button
     */
    private ItemStack createCloseButton(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(getLang(player, "color.error") + "Close");
        meta.setLore(List.of(
                "",
                getLang(player, "color.description") + "Close this menu"
        ));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Fill empty slots with glass panes
     */
    private void fillWithGlass(Inventory gui) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }
    }

    /**
     * Get translation for player
     */
    private String getLang(Player player, String key) {
        return translator.isEnabled() ? translator.getTranslation(player, key) : key;
    }

    private String getLang(Player player, String key, Object... args) {
        return translator.isEnabled() ? translator.getTranslation(player, key, args) : key;
    }

    /**
     * Check if inventory is language GUI
     */
    public boolean isLanguageGUI(Player player, String title) {
        return title.equals(getLang(player, GUI_TITLE_KEY));
    }

    /**
     * Handle inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's our GUI
        if (!isLanguageGUI(player, title)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Material type = clickedItem.getType();

        // Back button - reopen main interface
        if (type == Material.ARROW) {
            player.closeInventory();
            // Trigger event to reopen main GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().getPluginManager().callEvent(
                        new LanguageGUICloseEvent(player, true)
                );
            });
            return;
        }

        // Close button
        if (type == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Info item - ignore
        if (type == Material.BOOK) {
            return;
        }

        // Glass pane - ignore
        if (type == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        // Language selection (banners)
        if (type == Material.WHITE_BANNER || type == Material.LIME_BANNER) {
            handleLanguageSelection(player, clickedItem);
        }
    }

    /**
     * Handle language selection
     */
    private void handleLanguageSelection(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String displayName = meta.getDisplayName()
                .replace("§e", "")
                .replace("§a§l", "")
                .replace("§a", "")
                .trim();

        // Find matching language
        for (String lang : translator.getAvailableLanguages()) {
            TranslationWorker.LanguageMetadata langMeta = translator.getLanguageMetadata(lang);
            if (langMeta != null && langMeta.getNativeName().equals(displayName)) {
                translator.setPlayerLanguage(player, lang);

                // Refresh GUI
                plugin.getServer().getScheduler().runTask(plugin, () -> open(player));
                return;
            }
        }

        plugin.getLogger().warning("Could not find language for display name: " + displayName);
    }
}
