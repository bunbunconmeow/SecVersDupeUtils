package org.secvers.DupeUtility.Translation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TranslationWorker {
    private final Plugin plugin;
    private final Gson gson;
    private final Path translationsFolder;

    // Cache for translations: language -> key -> translation
    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();

    // Player language preferences: UUID -> language
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();

    // IP to country cache: IP -> country code
    private final Map<String, String> ipCountryCache = new ConcurrentHashMap<>();

    // Metadata for each language
    private final Map<String, LanguageMetadata> languageMetadata = new ConcurrentHashMap<>();

    private boolean enabled;
    private boolean ipBased;
    private String defaultLanguage;
    private Set<String> detectedLanguages = new HashSet<>();

    public TranslationWorker(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        this.translationsFolder = Paths.get(plugin.getDataFolder().getPath(), "translations");

        loadConfig();
        initializeTranslations();
    }

    /**
     * Load configuration from config.yml
     */
    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("Translation.enabled", false);
        ipBased = plugin.getConfig().getBoolean("Translation.ipbased", false);
        defaultLanguage = plugin.getConfig().getString("Translation.default", "English");

        plugin.getLogger().info("Translation System - Enabled: " + enabled + ", IP-Based: " + ipBased + ", Default: " + defaultLanguage);
    }

    /**
     * Initialize translation system and create default files
     */
    private void initializeTranslations() {
        if (!enabled) {
            plugin.getLogger().info("Translation system is disabled.");
            return;
        }

        try {
            // Create translations folder if it doesn't exist
            if (!Files.exists(translationsFolder)) {
                Files.createDirectories(translationsFolder);
                plugin.getLogger().info("Created translations folder: " + translationsFolder);
            }

            // Create default translation files (English and German)
            createDefaultTranslationFile("English");
            createDefaultTranslationFile("German");

            // Scan folder for all available translations
            scanTranslationsFolder();

            // Load all translations into cache
            loadAllTranslations();

            // Update config with detected languages
            updateConfigWithDetectedLanguages();

            plugin.getLogger().info("§a═══════════════════════════════════════");
            plugin.getLogger().info("§aTranslation System Initialized");
            plugin.getLogger().info("§aDetected Languages: " + detectedLanguages.size());
            for (String lang : detectedLanguages) {
                LanguageMetadata meta = languageMetadata.get(lang.toLowerCase());
                if (meta != null) {
                    plugin.getLogger().info("§a  - " + lang + " (" + meta.getNativeName() + ") by " + meta.getAuthor());
                } else {
                    plugin.getLogger().info("§a  - " + lang);
                }
            }
            plugin.getLogger().info("§a═══════════════════════════════════════");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to initialize translations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Scan translations folder for all available language files
     */
    private void scanTranslationsFolder() throws IOException {
        detectedLanguages.clear();

        if (!Files.exists(translationsFolder)) {
            return;
        }

        Files.list(translationsFolder)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String languageName = fileName.substring(0, fileName.length() - 5); // Remove .json
                    // Capitalize first letter
                    languageName = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
                    detectedLanguages.add(languageName);
                });

        plugin.getLogger().info("Scanned translations folder, found " + detectedLanguages.size() + " language(s)");
    }

    /**
     * Update config.yml with detected languages
     */
    private void updateConfigWithDetectedLanguages() {
        if (detectedLanguages.isEmpty()) {
            return;
        }

        List<String> currentList = plugin.getConfig().getStringList("Translation.translations");
        Set<String> currentSet = new HashSet<>(currentList);

        boolean updated = false;
        for (String lang : detectedLanguages) {
            if (!currentSet.contains(lang)) {
                currentList.add(lang);
                updated = true;
            }
        }

        if (updated) {
            plugin.getConfig().set("Translation.translations", currentList);
            plugin.saveConfig();
            plugin.getLogger().info("Updated config.yml with newly detected languages");
        }
    }

    /**
     * Create default translation file if it doesn't exist
     */
    private void createDefaultTranslationFile(String language) throws IOException {
        Path filePath = translationsFolder.resolve(language.toLowerCase() + ".json");

        if (Files.exists(filePath)) {
            plugin.getLogger().info("Translation file already exists: " + language);
            return;
        }

        LanguageFile langFile = new LanguageFile();
        langFile.metadata = createDefaultMetadata(language);
        langFile.translations = getDefaultTranslations(language);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8)) {
            gson.toJson(langFile, writer);
            plugin.getLogger().info("Created default translation file: " + language);
        }
    }

    /**
     * Create default metadata for a language
     */
    private LanguageMetadata createDefaultMetadata(String language) {
        LanguageMetadata meta = new LanguageMetadata();
        meta.setLanguage(language);
        meta.setVersion("1.0.0");
        meta.setAuthor("SecVers");
        meta.setDescription("Default " + language + " translation");

        if (language.equalsIgnoreCase("German")) {
            meta.setNativeName("Deutsch");
            meta.setCountryCodes(Arrays.asList("DE", "AT", "CH"));
        } else if (language.equalsIgnoreCase("English")) {
            meta.setNativeName("English");
            meta.setCountryCodes(Arrays.asList("US", "GB", "CA", "AU", "NZ"));
        }

        return meta;
    }

    /**
     * Get default translations based on language
     */
    private Map<String, String> getDefaultTranslations(String language) {
        Map<String, String> translations = new LinkedHashMap<>();

        if (language.equalsIgnoreCase("German")) {
            // German translations
            // Translation Menu
            translations.put("gui.language", "Sprache");
            translations.put("gui.language_desc", "Wähle deine bevorzugte Sprache");
            translations.put("gui.language.desc", "Klicke um die Sprache zu ändern");
            translations.put("gui.language.current", "Aktuelle Sprache");
            translations.put("gui.language.version", "Version");
            translations.put("gui.language.author", "Autor");
            translations.put("gui.language.title", "§6Sprach Einstellungen");
            translations.put("gui.language.click_to_select", "Klicken zum Auswählen");

            // Menu
            translations.put("gui.title", "§6SecVers Dupe Konfiguration");
            translations.put("gui.title.settings", "§6{0} Einstellungen");
            translations.put("gui.enabled", "§aAktiviert");
            translations.put("gui.disabled", "§cDeaktiviert");
            translations.put("gui.probability", "Wahrscheinlichkeit");
            translations.put("gui.multiplier", "Multiplikator");
            translations.put("gui.min_timing", "Minimale Zeit");
            translations.put("gui.max_timing", "Maximale Zeit");
            translations.put("gui.drop_naturally", "Natürlich fallen lassen");
            translations.put("gui.add_to_inventory", "Zum Inventar hinzufügen");
            translations.put("gui.destroy_crafter", "Crafter zerstören");
            translations.put("gui.drop_originals", "Originale droppen");
            translations.put("gui.yes", "Ja");
            translations.put("gui.no", "Nein");
            translations.put("gui.left_click", "Linksklick");
            translations.put("gui.right_click", "Rechtsklick");
            translations.put("gui.shift_left_click", "Shift+Linksklick");
            translations.put("gui.shift_right_click", "Shift+Rechtsklick");
            translations.put("gui.to_toggle", "zum Umschalten");
            translations.put("gui.to_change_probability", "um Wahrscheinlichkeit zu ändern");
            translations.put("gui.to_change_multiplier", "um Multiplikator zu ändern");
            translations.put("gui.to_cycle_mode", "um Modus zu wechseln");
            translations.put("gui.to_toggle_option", "um Option umzuschalten");
            translations.put("gui.to_increase", "zum Erhöhen");
            translations.put("gui.to_decrease", "zum Verringern");
            translations.put("gui.to_open_settings", "um Einstellungen zu öffnen");
            translations.put("gui.reload", "Konfiguration neu laden");
            translations.put("gui.reload_desc", "Lädt alle Dupe-Konfigurationen neu");
            translations.put("gui.back", "Zurück zum Hauptmenü");
            translations.put("gui.blacklist", "Blacklist");
            translations.put("gui.blacklist_desc", "Verwalte blockierte Items");

            // Dupe names and descriptions
            translations.put("dupe.itemframe.name", "Item Frame Dupe");
            translations.put("dupe.itemframe.desc", "Dupliziere Items mit Item Frames");
            translations.put("dupe.glowframe.name", "Glow Frame Dupe");
            translations.put("dupe.glowframe.desc", "Dupliziere Items mit Glow Item Frames");
            translations.put("dupe.donkey.name", "Donkey Shulker Dupe");
            translations.put("dupe.donkey.desc", "Dupliziere Items mit Esel und Shulkern");
            translations.put("dupe.grindstone.name", "Grindstone Dupe");
            translations.put("dupe.grindstone.desc", "Dupliziere Items mit Schleifstein");
            translations.put("dupe.crafter.name", "Crafter Dupe");
            translations.put("dupe.crafter.desc", "Dupliziere Items mit Crafter");
            translations.put("dupe.dropper.name", "Dropper Dupe");
            translations.put("dupe.dropper.desc", "Dupliziere Items mit Dropper");
            translations.put("dupe.death.name", "Death Dupe");
            translations.put("dupe.death.desc", "Behalte Items beim Tod");

            // Messages
            translations.put("msg.enabled", "{0} wurde §aaktiviert");
            translations.put("msg.disabled", "{0} wurde §cdeaktiviert");
            translations.put("msg.probability_set", "Wahrscheinlichkeit gesetzt auf");
            translations.put("msg.multiplier_set", "Multiplikator gesetzt auf");
            translations.put("msg.mode_drop_only", "Modus: Nur Droppen");
            translations.put("msg.mode_inventory_only", "Modus: Nur Inventar");
            translations.put("msg.mode_both", "Modus: Droppen + Inventar");
            translations.put("msg.destroy_crafter", "Crafter zerstören");
            translations.put("msg.drop_originals", "Originale droppen");
            translations.put("msg.timing_set", "Timing gesetzt auf");
            translations.put("msg.value_set", "Wert gesetzt auf");
            translations.put("msg.config_reloaded", "§aKonfiguration erfolgreich neu geladen!");
            translations.put("msg.min_cannot_exceed_max", "§cMin Timing kann nicht größer als Max Timing sein!");
            translations.put("msg.max_cannot_below_min", "§cMax Timing kann nicht kleiner als Min Timing sein!");
            translations.put("msg.value_too_low", "§cWert kann nicht unter 0 gesetzt werden!");
            translations.put("msg.language_changed", "§aSprache geändert zu: {0}");
            translations.put("msg.language_list", "§eVerfügbare Sprachen: {0}");
            translations.put("msg.language_invalid", "§cUngültige Sprache! Verfügbare Sprachen: {0}");

            // Colors
            translations.put("color.success", "§a");
            translations.put("color.error", "§c");
            translations.put("color.info", "§e");
            translations.put("color.value", "§e");
            translations.put("color.description", "§7");
            translations.put("color.reset", "§r");

        } else {
            // English translations (default)
            // Translation Menu
            translations.put("gui.language", "Language");
            translations.put("gui.language_desc", "Select your preferred language");
            translations.put("gui.language.desc", "Click to change language");
            translations.put("gui.language.current", "Current Language");
            translations.put("gui.language.version", "Version");
            translations.put("gui.language.author", "Author");
            translations.put("gui.language.title", "§6Language Settings");
            translations.put("gui.language.click_to_select", "Click to select");

            // Menu
            translations.put("gui.title", "§6SecVers Dupe Configuration");
            translations.put("gui.title.settings", "§6{0} Settings");
            translations.put("gui.enabled", "§aEnabled");
            translations.put("gui.disabled", "§cDisabled");
            translations.put("gui.probability", "Probability");
            translations.put("gui.multiplier", "Multiplier");
            translations.put("gui.min_timing", "Minimum Timing");
            translations.put("gui.max_timing", "Maximum Timing");
            translations.put("gui.drop_naturally", "Drop Naturally");
            translations.put("gui.add_to_inventory", "Add to Inventory");
            translations.put("gui.destroy_crafter", "Destroy Crafter");
            translations.put("gui.drop_originals", "Drop Originals");
            translations.put("gui.yes", "Yes");
            translations.put("gui.no", "No");
            translations.put("gui.left_click", "Left Click");
            translations.put("gui.right_click", "Right Click");
            translations.put("gui.shift_left_click", "Shift+Left Click");
            translations.put("gui.shift_right_click", "Shift+Right Click");
            translations.put("gui.to_toggle", "to toggle");
            translations.put("gui.to_change_probability", "to change probability");
            translations.put("gui.to_change_multiplier", "to change multiplier");
            translations.put("gui.to_cycle_mode", "to cycle mode");
            translations.put("gui.to_toggle_option", "to toggle option");
            translations.put("gui.to_increase", "to increase");
            translations.put("gui.to_decrease", "to decrease");
            translations.put("gui.to_open_settings", "to open settings");
            translations.put("gui.reload", "Reload Configuration");
            translations.put("gui.reload_desc", "Reloads all dupe configurations");
            translations.put("gui.back", "Back to Main Menu");
            translations.put("gui.blacklist", "Blacklist");
            translations.put("gui.blacklist_desc", "Manage blocked items");

            // Dupe names and descriptions
            translations.put("dupe.itemframe.name", "Item Frame Dupe");
            translations.put("dupe.itemframe.desc", "Duplicate items using item frames");
            translations.put("dupe.glowframe.name", "Glow Frame Dupe");
            translations.put("dupe.glowframe.desc", "Duplicate items using glow item frames");
            translations.put("dupe.donkey.name", "Donkey Shulker Dupe");
            translations.put("dupe.donkey.desc", "Duplicate items using donkeys and shulkers");
            translations.put("dupe.grindstone.name", "Grindstone Dupe");
            translations.put("dupe.grindstone.desc", "Duplicate items using grindstone");
            translations.put("dupe.crafter.name", "Crafter Dupe");
            translations.put("dupe.crafter.desc", "Duplicate items using crafter");
            translations.put("dupe.dropper.name", "Dropper Dupe");
            translations.put("dupe.dropper.desc", "Duplicate items using droppers");
            translations.put("dupe.death.name", "Death Dupe");
            translations.put("dupe.death.desc", "Keep items on death");

            // Messages
            translations.put("msg.enabled", "{0} has been §aenabled");
            translations.put("msg.disabled", "{0} has been §cdisabled");
            translations.put("msg.probability_set", "Probability set to");
            translations.put("msg.multiplier_set", "Multiplier set to");
            translations.put("msg.mode_drop_only", "Mode: Drop Only");
            translations.put("msg.mode_inventory_only", "Mode: Inventory Only");
            translations.put("msg.mode_both", "Mode: Drop + Inventory");
            translations.put("msg.destroy_crafter", "Destroy Crafter");
            translations.put("msg.drop_originals", "Drop Originals");
            translations.put("msg.timing_set", "Timing set to");
            translations.put("msg.value_set", "Value set to");
            translations.put("msg.config_reloaded", "§aConfiguration reloaded successfully!");
            translations.put("msg.min_cannot_exceed_max", "§cMin timing cannot exceed max timing!");
            translations.put("msg.max_cannot_below_min", "§cMax timing cannot be below min timing!");
            translations.put("msg.value_too_low", "§cValue cannot be set below 0!");
            translations.put("msg.language_changed", "§aLanguage changed to: {0}");
            translations.put("msg.language_list", "§eAvailable languages: {0}");
            translations.put("msg.language_invalid", "§cInvalid language! Available languages: {0}");

            // Colors
            translations.put("color.success", "§a");
            translations.put("color.error", "§c");
            translations.put("color.info", "§e");
            translations.put("color.value", "§e");
            translations.put("color.description", "§7");
            translations.put("color.reset", "§r");
        }

        return translations;
    }

    /**
     * Load all translation files into cache
     */
    private void loadAllTranslations() {
        translationCache.clear();
        languageMetadata.clear();

        for (String language : detectedLanguages) {
            try {
                LanguageFile langFile = loadTranslationFile(language);
                translationCache.put(language.toLowerCase(), langFile.translations);
                languageMetadata.put(language.toLowerCase(), langFile.metadata);
                plugin.getLogger().info("Loaded " + langFile.translations.size() + " translations for: " + language + " v" + langFile.metadata.getVersion());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load translations for " + language + ": " + e.getMessage());
            }
        }
    }

    /**
     * Load a single translation file
     */
    private LanguageFile loadTranslationFile(String language) throws IOException {
        Path filePath = translationsFolder.resolve(language.toLowerCase() + ".json");

        if (!Files.exists(filePath)) {
            plugin.getLogger().warning("Translation file not found: " + language);
            return new LanguageFile();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8)) {
            LanguageFile langFile = gson.fromJson(reader, LanguageFile.class);

            // Ensure metadata exists
            if (langFile.metadata == null) {
                langFile.metadata = createDefaultMetadata(language);
            }

            // Ensure translations map exists
            if (langFile.translations == null) {
                langFile.translations = new HashMap<>();
            }

            return langFile;
        }
    }

    /**
     * Get translation for a player
     */
    public String getTranslation(Player player, String key) {
        if (!enabled) {
            return key;
        }

        String language = getPlayerLanguage(player);
        Map<String, String> translations = translationCache.get(language.toLowerCase());

        // Fallback to default language
        if (translations == null || !translations.containsKey(key)) {
            translations = translationCache.get(defaultLanguage.toLowerCase());
        }

        // Fallback to English if default is also missing
        if (translations == null || !translations.containsKey(key)) {
            translations = translationCache.get("english");
        }

        return translations != null ? translations.getOrDefault(key, key) : key;
    }

    /**
     * Get translation with placeholders
     */
    public String getTranslation(Player player, String key, Object... args) {
        String translation = getTranslation(player, key);

        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return translation;
    }

    /**
     * Get player's language preference
     */
    public String getPlayerLanguage(Player player) {
        // Check if player has a set preference
        if (playerLanguages.containsKey(player.getUniqueId())) {
            return playerLanguages.get(player.getUniqueId());
        }

        // Use default language
        return defaultLanguage;
    }


    /**
     * Set player's language preference
     */
    public void setPlayerLanguage(Player player, String language) {
        // Case-insensitive check
        String matchedLanguage = null;
        for (String availableLang : detectedLanguages) {
            if (availableLang.equalsIgnoreCase(language)) {
                matchedLanguage = availableLang;
                break;
            }
        }

        if (matchedLanguage == null) {
            String availableList = String.join(", ", detectedLanguages);
            player.sendMessage(getTranslation(player, "msg.language_invalid", availableList));
            return;
        }

        playerLanguages.put(player.getUniqueId(), matchedLanguage);
        player.sendMessage(getTranslation(player, "msg.language_changed", matchedLanguage));
    }

    /**
     * Reload translation system
     */
    public void reload() {
        try {
            loadConfig();
            scanTranslationsFolder();
            loadAllTranslations();
            updateConfigWithDetectedLanguages();
            plugin.getLogger().info("§aTranslation system reloaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload translation system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get available languages
     */
    public List<String> getAvailableLanguages() {
        return new ArrayList<>(detectedLanguages);
    }

    /**
     * Check if translation system is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get language metadata
     */
    public LanguageMetadata getLanguageMetadata(String language) {
        return languageMetadata.get(language.toLowerCase());
    }

    /**
     * Create a template translation file for community contributions
     * @ToDo: For future use in Inferface -> Button to create a Template
     */

    public void createTemplateFile(String languageName) throws IOException {
        Path templatePath = translationsFolder.resolve(languageName.toLowerCase() + ".json");

        if (Files.exists(templatePath)) {
            plugin.getLogger().warning("Template file already exists for: " + languageName);
            return;
        }

        LanguageFile template = new LanguageFile();

        // Create metadata
        template.metadata = new LanguageMetadata();
        template.metadata.setLanguage(languageName);
        template.metadata.setNativeName("[Your Native Language Name]");
        template.metadata.setVersion("1.0.0");
        template.metadata.setAuthor("[Your Name]");
        template.metadata.setDescription("Community translation for " + languageName);
        template.metadata.setCountryCodes(Arrays.asList("XX")); // Placeholder

        // Get English translations as template
        template.translations = translationCache.get("english");
        if (template.translations == null) {
            template.translations = getDefaultTranslations("English");
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(templatePath.toFile()), StandardCharsets.UTF_8)) {
            gson.toJson(template, writer);
            plugin.getLogger().info("Created template file for: " + languageName);
        }
    }

    /**
     * Language File structure
     */
    private static class LanguageFile {
        LanguageMetadata metadata;
        Map<String, String> translations;
    }

    /**
     * Language Metadata class
     */
    public static class LanguageMetadata {
        private String language;
        private String nativeName;
        private String version;
        private String author;
        private String description;
        private List<String> countryCodes;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getNativeName() { return nativeName; }
        public void setNativeName(String nativeName) { this.nativeName = nativeName; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getCountryCodes() {
            return countryCodes != null ? countryCodes : new ArrayList<>();
        }
        public void setCountryCodes(List<String> countryCodes) {
            this.countryCodes = countryCodes;
        }
    }
}
