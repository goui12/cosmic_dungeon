package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class PlayerList implements net.neoforged.neoforge.common.extensions.IPlayerListExtension {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
    public static final Component DUPLICATE_LOGIN_DISCONNECT_MESSAGE = Component.translatable("multiplayer.disconnect.duplicate_login");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    private final List<ServerPlayer> players = Lists.newArrayList();
    /**
     * A map containing the key-value pairs for UUIDs and their EntityPlayerMP objects.
     */
    private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
    private final UserBanList bans;
    private final IpBanList ipBans;
    private final ServerOpList ops;
    private final UserWhiteList whitelist;
    private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
    private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
    private final PlayerDataStorage playerIo;
    private final LayeredRegistryAccess<RegistryLayer> registries;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCommandsForAllPlayers;
    private int sendAllPlayerInfoIn;
    private final List<ServerPlayer> playersView = java.util.Collections.unmodifiableList(players);

    public PlayerList(MinecraftServer server, LayeredRegistryAccess<RegistryLayer> registries, PlayerDataStorage playerIo, NotificationService notificationService) {
        this.server = server;
        this.registries = registries;
        this.playerIo = playerIo;
        this.whitelist = new UserWhiteList(WHITELIST_FILE, notificationService);
        this.ops = new ServerOpList(OPLIST_FILE, notificationService);
        this.bans = new UserBanList(USERBANLIST_FILE, notificationService);
        this.ipBans = new IpBanList(IPBANLIST_FILE, notificationService);
    }

    public void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        NameAndId nameandid = player.nameAndId();
        UserNameToIdResolver usernametoidresolver = this.server.services().nameToIdCache();
        Optional<NameAndId> optional = usernametoidresolver.get(nameandid.id());
        String s = optional.map(NameAndId::name).orElse(nameandid.name());
        usernametoidresolver.add(nameandid);
        ServerLevel serverlevel = player.level();
        String s1 = connection.getLoggableAddress(this.server.logIPs());
        LOGGER.info(
            "{}[{}] logged in with entity id {} at ({}, {}, {})",
            player.getPlainTextName(),
            s1,
            player.getId(),
            player.getX(),
            player.getY(),
            player.getZ()
        );
        LevelData leveldata = serverlevel.getLevelData();
        ServerGamePacketListenerImpl servergamepacketlistenerimpl = new ServerGamePacketListenerImpl(this.server, connection, player, cookie);
        connection.setupInboundProtocol(
            GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess(), servergamepacketlistenerimpl.getConnectionType()), servergamepacketlistenerimpl),
            servergamepacketlistenerimpl
        );
        GameRules gamerules = serverlevel.getGameRules();
        boolean flag = gamerules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
        boolean flag2 = gamerules.getBoolean(GameRules.RULE_LIMITED_CRAFTING);
        servergamepacketlistenerimpl.send(
            new ClientboundLoginPacket(
                player.getId(),
                leveldata.isHardcore(),
                this.server.levelKeys(),
                this.getMaxPlayers(),
                this.getViewDistance(),
                this.getSimulationDistance(),
                flag1,
                !flag,
                flag2,
                player.createCommonSpawnInfo(serverlevel),
                this.server.enforceSecureProfile()
            )
        );
        servergamepacketlistenerimpl.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        servergamepacketlistenerimpl.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        servergamepacketlistenerimpl.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
        var dataSyncEvent = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(this, player));
        RecipeManager recipemanager = this.server.getRecipeManager();
        servergamepacketlistenerimpl.send(
            new ClientboundUpdateRecipesPacket(recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes())
        );
        net.neoforged.neoforge.common.CommonHooks.sendRecipes(player, dataSyncEvent.getRecipeTypesToSend(), recipemanager.recipeMap());
        this.sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.updateEntireScoreboard(serverlevel.getScoreboard(), player);
        this.server.invalidateStatus();
        MutableComponent mutablecomponent;
        if (player.getGameProfile().name().equalsIgnoreCase(s)) {
            mutablecomponent = Component.translatable("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutablecomponent = Component.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), s);
        }

        this.broadcastSystemMessage(mutablecomponent.withStyle(ChatFormatting.YELLOW), false);
        servergamepacketlistenerimpl.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ServerStatus serverstatus = this.server.getStatus();
        if (serverstatus != null && !cookie.transferred()) {
            player.sendServerStatus(serverstatus);
        }

        player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        this.sendLevelInfo(player, serverlevel);
        serverlevel.addNewPlayer(player);
        this.server.getCustomBossEvents().onPlayerConnect(player);
        this.sendActivePlayerEffects(player);
        player.initInventoryMenu();
        this.server.notificationManager().playerJoined(player);

        net.neoforged.neoforge.attachment.AttachmentSync.syncInitialPlayerAttachments(player);
        net.neoforged.neoforge.event.EventHooks.firePlayerLoggedIn(player);
    }

    protected void updateEntireScoreboard(ServerScoreboard scoreboard, ServerPlayer player) {
        Set<Objective> set = Sets.newHashSet();

        for (PlayerTeam playerteam : scoreboard.getPlayerTeams()) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerteam, true));
        }

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            Objective objective = scoreboard.getDisplayObjective(displayslot);
            if (objective != null && !set.contains(objective)) {
                for (Packet<?> packet : scoreboard.getStartTrackingPackets(objective)) {
                    player.connection.send(packet);
                }

                set.add(objective);
            }
        }
    }

    public void addWorldborderListener(final ServerLevel level) {
        level.getWorldBorder().addListener(new BorderChangeListener() {
            @Override
            public void onSetSize(WorldBorder p_446639_, double p_446495_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(p_446639_), level.dimension());
            }

            @Override
            public void onLerpSize(WorldBorder p_446360_, double p_445647_, double p_445492_, long p_446273_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(p_446360_), level.dimension());
            }

            @Override
            public void onSetCenter(WorldBorder p_445389_, double p_446055_, double p_447368_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(p_445389_), level.dimension());
            }

            @Override
            public void onSetWarningTime(WorldBorder p_445713_, int p_446020_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(p_445713_), level.dimension());
            }

            @Override
            public void onSetWarningBlocks(WorldBorder p_447253_, int p_447297_) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(p_447253_), level.dimension());
            }

            @Override
            public void onSetDamagePerBlock(WorldBorder p_446881_, double p_445420_) {
            }

            @Override
            public void onSetSafeZone(WorldBorder p_446156_, double p_446480_) {
            }
        });
    }

    public Optional<CompoundTag> loadPlayerData(NameAndId nameAndId) {
        CompoundTag compoundtag = this.server.getWorldData().getLoadedPlayerTag();
        if (this.server.isSingleplayerOwner(nameAndId) && compoundtag != null) {
            LOGGER.debug("loading single player");
            return Optional.of(compoundtag);
        } else {
            return this.playerIo.load(nameAndId);
        }
    }

    /**
     * Also stores the NBTTags if this is an IntegratedPlayerList.
     */
    protected void save(ServerPlayer player) {
        if (player.connection == null) return;
        this.playerIo.save(player);
        ServerStatsCounter serverstatscounter = this.stats.get(player.getUUID());
        if (serverstatscounter != null) {
            serverstatscounter.save();
        }

        PlayerAdvancements playeradvancements = this.advancements.get(player.getUUID());
        if (playeradvancements != null) {
            playeradvancements.save();
        }
    }

    /**
     * Called when a player disconnects from the game. Writes player data to disk and removes them from the world.
     */
    public void remove(ServerPlayer player) {
        net.neoforged.neoforge.event.EventHooks.firePlayerLoggedOut(player);
        ServerLevel serverlevel = player.level();
        player.awardStat(Stats.LEAVE_GAME);
        this.save(player);
        if (player.isPassenger()) {
            Entity entity = player.getRootVehicle();
            if (entity.hasExactlyOnePlayerPassenger()) {
                LOGGER.debug("Removing player mount");
                player.stopRiding();
                entity.getPassengersAndSelf().forEach(p_215620_ -> p_215620_.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
            }
        }

        player.unRide();

        for (ThrownEnderpearl thrownenderpearl : player.getEnderPearls()) {
            thrownenderpearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }

        serverlevel.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancements().stopListening();
        this.players.remove(player);
        this.server.getCustomBossEvents().onPlayerDisconnect(player);
        UUID uuid = player.getUUID();
        ServerPlayer serverplayer = this.playersByUUID.get(uuid);
        if (serverplayer == player) {
            this.playersByUUID.remove(uuid);
            this.stats.remove(uuid);
            this.advancements.remove(uuid);
            this.server.notificationManager().playerLeft(player);
        }

        this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
    }

    @Nullable
    public Component canPlayerLogin(SocketAddress address, NameAndId nameAndId) {
        if (this.bans.isBanned(nameAndId)) {
            UserBanListEntry userbanlistentry = this.bans.get(nameAndId);
            MutableComponent mutablecomponent1 = Component.translatable("multiplayer.disconnect.banned.reason", userbanlistentry.getReasonMessage());
            if (userbanlistentry.getExpires() != null) {
                mutablecomponent1.append(
                    Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userbanlistentry.getExpires()))
                );
            }

            return mutablecomponent1;
        } else if (!this.isWhiteListed(nameAndId)) {
            return Component.translatable("multiplayer.disconnect.not_whitelisted");
        } else if (this.ipBans.isBanned(address)) {
            IpBanListEntry ipbanlistentry = this.ipBans.get(address);
            MutableComponent mutablecomponent = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanlistentry.getReasonMessage());
            if (ipbanlistentry.getExpires() != null) {
                mutablecomponent.append(
                    Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanlistentry.getExpires()))
                );
            }

            return mutablecomponent;
        } else {
            return this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(nameAndId)
                ? Component.translatable("multiplayer.disconnect.server_full")
                : null;
        }
    }

    public boolean disconnectAllPlayersWithProfile(UUID uuid) {
        Set<ServerPlayer> set = Sets.newIdentityHashSet();

        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getUUID().equals(uuid)) {
                set.add(serverplayer);
            }
        }

        ServerPlayer serverplayer2 = this.playersByUUID.get(uuid);
        if (serverplayer2 != null) {
            set.add(serverplayer2);
        }

        for (ServerPlayer serverplayer1 : set) {
            serverplayer1.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
        }

        return !set.isEmpty();
    }

    public ServerPlayer respawn(ServerPlayer player, boolean keepInventory, Entity.RemovalReason reason) {
        TeleportTransition teleporttransition = player.findRespawnPositionAndUseSpawnBlock(!keepInventory, TeleportTransition.DO_NOTHING);
        this.players.remove(player);
        player.level().removePlayerImmediately(player, reason);

        // Neo: Allow changing the respawn position of players. The local dimension transition is updated with the new target.
        var event = net.neoforged.neoforge.event.EventHooks.firePlayerRespawnPositionEvent(player, teleporttransition, keepInventory);
        teleporttransition = event.getTeleportTransition();

        ServerLevel serverlevel = teleporttransition.newLevel();
        ServerPlayer serverplayer = new ServerPlayer(this.server, serverlevel, player.getGameProfile(), player.clientInformation());
        serverplayer.connection = player.connection;
        serverplayer.restoreFrom(player, keepInventory);
        serverplayer.setId(player.getId());
        serverplayer.setMainArm(player.getMainArm());

        // Neo: Allow the event to control if the original spawn position is copied
        if (event.copyOriginalSpawnPosition()) {
            serverplayer.copyRespawnPosition(player);
        }

        for (String s : player.getTags()) {
            serverplayer.addTag(s);
        }

        Vec3 vec3 = teleporttransition.position();
        serverplayer.snapTo(vec3.x, vec3.y, vec3.z, teleporttransition.yRot(), teleporttransition.xRot());
        if (teleporttransition.missingRespawnBlock()) {
            serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        byte b0 = (byte)(keepInventory ? 1 : 0);
        ServerLevel serverlevel1 = serverplayer.level();
        LevelData leveldata = serverlevel1.getLevelData();
        serverplayer.connection.send(new ClientboundRespawnPacket(serverplayer.createCommonSpawnInfo(serverlevel1), b0));
        serverplayer.connection.teleport(serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), serverplayer.getYRot(), serverplayer.getXRot());
        serverplayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverlevel.getRespawnData()));
        serverplayer.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
        serverplayer.connection
            .send(new ClientboundSetExperiencePacket(serverplayer.experienceProgress, serverplayer.totalExperience, serverplayer.experienceLevel));
        this.sendActivePlayerEffects(serverplayer);
        this.sendLevelInfo(serverplayer, serverlevel);
        this.sendPlayerPermissionLevel(serverplayer);
        serverlevel.addRespawnedPlayer(serverplayer);
        this.players.add(serverplayer);
        this.playersByUUID.put(serverplayer.getUUID(), serverplayer);
        serverplayer.initInventoryMenu();
        serverplayer.setHealth(serverplayer.getHealth());
        net.neoforged.neoforge.attachment.AttachmentSync.syncInitialPlayerAttachments(serverplayer);
        net.neoforged.neoforge.event.EventHooks.firePlayerRespawnEvent(serverplayer, keepInventory);
        ServerPlayer.RespawnConfig serverplayer$respawnconfig = serverplayer.getRespawnConfig();
        if (!keepInventory && serverplayer$respawnconfig != null) {
            LevelData.RespawnData leveldata$respawndata = serverplayer$respawnconfig.respawnData();
            ServerLevel serverlevel2 = this.server.getLevel(leveldata$respawndata.dimension());
            if (serverlevel2 != null) {
                BlockPos blockpos = leveldata$respawndata.pos();
                BlockState blockstate = serverlevel2.getBlockState(blockpos);
                if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
                    serverplayer.connection
                        .send(
                            new ClientboundSoundPacket(
                                SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                                SoundSource.BLOCKS,
                                blockpos.getX(),
                                blockpos.getY(),
                                blockpos.getZ(),
                                1.0F,
                                1.0F,
                                serverlevel.getRandom().nextLong()
                            )
                        );
                }
            }
        }

        return serverplayer;
    }

    public void sendActivePlayerEffects(ServerPlayer player) {
        this.sendActiveEffects(player, player.connection);
    }

    public void sendActiveEffects(LivingEntity entity, ServerGamePacketListenerImpl connection) {
        for (MobEffectInstance mobeffectinstance : entity.getActiveEffects()) {
            connection.send(new ClientboundUpdateMobEffectPacket(entity.getId(), mobeffectinstance, false));
        }
    }

    public void sendPlayerPermissionLevel(ServerPlayer player) {
        int i = this.server.getProfilePermissions(player.nameAndId());
        this.sendPlayerPermissionLevel(player, i);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
            this.sendAllPlayerInfoIn = 0;
        }
    }

    public void broadcastAll(Packet<?> packet) {
        for (ServerPlayer serverplayer : this.players) {
            serverplayer.connection.send(packet);
        }
    }

    public void broadcastAll(Packet<?> packet, ResourceKey<Level> dimension) {
        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.level().dimension() == dimension) {
                serverplayer.connection.send(packet);
            }
        }
    }

    public void broadcastSystemToTeam(Player player, Component message) {
        Team team = player.getTeam();
        if (team != null) {
            for (String s : team.getPlayers()) {
                ServerPlayer serverplayer = this.getPlayerByName(s);
                if (serverplayer != null && serverplayer != player) {
                    serverplayer.sendSystemMessage(message);
                }
            }
        }
    }

    public void broadcastSystemToAllExceptTeam(Player player, Component message) {
        Team team = player.getTeam();
        if (team == null) {
            this.broadcastSystemMessage(message, false);
        } else {
            for (int i = 0; i < this.players.size(); i++) {
                ServerPlayer serverplayer = this.players.get(i);
                if (serverplayer.getTeam() != team) {
                    serverplayer.sendSystemMessage(message);
                }
            }
        }
    }

    public String[] getPlayerNamesArray() {
        String[] astring = new String[this.players.size()];

        for (int i = 0; i < this.players.size(); i++) {
            astring[i] = this.players.get(i).getGameProfile().name();
        }

        return astring;
    }

    public UserBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(NameAndId nameAndId) {
        this.op(nameAndId, Optional.empty(), Optional.empty());
    }

    public void op(NameAndId nameAndId, Optional<Integer> permissionLevel, Optional<Boolean> canBypassPlayerLimit) {
        if (net.neoforged.neoforge.event.EventHooks.onPermissionChanged(nameAndId, this.server.operatorUserPermissionLevel(), this)) return;
        this.ops
            .add(
                new ServerOpListEntry(
                    nameAndId, permissionLevel.orElse(this.server.operatorUserPermissionLevel()), canBypassPlayerLimit.orElse(this.ops.canBypassPlayerLimit(nameAndId))
                )
            );
        ServerPlayer serverplayer = this.getPlayer(nameAndId.id());
        if (serverplayer != null) {
            this.sendPlayerPermissionLevel(serverplayer);
        }
    }

    public void deop(NameAndId nameAndId) {
        if (net.neoforged.neoforge.event.EventHooks.onPermissionChanged(nameAndId, 0, this)) return;
        if (this.ops.remove(nameAndId)) {
            ServerPlayer serverplayer = this.getPlayer(nameAndId.id());
            if (serverplayer != null) {
                this.sendPlayerPermissionLevel(serverplayer);
            }
        }
    }

    private void sendPlayerPermissionLevel(ServerPlayer player, int permLevel) {
        if (player.connection != null) {
            byte b0;
            if (permLevel <= 0) {
                b0 = 24;
            } else if (permLevel >= 4) {
                b0 = 28;
            } else {
                b0 = (byte)(24 + permLevel);
            }

            player.connection.send(new ClientboundEntityEventPacket(player, b0));
        }

        this.server.getCommands().sendCommands(player);
    }

    public boolean isWhiteListed(NameAndId nameAndId) {
        return !this.isUsingWhitelist() || this.ops.contains(nameAndId) || this.whitelist.contains(nameAndId);
    }

    public boolean isOp(NameAndId nameAndId) {
        return this.ops.contains(nameAndId)
            || this.server.isSingleplayerOwner(nameAndId) && this.server.getWorldData().isAllowCommands()
            || this.allowCommandsForAllPlayers;
    }

    @Nullable
    public ServerPlayer getPlayerByName(String username) {
        int i = this.players.size();

        for (int j = 0; j < i; j++) {
            ServerPlayer serverplayer = this.players.get(j);
            if (serverplayer.getGameProfile().name().equalsIgnoreCase(username)) {
                return serverplayer;
            }
        }

        return null;
    }

    public void broadcast(
        @Nullable Player except, double x, double y, double z, double radius, ResourceKey<Level> dimension, Packet<?> packet
    ) {
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer serverplayer = this.players.get(i);
            if (serverplayer != except && serverplayer.level().dimension() == dimension) {
                double d0 = x - serverplayer.getX();
                double d1 = y - serverplayer.getY();
                double d2 = z - serverplayer.getZ();
                if (d0 * d0 + d1 * d1 + d2 * d2 < radius * radius) {
                    serverplayer.connection.send(packet);
                }
            }
        }
    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.save(this.players.get(i));
        }
    }

    public UserWhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public ServerOpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {
    }

    /**
     * Updates the time and weather for the given player to those of the given world
     */
    public void sendLevelInfo(ServerPlayer player, ServerLevel level) {
        WorldBorder worldborder = level.getWorldBorder();
        player.connection.send(new ClientboundInitializeBorderPacket(worldborder));
        if (player.connection.hasChannel(net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload.TYPE)) {
            player.connection.send(new net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload(level.getGameTime(), level.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), level.getDayTimeFraction(), level.getDayTimePerTick()));
        } else {
        player.connection
            .send(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        }
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        if (level.isRaining()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)));
        }

        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
        this.server.tickRateManager().updateJoiningPlayer(player);
        net.neoforged.neoforge.attachment.AttachmentSync.syncInitialLevelAttachments(level, player);
    }

    /**
     * Sends the players inventory to himself.
     */
    public void sendAllPlayerInfo(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.resetSentInfo();
        player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    public boolean isUsingWhitelist() {
        return this.server.isUsingWhitelist();
    }

    public List<ServerPlayer> getPlayersWithAddress(String address) {
        List<ServerPlayer> list = Lists.newArrayList();

        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getIpAddress().equals(address)) {
                list.add(serverplayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    @Nullable
    public CompoundTag getSingleplayerData() {
        return null;
    }

    public void setAllowCommandsForAllPlayers(boolean allowCommandsForAllPlayers) {
        this.allowCommandsForAllPlayers = allowCommandsForAllPlayers;
    }

    public void removeAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
        }
    }

    public void broadcastSystemMessage(Component message, boolean bypassHiddenChat) {
        this.broadcastSystemMessage(message, p_215639_ -> message, bypassHiddenChat);
    }

    public void broadcastSystemMessage(Component serverMessage, Function<ServerPlayer, Component> playerMessageFactory, boolean bypassHiddenChat) {
        this.server.sendSystemMessage(serverMessage);

        for (ServerPlayer serverplayer : this.players) {
            Component component = playerMessageFactory.apply(serverplayer);
            if (component != null) {
                serverplayer.sendSystemMessage(component, bypassHiddenChat);
            }
        }
    }

    public void broadcastChatMessage(PlayerChatMessage message, CommandSourceStack sender, ChatType.Bound boundChatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender.getPlayer(), boundChatType);
    }

    public void broadcastChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender, boundChatType);
    }

    private void broadcastChatMessage(
        PlayerChatMessage message, Predicate<ServerPlayer> shouldFilterMessageTo, @Nullable ServerPlayer sender, ChatType.Bound boundChatType
    ) {
        boolean flag = this.verifyChatTrusted(message);
        this.server.logChatMessage(message.decoratedContent(), boundChatType, flag ? null : "Not Secure");
        OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(message);
        boolean flag1 = false;

        for (ServerPlayer serverplayer : this.players) {
            boolean flag2 = shouldFilterMessageTo.test(serverplayer);
            serverplayer.sendChatMessage(outgoingchatmessage, flag2, boundChatType);
            flag1 |= flag2 && message.isFullyFiltered();
        }

        if (flag1 && sender != null) {
            sender.sendSystemMessage(CHAT_FILTERED_FULL);
        }
    }

    private boolean verifyChatTrusted(PlayerChatMessage message) {
        return message.hasSignature() && !message.hasExpiredServer(Instant.now());
    }

    public ServerStatsCounter getPlayerStats(Player player) {
        GameProfile gameprofile = player.getGameProfile();
        UUID uuid = gameprofile.id();
        ServerStatsCounter serverstatscounter = this.stats.get(uuid);
        if (serverstatscounter == null) {
            File file1 = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
            File file2 = new File(file1, uuid + ".json");

            serverstatscounter = new ServerStatsCounter(this.server, file2);
            this.stats.put(uuid, serverstatscounter);
        }

        return serverstatscounter;
    }

    public PlayerAdvancements getPlayerAdvancements(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerAdvancements playeradvancements = this.advancements.get(uuid);
        if (playeradvancements == null) {
            Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
            playeradvancements = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), path, player);
            this.advancements.put(uuid, playeradvancements);
        }

        // Forge: don't overwrite active player with a fake one.
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer))
        playeradvancements.setPlayer(player);
        return playeradvancements;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance));

        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            if (serverlevel != null) {
                serverlevel.getChunkSource().setViewDistance(viewDistance);
            }
        }
    }

    public void setSimulationDistance(int simulationDistance) {
        this.simulationDistance = simulationDistance;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(simulationDistance));

        for (ServerLevel serverlevel : this.server.getAllLevels()) {
            if (serverlevel != null) {
                serverlevel.getChunkSource().setSimulationDistance(simulationDistance);
            }
        }
    }

    public List<ServerPlayer> getPlayers() {
        return this.playersView; //Unmodifiable view, we don't want people removing things without us knowing.
    }

    /**
     * Gets the ServerPlayer object representing the player with the UUID.
     */
    @Nullable
    public ServerPlayer getPlayer(UUID playerUUID) {
        return this.playersByUUID.get(playerUUID);
    }

    @Nullable
    public ServerPlayer getPlayer(String name) {
        for (ServerPlayer serverplayer : this.players) {
            if (serverplayer.getGameProfile().name().equalsIgnoreCase(name)) {
                return serverplayer;
            }
        }

        return null;
    }

    public boolean canBypassPlayerLimit(NameAndId nameAndId) {
        return false;
    }

    public void reloadResources() {
        for (PlayerAdvancements playeradvancements : this.advancements.values()) {
            playeradvancements.reload(this.server.getAdvancements());
        }

        var dataSyncEvent = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(this, null));
        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
        RecipeManager recipemanager = this.server.getRecipeManager();
        ClientboundUpdateRecipesPacket clientboundupdaterecipespacket = new ClientboundUpdateRecipesPacket(
            recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes()
        );

        for (ServerPlayer serverplayer : this.players) {
            serverplayer.connection.send(clientboundupdaterecipespacket);
            serverplayer.getRecipeBook().sendInitialRecipeBook(serverplayer);
            net.neoforged.neoforge.common.CommonHooks.sendRecipes(serverplayer, dataSyncEvent.getRecipeTypesToSend(), recipemanager.recipeMap());
        }
    }

    public boolean isAllowCommandsForAllPlayers() {
        return this.allowCommandsForAllPlayers;
    }

    public PlayerDataStorage getPlayerIo() {
        return this.playerIo;
    }
}
