package net.minecraft.world.level;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

public class GameRules {
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<GameRules.Key<?>, GameRules.Type<?>> GAME_RULE_TYPES = Maps.newTreeMap(Comparator.comparing(p_46218_ -> p_46218_.id));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOFIRETICK = register(
        "doFireTick", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ALLOWFIRETICKAWAYFROMPLAYERS = register(
        "allowFireTicksAwayFromPlayer", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_MOBGRIEFING = register(
        "mobGriefing", GameRules.Category.MOBS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_KEEPINVENTORY = register(
        "keepInventory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBSPAWNING = register(
        "doMobSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBLOOT = register(
        "doMobLoot", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_PROJECTILESCANBREAKBLOCKS = register(
        "projectilesCanBreakBlocks", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOBLOCKDROPS = register(
        "doTileDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOENTITYDROPS = register(
        "doEntityDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMANDBLOCKOUTPUT = register(
        "commandBlockOutput", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_NATURAL_REGENERATION = register(
        "naturalRegeneration", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DAYLIGHT = register(
        "doDaylightCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(!SharedConstants.DEBUG_WORLD_RECREATE)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LOGADMINCOMMANDS = register(
        "logAdminCommands", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SHOWDEATHMESSAGES = register(
        "showDeathMessages", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_RANDOMTICKING = register(
        "randomTickSpeed", GameRules.Category.UPDATES, GameRules.IntegerValue.create(3)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SENDCOMMANDFEEDBACK = register(
        "sendCommandFeedback", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_REDUCEDDEBUGINFO = register(
        "reducedDebugInfo", GameRules.Category.MISC, GameRules.BooleanValue.create(false, (p_294061_, p_294062_) -> {
            byte b0 = (byte)(p_294062_.get() ? 22 : 23);

            for (ServerPlayer serverplayer : p_294061_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundEntityEventPacket(serverplayer, b0));
            }
        })
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SPECTATORSGENERATECHUNKS = register(
        "spectatorsGenerateChunks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SPAWN_RADIUS = register(
        "spawnRadius", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_PLAYER_MOVEMENT_CHECK = register(
        "disablePlayerMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = register(
        "disableElytraMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_ENTITY_CRAMMING = register(
        "maxEntityCramming", GameRules.Category.MOBS, GameRules.IntegerValue.create(24)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_WEATHER_CYCLE = register(
        "doWeatherCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(!SharedConstants.DEBUG_WORLD_RECREATE)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LIMITED_CRAFTING = register(
        "doLimitedCrafting", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (p_301943_, p_301944_) -> {
            for (ServerPlayer serverplayer : p_301943_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LIMITED_CRAFTING, p_301944_.get() ? 1.0F : 0.0F));
            }
        })
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_CHAIN_LENGTH = register(
        "maxCommandChainLength", GameRules.Category.MISC, GameRules.IntegerValue.create(65536)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_FORK_COUNT = register(
        "maxCommandForkCount", GameRules.Category.MISC, GameRules.IntegerValue.create(65536)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_COMMAND_MODIFICATION_BLOCK_LIMIT = register(
        "commandModificationBlockLimit", GameRules.Category.MISC, GameRules.IntegerValue.create(32768)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ANNOUNCE_ADVANCEMENTS = register(
        "announceAdvancements", GameRules.Category.CHAT, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_RAIDS = register(
        "disableRaids", GameRules.Category.MOBS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOINSOMNIA = register(
        "doInsomnia", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_IMMEDIATE_RESPAWN = register(
        "doImmediateRespawn", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (p_294059_, p_294060_) -> {
            for (ServerPlayer serverplayer : p_294059_.getPlayerList().getPlayers()) {
                serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, p_294060_.get() ? 1.0F : 0.0F));
            }
        })
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = register(
        "playersNetherPortalDefaultDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(80)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = register(
        "playersNetherPortalCreativeDelay", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DROWNING_DAMAGE = register(
        "drowningDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FALL_DAMAGE = register(
        "fallDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FIRE_DAMAGE = register(
        "fireDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FREEZE_DAMAGE = register(
        "freezeDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_PATROL_SPAWNING = register(
        "doPatrolSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_TRADER_SPAWNING = register(
        "doTraderSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_WARDEN_SPAWNING = register(
        "doWardenSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FORGIVE_DEAD_PLAYERS = register(
        "forgiveDeadPlayers", GameRules.Category.MOBS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_UNIVERSAL_ANGER = register(
        "universalAnger", GameRules.Category.MOBS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_SLEEPING_PERCENTAGE = register(
        "playersSleepingPercentage", GameRules.Category.PLAYER, GameRules.IntegerValue.create(100)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_BLOCK_EXPLOSION_DROP_DECAY = register(
        "blockExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_MOB_EXPLOSION_DROP_DECAY = register(
        "mobExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLOSION_DROP_DECAY = register(
        "tntExplosionDropDecay", GameRules.Category.DROPS, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SNOW_ACCUMULATION_HEIGHT = register(
        "snowAccumulationHeight", GameRules.Category.UPDATES, GameRules.IntegerValue.create(1)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_WATER_SOURCE_CONVERSION = register(
        "waterSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LAVA_SOURCE_CONVERSION = register(
        "lavaSourceConversion", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_GLOBAL_SOUND_EVENTS = register(
        "globalSoundEvents", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_VINES_SPREAD = register(
        "doVinesSpread", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ENDER_PEARLS_VANISH_ON_DEATH = register(
        "enderPearlsVanishOnDeath", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MINECART_MAX_SPEED = register(
        "minecartMaxSpeed",
        GameRules.Category.MISC,
        GameRules.IntegerValue.create(8, 1, 1000, FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS), (p_360120_, p_360121_) -> {})
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_TNT_EXPLODES = register(
        "tntExplodes", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LOCATOR_BAR = register(
        "locatorBar", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true, (p_415457_, p_415458_) -> p_415457_.getAllLevels().forEach(p_415462_ -> {
            ServerWaypointManager serverwaypointmanager = p_415462_.getWaypointManager();
            if (p_415458_.get()) {
                p_415462_.players().forEach(serverwaypointmanager::updatePlayer);
            } else {
                serverwaypointmanager.breakAllConnections();
            }
        }))
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_PVP = register("pvp", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ALLOW_NETHER = register(
        "allowEnteringNetherUsingPortals", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SPAWN_MONSTERS = register(
        "spawnMonsters", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true, (p_448978_, p_448979_) -> p_448978_.updateMobSpawningFlags())
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMAND_BLOCKS_ENABLED = register(
        "commandBlocksEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SPAWNER_BLOCKS_ENABLED = register(
        "spawnerBlocksEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
    );
    private final Map<GameRules.Key<?>, GameRules.Value<?>> rules;
    private final FeatureFlagSet enabledFeatures;

    public static <T extends GameRules.Value<T>> GameRules.Type<T> getType(GameRules.Key<T> key) {
        return (GameRules.Type<T>)GAME_RULE_TYPES.get(key);
    }

    public static <T extends GameRules.Value<T>> Codec<GameRules.Key<T>> keyCodec(Class<T> clazz) {
        return Codec.STRING
            .comapFlatMap(
                p_396930_ -> GAME_RULE_TYPES.entrySet()
                    .stream()
                    .filter(p_396932_ -> p_396932_.getValue().valueClass == clazz)
                    .map(Entry::getKey)
                    .filter(p_396927_ -> p_396927_.getId().equals(p_396930_))
                    .map(p_396934_ -> (GameRules.Key<T>)p_396934_)
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Invalid game rule ID for type: " + p_396930_)),
                GameRules.Key::getId
            );
    }

    public static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name, GameRules.Category category, GameRules.Type<T> p_type) {
        GameRules.Key<T> key = new GameRules.Key<>(name, category);
        GameRules.Type<?> type = GAME_RULE_TYPES.put(key, p_type);
        if (type != null) {
            throw new IllegalStateException("Duplicate game rule registration for " + name);
        } else {
            return key;
        }
    }

    public GameRules(FeatureFlagSet enabledFeatures, DynamicLike<?> tag) {
        this(enabledFeatures);
        this.loadFromTag(tag);
    }

    public GameRules(FeatureFlagSet enabledFeatures) {
        this(availableRules(enabledFeatures).collect(ImmutableMap.toImmutableMap(Entry::getKey, p_46210_ -> p_46210_.getValue().createRule())), enabledFeatures);
    }

    public static Stream<Entry<GameRules.Key<?>, GameRules.Type<?>>> availableRules(FeatureFlagSet enabledFeatures) {
        return GAME_RULE_TYPES.entrySet().stream().filter(p_360118_ -> p_360118_.getValue().requiredFeatures.isSubsetOf(enabledFeatures));
    }

    private GameRules(Map<GameRules.Key<?>, GameRules.Value<?>> rules, FeatureFlagSet enabledFeatures) {
        this.rules = rules;
        this.enabledFeatures = enabledFeatures;
    }

    public <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> key) {
        T t = (T)this.rules.get(key);
        if (t == null) {
            throw new IllegalArgumentException("Tried to access invalid game rule");
        } else {
            return t;
        }
    }

    public CompoundTag createTag() {
        CompoundTag compoundtag = new CompoundTag();
        this.rules.forEach((p_46197_, p_46198_) -> compoundtag.putString(p_46197_.id, p_46198_.serialize()));
        return compoundtag;
    }

    private void loadFromTag(DynamicLike<?> dynamic) {
        this.rules.forEach((p_337968_, p_337969_) -> dynamic.get(p_337968_.id).asString().ifSuccess(p_337969_::deserialize));
    }

    public GameRules copy(FeatureFlagSet enabledFeatures) {
        return new GameRules(
            availableRules(enabledFeatures)
                .collect(
                    ImmutableMap.toImmutableMap(
                        Entry::getKey,
                        p_415463_ -> this.rules.containsKey(p_415463_.getKey()) ? this.rules.get(p_415463_.getKey()).copy() : p_415463_.getValue().createRule()
                    )
                ),
            enabledFeatures
        );
    }

    public void visitGameRuleTypes(GameRules.GameRuleTypeVisitor visitor) {
        GAME_RULE_TYPES.forEach((p_360123_, p_360124_) -> this.callVisitorCap(visitor, (GameRules.Key<?>)p_360123_, (GameRules.Type<?>)p_360124_));
    }

    private <T extends GameRules.Value<T>> void callVisitorCap(GameRules.GameRuleTypeVisitor visitor, GameRules.Key<?> key, GameRules.Type<?> type) {
        if (type.requiredFeatures.isSubsetOf(this.enabledFeatures)) {
            visitor.visit((GameRules.Key<T>)key, (GameRules.Type<T>)type);
            ((GameRules.Type<T>)type).callVisitor(visitor, (GameRules.Key<T>)key);
        }
    }

    public void assignFrom(GameRules rules, @Nullable MinecraftServer server) {
        rules.rules.keySet().forEach(p_46182_ -> this.assignCap((GameRules.Key<?>)p_46182_, rules, server));
    }

    private <T extends GameRules.Value<T>> void assignCap(GameRules.Key<T> key, GameRules rules, @Nullable MinecraftServer server) {
        T t = rules.getRule(key);
        this.<T>getRule(key).setFrom(t, server);
    }

    public boolean getBoolean(GameRules.Key<GameRules.BooleanValue> key) {
        return this.getRule(key).get();
    }

    public int getInt(GameRules.Key<GameRules.IntegerValue> key) {
        return this.getRule(key).get();
    }

    public static class BooleanValue extends GameRules.Value<GameRules.BooleanValue> {
        private boolean value;

        private static GameRules.Type<GameRules.BooleanValue> create(
            boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> changeListener, FeatureFlagSet requiredFeatures
        ) {
            return new GameRules.Type<>(
                BoolArgumentType::bool,
                p_46242_ -> new GameRules.BooleanValue(p_46242_, defaultValue),
                changeListener,
                GameRules.GameRuleTypeVisitor::visitBoolean,
                GameRules.BooleanValue.class,
                requiredFeatures
            );
        }

        public static GameRules.Type<GameRules.BooleanValue> create(boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> changeListener) {
            return new GameRules.Type<>(
                BoolArgumentType::bool,
                p_415465_ -> new GameRules.BooleanValue(p_415465_, defaultValue),
                changeListener,
                GameRules.GameRuleTypeVisitor::visitBoolean,
                GameRules.BooleanValue.class,
                FeatureFlagSet.of()
            );
        }

        public static GameRules.Type<GameRules.BooleanValue> create(boolean defaultValue) {
            return create(defaultValue, (p_46236_, p_46237_) -> {});
        }

        public BooleanValue(GameRules.Type<GameRules.BooleanValue> type, boolean value) {
            super(type);
            this.value = value;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
            this.value = BoolArgumentType.getBool(context, paramName);
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChanged(server);
        }

        @Override
        public String serialize() {
            return Boolean.toString(this.value);
        }

        @Override
        protected void deserialize(String value) {
            this.value = Boolean.parseBoolean(value);
        }

        @Override
        public int getCommandResult() {
            return this.value ? 1 : 0;
        }

        protected GameRules.BooleanValue getSelf() {
            return this;
        }

        protected GameRules.BooleanValue copy() {
            return new GameRules.BooleanValue(this.type, this.value);
        }

        public void setFrom(GameRules.BooleanValue value, @Nullable MinecraftServer server) {
            this.value = value.value;
            this.onChanged(server);
        }
    }

    public static enum Category {
        PLAYER("gamerule.category.player"),
        MOBS("gamerule.category.mobs"),
        SPAWNING("gamerule.category.spawning"),
        DROPS("gamerule.category.drops"),
        UPDATES("gamerule.category.updates"),
        CHAT("gamerule.category.chat"),
        MISC("gamerule.category.misc");

        private final String descriptionId;

        private Category(String descriptionId) {
            this.descriptionId = descriptionId;
        }

        public String getDescriptionId() {
            return this.descriptionId;
        }
    }

    public interface GameRuleTypeVisitor {
        default <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
        }

        default void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
        }

        default void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
        }
    }

    public static class IntegerValue extends GameRules.Value<GameRules.IntegerValue> {
        private int value;

        public static GameRules.Type<GameRules.IntegerValue> create(int defaultValue, BiConsumer<MinecraftServer, GameRules.IntegerValue> changeListener) {
            return new GameRules.Type<>(
                IntegerArgumentType::integer,
                p_46293_ -> new GameRules.IntegerValue(p_46293_, defaultValue),
                changeListener,
                GameRules.GameRuleTypeVisitor::visitInteger,
                GameRules.IntegerValue.class,
                FeatureFlagSet.of()
            );
        }

        static GameRules.Type<GameRules.IntegerValue> create(
            int defaultValue, int min, int max, FeatureFlagSet requiredFeatures, BiConsumer<MinecraftServer, GameRules.IntegerValue> changeListener
        ) {
            return new GameRules.Type<>(
                () -> IntegerArgumentType.integer(min, max),
                p_319748_ -> new GameRules.IntegerValue(p_319748_, defaultValue),
                changeListener,
                GameRules.GameRuleTypeVisitor::visitInteger,
                GameRules.IntegerValue.class,
                requiredFeatures
            );
        }

        public static GameRules.Type<GameRules.IntegerValue> create(int defaultValue) {
            return create(defaultValue, (p_46309_, p_46310_) -> {});
        }

        public IntegerValue(GameRules.Type<GameRules.IntegerValue> type, int value) {
            super(type);
            this.value = value;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
            this.value = IntegerArgumentType.getInteger(context, paramName);
        }

        public int get() {
            return this.value;
        }

        public void set(int value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChanged(server);
        }

        @Override
        public String serialize() {
            return Integer.toString(this.value);
        }

        @Override
        protected void deserialize(String value) {
            this.value = safeParse(value);
        }

        public boolean tryDeserialize(String name) {
            try {
                StringReader stringreader = new StringReader(name);
                this.value = (Integer)this.type.argument.get().parse(stringreader);
                return !stringreader.canRead();
            } catch (CommandSyntaxException commandsyntaxexception) {
                return false;
            }
        }

        private static int safeParse(String strValue) {
            if (!strValue.isEmpty()) {
                try {
                    return Integer.parseInt(strValue);
                } catch (NumberFormatException numberformatexception) {
                    GameRules.LOGGER.warn("Failed to parse integer {}", strValue);
                }
            }

            return 0;
        }

        @Override
        public int getCommandResult() {
            return this.value;
        }

        protected GameRules.IntegerValue getSelf() {
            return this;
        }

        protected GameRules.IntegerValue copy() {
            return new GameRules.IntegerValue(this.type, this.value);
        }

        public void setFrom(GameRules.IntegerValue value, @Nullable MinecraftServer server) {
            this.value = value.value;
            this.onChanged(server);
        }
    }

    public static final class Key<T extends GameRules.Value<T>> {
        final String id;
        private final GameRules.Category category;

        public Key(String id, GameRules.Category category) {
            this.id = id;
            this.category = category;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public boolean equals(Object other) {
            return this == other ? true : other instanceof GameRules.Key && ((GameRules.Key)other).id.equals(this.id);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        public String getId() {
            return this.id;
        }

        public String getDescriptionId() {
            return "gamerule." + this.id;
        }

        public GameRules.Category getCategory() {
            return this.category;
        }
    }

    public static class Type<T extends GameRules.Value<T>> {
        final Supplier<ArgumentType<?>> argument;
        private final Function<GameRules.Type<T>, T> constructor;
        final BiConsumer<MinecraftServer, T> callback;
        private final GameRules.VisitorCaller<T> visitorCaller;
        final Class<T> valueClass;
        final FeatureFlagSet requiredFeatures;

        Type(
            Supplier<ArgumentType<?>> argument,
            Function<GameRules.Type<T>, T> constructor,
            BiConsumer<MinecraftServer, T> callback,
            GameRules.VisitorCaller<T> visitorCaller,
            Class<T> valueClass,
            FeatureFlagSet requiredFeatures
        ) {
            this.argument = argument;
            this.constructor = constructor;
            this.callback = callback;
            this.visitorCaller = visitorCaller;
            this.valueClass = valueClass;
            this.requiredFeatures = requiredFeatures;
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> createArgument(String name) {
            return Commands.argument(name, this.argument.get());
        }

        public T createRule() {
            return this.constructor.apply(this);
        }

        public void callVisitor(GameRules.GameRuleTypeVisitor visitor, GameRules.Key<T> key) {
            this.visitorCaller.call(visitor, key, this);
        }

        public FeatureFlagSet requiredFeatures() {
            return this.requiredFeatures;
        }
    }

    public abstract static class Value<T extends GameRules.Value<T>> {
        protected final GameRules.Type<T> type;

        public Value(GameRules.Type<T> type) {
            this.type = type;
        }

        protected abstract void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName);

        public void setFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
            this.updateFromArgument(context, paramName);
            this.onChanged(context.getSource().getServer());
        }

        protected void onChanged(@Nullable MinecraftServer server) {
            if (server != null) {
                this.type.callback.accept(server, this.getSelf());
            }
        }

        protected abstract void deserialize(String value);

        public abstract String serialize();

        @Override
        public String toString() {
            return this.serialize();
        }

        public abstract int getCommandResult();

        protected abstract T getSelf();

        protected abstract T copy();

        public abstract void setFrom(T value, @Nullable MinecraftServer server);
    }

    interface VisitorCaller<T extends GameRules.Value<T>> {
        void call(GameRules.GameRuleTypeVisitor visitor, GameRules.Key<T> key, GameRules.Type<T> type);
    }
}
