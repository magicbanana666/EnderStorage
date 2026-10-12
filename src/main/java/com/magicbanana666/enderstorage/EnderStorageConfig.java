package com.magicbanana666.enderstorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EnderStorageConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> PERSONAL_ITEM;
    private static final ModConfigSpec.BooleanValue ANARCHY_MODE;
    private static final ModConfigSpec.IntValue STORAGE_SIZE;
    private static final ModConfigSpec.IntValue TANK_BUCKETS;
    private static final ModConfigSpec.BooleanValue VANILLA_SOUNDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        PERSONAL_ITEM = builder.comment("The item that locks an ender chest or tank to its owner (right click the lock or valve with it).")
                .define("personalItem", "minecraft:diamond");
        ANARCHY_MODE = builder.comment("Personal chests and tanks lose their owner when broken and drop the personal item, and anyone can open them.")
                .define("anarchyMode", false);
        STORAGE_SIZE = builder.comment("The size of each ender chest inventory: 0 = 3x3, 1 = 3x9, 2 = 6x9.")
                .defineInRange("storageSize", 1, 0, 2);
        TANK_BUCKETS = builder.comment("How many buckets an ender tank holds.")
                .defineInRange("tankBuckets", 16, 1, 1024);
        VANILLA_SOUNDS = builder.comment("Use the vanilla ender chest sounds instead of the normal chest sounds.")
                .define("useVanillaEnderChestSounds", false);
        SPEC = builder.build();
    }

    private EnderStorageConfig() {
    }

    public static ItemStack personalItem() {
        Identifier id = Identifier.tryParse(SPEC.isLoaded() ? PERSONAL_ITEM.get() : "minecraft:diamond");
        Item item = id == null ? Items.DIAMOND : BuiltInRegistries.ITEM.getValue(id);
        return new ItemStack(item == Items.AIR ? Items.DIAMOND : item);
    }

    public static boolean anarchyMode() {
        return SPEC.isLoaded() && ANARCHY_MODE.get();
    }

    /** Number of slots of an ender chest. */
    public static int storageSlots() {
        int size = SPEC.isLoaded() ? STORAGE_SIZE.get() : 1;
        return size == 0 ? 9 : size == 1 ? 27 : 54;
    }

    public static int tankCapacity() {
        return (SPEC.isLoaded() ? TANK_BUCKETS.get() : 16) * 1000;
    }

    public static boolean vanillaSounds() {
        return SPEC.isLoaded() && VANILLA_SOUNDS.get();
    }
}
