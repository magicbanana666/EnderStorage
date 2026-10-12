package com.magicbanana666.enderstorage.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.magicbanana666.enderstorage.EnderStorage;
import com.magicbanana666.enderstorage.Frequency;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.fluids.FluidStack;

/** All ender chest inventories and ender tanks of a world, by frequency. Saved with the world. */
public class SharedStorage extends SavedData {

    private record SlotItem(int slot, ItemStack stack) {
        static final Codec<SlotItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(SlotItem::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(SlotItem::stack)
        ).apply(i, SlotItem::new));
    }

    private record ItemEntry(Frequency frequency, List<SlotItem> items, int bonusSlots) {
        static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Frequency.CODEC.fieldOf("frequency").forGetter(ItemEntry::frequency),
                SlotItem.CODEC.listOf().fieldOf("items").forGetter(ItemEntry::items),
                Codec.INT.optionalFieldOf("bonus_slots", 0).forGetter(ItemEntry::bonusSlots)
        ).apply(i, ItemEntry::new));
    }

    private record FluidEntry(Frequency frequency, FluidStack fluid, int bonusCapacity) {
        static final Codec<FluidEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Frequency.CODEC.fieldOf("frequency").forGetter(FluidEntry::frequency),
                FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(FluidEntry::fluid),
                Codec.INT.optionalFieldOf("bonus_capacity", 0).forGetter(FluidEntry::bonusCapacity)
        ).apply(i, FluidEntry::new));
    }

    private static final Codec<SharedStorage> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemEntry.CODEC.listOf().fieldOf("chests").forGetter(SharedStorage::saveItems),
            FluidEntry.CODEC.listOf().fieldOf("tanks").forGetter(SharedStorage::saveFluids)
    ).apply(i, SharedStorage::new));

    public static final SavedDataType<SharedStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EnderStorage.MOD_ID, "shared_storage"), SharedStorage::new, CODEC);

    private final Map<String, ItemStorage> chests = new HashMap<>();
    private final Map<String, FluidStorage> tanks = new HashMap<>();

    public SharedStorage() {
    }

    private SharedStorage(List<ItemEntry> items, List<FluidEntry> fluids) {
        for (ItemEntry entry : items) {
            ItemStorage storage = new ItemStorage(this, entry.frequency());
            storage.loadBonusSlots(entry.bonusSlots());
            for (SlotItem item : entry.items()) {
                storage.load(item.slot(), item.stack());
            }
            this.chests.put(entry.frequency().key(), storage);
        }
        for (FluidEntry entry : fluids) {
            FluidStorage storage = new FluidStorage(this, entry.frequency());
            storage.loadBonusCapacity(entry.bonusCapacity());
            storage.load(entry.fluid());
            this.tanks.put(entry.frequency().key(), storage);
        }
    }

    public static SharedStorage get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public ItemStorage chest(Frequency frequency) {
        return this.chests.computeIfAbsent(frequency.key(), key -> new ItemStorage(this, frequency));
    }

    public FluidStorage tank(Frequency frequency) {
        return this.tanks.computeIfAbsent(frequency.key(), key -> new FluidStorage(this, frequency));
    }

    private List<ItemEntry> saveItems() {
        List<ItemEntry> list = new ArrayList<>();
        for (ItemStorage storage : this.chests.values()) {
            List<SlotItem> items = new ArrayList<>();
            for (int slot = 0; slot < storage.getContainerSize(); slot++) {
                ItemStack stack = storage.getItem(slot);
                if (!stack.isEmpty()) {
                    items.add(new SlotItem(slot, stack));
                }
            }
            for (ItemStack overflow : storage.overflow()) {
                items.add(new SlotItem(-1, overflow));
            }
            if (!items.isEmpty() || storage.bonusSlots() > 0) {
                list.add(new ItemEntry(storage.frequency(), items, storage.bonusSlots()));
            }
        }
        return list;
    }

    private List<FluidEntry> saveFluids() {
        List<FluidEntry> list = new ArrayList<>();
        for (FluidStorage storage : this.tanks.values()) {
            if (!storage.getFluid().isEmpty() || storage.bonusCapacity() > 0) {
                list.add(new FluidEntry(storage.frequency(), storage.getFluid(), storage.bonusCapacity()));
            }
        }
        return list;
    }
}
