package com.magicbanana666.enderstorage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EnderStorageConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> PERSONAL_ITEM;
    private static final ModConfigSpec.ConfigValue<String> TEAM_ITEM;
    private static final ModConfigSpec.BooleanValue ANARCHY_MODE;
    private static final ModConfigSpec.BooleanValue VANILLA_SOUNDS;

    private static final ModConfigSpec.ConfigValue<String> SMALL_UPGRADE_ITEM;
    private static final ModConfigSpec.ConfigValue<String> LARGE_UPGRADE_ITEM;
    private static final ModConfigSpec.ConfigValue<String> PUMP_ITEM;

    private static final ModConfigSpec.IntValue CHEST_START_SLOTS;
    private static final ModConfigSpec.IntValue CHEST_MAX_SLOTS;
    private static final ModConfigSpec.IntValue CHEST_SMALL_UPGRADE;
    private static final ModConfigSpec.IntValue CHEST_LARGE_UPGRADE;

    private static final ModConfigSpec.IntValue TANK_BUCKETS;
    private static final ModConfigSpec.IntValue TANK_MAX_BUCKETS;
    private static final ModConfigSpec.IntValue TANK_SMALL_UPGRADE;
    private static final ModConfigSpec.IntValue TANK_LARGE_UPGRADE;

    private static final ModConfigSpec.IntValue PUMP_BASE_RATE;
    private static final ModConfigSpec.IntValue PUMP_UPGRADE_RATE;
    private static final ModConfigSpec.IntValue PUMP_MAX_UPGRADES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("access");
        PERSONAL_ITEM = builder.comment("The item that locks an ender chest or tank to its owner (right click the lock or valve with it). Use #namespace:tag for a tag.")
                .define("personalItem", "minecraft:diamond");
        TEAM_ITEM = builder.comment("The item that locks an ender chest or tank to your team (FTB Teams party, or /team). Use #namespace:tag for a tag.")
                .define("teamItem", "minecraft:emerald");
        ANARCHY_MODE = builder.comment("Personal and team chests and tanks lose their lock when broken, and anyone can open them.")
                .define("anarchyMode", false);
        VANILLA_SOUNDS = builder.comment("Use the vanilla ender chest sounds instead of the normal chest sounds.")
                .define("useVanillaEnderChestSounds", false);
        builder.pop();

        builder.push("upgrades");
        SMALL_UPGRADE_ITEM = builder.comment("Right click a chest or tank with this item for a small capacity upgrade. Use #namespace:tag for a tag.")
                .define("smallUpgradeItem", "#c:ender_pearls");
        LARGE_UPGRADE_ITEM = builder.comment("Right click a chest or tank with this item for a large capacity upgrade. Use #namespace:tag for a tag.")
                .define("largeUpgradeItem", "minecraft:ender_eye");
        PUMP_ITEM = builder.comment("Right click a tank with this item to make its valve pump faster. Use #namespace:tag for a tag.")
                .define("pumpItem", "minecraft:piston");
        builder.pop();

        builder.push("chest");
        CHEST_START_SLOTS = builder.comment("Slots of an ender chest frequency before upgrades.")
                .defineInRange("startSlots", 27, 1, 270);
        CHEST_MAX_SLOTS = builder.comment("Most slots an ender chest frequency can be upgraded to.")
                .defineInRange("maxSlots", 108, 1, 270);
        CHEST_SMALL_UPGRADE = builder.comment("Slots added by a small upgrade.")
                .defineInRange("smallUpgradeSlots", 3, 1, 270);
        CHEST_LARGE_UPGRADE = builder.comment("Slots added by a large upgrade.")
                .defineInRange("largeUpgradeSlots", 9, 1, 270);
        builder.pop();

        builder.push("tank");
        TANK_BUCKETS = builder.comment("Buckets an ender tank frequency holds before upgrades.")
                .defineInRange("tankBuckets", 16, 1, 4096);
        TANK_MAX_BUCKETS = builder.comment("Most buckets an ender tank frequency can be upgraded to.")
                .defineInRange("maxBuckets", 256, 1, 4096);
        TANK_SMALL_UPGRADE = builder.comment("Buckets added by a small upgrade.")
                .defineInRange("smallUpgradeBuckets", 4, 1, 4096);
        TANK_LARGE_UPGRADE = builder.comment("Buckets added by a large upgrade.")
                .defineInRange("largeUpgradeBuckets", 16, 1, 4096);
        PUMP_BASE_RATE = builder.comment("mB per tick an open valve pushes out without pump upgrades.")
                .defineInRange("pumpBaseRate", 250, 1, 100000);
        PUMP_UPGRADE_RATE = builder.comment("mB per tick added by each pump upgrade.")
                .defineInRange("pumpUpgradeRate", 1000, 1, 100000);
        PUMP_MAX_UPGRADES = builder.comment("How many pump upgrades a tank can have.")
                .defineInRange("pumpMaxUpgrades", 4, 0, 64);
        builder.pop();
        SPEC = builder.build();
    }

    private EnderStorageConfig() {
    }

    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    /** Whether the stack is the configured item, or in the configured tag when the value starts with #. */
    private static boolean matches(ItemStack stack, ModConfigSpec.ConfigValue<String> value, String fallback) {
        if (stack.isEmpty()) {
            return false;
        }
        String name = loaded() ? value.get() : fallback;
        if (name.startsWith("#")) {
            Identifier id = Identifier.tryParse(name.substring(1));
            return id != null && stack.is(TagKey.create(Registries.ITEM, id));
        }
        return stack.is(item(name, Items.AIR));
    }

    /** One item of the configured value, for giving a lock or upgrade item back. */
    private static ItemStack firstItem(ModConfigSpec.ConfigValue<String> value, String fallback, Item fallbackItem) {
        String name = loaded() ? value.get() : fallback;
        if (name.startsWith("#")) {
            Identifier id = Identifier.tryParse(name.substring(1));
            if (id != null) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id))) {
                    return new ItemStack(holder);
                }
            }
            return new ItemStack(fallbackItem);
        }
        return new ItemStack(item(name, fallbackItem));
    }

    private static Item item(String name, Item fallback) {
        Identifier id = Identifier.tryParse(name);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? fallback : item;
    }

    public static boolean isPersonalItem(ItemStack stack) {
        return matches(stack, PERSONAL_ITEM, "minecraft:diamond");
    }

    public static ItemStack personalItem() {
        return firstItem(PERSONAL_ITEM, "minecraft:diamond", Items.DIAMOND);
    }

    public static boolean isTeamItem(ItemStack stack) {
        return matches(stack, TEAM_ITEM, "minecraft:emerald");
    }

    public static ItemStack teamItem() {
        return firstItem(TEAM_ITEM, "minecraft:emerald", Items.EMERALD);
    }

    public static boolean isSmallUpgrade(ItemStack stack) {
        return matches(stack, SMALL_UPGRADE_ITEM, "#c:ender_pearls");
    }

    public static boolean isLargeUpgrade(ItemStack stack) {
        return matches(stack, LARGE_UPGRADE_ITEM, "minecraft:ender_eye");
    }

    public static boolean isPumpItem(ItemStack stack) {
        return matches(stack, PUMP_ITEM, "minecraft:piston");
    }

    public static ItemStack pumpItem() {
        return firstItem(PUMP_ITEM, "minecraft:piston", Items.PISTON);
    }

    public static boolean anarchyMode() {
        return loaded() && ANARCHY_MODE.get();
    }

    public static boolean vanillaSounds() {
        return loaded() && VANILLA_SOUNDS.get();
    }

    public static int chestStartSlots() {
        return Math.min(loaded() ? CHEST_START_SLOTS.get() : 27, chestMaxSlots());
    }

    public static int chestMaxSlots() {
        return loaded() ? CHEST_MAX_SLOTS.get() : 108;
    }

    /** Slots an upgrade item adds, or 0 when the stack is no upgrade. */
    public static int chestUpgradeSlots(ItemStack stack) {
        if (isLargeUpgrade(stack)) {
            return loaded() ? CHEST_LARGE_UPGRADE.get() : 9;
        }
        return isSmallUpgrade(stack) ? (loaded() ? CHEST_SMALL_UPGRADE.get() : 3) : 0;
    }

    /** Capacity of a tank without upgrades, in mB. */
    public static int tankCapacity() {
        return Math.min(loaded() ? TANK_BUCKETS.get() : 16, maxTankBuckets()) * 1000;
    }

    /** Most a tank can be upgraded to, in mB. */
    public static int tankMaxCapacity() {
        return maxTankBuckets() * 1000;
    }

    private static int maxTankBuckets() {
        return loaded() ? TANK_MAX_BUCKETS.get() : 256;
    }

    /** mB an upgrade item adds, or 0 when the stack is no upgrade. */
    public static int tankUpgradeCapacity(ItemStack stack) {
        if (isLargeUpgrade(stack)) {
            return 1000 * (loaded() ? TANK_LARGE_UPGRADE.get() : 16);
        }
        return isSmallUpgrade(stack) ? 1000 * (loaded() ? TANK_SMALL_UPGRADE.get() : 4) : 0;
    }

    /** mB per tick the valve pushes out with the given number of pump upgrades. */
    public static int pumpRate(int upgrades) {
        return (loaded() ? PUMP_BASE_RATE.get() : 250) + upgrades * (loaded() ? PUMP_UPGRADE_RATE.get() : 1000);
    }

    public static int pumpMaxUpgrades() {
        return loaded() ? PUMP_MAX_UPGRADES.get() : 4;
    }
}
