package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;

public class GameTestServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROGRESS_REPORT_INTERVAL = 20;
    private static final int TEST_POSITION_RANGE = 14999992;
    private static final Services NO_SERVICES = new Services(
        null, ServicesKeySet.EMPTY, null, new GameTestServer.MockUserNameToIdResolver(), new GameTestServer.MockProfileResolver()
    );
    private static final FeatureFlagSet ENABLED_FEATURES = FeatureFlags.REGISTRY
        .allFlags()
        .subtract(FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS));
    private final LocalSampleLogger sampleLogger = new LocalSampleLogger(4);
    private final Optional<String> testSelection;
    private final boolean verify;
    private List<GameTestBatch> testBatches = new ArrayList<>();
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);
    @Nullable
    private MultipleTestTracker testTracker;
    // Neo: disable mob spawning and weather cycling when running game tests, same as for the ephemeral test server
    private static final GameRules TEST_GAME_RULES = Util.make(new GameRules(ENABLED_FEATURES), rules -> {
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
    });

    public static GameTestServer create(
        Thread serverThread, LevelStorageSource.LevelStorageAccess storageSource, PackRepository packRepository, Optional<String> testSelection, boolean verify
    ) {
        packRepository.reload();
        ArrayList<String> arraylist = new ArrayList<>(packRepository.getAvailableIds());
        arraylist.remove("vanilla");
        arraylist.addFirst("vanilla");
        WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(new DataPackConfig(arraylist, List.of()), ENABLED_FEATURES);
        LevelSettings levelsettings = new LevelSettings(
            "Test Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, TEST_GAME_RULES, worlddataconfiguration
        );
        WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(packRepository, worlddataconfiguration, false, true);
        WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(worldloader$packconfig, Commands.CommandSelection.DEDICATED, 4);

        try {
            LOGGER.debug("Starting resource loading");
            Stopwatch stopwatch = Stopwatch.createStarted();
            WorldStem worldstem = Util.<WorldStem>blockUntilDone(
                    p_372641_ -> WorldLoader.load(
                        worldloader$initconfig,
                        p_359463_ -> {
                            Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                            WorldDimensions.Complete worlddimensions$complete = p_359463_.datapackWorldgen()
                                .lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions()
                                .bake(registry);
                            return new WorldLoader.DataLoadOutput<>(
                                new PrimaryLevelData(
                                    levelsettings, WORLD_OPTIONS, worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()
                                ),
                                worlddimensions$complete.dimensionsRegistryAccess()
                            );
                        },
                        WorldStem::new,
                        Util.backgroundExecutor(),
                        p_372641_
                    )
                )
                .get();
            stopwatch.stop();
            LOGGER.debug("Finished resource loading after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return new GameTestServer(serverThread, storageSource, packRepository, worldstem, testSelection, verify);
        } catch (Exception exception) {
            LOGGER.warn("Failed to load vanilla datapack, bit oops", (Throwable)exception);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    private GameTestServer(
        Thread serverThread,
        LevelStorageSource.LevelStorageAccess storageSource,
        PackRepository packRepository,
        WorldStem worldStem,
        Optional<String> testSelection,
        boolean verify
    ) {
        super(serverThread, storageSource, packRepository, worldStem, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, LoggingLevelLoadListener.forDedicatedServer());
        this.testSelection = testSelection;
        this.verify = verify;
    }

    @Override
    public boolean initServer() {
        this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, new EmptyNotificationService()) {});
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerAboutToStart(this);
        this.loadLevel();
        ServerLevel serverlevel = this.overworld();
        this.testBatches = this.evaluateTestsToRun(serverlevel);
        LOGGER.info("Started game test server");
        net.neoforged.neoforge.server.ServerLifecycleHooks.handleServerStarting(this);
        return true;
    }

    private List<GameTestBatch> evaluateTestsToRun(ServerLevel level) {
        Registry<GameTestInstance> registry = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
        Collection<Holder.Reference<GameTestInstance>> collection;
        GameTestBatchFactory.TestDecorator gametestbatchfactory$testdecorator;
        if (this.testSelection.isPresent()) {
            collection = getTestsForSelection(level.registryAccess(), this.testSelection.get())
                .filter(p_396401_ -> !p_396401_.value().manualOnly())
                .toList();
            if (this.verify) {
                gametestbatchfactory$testdecorator = GameTestServer::rotateAndMultiply;
                LOGGER.info("Verify requested. Will run each test that matches {} {} times", this.testSelection.get(), 100 * Rotation.values().length);
            } else {
                gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
                LOGGER.info("Will run tests matching {} ({} tests)", this.testSelection.get(), collection.size());
            }
        } else {
            collection = registry.listElements().filter(p_396402_ -> !p_396402_.value().manualOnly()).toList();
            gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
        }

        return GameTestBatchFactory.divideIntoBatches(collection, gametestbatchfactory$testdecorator, level);
    }

    private static Stream<GameTestInfo> rotateAndMultiply(Holder.Reference<GameTestInstance> test, ServerLevel level) {
        Builder<GameTestInfo> builder = Stream.builder();

        for (Rotation rotation : Rotation.values()) {
            for (int i = 0; i < 100; i++) {
                builder.add(new GameTestInfo(test, rotation, level, RetryOptions.noRetries()));
            }
        }

        return builder.build();
    }

    public static Stream<Holder.Reference<GameTestInstance>> getTestsForSelection(RegistryAccess registries, String selection) {
        return ResourceSelectorArgument.parse(new StringReader(selection), registries.lookupOrThrow(Registries.TEST_INSTANCE)).stream();
    }

    /**
     * Main function called by run() every loop.
     */
    @Override
    public void tickServer(BooleanSupplier hasTimeLeft) {
        super.tickServer(hasTimeLeft);
        ServerLevel serverlevel = this.overworld();
        if (!this.haveTestsStarted()) {
            this.startTests(serverlevel);
        }

        if (serverlevel.getGameTime() % 20L == 0L) {
            LOGGER.info(this.testTracker.getProgressBar());
        }

        if (this.testTracker.isDone()) {
            this.halt(false);
            LOGGER.info(this.testTracker.getProgressBar());
            GlobalTestReporter.finish();
            LOGGER.info("========= {} GAME TESTS COMPLETE IN {} ======================", this.testTracker.getTotalCount(), this.stopwatch.stop());
            if (this.testTracker.hasFailedRequired()) {
                LOGGER.error("{} required tests failed :(", this.testTracker.getFailedRequiredCount());
                this.testTracker.getFailedRequired().forEach(GameTestServer::logFailedTest);

                // Neo: when running in GitHub actions emit actions-specific error annotations to make finding the error message easier
                // See https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#example-creating-an-annotation-for-an-error
                if (System.getenv().getOrDefault("CI", "false").equals("true") && System.getenv().getOrDefault("GITHUB_ACTIONS", "false").equals("true")) {
                    System.out.printf("\n::error title=GameTest Failure::%s required game tests failed: %s\n\n", testTracker.getFailedRequiredCount(), testTracker.getFailedRequired().stream().map(info -> info.id().toString()).collect(java.util.stream.Collectors.joining(", ")));
                }
            } else {
                LOGGER.info("All {} required tests passed :)", this.testTracker.getTotalCount());
            }

            if (this.testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", this.testTracker.getFailedOptionalCount());
                this.testTracker.getFailedOptional().forEach(GameTestServer::logFailedTest);
            }

            LOGGER.info("====================================================");
        }
    }

    private static void logFailedTest(GameTestInfo info) {
        if (info.getRotation() != Rotation.NONE) {
            LOGGER.error(
                "   - {} with rotation {}: {}", info.id(), info.getRotation().getSerializedName(), info.getError().getDescription().getString()
            );
        } else {
            LOGGER.error("   - {}: {}", info.id(), info.getError().getDescription().getString());
        }
    }

    @Override
    public SampleLogger getTickTimeLogger() {
        return this.sampleLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    public void waitUntilNextTick() {
        this.runAllTasks();
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport report) {
        report.setDetail("Type", "Game test server");
        return report;
    }

    @Override
    public void onServerExit() {
        super.onServerExit();
        LOGGER.info("Game test server shutting down");
        System.exit(this.testTracker != null ? this.testTracker.getFailedRequiredCount() : -1);
    }

    /**
     * Called on exit from the main run() loop.
     */
    @Override
    public void onServerCrash(CrashReport report) {
        super.onServerCrash(report);
        LOGGER.error("Game test server crashed\n{}", report.getFriendlyReport(ReportType.CRASH));
        System.exit(1);
    }

    private void startTests(ServerLevel serverLevel) {
        BlockPos blockpos = new BlockPos(
            serverLevel.random.nextIntBetweenInclusive(-14999992, 14999992), -59, serverLevel.random.nextIntBetweenInclusive(-14999992, 14999992)
        );
        serverLevel.setRespawnData(LevelData.RespawnData.of(serverLevel.dimension(), blockpos, 0.0F, 0.0F));
        GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(this.testBatches, serverLevel)
            .newStructureSpawner(new StructureGridSpawner(blockpos, 8, false))
            .build();
        Collection<GameTestInfo> collection = gametestrunner.getTestInfos();
        this.testTracker = new MultipleTestTracker(collection);
        LOGGER.info("{} tests are now running at position {}!", this.testTracker.getTotalCount(), blockpos.toShortString());
        this.stopwatch.reset();
        this.stopwatch.start();
        gametestrunner.start();
    }

    private boolean haveTestsStarted() {
        return this.testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int operatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 4;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public boolean isSpawnerBlockEnabled() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(NameAndId player) {
        return false;
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    static class MockProfileResolver implements ProfileResolver {
        @Override
        public Optional<GameProfile> fetchByName(String p_440569_) {
            return Optional.empty();
        }

        @Override
        public Optional<GameProfile> fetchById(UUID p_439458_) {
            return Optional.empty();
        }
    }

    static class MockUserNameToIdResolver implements UserNameToIdResolver {
        private final Set<NameAndId> savedIds = new HashSet<>();

        @Override
        public void add(NameAndId p_434475_) {
            this.savedIds.add(p_434475_);
        }

        @Override
        public Optional<NameAndId> get(String p_433921_) {
            return this.savedIds
                .stream()
                .filter(p_433633_ -> p_433633_.name().equals(p_433921_))
                .findFirst()
                .or(() -> Optional.of(NameAndId.createOffline(p_433921_)));
        }

        @Override
        public Optional<NameAndId> get(UUID p_433513_) {
            return this.savedIds.stream().filter(p_435750_ -> p_435750_.id().equals(p_433513_)).findFirst();
        }

        @Override
        public void resolveOfflineUsers(boolean p_435278_) {
        }

        @Override
        public void save() {
        }
    }
}
