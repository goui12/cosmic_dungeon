package net.minecraft.client.main;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.blaze3d.TracyBootstrap;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.util.UndashedUuid;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Optionull;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Main {
    @DontObfuscate
    public static void main(String[] args) {
        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();
        optionparser.accepts("demo");
        optionparser.accepts("disableMultiplayer");
        optionparser.accepts("disableChat");
        optionparser.accepts("fullscreen");
        optionparser.accepts("checkGlErrors");
        OptionSpec<Void> optionspec = optionparser.accepts("renderDebugLabels");
        OptionSpec<Void> optionspec1 = optionparser.accepts("jfrProfile");
        OptionSpec<Void> optionspec2 = optionparser.accepts("tracy");
        OptionSpec<Void> optionspec3 = optionparser.accepts("tracyNoImages");
        OptionSpec<String> optionspec4 = optionparser.accepts("quickPlayPath").withRequiredArg();
        OptionSpec<String> optionspec5 = optionparser.accepts("quickPlaySingleplayer").withOptionalArg();
        OptionSpec<String> optionspec6 = optionparser.accepts("quickPlayMultiplayer").withRequiredArg();
        OptionSpec<String> optionspec7 = optionparser.accepts("quickPlayRealms").withRequiredArg();
        OptionSpec<File> optionspec8 = optionparser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<File> optionspec9 = optionparser.accepts("assetsDir").withRequiredArg().ofType(File.class);
        OptionSpec<File> optionspec10 = optionparser.accepts("resourcePackDir").withRequiredArg().ofType(File.class);
        OptionSpec<String> optionspec11 = optionparser.accepts("proxyHost").withRequiredArg();
        OptionSpec<Integer> optionspec12 = optionparser.accepts("proxyPort").withRequiredArg().defaultsTo("8080").ofType(Integer.class);
        OptionSpec<String> optionspec13 = optionparser.accepts("proxyUser").withRequiredArg();
        OptionSpec<String> optionspec14 = optionparser.accepts("proxyPass").withRequiredArg();
        OptionSpec<String> optionspec15 = optionparser.accepts("username").withRequiredArg().defaultsTo(net.neoforged.fml.loading.FMLEnvironment.isProduction() ? "Player" + System.currentTimeMillis() % 1000L : "Dev").withValuesConvertedBy(net.neoforged.neoforge.client.ClientHooks.convertUsername());
        OptionSpec<Void> optionspec16 = optionparser.accepts("offlineDeveloperMode");
        OptionSpec<String> optionspec17 = optionparser.accepts("uuid").withRequiredArg();
        OptionSpec<String> optionspec18 = optionparser.accepts("xuid").withOptionalArg().defaultsTo("");
        OptionSpec<String> optionspec19 = optionparser.accepts("clientId").withOptionalArg().defaultsTo("");
        // Neo: Make accessToken optional in dev
        OptionSpec<String> optionspec20 = net.neoforged.neoforge.client.ClientHooks.optionalInDev(optionparser.accepts("accessToken").withRequiredArg(), "0");
        OptionSpec<String> optionspec21 = optionparser.accepts("version").withRequiredArg().required();
        OptionSpec<Integer> optionspec22 = optionparser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(854);
        OptionSpec<Integer> optionspec23 = optionparser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(480);
        OptionSpec<Integer> optionspec24 = optionparser.accepts("fullscreenWidth").withRequiredArg().ofType(Integer.class);
        OptionSpec<Integer> optionspec25 = optionparser.accepts("fullscreenHeight").withRequiredArg().ofType(Integer.class);
        OptionSpec<String> optionspec26 = optionparser.accepts("assetIndex").withRequiredArg();
        OptionSpec<String> optionspec27 = optionparser.accepts("versionType").withRequiredArg().defaultsTo("release");
        OptionSpec<String> optionspec28 = optionparser.nonOptions();
        OptionSet optionset = optionparser.parse(args);
        File file1 = parseArgument(optionset, optionspec8);
        String s = parseArgument(optionset, optionspec21);
        String s1 = "Pre-bootstrap";

        Logger logger;
        GameConfig gameconfig;
        try {
            if (optionset.has(optionspec1)) {
                JvmProfiler.INSTANCE.start(Environment.CLIENT);
            }

            if (optionset.has(optionspec2)) {
                TracyBootstrap.setup();
            }

            Stopwatch stopwatch = Stopwatch.createStarted(Ticker.systemTicker());
            Stopwatch stopwatch1 = Stopwatch.createStarted(Ticker.systemTicker());
            GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS, stopwatch);
            GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS, stopwatch1);
            SharedConstants.tryDetectVersion();
            TracyClient.reportAppInfo("Minecraft Java Edition " + SharedConstants.getCurrentVersion().name());
            CompletableFuture<?> completablefuture = DataFixers.optimize(DataFixTypes.TYPES_FOR_LEVEL_LIST);
            CrashReport.preload();
            logger = LogUtils.getLogger();
            s1 = "Bootstrap";
            net.neoforged.fml.loading.BackgroundWaiter.runAndTick(() -> Bootstrap.bootStrap(), net.neoforged.fml.loading.ImmediateWindowHandler::renderTick);
            GameLoadTimesEvent.INSTANCE.setBootstrapTime(Bootstrap.bootstrapDuration.get());
            Bootstrap.validate();

            net.neoforged.neoforge.client.loading.ClientModLoader.begin(); // Mirroring server mod-loading, we construct immediately after Bootstrap.validate()

            s1 = "Argument parsing";
            List<String> list = optionset.valuesOf(optionspec28);
            if (!list.isEmpty()) {
                logger.info("Completely ignored arguments: {}", list);
            }

            String s2 = parseArgument(optionset, optionspec11);
            Proxy proxy = Proxy.NO_PROXY;
            if (s2 != null) {
                try {
                    proxy = new Proxy(Type.SOCKS, new InetSocketAddress(s2, parseArgument(optionset, optionspec12)));
                } catch (Exception exception) {
                }
            }

            final String s3 = parseArgument(optionset, optionspec13);
            final String s4 = parseArgument(optionset, optionspec14);
            if (!proxy.equals(Proxy.NO_PROXY) && stringHasValue(s3) && stringHasValue(s4)) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(s3, s4.toCharArray());
                    }
                });
            }

            int i = parseArgument(optionset, optionspec22);
            int j = parseArgument(optionset, optionspec23);
            OptionalInt optionalint = ofNullable(parseArgument(optionset, optionspec24));
            OptionalInt optionalint1 = ofNullable(parseArgument(optionset, optionspec25));
            boolean flag = optionset.has("fullscreen");
            boolean flag1 = optionset.has("demo");
            boolean flag2 = optionset.has("disableMultiplayer");
            boolean flag3 = optionset.has("disableChat");
            boolean flag4 = !optionset.has(optionspec3);
            boolean flag5 = optionset.has(optionspec);
            String s5 = parseArgument(optionset, optionspec27);
            File file2 = optionset.has(optionspec9) ? parseArgument(optionset, optionspec9) : new File(file1, "assets/");
            File file3 = optionset.has(optionspec10) ? parseArgument(optionset, optionspec10) : new File(file1, "resourcepacks/");
            UUID uuid = hasValidUuid(optionspec17, optionset, logger)
                ? UndashedUuid.fromStringLenient(optionspec17.value(optionset))
                : UUIDUtil.createOfflinePlayerUUID(optionspec15.value(optionset));
            String s6 = optionset.has(optionspec26) ? optionspec26.value(optionset) : null;
            String s7 = optionset.valueOf(optionspec18);
            String s8 = optionset.valueOf(optionspec19);
            String s9 = parseArgument(optionset, optionspec4);
            GameConfig.QuickPlayVariant gameconfig$quickplayvariant = getQuickPlayVariant(optionset, optionspec5, optionspec6, optionspec7);
            User user = new User(
                optionspec15.value(optionset), uuid, optionspec20.value(optionset), emptyStringToEmptyOptional(s7), emptyStringToEmptyOptional(s8)
            );
            gameconfig = new GameConfig(
                new GameConfig.UserData(user, proxy),
                new DisplayData(i, j, optionalint, optionalint1, flag),
                new GameConfig.FolderData(file1, file3, file2, s6),
                // Neo: Auto-enable offlineDeveloperMode if no accessToken is passed in dev
                new GameConfig.GameData(flag1, s, s5, flag2, flag3, flag4, flag5, optionset.has(optionspec16) || !optionset.has(optionspec20)),
                new GameConfig.QuickPlayData(s9, gameconfig$quickplayvariant)
            );
            Util.startTimerHackThread();
            completablefuture.join();
        } catch (Throwable throwable1) {
            CrashReport crashreport = CrashReport.forThrowable(throwable1, s1);
            CrashReportCategory crashreportcategory = crashreport.addCategory("Initialization");
            NativeModuleLister.addCrashSection(crashreportcategory);
            Minecraft.fillReport(null, null, s, null, crashreport);
            Minecraft.crash(null, file1, crashreport);
            return;
        }

        Thread thread = new Thread("Client Shutdown Thread") {
            @Override
            public void run() {
                Minecraft minecraft2 = Minecraft.getInstance();
                if (minecraft2 != null) {
                    IntegratedServer integratedserver = minecraft2.getSingleplayerServer();
                    if (integratedserver != null) {
                        integratedserver.halt(true);
                    }
                }
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(logger));
        Runtime.getRuntime().addShutdownHook(thread);
        Minecraft minecraft = null;

        try {
            Thread.currentThread().setName("Render thread");
            RenderSystem.initRenderThread();
            minecraft = new Minecraft(gameconfig);
        } catch (SilentInitException silentinitexception) {
            Util.shutdownExecutors();
            logger.warn("Failed to create window: ", (Throwable)silentinitexception);
            return;
        } catch (Throwable throwable) {
            CrashReport crashreport1 = CrashReport.forThrowable(throwable, "Initializing game");
            CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Initialization");
            NativeModuleLister.addCrashSection(crashreportcategory1);
            Minecraft.fillReport(minecraft, null, gameconfig.game.launchVersion, null, crashreport1);
            Minecraft.crash(minecraft, gameconfig.location.gameDirectory, crashreport1);
            return;
        }

        Minecraft minecraft1 = minecraft;
        minecraft.run();

        try {
            minecraft1.stop();
        } finally {
            minecraft.destroy();
        }
    }

    private static GameConfig.QuickPlayVariant getQuickPlayVariant(
        OptionSet options, OptionSpec<String> singleplayer, OptionSpec<String> multiplayer, OptionSpec<String> realms
    ) {
        long i = Stream.of(singleplayer, multiplayer, realms).filter(options::has).count();
        if (i == 0L) {
            return GameConfig.QuickPlayVariant.DISABLED;
        } else if (i > 1L) {
            throw new IllegalArgumentException("Only one quick play option can be specified");
        } else if (options.has(singleplayer)) {
            String s2 = unescapeJavaArgument(parseArgument(options, singleplayer));
            return new GameConfig.QuickPlaySinglePlayerData(s2);
        } else if (options.has(multiplayer)) {
            String s1 = unescapeJavaArgument(parseArgument(options, multiplayer));
            return Optionull.mapOrDefault(s1, GameConfig.QuickPlayMultiplayerData::new, GameConfig.QuickPlayVariant.DISABLED);
        } else if (options.has(realms)) {
            String s = unescapeJavaArgument(parseArgument(options, realms));
            return Optionull.mapOrDefault(s, GameConfig.QuickPlayRealmsData::new, GameConfig.QuickPlayVariant.DISABLED);
        } else {
            return GameConfig.QuickPlayVariant.DISABLED;
        }
    }

    @Nullable
    private static String unescapeJavaArgument(@Nullable String arg) {
        return arg == null ? null : StringEscapeUtils.unescapeJava(arg);
    }

    private static Optional<String> emptyStringToEmptyOptional(String input) {
        return input.isEmpty() ? Optional.empty() : Optional.of(input);
    }

    private static OptionalInt ofNullable(@Nullable Integer value) {
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    /**
     * Gets the value of a specified command-line parameter from an OptionSet. If it doesn't exist, it returns the default value for the parameter.
     */
    @Nullable
    private static <T> T parseArgument(OptionSet set, OptionSpec<T> option) {
        try {
            return set.valueOf(option);
        } catch (Throwable throwable) {
            if (option instanceof ArgumentAcceptingOptionSpec<T> argumentacceptingoptionspec) {
                List<T> list = argumentacceptingoptionspec.defaultValues();
                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }

            throw throwable;
        }
    }

    /**
     * Returns {@code true} if the given string is neither null nor empty.
     */
    private static boolean stringHasValue(@Nullable String str) {
        return str != null && !str.isEmpty();
    }

    private static boolean hasValidUuid(OptionSpec<String> uuidOption, OptionSet options, Logger logger) {
        return options.has(uuidOption) && isUuidValid(uuidOption, options, logger);
    }

    private static boolean isUuidValid(OptionSpec<String> uuidOption, OptionSet optionSet, Logger logger) {
        try {
            UndashedUuid.fromStringLenient(uuidOption.value(optionSet));
            return true;
        } catch (IllegalArgumentException illegalargumentexception) {
            logger.warn("Invalid UUID: '{}", uuidOption.value(optionSet));
            return false;
        }
    }

    static {
        System.setProperty("java.awt.headless", "true");
    }
}
