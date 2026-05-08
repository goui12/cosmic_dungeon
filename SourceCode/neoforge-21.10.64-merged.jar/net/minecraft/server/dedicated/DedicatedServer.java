package net.minecraft.server.dedicated;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.net.HostAndPort;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.jsonrpc.JsonRpcNotificationService;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.security.AuthenticationHandler;
import net.minecraft.server.jsonrpc.security.JsonRpcSslContextProvider;
import net.minecraft.server.jsonrpc.security.SecurityConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.network.ServerTextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.server.rcon.thread.RconThread;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.RemoteSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.monitoring.jmx.MinecraftServerStatistics;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class DedicatedServer extends MinecraftServer implements ServerInterface {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int CONVERSION_RETRY_DELAY_MS = 5000;
    private static final int CONVERSION_RETRIES = 2;
    private final List<ConsoleInput> consoleInput = Collections.synchronizedList(Lists.newArrayList());
    @Nullable
    private QueryThreadGs4 queryThreadGs4;
    private final RconConsoleSource rconConsoleSource;
    @Nullable
    private RconThread rconThread;
    private final DedicatedServerSettings settings;
    @Nullable
    private MinecraftServerGui gui;
    @Nullable
    private final ServerTextFilter serverTextFilter;
    @Nullable
    private RemoteSampleLogger tickTimeLogger;
    private boolean isTickTimeLoggingEnabled;
    private final ServerLinks serverLinks;
    private final Map<String, String> codeOfConductTexts;
    @Nullable
    private ManagementServer jsonRpcServer;
    private long lastHeartbeat;
    @Nullable
    private net.neoforged.neoforge.server.dedicated.DedicatedLanServerPinger dediLanPinger;

    public DedicatedServer(
        Thread serverThread,
        LevelStorageSource.LevelStorageAccess storageSource,
        PackRepository packRepository,
        WorldStem worldStem,
        DedicatedServerSettings settings,
        DataFixer fixerUpper,
        Services services
    ) {
        super(serverThread, storageSource, packRepository, worldStem, Proxy.NO_PROXY, fixerUpper, services, LoggingLevelLoadListener.forDedicatedServer());
        this.settings = settings;
        this.rconConsoleSource = new RconConsoleSource(this);
        this.serverTextFilter = ServerTextFilter.createFromConfig(settings.getProperties());
        this.serverLinks = createServerLinks(settings);
        if (settings.getProperties().codeOfConduct) {
            this.codeOfConductTexts = readCodeOfConducts();
        } else {
            this.codeOfConductTexts = Map.of();
        }
    }

    private static Map<String, String> readCodeOfConducts() {
        Path path = Path.of("codeofconduct");
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Code of Conduct folder does not exist: " + path);
        } else {
            try {
                Builder<String, String> builder = ImmutableMap.builder();

                try (Stream<Path> stream = Files.list(path)) {
                    for (Path path1 : stream.toList()) {
                        String s = path1.getFileName().toString();
                        if (s.endsWith(".txt")) {
                            String s1 = s.substring(0, s.length() - 4).toLowerCase(Locale.ROOT);
                            if (!path1.toRealPath().getParent().equals(path.toAbsolutePath())) {
                                throw new IllegalArgumentException(
                                    "Failed to read Code of Conduct file \"" + s + "\" because it links to a file outside the allowed directory"
                                );
                            }

                            try {
                                String s2 = String.join("\n", Files.readAllLines(path1, StandardCharsets.UTF_8));
                                builder.put(s1, StringUtil.stripColor(s2));
                            } catch (IOException ioexception) {
                                throw new IllegalArgumentException("Failed to read Code of Conduct file " + s, ioexception);
                            }
                        }
                    }
                }

                return builder.build();
            } catch (IOException ioexception1) {
                throw new IllegalArgumentException("Failed to read Code of Conduct folder", ioexception1);
            }
        }
    }

    private SslContext createSslContext() {
        try {
            return JsonRpcSslContextProvider.createFrom(
                this.getProperties().managementServerTlsKeystore, this.getProperties().managementServerTlsKeystorePassword
            );
        } catch (Exception exception) {
            JsonRpcSslContextProvider.printInstructions();
            throw new IllegalStateException("Failed to configure TLS for the server management protocol", exception);
        }
    }

    @Override
    public boolean initServer() throws IOException {
        int i = this.getProperties().managementServerPort;
        if (this.getProperties().managementServerEnabled) {
            String s = this.settings.getProperties().managementServerSecret;
            if (!SecurityConfig.isValid(s)) {
                throw new IllegalStateException("Invalid management server secret, must be 40 alphanumeric characters");
            }

            String s1 = this.getProperties().managementServerHost;
            HostAndPort hostandport = HostAndPort.fromParts(s1, i);
            SecurityConfig securityconfig = new SecurityConfig(s);
            AuthenticationHandler authenticationhandler = new AuthenticationHandler(securityconfig);
            LOGGER.info("Starting json RPC server on {}", hostandport);
            this.jsonRpcServer = new ManagementServer(hostandport, authenticationhandler);
            MinecraftApi minecraftapi = MinecraftApi.of(this);
            minecraftapi.notificationManager().registerService(new JsonRpcNotificationService(minecraftapi, this.jsonRpcServer));
            if (this.getProperties().managementServerTlsEnabled) {
                SslContext sslcontext = this.createSslContext();
                this.jsonRpcServer.startWithTls(minecraftapi, sslcontext);
            } else {
                this.jsonRpcServer.startWithoutTls(minecraftapi);
            }
        }

        Thread thread1 = new Thread("Server console handler") {
            @Override
            public void run() {
                if (net.neoforged.neoforge.server.console.TerminalHandler.handleCommands(DedicatedServer.this)) return;
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

                String s3;
                try {
                    while (!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning() && (s3 = bufferedreader.readLine()) != null) {
                        DedicatedServer.this.handleConsoleInput(s3, DedicatedServer.this.createCommandSourceStack());
                    }
                } catch (IOException ioexception1) {
                    DedicatedServer.LOGGER.error("Exception handling console input", (Throwable)ioexception1);
                }
            }
        };
        thread1.setDaemon(true);
        thread1.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread1.start();
        LOGGER.info("Starting minecraft server version {}", SharedConstants.getCurrentVersion().name());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        LOGGER.info("Loading properties");
        DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();
        if (this.isSingleplayer()) {
            this.setLocalIp("127.0.0.1");
        } else {
            this.setUsesAuthentication(dedicatedserverproperties.onlineMode);
            this.setPreventProxyConnections(dedicatedserverproperties.preventProxyConnections);
            this.setLocalIp(dedicatedserverproperties.serverIp);
        }

        this.worldData.setGameType(dedicatedserverproperties.gameMode.get());
        LOGGER.info("Default game type: {}", dedicatedserverproperties.gameMode.get());
        InetAddress inetaddress = null;
        if (!this.getLocalIp().isEmpty()) {
            inetaddress = InetAddress.getByName(this.getLocalIp());
        }

        if (this.getPort() < 0) {
            this.setPort(dedicatedserverproperties.serverPort);
        }

        this.initializeKeyPair();
        LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

        try {
            this.getConnection().startTcpServerListener(inetaddress, this.getPort());
        } catch (IOException ioexception) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", ioexception.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        if (!this.usesAuthentication()) {
            LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            LOGGER.warn(
                "While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose."
            );
            LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }

        if (this.convertOldUsers()) {
            this.services.nameToIdCache().save();
        }

        if (!OldUsersConverter.serverReadyAfterUserconversion(this)) {
            return false;
        } else {
            this.setPlayerList(new DedicatedPlayerList(this, this.registries(), this.playerDataStorage));
            this.tickTimeLogger = new RemoteSampleLogger(TpsDebugDimensions.values().length, this.debugSubscribers(), RemoteDebugSampleType.TICK_TIME);
            long j = Util.getNanos();
            this.services.nameToIdCache().resolveOfflineUsers(!this.usesAuthentication());
            net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerAboutToStart(this);
            LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
            this.loadLevel();
            long k = Util.getNanos() - j;
            String s2 = String.format(Locale.ROOT, "%.3fs", k / 1.0E9);
            LOGGER.info("Done ({})! For help, type \"help\"", s2);
            this.nextTickTimeNanos = Util.getNanos(); // Neo: Update server time to prevent watchdog/spamming during long load.
            if (dedicatedserverproperties.announcePlayerAchievements != null) {
                this.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(dedicatedserverproperties.announcePlayerAchievements, this);
            }

            if (dedicatedserverproperties.enableQuery) {
                LOGGER.info("Starting GS4 status listener");
                this.queryThreadGs4 = QueryThreadGs4.create(this);
            }

            if (dedicatedserverproperties.enableRcon) {
                LOGGER.info("Starting remote control listener");
                this.rconThread = RconThread.create(this);
            }

            if (this.getMaxTickLength() > 0L) {
                Thread thread = new Thread(new ServerWatchdog(this));
                thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
                thread.setName("Server Watchdog");
                thread.setDaemon(true);
                thread.start();
            }

            if (dedicatedserverproperties.enableJmxMonitoring) {
                MinecraftServerStatistics.registerJmxMonitoring(this);
                LOGGER.info("JMX monitoring enabled");
            }

            this.notificationManager().serverStarted();
            if (net.neoforged.neoforge.common.config.NeoForgeServerConfig.INSTANCE.advertiseDedicatedServerToLan.get()) {
                this.dediLanPinger = new net.neoforged.neoforge.server.dedicated.DedicatedLanServerPinger(this.getMotd(), String.valueOf(this.getServerPort()));
                this.dediLanPinger.start();
            }

            net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStarting(this);
            return true;
        }
    }

    @Override
    public boolean isEnforceWhitelist() {
        return this.settings.getProperties().enforceWhitelist.get();
    }

    @Override
    public void setEnforceWhitelist(boolean whitelistEnabled) {
        this.settings.update(p_442410_ -> p_442410_.enforceWhitelist.update(this.registryAccess(), whitelistEnabled));
    }

    @Override
    public boolean isUsingWhitelist() {
        return this.settings.getProperties().whiteList.get();
    }

    @Override
    public void setUsingWhitelist(boolean usingWhitelist) {
        this.settings.update(p_432448_ -> p_432448_.whiteList.update(this.registryAccess(), usingWhitelist));
    }

    /**
     * Main function called by run() every loop.
     */
    @Override
    public void tickServer(BooleanSupplier hasTimeLeft) {
        super.tickServer(hasTimeLeft);
        if (this.jsonRpcServer != null) {
            this.jsonRpcServer.tick();
        }

        long i = Util.getMillis();
        int j = this.statusHeartbeatInterval();
        if (j > 0) {
            long k = j * TimeUtil.MILLISECONDS_PER_SECOND;
            if (i - this.lastHeartbeat >= k) {
                this.lastHeartbeat = i;
                this.notificationManager().statusHeartbeat();
            }
        }
    }

    @Override
    public boolean saveAllChunks(boolean suppressLogs, boolean flush, boolean force) {
        this.notificationManager().serverSaveStarted();
        boolean flag = super.saveAllChunks(suppressLogs, flush, force);
        this.notificationManager().serverSaveCompleted();
        return flag;
    }

    @Override
    public boolean allowFlight() {
        return this.settings.getProperties().allowFlight.get();
    }

    public void setAllowFlight(boolean allowFlight) {
        this.settings.update(p_442404_ -> p_442404_.allowFlight.update(this.registryAccess(), allowFlight));
    }

    @Override
    public DedicatedServerProperties getProperties() {
        return this.settings.getProperties();
    }

    public void setDifficulty(Difficulty difficulty) {
        this.settings.update(p_442394_ -> p_442394_.difficulty.update(this.registryAccess(), difficulty));
        this.forceDifficulty();
    }

    @Override
    public void forceDifficulty() {
        this.setDifficulty(this.getProperties().difficulty.get(), true);
    }

    public int viewDistance() {
        return this.settings.getProperties().viewDistance.get();
    }

    public void setViewDistance(int viewDistance) {
        this.settings.update(p_442408_ -> p_442408_.viewDistance.update(this.registryAccess(), viewDistance));
        this.getPlayerList().setViewDistance(viewDistance);
    }

    public int simulationDistance() {
        return this.settings.getProperties().simulationDistance.get();
    }

    public void setSimulationDistance(int simulationDistance) {
        this.settings.update(p_442396_ -> p_442396_.simulationDistance.update(this.registryAccess(), simulationDistance));
        this.getPlayerList().setSimulationDistance(simulationDistance);
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport report) {
        report.setDetail("Is Modded", () -> this.getModdedStatus().fullDescription());
        report.setDetail("Type", () -> "Dedicated Server (map_server.txt)");
        return report;
    }

    @Override
    public void dumpServerProperties(Path path) throws IOException {
        DedicatedServerProperties dedicatedserverproperties = this.getProperties();

        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write(String.format(Locale.ROOT, "sync-chunk-writes=%s%n", dedicatedserverproperties.syncChunkWrites));
            writer.write(String.format(Locale.ROOT, "gamemode=%s%n", dedicatedserverproperties.gameMode.get()));
            writer.write(String.format(Locale.ROOT, "entity-broadcast-range-percentage=%d%n", dedicatedserverproperties.entityBroadcastRangePercentage.get()));
            writer.write(String.format(Locale.ROOT, "max-world-size=%d%n", dedicatedserverproperties.maxWorldSize));
            writer.write(String.format(Locale.ROOT, "view-distance=%d%n", dedicatedserverproperties.viewDistance.get()));
            writer.write(String.format(Locale.ROOT, "simulation-distance=%d%n", dedicatedserverproperties.simulationDistance.get()));
            writer.write(String.format(Locale.ROOT, "generate-structures=%s%n", dedicatedserverproperties.worldOptions.generateStructures()));
            writer.write(String.format(Locale.ROOT, "use-native=%s%n", dedicatedserverproperties.useNativeTransport));
            writer.write(String.format(Locale.ROOT, "rate-limit=%d%n", dedicatedserverproperties.rateLimitPacketsPerSecond));
        }
    }

    @Override
    public void onServerExit() {
        if (this.serverTextFilter != null) {
            this.serverTextFilter.close();
        }

        if (this.gui != null) {
            this.gui.close();
        }

        if (this.rconThread != null) {
            this.rconThread.stop();
        }

        if (this.queryThreadGs4 != null) {
            this.queryThreadGs4.stop();
        }

        if (this.dediLanPinger != null) {
            this.dediLanPinger.interrupt();
            this.dediLanPinger = null;
        }

        if (this.jsonRpcServer != null) {
            try {
                this.jsonRpcServer.stop(true);
            } catch (InterruptedException interruptedexception) {
                LOGGER.error("Interrupted while stopping the management server", (Throwable)interruptedexception);
            }
        }
    }

    @Override
    public void tickConnection() {
        super.tickConnection();
        this.handleConsoleInputs();
    }

    public void handleConsoleInput(String msg, CommandSourceStack source) {
        this.consoleInput.add(new ConsoleInput(msg, source));
    }

    public void handleConsoleInputs() {
        while (!this.consoleInput.isEmpty()) {
            ConsoleInput consoleinput = this.consoleInput.remove(0);
            this.getCommands().performPrefixedCommand(consoleinput.source, consoleinput.msg);
        }
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return this.getProperties().rateLimitPacketsPerSecond;
    }

    @Override
    public boolean isEpollEnabled() {
        return this.getProperties().useNativeTransport;
    }

    public DedicatedPlayerList getPlayerList() {
        return (DedicatedPlayerList)super.getPlayerList();
    }

    @Override
    public int getMaxPlayers() {
        return this.settings.getProperties().maxPlayers.get();
    }

    public void setMaxPlayers(int maxPlayers) {
        this.settings.update(p_442388_ -> p_442388_.maxPlayers.update(this.registryAccess(), maxPlayers));
    }

    @Override
    public boolean isPublished() {
        return true;
    }

    @Override
    public String getServerIp() {
        return this.getLocalIp();
    }

    @Override
    public int getServerPort() {
        return this.getPort();
    }

    @Override
    public String getServerName() {
        return this.getMotd();
    }

    public void showGui() {
        if (this.gui == null) {
            this.gui = MinecraftServerGui.showFrameFor(this);
        }
    }

    @Override
    public boolean hasGui() {
        return this.gui != null;
    }

    public int spawnProtectionRadius() {
        return this.getProperties().spawnProtection.get();
    }

    public void setSpawnProtectionRadius(int spawnProtectionRadius) {
        this.settings.update(p_442386_ -> p_442386_.spawnProtection.update(this.registryAccess(), spawnProtectionRadius));
    }

    @Override
    public boolean isUnderSpawnProtection(ServerLevel level, BlockPos pos, Player player) {
        LevelData.RespawnData leveldata$respawndata = level.getRespawnData();
        if (level.dimension() != leveldata$respawndata.dimension()) {
            return false;
        } else if (this.getPlayerList().getOps().isEmpty()) {
            return false;
        } else if (this.getPlayerList().isOp(player.nameAndId())) {
            return false;
        } else if (this.spawnProtectionRadius() <= 0) {
            return false;
        } else {
            BlockPos blockpos = leveldata$respawndata.pos();
            int i = Mth.abs(pos.getX() - blockpos.getX());
            int j = Mth.abs(pos.getZ() - blockpos.getZ());
            int k = Math.max(i, j);
            return k <= this.spawnProtectionRadius();
        }
    }

    @Override
    public boolean repliesToStatus() {
        return this.getProperties().enableStatus.get();
    }

    public void setRepliesToStatus(boolean repliesToStatus) {
        this.settings.update(p_442402_ -> p_442402_.enableStatus.update(this.registryAccess(), repliesToStatus));
    }

    @Override
    public boolean hidesOnlinePlayers() {
        return this.getProperties().hideOnlinePlayers.get();
    }

    public void setHidesOnlinePlayers(boolean hidesOnlinePlayers) {
        this.settings.update(p_442382_ -> p_442382_.hideOnlinePlayers.update(this.registryAccess(), hidesOnlinePlayers));
    }

    @Override
    public int operatorUserPermissionLevel() {
        return this.getProperties().opPermissionLevel.get();
    }

    public void setOperatorUserPermissionLevel(int operatorUserPermissionLevel) {
        this.settings.update(p_442406_ -> p_442406_.opPermissionLevel.update(this.registryAccess(), operatorUserPermissionLevel));
    }

    @Override
    public int getFunctionCompilationLevel() {
        return this.getProperties().functionPermissionLevel;
    }

    @Override
    public int playerIdleTimeout() {
        return this.settings.getProperties().playerIdleTimeout.get();
    }

    @Override
    public void setPlayerIdleTimeout(int idleTimeout) {
        this.settings.update(p_432450_ -> p_432450_.playerIdleTimeout.update(this.registryAccess(), idleTimeout));
    }

    public int statusHeartbeatInterval() {
        return this.settings.getProperties().statusHeartbeatInterval.get();
    }

    public void setStatusHeartbeatInterval(int statusHeartbeatInterval) {
        this.settings.update(p_442398_ -> p_442398_.statusHeartbeatInterval.update(this.registryAccess(), statusHeartbeatInterval));
    }

    @Override
    public String getMotd() {
        return this.settings.getProperties().motd.get();
    }

    @Override
    public void setMotd(String motd) {
        this.settings.update(p_442392_ -> p_442392_.motd.update(this.registryAccess(), motd));
    }

    @Override
    public boolean shouldRconBroadcast() {
        return this.getProperties().broadcastRconToOps;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.getProperties().broadcastConsoleToOps;
    }

    @Override
    public int getAbsoluteMaxWorldSize() {
        return this.getProperties().maxWorldSize;
    }

    @Override
    public int getCompressionThreshold() {
        return this.getProperties().networkCompressionThreshold;
    }

    @Override
    public boolean enforceSecureProfile() {
        DedicatedServerProperties dedicatedserverproperties = this.getProperties();
        return dedicatedserverproperties.enforceSecureProfile && dedicatedserverproperties.onlineMode && this.services.canValidateProfileKeys();
    }

    @Override
    public boolean logIPs() {
        return this.getProperties().logIPs;
    }

    protected boolean convertOldUsers() {
        boolean flag = false;

        for (int i = 0; !flag && i <= 2; i++) {
            if (i > 0) {
                LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag = OldUsersConverter.convertUserBanlist(this);
        }

        boolean flag1 = false;

        for (int j = 0; !flag1 && j <= 2; j++) {
            if (j > 0) {
                LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag1 = OldUsersConverter.convertIpBanlist(this);
        }

        boolean flag2 = false;

        for (int k = 0; !flag2 && k <= 2; k++) {
            if (k > 0) {
                LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.waitForRetry();
            }

            flag2 = OldUsersConverter.convertOpsList(this);
        }

        boolean flag3 = false;

        for (int l = 0; !flag3 && l <= 2; l++) {
            if (l > 0) {
                LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.waitForRetry();
            }

            flag3 = OldUsersConverter.convertWhiteList(this);
        }

        boolean flag4 = false;

        for (int i1 = 0; !flag4 && i1 <= 2; i1++) {
            if (i1 > 0) {
                LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.waitForRetry();
            }

            flag4 = OldUsersConverter.convertPlayers(this);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void waitForRetry() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException interruptedexception) {
        }
    }

    public long getMaxTickLength() {
        return this.getProperties().maxTickTime;
    }

    @Override
    public int getMaxChainedNeighborUpdates() {
        return this.getProperties().maxChainedNeighborUpdates;
    }

    @Override
    public String getPluginNames() {
        return "";
    }

    /**
     * Handle a command received by an RCon instance
     */
    @Override
    public String runCommand(String command) {
        this.rconConsoleSource.prepareForCommand();
        this.executeBlocking(() -> this.getCommands().performPrefixedCommand(this.rconConsoleSource.createCommandSourceStack(), command));
        return this.rconConsoleSource.getCommandResponse();
    }

    @Override
    public void stopServer() {
        this.notificationManager().serverShuttingDown();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.GameShuttingDownEvent());
        super.stopServer();
        if (this.dediLanPinger != null) {
            this.dediLanPinger.interrupt();
            this.dediLanPinger = null;
        }
        Util.shutdownExecutors();
    }

    @Override
    public boolean isSingleplayerOwner(NameAndId player) {
        return false;
    }

    @Override
    public int getScaledTrackingDistance(int trackingDistance) {
        return this.entityBroadcastRangePercentage() * trackingDistance / 100;
    }

    public int entityBroadcastRangePercentage() {
        return this.getProperties().entityBroadcastRangePercentage.get();
    }

    public void setEntityBroadcastRangePercentage(int entityBroadcastRangePercentage) {
        this.settings.update(p_442384_ -> p_442384_.entityBroadcastRangePercentage.update(this.registryAccess(), entityBroadcastRangePercentage));
    }

    @Override
    public String getLevelIdName() {
        return this.storageSource.getLevelId();
    }

    @Override
    public boolean forceSynchronousWrites() {
        return this.settings.getProperties().syncChunkWrites;
    }

    @Override
    public TextFilter createTextFilterForPlayer(ServerPlayer player) {
        return this.serverTextFilter != null ? this.serverTextFilter.createContext(player.getGameProfile()) : TextFilter.DUMMY;
    }

    @Nullable
    @Override
    public GameType getForcedGameType() {
        return this.forceGameMode() ? this.worldData.getGameType() : null;
    }

    public boolean forceGameMode() {
        return this.settings.getProperties().forceGameMode.get();
    }

    public void setForceGameMode(boolean forceGameMode) {
        this.settings.update(p_442378_ -> p_442378_.forceGameMode.update(this.registryAccess(), forceGameMode));
        this.enforceGameTypeForPlayers(this.getForcedGameType());
    }

    public GameType gameMode() {
        return this.getProperties().gameMode.get();
    }

    public void setGameMode(GameType gameMode) {
        this.settings.update(p_442380_ -> p_442380_.gameMode.update(this.registryAccess(), gameMode));
        this.worldData.setGameType(this.gameMode());
        this.enforceGameTypeForPlayers(this.getForcedGameType());
    }

    @Override
    public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
        return this.settings.getProperties().serverResourcePackInfo;
    }

    @Override
    public void endMetricsRecordingTick() {
        super.endMetricsRecordingTick();
        this.isTickTimeLoggingEnabled = this.debugSubscribers().hasAnySubscriberFor(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);
    }

    @Override
    public SampleLogger getTickTimeLogger() {
        return this.tickTimeLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return this.isTickTimeLoggingEnabled;
    }

    @Override
    public boolean acceptsTransfers() {
        return this.settings.getProperties().acceptsTransfers.get();
    }

    public void setAcceptsTransfers(boolean acceptsTransfers) {
        this.settings.update(p_442390_ -> p_442390_.acceptsTransfers.update(this.registryAccess(), acceptsTransfers));
    }

    @Override
    public ServerLinks serverLinks() {
        return this.serverLinks;
    }

    @Override
    public int pauseWhenEmptySeconds() {
        return this.settings.getProperties().pauseWhenEmptySeconds.get();
    }

    public void setPauseWhenEmptySeconds(int pauseWhenEmptySeconds) {
        this.settings.update(p_442400_ -> p_442400_.pauseWhenEmptySeconds.update(this.registryAccess(), pauseWhenEmptySeconds));
    }

    private static ServerLinks createServerLinks(DedicatedServerSettings settings) {
        Optional<URI> optional = parseBugReportLink(settings.getProperties());
        return optional.<ServerLinks>map(p_351772_ -> new ServerLinks(List.of(ServerLinks.KnownLinkType.BUG_REPORT.create(p_351772_))))
            .orElse(ServerLinks.EMPTY);
    }

    private static Optional<URI> parseBugReportLink(DedicatedServerProperties properties) {
        String s = properties.bugReportLink;
        if (s.isEmpty()) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(Util.parseAndValidateUntrustedUri(s));
            } catch (Exception exception) {
                LOGGER.warn("Failed to parse bug link {}", s, exception);
                return Optional.empty();
            }
        }
    }

    @Override
    public Map<String, String> getCodeOfConducts() {
        return this.codeOfConductTexts;
    }
}
