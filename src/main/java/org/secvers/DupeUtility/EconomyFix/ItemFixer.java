package org.secvers.DupeUtility.EconomyFix;

import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive item fixer that validates and corrects all illegal item modifications.
 * Checks every item against vanilla Minecraft limits.
 */
public class ItemFixer {

    private final JavaPlugin plugin;
    private final boolean verboseLogging;
    private final boolean logFixedItems;

    // Cache for legitimate max stack sizes
    private static final Map<Material, Integer> LEGITIMATE_STACK_SIZES = new HashMap<>();

    // Cache for legitimate max enchantment levels
    private static final Map<Enchantment, Integer> LEGITIMATE_ENCHANT_LEVELS = new HashMap<>();

    // Materials that should NEVER stack (tools, armor, special items)
    private static final Set<Material> NON_STACKABLE_ITEMS = new HashSet<>();

    // Materials that can be enchanted
    private static final Map<Material, Set<Enchantment>> ALLOWED_ENCHANTMENTS = new HashMap<>();
    private static final Set<Material> NON_SURVIVAL_ITEMS = new HashSet<>();
    static {
        initializeValidationData();
        initializeNonSurvivalItems();
    }

    public ItemFixer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.verboseLogging = plugin.getConfig().getBoolean("EconomyFix.VerboseLogging", false);
        this.logFixedItems = plugin.getConfig().getBoolean("EconomyFix.LogFixedItems", true);
    }

    /**
     * Initialize all validation data for items
     */
    private static void initializeValidationData() {
        // Initialize stack sizes for all materials
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;

            LEGITIMATE_STACK_SIZES.put(material, material.getMaxStackSize());

            // Mark non-stackable items (tools, armor, etc.)
            if (material.getMaxStackSize() == 1) {
                NON_STACKABLE_ITEMS.add(material);
            }
        }

        // Initialize enchantment max levels
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.PROTECTION, 4);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FIRE_PROTECTION, 4);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FEATHER_FALLING, 4);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.BLAST_PROTECTION, 4);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.PROJECTILE_PROTECTION, 4);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.RESPIRATION, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.AQUA_AFFINITY, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.THORNS, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.DEPTH_STRIDER, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FROST_WALKER, 2);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SOUL_SPEED, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SWIFT_SNEAK, 3);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SHARPNESS, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SMITE, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.BANE_OF_ARTHROPODS, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.KNOCKBACK, 2);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FIRE_ASPECT, 2);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.LOOTING, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SWEEPING_EDGE, 3);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.EFFICIENCY, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.SILK_TOUCH, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.UNBREAKING, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FORTUNE, 3);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.POWER, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.PUNCH, 2);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.FLAME, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.INFINITY, 1);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.LUCK_OF_THE_SEA, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.LURE, 3);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.LOYALTY, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.IMPALING, 5);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.RIPTIDE, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.CHANNELING, 1);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.MULTISHOT, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.QUICK_CHARGE, 3);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.PIERCING, 4);

        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.MENDING, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.VANISHING_CURSE, 1);
        LEGITIMATE_ENCHANT_LEVELS.put(Enchantment.BINDING_CURSE, 1);

        // Initialize allowed enchantments per material type
        initializeAllowedEnchantments();
    }

    private static void initializeNonSurvivalItems() {
        // Command blocks
        NON_SURVIVAL_ITEMS.add(Material.COMMAND_BLOCK);
        NON_SURVIVAL_ITEMS.add(Material.CHAIN_COMMAND_BLOCK);
        NON_SURVIVAL_ITEMS.add(Material.REPEATING_COMMAND_BLOCK);
        NON_SURVIVAL_ITEMS.add(Material.COMMAND_BLOCK_MINECART);

        // Structure blocks and related
        NON_SURVIVAL_ITEMS.add(Material.STRUCTURE_BLOCK);
        NON_SURVIVAL_ITEMS.add(Material.STRUCTURE_VOID);
        NON_SURVIVAL_ITEMS.add(Material.JIGSAW);

        // Barrier and light
        NON_SURVIVAL_ITEMS.add(Material.BARRIER);
        NON_SURVIVAL_ITEMS.add(Material.LIGHT);

        // Bedrock
        NON_SURVIVAL_ITEMS.add(Material.BEDROCK);

        // Spawn eggs (all variants)
        for (Material material : Material.values()) {
            if (material.name().endsWith("_SPAWN_EGG")) {
                NON_SURVIVAL_ITEMS.add(material);
            }
        }

        // End portal frame
        NON_SURVIVAL_ITEMS.add(Material.END_PORTAL_FRAME);
        NON_SURVIVAL_ITEMS.add(Material.END_PORTAL);

        // Nether portal
        NON_SURVIVAL_ITEMS.add(Material.NETHER_PORTAL);

        // End gateway
        NON_SURVIVAL_ITEMS.add(Material.END_GATEWAY);

        // Frosted ice
        NON_SURVIVAL_ITEMS.add(Material.FROSTED_ICE);

        // Budding amethyst
        NON_SURVIVAL_ITEMS.add(Material.BUDDING_AMETHYST);

        // Reinforced deepslate
        NON_SURVIVAL_ITEMS.add(Material.REINFORCED_DEEPSLATE);

        // Petrified oak slab
        NON_SURVIVAL_ITEMS.add(Material.PETRIFIED_OAK_SLAB);

        // Infested blocks (silverfish blocks) - technically obtainable but controversial
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_STONE);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_COBBLESTONE);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_STONE_BRICKS);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_MOSSY_STONE_BRICKS);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_CRACKED_STONE_BRICKS);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_CHISELED_STONE_BRICKS);
        NON_SURVIVAL_ITEMS.add(Material.INFESTED_DEEPSLATE);

        // Farmland (can be created but not picked up)
        NON_SURVIVAL_ITEMS.add(Material.FARMLAND);

        // Piston head and moving piston
        NON_SURVIVAL_ITEMS.add(Material.PISTON_HEAD);
        NON_SURVIVAL_ITEMS.add(Material.MOVING_PISTON);

        // Fire
        NON_SURVIVAL_ITEMS.add(Material.FIRE);
        NON_SURVIVAL_ITEMS.add(Material.SOUL_FIRE);

        // Water and lava (as items)
        NON_SURVIVAL_ITEMS.add(Material.WATER);
        NON_SURVIVAL_ITEMS.add(Material.LAVA);
        NON_SURVIVAL_ITEMS.add(Material.BUBBLE_COLUMN);

        // Powder snow cauldrons with levels
        NON_SURVIVAL_ITEMS.add(Material.POWDER_SNOW_CAULDRON);
        NON_SURVIVAL_ITEMS.add(Material.WATER_CAULDRON);
        NON_SURVIVAL_ITEMS.add(Material.LAVA_CAULDRON);

        // Potted plants (the pot with plant, not separate items)
        for (Material material : Material.values()) {
            if (material.name().startsWith("POTTED_")) {
                NON_SURVIVAL_ITEMS.add(material);
            }
        }

        // Technical blocks
        NON_SURVIVAL_ITEMS.add(Material.SPAWNER); // Spawners can't be obtained with Silk Touch legitimately
        NON_SURVIVAL_ITEMS.add(Material.PLAYER_HEAD); // Player heads are not survival obtainable
        NON_SURVIVAL_ITEMS.add(Material.PLAYER_WALL_HEAD);

        // Debug stick
        NON_SURVIVAL_ITEMS.add(Material.DEBUG_STICK);

        // Knowledge book
        NON_SURVIVAL_ITEMS.add(Material.KNOWLEDGE_BOOK);


        // Air blocks
        NON_SURVIVAL_ITEMS.add(Material.AIR);
        NON_SURVIVAL_ITEMS.add(Material.CAVE_AIR);
        NON_SURVIVAL_ITEMS.add(Material.VOID_AIR);

        // Lit furnaces/smokers/blast furnaces
        NON_SURVIVAL_ITEMS.add(Material.FURNACE); // Only when lit, but we'll check state
        // Note: The lit state is handled by block data, not material

        // Wall-mounted variants of certain blocks
        for (Material material : Material.values()) {
            String name = material.name();
            // Wall signs, wall torches, wall banners, etc. are obtainable,
            // but some wall variants might not be
            if (name.contains("_WALL_") && !name.endsWith("_SIGN") &&
                    !name.endsWith("_BANNER") && !name.equals("COBBLESTONE_WALL") &&
                    !name.contains("_WALL_TORCH") && !name.contains("_WALL_FAN")) {
                // Most wall variants ARE obtainable, so be careful here
            }
        }

        // Attached stems
        NON_SURVIVAL_ITEMS.add(Material.ATTACHED_MELON_STEM);
        NON_SURVIVAL_ITEMS.add(Material.ATTACHED_PUMPKIN_STEM);

        // Carrots, potatoes, beetroots as blocks (not items)
        NON_SURVIVAL_ITEMS.add(Material.CARROTS);
        NON_SURVIVAL_ITEMS.add(Material.POTATOES);
        NON_SURVIVAL_ITEMS.add(Material.BEETROOTS);

        // Cocoa (the block state, not the beans)
        NON_SURVIVAL_ITEMS.add(Material.COCOA);

        // Tall seagrass (top part)
        NON_SURVIVAL_ITEMS.add(Material.TALL_SEAGRASS);

        // Bamboo sapling (the small state)
        NON_SURVIVAL_ITEMS.add(Material.BAMBOO_SAPLING);

        // Sweet berry bush
        NON_SURVIVAL_ITEMS.add(Material.SWEET_BERRY_BUSH);

        // Kelp plant (different from kelp item)
        NON_SURVIVAL_ITEMS.add(Material.KELP_PLANT);

        // Big dripleaf stem
        NON_SURVIVAL_ITEMS.add(Material.BIG_DRIPLEAF_STEM);

        // Powder snow (as block, powder snow bucket IS obtainable)
        NON_SURVIVAL_ITEMS.add(Material.POWDER_SNOW);

        // Redstone wire (redstone dust IS obtainable)
        NON_SURVIVAL_ITEMS.add(Material.REDSTONE_WIRE);

        // Tripwire (string and tripwire hook ARE obtainable)
        NON_SURVIVAL_ITEMS.add(Material.TRIPWIRE);
        NON_SURVIVAL_ITEMS.add(Material.SPAWNER);
        NON_SURVIVAL_ITEMS.add(Material.END_PORTAL_FRAME);

        NON_SURVIVAL_ITEMS.add(Material.SPIDER_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.STRIPPED_PALE_OAK_WOOD);
        NON_SURVIVAL_ITEMS.add(Material.ALLAY_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.BEE_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.ENDERMAN_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.ENDER_DRAGON_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.AXOLOTL_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.ARMADILLO_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.BAT_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.BLAZE_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.CAVE_SPIDER_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.COD_SPAWN_EGG);
        NON_SURVIVAL_ITEMS.add(Material.WITHER_SPAWN_EGG);

        // Cake with candle variants

        for (Material material : Material.values()) {
            if (material.name().startsWith("CANDLE_CAKE")) {
                NON_SURVIVAL_ITEMS.add(material);
            }
        }
    }


    /**
     * Initialize which enchantments are allowed on which materials
     */
    private static void initializeAllowedEnchantments() {
        // Armor enchantments
        Set<Enchantment> helmetEnchants = new HashSet<>(Arrays.asList(
                Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION,
                Enchantment.PROJECTILE_PROTECTION, Enchantment.RESPIRATION, Enchantment.AQUA_AFFINITY,
                Enchantment.THORNS, Enchantment.UNBREAKING, Enchantment.MENDING,
                Enchantment.VANISHING_CURSE, Enchantment.BINDING_CURSE
        ));

        Set<Enchantment> chestplateEnchants = new HashSet<>(Arrays.asList(
                Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION,
                Enchantment.PROJECTILE_PROTECTION, Enchantment.THORNS, Enchantment.UNBREAKING,
                Enchantment.MENDING, Enchantment.VANISHING_CURSE, Enchantment.BINDING_CURSE
        ));

        Set<Enchantment> leggingsEnchants = new HashSet<>(Arrays.asList(
                Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION,
                Enchantment.PROJECTILE_PROTECTION, Enchantment.THORNS, Enchantment.UNBREAKING,
                Enchantment.MENDING, Enchantment.VANISHING_CURSE, Enchantment.BINDING_CURSE,
                Enchantment.SWIFT_SNEAK
        ));

        Set<Enchantment> bootsEnchants = new HashSet<>(Arrays.asList(
                Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION,
                Enchantment.PROJECTILE_PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.DEPTH_STRIDER,
                Enchantment.FROST_WALKER, Enchantment.SOUL_SPEED, Enchantment.THORNS,
                Enchantment.UNBREAKING, Enchantment.MENDING, Enchantment.VANISHING_CURSE,
                Enchantment.BINDING_CURSE
        ));

        // Apply to all armor types
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_HELMET")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(helmetEnchants));
            } else if (name.endsWith("_CHESTPLATE")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(chestplateEnchants));
            } else if (name.endsWith("_LEGGINGS")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(leggingsEnchants));
            } else if (name.endsWith("_BOOTS")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(bootsEnchants));
            }
        }

        // Sword enchantments
        Set<Enchantment> swordEnchants = new HashSet<>(Arrays.asList(
                Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
                Enchantment.KNOCKBACK, Enchantment.FIRE_ASPECT, Enchantment.LOOTING,
                Enchantment.SWEEPING_EDGE, Enchantment.UNBREAKING, Enchantment.MENDING,
                Enchantment.VANISHING_CURSE
        ));

        for (Material material : Material.values()) {
            if (material.name().endsWith("_SWORD")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(swordEnchants));
            }
        }

        // Tool enchantments
        Set<Enchantment> toolEnchants = new HashSet<>(Arrays.asList(
                Enchantment.EFFICIENCY, Enchantment.SILK_TOUCH, Enchantment.UNBREAKING,
                Enchantment.FORTUNE, Enchantment.MENDING, Enchantment.VANISHING_CURSE
        ));

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") ||
                    name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {
                ALLOWED_ENCHANTMENTS.put(material, new HashSet<>(toolEnchants));
            }
        }

        // Bow enchantments
        Set<Enchantment> bowEnchants = new HashSet<>(Arrays.asList(
                Enchantment.POWER, Enchantment.PUNCH, Enchantment.FLAME,
                Enchantment.INFINITY, Enchantment.UNBREAKING, Enchantment.MENDING,
                Enchantment.VANISHING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.BOW, bowEnchants);

        // Crossbow enchantments
        Set<Enchantment> crossbowEnchants = new HashSet<>(Arrays.asList(
                Enchantment.MULTISHOT, Enchantment.QUICK_CHARGE, Enchantment.PIERCING,
                Enchantment.UNBREAKING, Enchantment.MENDING, Enchantment.VANISHING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.CROSSBOW, crossbowEnchants);

        // Trident enchantments
        Set<Enchantment> tridentEnchants = new HashSet<>(Arrays.asList(
                Enchantment.LOYALTY, Enchantment.IMPALING, Enchantment.RIPTIDE,
                Enchantment.CHANNELING, Enchantment.UNBREAKING, Enchantment.MENDING,
                Enchantment.VANISHING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.TRIDENT, tridentEnchants);

        // Fishing rod enchantments
        Set<Enchantment> fishingRodEnchants = new HashSet<>(Arrays.asList(
                Enchantment.LUCK_OF_THE_SEA, Enchantment.LURE, Enchantment.UNBREAKING,
                Enchantment.MENDING, Enchantment.VANISHING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.FISHING_ROD, fishingRodEnchants);

        // Elytra enchantments
        Set<Enchantment> elytraEnchants = new HashSet<>(Arrays.asList(
                Enchantment.UNBREAKING, Enchantment.MENDING, Enchantment.VANISHING_CURSE,
                Enchantment.BINDING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.ELYTRA, elytraEnchants);

        // Shield enchantments
        Set<Enchantment> shieldEnchants = new HashSet<>(Arrays.asList(
                Enchantment.UNBREAKING, Enchantment.MENDING, Enchantment.VANISHING_CURSE
        ));
        ALLOWED_ENCHANTMENTS.put(Material.SHIELD, shieldEnchants);
    }

    /**
     * Fix all items in an inventory
     *
     * @param inventory Inventory to fix
     * @return Number of items fixed
     */
    public int fixInventory(Inventory inventory) {
        if (inventory == null) return 0;

        int fixedCount = 0;
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            FixResult result = fixItem(item);

            if (result.wasFixed) {
                if (result.replacementItem != null) {
                    inventory.setItem(i, result.replacementItem);
                } else {
                    inventory.setItem(i, null);
                }
                fixedCount++;

                if (logFixedItems) {
                    logItemFix(item, result);
                }
            }
        }

        return fixedCount;
    }

    private boolean validateSurvivalObtainable(ItemStack item, FixResult result) {
        Material material = item.getType();

        if (NON_SURVIVAL_ITEMS.contains(material)) {
            result.addViolation(String.format(
                    "Non-survival obtainable item: %s",
                    material.name()
            ));

            // Remove the item completely (set to null)
            result.replacementItem = null;
            result.wasFixed = true;

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Removed non-survival item: %s",
                        material.name()
                ));
            }

            return false;
        }

        return true;
    }

    /**
     * Fix a single item and return the result
     *
     * @param item Item to fix
     * @return FixResult containing the fixed item or replacement
     */
    public FixResult fixItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return FixResult.noChange();
        }

        FixResult result = new FixResult();
        Material material = item.getType();

        // Check 1: Validate stack size for ALL items
        if (!validateStackSize(item, result)) {
            return result;
        }
        // Check 2: Check NBT Data Stackable
        if(!validateNBTStackable(item, result)) {
            return result;
        }

        if (!validateSurvivalObtainable(item, result)) {
            return result;
        }

        // Check 2: Ensure non-stackable items are not stacked
        if (!validateNonStackable(item, result)) {
            return result;
        }

        // Check 3: Remove illegal Unbreakable tag
        if (!validateUnbreakable(item, result)) {
            return result;
        }

        // Check 4: Validate and fix enchantments
        if (!validateEnchantments(item, result)) {
            return result;
        }

        // Check 5: Check nested containers (Shulker Boxes, Bundles)
        if (!validateNestedContainers(item, result)) {
            return result;
        }

        // Check 6: Validate damage/durability
        if (!validateDurability(item, result)) {
            return result;
        }

        // Check 7: Validate NBT data size (prevent lag)
        if (!validateNBTSize(item, result)) {
            return result;
        }

        return result;
    }

    /**
     * Validate stack size against legitimate maximum
     */
    private boolean validateStackSize(ItemStack item, FixResult result) {
        Material material = item.getType();
        int currentAmount = item.getAmount();
        int maxStackSize = LEGITIMATE_STACK_SIZES.getOrDefault(material, 64);


        if (currentAmount > maxStackSize) {
            result.addViolation(String.format(
                    "Illegal stack size: %s x%d (max: %d)",
                    material.name(), currentAmount, maxStackSize
            ));
            item.setAmount(maxStackSize);

            result.replacementItem = item;
            result.wasFixed = true;

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Fixed illegal stack: %s reduced from %d to %d",
                        material.name(), currentAmount, maxStackSize
                ));
            }
        }

        return true;
    }

    /**
     * Validate that non-stackable items (armor, tools) are not stacked
     */
    private boolean validateNonStackable(ItemStack item, FixResult result) {
        Material material = item.getType();

        if (NON_STACKABLE_ITEMS.contains(material) && item.getAmount() > 1) {
            result.addViolation(String.format(
                    "Illegally stacked non-stackable item: %s x%d",
                    material.name(), item.getAmount()
            ));

            item.setAmount(1);
            result.replacementItem = item;
            result.wasFixed = true;

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Fixed stacked non-stackable: %s",
                        material.name()
                ));
            }
        }

        return true;
    }


    /**
     * Validate that non-stackable items (armor, tools) are not stacked
     */
    private boolean validateNBTStackable(ItemStack item, FixResult result) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        Material material = item.getType();
        int legitimateMax = LEGITIMATE_STACK_SIZES.getOrDefault(material, material.getMaxStackSize());

        // ========== MAX STACK SIZE ==========
        if (meta.hasMaxStackSize()) {
            int metaMaxStack = meta.getMaxStackSize();

            if (metaMaxStack > legitimateMax) {
                meta.setMaxStackSize(legitimateMax);
                result.addViolation(String.format(
                        "Removed illegal max_stack_size: %d -> %d for %s",
                        metaMaxStack, legitimateMax, material.name()
                ));

            }
        }

        // ========== MAX DAMAGE (DURABILITY) ==========
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;

            if (damageable.hasMaxDamage()) {
                int metaMaxDamage = damageable.getMaxDamage();
                int vanillaMaxDamage = material.getMaxDurability();

                if (vanillaMaxDamage > 0 && metaMaxDamage != vanillaMaxDamage) {
                    damageable.setMaxDamage(vanillaMaxDamage);
                    result.addViolation(String.format(
                            "Fixed illegal max_damage: %d -> %d for %s",
                            metaMaxDamage, vanillaMaxDamage, material.name()
                    ));

                }
            }

            // Also check current damage value
            if (damageable.hasDamage()) {
                int damage = damageable.getDamage();
                int maxDamage = damageable.hasMaxDamage() ? damageable.getMaxDamage() : material.getMaxDurability();

                if (damage > maxDamage) {
                    damageable.setDamage(maxDamage);
                    result.addViolation(String.format(
                            "Fixed illegal damage: %d -> %d for %s",
                            damage, maxDamage, material.name()
                    ));

                }
            }
        }

        return true;
    }

    /**
     * Remove illegal Unbreakable tag
     */
    private boolean validateUnbreakable(ItemStack item, FixResult result) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        if (meta.isUnbreakable()) {
            result.addViolation("Illegal Unbreakable tag");

            meta.setUnbreakable(false);
            item.setItemMeta(meta);
            result.replacementItem = item;
            result.wasFixed = true;

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Removed Unbreakable from %s",
                        item.getType().name()
                ));
            }
        }

        return true;
    }

    /**
     * Validate and fix enchantments
     */
    private boolean validateEnchantments(ItemStack item, FixResult result) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants()) return true;

        Material material = item.getType();
        Map<Enchantment, Integer> enchants = meta.getEnchants();
        Set<Enchantment> allowedEnchants = ALLOWED_ENCHANTMENTS.get(material);

        boolean modified = false;
        List<Enchantment> toRemove = new ArrayList<>();

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();

            // Check if enchantment is allowed on this item
            if (allowedEnchants == null || !allowedEnchants.contains(enchant)) {
                result.addViolation(String.format(
                        "Illegal enchantment: %s on %s",
                        enchant.getKey().getKey(), material.name()
                ));
                toRemove.add(enchant);
                modified = true;
                continue;
            }

            // Check if level exceeds legitimate maximum
            int maxLevel = LEGITIMATE_ENCHANT_LEVELS.getOrDefault(enchant, 1);
            if (level > maxLevel) {
                result.addViolation(String.format(
                        "Illegal enchantment level: %s %d (max: %d)",
                        enchant.getKey().getKey(), level, maxLevel
                ));

                // Reduce to maximum
                meta.removeEnchant(enchant);
                meta.addEnchant(enchant, maxLevel, true);
                modified = true;

                if (verboseLogging) {
                    plugin.getLogger().warning(String.format(
                            "Reduced %s from %d to %d on %s",
                            enchant.getKey().getKey(), level, maxLevel, material.name()
                    ));
                }
            }
        }

        // Remove illegal enchantments
        for (Enchantment enchant : toRemove) {
            meta.removeEnchant(enchant);

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Removed illegal enchantment %s from %s",
                        enchant.getKey().getKey(), material.name()
                ));
            }
        }

        if (modified) {
            item.setItemMeta(meta);
            result.replacementItem = item;
            result.wasFixed = true;
        }

        return true;
    }

    /**
     * Validate nested containers (Shulker Boxes, Bundles)
     */
    private boolean validateNestedContainers(ItemStack item, FixResult result) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        // Check Shulker Boxes
        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                int fixedInside = fixInventory(shulkerBox.getInventory());

                if (fixedInside > 0) {
                    blockStateMeta.setBlockState(shulkerBox);
                    item.setItemMeta(blockStateMeta);
                    result.replacementItem = item;
                    result.wasFixed = true;
                    result.addViolation(String.format(
                            "Fixed %d items inside Shulker Box",
                            fixedInside
                    ));
                }
            }
        }

        // Check Bundles
        if (meta instanceof BundleMeta bundleMeta) {
            List<ItemStack> bundleItems = bundleMeta.getItems();
            boolean bundleModified = false;

            for (int i = 0; i < bundleItems.size(); i++) {
                ItemStack bundleItem = bundleItems.get(i);
                FixResult bundleResult = fixItem(bundleItem);

                if (bundleResult.wasFixed) {
                    if (bundleResult.replacementItem != null) {
                        bundleItems.set(i, bundleResult.replacementItem);
                    } else {
                        bundleItems.remove(i);
                        i--;
                    }
                    bundleModified = true;
                }
            }

            if (bundleModified) {
                bundleMeta.setItems(bundleItems);
                item.setItemMeta(bundleMeta);
                result.replacementItem = item;
                result.wasFixed = true;
                result.addViolation("Fixed items inside Bundle");
            }
        }

        return true;
    }

    /**
     * Validate durability/damage values
     */
    private boolean validateDurability(ItemStack item, FixResult result) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return true;

        int damage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();

        // Check for negative damage (infinite durability hack)
        if (damage < 0) {
            result.addViolation("Negative damage value (durability hack)");
            damageable.setDamage(0);
            item.setItemMeta((ItemMeta) damageable);
            result.replacementItem = item;
            result.wasFixed = true;

            if (verboseLogging) {
                plugin.getLogger().warning(String.format(
                        "Fixed negative damage on %s",
                        item.getType().name()
                ));
            }
        }

        // Check for damage exceeding max durability
        if (maxDurability > 0 && damage > maxDurability) {
            result.addViolation(String.format(
                    "Damage exceeds max durability: %d > %d",
                    damage, maxDurability
            ));

            // Replace with egg (item is broken beyond repair)
            result.replacementItem = new ItemStack(Material.EGG);
            result.wasFixed = true;
        }

        return true;
    }

    /**
     * Validate NBT data size to prevent lag items
     */
    private boolean validateNBTSize(ItemStack item, FixResult result) {
        // Estimate NBT size by serializing
        try {
            // Check lore size
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    int totalLoreLength = lore.stream()
                            .mapToInt(String::length)
                            .sum();

                    // Max 10KB of lore
                    if (totalLoreLength > 10000) {
                        result.addViolation("Excessive lore size (lag item)");
                        result.replacementItem = new ItemStack(Material.EGG);
                        result.wasFixed = true;

                        if (verboseLogging) {
                            plugin.getLogger().warning(String.format(
                                    "Removed lag item with %d chars of lore",
                                    totalLoreLength
                            ));
                        }
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            // If we can't check, better safe than sorry
            result.addViolation("Corrupted NBT data");
            result.replacementItem = new ItemStack(Material.EGG);
            result.wasFixed = true;
            return false;
        }

        return true;
    }

    /**
     * Log fixed item details
     */
    private void logItemFix(ItemStack original, FixResult result) {
        plugin.getLogger().info(String.format(
                "Fixed item: %s | Violations: %s",
                original.getType().name(),
                String.join(", ", result.violations)
        ));
    }

    /**
     * Result of fixing an item
     */
    public static class FixResult {
        public boolean wasFixed = false;
        public ItemStack replacementItem = null;
        public List<String> violations = new ArrayList<>();

        public void addViolation(String violation) {
            violations.add(violation);
        }

        public static FixResult noChange() {
            return new FixResult();
        }
    }
}
