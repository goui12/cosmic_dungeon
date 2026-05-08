package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.ClientShutdownWatchdog;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.Optionull;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreens;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.LocalPlayerResolver;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DryFoliageColorReloadListener;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicInfo;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DialogTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.FileZipper;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler, net.neoforged.neoforge.client.extensions.IMinecraftExtension {
    static Minecraft instance;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_PER_UPDATE = 10;
    public static final ResourceLocation DEFAULT_FONT = ResourceLocation.withDefaultNamespace("default");
    public static final ResourceLocation UNIFORM_FONT = ResourceLocation.withDefaultNamespace("uniform");
    public static final ResourceLocation ALT_FONT = ResourceLocation.withDefaultNamespace("alt");
    private static final ResourceLocation REGIONAL_COMPLIANCIES = ResourceLocation.withDefaultNamespace("regional_compliancies.json");
    private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
    public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
    private final long canary = Double.doubleToLongBits(Math.PI);
    private final Path resourcePackDirectory;
    private final CompletableFuture<ProfileResult> profileFuture;
    private final TextureManager textureManager;
    private final ShaderManager shaderManager;
    private final DataFixer fixerUpper;
    private final VirtualScreen virtualScreen;
    private final Window window;
    private final DeltaTracker.Timer deltaTracker = new DeltaTracker.Timer(20.0F, 0L, this::getTickTargetMillis);
    private final RenderBuffers renderBuffers;
    public final LevelRenderer levelRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemModelResolver itemModelResolver;
    private final ItemRenderer itemRenderer;
    private final MapRenderer mapRenderer;
    public final ParticleEngine particleEngine;
    private final ParticleResources particleResources;
    private final User user;
    public final Font font;
    public final Font fontFilterFishy;
    public final GameRenderer gameRenderer;
    public final Gui gui;
    public final Options options;
    public final DebugScreenEntryList debugEntries;
    private final HotbarManager hotbarManager;
    public final MouseHandler mouseHandler;
    public final KeyboardHandler keyboardHandler;
    private InputType lastInputType = InputType.NONE;
    public final File gameDirectory;
    private final String launchedVersion;
    private final String versionType;
    private final Proxy proxy;
    private final boolean offlineDeveloperMode;
    private final LevelStorageSource levelSource;
    private final boolean demo;
    private final boolean allowsMultiplayer;
    private final boolean allowsChat;
    private final ReloadableResourceManager resourceManager;
    private final VanillaPackResources vanillaPackResources;
    private final DownloadedPackSource downloadedPackSource;
    private final PackRepository resourcePackRepository;
    private final LanguageManager languageManager;
    private final BlockColors blockColors;
    private final RenderTarget mainRenderTarget;
    @Nullable
    private final TracyFrameCapture tracyFrameCapture;
    private final SoundManager soundManager;
    private final MusicManager musicManager;
    private final FontManager fontManager;
    private final SplashManager splashManager;
    private final GpuWarnlistManager gpuWarnlistManager;
    private final PeriodicNotificationManager regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
    private final UserApiService userApiService;
    private final CompletableFuture<UserProperties> userPropertiesFuture;
    private final SkinManager skinManager;
    private final AtlasManager atlasManager;
    private final ModelManager modelManager;
    /**
     * The BlockRenderDispatcher instance that will be used based off gamesettings
     */
    private final BlockRenderDispatcher blockRenderer;
    private final MapTextureManager mapTextureManager;
    private final WaypointStyleManager waypointStyles;
    private final ToastManager toastManager;
    private final Tutorial tutorial;
    private final PlayerSocialManager playerSocialManager;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final ClientTelemetryManager telemetryManager;
    private final ProfileKeyPairManager profileKeyPairManager;
    private final RealmsDataFetcher realmsDataFetcher;
    private final QuickPlayLog quickPlayLog;
    private final Services services;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Nullable
    public ClientLevel level;
    @Nullable
    public LocalPlayer player;
    @Nullable
    private IntegratedServer singleplayerServer;
    @Nullable
    private Connection pendingConnection;
    private boolean isLocalServer;
    @Nullable
    private Entity cameraEntity;
    @Nullable
    public Entity crosshairPickEntity;
    @Nullable
    public HitResult hitResult;
    private int rightClickDelay;
    protected int missTime;
    private volatile boolean pause;
    /**
     * Time in nanoseconds of when the class is loaded
     */
    private long lastNanoTime = Util.getNanos();
    private long lastTime;
    private int frames;
    public boolean noRender;
    @Nullable
    public Screen screen;
    @Nullable
    private Overlay overlay;
    private boolean clientLevelTeardownInProgress;
    Thread gameThread;
    private volatile boolean running;
    @Nullable
    private Supplier<CrashReport> delayedCrash;
    private static int fps;
    private long frameTimeNs;
    private final FramerateLimitTracker framerateLimitTracker;
    public boolean wireframe;
    public boolean smartCull = true;
    private boolean windowActive;
    @Nullable
    private CompletableFuture<Void> pendingReload;
    @Nullable
    private TutorialToast socialInteractionsToast;
    private int fpsPieRenderTicks;
    private final ContinuousProfiler fpsPieProfiler;
    private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    private final ResourceLoadStateTracker reloadStateTracker = new ResourceLoadStateTracker();
    private long savedCpuDuration;
    private double gpuUtilization;
    @Nullable
    private TimerQuery.FrameProfile currentFrameProfile;
    private final GameNarrator narrator;
    private final ChatListener chatListener;
    private ReportingContext reportingContext;
    private final CommandHistory commandHistory;
    private final DirectoryValidator directoryValidator;
    private boolean gameLoadFinished;
    private final long clientStartTimeMs;
    private long clientTickCount;
    private final PacketProcessor packetProcessor;

    public Minecraft(final GameConfig gameConfig) {
        super("Client");
        instance = this;
        this.clientStartTimeMs = System.currentTimeMillis();
        this.gameDirectory = gameConfig.location.gameDirectory;
        File file1 = gameConfig.location.assetDirectory;
        this.resourcePackDirectory = gameConfig.location.resourcePackDirectory.toPath();
        this.launchedVersion = gameConfig.game.launchVersion;
        this.versionType = gameConfig.game.versionType;
        Path path = this.gameDirectory.toPath();
        this.directoryValidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
        ClientPackSource clientpacksource = new ClientPackSource(gameConfig.location.getExternalAssetSource(), this.directoryValidator);
        this.downloadedPackSource = new DownloadedPackSource(this, path.resolve("downloads"), gameConfig.user);
        RepositorySource repositorysource = new FolderRepositorySource(
            this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT, this.directoryValidator
        );
        this.resourcePackRepository = new PackRepository(clientpacksource, this.downloadedPackSource.createRepositorySource(), repositorysource);
        this.vanillaPackResources = clientpacksource.getVanillaPack();
        this.proxy = gameConfig.user.proxy;
        this.offlineDeveloperMode = gameConfig.game.offlineDeveloperMode;
        YggdrasilAuthenticationService yggdrasilauthenticationservice = this.offlineDeveloperMode
            ? YggdrasilAuthenticationService.createOffline(this.proxy)
            : new YggdrasilAuthenticationService(this.proxy);
        this.services = Services.create(yggdrasilauthenticationservice, this.gameDirectory);
        this.user = gameConfig.user.user;
        this.profileFuture = this.offlineDeveloperMode
            ? CompletableFuture.completedFuture(null)
            : CompletableFuture.supplyAsync(() -> this.services.sessionService().fetchProfile(this.user.getProfileId(), true), Util.nonCriticalIoPool());
        this.userApiService = this.createUserApiService(yggdrasilauthenticationservice, gameConfig);
        this.userPropertiesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return this.userApiService.fetchProperties();
            } catch (AuthenticationException authenticationexception) {
                LOGGER.error("Failed to fetch user properties", (Throwable)authenticationexception);
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool());
        LOGGER.info("Setting user: {}", this.user.getName());
        this.demo = gameConfig.game.demo;
        this.allowsMultiplayer = !gameConfig.game.disableMultiplayer;
        this.allowsChat = !gameConfig.game.disableChat;
        this.singleplayerServer = null;
        KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
        this.fixerUpper = DataFixers.getDataFixer();
        this.gameThread = Thread.currentThread();
        this.options = new Options(this, this.gameDirectory);
        this.debugEntries = new DebugScreenEntryList(this.gameDirectory);
        this.toastManager = new ToastManager(this, this.options);
        boolean flag = this.options.startedCleanly;
        this.options.startedCleanly = false;
        this.options.save();
        this.running = true;
        this.tutorial = new Tutorial(this, this.options);
        this.hotbarManager = new HotbarManager(path, this.fixerUpper);
        LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
        DisplayData displaydata = gameConfig.display;
        if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
            displaydata = gameConfig.display.withSize(this.options.overrideWidth, this.options.overrideHeight);
        }

        if (!flag) {
            displaydata = displaydata.withFullscreen(false);
            this.options.fullscreenVideoModeString = null;
            LOGGER.warn("Detected unexpected shutdown during last game startup: resetting fullscreen mode");
        }

        Util.timeSource = RenderSystem.initBackendSystem();
        this.virtualScreen = new VirtualScreen(this);
        this.window = this.virtualScreen.newWindow(displaydata, this.options.fullscreenVideoModeString, this.createTitle());
        this.setWindowActive(true);
        this.window.setWindowCloseCallback(new Runnable() {
            private boolean threadStarted;

            @Override
            public void run() {
                if (!this.threadStarted) {
                    this.threadStarted = true;
                    ClientShutdownWatchdog.startShutdownWatchdog(gameConfig.location.gameDirectory, Minecraft.this.gameThread.threadId());
                }
            }
        });
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

        try {
            this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().stable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't set icon", (Throwable)ioexception);
        }

        // FORGE: Move mouse and keyboard handler setup further below
        this.mouseHandler = new MouseHandler(this);
        this.keyboardHandler = new KeyboardHandler(this);
        // Neo: Enable GL debug labels and synchronous GL debug output via FML config
        boolean enableGlDebug = net.neoforged.fml.loading.FMLConfig.getBoolConfigValue(net.neoforged.fml.loading.FMLConfig.ConfigValue.DEBUG_OPENGL);
        RenderSystem.initRenderer(
            this.window.handle(),
            this.options.glDebugVerbosity,
            SharedConstants.DEBUG_SYNCHRONOUS_GL_LOGS || enableGlDebug,
            (p_408948_, p_408949_) -> this.getShaderManager().getShader(p_408948_, p_408949_),
            gameConfig.game.renderDebugLabels || enableGlDebug
        );
        LOGGER.info("Using optional rendering extensions: {}", String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
        this.mainRenderTarget = net.neoforged.neoforge.client.ClientHooks.instantiateMainTarget(this.window.getWidth(), this.window.getHeight());
        this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
        net.neoforged.neoforge.client.loading.ClientModLoader.finish(this.resourcePackRepository, this.resourceManager);
        //Move client bootstrap to after mod loading so that events can be fired for it.
        ClientBootstrap.bootstrap();
        this.resourcePackRepository.reload();
        this.options.loadSelectedResourcePacks(this.resourcePackRepository);
        this.languageManager = new LanguageManager(this.options.languageCode, p_344151_ -> {
            if (this.player != null) {
                this.player.connection.updateSearchTrees();
            }
        });
        this.resourceManager.registerReloadListener(this.languageManager);
        this.textureManager = new TextureManager(this.resourceManager);
        this.resourceManager.registerReloadListener(this.textureManager);
        this.shaderManager = new ShaderManager(this.textureManager, this::triggerResourcePackRecovery);
        this.resourceManager.registerReloadListener(this.shaderManager);
        SkinTextureDownloader skintexturedownloader = new SkinTextureDownloader(this.proxy, this.textureManager, this);
        this.skinManager = new SkinManager(file1.toPath().resolve("skins"), this.services, skintexturedownloader, this);
        this.levelSource = new LevelStorageSource(path.resolve("saves"), path.resolve("backups"), this.directoryValidator, this.fixerUpper);
        this.commandHistory = new CommandHistory(path);
        this.musicManager = new MusicManager(this);
        this.soundManager = new SoundManager(this.options, this.musicManager);
        this.resourceManager.registerReloadListener(this.soundManager);
        this.splashManager = new SplashManager(this.user);
        this.resourceManager.registerReloadListener(this.splashManager);
        this.atlasManager = new AtlasManager(this.textureManager, this.options.mipmapLevels().get());
        this.resourceManager.registerReloadListener(this.atlasManager);
        ProfileResolver profileresolver = new LocalPlayerResolver(this, this.services.profileResolver());
        this.playerSkinRenderCache = new PlayerSkinRenderCache(this.textureManager, this.skinManager, profileresolver);
        ClientMannequin.registerOverrides(this.playerSkinRenderCache);
        this.fontManager = new FontManager(this.textureManager, this.atlasManager, this.playerSkinRenderCache);
        this.font = this.fontManager.createFont();
        this.fontFilterFishy = this.fontManager.createFontFilterFishy();
        this.resourceManager.registerReloadListener(this.fontManager);
        this.updateFontOptions();
        this.resourceManager.registerReloadListener(new GrassColorReloadListener());
        this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
        this.resourceManager.registerReloadListener(new DryFoliageColorReloadListener());
        this.window.setErrorSection("Startup");
        RenderSystem.setupDefaultState();
        this.window.setErrorSection("Post startup");
        this.blockColors = BlockColors.createDefault();
        net.neoforged.neoforge.client.model.UnbakedModelParser.init();
        this.modelManager = new ModelManager(this.blockColors, this.atlasManager, this.playerSkinRenderCache);
        this.resourceManager.registerReloadListener(this.modelManager);
        EquipmentAssetManager equipmentassetmanager = new EquipmentAssetManager();
        this.resourceManager.registerReloadListener(equipmentassetmanager);
        this.itemModelResolver = new ItemModelResolver(this.modelManager);
        this.itemRenderer = new ItemRenderer();
        this.mapTextureManager = new MapTextureManager(this.textureManager);
        this.mapRenderer = new MapRenderer(this.atlasManager, this.mapTextureManager);

        try {
            int i = Runtime.getRuntime().availableProcessors();
            Tesselator.init();
            this.renderBuffers = new RenderBuffers(i);
        } catch (OutOfMemoryError outofmemoryerror) {
            TinyFileDialogs.tinyfd_messageBox(
                "Minecraft",
                "Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
                    + CommonLinks.GENERAL_HELP,
                "ok",
                "error",
                true
            );
            throw new SilentInitException("Unable to allocate render buffers", outofmemoryerror);
        }

        this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
        this.blockRenderer = new BlockRenderDispatcher(
            this.modelManager.getBlockModelShaper(), this.atlasManager, this.modelManager.specialBlockModelRenderer(), this.blockColors
        );
        this.resourceManager.registerReloadListener(this.blockRenderer);
        this.entityRenderDispatcher = new EntityRenderDispatcher(
            this,
            this.textureManager,
            this.itemModelResolver,
            this.itemRenderer,
            this.mapRenderer,
            this.blockRenderer,
            this.atlasManager,
            this.font,
            this.options,
            this.modelManager.entityModels(),
            equipmentassetmanager,
            this.playerSkinRenderCache
        );
        this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
        this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(
            this.font,
            this.modelManager.entityModels(),
            this.blockRenderer,
            this.itemModelResolver,
            this.itemRenderer,
            this.entityRenderDispatcher,
            this.atlasManager,
            this.playerSkinRenderCache
        );
        this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
        this.particleResources = new ParticleResources();
        this.resourceManager.registerReloadListener(this.particleResources);
        this.particleEngine = new ParticleEngine(this.level, this.particleResources);
        this.particleResources.onReload(this.particleEngine::clearParticles);
        net.neoforged.neoforge.client.ClientHooks.onRegisterParticleProviders(this.particleResources);
        this.waypointStyles = new WaypointStyleManager();
        this.resourceManager.registerReloadListener(this.waypointStyles);
        this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.renderBuffers, this.blockRenderer);
        this.levelRenderer = new LevelRenderer(
            this,
            this.entityRenderDispatcher,
            this.blockEntityRenderDispatcher,
            this.renderBuffers,
            this.gameRenderer.getLevelRenderState(),
            this.gameRenderer.getFeatureRenderDispatcher()
        );
        this.resourceManager.registerReloadListener(this.levelRenderer);
        this.resourceManager.registerReloadListener(this.levelRenderer.getCloudRenderer());
        this.gpuWarnlistManager = new GpuWarnlistManager();
        this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
        this.resourceManager.registerReloadListener(this.regionalCompliancies);
        // NEO: Moved keyboard and mouse handler setup below ingame gui creation to prevent NPEs in them.
        this.mouseHandler.setup(this.window);
        this.keyboardHandler.setup(this.window);
        this.gui = new Gui(this);
        RealmsClient realmsclient = RealmsClient.getOrCreate(this);
        this.realmsDataFetcher = new RealmsDataFetcher(realmsclient);
        RenderSystem.setErrorCallback(this::onFullscreenError);
        if (this.mainRenderTarget.width != this.window.getWidth() || this.mainRenderTarget.height != this.window.getHeight()) {
            StringBuilder stringbuilder = new StringBuilder(
                "Recovering from unsupported resolution ("
                    + this.window.getWidth()
                    + "x"
                    + this.window.getHeight()
                    + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions)."
            );

            try {
                GpuDevice gpudevice = RenderSystem.getDevice();
                List<String> list = gpudevice.getLastDebugMessages();
                if (!list.isEmpty()) {
                    stringbuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", list));
                }
            } catch (Throwable throwable) {
            }

            this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
            TinyFileDialogs.tinyfd_messageBox("Minecraft", stringbuilder.toString(), "ok", "error", false);
        } else if (this.options.fullscreen().get() && !this.window.isFullscreen()) {
            if (flag) {
                this.window.toggleFullScreen();
                this.options.fullscreen().set(this.window.isFullscreen());
            } else {
                this.options.fullscreen().set(false);
            }
        }

        net.neoforged.neoforge.client.ClientHooks.initClientHooks(this, this.resourceManager);
        this.window.updateVsync(this.options.enableVsync().get());
        this.window.updateRawMouseInput(this.options.rawMouseInput().get());
        this.window.setAllowCursorChanges(this.options.allowCursorChanges().get());
        this.window.setDefaultErrorCallback();
        this.resizeDisplay();
        this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
        this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
        this.profileKeyPairManager = this.offlineDeveloperMode
            ? ProfileKeyPairManager.EMPTY_KEY_MANAGER
            : ProfileKeyPairManager.create(this.userApiService, this.user, path);
        this.narrator = new GameNarrator(this);
        this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
        this.chatListener = new ChatListener(this);
        this.chatListener.setMessageDelay(this.options.chatDelay().get());
        this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
        TitleScreen.registerTextures(this.textureManager);
        LoadingOverlay.registerTextures(this.textureManager);
        this.gameRenderer.getPanorama().registerTextures(this.textureManager);
        this.setScreen(new GenericMessageScreen(Component.translatable("gui.loadingMinecraft")));
        List<PackResources> list1 = this.resourcePackRepository.openAllSelected();
        this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, list1);
        ReloadInstance reloadinstance = this.resourceManager
            .createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list1);
        GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
        Minecraft.GameLoadCookie minecraft$gameloadcookie = new Minecraft.GameLoadCookie(realmsclient, gameConfig.quickPlay);
        this.setOverlay(
            net.neoforged.neoforge.client.ClientHooks.createLoadingOverlay(
                this, reloadinstance, p_299779_ -> Util.ifElse(p_299779_, p_299772_ -> this.rollbackResourcePacks(p_299772_, minecraft$gameloadcookie), () -> {
                    if (SharedConstants.IS_RUNNING_IN_IDE && false /* Neo: Disable self-test */) {
                        this.selfTest();
                    }

                    this.reloadStateTracker.finishReload();
                    this.onResourceLoadFinished(minecraft$gameloadcookie);
                }), false
            )
        );
        this.quickPlayLog = QuickPlayLog.of(gameConfig.quickPlay.logPath());
        this.framerateLimitTracker = new FramerateLimitTracker(this.options, this);
        this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> this.fpsPieRenderTicks, this.framerateLimitTracker::isHeavilyThrottled);
        if (TracyClient.isAvailable() && gameConfig.game.captureTracyImages) {
            this.tracyFrameCapture = new TracyFrameCapture();
        } else {
            this.tracyFrameCapture = null;
        }

        this.packetProcessor = new PacketProcessor(this.gameThread);
    }

    public boolean hasShiftDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
    }

    public boolean hasControlDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, InputQuirks.EDIT_SHORTCUT_KEY_LEFT) || InputConstants.isKeyDown(window, InputQuirks.EDIT_SHORTCUT_KEY_RIGHT);
    }

    public boolean hasAltDown() {
        Window window = this.getWindow();
        return InputConstants.isKeyDown(window, 342) || InputConstants.isKeyDown(window, 346);
    }

    private void onResourceLoadFinished(@Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        net.neoforged.neoforge.client.ClientHooks.fireResourceLoadFinishedEvent(!this.gameLoadFinished);
        if (!this.gameLoadFinished) {
            this.gameLoadFinished = true;
            this.onGameLoadFinished(gameLoadCookie);
        }
    }

    private void onGameLoadFinished(@Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        Runnable runnable = this.buildInitialScreens(gameLoadCookie);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
        GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
        GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
        runnable.run();
        this.options.startedCleanly = true;
        this.options.save();
    }

    public boolean isGameLoadFinished() {
        return this.gameLoadFinished;
    }

    private Runnable buildInitialScreens(@Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        List<Function<Runnable, Screen>> list = new ArrayList<>();
        boolean flag = this.addInitialScreens(list);
        Runnable runnable = () -> {
            if (gameLoadCookie != null && gameLoadCookie.quickPlayData.isEnabled()) {
                QuickPlay.connect(this, gameLoadCookie.quickPlayData.variant(), gameLoadCookie.realmsClient());
            } else {
                this.setScreen(new TitleScreen(true, new LogoRenderer(flag)));
            }
        };

        for (Function<Runnable, Screen> function : Lists.reverse(list)) {
            Screen screen = function.apply(runnable);
            runnable = () -> this.setScreen(screen);
        }

        runnable = net.neoforged.neoforge.client.loading.ClientModLoader.completeModLoading(runnable);

        return runnable;
    }

    private boolean addInitialScreens(List<Function<Runnable, Screen>> output) {
        boolean flag = false;
        if (this.options.onboardAccessibility || SharedConstants.DEBUG_FORCE_ONBOARDING_SCREEN) {
            output.add(p_299781_ -> new AccessibilityOnboardingScreen(this.options, p_299781_));
            flag = true;
        }

        BanDetails bandetails = this.multiplayerBan();
        if (bandetails != null) {
            output.add(p_299775_ -> BanNoticeScreens.create(p_351634_ -> {
                if (p_351634_) {
                    Util.getPlatform().openUri(CommonLinks.SUSPENSION_HELP);
                }

                p_299775_.run();
            }, bandetails));
        }

        ProfileResult profileresult = this.profileFuture.join();
        if (profileresult != null) {
            GameProfile gameprofile = profileresult.profile();
            Set<ProfileActionType> set = profileresult.actions();
            if (set.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
                output.add(p_442306_ -> BanNoticeScreens.createNameBan(gameprofile.name(), p_442306_));
            }

            if (set.contains(ProfileActionType.USING_BANNED_SKIN)) {
                output.add(BanNoticeScreens::createSkinBan);
            }
        }

        return flag;
    }

    private static boolean countryEqualsISO3(Object country) {
        try {
            return Locale.getDefault().getISO3Country().equals(country);
        } catch (MissingResourceException missingresourceexception) {
            return false;
        }
    }

    public void updateTitle() {
        this.window.setTitle(this.createTitle());
    }

    private String createTitle() {
        StringBuilder stringbuilder = new StringBuilder("Minecraft");
        if (checkModStatus().shouldReportAsModified()) {
            stringbuilder.append(' ').append(net.neoforged.neoforge.internal.BrandingControl.BRANDING_NAME).append('*');
        }

        stringbuilder.append(" ");
        stringbuilder.append(SharedConstants.getCurrentVersion().name());
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null && clientpacketlistener.getConnection().isConnected()) {
            stringbuilder.append(" - ");
            ServerData serverdata = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                stringbuilder.append(I18n.get("title.singleplayer"));
            } else if (serverdata != null && serverdata.isRealm()) {
                stringbuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (serverdata == null || !serverdata.isLan())) {
                stringbuilder.append(I18n.get("title.multiplayer.other"));
            } else {
                stringbuilder.append(I18n.get("title.multiplayer.lan"));
            }
        }

        return stringbuilder.toString();
    }

    private UserApiService createUserApiService(YggdrasilAuthenticationService authenticationService, GameConfig gameConfig) {
        return gameConfig.game.offlineDeveloperMode ? UserApiService.OFFLINE : authenticationService.createUserApiService(gameConfig.user.user.getAccessToken());
    }

    public boolean isOfflineDeveloperMode() {
        return this.offlineDeveloperMode;
    }

    public static ModCheck checkModStatus() {
        return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
    }

    private void rollbackResourcePacks(Throwable throwable, @Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        if (this.resourcePackRepository.getSelectedPacks().stream().anyMatch(e -> !e.isRequired())) { //Forge: This caused infinite loop if any resource packs are forced. Such as mod resources. So check if we can disable any.
            this.clearResourcePacksOnError(throwable, null, gameLoadCookie);
        } else {
            Util.throwAsRuntime(throwable);
        }
    }

    public void clearResourcePacksOnError(Throwable throwable, @Nullable Component errorMessage, @Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", throwable);
        this.reloadStateTracker.startRecovery(throwable);
        this.downloadedPackSource.onRecovery();
        this.resourcePackRepository.setSelected(Collections.emptyList());
        this.options.resourcePacks.clear();
        this.options.incompatibleResourcePacks.clear();
        this.options.save();
        this.reloadResourcePacks(true, gameLoadCookie).thenRunAsync(() -> this.addResourcePackLoadFailToast(errorMessage), this);
    }

    private void abortResourcePackRecovery() {
        this.setOverlay(null);
        if (this.level != null) {
            this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
            this.disconnectWithProgressScreen();
        }

        this.setScreen(new TitleScreen());
        this.addResourcePackLoadFailToast(null);
    }

    private void addResourcePackLoadFailToast(@Nullable Component message) {
        ToastManager toastmanager = this.getToastManager();
        SystemToast.addOrUpdate(toastmanager, SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), message);
    }

    public void triggerResourcePackRecovery(Exception error) {
        if (!this.resourcePackRepository.isAbleToClearAnyPack()) {
            if (this.resourcePackRepository.getSelectedIds().size() <= 1) {
                LOGGER.error(LogUtils.FATAL_MARKER, error.getMessage(), (Throwable)error);
                this.emergencySaveAndCrash(new CrashReport(error.getMessage(), error));
            } else {
                this.schedule(this::abortResourcePackRecovery);
            }
        } else {
            this.clearResourcePacksOnError(error, Component.translatable("resourcePack.runtime_failure"), null);
        }
    }

    public void run() {
        this.gameThread = Thread.currentThread();
        if (Runtime.getRuntime().availableProcessors() > 4) {
            this.gameThread.setPriority(10);
        }

        DiscontinuousFrame discontinuousframe = TracyClient.createDiscontinuousFrame("Client Tick");

        try {
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStarted(this);

            boolean flag = false;

            while (this.running) {
                this.handleDelayedCrash();

                try {
                    SingleTickProfiler singletickprofiler = SingleTickProfiler.createTickProfiler("Renderer");
                    boolean flag1 = this.getDebugOverlay().showProfilerChart();

                    try (Profiler.Scope profiler$scope = Profiler.use(this.constructProfiler(flag1, singletickprofiler))) {
                        this.metricsRecorder.startTick();
                        discontinuousframe.start();
                        this.runTick(!flag);
                        discontinuousframe.end();
                        this.metricsRecorder.endTick();
                    }

                    this.finishProfilers(flag1, singletickprofiler);
                } catch (OutOfMemoryError outofmemoryerror) {
                    if (flag) {
                        throw outofmemoryerror;
                    }

                    this.emergencySave();
                    this.setScreen(new OutOfMemoryScreen());
                    System.gc();
                    LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", (Throwable)outofmemoryerror);
                    flag = true;
                }
            }
        } catch (ReportedException reportedexception) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", (Throwable)reportedexception);
            this.emergencySaveAndCrash(reportedexception.getReport());
        } catch (Throwable throwable1) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", throwable1);
            this.emergencySaveAndCrash(new CrashReport("Unexpected error", throwable1));
        }
    }

    void updateFontOptions() {
        this.fontManager.updateOptions(this.options);
    }

    private void onFullscreenError(int error, long description) {
        this.options.enableVsync().set(false);
        this.options.save();
    }

    public RenderTarget getMainRenderTarget() {
        return this.mainRenderTarget;
    }

    public String getLaunchedVersion() {
        return this.launchedVersion;
    }

    public String getVersionType() {
        return this.versionType;
    }

    public void delayCrash(CrashReport report) {
        this.delayedCrash = () -> this.fillReport(report);
    }

    public void delayCrashRaw(CrashReport report) {
        this.delayedCrash = () -> report;
    }

    private void handleDelayedCrash() {
        if (this.delayedCrash != null) {
            crash(this, this.gameDirectory, this.delayedCrash.get());
        }
    }

    public void emergencySaveAndCrash(CrashReport crashReport) {
        MemoryReserve.release();
        CrashReport crashreport = this.fillReport(crashReport);
        this.emergencySave();
        crash(this, this.gameDirectory, crashreport);
    }

    public static int saveReport(File file, CrashReport crashReport) {
        Path path = file.toPath().resolve("crash-reports");
        Path path1 = path.resolve("crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Bootstrap.realStdoutPrintln(crashReport.getFriendlyReport(ReportType.CRASH));
        if (crashReport.getSaveFile() != null) {
            Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReport.getSaveFile().toAbsolutePath());
            return -1;
        } else if (crashReport.saveToFile(path1, ReportType.CRASH)) {
            Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + path1.toAbsolutePath());
            return -1;
        } else {
            Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
            return -2;
        }
    }

    public static void crash(@Nullable Minecraft minecraft, File gameDirectory, CrashReport crashReport) {
        int i = saveReport(gameDirectory, crashReport);
        if (minecraft != null) {
            minecraft.soundManager.emergencyShutdown();
        }

        net.neoforged.neoforge.server.ServerLifecycleHooks.handleExit(i);
    }

    public boolean isEnforceUnicode() {
        return this.options.forceUnicodeFont().get();
    }

    public CompletableFuture<Void> reloadResourcePacks() {
        return this.reloadResourcePacks(false, null);
    }

    private CompletableFuture<Void> reloadResourcePacks(boolean error, @Nullable Minecraft.GameLoadCookie gameLoadCookie) {
        if (this.pendingReload != null) {
            return this.pendingReload;
        } else {
            CompletableFuture<Void> completablefuture = new CompletableFuture<>();
            if (!error && this.overlay instanceof LoadingOverlay) {
                this.pendingReload = completablefuture;
                return completablefuture;
            } else {
                this.resourcePackRepository.reload();
                List<PackResources> list = this.resourcePackRepository.openAllSelected();
                if (!error) {
                    this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, list);
                }

                this.setOverlay(
                    new LoadingOverlay(
                        this,
                        this.resourceManager.createReload(Util.backgroundExecutor().forName("resourceLoad"), this, RESOURCE_RELOAD_INITIAL_TASK, list),
                        p_299767_ -> Util.ifElse(p_299767_, p_314392_ -> {
                            if (error) {
                                this.downloadedPackSource.onRecoveryFailure();
                                this.abortResourcePackRecovery();
                            } else {
                                this.rollbackResourcePacks(p_314392_, gameLoadCookie);
                            }
                        }, () -> {
                            this.levelRenderer.allChanged();
                            this.reloadStateTracker.finishReload();
                            this.downloadedPackSource.onReloadSuccess();
                            completablefuture.complete(null);
                            this.onResourceLoadFinished(gameLoadCookie);
                        }),
                        !error
                    )
                );
                return completablefuture;
            }
        }
    }

    private void selfTest() {
        boolean flag = false;
        BlockModelShaper blockmodelshaper = this.getBlockRenderer().getBlockModelShaper();
        BlockStateModel blockstatemodel = blockmodelshaper.getModelManager().getMissingBlockStateModel();

        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState blockstate : block.getStateDefinition().getPossibleStates()) {
                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel blockstatemodel1 = blockmodelshaper.getBlockModel(blockstate);
                    if (blockstatemodel1 == blockstatemodel) {
                        LOGGER.debug("Missing model for: {}", blockstate);
                        flag = true;
                    }
                }
            }
        }

        TextureAtlasSprite textureatlassprite1 = blockstatemodel.particleIcon();

        for (Block block1 : BuiltInRegistries.BLOCK) {
            for (BlockState blockstate1 : block1.getStateDefinition().getPossibleStates()) {
                TextureAtlasSprite textureatlassprite = blockmodelshaper.getParticleIcon(blockstate1);
                if (!blockstate1.isAir() && textureatlassprite == textureatlassprite1) {
                    LOGGER.debug("Missing particle icon for: {}", blockstate1);
                }
            }
        }

        BuiltInRegistries.ITEM.listElements().forEach(p_370276_ -> {
            Item item = p_370276_.value();
            String s = item.getDescriptionId();
            String s1 = Component.translatable(s).getString();
            if (s1.toLowerCase(Locale.ROOT).equals(item.getDescriptionId())) {
                LOGGER.debug("Missing translation for: {} {} {}", p_370276_.key().location(), s, item);
            }
        });
        flag |= MenuScreens.selfTest();
        flag |= EntityRenderers.validateRegistrations();
        if (flag) {
            throw new IllegalStateException("Your game data is foobar, fix the errors above!");
        }
    }

    public LevelStorageSource getLevelSource() {
        return this.levelSource;
    }

    public void openChatScreen(ChatComponent.ChatMethod chatMethod) {
        Minecraft.ChatStatus minecraft$chatstatus = this.getChatStatus();
        if (!minecraft$chatstatus.isChatAllowed(this.isLocalServer())) {
            if (this.gui.isShowingChatDisabledByPlayer()) {
                this.gui.setChatDisabledByPlayerShown(false);
                this.setScreen(new ConfirmLinkScreen(p_351635_ -> {
                    if (p_351635_) {
                        Util.getPlatform().openUri(CommonLinks.ACCOUNT_SETTINGS);
                    }

                    this.setScreen(null);
                }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, CommonLinks.ACCOUNT_SETTINGS, true));
            } else {
                Component component = minecraft$chatstatus.getMessage();
                this.gui.setOverlayMessage(component, false);
                this.narrator.saySystemNow(component);
                this.gui.setChatDisabledByPlayerShown(minecraft$chatstatus == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
            }
        } else {
            this.gui.getChat().openScreen(chatMethod, ChatScreen::new);
        }
    }

    public void setScreen(@Nullable Screen guiScreen) {
        if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
            LOGGER.error("setScreen called from non-game thread");
        }

        if (this.screen == null) {
            this.setLastInputType(InputType.NONE);
        }

        if (guiScreen == null) {
            if (this.clientLevelTeardownInProgress) {
                throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
            }

            if (this.level == null) {
                guiScreen = new TitleScreen();
            } else if (this.player.isDeadOrDying()) {
                if (this.player.shouldShowDeathScreen()) {
                    guiScreen = new DeathScreen(null, this.level.getLevelData().isHardcore());
                } else {
                    this.player.respawn();
                }
            } else {
                guiScreen = this.gui.getChat().restoreChatScreen();
            }
        }

        net.neoforged.neoforge.client.ClientHooks.clearGuiLayers(this);
        Screen old = this.screen;
        if (guiScreen != null) {
            var event = new net.neoforged.neoforge.client.event.ScreenEvent.Opening(old, guiScreen);
            if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
            guiScreen = event.getNewScreen();
        }

        if (old != null && guiScreen != old) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Closing(old));
            old.removed();
        }

        this.screen = guiScreen;
        if (this.screen != null) {
            this.screen.added();
        }

        if (guiScreen != null) {
            this.mouseHandler.releaseMouse();
            KeyMapping.releaseAll();
            guiScreen.init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            this.noRender = false;
        } else {
            if (this.level != null) {
                KeyMapping.restoreToggleStatesOnScreenClosed();
            }

            this.soundManager.resume();
            this.mouseHandler.grabMouse();
        }

        this.updateTitle();
    }

    public void setOverlay(@Nullable Overlay loadingGui) {
        this.overlay = loadingGui;
    }

    public void destroy() {
        try {
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStopping(this);
            LOGGER.info("Stopping!");

            try {
                this.narrator.destroy();
            } catch (Throwable throwable1) {
            }

            try {
                if (this.level != null) {
                    this.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
                }

                this.disconnectWithProgressScreen();
            } catch (Throwable throwable) {
            }

            if (this.screen != null) {
                this.screen.removed();
            }

            this.close();
            net.neoforged.neoforge.client.ClientLifecycleHooks.handleClientStopped(this);
        } finally {
            Util.timeSource = System::nanoTime;
            if (this.delayedCrash == null) {
                System.exit(0);
            }
        }
    }

    @Override
    public void close() {
        if (this.currentFrameProfile != null) {
            this.currentFrameProfile.cancel();
        }

        try {
            this.telemetryManager.close();
            this.regionalCompliancies.close();
            this.atlasManager.close();
            this.fontManager.close();
            this.gameRenderer.close();
            this.shaderManager.close();
            this.levelRenderer.close();
            this.soundManager.destroy();
            this.mapTextureManager.close();
            this.textureManager.close();
            this.resourceManager.close();
            if (this.tracyFrameCapture != null) {
                this.tracyFrameCapture.close();
            }

            FreeTypeUtil.destroy();
            Util.shutdownExecutors();
            RenderSystem.getDevice().close();
        } catch (Throwable throwable) {
            LOGGER.error("Shutdown failure!", throwable);
            throw throwable;
        } finally {
            this.virtualScreen.close();
            this.window.close();
        }
    }

    private void runTick(boolean renderLevel) {
        this.window.setErrorSection("Pre render");
        if (this.window.shouldClose()) {
            this.stop();
        }

        if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
            CompletableFuture<Void> completablefuture = this.pendingReload;
            this.pendingReload = null;
            this.reloadResourcePacks().thenRun(() -> completablefuture.complete(null));
        }

        int i1 = this.deltaTracker.advanceTime(Util.getMillis(), renderLevel);
        ProfilerFiller profilerfiller = Profiler.get();
        if (renderLevel) {
            profilerfiller.push("scheduledPacketProcessing");
            this.packetProcessor.processQueuedPackets();
            profilerfiller.popPush("scheduledExecutables");
            this.runAllTasks();
            profilerfiller.popPush("tick");

            for (int i = 0; i < Math.min(10, i1); i++) {
                profilerfiller.incrementCounter("clientTick");
                this.tick();
            }

            profilerfiller.pop();
        }

        this.window.setErrorSection("Render");
        profilerfiller.push("gpuAsync");
        RenderSystem.executePendingTasks();
        profilerfiller.popPush("sound");
        this.soundManager.updateSource(this.gameRenderer.getMainCamera());
        profilerfiller.popPush("toasts");
        this.toastManager.update();
        profilerfiller.popPush("mouse");
        this.mouseHandler.handleAccumulatedMovement();
        profilerfiller.popPush("render");
        long j1 = Util.getNanos();
        boolean flag;
        if (!this.debugEntries.isCurrentlyEnabled(DebugScreenEntries.GPU_UTILIZATION) && !this.metricsRecorder.isRecording()) {
            flag = false;
            this.gpuUtilization = 0.0;
        } else {
            flag = (this.currentFrameProfile == null || this.currentFrameProfile.isDone()) && !TimerQuery.getInstance().isRecording();
            if (flag) {
                TimerQuery.getInstance().beginProfile();
            }
        }

        RenderTarget rendertarget = this.getMainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
        profilerfiller.push("gameRenderer");
        if (!this.noRender) {
            net.neoforged.neoforge.client.ClientHooks.fireRenderFramePre(this.deltaTracker);
            this.gameRenderer.render(this.deltaTracker, renderLevel);
            net.neoforged.neoforge.client.ClientHooks.fireRenderFramePost(this.deltaTracker);
        }

        profilerfiller.popPush("blit");
        if (!this.window.isMinimized()) {
            rendertarget.blitToScreen();
        }

        this.frameTimeNs = Util.getNanos() - j1;
        if (flag) {
            this.currentFrameProfile = TimerQuery.getInstance().endProfile();
        }

        profilerfiller.popPush("updateDisplay");
        if (this.tracyFrameCapture != null) {
            this.tracyFrameCapture.upload();
            this.tracyFrameCapture.capture(rendertarget);
        }

        this.window.updateDisplay(this.tracyFrameCapture);
        int j = this.framerateLimitTracker.getFramerateLimit();
        if (j < 260) {
            RenderSystem.limitDisplayFPS(j);
        }

        profilerfiller.pop();
        profilerfiller.popPush("yield");
        Thread.yield();
        profilerfiller.pop();
        this.window.setErrorSection("Post render");
        this.frames++;
        boolean flag1 = this.pause;
        boolean pause = this.hasSingleplayerServer()
            && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen())
            && !this.singleplayerServer.isPublished();
        if (pause != this.pause && !net.neoforged.neoforge.client.ClientHooks.onClientPauseChangePre(pause)) {
            this.pause = pause;
            net.neoforged.neoforge.client.ClientHooks.onClientPauseChangePost(pause);
        }
        if (!flag1 && this.pause) {
            this.soundManager.pauseAllExcept(SoundSource.MUSIC, SoundSource.UI);
        }

        this.deltaTracker.updatePauseState(this.pause);
        this.deltaTracker.updateFrozenState(!this.isLevelRunningNormally());
        long k = Util.getNanos();
        long l = k - this.lastNanoTime;
        if (flag) {
            this.savedCpuDuration = l;
        }

        this.getDebugOverlay().logFrameDuration(l);
        this.lastNanoTime = k;
        profilerfiller.push("fpsUpdate");
        if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
            this.gpuUtilization = this.currentFrameProfile.get() * 100.0 / this.savedCpuDuration;
        }

        while (Util.getMillis() >= this.lastTime + 1000L) {
            fps = this.frames;
            this.lastTime += 1000L;
            this.frames = 0;
        }

        profilerfiller.pop();
    }

    private ProfilerFiller constructProfiler(boolean renderFpsPie, @Nullable SingleTickProfiler singleTickProfiler) {
        if (!renderFpsPie) {
            this.fpsPieProfiler.disable();
            if (!this.metricsRecorder.isRecording() && singleTickProfiler == null) {
                return InactiveProfiler.INSTANCE;
            }
        }

        ProfilerFiller profilerfiller;
        if (renderFpsPie) {
            if (!this.fpsPieProfiler.isEnabled()) {
                this.fpsPieRenderTicks = 0;
                this.fpsPieProfiler.enable();
            }

            this.fpsPieRenderTicks++;
            profilerfiller = this.fpsPieProfiler.getFiller();
        } else {
            profilerfiller = InactiveProfiler.INSTANCE;
        }

        if (this.metricsRecorder.isRecording()) {
            profilerfiller = ProfilerFiller.combine(profilerfiller, this.metricsRecorder.getProfiler());
        }

        return SingleTickProfiler.decorateFiller(profilerfiller, singleTickProfiler);
    }

    private void finishProfilers(boolean renderFpsPie, @Nullable SingleTickProfiler profiler) {
        if (profiler != null) {
            profiler.endTick();
        }

        ProfilerPieChart profilerpiechart = this.getDebugOverlay().getProfilerPieChart();
        if (renderFpsPie) {
            profilerpiechart.setPieChartResults(this.fpsPieProfiler.getResults());
        } else {
            profilerpiechart.setPieChartResults(null);
        }
    }

    @Override
    public void resizeDisplay() {
        int i = this.window.calculateScale(this.options.guiScale().get(), this.isEnforceUnicode());
        this.window.setGuiScale(i);
        if (this.screen != null) {
            this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
            net.neoforged.neoforge.client.ClientHooks.resizeGuiLayers(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
        }

        RenderTarget rendertarget = this.getMainRenderTarget();
        if (rendertarget != null)
        rendertarget.resize(this.window.getWidth(), this.window.getHeight());
        if (this.gameRenderer != null)
        this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
        this.mouseHandler.setIgnoreFirstMove();
    }

    @Override
    public void cursorEntered() {
        this.mouseHandler.cursorEntered();
    }

    public int getFps() {
        return fps;
    }

    public long getFrameTimeNs() {
        return this.frameTimeNs;
    }

    private void emergencySave() {
        MemoryReserve.release();

        try {
            if (this.isLocalServer && this.singleplayerServer != null) {
                this.singleplayerServer.halt(true);
            }

            this.disconnectWithSavingScreen();
        } catch (Throwable throwable) {
        }

        System.gc();
    }

    public boolean debugClientMetricsStart(Consumer<Component> logger) {
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsStop();
            return false;
        } else {
            Consumer<ProfileResults> consumer = p_231435_ -> {
                if (p_231435_ != EmptyProfileResults.EMPTY) {
                    int i = p_231435_.getTickDuration();
                    double d0 = (double)p_231435_.getNanoDuration() / TimeUtil.NANOSECONDS_PER_SECOND;
                    this.execute(
                        () -> logger.accept(
                            Component.translatable(
                                "commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d0), i, String.format(Locale.ROOT, "%.2f", i / d0)
                            )
                        )
                    );
                }
            };
            Consumer<Path> consumer1 = p_231438_ -> {
                Component component = Component.literal(p_231438_.toString())
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(p_392490_ -> p_392490_.withClickEvent(new ClickEvent.OpenFile(p_231438_.getParent())));
                this.execute(() -> logger.accept(Component.translatable("debug.profiling.stop", component)));
            };
            SystemReport systemreport = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
            Consumer<List<Path>> consumer2 = p_231349_ -> {
                Path path = this.archiveProfilingReport(systemreport, p_231349_);
                consumer1.accept(path);
            };
            Consumer<Path> consumer3;
            if (this.singleplayerServer == null) {
                consumer3 = p_231404_ -> consumer2.accept(ImmutableList.of(p_231404_));
            } else {
                this.singleplayerServer.fillSystemReport(systemreport);
                CompletableFuture<Path> completablefuture = new CompletableFuture<>();
                CompletableFuture<Path> completablefuture1 = new CompletableFuture<>();
                CompletableFuture.allOf(completablefuture, completablefuture1)
                    .thenRunAsync(() -> consumer2.accept(ImmutableList.of(completablefuture.join(), completablefuture1.join())), Util.ioPool());
                this.singleplayerServer.startRecordingMetrics(p_231351_ -> {}, completablefuture1::complete);
                consumer3 = completablefuture::complete;
            }

            this.metricsRecorder = ActiveMetricsRecorder.createStarted(
                new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer),
                Util.timeSource,
                Util.ioPool(),
                new MetricsPersister("client"),
                p_231401_ -> {
                    this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
                    consumer.accept(p_231401_);
                },
                consumer3
            );
            return true;
        }
    }

    private void debugClientMetricsStop() {
        this.metricsRecorder.end();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.finishRecordingMetrics();
        }
    }

    private void debugClientMetricsCancel() {
        this.metricsRecorder.cancel();
        if (this.singleplayerServer != null) {
            this.singleplayerServer.cancelRecordingMetrics();
        }
    }

    private Path archiveProfilingReport(SystemReport report, List<Path> paths) {
        String s;
        if (this.isLocalServer()) {
            s = this.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData serverdata = this.getCurrentServer();
            s = serverdata != null ? serverdata.name : "unknown";
        }

        Path path;
        try {
            String s2 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), s, SharedConstants.getCurrentVersion().id());
            String s1 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, s2, ".zip");
            path = MetricsPersister.PROFILING_RESULTS_DIR.resolve(s1);
        } catch (IOException ioexception1) {
            throw new UncheckedIOException(ioexception1);
        }

        try (FileZipper filezipper = new FileZipper(path)) {
            filezipper.add(Paths.get("system.txt"), report.toLineSeparatedString());
            filezipper.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
            paths.forEach(filezipper::add);
        } finally {
            for (Path path1 : paths) {
                try {
                    FileUtils.forceDelete(path1.toFile());
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to delete temporary profiling result {}", path1, ioexception);
                }
            }
        }

        return path;
    }

    public void stop() {
        if (this.isRunning()) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.GameShuttingDownEvent());
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * Displays the in-game menu
     */
    public void pauseGame(boolean pauseOnly) {
        if (this.screen == null) {
            boolean flag = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
            if (flag) {
                this.setScreen(new PauseScreen(!pauseOnly));
            } else {
                this.setScreen(new PauseScreen(true));
            }
        }
    }

    private void continueAttack(boolean leftClick) {
        if (!leftClick) {
            this.missTime = 0;
        }

        if (this.missTime <= 0 && !this.player.isUsingItem()) {
            if (leftClick && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                BlockPos blockpos = blockhitresult.getBlockPos();
                if (!this.level.getBlockState(blockpos).isAir()) {
                    Direction direction = blockhitresult.getDirection();
                    var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                    if (inputEvent.isCanceled()) {
                        if (inputEvent.shouldSwingHand()) {
                            this.level.addBreakingBlockEffect(blockpos, direction, blockhitresult);
                            this.player.swing(InteractionHand.MAIN_HAND);
                        }
                        return;
                    }
                    if (this.gameMode.continueDestroyBlock(blockpos, direction) && inputEvent.shouldSwingHand()) {
                        this.level.addBreakingBlockEffect(blockpos, direction, blockhitresult);
                        this.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            } else {
                this.gameMode.stopDestroyBlock();
            }
        }
    }

    private boolean startAttack() {
        if (this.missTime > 0) {
            return false;
        } else if (this.hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.gameMode.hasMissTime()) {
                this.missTime = 10;
            }

            return false;
        } else if (this.player.isHandsBusy()) {
            return false;
        } else {
            ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                return false;
            } else {
                boolean flag = false;
                var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
                if (!inputEvent.isCanceled())
                switch (this.hitResult.getType()) {
                    case ENTITY:
                        this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
                        break;
                    case BLOCK:
                        BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                        BlockPos blockpos = blockhitresult.getBlockPos();
                        if (!this.level.getBlockState(blockpos).isAir()) {
                            this.gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
                            if (this.level.getBlockState(blockpos).isAir()) {
                                flag = true;
                            }
                            break;
                        }
                    case MISS:
                        if (this.gameMode.hasMissTime()) {
                            this.missTime = 10;
                        }

                        this.player.resetAttackStrengthTicker();
                        net.neoforged.neoforge.common.CommonHooks.onEmptyLeftClick(this.player);
                }

                if (inputEvent.shouldSwingHand())
                this.player.swing(InteractionHand.MAIN_HAND);
                return flag;
            }
        }
    }

    private void startUseItem() {
        if (!this.gameMode.isDestroying()) {
            this.rightClickDelay = 4;
            if (!this.player.isHandsBusy()) {
                if (this.hitResult == null) {
                    LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
                }

                for (InteractionHand interactionhand : InteractionHand.values()) {
                    var inputEvent = net.neoforged.neoforge.client.ClientHooks.onClickInput(1, this.options.keyUse, interactionhand);
                    if (inputEvent.isCanceled()) {
                        if (inputEvent.shouldSwingHand()) this.player.swing(interactionhand);
                        return;
                    }
                    ItemStack itemstack = this.player.getItemInHand(interactionhand);
                    if (!itemstack.isItemEnabled(this.level.enabledFeatures())) {
                        return;
                    }

                    if (this.hitResult != null) {
                        switch (this.hitResult.getType()) {
                            case ENTITY:
                                EntityHitResult entityhitresult = (EntityHitResult)this.hitResult;
                                Entity entity = entityhitresult.getEntity();
                                if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                                    return;
                                }

                                InteractionResult interactionresult = this.gameMode.interactAt(this.player, entity, entityhitresult, interactionhand);
                                if (!interactionresult.consumesAction()) {
                                    interactionresult = this.gameMode.interact(this.player, entity, interactionhand);
                                }

                                if (interactionresult instanceof InteractionResult.Success interactionresult$success2) {
                                    if (interactionresult$success2.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(interactionhand);
                                    }

                                    return;
                                }
                                break;
                            case BLOCK:
                                BlockHitResult blockhitresult = (BlockHitResult)this.hitResult;
                                int i = itemstack.getCount();
                                InteractionResult interactionresult1 = this.gameMode.useItemOn(this.player, interactionhand, blockhitresult);
                                if (interactionresult1 instanceof InteractionResult.Success interactionresult$success) {
                                    if (interactionresult$success.swingSource() == InteractionResult.SwingSource.CLIENT && inputEvent.shouldSwingHand()) {
                                        this.player.swing(interactionhand);
                                        if (!itemstack.isEmpty() && (itemstack.getCount() != i || this.player.hasInfiniteMaterials())) {
                                            this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                                        }
                                    }

                                    return;
                                }

                                if (interactionresult1 instanceof InteractionResult.Fail) {
                                    return;
                                }
                        }
                    }

                    if (itemstack.isEmpty() && (this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS))
                        net.neoforged.neoforge.common.CommonHooks.onEmptyClick(this.player, interactionhand);

                    if (!itemstack.isEmpty()
                        && this.gameMode.useItem(this.player, interactionhand) instanceof InteractionResult.Success interactionresult$success1) {
                        if (interactionresult$success1.swingSource() == InteractionResult.SwingSource.CLIENT) {
                            this.player.swing(interactionhand);
                        }

                        this.gameRenderer.itemInHandRenderer.itemUsed(interactionhand);
                        return;
                    }
                }
            }
        }
    }

    public MusicManager getMusicManager() {
        return this.musicManager;
    }

    public void tick() {
        this.clientTickCount++;
        if (this.gameLoadFinished) {
            net.neoforged.neoforge.client.ClientHooks.fireClientTickPre();
        }

        if (this.level != null && !this.pause) {
            this.level.tickRateManager().tick();
        }

        if (this.rightClickDelay > 0) {
            this.rightClickDelay--;
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("gui");
        this.chatListener.tick();
        this.gui.tick(this.pause);
        profilerfiller.pop();
        this.gameRenderer.pick(1.0F);
        this.tutorial.onLookAt(this.level, this.hitResult);
        profilerfiller.push("gameMode");
        if (!this.pause && this.level != null) {
            this.gameMode.tick();
        }

        profilerfiller.popPush("textures");
        if (this.isLevelRunningNormally()) {
            this.textureManager.tick();
        }

        if (this.screen != null || this.player == null) {
            if (this.screen instanceof InBedChatScreen inbedchatscreen && !this.player.isSleeping()) {
                inbedchatscreen.onPlayerWokeUp();
            }
        } else if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen(null);
        } else if (this.player.isSleeping() && this.level != null) {
            this.gui.getChat().openScreen(ChatComponent.ChatMethod.MESSAGE, InBedChatScreen::new);
        }

        if (this.screen != null) {
            this.missTime = 10000;
        }

        if (this.screen != null) {
            try {
                this.screen.tick();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking screen");
                this.screen.fillCrashDetails(crashreport);
                throw new ReportedException(crashreport);
            }
        }

        if (this.overlay != null) {
            this.overlay.tick();
        }

        if (!this.getDebugOverlay().showDebugScreen()) {
            this.gui.clearCache();
        }

        if (this.overlay == null && this.screen == null) {
            profilerfiller.popPush("Keybindings");
            this.handleKeybinds();
            if (this.missTime > 0) {
                this.missTime--;
            }
        }

        if (this.level != null) {
            if (!this.pause) {
                profilerfiller.popPush("gameRenderer");
                this.gameRenderer.tick();
                profilerfiller.popPush("entities");
                this.level.tickEntities();
                profilerfiller.popPush("blockEntities");
                this.level.tickBlockEntities();
            }
        } else if (this.gameRenderer.currentPostEffect() != null) {
            this.gameRenderer.clearPostEffect();
        }

        this.musicManager.tick();
        this.soundManager.tick(this.pause);
        if (this.level != null) {
            if (!this.pause) {
                profilerfiller.popPush("level");
                if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
                    Component component = Component.translatable("tutorial.socialInteractions.title");
                    Component component1 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
                    this.socialInteractionsToast = new TutorialToast(this.font, TutorialToast.Icons.SOCIAL_INTERACTIONS, component, component1, true, 8000);
                    this.toastManager.addToast(this.socialInteractionsToast);
                    this.options.joinedFirstServer = true;
                    this.options.save();
                }

                this.tutorial.tick();

                net.neoforged.neoforge.event.EventHooks.fireLevelTickPre(this.level, () -> true);
                try {
                    this.level.tick(() -> true);
                } catch (Throwable throwable1) {
                    CrashReport crashreport1 = CrashReport.forThrowable(throwable1, "Exception in world tick");
                    if (this.level == null) {
                        CrashReportCategory crashreportcategory = crashreport1.addCategory("Affected level");
                        crashreportcategory.setDetail("Problem", "Level is null!");
                    } else {
                        this.level.fillReportDetails(crashreport1);
                    }

                    throw new ReportedException(crashreport1);
                }
                net.neoforged.neoforge.event.EventHooks.fireLevelTickPost(this.level, () -> true);
            }

            profilerfiller.popPush("animateTick");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
            }

            profilerfiller.popPush("particles");
            if (!this.pause && this.isLevelRunningNormally()) {
                this.particleEngine.tick();
            }

            ClientPacketListener clientpacketlistener = this.getConnection();
            if (clientpacketlistener != null && !this.pause) {
                clientpacketlistener.send(ServerboundClientTickEndPacket.INSTANCE);
            }
        } else if (this.pendingConnection != null) {
            profilerfiller.popPush("pendingConnection");
            this.pendingConnection.tick();
        }

        profilerfiller.popPush("keyboard");
        this.keyboardHandler.tick();
        profilerfiller.pop();

        if (this.gameLoadFinished) {
            net.neoforged.neoforge.client.ClientHooks.fireClientTickPost();
        }
    }

    private boolean isLevelRunningNormally() {
        return this.level == null || this.level.tickRateManager().runsNormally();
    }

    private boolean isMultiplayerServer() {
        return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
    }

    private void handleKeybinds() {
        while (this.options.keyTogglePerspective.consumeClick()) {
            CameraType cameratype = this.options.getCameraType();
            this.options.setCameraType(this.options.getCameraType().cycle());
            if (cameratype.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
                this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
            }

            this.levelRenderer.needsUpdate();
        }

        while (this.options.keySmoothCamera.consumeClick()) {
            this.options.smoothCamera = !this.options.smoothCamera;
        }

        for (int i = 0; i < 9; i++) {
            boolean flag = this.options.keySaveHotbarActivator.isDown();
            boolean flag1 = this.options.keyLoadHotbarActivator.isDown();
            if (this.options.keyHotbarSlots[i].consumeClick()) {
                if (this.player.isSpectator()) {
                    this.gui.getSpectatorGui().onHotbarSelected(i);
                } else if (!this.player.hasInfiniteMaterials() || this.screen != null || !flag1 && !flag) {
                    this.player.getInventory().setSelectedSlot(i);
                } else {
                    CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, i, flag1, flag);
                }
            }
        }

        while (this.options.keySocialInteractions.consumeClick()) {
            if (!this.isMultiplayerServer() && !SharedConstants.DEBUG_SOCIAL_INTERACTIONS) {
                this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
                this.narrator.saySystemNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
            } else {
                if (this.socialInteractionsToast != null) {
                    this.socialInteractionsToast.hide();
                    this.socialInteractionsToast = null;
                }

                this.setScreen(new SocialInteractionsScreen());
            }
        }

        while (this.options.keyInventory.consumeClick()) {
            if (this.gameMode.isServerControlledInventory()) {
                this.player.sendOpenInventory();
            } else {
                this.tutorial.onOpenInventory();
                this.setScreen(new InventoryScreen(this.player));
            }
        }

        while (this.options.keyAdvancements.consumeClick()) {
            this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
        }

        while (this.options.keyQuickActions.consumeClick()) {
            this.getQuickActionsDialog().ifPresent(p_428041_ -> this.player.connection.showDialog((Holder<Dialog>)p_428041_, this.screen));
        }

        while (this.options.keySwapOffhand.consumeClick()) {
            if (!this.player.isSpectator()) {
                this.getConnection()
                    .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            }
        }

        while (this.options.keyDrop.consumeClick()) {
            if (!this.player.isSpectator() && this.player.drop(this.hasControlDown())) {
                this.player.swing(InteractionHand.MAIN_HAND);
            }
        }

        while (this.options.keyChat.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
        }

        if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
            this.openChatScreen(ChatComponent.ChatMethod.COMMAND);
        }

        boolean flag2 = false;
        if (this.player.isUsingItem()) {
            if (!this.options.keyUse.isDown()) {
                this.gameMode.releaseUsingItem(this.player);
            }

            while (this.options.keyAttack.consumeClick()) {
            }

            while (this.options.keyUse.consumeClick()) {
            }

            while (this.options.keyPickItem.consumeClick()) {
            }
        } else {
            while (this.options.keyAttack.consumeClick()) {
                flag2 |= this.startAttack();
            }

            while (this.options.keyUse.consumeClick()) {
                this.startUseItem();
            }

            while (this.options.keyPickItem.consumeClick()) {
                this.pickBlock();
            }

            if (this.player.isSpectator()) {
                while (this.options.keySpectatorHotbar.consumeClick()) {
                    this.gui.getSpectatorGui().onHotbarActionKeyPressed();
                }
            }
        }

        if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
            this.startUseItem();
        }

        this.continueAttack(this.screen == null && !flag2 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
    }

    private Optional<Holder<Dialog>> getQuickActionsDialog() {
        Registry<Dialog> registry = this.player.connection.registryAccess().lookupOrThrow(Registries.DIALOG);
        return registry.get(DialogTags.QUICK_ACTIONS).flatMap(p_428040_ -> {
            if (p_428040_.size() == 0) {
                return Optional.empty();
            } else {
                return p_428040_.size() == 1 ? Optional.of(p_428040_.get(0)) : registry.get(Dialogs.QUICK_ACTIONS);
            }
        });
    }

    public ClientTelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    public double getGpuUtilization() {
        return this.gpuUtilization;
    }

    public ProfileKeyPairManager getProfileKeyPairManager() {
        return this.profileKeyPairManager;
    }

    public WorldOpenFlows createWorldOpenFlows() {
        return new WorldOpenFlows(this, this.levelSource);
    }

    public void doWorldLoad(LevelStorageSource.LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem, boolean newWorld) {
        this.disconnectWithProgressScreen();
        Instant instant = Instant.now();
        LevelLoadTracker levelloadtracker = new LevelLoadTracker(newWorld ? 500L : 0L);
        LevelLoadingScreen levelloadingscreen = new LevelLoadingScreen(levelloadtracker, LevelLoadingScreen.Reason.OTHER);
        this.setScreen(levelloadingscreen);
        int i = Math.max(5, 3) + ChunkLevel.RADIUS_AROUND_FULL_CHUNK + 1;

        try {
            levelStorage.saveDataTag(worldStem.registries().compositeAccess(), worldStem.worldData());
            LevelLoadListener levelloadlistener = LevelLoadListener.compose(levelloadtracker, LoggingLevelLoadListener.forSingleplayer());
            this.singleplayerServer = MinecraftServer.spin(
                p_438710_ -> new IntegratedServer(p_438710_, this, levelStorage, packRepository, worldStem, this.services, levelloadlistener)
            );
            levelloadtracker.setServerChunkStatusView(this.singleplayerServer.createChunkLoadStatusView(i));
            this.isLocalServer = true;
            this.updateReportEnvironment(ReportEnvironment.local());
            this.quickPlayLog.setWorldData(QuickPlayLog.Type.SINGLEPLAYER, levelStorage.getLevelId(), worldStem.worldData().getLevelName());
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Starting integrated server");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Starting integrated server");
            crashreportcategory.setDetail("Level ID", levelStorage.getLevelId());
            crashreportcategory.setDetail("Level Name", () -> worldStem.worldData().getLevelName());
            throw new ReportedException(crashreport);
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("waitForServer");
        long k = TimeUnit.SECONDS.toNanos(1L) / 60L;

        while (!this.singleplayerServer.isReady() || this.overlay != null) {
            long j = Util.getNanos() + k;
            levelloadingscreen.tick();
            if (this.overlay != null) {
                this.overlay.tick();
            }

            this.runTick(false);
            this.runAllTasks();
            this.managedBlock(() -> Util.getNanos() > j);
            this.handleDelayedCrash();
        }

        profilerfiller.pop();
        Duration duration = Duration.between(instant, Instant.now());
        SocketAddress socketaddress = this.singleplayerServer.getConnection().startMemoryChannel();
        Connection connection = Connection.connectToLocalServer(socketaddress);
        connection.initiateServerboundPlayConnection(
            socketaddress.toString(),
            0,
            new ClientHandshakePacketListenerImpl(connection, this, null, null, newWorld, duration, p_231442_ -> {}, levelloadtracker, null)
        );
        connection.send(new ServerboundHelloPacket(this.getUser().getName(), this.getUser().getProfileId()));
        this.pendingConnection = connection;
    }

    public void setLevel(ClientLevel level) {
        if (this.level != null) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(this.level));
        this.level = level;
        this.updateLevelInEngines(level);
    }

    public void disconnectFromWorld(Component reason) {
        boolean flag = this.isLocalServer();
        ServerData serverdata = this.getCurrentServer();
        if (this.level != null) {
            this.level.disconnect(reason);
        }

        if (flag) {
            this.disconnectWithSavingScreen();
        } else {
            this.disconnectWithProgressScreen();
        }

        TitleScreen titlescreen = new TitleScreen();
        if (flag) {
            this.setScreen(titlescreen);
        } else if (serverdata != null && serverdata.isRealm()) {
            this.setScreen(new RealmsMainScreen(titlescreen));
        } else {
            this.setScreen(new JoinMultiplayerScreen(titlescreen));
        }
    }

    public void disconnectWithSavingScreen() {
        this.disconnect(new GenericMessageScreen(SAVING_LEVEL), false);
    }

    public void disconnectWithProgressScreen() {
        this.disconnect(new ProgressScreen(true), false);
    }

    public void disconnect(Screen nextScreen, boolean keepResourcePacks) {
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null) {
            this.dropAllTasks();
            clientpacketlistener.close();
            if (!keepResourcePacks) {
                this.clearDownloadedResourcePacks();
            }
        }

        this.playerSocialManager.stopOnlineMode();
        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        IntegratedServer integratedserver = this.singleplayerServer;
        this.singleplayerServer = null;
        this.gameRenderer.resetData();
        net.neoforged.neoforge.client.ClientHooks.firePlayerLogout(this.gameMode, this.player);
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        var shouldRevertRegistriesToFrozen = this.getConnection() != null && this.getConnection().getConnection() != null; // Neo: Track whether to revert registries after disconnect
        try {
            if (this.level != null) {
                this.gui.onDisconnected();
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.LevelEvent.Unload(this.level));
            }

            if (integratedserver != null) {
                this.setScreen(new GenericMessageScreen(SAVING_LEVEL));
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("waitForServer");

                while (!integratedserver.isShutdown()) {
                    this.runTick(false);
                }

                profilerfiller.pop();
            }

            this.setScreenAndShow(nextScreen);
            this.isLocalServer = false;
            this.level = null;
            this.updateLevelInEngines(null);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }
        if(shouldRevertRegistriesToFrozen) net.neoforged.neoforge.registries.RegistryManager.revertToFrozen(); // Neo: Revert registries to frozen on disconnect
    }

    public void clearDownloadedResourcePacks() {
        this.downloadedPackSource.cleanupAfterDisconnect();
        this.runAllTasks();
    }

    public void clearClientLevel(Screen nextScreen) {
        ClientPacketListener clientpacketlistener = this.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.clearLevel();
        }

        if (this.metricsRecorder.isRecording()) {
            this.debugClientMetricsCancel();
        }

        this.gameRenderer.resetData();
        this.gameMode = null;
        this.narrator.clear();
        this.clientLevelTeardownInProgress = true;

        try {
            this.setScreenAndShow(nextScreen);
            this.gui.onDisconnected();
            this.level = null;
            this.updateLevelInEngines(null);
            this.player = null;
        } finally {
            this.clientLevelTeardownInProgress = false;
        }
    }

    public void setScreenAndShow(Screen screen) {
        try (Zone zone = Profiler.get().zone("forcedTick")) {
            this.setScreen(screen);
            this.runTick(false);
        }
    }

    private void updateLevelInEngines(@Nullable ClientLevel level) {
        this.soundManager.stop();
        this.setCameraEntity(null);
        this.pendingConnection = null;
        this.levelRenderer.setLevel(level);
        this.particleEngine.setLevel(level);
        this.gameRenderer.setLevel(level);
        this.updateTitle();
    }

    private UserProperties userProperties() {
        return this.userPropertiesFuture.join();
    }

    public boolean telemetryOptInExtra() {
        return this.extraTelemetryAvailable() && this.options.telemetryOptInExtra().get();
    }

    public boolean extraTelemetryAvailable() {
        return this.allowsTelemetry() && this.userProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
    }

    public boolean allowsTelemetry() {
        return SharedConstants.IS_RUNNING_IN_IDE && !SharedConstants.DEBUG_FORCE_TELEMETRY ? false : this.userProperties().flag(UserFlag.TELEMETRY_ENABLED);
    }

    public boolean allowsMultiplayer() {
        return this.allowsMultiplayer && this.userProperties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null && !this.isNameBanned();
    }

    public boolean allowsRealms() {
        return this.userProperties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
    }

    @Nullable
    public BanDetails multiplayerBan() {
        return this.userProperties().bannedScopes().get("MULTIPLAYER");
    }

    public boolean isNameBanned() {
        ProfileResult profileresult = this.profileFuture.getNow(null);
        return profileresult != null && profileresult.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
    }

    public boolean isBlocked(UUID playerUUID) {
        return this.getChatStatus().isChatAllowed(false)
            ? this.playerSocialManager.shouldHideMessageFrom(playerUUID)
            : (this.player == null || !playerUUID.equals(this.player.getUUID())) && !playerUUID.equals(Util.NIL_UUID);
    }

    public Minecraft.ChatStatus getChatStatus() {
        if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
        } else if (!this.allowsChat) {
            return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
        } else {
            return !this.userProperties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
        }
    }

    public final boolean isDemo() {
        return this.demo;
    }

    public final boolean canSwitchGameMode() {
        return this.player != null && this.gameMode != null;
    }

    @Nullable
    public ClientPacketListener getConnection() {
        return this.player == null ? null : this.player.connection;
    }

    public static boolean renderNames() {
        return !instance.options.hideGui;
    }

    public static boolean useFancyGraphics() {
        return instance.options.graphicsMode().get().getId() >= GraphicsStatus.FANCY.getId();
    }

    public static boolean useShaderTransparency() {
        return !instance.gameRenderer.isPanoramicMode() && instance.options.graphicsMode().get().getId() >= GraphicsStatus.FABULOUS.getId();
    }

    public static boolean useAmbientOcclusion() {
        return instance.options.ambientOcclusion().get();
    }

    private void pickBlock() {
        if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
            if (net.neoforged.neoforge.client.ClientHooks.onClickInput(2, this.options.keyPickItem, InteractionHand.MAIN_HAND).isCanceled()) return;
            boolean flag = this.hasControlDown();
            switch (this.hitResult) {
                case BlockHitResult blockhitresult:
                    this.gameMode.handlePickItemFromBlock(blockhitresult.getBlockPos(), flag);
                    break;
                case EntityHitResult entityhitresult:
                    this.gameMode.handlePickItemFromEntity(entityhitresult.getEntity(), flag);
                    break;
                default:
            }
        }
    }

    /**
     * Adds core server Info (GL version, Texture pack, isModded, type), and the worldInfo to the crash report.
     */
    public CrashReport fillReport(CrashReport theCrash) {
        SystemReport systemreport = theCrash.getSystemReport();

        try {
            fillSystemReport(systemreport, this, this.languageManager, this.launchedVersion, this.options);
            this.fillUptime(theCrash.addCategory("Uptime"));
            if (this.level != null) {
                this.level.fillReportDetails(theCrash);
            }

            if (this.singleplayerServer != null) {
                this.singleplayerServer.fillSystemReport(systemreport);
            }

            this.reloadStateTracker.fillCrashReport(theCrash);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to collect details", throwable);
        }

        return theCrash;
    }

    public static void fillReport(
        @Nullable Minecraft minecraft, @Nullable LanguageManager languageManager, String launchVersion, @Nullable Options options, CrashReport report
    ) {
        SystemReport systemreport = report.getSystemReport();
        fillSystemReport(systemreport, minecraft, languageManager, launchVersion, options);
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.3fs", seconds);
    }

    private void fillUptime(CrashReportCategory category) {
        category.setDetail("JVM uptime", () -> formatSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
        category.setDetail("Wall uptime", () -> formatSeconds((System.currentTimeMillis() - this.clientStartTimeMs) / 1000.0));
        category.setDetail("High-res time", () -> formatSeconds(Util.getMillis() / 1000.0));
        category.setDetail("Client ticks", () -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.clientTickCount, this.clientTickCount / 20.0));
    }

    private static SystemReport fillSystemReport(
        SystemReport report, @Nullable Minecraft minecraft, @Nullable LanguageManager languageManager, String launchVersion, @Nullable Options options
    ) {
        report.setDetail("Launched Version", () -> launchVersion);
        String s = getLauncherBrand();
        if (s != null) {
            report.setDetail("Launcher name", s);
        }

        report.setDetail("Backend library", RenderSystem::getBackendDescription);
        report.setDetail("Backend API", RenderSystem::getApiDescription);
        report.setDetail("Window size", () -> minecraft != null ? minecraft.window.getWidth() + "x" + minecraft.window.getHeight() : "<not initialized>");
        report.setDetail("GFLW Platform", Window::getPlatform);
        report.setDetail("Render Extensions", () -> String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
        report.setDetail("GL debug messages", () -> {
            GpuDevice gpudevice = RenderSystem.tryGetDevice();
            if (gpudevice == null) {
                return "<no renderer available>";
            } else {
                return gpudevice.isDebuggingEnabled() ? String.join("\n", gpudevice.getLastDebugMessages()) : "<debugging unavailable>";
            }
        });
        report.setDetail("Is Modded", () -> checkModStatus().fullDescription());
        report.setDetail("Universe", () -> minecraft != null ? Long.toHexString(minecraft.canary) : "404");
        report.setDetail("Type", "Client (map_client.txt)");
        if (options != null) {
            if (minecraft != null) {
                String s1 = minecraft.getGpuWarnlistManager().getAllWarnings();
                if (s1 != null) {
                    report.setDetail("GPU Warnings", s1);
                }
            }

            report.setDetail("Graphics mode", options.graphicsMode().get().toString());
            report.setDetail("Render Distance", options.getEffectiveRenderDistance() + "/" + options.renderDistance().get() + " chunks");
        }

        if (minecraft != null) {
            report.setDetail("Resource Packs", () -> PackRepository.displayPackList(minecraft.getResourcePackRepository().getSelectedPacks()));
        }

        if (languageManager != null) {
            report.setDetail("Current Language", () -> languageManager.getSelected());
        }

        report.setDetail("Locale", String.valueOf(Locale.getDefault()));
        report.setDetail("System encoding", () -> System.getProperty("sun.jnu.encoding", "<not set>"));
        report.setDetail("File encoding", () -> System.getProperty("file.encoding", "<not set>"));
        report.setDetail("CPU", GLX::_getCpuInfo);
        return report;
    }

    public static Minecraft getInstance() {
        return instance;
    }

    public CompletableFuture<Void> delayTextureReload() {
        return this.<CompletableFuture<Void>>submit((Supplier<CompletableFuture<Void>>)this::reloadResourcePacks).thenCompose(p_231391_ -> (CompletionStage<Void>)p_231391_);
    }

    public void updateReportEnvironment(ReportEnvironment reportEnvironment) {
        if (!this.reportingContext.matches(reportEnvironment)) {
            this.reportingContext = ReportingContext.create(reportEnvironment, this.userApiService);
        }
    }

    @Nullable
    public ServerData getCurrentServer() {
        return Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
    }

    public boolean isLocalServer() {
        return this.isLocalServer;
    }

    public boolean hasSingleplayerServer() {
        return this.isLocalServer && this.singleplayerServer != null;
    }

    @Nullable
    public IntegratedServer getSingleplayerServer() {
        return this.singleplayerServer;
    }

    public boolean isSingleplayer() {
        IntegratedServer integratedserver = this.getSingleplayerServer();
        return integratedserver != null && !integratedserver.isPublished();
    }

    public boolean isLocalPlayer(UUID uuid) {
        return uuid.equals(this.getUser().getProfileId());
    }

    public User getUser() {
        return this.user;
    }

    public GameProfile getGameProfile() {
        ProfileResult profileresult = this.profileFuture.join();
        return profileresult != null ? profileresult.profile() : new GameProfile(this.user.getProfileId(), this.user.getName());
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public TextureManager getTextureManager() {
        return this.textureManager;
    }

    public ShaderManager getShaderManager() {
        return this.shaderManager;
    }

    public ResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public PackRepository getResourcePackRepository() {
        return this.resourcePackRepository;
    }

    public VanillaPackResources getVanillaPackResources() {
        return this.vanillaPackResources;
    }

    public DownloadedPackSource getDownloadedPackSource() {
        return this.downloadedPackSource;
    }

    public Path getResourcePackDirectory() {
        return this.resourcePackDirectory;
    }

    public LanguageManager getLanguageManager() {
        return this.languageManager;
    }

    public boolean isPaused() {
        return this.pause;
    }

    public GpuWarnlistManager getGpuWarnlistManager() {
        return this.gpuWarnlistManager;
    }

    public SoundManager getSoundManager() {
        return this.soundManager;
    }

    public MusicInfo getSituationalMusic() {
        Music music = Optionull.map(this.screen, Screen::getBackgroundMusic);
        if (music != null) {
            return new MusicInfo(music);
        } else if (this.player == null) {
            return new MusicInfo(Musics.MENU);
        } else {
            Level level = this.player.level();
            if (level.dimension() == Level.END) {
                return this.gui.getBossOverlay().shouldPlayMusic() ? new MusicInfo(Musics.END_BOSS) : new MusicInfo(Musics.END);
            } else {
                Holder<Biome> holder = level.getBiome(this.player.blockPosition());
                Biome biome = holder.value();
                float f = biome.getBackgroundMusicVolume();
                Optional<WeightedList<Music>> optional = biome.getBackgroundMusic();
                if (optional.isPresent()) {
                    Optional<Music> optional1 = optional.get().getRandom(level.random);
                    return new MusicInfo(optional1.orElse(null), f);
                } else if (!this.musicManager.isPlayingMusic(Musics.UNDER_WATER)
                    && (!this.player.isUnderWater() || !holder.is(BiomeTags.PLAYS_UNDERWATER_MUSIC))) {
                    return level.dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly
                        ? new MusicInfo(Musics.CREATIVE, f)
                        : new MusicInfo(Musics.GAME, f);
                } else {
                    return new MusicInfo(Musics.UNDER_WATER, f);
                }
            }
        }
    }

    public Services services() {
        return this.services;
    }

    public SkinManager getSkinManager() {
        return this.skinManager;
    }

    @Nullable
    public Entity getCameraEntity() {
        return this.cameraEntity;
    }

    public void setCameraEntity(@Nullable Entity viewingEntity) {
        this.cameraEntity = viewingEntity;
        this.gameRenderer.checkEntityPostEffect(viewingEntity);
    }

    public boolean shouldEntityAppearGlowing(Entity entity) {
        return entity.isCurrentlyGlowing()
            || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && entity.getType() == EntityType.PLAYER;
    }

    @Override
    protected Thread getRunningThread() {
        return this.gameThread;
    }

    @Override
    public Runnable wrapRunnable(Runnable runnable) {
        return runnable;
    }

    @Override
    protected boolean shouldRun(Runnable runnable) {
        return true;
    }

    public BlockRenderDispatcher getBlockRenderer() {
        return this.blockRenderer;
    }

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return this.entityRenderDispatcher;
    }

    public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
        return this.blockEntityRenderDispatcher;
    }

    public ItemRenderer getItemRenderer() {
        return this.itemRenderer;
    }

    public MapRenderer getMapRenderer() {
        return this.mapRenderer;
    }

    public DataFixer getFixerUpper() {
        return this.fixerUpper;
    }

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public BlockColors getBlockColors() {
        return this.blockColors;
    }

    public boolean showOnlyReducedInfo() {
        return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo().get();
    }

    public ToastManager getToastManager() {
        return this.toastManager;
    }

    public Tutorial getTutorial() {
        return this.tutorial;
    }

    public boolean isWindowActive() {
        return this.windowActive;
    }

    public HotbarManager getHotbarManager() {
        return this.hotbarManager;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public AtlasManager getAtlasManager() {
        return this.atlasManager;
    }

    public MapTextureManager getMapTextureManager() {
        return this.mapTextureManager;
    }

    public WaypointStyleManager getWaypointStyles() {
        return this.waypointStyles;
    }

    @Override
    public void setWindowActive(boolean focused) {
        this.windowActive = focused;
    }

    public Component grabPanoramixScreenshot(File gameDirectory) {
        int i = 4;
        int j = 4096;
        int k = 4096;
        int l = this.window.getWidth();
        int i1 = this.window.getHeight();
        RenderTarget rendertarget = this.getMainRenderTarget();
        float f = this.player.getXRot();
        float f1 = this.player.getYRot();
        float f2 = this.player.xRotO;
        float f3 = this.player.yRotO;
        this.gameRenderer.setRenderBlockOutline(false);

        MutableComponent mutablecomponent;
        try {
            this.gameRenderer.setPanoramicMode(true);
            this.window.setWidth(4096);
            this.window.setHeight(4096);
            rendertarget.resize(4096, 4096);

            for (int j1 = 0; j1 < 6; j1++) {
                switch (j1) {
                    case 0:
                        this.player.setYRot(f1);
                        this.player.setXRot(0.0F);
                        break;
                    case 1:
                        this.player.setYRot((f1 + 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 2:
                        this.player.setYRot((f1 + 180.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 3:
                        this.player.setYRot((f1 - 90.0F) % 360.0F);
                        this.player.setXRot(0.0F);
                        break;
                    case 4:
                        this.player.setYRot(f1);
                        this.player.setXRot(-90.0F);
                        break;
                    case 5:
                    default:
                        this.player.setYRot(f1);
                        this.player.setXRot(90.0F);
                }

                this.player.yRotO = this.player.getYRot();
                this.player.xRotO = this.player.getXRot();
                this.gameRenderer.renderLevel(DeltaTracker.ONE);

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException interruptedexception) {
                }

                Screenshot.grab(gameDirectory, "panorama_" + j1 + ".png", rendertarget, 4, p_231415_ -> {});
            }

            Component component = Component.literal(gameDirectory.getName())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(p_392492_ -> p_392492_.withClickEvent(new ClickEvent.OpenFile(gameDirectory.getAbsoluteFile())));
            return Component.translatable("screenshot.success", component);
        } catch (Exception exception) {
            LOGGER.error("Couldn't save image", (Throwable)exception);
            mutablecomponent = Component.translatable("screenshot.failure", exception.getMessage());
        } finally {
            this.player.setXRot(f);
            this.player.setYRot(f1);
            this.player.xRotO = f2;
            this.player.yRotO = f3;
            this.gameRenderer.setRenderBlockOutline(true);
            this.window.setWidth(l);
            this.window.setHeight(i1);
            rendertarget.resize(l, i1);
            this.gameRenderer.setPanoramicMode(false);
        }

        return mutablecomponent;
    }

    public SplashManager getSplashManager() {
        return this.splashManager;
    }

    @Nullable
    public Overlay getOverlay() {
        return this.overlay;
    }

    public PlayerSocialManager getPlayerSocialManager() {
        return this.playerSocialManager;
    }

    public Window getWindow() {
        return this.window;
    }

    public FramerateLimitTracker getFramerateLimitTracker() {
        return this.framerateLimitTracker;
    }

    public DebugScreenOverlay getDebugOverlay() {
        return this.gui.getDebugOverlay();
    }

    public RenderBuffers renderBuffers() {
        return this.renderBuffers;
    }

    public void updateMaxMipLevel(int mipMapLevel) {
        this.atlasManager.updateMaxMipLevel(mipMapLevel);
    }

    public EntityModelSet getEntityModels() {
        return this.modelManager.entityModels().get();
    }

    public boolean isTextFilteringEnabled() {
        return this.userProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
    }

    public void prepareForMultiplayer() {
        this.playerSocialManager.startOnlineMode();
        this.getProfileKeyPairManager().prepareKeyPair();
    }

    public InputType getLastInputType() {
        return this.lastInputType;
    }

    public void setLastInputType(InputType lastInputType) {
        this.lastInputType = lastInputType;
    }

    public GameNarrator getNarrator() {
        return this.narrator;
    }

    public ChatListener getChatListener() {
        return this.chatListener;
    }

    public ReportingContext getReportingContext() {
        return this.reportingContext;
    }

    public RealmsDataFetcher realmsDataFetcher() {
        return this.realmsDataFetcher;
    }

    public QuickPlayLog quickPlayLog() {
        return this.quickPlayLog;
    }

    public CommandHistory commandHistory() {
        return this.commandHistory;
    }

    public DirectoryValidator directoryValidator() {
        return this.directoryValidator;
    }

    public PlayerSkinRenderCache playerSkinRenderCache() {
        return this.playerSkinRenderCache;
    }

    private float getTickTargetMillis(float defaultValue) {
        if (this.level != null) {
            TickRateManager tickratemanager = this.level.tickRateManager();
            if (tickratemanager.runsNormally()) {
                return Math.max(defaultValue, tickratemanager.millisecondsPerTick());
            }
        }

        return defaultValue;
    }

    public ItemModelResolver getItemModelResolver() {
        return this.itemModelResolver;
    }

    public boolean canInterruptScreen() {
        return (this.screen == null || this.screen.canInterruptWithAnotherScreen()) && !this.clientLevelTeardownInProgress;
    }

    @Nullable
    public static String getLauncherBrand() {
        return System.getProperty("minecraft.launcher.brand");
    }

    public PacketProcessor packetProcessor() {
        return this.packetProcessor;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ChatStatus {
        ENABLED(CommonComponents.EMPTY) {
            @Override
            public boolean isChatAllowed(boolean p_168045_) {
                return true;
            }
        },
        DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
            @Override
            public boolean isChatAllowed(boolean p_168051_) {
                return false;
            }
        },
        DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
            @Override
            public boolean isChatAllowed(boolean p_168057_) {
                return p_168057_;
            }
        },
        DISABLED_BY_PROFILE(
            Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)
        ) {
            @Override
            public boolean isChatAllowed(boolean p_168063_) {
                return p_168063_;
            }
        };

        static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
        private final Component message;

        ChatStatus(Component message) {
            this.message = message;
        }

        public Component getMessage() {
            return this.message;
        }

        public abstract boolean isChatAllowed(boolean isLocalServer);
    }

    @OnlyIn(Dist.CLIENT)
    record GameLoadCookie(RealmsClient realmsClient, GameConfig.QuickPlayData quickPlayData) {
    }
}
