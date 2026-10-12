package com.magicbanana666.enderstorage;

import com.magicbanana666.enderstorage.block.EnderChestBlock;
import com.magicbanana666.enderstorage.block.EnderTankBlock;
import com.magicbanana666.enderstorage.block.entity.EnderChestBlockEntity;
import com.magicbanana666.enderstorage.block.entity.EnderTankBlockEntity;
import com.magicbanana666.enderstorage.item.EnderPouchItem;
import com.magicbanana666.enderstorage.item.FrequencyBlockItem;
import com.magicbanana666.enderstorage.recipe.FrequencyShapedRecipe;
import com.magicbanana666.enderstorage.recipe.RecolourRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * EnderStorage: ender chests, tanks and pouches that share their contents with everything of the same three colours.
 */
@Mod(EnderStorage.MOD_ID)
public final class EnderStorage {

    public static final String MOD_ID = "enderstorage";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Frequency>> FREQUENCY = COMPONENTS.registerComponentType("frequency",
            builder -> builder.persistent(Frequency.CODEC).networkSynchronized(Frequency.STREAM_CODEC));

    public static final DeferredBlock<EnderChestBlock> ENDER_CHEST = BLOCKS.registerBlock("ender_chest", EnderChestBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(20F, 100F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<EnderTankBlock> ENDER_TANK = BLOCKS.registerBlock("ender_tank", EnderTankBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(20F, 100F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<FrequencyBlockItem> ENDER_CHEST_ITEM = ITEMS.registerItem("ender_chest",
            p -> new FrequencyBlockItem(ENDER_CHEST.get(), p), p -> p.useBlockDescriptionPrefix().component(FREQUENCY.get(), Frequency.DEFAULT));
    public static final DeferredItem<FrequencyBlockItem> ENDER_TANK_ITEM = ITEMS.registerItem("ender_tank",
            p -> new FrequencyBlockItem(ENDER_TANK.get(), p), p -> p.useBlockDescriptionPrefix().component(FREQUENCY.get(), Frequency.DEFAULT));
    public static final DeferredItem<EnderPouchItem> ENDER_POUCH = ITEMS.registerItem("ender_pouch", EnderPouchItem::new,
            p -> p.stacksTo(1).component(FREQUENCY.get(), Frequency.DEFAULT));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderChestBlockEntity>> ENDER_CHEST_BE = BLOCK_ENTITIES.register("ender_chest",
            () -> new BlockEntityType<>(EnderChestBlockEntity::new, ENDER_CHEST.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderTankBlockEntity>> ENDER_TANK_BE = BLOCK_ENTITIES.register("ender_tank",
            () -> new BlockEntityType<>(EnderTankBlockEntity::new, ENDER_TANK.get()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FrequencyShapedRecipe>> CREATE_RECIPE = RECIPES.register("create_recipe",
            () -> FrequencyShapedRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RecolourRecipe>> RECOLOUR_RECIPE = RECIPES.register("recolour_recipe",
            () -> RecolourRecipe.SERIALIZER);

    public EnderStorage(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        RECIPES.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, EnderStorageConfig.SPEC);
        modBus.addListener(EnderStorage::registerCapabilities);
        modBus.addListener(EnderStorage::addToCreativeTabs);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENDER_CHEST_BE.get(), (chest, side) -> chest.getItemHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ENDER_TANK_BE.get(), (tank, side) -> tank.getFluidHandler());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ENDER_CHEST_ITEM.get());
            event.accept(ENDER_TANK_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ENDER_POUCH.get());
        }
    }
}
