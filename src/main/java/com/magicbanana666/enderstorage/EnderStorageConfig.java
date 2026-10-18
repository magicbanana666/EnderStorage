package com.magicbanana666.enderstorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The server config, in {@code <world>/serverconfig/enderstorage-server.toml} (a copy in {@code defaultconfigs/} is used
 * for new worlds). It is read when the server starts and again when the file changes, and sent to every client that
 * joins, so client side checks use the same values.
 */
public final class EnderStorageConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = EnderStorage.MOD_ID + "-server.toml";
    private static final List<Option<?>> OPTIONS = new ArrayList<>();

    private static final StringOption PERSONAL_ITEM = new StringOption("access", "personalItem", "minecraft:diamond",
            "The item that locks an ender chest or tank to its owner (right click the lock or valve with it). Use #namespace:tag for a tag.");
    private static final StringOption TEAM_ITEM = new StringOption("access", "teamItem", "minecraft:emerald",
            "The item that locks an ender chest or tank to your team (FTB Teams party, or /team). Use #namespace:tag for a tag.");
    private static final BooleanOption ANARCHY_MODE = new BooleanOption("access", "anarchyMode", false,
            "Personal and team chests and tanks lose their lock when broken, and anyone can open them.");
    private static final BooleanOption VANILLA_SOUNDS = new BooleanOption("access", "useVanillaEnderChestSounds", false,
            "Use the vanilla ender chest sounds instead of the normal chest sounds.");

    private static final StringOption SMALL_UPGRADE_ITEM = new StringOption("upgrades", "smallUpgradeItem", "#c:ender_pearls",
            "Right click a chest or tank with this item for a small capacity upgrade. Use #namespace:tag for a tag.");
    private static final StringOption LARGE_UPGRADE_ITEM = new StringOption("upgrades", "largeUpgradeItem", "minecraft:ender_eye",
            "Right click a chest or tank with this item for a large capacity upgrade. Use #namespace:tag for a tag.");
    private static final StringOption PUMP_ITEM = new StringOption("upgrades", "pumpItem", "minecraft:piston",
            "Right click a tank with this item to make its valve pump faster. Use #namespace:tag for a tag.");

    private static final IntOption CHEST_START_SLOTS = new IntOption("chest", "startSlots", 27, 1, 270,
            "Slots of an ender chest frequency before upgrades.");
    private static final IntOption CHEST_MAX_SLOTS = new IntOption("chest", "maxSlots", 108, 1, 270,
            "Most slots an ender chest frequency can be upgraded to.");
    private static final IntOption CHEST_SMALL_UPGRADE = new IntOption("chest", "smallUpgradeSlots", 3, 1, 270,
            "Slots added by a small upgrade.");
    private static final IntOption CHEST_LARGE_UPGRADE = new IntOption("chest", "largeUpgradeSlots", 9, 1, 270,
            "Slots added by a large upgrade.");

    private static final IntOption TANK_BUCKETS = new IntOption("tank", "tankBuckets", 16, 1, 4096,
            "Buckets an ender tank frequency holds before upgrades.");
    private static final IntOption TANK_MAX_BUCKETS = new IntOption("tank", "maxBuckets", 256, 1, 4096,
            "Most buckets an ender tank frequency can be upgraded to.");
    private static final IntOption TANK_SMALL_UPGRADE = new IntOption("tank", "smallUpgradeBuckets", 4, 1, 4096,
            "Buckets added by a small upgrade.");
    private static final IntOption TANK_LARGE_UPGRADE = new IntOption("tank", "largeUpgradeBuckets", 16, 1, 4096,
            "Buckets added by a large upgrade.");
    private static final IntOption PUMP_BASE_RATE = new IntOption("tank", "pumpBaseRate", 250, 1, 100000,
            "mB per tick an open valve pushes out without pump upgrades.");
    private static final IntOption PUMP_UPGRADE_RATE = new IntOption("tank", "pumpUpgradeRate", 1000, 1, 100000,
            "mB per tick added by each pump upgrade.");
    private static final IntOption PUMP_MAX_UPGRADES = new IntOption("tank", "pumpMaxUpgrades", 4, 0, 64,
            "How many pump upgrades a tank can have.");

    /** Whether the values come from a config file (on the server) or from the server (on a client). */
    private static boolean loaded;
    private static @Nullable Path file;
    private static long lastModified;
    private static int watchTicks;

    private EnderStorageConfig() {
    }

    static void init() {
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.STREAM_CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(EnderStorageConfig::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> unload());
        ServerTickEvents.END_SERVER_TICK.register(EnderStorageConfig::watch);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }

    private static boolean loaded() {
        return loaded;
    }

    // ---- File --------------------------------------------------------------------------------------------------

    private static void load(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
        file = path;
        try {
            Files.createDirectories(path.getParent());
            Path defaults = FabricLoader.getInstance().getGameDir().resolve("defaultconfigs").resolve(FILE_NAME);
            if (!Files.exists(path) && Files.exists(defaults)) {
                Files.copy(defaults, path);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not create {}", path, e);
        }
        read(path);
        loaded = true;
    }

    private static void unload() {
        loaded = false;
        file = null;
        OPTIONS.forEach(Option::reset);
    }

    /** Reads the file; missing or invalid values get their default and the file is written again. */
    private static void read(Path path) {
        OPTIONS.forEach(Option::reset);
        boolean rewrite = true;
        if (Files.exists(path)) {
            rewrite = false;
            Map<String, String> values = new LinkedHashMap<>();
            try {
                String section = "";
                for (String raw : Files.readAllLines(path)) {
                    String line = raw.strip();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (line.startsWith("[") && line.endsWith("]")) {
                        section = line.substring(1, line.length() - 1).strip();
                        continue;
                    }
                    int equals = line.indexOf('=');
                    if (equals > 0) {
                        values.put(section + "." + line.substring(0, equals).strip(), line.substring(equals + 1).strip());
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Could not read {}, using the defaults", path, e);
            }
            for (Option<?> option : OPTIONS) {
                String value = values.get(option.path());
                if (value == null || !option.parse(value)) {
                    if (value != null) {
                        LOGGER.warn("Invalid value {} for {} in {}, using {}", value, option.path(), path.getFileName(), option.asString());
                    }
                    option.reset();
                    rewrite = true;
                }
            }
        }
        if (rewrite) {
            write(path);
        }
        try {
            lastModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            lastModified = 0;
        }
    }

    private static void write(Path path) {
        StringBuilder out = new StringBuilder();
        String section = null;
        for (Option<?> option : OPTIONS) {
            if (!option.section.equals(section)) {
                if (section != null) {
                    out.append('\n');
                }
                section = option.section;
                out.append('[').append(section).append("]\n");
            }
            out.append("\t#").append(option.comment).append('\n');
            option.extraComment().ifPresent(extra -> out.append("\t#").append(extra).append('\n'));
            out.append('\t').append(option.key).append(" = ").append(option.asString()).append('\n');
        }
        try {
            Files.writeString(path, out.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", path, e);
        }
    }

    /** Reads the file again when it was changed while the server runs, and sends the new values to everyone. */
    private static void watch(MinecraftServer server) {
        if (++watchTicks < 40 || file == null) {
            return;
        }
        watchTicks = 0;
        try {
            if (Files.exists(file) && Files.getLastModifiedTime(file).toMillis() != lastModified) {
                LOGGER.info("Reloading {}", file.getFileName());
                read(file);
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    sync(player);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not check {}", file, e);
        }
    }

    // ---- Config screen -----------------------------------------------------------------------------------------

    /** Every option in file order, for the config screen. */
    public static List<Option<?>> options() {
        return Collections.unmodifiableList(OPTIONS);
    }

    /** Whether the config of a running server in this game (singleplayer) can be edited. */
    public static boolean isEditable() {
        return file != null;
    }

    /** Saves the edited values (all valid) and sends them to everyone. */
    public static void save(MinecraftServer server) {
        if (file != null) {
            write(file);
            read(file);
            for (ServerPlayer player : PlayerLookup.all(server)) {
                sync(player);
            }
        }
    }

    // ---- Sync --------------------------------------------------------------------------------------------------

    /** All values, sent to a client when it joins and when the file is reloaded. */
    public record SyncPayload(List<String> values) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(EnderStorage.id("config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).<RegistryFriendlyByteBuf>cast().map(SyncPayload::new, SyncPayload::values);

        @Override
        public Type<SyncPayload> type() {
            return TYPE;
        }
    }

    private static void sync(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, SyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new SyncPayload(OPTIONS.stream().map(Option::asString).toList()));
        }
    }

    /** Client side: uses the values of the server. In singleplayer they are already the same. */
    public static void receive(SyncPayload payload) {
        if (file != null) {
            return;
        }
        for (int i = 0; i < OPTIONS.size() && i < payload.values().size(); i++) {
            if (!OPTIONS.get(i).parse(payload.values().get(i))) {
                OPTIONS.get(i).reset();
            }
        }
        loaded = true;
    }

    /** Client side: back to the defaults after leaving a server. */
    public static void disconnected() {
        if (file == null) {
            loaded = false;
            OPTIONS.forEach(Option::reset);
        }
    }

    // ---- Options -----------------------------------------------------------------------------------------------

    /** One config value with its section, key, comment and default. */
    public abstract static sealed class Option<T> permits StringOption, BooleanOption, IntOption {
        private final String section;
        private final String key;
        private final String comment;
        private final T defaultValue;
        protected T value;

        Option(String section, String key, T defaultValue, String comment) {
            this.section = section;
            this.key = key;
            this.comment = comment;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            OPTIONS.add(this);
        }

        public String section() {
            return this.section;
        }

        public String key() {
            return this.key;
        }

        public String path() {
            return this.section + "." + this.key;
        }

        public String comment() {
            return this.comment;
        }

        public T get() {
            return this.value;
        }

        public T defaultValue() {
            return this.defaultValue;
        }

        void reset() {
            this.value = this.defaultValue;
        }

        java.util.Optional<String> extraComment() {
            return java.util.Optional.empty();
        }

        /** The value as it is written in the file. */
        public abstract String asString();

        /** Sets the value from the file format (strings may also be given without quotes); false when invalid. */
        public abstract boolean parse(String raw);

        /** Whether {@link #parse} would accept the text, without changing the value. */
        public boolean isValid(String raw) {
            T old = this.value;
            boolean valid = this.parse(raw);
            this.value = old;
            return valid;
        }
    }

    public static final class StringOption extends Option<String> {
        StringOption(String section, String key, String defaultValue, String comment) {
            super(section, key, defaultValue, comment);
        }

        @Override
        public String asString() {
            return "\"" + this.value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        @Override
        public boolean parse(String raw) {
            String text = raw.strip();
            if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
            } else if (text.contains("\"") || text.isEmpty()) {
                return false;
            }
            this.value = text;
            return true;
        }
    }

    public static final class BooleanOption extends Option<Boolean> {
        BooleanOption(String section, String key, boolean defaultValue, String comment) {
            super(section, key, defaultValue, comment);
        }

        @Override
        public String asString() {
            return this.value.toString();
        }

        @Override
        public boolean parse(String raw) {
            String text = raw.strip();
            if (text.equals("true") || text.equals("false")) {
                this.value = Boolean.parseBoolean(text);
                return true;
            }
            return false;
        }
    }

    public static final class IntOption extends Option<Integer> {
        private final int min;
        private final int max;

        IntOption(String section, String key, int defaultValue, int min, int max, String comment) {
            super(section, key, defaultValue, comment);
            this.min = min;
            this.max = max;
        }

        public int min() {
            return this.min;
        }

        public int max() {
            return this.max;
        }

        @Override
        java.util.Optional<String> extraComment() {
            return java.util.Optional.of("Range: " + this.min + " ~ " + this.max);
        }

        @Override
        public String asString() {
            return this.value.toString();
        }

        @Override
        public boolean parse(String raw) {
            try {
                int parsed = Integer.parseInt(raw.strip());
                if (parsed < this.min || parsed > this.max) {
                    return false;
                }
                this.value = parsed;
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    // ---- Values ------------------------------------------------------------------------------------------------

    /** Whether the stack is the configured item, or in the configured tag when the value starts with #. */
    private static boolean matches(ItemStack stack, Option<String> value, String fallback) {
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
    private static ItemStack firstItem(Option<String> value, String fallback, Item fallbackItem) {
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
