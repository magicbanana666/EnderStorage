package com.magicbanana666.enderstorage;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.magicbanana666.enderstorage.block.EnderChestBlock;
import com.magicbanana666.enderstorage.block.EnderTankBlock;
import com.magicbanana666.enderstorage.block.entity.EnderChestBlockEntity;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import com.magicbanana666.enderstorage.item.EnderPouchItem;
import com.magicbanana666.enderstorage.item.FrequencyBlockItem;
import com.magicbanana666.enderstorage.menu.EnderChestMenu;
import com.magicbanana666.enderstorage.menu.EnderTankMenu;
import com.magicbanana666.enderstorage.recipe.FrequencyShapedRecipe;
import com.magicbanana666.enderstorage.recipe.RecolourRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * EnderStorage: ender chests, tanks and pouches that share their contents with everything of the same three colours.
 */
public final class EnderStorage implements ModInitializer {

    public static final String MOD_ID = "enderstorage";

    public static final DataComponentType<Frequency> FREQUENCY = component("frequency",
            builder -> builder.persistent(Frequency.CODEC).networkSynchronized(Frequency.STREAM_CODEC));
    /** Pump upgrades of an ender tank item, kept when the tank is broken. */
    public static final DataComponentType<Integer> PUMP_UPGRADES = component("pump_upgrades",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** The item that locked a personal or team chest or tank, given back when the lock is removed. */
    public static final DataComponentType<ItemStack> LOCK_ITEM = component("lock_item",
            builder -> builder.persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC));

    public static final EnderChestBlock ENDER_CHEST = block("ender_chest", EnderChestBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(20F, 100F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    public static final EnderTankBlock ENDER_TANK = block("ender_tank", EnderTankBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(20F, 100F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));

    public static final FrequencyBlockItem ENDER_CHEST_ITEM = item("ender_chest",
            p -> new FrequencyBlockItem(ENDER_CHEST, p), p -> p.useBlockDescriptionPrefix().component(FREQUENCY, Frequency.DEFAULT));
    public static final FrequencyBlockItem ENDER_TANK_ITEM = item("ender_tank",
            p -> new FrequencyBlockItem(ENDER_TANK, p), p -> p.useBlockDescriptionPrefix().component(FREQUENCY, Frequency.DEFAULT));
    public static final EnderPouchItem ENDER_POUCH = item("ender_pouch", EnderPouchItem::new,
            p -> p.stacksTo(1).component(FREQUENCY, Frequency.DEFAULT));

    public static final BlockEntityType<EnderChestBlockEntity> ENDER_CHEST_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("ender_chest"),
            FabricBlockEntityTypeBuilder.create(EnderChestBlockEntity::new, ENDER_CHEST).build());
    public static final BlockEntityType<EnderTankBlockEntity> ENDER_TANK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("ender_tank"),
            FabricBlockEntityTypeBuilder.create(EnderTankBlockEntity::new, ENDER_TANK).build());

    /** The client gets the number of slots of the storage. */
    public static final MenuType<EnderChestMenu> ENDER_CHEST_MENU = Registry.register(BuiltInRegistries.MENU, id("ender_chest"),
            new ExtendedMenuType<>(EnderChestMenu::new, ByteBufCodecs.VAR_INT));
    /** The client gets the position of the tank. */
    public static final MenuType<EnderTankMenu> ENDER_TANK_MENU = Registry.register(BuiltInRegistries.MENU, id("ender_tank"),
            new ExtendedMenuType<>(EnderTankMenu::new, BlockPos.STREAM_CODEC));

    public static final RecipeSerializer<FrequencyShapedRecipe> CREATE_RECIPE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("create_recipe"),
            FrequencyShapedRecipe.SERIALIZER);
    public static final RecipeSerializer<RecolourRecipe> RECOLOUR_RECIPE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("recolour_recipe"),
            RecolourRecipe.SERIALIZER);

    @Override
    public void onInitialize() {
        EnderStorageConfig.init();
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((chest, side) -> chest.getItemHandler(), ENDER_CHEST_BE);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((tank, side) -> tank.getFluidHandler(), ENDER_TANK_BE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
            output.accept(ENDER_CHEST_ITEM);
            output.accept(ENDER_TANK_ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ENDER_POUCH));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static <T> DataComponentType<T> component(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(name), builder.apply(DataComponentType.builder()).build());
    }

    private static <B extends Block> B block(String name, Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.apply(BlockBehaviour.Properties.of()).setId(key)));
    }

    private static <I extends Item> I item(String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.apply(new Item.Properties()).setId(key)));
    }
}
