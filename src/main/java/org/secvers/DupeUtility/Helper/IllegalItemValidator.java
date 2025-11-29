package org.secvers.DupeUtility.Helper;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

public class IllegalItemValidator {
    private final Plugin plugin;

    // Configuration cache
    private boolean checkUnbreakable;
    private boolean checkOverstacked;
    private boolean checkIllegalEnchantments;
    private boolean checkUnobtainableItems;
    private boolean allowCustomNames;
    private boolean allowCustomLore;
    private boolean autoFixEnchantments;
    private boolean logIllegalItems;
    private String action;
    private Set<Material> whitelist;

    // Materials that cannot be obtained in survival
    private static final Set<Material> UNOBTAINABLE_MATERIALS = new HashSet<>(Arrays.asList(
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.LIGHT,
            Material.BEDROCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.SPAWNER,
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK
    ));

    // Maximum vanilla enchantment levels
    private static final Map<Enchantment, Integer> MAX_ENCHANTMENT_LEVELS = new HashMap<>();

    static {
        // Initialize max enchantment levels
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.PROTECTION, 4);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.FIRE_PROTECTION, 4);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.BLAST_PROTECTION, 4);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.PROJECTILE_PROTECTION, 4);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.RESPIRATION, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.AQUA_AFFINITY, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.THORNS, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.DEPTH_STRIDER, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.FROST_WALKER, 2);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SOUL_SPEED, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SWIFT_SNEAK, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SHARPNESS, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SMITE, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.BANE_OF_ARTHROPODS, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.KNOCKBACK, 2);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.FIRE_ASPECT, 2);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.LOOTING, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SWEEPING_EDGE, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.EFFICIENCY, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.SILK_TOUCH, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.UNBREAKING, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.FORTUNE, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.POWER, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.PUNCH, 2);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.FLAME, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.INFINITY, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.LUCK_OF_THE_SEA, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.LURE, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.LOYALTY, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.IMPALING, 5);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.RIPTIDE, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.CHANNELING, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.MULTISHOT, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.QUICK_CHARGE, 3);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.PIERCING, 4);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.MENDING, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.VANISHING_CURSE, 1);
        MAX_ENCHANTMENT_LEVELS.put(Enchantment.BINDING_CURSE, 1);
    }

    public IllegalItemValidator(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // Load configuration
        checkUnbreakable = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.CheckUnbreakable", true);
        checkOverstacked = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.CheckOverstacked", true);
        checkIllegalEnchantments = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.CheckIllegalEnchantments", true);
        checkUnobtainableItems = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.CheckUnobtainableItems", true);
        allowCustomNames = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.AllowCustomNames", true);
        allowCustomLore = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.AllowCustomLore", true);
        autoFixEnchantments = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.AutoFixEnchantments", true);
        logIllegalItems = plugin.getConfig().getBoolean("Settings.IllegalItemDetection.LogIllegalItems", true);
        action = plugin.getConfig().getString("Settings.IllegalItemDetection.Action", "REMOVE").toUpperCase();

        // Load whitelist
        whitelist = new HashSet<>();
        List<String> whitelistConfig = plugin.getConfig().getStringList("Settings.IllegalItemDetection.Whitelist");
        for (String materialName : whitelistConfig) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                whitelist.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in whitelist: " + materialName);
            }
        }
    }

    public ItemStack validateAndClean(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        // Check if item is whitelisted
        if (whitelist.contains(item.getType())) {
            return item;
        }

        List<String> violations = new ArrayList<>();

        // Check for unobtainable items
        if (checkUnobtainableItems && isUnobtainable(item)) {
            violations.add("Unobtainable material: " + item.getType());
            if (shouldRemove()) {
                logViolation(item, violations);
                return null; // Return null to replace with AIR
            }
        }

        // Check for overstacked items
        if (checkOverstacked && isOverstacked(item)) {
            violations.add("Overstacked: " + item.getAmount() + "/" + item.getMaxStackSize());
            if (shouldRemove()) {
                logViolation(item, violations);
                return null;
            } else if ("RESET".equals(action)) {
                item.setAmount(item.getMaxStackSize());
            }
        }

        if (!item.hasItemMeta()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        boolean modified = false;

        // Check for unbreakable
        if (checkUnbreakable && meta.isUnbreakable()) {
            violations.add("Unbreakable tag present");
            if (shouldRemove()) {
                logViolation(item, violations);
                return null;
            } else if ("RESET".equals(action)) {
                meta.setUnbreakable(false);
                modified = true;
            }
        }

        // Check for illegal enchantments
        if (checkIllegalEnchantments && meta.hasEnchants()) {
            Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
            boolean hasIllegalEnchant = false;

            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();

                // Check if enchantment can be applied to this item
                if (!enchant.canEnchantItem(item)) {
                    violations.add("Incompatible enchantment: " + enchant.getKey().getKey() + " on " + item.getType());
                    hasIllegalEnchant = true;

                    if (autoFixEnchantments) {
                        meta.removeEnchant(enchant);
                        modified = true;
                    }
                    continue;
                }

                // Check enchantment level
                Integer maxLevel = MAX_ENCHANTMENT_LEVELS.get(enchant);
                if (maxLevel != null && level > maxLevel) {
                    violations.add("Illegal enchantment level: " + enchant.getKey().getKey() + " " + level + " (max: " + maxLevel + ")");
                    hasIllegalEnchant = true;

                    if (autoFixEnchantments) {
                        meta.removeEnchant(enchant);
                        meta.addEnchant(enchant, maxLevel, true);
                        modified = true;
                    }
                }
            }

            if (hasIllegalEnchant && !autoFixEnchantments && shouldRemove()) {
                logViolation(item, violations);
                return null;
            }
        }

        // Check for conflicting enchantments
        if (checkIllegalEnchantments && meta.hasEnchants()) {
            if (hasConflictingEnchantments(meta)) {
                violations.add("Conflicting enchantments detected");
                if (shouldRemove() && !autoFixEnchantments) {
                    logViolation(item, violations);
                    return null;
                } else if (autoFixEnchantments) {
                    removeConflictingEnchantments(meta);
                    modified = true;
                }
            }
        }

        // Apply modifications if any
        if (modified) {
            item.setItemMeta(meta);
            logViolation(item, violations, true);
        } else if (!violations.isEmpty() && "LOG_ONLY".equals(action)) {
            logViolation(item, violations);
        }

        return item;
    }

    private boolean isUnobtainable(ItemStack item) {
        return UNOBTAINABLE_MATERIALS.contains(item.getType());
    }

    private boolean isOverstacked(ItemStack item) {
        return item.getAmount() > item.getMaxStackSize();
    }

    private boolean hasConflictingEnchantments(ItemMeta meta) {
        Map<Enchantment, Integer> enchants = meta.getEnchants();

        // Check for common conflicts
        if (enchants.containsKey(Enchantment.SILK_TOUCH) && enchants.containsKey(Enchantment.FORTUNE)) {
            return true;
        }
        if (enchants.containsKey(Enchantment.INFINITY) && enchants.containsKey(Enchantment.MENDING)) {
            return true;
        }
        if (enchants.containsKey(Enchantment.RIPTIDE) &&
                (enchants.containsKey(Enchantment.LOYALTY) || enchants.containsKey(Enchantment.CHANNELING))) {
            return true;
        }

        // Check for protection conflicts
        long protectionCount = enchants.keySet().stream()
                .filter(e -> e.equals(Enchantment.PROTECTION) ||
                        e.equals(Enchantment.FIRE_PROTECTION) ||
                        e.equals(Enchantment.BLAST_PROTECTION) ||
                        e.equals(Enchantment.PROJECTILE_PROTECTION))
                .count();

        if (protectionCount > 1) {
            return true;
        }

        // Check for damage conflicts
        long damageCount = enchants.keySet().stream()
                .filter(e -> e.equals(Enchantment.SHARPNESS) ||
                        e.equals(Enchantment.SMITE) ||
                        e.equals(Enchantment.BANE_OF_ARTHROPODS))
                .count();

        return damageCount > 1;
    }

    private void removeConflictingEnchantments(ItemMeta meta) {
        Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());

        // Remove Silk Touch if Fortune is present (keep Fortune)
        if (enchants.containsKey(Enchantment.SILK_TOUCH) && enchants.containsKey(Enchantment.FORTUNE)) {
            meta.removeEnchant(Enchantment.SILK_TOUCH);
        }

        // Remove Infinity if Mending is present (keep Mending)
        if (enchants.containsKey(Enchantment.INFINITY) && enchants.containsKey(Enchantment.MENDING)) {
            meta.removeEnchant(Enchantment.INFINITY);
        }

        // Remove conflicting trident enchantments (keep Riptide)
        if (enchants.containsKey(Enchantment.RIPTIDE)) {
            meta.removeEnchant(Enchantment.LOYALTY);
            meta.removeEnchant(Enchantment.CHANNELING);
        }

        // Keep only the highest level protection enchantment
        List<Enchantment> protections = Arrays.asList(
                Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION,
                Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION
        );

        Enchantment highestProtection = null;
        int highestProtectionLevel = 0;

        for (Enchantment prot : protections) {
            if (enchants.containsKey(prot) && enchants.get(prot) > highestProtectionLevel) {
                highestProtection = prot;
                highestProtectionLevel = enchants.get(prot);
            }
        }

        for (Enchantment prot : protections) {
            if (prot != highestProtection) {
                meta.removeEnchant(prot);
            }
        }

        // Keep only the highest damage enchantment
        List<Enchantment> damageEnchants = Arrays.asList(
                Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS
        );

        Enchantment highestDamage = null;
        int highestDamageLevel = 0;

        for (Enchantment dmg : damageEnchants) {
            if (enchants.containsKey(dmg) && enchants.get(dmg) > highestDamageLevel) {
                highestDamage = dmg;
                highestDamageLevel = enchants.get(dmg);
            }
        }

        for (Enchantment dmg : damageEnchants) {
            if (dmg != highestDamage) {
                meta.removeEnchant(dmg);
            }
        }
    }

    private boolean shouldRemove() {
        return "REMOVE".equals(action);
    }

    private void logViolation(ItemStack item, List<String> violations) {
        logViolation(item, violations, false);
    }

    private void logViolation(ItemStack item, List<String> violations, boolean fixed) {
        if (!logIllegalItems || violations.isEmpty()) return;

        String status = fixed ? "FIXED" : (shouldRemove() ? "REMOVED" : "DETECTED");
        StringBuilder log = new StringBuilder();
        log.append("[IllegalItem ").append(status).append("] ");
        log.append(item.getType()).append(" x").append(item.getAmount());

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            log.append(" (").append(item.getItemMeta().getDisplayName()).append(")");
        }

        log.append(" - Violations: ").append(String.join(", ", violations));

        plugin.getLogger().log(Level.WARNING, log.toString());
    }
}
