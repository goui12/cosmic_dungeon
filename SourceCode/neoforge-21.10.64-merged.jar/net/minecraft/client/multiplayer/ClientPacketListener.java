package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.HashOps;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientPacketListener extends ClientCommonPacketListenerImpl implements ClientGamePacketListener, TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
    private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
    private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
    private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
    private static final Component BAD_CHAT_INDEX = Component.translatable("multiplayer.disconnect.bad_chat_index");
    private static final Component COMMAND_SEND_CONFIRM_TITLE = Component.translatable("multiplayer.confirm_command.title");
    private static final Component BUTTON_RUN_COMMAND = Component.translatable("multiplayer.confirm_command.run_command");
    private static final Component BUTTON_SUGGEST_COMMAND = Component.translatable("multiplayer.confirm_command.suggest_command");
    private static final int PENDING_OFFSET_THRESHOLD = 64;
    public static final int TELEPORT_INTERPOLATION_THRESHOLD = 64;
    private static final ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> COMMAND_NODE_BUILDER = new ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider>() {
        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createLiteral(String p_426062_) {
            return LiteralArgumentBuilder.literal(p_426062_);
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createArgument(String p_425699_, ArgumentType<?> p_426176_, @Nullable ResourceLocation p_425669_) {
            RequiredArgumentBuilder<ClientSuggestionProvider, ?> requiredargumentbuilder = RequiredArgumentBuilder.argument(p_425699_, p_426176_);
            if (p_425669_ != null) {
                requiredargumentbuilder.suggests(SuggestionProviders.getProvider(p_425669_));
            }

            return requiredargumentbuilder;
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> configure(
            ArgumentBuilder<ClientSuggestionProvider, ?> p_426126_, boolean p_425940_, boolean p_425720_
        ) {
            if (p_425940_) {
                p_426126_.executes(p_425695_ -> 0);
            }

            if (p_425720_) {
                p_426126_.requires(ClientSuggestionProvider::allowsRestrictedCommands);
            }

            return p_426126_;
        }
    };
    private final GameProfile localGameProfile;
    /**
     * Reference to the current ClientWorld instance, which many handler methods operate on
     */
    private ClientLevel level;
    private ClientLevel.ClientLevelData levelData;
    /**
     * A mapping from player names to their respective GuiPlayerInfo (specifies the clients response time to the server)
     */
    private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
    private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet<>();
    private final ClientAdvancements advancements;
    private final ClientSuggestionProvider suggestionsProvider;
    private final ClientSuggestionProvider restrictedSuggestionsProvider;
    private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
    private int serverChunkRadius = 3;
    private int serverSimulationDistance = 3;
    /**
     * Just an ordinary random number generator, used to randomize audio pitch of item/orb pickup and randomize both particlespawn offset and velocity
     */
    private final RandomSource random = RandomSource.createThreadSafe();
    public CommandDispatcher<ClientSuggestionProvider> commands = new CommandDispatcher<>();
    private ClientRecipeContainer recipes = new ClientRecipeContainer(Map.of(), SelectableRecipe.SingleInputSet.empty());
    private final UUID id = UUID.randomUUID();
    private Set<ResourceKey<Level>> levels;
    private final RegistryAccess.Frozen registryAccess;
    private final FeatureFlagSet enabledFeatures;
    private final PotionBrewing potionBrewing;
    private FuelValues fuelValues;
    private final HashedPatchMap.HashGenerator decoratedHashOpsGenerator;
    private OptionalInt removedPlayerVehicleId = OptionalInt.empty();
    @Nullable
    private LocalChatSession chatSession;
    private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
    private int nextChatIndex;
    private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
    private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    @Nullable
    private CompletableFuture<Optional<ProfileKeyPair>> keyPairFuture;
    @Nullable
    private ClientInformation remoteClientInformation;
    private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
    private final PingDebugMonitor pingDebugMonitor;
    private final ClientDebugSubscriber debugSubscriber;
    private net.neoforged.neoforge.network.connection.ConnectionType connectionType;
    @Nullable
    private LevelLoadTracker levelLoadTracker;
    private boolean serverEnforcesSecureChat;
    private volatile boolean closed;
    private final Scoreboard scoreboard = new Scoreboard();
    private final ClientWaypointManager waypointManager = new ClientWaypointManager();
    private final SessionSearchTrees searchTrees = new SessionSearchTrees();
    private final List<WeakReference<CacheSlot<?, ?>>> cacheSlots = new ArrayList<>();

    public ClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
        this.localGameProfile = commonListenerCookie.localGameProfile();
        this.registryAccess = commonListenerCookie.receivedRegistries();
        RegistryOps<HashCode> registryops = this.registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE);
        this.decoratedHashOpsGenerator = p_412017_ -> p_412017_.encodeValue(registryops)
            .getOrThrow(p_412015_ -> new IllegalArgumentException("Failed to hash " + p_412017_ + ": " + p_412015_))
            .asInt();
        this.enabledFeatures = commonListenerCookie.enabledFeatures();
        this.advancements = new ClientAdvancements(minecraft, this.telemetryManager);
        this.suggestionsProvider = new ClientSuggestionProvider(this, minecraft, true);
        this.restrictedSuggestionsProvider = new ClientSuggestionProvider(this, minecraft, false);
        this.pingDebugMonitor = new PingDebugMonitor(this, minecraft.getDebugOverlay().getPingLogger());
        this.debugSubscriber = new ClientDebugSubscriber(this, minecraft.getDebugOverlay());
        if (commonListenerCookie.chatState() != null) {
            minecraft.gui.getChat().restoreState(commonListenerCookie.chatState());
        }

        this.connectionType = commonListenerCookie.connectionType();
        this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures, this.registryAccess);
        this.fuelValues = FuelValues.vanillaBurnTimes(commonListenerCookie.receivedRegistries(), this.enabledFeatures);
        this.levelLoadTracker = commonListenerCookie.levelLoadTracker();
    }

    public ClientSuggestionProvider getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    public void close() {
        this.closed = true;
        this.clearLevel();
        this.telemetryManager.onDisconnect();
    }

    public void clearLevel() {
        this.clearCacheSlots();
        this.level = null;
        this.levelLoadTracker = null;
    }

    private void clearCacheSlots() {
        for (WeakReference<CacheSlot<?, ?>> weakreference : this.cacheSlots) {
            CacheSlot<?, ?> cacheslot = weakreference.get();
            if (cacheslot != null) {
                cacheslot.clear();
            }
        }

        this.cacheSlots.clear();
    }

    public RecipeAccess recipes() {
        return this.recipes;
    }

    /**
     * Registers some server properties (gametype, hardcore-mode, terraintype, difficulty, player limit), creates a new WorldClient and sets the player initial dimension.
     */
    @Override
    public void handleLogin(ClientboundLoginPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
        CommonPlayerSpawnInfo commonplayerspawninfo = packet.commonPlayerSpawnInfo();
        List<ResourceKey<Level>> list = Lists.newArrayList(packet.levels());
        Collections.shuffle(list);
        this.levels = Sets.newLinkedHashSet(list);
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        this.serverChunkRadius = packet.chunkRadius();
        this.serverSimulationDistance = packet.simulationDistance();
        boolean flag = commonplayerspawninfo.isDebug();
        boolean flag1 = commonplayerspawninfo.isFlat();
        int i = commonplayerspawninfo.seaLevel();
        ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(Difficulty.NORMAL, packet.hardcore(), flag1);
        this.levelData = clientlevel$clientleveldata;
        this.level = new ClientLevel(
            this,
            clientlevel$clientleveldata,
            resourcekey,
            holder,
            this.serverChunkRadius,
            this.serverSimulationDistance,
            this.minecraft.levelRenderer,
            flag,
            commonplayerspawninfo.seed(),
            i
        );
        this.minecraft.setLevel(this.level);
        if (this.minecraft.player == null) {
            this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
            this.minecraft.player.setYRot(-180.0F);
            if (this.minecraft.getSingleplayerServer() != null) {
                this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
            }
        }

        this.debugSubscriber.clear();
        this.minecraft.levelRenderer.debugRenderer.refreshRendererList();
        this.minecraft.player.resetPos();
        net.neoforged.neoforge.client.ClientHooks.firePlayerLogin(this.minecraft.gameMode, this.minecraft.player, this.minecraft.getConnection().connection);
        this.minecraft.player.setId(packet.playerId());
        this.level.addEntity(this.minecraft.player);
        this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
        this.minecraft.setCameraEntity(this.minecraft.player);
        this.startWaitingForNewLevel(this.minecraft.player, this.level, LevelLoadingScreen.Reason.OTHER, null, null);
        this.minecraft.player.setReducedDebugInfo(packet.reducedDebugInfo());
        this.minecraft.player.setShowDeathScreen(packet.showDeathScreen());
        this.minecraft.player.setDoLimitedCrafting(packet.doLimitedCrafting());
        this.minecraft.player.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        this.minecraft.player.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
        this.minecraft.options.setServerRenderDistance(packet.chunkRadius());
        this.chatSession = null;
        this.signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
        this.nextChatIndex = 0;
        this.lastSeenMessages = new LastSeenMessagesTracker(20);
        this.messageSignatureCache = MessageSignatureCache.createDefault();
        if (this.connection.isEncrypted()) {
            this.prepareKeyPair();
        }

        this.telemetryManager.onPlayerInfoReceived(commonplayerspawninfo.gameType(), packet.hardcore());
        this.minecraft.quickPlayLog().log(this.minecraft);
        this.serverEnforcesSecureChat = packet.enforcesSecureChat();
        if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
            SystemToast systemtoast = SystemToast.multiline(
                this.minecraft, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST
            );
            this.minecraft.getToastManager().addToast(systemtoast);
            this.seenInsecureChatWarning = true;
        }
    }

    /**
     * Spawns an instance of the objecttype indicated by the packet and sets its position and momentum
     */
    @Override
    public void handleAddEntity(ClientboundAddEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.getId()) {
            this.removedPlayerVehicleId = OptionalInt.empty();
        }

        Entity entity = this.createEntityFromPacket(packet);
        if (entity != null) {
            entity.recreateFromPacket(packet);
            this.level.addEntity(entity);
            this.postAddEntitySoundInstance(entity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", packet.getType());
        }

        if (entity instanceof Player player) {
            UUID uuid = player.getUUID();
            PlayerInfo playerinfo = this.playerInfoMap.get(uuid);
            if (playerinfo != null) {
                this.seenPlayers.put(uuid, playerinfo);
            }
        }
    }

    @Nullable
    private Entity createEntityFromPacket(ClientboundAddEntityPacket packet) {
        EntityType<?> entitytype = packet.getType();
        if (entitytype == EntityType.PLAYER) {
            PlayerInfo playerinfo = this.getPlayerInfo(packet.getUUID());
            if (playerinfo == null) {
                LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", packet.getUUID());
                return null;
            } else {
                return new RemotePlayer(this.level, playerinfo.getProfile());
            }
        } else {
            return entitytype.create(this.level, EntitySpawnReason.LOAD);
        }
    }

    private void postAddEntitySoundInstance(Entity entity) {
        if (entity instanceof AbstractMinecart abstractminecart) {
            this.minecraft.getSoundManager().play(new MinecartSoundInstance(abstractminecart));
        } else if (entity instanceof Bee bee) {
            boolean flag = bee.isAngry();
            BeeSoundInstance beesoundinstance;
            if (flag) {
                beesoundinstance = new BeeAggressiveSoundInstance(bee);
            } else {
                beesoundinstance = new BeeFlyingSoundInstance(bee);
            }

            this.minecraft.getSoundManager().queueTickingSound(beesoundinstance);
        }
    }

    /**
     * Sets the velocity of the specified entity to the specified value
     */
    @Override
    public void handleSetEntityMotion(ClientboundSetEntityMotionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity != null) {
            entity.lerpMotion(packet.getMovement());
        }
    }

    /**
     * Invoked when the server registers new proximate objects in your watchlist or when objects in your watchlist have changed -> Registers any changes locally
     */
    @Override
    public void handleSetEntityData(ClientboundSetEntityDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity != null) {
            entity.getEntityData().assignValues(packet.packedItems());
        }
    }

    @Override
    public void handleEntityPositionSync(ClientboundEntityPositionSyncPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity != null) {
            Vec3 vec3 = packet.values().position();
            entity.getPositionCodec().setBase(vec3);
            if (!entity.isLocalInstanceAuthoritative()) {
                float f = packet.values().yRot();
                float f1 = packet.values().xRot();
                boolean flag = entity.position().distanceToSqr(vec3) > 4096.0;
                if (this.level.isTickingEntity(entity) && !flag) {
                    entity.moveOrInterpolateTo(vec3, f, f1);
                } else {
                    entity.snapTo(vec3, f, f1);
                }

                if (!entity.isInterpolating() && entity.hasIndirectPassenger(this.minecraft.player)) {
                    entity.positionRider(this.minecraft.player);
                    this.minecraft.player.setOldPosAndRot();
                }

                entity.setOnGround(packet.onGround());
            }
        }
    }

    /**
     * Updates an entity's position and rotation as specified by the packet
     */
    @Override
    public void handleTeleportEntity(ClientboundTeleportEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity == null) {
            if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.id()) {
                LOGGER.debug("Trying to teleport entity with id {}, that was formerly player vehicle, applying teleport to player instead", packet.id());
                setValuesFromPositionPacket(packet.change(), packet.relatives(), this.minecraft.player, false);
                this.connection
                    .send(
                        new ServerboundMovePlayerPacket.PosRot(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            this.minecraft.player.getYRot(),
                            this.minecraft.player.getXRot(),
                            false,
                            false
                        )
                    );
            }
        } else {
            boolean flag = packet.relatives().contains(Relative.X)
                || packet.relatives().contains(Relative.Y)
                || packet.relatives().contains(Relative.Z);
            boolean flag1 = this.level.isTickingEntity(entity) || !entity.isLocalInstanceAuthoritative() || flag;
            boolean flag2 = setValuesFromPositionPacket(packet.change(), packet.relatives(), entity, flag1);
            entity.setOnGround(packet.onGround());
            if (!flag2 && entity.hasIndirectPassenger(this.minecraft.player)) {
                entity.positionRider(this.minecraft.player);
                this.minecraft.player.setOldPosAndRot();
                if (entity.isLocalInstanceAuthoritative()) {
                    this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
                }
            }
        }
    }

    @Override
    public void handleTickingState(ClientboundTickingStatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setTickRate(packet.tickRate());
            tickratemanager.setFrozen(packet.isFrozen());
        }
    }

    @Override
    public void handleTickingStep(ClientboundTickingStepPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.minecraft.level != null) {
            TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
            tickratemanager.setFrozenTicksToRun(packet.tickSteps());
        }
    }

    @Override
    public void handleSetHeldSlot(ClientboundSetHeldSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (Inventory.isHotbarSlot(packet.slot())) {
            this.minecraft.player.getInventory().setSelectedSlot(packet.slot());
        }
    }

    /**
     * Updates the specified entity's position by the specified relative momentum and absolute rotation. Note that subclassing of the packet allows for the specification of a subset of this data (e.g. only rel. position, abs. rotation or both).
     */
    @Override
    public void handleMoveEntity(ClientboundMoveEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            if (entity.isLocalInstanceAuthoritative()) {
                VecDeltaCodec vecdeltacodec1 = entity.getPositionCodec();
                Vec3 vec31 = vecdeltacodec1.decode(packet.getXa(), packet.getYa(), packet.getZa());
                vecdeltacodec1.setBase(vec31);
            } else {
                if (packet.hasPosition()) {
                    VecDeltaCodec vecdeltacodec = entity.getPositionCodec();
                    Vec3 vec3 = vecdeltacodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
                    vecdeltacodec.setBase(vec3);
                    if (packet.hasRotation()) {
                        entity.moveOrInterpolateTo(vec3, packet.getYRot(), packet.getXRot());
                    } else {
                        entity.moveOrInterpolateTo(vec3);
                    }
                } else if (packet.hasRotation()) {
                    entity.moveOrInterpolateTo(packet.getYRot(), packet.getXRot());
                }

                entity.setOnGround(packet.isOnGround());
            }
        }
    }

    @Override
    public void handleMinecartAlongTrack(ClientboundMoveMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.getEntity(this.level) instanceof AbstractMinecart abstractminecart) {
            if (abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
                newminecartbehavior.lerpSteps.addAll(packet.lerpSteps());
            }
        }
    }

    /**
     * Updates the direction in which the specified entity is looking, normally this head rotation is independent of the rotation of the entity itself
     */
    @Override
    public void handleRotateMob(ClientboundRotateHeadPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            entity.lerpHeadTo(packet.getYHeadRot(), 3);
        }
    }

    @Override
    public void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.getEntityIds().forEach(p_448696_ -> {
            Entity entity = this.level.getEntity(p_448696_);
            if (entity != null) {
                if (entity.hasIndirectPassenger(this.minecraft.player)) {
                    LOGGER.debug("Remove entity {}:{} that has player as passenger", entity.getType(), p_448696_);
                    this.removedPlayerVehicleId = OptionalInt.of(p_448696_);
                }

                this.level.removeEntity(p_448696_, Entity.RemovalReason.DISCARDED);
                this.debugSubscriber.dropEntity(entity);
            }
        });
    }

    @Override
    public void handleMovePlayer(ClientboundPlayerPositionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        if (!player.isPassenger()) {
            setValuesFromPositionPacket(packet.change(), packet.relatives(), player, false);
        }

        this.connection.send(new ServerboundAcceptTeleportationPacket(packet.id()));
        this.connection
            .send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false, false));
    }

    private static boolean setValuesFromPositionPacket(PositionMoveRotation positionMoveRotation, Set<Relative> relatives, Entity entity, boolean lerp) {
        PositionMoveRotation positionmoverotation = PositionMoveRotation.of(entity);
        PositionMoveRotation positionmoverotation1 = PositionMoveRotation.calculateAbsolute(positionmoverotation, positionMoveRotation, relatives);
        boolean flag = positionmoverotation.position().distanceToSqr(positionmoverotation1.position()) > 4096.0;
        if (lerp && !flag) {
            entity.moveOrInterpolateTo(positionmoverotation1.position(), positionmoverotation1.yRot(), positionmoverotation1.xRot());
            entity.setDeltaMovement(positionmoverotation1.deltaMovement());
            return true;
        } else {
            entity.setPos(positionmoverotation1.position());
            entity.setDeltaMovement(positionmoverotation1.deltaMovement());
            entity.setYRot(positionmoverotation1.yRot());
            entity.setXRot(positionmoverotation1.xRot());
            PositionMoveRotation positionmoverotation2 = new PositionMoveRotation(entity.oldPosition(), Vec3.ZERO, entity.yRotO, entity.xRotO);
            PositionMoveRotation positionmoverotation3 = PositionMoveRotation.calculateAbsolute(positionmoverotation2, positionMoveRotation, relatives);
            entity.setOldPosAndRot(positionmoverotation3.position(), positionmoverotation3.yRot(), positionmoverotation3.xRot());
            return false;
        }
    }

    @Override
    public void handleRotatePlayer(ClientboundPlayerRotationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        Set<Relative> set = Relative.rotation(packet.relativeY(), packet.relativeX());
        PositionMoveRotation positionmoverotation = PositionMoveRotation.of(player);
        PositionMoveRotation positionmoverotation1 = PositionMoveRotation.calculateAbsolute(
            positionmoverotation, positionmoverotation.withRotation(packet.yRot(), packet.xRot()), set
        );
        player.setYRot(positionmoverotation1.yRot());
        player.setXRot(positionmoverotation1.xRot());
        player.setOldRot();
        this.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), false, false));
    }

    /**
     * Received from the servers PlayerManager if between 1 and 64 blocks in a chunk are changed. If only one block requires an update, the server sends S23PacketBlockChange and if 64 or more blocks are changed, the server sends S21PacketChunkData
     */
    @Override
    public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.runUpdates((p_284633_, p_284634_) -> this.level.setServerVerifiedBlockState(p_284633_, p_284634_, 19));
    }

    @Override
    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int i = packet.getX();
        int j = packet.getZ();
        this.updateLevelChunk(i, j, packet.getChunkData());
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = packet.getLightData();
        this.level.queueLightUpdate(() -> {
            this.applyLightData(i, j, clientboundlightupdatepacketdata, false);
            LevelChunk levelchunk = this.level.getChunkSource().getChunk(i, j, false);
            if (levelchunk != null) {
                this.enableChunkLight(levelchunk, i, j);
                this.minecraft.levelRenderer.onChunkReadyToRender(levelchunk.getPos());
            }
        });
    }

    @Override
    public void handleChunksBiomes(ClientboundChunksBiomesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata : packet.chunkBiomeData()) {
            this.level
                .getChunkSource()
                .replaceBiomes(
                    clientboundchunksbiomespacket$chunkbiomedata.pos().x,
                    clientboundchunksbiomespacket$chunkbiomedata.pos().z,
                    clientboundchunksbiomespacket$chunkbiomedata.getReadBuffer()
                );
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata1 : packet.chunkBiomeData()) {
            this.level
                .onChunkLoaded(new ChunkPos(clientboundchunksbiomespacket$chunkbiomedata1.pos().x, clientboundchunksbiomespacket$chunkbiomedata1.pos().z));
        }

        for (ClientboundChunksBiomesPacket.ChunkBiomeData clientboundchunksbiomespacket$chunkbiomedata2 : packet.chunkBiomeData()) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = this.level.getMinSectionY(); k <= this.level.getMaxSectionY(); k++) {
                        this.minecraft
                            .levelRenderer
                            .setSectionDirty(
                                clientboundchunksbiomespacket$chunkbiomedata2.pos().x + i, k, clientboundchunksbiomespacket$chunkbiomedata2.pos().z + j
                            );
                    }
                }
            }
        }
    }

    private void updateLevelChunk(int x, int z, ClientboundLevelChunkPacketData data) {
        this.level
            .getChunkSource()
            .replaceWithPacketData(
                x, z, data.getReadBuffer(), data.getHeightmaps(), data.getBlockEntitiesTagsConsumer(x, z)
            );
    }

    private void enableChunkLight(LevelChunk chunk, int x, int z) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        LevelChunkSection[] alevelchunksection = chunk.getSections();
        ChunkPos chunkpos = chunk.getPos();

        for (int i = 0; i < alevelchunksection.length; i++) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            int j = this.level.getSectionYFromSectionIndex(i);
            levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), levelchunksection.hasOnlyAir());
        }

        this.level.setSectionRangeDirty(x - 1, this.level.getMinSectionY(), z - 1, x + 1, this.level.getMaxSectionY(), z + 1);
    }

    @Override
    public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getChunkSource().drop(packet.pos());
        this.debugSubscriber.dropChunk(packet.pos());
        this.queueLightRemoval(packet);
    }

    private void queueLightRemoval(ClientboundForgetLevelChunkPacket packet) {
        ChunkPos chunkpos = packet.pos();
        this.level.queueLightUpdate(() -> {
            LevelLightEngine levellightengine = this.level.getLightEngine();
            levellightengine.setLightEnabled(chunkpos, false);

            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
                SectionPos sectionpos = SectionPos.of(chunkpos, i);
                levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, null);
                levellightengine.queueSectionData(LightLayer.SKY, sectionpos, null);
            }

            for (int j = this.level.getMinSectionY(); j <= this.level.getMaxSectionY(); j++) {
                levellightengine.updateSectionStatus(SectionPos.of(chunkpos, j), true);
            }
        });
    }

    /**
     * Updates the block and metadata and generates a blockupdate (and notify the clients)
     */
    @Override
    public void handleBlockUpdate(ClientboundBlockUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.setServerVerifiedBlockState(packet.getPos(), packet.getBlockState(), 19);
    }

    @Override
    public void handleConfigurationStart(ClientboundStartConfigurationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getChatListener().flushQueue();
        this.sendChatAcknowledgement();
        ChatComponent.State chatcomponent$state = this.minecraft.gui.getChat().storeState();
        this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
        this.connection
            .setupInboundProtocol(
                ConfigurationProtocols.CLIENTBOUND,
                new ClientConfigurationPacketListenerImpl(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        new LevelLoadTracker(),
                        this.localGameProfile,
                        this.telemetryManager,
                        this.registryAccess,
                        this.enabledFeatures,
                        this.serverBrand,
                        this.serverData,
                        this.postDisconnectScreen,
                        this.serverCookies,
                        chatcomponent$state,
                        this.customReportDetails,
                        this.serverLinks(),
                        this.seenPlayers,
                        this.seenInsecureChatWarning,
                        this.connectionType
                    )
                )
            );
        this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
    }

    @Override
    public void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getItemId());
        LivingEntity livingentity = (LivingEntity)this.level.getEntity(packet.getPlayerId());
        if (livingentity == null) {
            livingentity = this.minecraft.player;
        }

        if (entity != null) {
            if (entity instanceof ExperienceOrb) {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.1F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F,
                        false
                    );
            } else {
                this.level
                    .playLocalSound(
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.2F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F,
                        false
                    );
            }

            EntityRenderState entityrenderstate = this.minecraft.getEntityRenderDispatcher().extractEntity(entity, 1.0F);
            this.minecraft.particleEngine.add(new ItemPickupParticle(this.level, entityrenderstate, livingentity, entity.getDeltaMovement()));
            if (entity instanceof ItemEntity itementity) {
                ItemStack itemstack = itementity.getItem();
                if (!itemstack.isEmpty()) {
                    itemstack.shrink(packet.getAmount());
                }

                if (itemstack.isEmpty()) {
                    this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
                }
            } else if (!(entity instanceof ExperienceOrb)) {
                this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void handleSystemChat(ClientboundSystemChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getChatListener().handleSystemMessage(packet.content(), packet.overlay());
    }

    @Override
    public void handlePlayerChat(ClientboundPlayerChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int i = this.nextChatIndex++;
        if (packet.globalIndex() != i) {
            LOGGER.error("Missing or out-of-order chat message from server, expected index {} but got {}", i, packet.globalIndex());
            this.connection.disconnect(BAD_CHAT_INDEX);
        } else {
            Optional<SignedMessageBody> optional = packet.body().unpack(this.messageSignatureCache);
            if (optional.isEmpty()) {
                LOGGER.error("Message from player with ID {} referenced unrecognized signature id", packet.sender());
                this.connection.disconnect(INVALID_PACKET);
            } else {
                this.messageSignatureCache.push(optional.get(), packet.signature());
                UUID uuid = packet.sender();
                PlayerInfo playerinfo = this.getPlayerInfo(uuid);
                if (playerinfo == null) {
                    LOGGER.error("Received player chat packet for unknown player with ID: {}", uuid);
                    this.minecraft.getChatListener().handleChatMessageError(uuid, packet.signature(), packet.chatType());
                } else {
                    RemoteChatSession remotechatsession = playerinfo.getChatSession();
                    SignedMessageLink signedmessagelink;
                    if (remotechatsession != null) {
                        signedmessagelink = new SignedMessageLink(packet.index(), uuid, remotechatsession.sessionId());
                    } else {
                        signedmessagelink = SignedMessageLink.unsigned(uuid);
                    }

                    PlayerChatMessage playerchatmessage = new PlayerChatMessage(
                        signedmessagelink, packet.signature(), optional.get(), packet.unsignedContent(), packet.filterMask()
                    );
                    playerchatmessage = playerinfo.getMessageValidator().updateAndValidate(playerchatmessage);
                    if (playerchatmessage != null) {
                        this.minecraft.getChatListener().handlePlayerChatMessage(playerchatmessage, playerinfo.getProfile(), packet.chatType());
                    } else {
                        this.minecraft.getChatListener().handleChatMessageError(uuid, packet.signature(), packet.chatType());
                    }
                }
            }
        }
    }

    @Override
    public void handleDisguisedChat(ClientboundDisguisedChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getChatListener().handleDisguisedChatMessage(packet.message(), packet.chatType());
    }

    @Override
    public void handleDeleteChat(ClientboundDeleteChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Optional<MessageSignature> optional = packet.messageSignature().unpack(this.messageSignatureCache);
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
        } else {
            this.lastSeenMessages.ignorePending(optional.get());
            if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue(optional.get())) {
                this.minecraft.gui.getChat().deleteMessage(optional.get());
            }
        }
    }

    /**
     * Renders a specified animation: Waking up a player, a living entity swinging its currently held item, being hurt or receiving a critical hit by normal or magical means
     */
    @Override
    public void handleAnimate(ClientboundAnimatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity != null) {
            if (packet.getAction() == 0) {
                LivingEntity livingentity = (LivingEntity)entity;
                livingentity.swing(InteractionHand.MAIN_HAND);
            } else if (packet.getAction() == 3) {
                LivingEntity livingentity1 = (LivingEntity)entity;
                livingentity1.swing(InteractionHand.OFF_HAND);
            } else if (packet.getAction() == 2) {
                Player player = (Player)entity;
                player.stopSleepInBed(false, false);
            } else if (packet.getAction() == 4) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
            } else if (packet.getAction() == 5) {
                this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }

    @Override
    public void handleHurtAnimation(ClientboundHurtAnimationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity != null) {
            entity.animateHurt(packet.yaw());
        }
    }

    @Override
    public void handleSetTime(ClientboundSetTimePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.setTimeFromServer(packet.gameTime(), packet.dayTime(), packet.tickDayTime());
        this.telemetryManager.setTime(packet.gameTime());
    }

    @Override
    public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.setRespawnData(packet.respawnData());
    }

    @Override
    public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getVehicle());
        if (entity == null) {
            LOGGER.warn("Received passengers for unknown entity");
        } else {
            boolean flag = entity.hasIndirectPassenger(this.minecraft.player);
            entity.ejectPassengers();

            for (int i : packet.getPassengers()) {
                Entity entity1 = this.level.getEntity(i);
                if (entity1 != null) {
                    entity1.startRiding(entity, true, false);
                    if (entity1 == this.minecraft.player) {
                        this.removedPlayerVehicleId = OptionalInt.empty();
                        if (!flag) {
                            if (entity instanceof AbstractBoat) {
                                this.minecraft.player.yRotO = entity.getYRot();
                                this.minecraft.player.setYRot(entity.getYRot());
                                this.minecraft.player.setYHeadRot(entity.getYRot());
                            }

                            Component component = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                            this.minecraft.gui.setOverlayMessage(component, false);
                            this.minecraft.getNarrator().saySystemNow(component);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.level.getEntity(packet.getSourceId()) instanceof Leashable leashable) {
            leashable.setDelayedLeashHolderId(packet.getDestId());
        }
    }

    private static ItemStack findTotem(Player player) {
        for (InteractionHand interactionhand : InteractionHand.values()) {
            ItemStack itemstack = player.getItemInHand(interactionhand);
            if (itemstack.has(DataComponents.DEATH_PROTECTION)) {
                return itemstack;
            }
        }

        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    /**
     * Invokes the entities' handleUpdateHealth method which is implemented in LivingBase (hurt/death), MinecartMobSpawner (spawn delay), FireworkRocket & MinecartTNT (explosion), IronGolem (throwing, ...), Witch (spawn particles), Zombie (villager transformation), Animal (breeding mode particles), Horse (breeding/smoke particles), Sheep (...), Tameable (...), Villager (particles for breeding mode, angry and happy), Wolf (...)
     */
    @Override
    public void handleEntityEvent(ClientboundEntityEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            switch (packet.getEventId()) {
                case 21:
                    this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
                    break;
                case 35:
                    int i = 40;
                    this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                    this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0F, 1.0F, false);
                    if (entity == this.minecraft.player) {
                        this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
                    }
                    break;
                case 63:
                    this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
                    break;
                default:
                    entity.handleEntityEvent(packet.getEventId());
            }
        }
    }

    @Override
    public void handleDamageEvent(ClientboundDamageEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.entityId());
        if (entity != null) {
            entity.handleDamageEvent(packet.getSource(this.level));
        }
    }

    @Override
    public void handleSetHealth(ClientboundSetHealthPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.hurtTo(packet.getHealth());
        this.minecraft.player.getFoodData().setFoodLevel(packet.getFood());
        this.minecraft.player.getFoodData().setSaturation(packet.getSaturation());
    }

    @Override
    public void handleSetExperience(ClientboundSetExperiencePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.setExperienceValues(packet.getExperienceProgress(), packet.getTotalExperience(), packet.getExperienceLevel());
    }

    @Override
    public void handleRespawn(ClientboundRespawnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        CommonPlayerSpawnInfo commonplayerspawninfo = packet.commonPlayerSpawnInfo();
        ResourceKey<Level> resourcekey = commonplayerspawninfo.dimension();
        Holder<DimensionType> holder = commonplayerspawninfo.dimensionType();
        LocalPlayer localplayer = this.minecraft.player;
        ResourceKey<Level> resourcekey1 = localplayer.level().dimension();
        boolean flag = resourcekey != resourcekey1;
        LevelLoadingScreen.Reason levelloadingscreen$reason = this.determineLevelLoadingReason(localplayer.isDeadOrDying(), resourcekey, resourcekey1);
        if (flag) {
            Map<MapId, MapItemSavedData> map = this.level.getAllMapData();
            boolean flag1 = commonplayerspawninfo.isDebug();
            boolean flag2 = commonplayerspawninfo.isFlat();
            int i = commonplayerspawninfo.seaLevel();
            ClientLevel.ClientLevelData clientlevel$clientleveldata = new ClientLevel.ClientLevelData(
                this.levelData.getDifficulty(), this.levelData.isHardcore(), flag2
            );
            this.levelData = clientlevel$clientleveldata;
            this.level = new ClientLevel(
                this,
                clientlevel$clientleveldata,
                resourcekey,
                holder,
                this.serverChunkRadius,
                this.serverSimulationDistance,
                this.minecraft.levelRenderer,
                flag1,
                commonplayerspawninfo.seed(),
                i
            );
            this.level.addMapData(map);
            this.minecraft.setLevel(this.level);
            this.debugSubscriber.dropLevel();
        }

        this.minecraft.setCameraEntity(null);
        if (localplayer.hasContainerOpen()) {
            localplayer.closeContainer();
        }

        LocalPlayer localplayer1;
        if (packet.shouldKeep((byte)2)) {
            localplayer1 = this.minecraft
                .gameMode
                .createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook(), localplayer.getLastSentInput(), localplayer.isSprinting());
        } else {
            localplayer1 = this.minecraft.gameMode.createPlayer(this.level, localplayer.getStats(), localplayer.getRecipeBook());
        }

        this.startWaitingForNewLevel(localplayer1, this.level, levelloadingscreen$reason, localplayer.isDeadOrDying() ? null : resourcekey, localplayer.isDeadOrDying() ? null : resourcekey1);
        localplayer1.setId(localplayer.getId());
        this.minecraft.player = localplayer1;
        if (flag) {
            this.minecraft.getMusicManager().stopPlaying();
        }

        this.minecraft.setCameraEntity(localplayer1);
        if (packet.shouldKeep((byte)2)) {
            List<SynchedEntityData.DataValue<?>> list = localplayer.getEntityData().getNonDefaultValues();
            if (list != null) {
                localplayer1.getEntityData().assignValues(list);
            }

            localplayer1.setDeltaMovement(localplayer.getDeltaMovement());
            localplayer1.setYRot(localplayer.getYRot());
            localplayer1.setXRot(localplayer.getXRot());
        } else {
            localplayer1.resetPos();
            localplayer1.setYRot(-180.0F);
        }

        if (packet.shouldKeep((byte)1)) {
            localplayer1.getAttributes().assignAllValues(localplayer.getAttributes());
        } else {
            localplayer1.getAttributes().assignBaseValues(localplayer.getAttributes());
        }

        net.neoforged.neoforge.client.ClientHooks.firePlayerRespawn(this.minecraft.gameMode, localplayer, localplayer1, localplayer1.connection.connection);
        this.level.addEntity(localplayer1);
        localplayer1.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(localplayer1);
        localplayer1.setReducedDebugInfo(localplayer.isReducedDebugInfo());
        localplayer1.setShowDeathScreen(localplayer.shouldShowDeathScreen());
        localplayer1.setLastDeathLocation(commonplayerspawninfo.lastDeathLocation());
        localplayer1.setPortalCooldown(commonplayerspawninfo.portalCooldown());
        localplayer1.portalEffectIntensity = localplayer.portalEffectIntensity;
        localplayer1.oPortalEffectIntensity = localplayer.oPortalEffectIntensity;
        if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
            this.minecraft.setScreen(null);
        }

        this.minecraft.gameMode.setLocalMode(commonplayerspawninfo.gameType(), commonplayerspawninfo.previousGameType());
    }

    private LevelLoadingScreen.Reason determineLevelLoadingReason(boolean fromDeath, ResourceKey<Level> spawnDimension, ResourceKey<Level> playerDimension) {
        LevelLoadingScreen.Reason levelloadingscreen$reason = LevelLoadingScreen.Reason.OTHER;
        if (!fromDeath) {
            if (spawnDimension == Level.NETHER || playerDimension == Level.NETHER) {
                levelloadingscreen$reason = LevelLoadingScreen.Reason.NETHER_PORTAL;
            } else if (spawnDimension == Level.END || playerDimension == Level.END) {
                levelloadingscreen$reason = LevelLoadingScreen.Reason.END_PORTAL;
            }
        }

        return levelloadingscreen$reason;
    }

    /**
     * Initiates a new explosion (sound, particles, drop spawn) for the affected blocks indicated by the packet.
     */
    @Override
    public void handleExplosion(ClientboundExplodePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Vec3 vec3 = packet.center();
        this.minecraft
            .level
            .playLocalSound(
                vec3.x(),
                vec3.y(),
                vec3.z(),
                packet.explosionSound().value(),
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (this.minecraft.level.random.nextFloat() - this.minecraft.level.random.nextFloat()) * 0.2F) * 0.7F,
                false
            );
        this.minecraft.level.addParticle(packet.explosionParticle(), vec3.x(), vec3.y(), vec3.z(), 1.0, 0.0, 0.0);
        this.minecraft.level.trackExplosionEffects(vec3, packet.radius(), packet.blockCount(), packet.blockParticles());
        packet.playerKnockback().ifPresent(this.minecraft.player::addDeltaMovement);
    }

    @Override
    public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.level.getEntity(packet.getEntityId()) instanceof AbstractHorse abstracthorse) {
            LocalPlayer localplayer = this.minecraft.player;
            int i = packet.getInventoryColumns();
            SimpleContainer simplecontainer = new SimpleContainer(AbstractHorse.getInventorySize(i));
            HorseInventoryMenu horseinventorymenu = new HorseInventoryMenu(
                packet.getContainerId(), localplayer.getInventory(), simplecontainer, abstracthorse, i
            );
            localplayer.containerMenu = horseinventorymenu;
            this.minecraft.setScreen(new HorseInventoryScreen(horseinventorymenu, localplayer.getInventory(), abstracthorse, i));
        }
    }

    @Override
    public void handleOpenScreen(ClientboundOpenScreenPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        MenuScreens.create(packet.getType(), this.minecraft, packet.getContainerId(), packet.getTitle());
    }

    /**
     * Handles picking up an ItemStack or dropping one in your inventory or an open (non-creative) container
     */
    @Override
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        ItemStack itemstack = packet.getItem();
        int i = packet.getSlot();
        this.minecraft.getTutorial().onGetItem(itemstack);
        boolean flag;
        if (this.minecraft.screen instanceof CreativeModeInventoryScreen creativemodeinventoryscreen) {
            flag = !creativemodeinventoryscreen.isInventoryOpen();
        } else {
            flag = false;
        }

        if (packet.getContainerId() == 0) {
            if (InventoryMenu.isHotbarSlot(i) && !itemstack.isEmpty()) {
                ItemStack itemstack1 = player.inventoryMenu.getSlot(i).getItem();
                if (itemstack1.isEmpty() || itemstack1.getCount() < itemstack.getCount()) {
                    itemstack.setPopTime(5);
                }
            }

            player.inventoryMenu.setItem(i, packet.getStateId(), itemstack);
        } else if (packet.getContainerId() == player.containerMenu.containerId && (packet.getContainerId() != 0 || !flag)) {
            player.containerMenu.setItem(i, packet.getStateId(), itemstack);
        }

        if (this.minecraft.screen instanceof CreativeModeInventoryScreen) {
            player.inventoryMenu.setRemoteSlot(i, itemstack);
            player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public void handleSetCursorItem(ClientboundSetCursorItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getTutorial().onGetItem(packet.contents());
        if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
            this.minecraft.player.containerMenu.setCarried(packet.contents());
        }
    }

    @Override
    public void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getTutorial().onGetItem(packet.contents());
        this.minecraft.player.getInventory().setItem(packet.slot(), packet.contents());
    }

    /**
     * Handles the placement of a specified ItemStack in a specified container/inventory slot
     */
    @Override
    public void handleContainerContent(ClientboundContainerSetContentPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        if (packet.containerId() == 0) {
            player.inventoryMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
        } else if (packet.containerId() == player.containerMenu.containerId) {
            player.containerMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
        }
    }

    /**
     * Creates a sign in the specified location if it didn't exist and opens the GUI to edit its text
     */
    @Override
    public void handleOpenSignEditor(ClientboundOpenSignEditorPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        BlockPos blockpos = packet.getPos();
        if (this.level.getBlockEntity(blockpos) instanceof SignBlockEntity signblockentity) {
            this.minecraft.player.openTextEdit(signblockentity, packet.isFrontText());
        } else {
            LOGGER.warn("Ignoring openTextEdit on an invalid entity: {} at pos {}", this.level.getBlockEntity(blockpos), blockpos);
        }
    }

    /**
     * Updates the NBTTagCompound metadata of instances of the following entitytypes: Mob spawners, command blocks, beacons, skulls, flowerpot
     */
    @Override
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        BlockPos blockpos = packet.getPos();
        this.minecraft.level.getBlockEntity(blockpos, packet.getType()).ifPresent(p_421289_ -> {
            ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_421289_.problemPath(), LOGGER);

            try {
                p_421289_.onDataPacket(connection, TagValueInput.create(problemreporter$scopedcollector, this.registryAccess, packet.getTag()));
            } catch (Throwable throwable1) {
                try {
                    problemreporter$scopedcollector.close();
                } catch (Throwable throwable) {
                    throwable1.addSuppressed(throwable);
                }

                throw throwable1;
            }

            problemreporter$scopedcollector.close();
            if (p_421289_ instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
                ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
            }
        });
    }

    /**
     * Sets the progressbar of the opened window to the specified value
     */
    @Override
    public void handleContainerSetData(ClientboundContainerSetDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        if (player.containerMenu.containerId == packet.getContainerId()) {
            player.containerMenu.setData(packet.getId(), packet.getValue());
        }
    }

    @Override
    public void handleSetEquipment(ClientboundSetEquipmentPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.level.getEntity(packet.getEntity()) instanceof LivingEntity livingentity) {
            packet.getSlots().forEach(p_323056_ -> livingentity.setItemSlot(p_323056_.getFirst(), p_323056_.getSecond()));
        }
    }

    /**
     * Resets the ItemStack held in hand and closes the window that is opened
     */
    @Override
    public void handleContainerClose(ClientboundContainerClosePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.clientSideCloseContainer();
    }

    /**
     * Triggers Block.onBlockEventReceived, which is implemented in BlockPistonBase for extension/retraction, BlockNote for setting the instrument (including audiovisual feedback) and in BlockContainer to set the number of players accessing a (Ender)Chest
     */
    @Override
    public void handleBlockEvent(ClientboundBlockEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.blockEvent(packet.getPos(), packet.getBlock(), packet.getB0(), packet.getB1());
    }

    /**
     * Updates all registered IWorldAccess instances with destroyBlockInWorldPartially
     */
    @Override
    public void handleBlockDestruction(ClientboundBlockDestructionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.destroyBlockProgress(packet.getId(), packet.getPos(), packet.getProgress());
    }

    @Override
    public void handleGameEvent(ClientboundGameEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        ClientboundGameEventPacket.Type clientboundgameeventpacket$type = packet.getEvent();
        float f = packet.getParam();
        int i = Mth.floor(f + 0.5F);
        if (clientboundgameeventpacket$type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
            player.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.START_RAINING) {
            this.level.getLevelData().setRaining(true);
            this.level.setRainLevel(0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.STOP_RAINING) {
            this.level.getLevelData().setRaining(false);
            this.level.setRainLevel(1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            this.minecraft.gameMode.setLocalMode(GameType.byId(i));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.WIN_GAME) {
            this.minecraft.setScreen(new WinScreen(true, () -> {
                this.minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                this.minecraft.setScreen(null);
            }));
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.DEMO_EVENT) {
            Options options = this.minecraft.options;
            Component component = null;
            if (f == 0.0F) {
                this.minecraft.setScreen(new DemoIntroScreen());
            } else if (f == 101.0F) {
                component = Component.translatable(
                    "demo.help.movement",
                    options.keyUp.getTranslatedKeyMessage(),
                    options.keyLeft.getTranslatedKeyMessage(),
                    options.keyDown.getTranslatedKeyMessage(),
                    options.keyRight.getTranslatedKeyMessage()
                );
            } else if (f == 102.0F) {
                component = Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage());
            } else if (f == 103.0F) {
                component = Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage());
            } else if (f == 104.0F) {
                component = Component.translatable("demo.day.6", options.keyScreenshot.getTranslatedKeyMessage());
            }

            if (component != null) {
                this.minecraft.gui.getChat().addMessage(component);
                this.minecraft.getNarrator().saySystemQueued(component);
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND) {
            this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.level.setRainLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.level.setThunderLevel(f);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
            this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
            if (i == 1) {
                this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
            this.minecraft.player.setShowDeathScreen(f == 0.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LIMITED_CRAFTING) {
            this.minecraft.player.setDoLimitedCrafting(f == 1.0F);
        } else if (clientboundgameeventpacket$type == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadTracker != null) {
            this.levelLoadTracker.loadingPacketsReceived();
        }
    }

    /**
 * @deprecated Neo: use {@link #startWaitingForNewLevel(LocalPlayer, ClientLevel,
 *             LevelLoadingScreen.Reason, ResourceKey, ResourceKey)} instead.
 */
    @Deprecated
    private void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, LevelLoadingScreen.Reason reason) {
        this.startWaitingForNewLevel(player, level, reason, null, null);
    }

    private void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, LevelLoadingScreen.Reason reason, @Nullable ResourceKey<Level> toDimension, @Nullable ResourceKey<Level> fromDimension) {
        if (this.levelLoadTracker == null) {
            this.levelLoadTracker = new LevelLoadTracker();
        }

        this.levelLoadTracker.startClientLoad(player, level, this.minecraft.levelRenderer);
        if (this.minecraft.screen instanceof LevelLoadingScreen levelloadingscreen) {
            levelloadingscreen.update(this.levelLoadTracker, reason);
        } else {
            this.minecraft.gui.getChat().preserveCurrentChatScreen();
            this.minecraft.setScreenAndShow(net.neoforged.neoforge.client.DimensionTransitionScreenManager.getScreen(toDimension, fromDimension).create(this.levelLoadTracker, reason));
        }
    }

    /**
     * Updates the worlds MapStorage with the specified MapData for the specified map-identifier and invokes a MapItemRenderer for it
     */
    @Override
    public void handleMapItemData(ClientboundMapItemDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        MapId mapid = packet.mapId();
        MapItemSavedData mapitemsaveddata = this.minecraft.level.getMapData(mapid);
        if (mapitemsaveddata == null) {
            mapitemsaveddata = MapItemSavedData.createForClient(packet.scale(), packet.locked(), this.minecraft.level.dimension());
            this.minecraft.level.overrideMapData(mapid, mapitemsaveddata);
        }

        packet.applyToMap(mapitemsaveddata);
        this.minecraft.getMapTextureManager().update(mapid, mapitemsaveddata);
    }

    @Override
    public void handleLevelEvent(ClientboundLevelEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.isGlobalEvent()) {
            this.minecraft.level.globalLevelEvent(packet.getType(), packet.getPos(), packet.getData());
        } else {
            this.minecraft.level.levelEvent(packet.getType(), packet.getPos(), packet.getData());
        }
    }

    @Override
    public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.advancements.update(packet);
    }

    @Override
    public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ResourceLocation resourcelocation = packet.getTab();
        if (resourcelocation == null) {
            this.advancements.setSelectedTab(null, false);
        } else {
            AdvancementHolder advancementholder = this.advancements.get(resourcelocation);
            this.advancements.setSelectedTab(advancementholder, false);
        }
    }

    @Override
    public void handleCommands(ClientboundCommandsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        var context = CommandBuildContext.simple(this.registryAccess, this.enabledFeatures);
        this.commands = new CommandDispatcher<>(packet.getRoot(context, COMMAND_NODE_BUILDER));
        this.commands = net.neoforged.neoforge.client.ClientCommandHandler.mergeServerCommands(this.commands, context);
    }

    @Override
    public void handleStopSoundEvent(ClientboundStopSoundPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getSoundManager().stop(packet.getName(), packet.getSource());
    }

    /**
     * This method is only called for manual tab-completion (the {@link net.minecraft.commands.synchronization.SuggestionProviders#ASK_SERVER minecraft:ask_server} suggestion provider).
     */
    @Override
    public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.suggestionsProvider.completeCustomSuggestions(packet.id(), packet.toSuggestions());
    }

    @Override
    public void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.recipes = new ClientRecipeContainer(packet.itemSets(), packet.stonecutterRecipes());

        net.neoforged.neoforge.client.ClientHooks.handleUpdateRecipes(this, v -> this.fuelValues = v);
    }

    @Override
    public void handleLookAt(ClientboundPlayerLookAtPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Vec3 vec3 = packet.getPosition(this.level);
        if (vec3 != null) {
            this.minecraft.player.lookAt(packet.getFromAnchor(), vec3);
        }
    }

    @Override
    public void handleTagQueryPacket(ClientboundTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (!this.debugQueryHandler.handleResponse(packet.getTransactionId(), packet.getTag())) {
            LOGGER.debug("Got unhandled response to tag query {}", packet.getTransactionId());
        }
    }

    /**
     * Updates the players statistics or achievements
     */
    @Override
    public void handleAwardStats(ClientboundAwardStatsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

        for (Entry<Stat<?>> entry : packet.stats().object2IntEntrySet()) {
            Stat<?> stat = entry.getKey();
            int i = entry.getIntValue();
            this.minecraft.player.getStats().setValue(this.minecraft.player, stat, i);
        }

        if (this.minecraft.screen instanceof StatsScreen statsscreen) {
            statsscreen.onStatsUpdated();
        }
    }

    @Override
    public void handleRecipeBookAdd(ClientboundRecipeBookAddPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        if (packet.replace()) {
            clientrecipebook.clear();
        }

        for (ClientboundRecipeBookAddPacket.Entry clientboundrecipebookaddpacket$entry : packet.entries()) {
            clientrecipebook.add(clientboundrecipebookaddpacket$entry.contents());
            if (clientboundrecipebookaddpacket$entry.highlight()) {
                clientrecipebook.addHighlight(clientboundrecipebookaddpacket$entry.contents().id());
            }

            if (clientboundrecipebookaddpacket$entry.notification()) {
                RecipeToast.addOrUpdate(this.minecraft.getToastManager(), clientboundrecipebookaddpacket$entry.contents().display());
            }
        }

        this.refreshRecipeBook(clientrecipebook);
    }

    @Override
    public void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();

        for (RecipeDisplayId recipedisplayid : packet.recipes()) {
            clientrecipebook.remove(recipedisplayid);
        }

        this.refreshRecipeBook(clientrecipebook);
    }

    @Override
    public void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook clientrecipebook = this.minecraft.player.getRecipeBook();
        clientrecipebook.setBookSettings(packet.bookSettings());
        this.refreshRecipeBook(clientrecipebook);
    }

    private void refreshRecipeBook(ClientRecipeBook recipeBook) {
        recipeBook.rebuildCollections();
        this.searchTrees.updateRecipes(recipeBook, this.level);
        if (this.minecraft.screen instanceof RecipeUpdateListener recipeupdatelistener) {
            recipeupdatelistener.recipesUpdated();
        }
    }

    @Override
    public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntityId());
        if (entity instanceof LivingEntity) {
            Holder<MobEffect> holder = packet.getEffect();
            MobEffectInstance mobeffectinstance = new MobEffectInstance(
                holder,
                packet.getEffectDurationTicks(),
                packet.getEffectAmplifier(),
                packet.isEffectAmbient(),
                packet.isEffectVisible(),
                packet.effectShowsIcon(),
                null
            );
            if (!packet.shouldBlend()) {
                mobeffectinstance.skipBlending();
            }

            ((LivingEntity)entity).forceAddEffect(mobeffectinstance, null);
        }
    }

    private <T> Registry.PendingTags<T> updateTags(ResourceKey<? extends Registry<? extends T>> registryKey, TagNetworkSerialization.NetworkPayload payload) {
        Registry<T> registry = this.registryAccess.lookupOrThrow(registryKey);
        return registry.prepareTagReload(payload.resolve(registry));
    }

    @Override
    public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        List<Registry.PendingTags<?>> list = new ArrayList<>(packet.getTags().size());
        boolean flag = this.connection.isMemoryConnection();
        packet.getTags().forEach((p_359138_, p_359139_) -> {
            if (!flag || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_359138_)) {
                list.add(this.updateTags((ResourceKey<? extends Registry<?>>)p_359138_, p_359139_));
            }
        });
        list.forEach(Registry.PendingTags::apply);
        this.fuelValues = FuelValues.vanillaBurnTimes(this.registryAccess, this.enabledFeatures);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.TagsUpdatedEvent(this.registryAccess, true, flag));
        CreativeModeTabs.allTabs().stream().filter(net.minecraft.world.item.CreativeModeTab::hasSearchBar).forEach(tab -> {
            List<ItemStack> stacks = List.copyOf(tab.getDisplayItems());
            this.searchTrees.updateCreativeTags(stacks, net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getTagSearchKey(tab));
        });
    }

    @Override
    public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket packet) {
    }

    @Override
    public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket packet) {
    }

    @Override
    public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.playerId());
        if (entity == this.minecraft.player) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                this.minecraft.setScreen(new DeathScreen(packet.message(), this.level.getLevelData().isHardcore()));
            } else {
                this.minecraft.player.respawn();
            }
        }
    }

    @Override
    public void handleChangeDifficulty(ClientboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.levelData.setDifficulty(packet.difficulty());
        this.levelData.setDifficultyLocked(packet.locked());
    }

    @Override
    public void handleSetCamera(ClientboundSetCameraPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            this.minecraft.setCameraEntity(entity);
        }
    }

    @Override
    public void handleInitializeBorder(ClientboundInitializeBorderPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        WorldBorder worldborder = this.level.getWorldBorder();
        worldborder.setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
        long i = packet.getLerpTime();
        if (i > 0L) {
            worldborder.lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), i);
        } else {
            worldborder.setSize(packet.getNewSize());
        }

        worldborder.setAbsoluteMaxSize(packet.getNewAbsoluteMaxSize());
        worldborder.setWarningBlocks(packet.getWarningBlocks());
        worldborder.setWarningTime(packet.getWarningTime());
    }

    @Override
    public void handleSetBorderCenter(ClientboundSetBorderCenterPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
    }

    @Override
    public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), packet.getLerpTime());
    }

    @Override
    public void handleSetBorderSize(ClientboundSetBorderSizePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setSize(packet.getSize());
    }

    @Override
    public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setWarningBlocks(packet.getWarningBlocks());
    }

    @Override
    public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setWarningTime(packet.getWarningDelay());
    }

    @Override
    public void handleTitlesClear(ClientboundClearTitlesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.clearTitles();
        if (packet.shouldResetTimes()) {
            this.minecraft.gui.resetTitleTimes();
        }
    }

    @Override
    public void handleServerData(ClientboundServerDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.serverData != null) {
            this.serverData.motd = packet.motd();
            packet.iconBytes().map(ServerData::validateIcon).ifPresent(this.serverData::setIconBytes);
            ServerList.saveSingleServer(this.serverData);
        }
    }

    @Override
    public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.suggestionsProvider.modifyCustomCompletions(packet.action(), packet.entries());
    }

    @Override
    public void setActionBarText(ClientboundSetActionBarTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.setOverlayMessage(packet.text(), false);
    }

    @Override
    public void setTitleText(ClientboundSetTitleTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.setTitle(packet.text());
    }

    @Override
    public void setSubtitleText(ClientboundSetSubtitleTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.setSubtitle(packet.text());
    }

    @Override
    public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.setTimes(packet.getFadeIn(), packet.getStay(), packet.getFadeOut());
    }

    @Override
    public void handleTabListCustomisation(ClientboundTabListPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.getTabList().setHeader(packet.header().getString().isEmpty() ? null : packet.header());
        this.minecraft.gui.getTabList().setFooter(packet.footer().getString().isEmpty() ? null : packet.footer());
    }

    @Override
    public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.getEntity(this.level) instanceof LivingEntity livingentity) {
            livingentity.removeEffectNoUpdate(packet.effect());
        }
    }

    @Override
    public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

        for (UUID uuid : packet.profileIds()) {
            this.minecraft.getPlayerSocialManager().removePlayer(uuid);
            PlayerInfo playerinfo = this.playerInfoMap.remove(uuid);
            if (playerinfo != null) {
                this.listedPlayers.remove(playerinfo);
            }
        }
    }

    @Override
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry : packet.newEntries()) {
            PlayerInfo playerinfo = new PlayerInfo(Objects.requireNonNull(clientboundplayerinfoupdatepacket$entry.profile()), this.enforcesSecureChat());
            if (this.playerInfoMap.putIfAbsent(clientboundplayerinfoupdatepacket$entry.profileId(), playerinfo) == null) {
                this.minecraft.getPlayerSocialManager().addPlayer(playerinfo);
            }
        }

        for (ClientboundPlayerInfoUpdatePacket.Entry clientboundplayerinfoupdatepacket$entry1 : packet.entries()) {
            PlayerInfo playerinfo1 = this.playerInfoMap.get(clientboundplayerinfoupdatepacket$entry1.profileId());
            if (playerinfo1 == null) {
                LOGGER.warn("Ignoring player info update for unknown player {} ({})", clientboundplayerinfoupdatepacket$entry1.profileId(), packet.actions());
            } else {
                for (ClientboundPlayerInfoUpdatePacket.Action clientboundplayerinfoupdatepacket$action : packet.actions()) {
                    this.applyPlayerInfoUpdate(clientboundplayerinfoupdatepacket$action, clientboundplayerinfoupdatepacket$entry1, playerinfo1);
                }
            }
        }
    }

    private void applyPlayerInfoUpdate(
        ClientboundPlayerInfoUpdatePacket.Action action, ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo
    ) {
        switch (action) {
            case INITIALIZE_CHAT:
                this.initializeChatSession(entry, playerInfo);
                break;
            case UPDATE_GAME_MODE:
                if (playerInfo.getGameMode() != entry.gameMode()
                    && this.minecraft.player != null
                    && this.minecraft.player.getUUID().equals(entry.profileId())) {
                    this.minecraft.player.onGameModeChanged(entry.gameMode());
                }

                playerInfo.setGameMode(entry.gameMode());
                break;
            case UPDATE_LISTED:
                if (entry.listed()) {
                    this.listedPlayers.add(playerInfo);
                } else {
                    this.listedPlayers.remove(playerInfo);
                }
                break;
            case UPDATE_LATENCY:
                playerInfo.setLatency(entry.latency());
                break;
            case UPDATE_DISPLAY_NAME:
                playerInfo.setTabListDisplayName(entry.displayName());
                break;
            case UPDATE_HAT:
                playerInfo.setShowHat(entry.showHat());
                break;
            case UPDATE_LIST_ORDER:
                playerInfo.setTabListOrder(entry.listOrder());
        }
    }

    private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
        GameProfile gameprofile = playerInfo.getProfile();
        SignatureValidator signaturevalidator = this.minecraft.services().profileKeySignatureValidator();
        if (signaturevalidator == null) {
            LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameprofile.name());
            playerInfo.clearChatSession(this.enforcesSecureChat());
        } else {
            RemoteChatSession.Data remotechatsession$data = entry.chatSession();
            if (remotechatsession$data != null) {
                try {
                    RemoteChatSession remotechatsession = remotechatsession$data.validate(gameprofile, signaturevalidator);
                    playerInfo.setChatSession(remotechatsession);
                } catch (ProfilePublicKey.ValidationException profilepublickey$validationexception) {
                    LOGGER.error("Failed to validate profile key for player: '{}'", gameprofile.name(), profilepublickey$validationexception);
                    playerInfo.clearChatSession(this.enforcesSecureChat());
                }
            } else {
                playerInfo.clearChatSession(this.enforcesSecureChat());
            }
        }
    }

    private boolean enforcesSecureChat() {
        return this.minecraft.services().canValidateProfileKeys() && this.serverEnforcesSecureChat;
    }

    @Override
    public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Player player = this.minecraft.player;
        player.getAbilities().flying = packet.isFlying();
        player.getAbilities().instabuild = packet.canInstabuild();
        player.getAbilities().invulnerable = packet.isInvulnerable();
        player.getAbilities().mayfly = packet.canFly();
        player.getAbilities().setFlyingSpeed(packet.getFlyingSpeed());
        player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
    }

    @Override
    public void handleSoundEvent(ClientboundSoundPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft
            .level
            .playSeededSound(
                this.minecraft.player,
                packet.getX(),
                packet.getY(),
                packet.getZ(),
                packet.getSound(),
                packet.getSource(),
                packet.getVolume(),
                packet.getPitch(),
                packet.getSeed()
            );
    }

    @Override
    public void handleSoundEntityEvent(ClientboundSoundEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity != null) {
            this.minecraft
                .level
                .playSeededSound(
                    this.minecraft.player,
                    entity,
                    packet.getSound(),
                    packet.getSource(),
                    packet.getVolume(),
                    packet.getPitch(),
                    packet.getSeed()
                );
        }
    }

    @Override
    public void handleBossUpdate(ClientboundBossEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.getBossOverlay().update(packet);
    }

    @Override
    public void handleItemCooldown(ClientboundCooldownPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.duration() == 0) {
            this.minecraft.player.getCooldowns().removeCooldown(packet.cooldownGroup());
        } else {
            this.minecraft.player.getCooldowns().addCooldown(packet.cooldownGroup(), packet.duration());
        }
    }

    @Override
    public void handleMoveVehicle(ClientboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.minecraft.player.getRootVehicle();
        if (entity != this.minecraft.player && entity.isLocalInstanceAuthoritative()) {
            Vec3 vec3 = packet.position();
            Vec3 vec31;
            if (entity.isInterpolating()) {
                vec31 = entity.getInterpolation().position();
            } else {
                vec31 = entity.position();
            }

            if (vec3.distanceTo(vec31) > 1.0E-5F) {
                if (entity.isInterpolating()) {
                    entity.getInterpolation().cancel();
                }

                entity.absSnapTo(vec3.x(), vec3.y(), vec3.z(), packet.yRot(), packet.xRot());
            }

            this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
        }
    }

    @Override
    public void handleOpenBook(ClientboundOpenBookPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ItemStack itemstack = this.minecraft.player.getItemInHand(packet.getHand());
        BookViewScreen.BookAccess bookviewscreen$bookaccess = BookViewScreen.BookAccess.fromItem(itemstack);
        if (bookviewscreen$bookaccess != null) {
            this.minecraft.setScreen(new BookViewScreen(bookviewscreen$bookaccess));
        }
    }

    @Override
    public void handleCustomPayload(CustomPacketPayload payload) {
        this.handleUnknownCustomPayload(payload);
    }

    private void handleUnknownCustomPayload(CustomPacketPayload packet) {
        LOGGER.warn("Unknown custom packet payload: {}", packet.type().id());
    }

    /**
     * May create a scoreboard objective, remove an objective from the scoreboard or update an objectives' displayname
     */
    @Override
    public void handleAddObjective(ClientboundSetObjectivePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String s = packet.getObjectiveName();
        if (packet.getMethod() == 0) {
            this.scoreboard
                .addObjective(
                    s, ObjectiveCriteria.DUMMY, packet.getDisplayName(), packet.getRenderType(), false, packet.getNumberFormat().orElse(null)
                );
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                if (packet.getMethod() == 1) {
                    this.scoreboard.removeObjective(objective);
                } else if (packet.getMethod() == 2) {
                    objective.setRenderType(packet.getRenderType());
                    objective.setDisplayName(packet.getDisplayName());
                    objective.setNumberFormat(packet.getNumberFormat().orElse(null));
                }
            }
        }
    }

    /**
     * Either updates the score with a specified value or removes the score for an objective
     */
    @Override
    public void handleSetScore(ClientboundSetScorePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String s = packet.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(packet.owner());
        Objective objective = this.scoreboard.getObjective(s);
        if (objective != null) {
            ScoreAccess scoreaccess = this.scoreboard.getOrCreatePlayerScore(scoreholder, objective, true);
            scoreaccess.set(packet.score());
            scoreaccess.display(packet.display().orElse(null));
            scoreaccess.numberFormatOverride(packet.numberFormat().orElse(null));
        } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
        }
    }

    @Override
    public void handleResetScore(ClientboundResetScorePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String s = packet.objectiveName();
        ScoreHolder scoreholder = ScoreHolder.forNameOnly(packet.owner());
        if (s == null) {
            this.scoreboard.resetAllPlayerScores(scoreholder);
        } else {
            Objective objective = this.scoreboard.getObjective(s);
            if (objective != null) {
                this.scoreboard.resetSinglePlayerScore(scoreholder, objective);
            } else {
                LOGGER.warn("Received packet for unknown scoreboard objective: {}", s);
            }
        }
    }

    /**
     * Removes or sets the ScoreObjective to be displayed at a particular scoreboard position (list, sidebar, below name)
     */
    @Override
    public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String s = packet.getObjectiveName();
        Objective objective = s == null ? null : this.scoreboard.getObjective(s);
        this.scoreboard.setDisplayObjective(packet.getSlot(), objective);
    }

    /**
     * Updates a team managed by the scoreboard: Create/Remove the team registration, Register/Remove the player-team-memberships, Set team displayname/prefix/suffix and/or whether friendly fire is enabled
     */
    @Override
    public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action = packet.getTeamAction();
        PlayerTeam playerteam;
        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.ADD) {
            playerteam = this.scoreboard.addPlayerTeam(packet.getName());
        } else {
            playerteam = this.scoreboard.getPlayerTeam(packet.getName());
            if (playerteam == null) {
                LOGGER.warn(
                    "Received packet for unknown team {}: team action: {}, player action: {}",
                    packet.getName(),
                    packet.getTeamAction(),
                    packet.getPlayerAction()
                );
                return;
            }
        }

        Optional<ClientboundSetPlayerTeamPacket.Parameters> optional = packet.getParameters();
        optional.ifPresent(p_400869_ -> {
            playerteam.setDisplayName(p_400869_.getDisplayName());
            playerteam.setColor(p_400869_.getColor());
            playerteam.unpackOptions(p_400869_.getOptions());
            playerteam.setNameTagVisibility(p_400869_.getNametagVisibility());
            playerteam.setCollisionRule(p_400869_.getCollisionRule());
            playerteam.setPlayerPrefix(p_400869_.getPlayerPrefix());
            playerteam.setPlayerSuffix(p_400869_.getPlayerSuffix());
        });
        ClientboundSetPlayerTeamPacket.Action clientboundsetplayerteampacket$action1 = packet.getPlayerAction();
        if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.ADD) {
            for (String s : packet.getPlayers()) {
                this.scoreboard.addPlayerToTeam(s, playerteam);
            }
        } else if (clientboundsetplayerteampacket$action1 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            for (String s1 : packet.getPlayers()) {
                this.scoreboard.removePlayerFromTeam(s1, playerteam);
            }
        }

        if (clientboundsetplayerteampacket$action == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            this.scoreboard.removePlayerTeam(playerteam);
        }
    }

    /**
     * Spawns a specified number of particles at the specified location with a randomized displacement according to specified bounds
     */
    @Override
    public void handleParticleEvent(ClientboundLevelParticlesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.getCount() == 0) {
            double d0 = packet.getMaxSpeed() * packet.getXDist();
            double d2 = packet.getMaxSpeed() * packet.getYDist();
            double d4 = packet.getMaxSpeed() * packet.getZDist();

            try {
                this.level
                    .addParticle(
                        packet.getParticle(),
                        packet.isOverrideLimiter(),
                        packet.alwaysShow(),
                        packet.getX(),
                        packet.getY(),
                        packet.getZ(),
                        d0,
                        d2,
                        d4
                    );
            } catch (Throwable throwable1) {
                LOGGER.warn("Could not spawn particle effect {}", packet.getParticle());
            }
        } else {
            for (int i = 0; i < packet.getCount(); i++) {
                double d1 = this.random.nextGaussian() * packet.getXDist();
                double d3 = this.random.nextGaussian() * packet.getYDist();
                double d5 = this.random.nextGaussian() * packet.getZDist();
                double d6 = this.random.nextGaussian() * packet.getMaxSpeed();
                double d7 = this.random.nextGaussian() * packet.getMaxSpeed();
                double d8 = this.random.nextGaussian() * packet.getMaxSpeed();

                try {
                    this.level
                        .addParticle(
                            packet.getParticle(),
                            packet.isOverrideLimiter(),
                            packet.alwaysShow(),
                            packet.getX() + d1,
                            packet.getY() + d3,
                            packet.getZ() + d5,
                            d6,
                            d7,
                            d8
                        );
                } catch (Throwable throwable) {
                    LOGGER.warn("Could not spawn particle effect {}", packet.getParticle());
                    return;
                }
            }
        }
    }

    /**
     * Updates en entity's attributes and their respective modifiers, which are used for speed bonuses (player sprinting, animals fleeing, baby speed), weapon/tool attackDamage, hostiles followRange randomization, zombie maxHealth and knockback resistance as well as reinforcement spawning chance.
     */
    @Override
    public void handleUpdateAttributes(ClientboundUpdateAttributesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntityId());
        if (entity != null) {
            if (!(entity instanceof LivingEntity)) {
                throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
            } else {
                AttributeMap attributemap = ((LivingEntity)entity).getAttributes();

                for (ClientboundUpdateAttributesPacket.AttributeSnapshot clientboundupdateattributespacket$attributesnapshot : packet.getValues()) {
                    AttributeInstance attributeinstance = attributemap.getInstance(clientboundupdateattributespacket$attributesnapshot.attribute());
                    if (attributeinstance == null) {
                        LOGGER.warn(
                            "Entity {} does not have attribute {}", entity, clientboundupdateattributespacket$attributesnapshot.attribute().getRegisteredName()
                        );
                    } else {
                        attributeinstance.setBaseValue(clientboundupdateattributespacket$attributesnapshot.base());
                        attributeinstance.removeModifiers();

                        for (AttributeModifier attributemodifier : clientboundupdateattributespacket$attributesnapshot.modifiers()) {
                            attributeinstance.addTransientModifier(attributemodifier);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (abstractcontainermenu.containerId == packet.containerId()) {
            if (this.minecraft.screen instanceof RecipeUpdateListener recipeupdatelistener) {
                recipeupdatelistener.fillGhostRecipe(packet.recipeDisplay());
            }
        }
    }

    @Override
    public void handleLightUpdatePacket(ClientboundLightUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int i = packet.getX();
        int j = packet.getZ();
        ClientboundLightUpdatePacketData clientboundlightupdatepacketdata = packet.getLightData();
        this.level.queueLightUpdate(() -> this.applyLightData(i, j, clientboundlightupdatepacketdata, true));
    }

    private void applyLightData(int x, int z, ClientboundLightUpdatePacketData data, boolean update) {
        LevelLightEngine levellightengine = this.level.getChunkSource().getLightEngine();
        BitSet bitset = data.getSkyYMask();
        BitSet bitset1 = data.getEmptySkyYMask();
        Iterator<byte[]> iterator = data.getSkyUpdates().iterator();
        this.readSectionList(x, z, levellightengine, LightLayer.SKY, bitset, bitset1, iterator, update);
        BitSet bitset2 = data.getBlockYMask();
        BitSet bitset3 = data.getEmptyBlockYMask();
        Iterator<byte[]> iterator1 = data.getBlockUpdates().iterator();
        this.readSectionList(x, z, levellightengine, LightLayer.BLOCK, bitset2, bitset3, iterator1, update);
        levellightengine.setLightEnabled(new ChunkPos(x, z), true);
    }

    @Override
    public void handleMerchantOffers(ClientboundMerchantOffersPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        AbstractContainerMenu abstractcontainermenu = this.minecraft.player.containerMenu;
        if (packet.getContainerId() == abstractcontainermenu.containerId && abstractcontainermenu instanceof MerchantMenu merchantmenu) {
            merchantmenu.setOffers(packet.getOffers());
            merchantmenu.setXp(packet.getVillagerXp());
            merchantmenu.setMerchantLevel(packet.getVillagerLevel());
            merchantmenu.setShowProgressBar(packet.showProgress());
            merchantmenu.setCanRestock(packet.canRestock());
        }
    }

    @Override
    public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.serverChunkRadius = packet.getRadius();
        this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
        this.level.getChunkSource().updateViewRadius(packet.getRadius());
    }

    @Override
    public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.serverSimulationDistance = packet.simulationDistance();
        this.level.setServerSimulationDistance(this.serverSimulationDistance);
    }

    @Override
    public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getChunkSource().updateViewCenter(packet.getX(), packet.getZ());
    }

    @Override
    public void handleBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.handleBlockChangedAck(packet.sequence());
    }

    @Override
    public void handleBundlePacket(ClientboundBundlePacket p_packet) {
        PacketUtils.ensureRunningOnSameThread(p_packet, this, this.minecraft.packetProcessor());

        for (Packet<? super ClientGamePacketListener> packet : p_packet.subPackets()) {
            packet.handle(this);
        }
    }

    @Override
    public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.level.getEntity(packet.getId()) instanceof AbstractHurtingProjectile abstracthurtingprojectile) {
            abstracthurtingprojectile.accelerationPower = packet.getAccelerationPower();
        }
    }

    @Override
    public void handleChunkBatchStart(ClientboundChunkBatchStartPacket packet) {
        this.chunkBatchSizeCalculator.onBatchStart();
    }

    @Override
    public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket packet) {
        this.chunkBatchSizeCalculator.onBatchFinished(packet.batchSize());
        this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
    }

    @Override
    public void handleDebugSample(ClientboundDebugSamplePacket packet) {
        this.minecraft.getDebugOverlay().logRemoteSample(packet.sample(), packet.debugSampleType());
    }

    @Override
    public void handlePongResponse(ClientboundPongResponsePacket packet) {
        this.pingDebugMonitor.onPongReceived(packet);
    }

    @Override
    public void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus status) {
        PacketUtils.ensureRunningOnSameThread(status, this, this.minecraft.packetProcessor());
        if (this.minecraft.screen instanceof TestInstanceBlockEditScreen testinstanceblockeditscreen) {
            testinstanceblockeditscreen.setStatus(status.status(), status.size());
        }
    }

    @Override
    public void handleWaypoint(ClientboundTrackedWaypointPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.apply(this.waypointManager);
    }

    @Override
    public void handleDebugChunkValue(ClientboundDebugChunkValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.updateChunk(this.level.getGameTime(), packet.chunkPos(), packet.update());
    }

    @Override
    public void handleDebugBlockValue(ClientboundDebugBlockValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.updateBlock(this.level.getGameTime(), packet.blockPos(), packet.update());
    }

    @Override
    public void handleDebugEntityValue(ClientboundDebugEntityValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.entityId());
        if (entity != null) {
            this.debugSubscriber.updateEntity(this.level.getGameTime(), entity, packet.update());
        }
    }

    @Override
    public void handleDebugEvent(ClientboundDebugEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.pushEvent(this.level.getGameTime(), packet.event());
    }

    @Override
    public void handleGameTestHighlightPos(ClientboundGameTestHighlightPosPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.levelRenderer.gameTestBlockHighlightRenderer.highlightPos(packet.absolutePos(), packet.relativePos());
    }

    private void readSectionList(
        int x,
        int z,
        LevelLightEngine lightEngine,
        LightLayer lightLayer,
        BitSet skyYMask,
        BitSet emptySkyYMask,
        Iterator<byte[]> skyUpdates,
        boolean update
    ) {
        for (int i = 0; i < lightEngine.getLightSectionCount(); i++) {
            int j = lightEngine.getMinLightSection() + i;
            boolean flag = skyYMask.get(i);
            boolean flag1 = emptySkyYMask.get(i);
            if (flag || flag1) {
                lightEngine.queueSectionData(
                    lightLayer, SectionPos.of(x, j, z), flag ? new DataLayer((byte[])skyUpdates.next().clone()) : new DataLayer()
                );
                if (update) {
                    this.level.setSectionDirtyWithNeighbors(x, j, z);
                }
            }
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.closed;
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return this.listedPlayers;
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return this.playerInfoMap.values();
    }

    public Collection<UUID> getOnlinePlayerIds() {
        return this.playerInfoMap.keySet();
    }

    @Nullable
    public PlayerInfo getPlayerInfo(UUID uuid) {
        return this.playerInfoMap.get(uuid);
    }

    /**
     * Gets the client's description information about another player on the server.
     */
    @Nullable
    public PlayerInfo getPlayerInfo(String name) {
        for (PlayerInfo playerinfo : this.playerInfoMap.values()) {
            if (playerinfo.getProfile().name().equals(name)) {
                return playerinfo;
            }
        }

        return null;
    }

    public Map<UUID, PlayerInfo> getSeenPlayers() {
        return this.seenPlayers;
    }

    @Nullable
    public PlayerInfo getPlayerInfoIgnoreCase(String playerName) {
        for (PlayerInfo playerinfo : this.playerInfoMap.values()) {
            if (playerinfo.getProfile().name().equalsIgnoreCase(playerName)) {
                return playerinfo;
            }
        }

        return null;
    }

    public GameProfile getLocalGameProfile() {
        return this.localGameProfile;
    }

    public ClientAdvancements getAdvancements() {
        return this.advancements;
    }

    public CommandDispatcher<ClientSuggestionProvider> getCommands() {
        return this.commands;
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public DebugQueryHandler getDebugQueryHandler() {
        return this.debugQueryHandler;
    }

    public UUID getId() {
        return this.id;
    }

    public Set<ResourceKey<Level>> levels() {
        return this.levels;
    }

    public RegistryAccess.Frozen registryAccess() {
        return this.registryAccess;
    }

    public void markMessageAsProcessed(MessageSignature signature, boolean acknowledged) {
        if (this.lastSeenMessages.addPending(signature, acknowledged) && this.lastSeenMessages.offset() > 64) {
            this.sendChatAcknowledgement();
        }
    }

    private void sendChatAcknowledgement() {
        int i = this.lastSeenMessages.getAndClearOffset();
        if (i > 0) {
            this.send(new ServerboundChatAckPacket(i));
        }
    }

    public void sendChat(String message) {
        message = net.neoforged.neoforge.client.ClientHooks.onClientSendMessage(message);
        if (message.isEmpty()) return;
        Instant instant = Instant.now();
        long i = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
        MessageSignature messagesignature = this.signedMessageEncoder
            .pack(new SignedMessageBody(message, instant, i, lastseenmessagestracker$update.lastSeen()));
        this.send(new ServerboundChatPacket(message, instant, i, messagesignature, lastseenmessagestracker$update.update()));
    }

    public void sendCommand(String command) {
        if (net.neoforged.neoforge.client.ClientCommandHandler.runCommand(command)) return;
        SignableCommand<ClientSuggestionProvider> signablecommand = SignableCommand.of(this.commands.parse(command, this.suggestionsProvider));
        if (signablecommand.arguments().isEmpty()) {
            this.send(new ServerboundChatCommandPacket(command));
        } else {
            Instant instant = Instant.now();
            long i = Crypt.SaltSupplier.getLong();
            LastSeenMessagesTracker.Update lastseenmessagestracker$update = this.lastSeenMessages.generateAndApplyUpdate();
            ArgumentSignatures argumentsignatures = ArgumentSignatures.signCommand(signablecommand, p_247875_ -> {
                SignedMessageBody signedmessagebody = new SignedMessageBody(p_247875_, instant, i, lastseenmessagestracker$update.lastSeen());
                return this.signedMessageEncoder.pack(signedmessagebody);
            });
            this.send(new ServerboundChatCommandSignedPacket(command, instant, i, argumentsignatures, lastseenmessagestracker$update.update()));
        }
    }

    public void sendUnattendedCommand(String command, @Nullable Screen previousScreen) {
        // Neo: Dispatch client commands for text component click actions.
        if (net.neoforged.neoforge.client.ClientCommandHandler.runCommand(command)) return;
        switch (this.verifyCommand(command)) {
            case NO_ISSUES:
                this.send(new ServerboundChatCommandPacket(command));
                this.minecraft.setScreen(previousScreen);
                break;
            case PARSE_ERRORS:
                this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.parse_errors", previousScreen);
                break;
            case SIGNATURE_REQUIRED:
                this.openSignedCommandSendConfirmationWindow(command, "multiplayer.confirm_command.signature_required", previousScreen);
                break;
            case PERMISSIONS_REQUIRED:
                this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.permissions_required", previousScreen);
        }
    }

    private ClientPacketListener.CommandCheckResult verifyCommand(String command) {
        ParseResults<ClientSuggestionProvider> parseresults = this.commands.parse(command, this.suggestionsProvider);
        if (!isValidCommand(parseresults)) {
            return ClientPacketListener.CommandCheckResult.PARSE_ERRORS;
        } else if (SignableCommand.hasSignableArguments(parseresults)) {
            return ClientPacketListener.CommandCheckResult.SIGNATURE_REQUIRED;
        } else {
            ParseResults<ClientSuggestionProvider> parseresults1 = this.commands.parse(command, this.restrictedSuggestionsProvider);
            return !isValidCommand(parseresults1)
                ? ClientPacketListener.CommandCheckResult.PERMISSIONS_REQUIRED
                : ClientPacketListener.CommandCheckResult.NO_ISSUES;
        }
    }

    private static boolean isValidCommand(ParseResults<?> parseResults) {
        return !parseResults.getReader().canRead() && parseResults.getExceptions().isEmpty() && parseResults.getContext().getLastChild().getCommand() != null;
    }

    private void openSendConfirmationWindow(String command, String titleKey, Component buttonText, Runnable onConfirm) {
        Screen screen = this.minecraft.screen;
        this.minecraft
            .setScreen(
                new ConfirmScreen(
                    p_437167_ -> {
                        if (p_437167_) {
                            onConfirm.run();
                        } else {
                            this.minecraft.setScreen(screen);
                        }
                    },
                    COMMAND_SEND_CONFIRM_TITLE,
                    Component.translatable(titleKey, Component.literal(command).withStyle(ChatFormatting.YELLOW)),
                    buttonText,
                    screen != null ? CommonComponents.GUI_BACK : CommonComponents.GUI_CANCEL
                )
            );
    }

    private void openCommandSendConfirmationWindow(String command, String titleKey, @Nullable Screen previousScreen) {
        this.openSendConfirmationWindow(command, titleKey, BUTTON_RUN_COMMAND, () -> {
            this.send(new ServerboundChatCommandPacket(command));
            this.minecraft.setScreen(previousScreen);
        });
    }

    private void openSignedCommandSendConfirmationWindow(String command, String titleKey, @Nullable Screen previousScreen) {
        boolean flag = previousScreen == null && this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer());
        this.openSendConfirmationWindow(command, titleKey, flag ? BUTTON_SUGGEST_COMMAND : CommonComponents.GUI_COPY_TO_CLIPBOARD, () -> {
            if (flag) {
                this.minecraft.openChatScreen(ChatComponent.ChatMethod.COMMAND);
                if (this.minecraft.screen instanceof ChatScreen chatscreen) {
                    chatscreen.insertText(command, false);
                }
            } else {
                this.minecraft.keyboardHandler.setClipboard("/" + command);
                this.minecraft.setScreen(previousScreen);
            }
        });
    }

    public void broadcastClientInformation(ClientInformation information) {
        if (!information.equals(this.remoteClientInformation)) {
            this.send(new ServerboundClientInformationPacket(information));
            this.remoteClientInformation = information;
        }
    }

    @Override
    public void tick() {
        if (this.chatSession != null && this.minecraft.getProfileKeyPairManager().shouldRefreshKeyPair()) {
            this.prepareKeyPair();
        }

        if (this.keyPairFuture != null && this.keyPairFuture.isDone()) {
            this.keyPairFuture.join().ifPresent(this::setKeyPair);
            this.keyPairFuture = null;
        }

        this.sendDeferredPackets();
        if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
            this.pingDebugMonitor.tick();
        }

        if (this.level != null) {
            this.debugSubscriber.tick(this.level.getGameTime());
        }

        this.telemetryManager.tick();
        if (this.levelLoadTracker != null) {
            this.levelLoadTracker.tickClientLoad();
            if (this.levelLoadTracker.isLevelReady()) {
                this.notifyPlayerLoaded();
                this.levelLoadTracker = null;
            }
        }
    }

    private void notifyPlayerLoaded() {
        if (!this.minecraft.player.hasClientLoaded()) {
            this.connection.send(new ServerboundPlayerLoadedPacket());
            this.minecraft.player.setClientLoaded(true);
        }
    }

    public void prepareKeyPair() {
        this.keyPairFuture = this.minecraft.getProfileKeyPairManager().prepareKeyPair();
    }

    private void setKeyPair(ProfileKeyPair keyPair) {
        if (this.minecraft.isLocalPlayer(this.localGameProfile.id())) {
            if (this.chatSession == null || !this.chatSession.keyPair().equals(keyPair)) {
                this.chatSession = LocalChatSession.create(keyPair);
                this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.id());
                this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
            }
        }
    }

    @Override
    protected DialogConnectionAccess createDialogAccess() {
        return new ClientCommonPacketListenerImpl.CommonDialogAccess() {
            @Override
            public void runCommand(String p_427295_, @Nullable Screen p_427359_) {
                ClientPacketListener.this.sendUnattendedCommand(p_427295_, p_427359_);
            }
        };
    }

    @Nullable
    public ServerData getServerData() {
        return this.serverData;
    }

    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    public boolean isFeatureEnabled(FeatureFlagSet enabledFeatures) {
        return enabledFeatures.isSubsetOf(this.enabledFeatures());
    }

    public Scoreboard scoreboard() {
        return this.scoreboard;
    }

    public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public PotionBrewing potionBrewing() {
        return this.potionBrewing;
    }

    public FuelValues fuelValues() {
        return this.fuelValues;
    }

    public void updateSearchTrees() {
        this.searchTrees.rebuildAfterLanguageChange();
    }

    public SessionSearchTrees searchTrees() {
        return this.searchTrees;
    }

    public void registerForCleaning(CacheSlot<?, ?> cacheSlot) {
        this.cacheSlots.add(new WeakReference<>(cacheSlot));
    }

    public HashedPatchMap.HashGenerator decoratedHashOpsGenenerator() {
        return this.decoratedHashOpsGenerator;
    }

    public ClientWaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    public DebugValueAccess createDebugValueAccess() {
        return this.debugSubscriber.createDebugValueAccess(this.level);
    }

    @OnlyIn(Dist.CLIENT)
    static enum CommandCheckResult {
        NO_ISSUES,
        PARSE_ERRORS,
        SIGNATURE_REQUIRED,
        PERMISSIONS_REQUIRED;
    }
}
