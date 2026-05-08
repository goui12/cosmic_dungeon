package net.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.commands.BrigadierExceptions;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared global constants.
 *
 * <p><strong>Note:</strong> The majority of the fields within this class are {@code public static final} with constant expressions (constants), and are inlined by the Java compiler at all places which reference these constant fields. Therefore, changing the value of these constant fields will have no effect on already compiled code.</p>
 *
 * <p>In addition, it is presumed that a large portion of these constant fields (such as those prefixed with {@code DEBUG_} are used as 'flags', for manually toggling code meant for use by Mojang developers in debugging. Therefore, optimizing compilers (which include the Java compiler) may omit the code hidden behind disabled flags, and will result in these flags having no apparent use in the code (when in reality, the optimizing compiler has removed the code which uses them).</p>
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-15.html#jls-15.29">The Java&reg; Language Specification, Java SE 16 Edition, &sect; 15.29. "Constant Expressions"</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-14.html#jls-14.22">The Java&reg; Language Specification, Java SE 16 Edition, &sect; 14.22. "Unreachable Statements"</a>
 */
@SuppressForbidden(
    reason = "System.out needed before bootstrap"
)
public class SharedConstants {
    /**
     * Indicates whether the currently running game version is a snapshot version.
     *
     * @see com.mojang.bridge.game.GameVersion#isStable()
     * @deprecated Use {@link #getCurrentVersion()} and {@link com.mojang.bridge.game.GameVersion#isStable()} instead.
     */
    @Deprecated
    public static final boolean SNAPSHOT = false;
    /**
     * The numeric format number for worlds used by this game version.
     *
     * @see com.mojang.bridge.game.GameVersion#getWorldVersion()
     * @deprecated Use {@link #getCurrentVersion()} and {@link com.mojang.bridge.game.GameVersion#getWorldVersion()} instead.
     */
    @Deprecated
    public static final int WORLD_VERSION = 4556;
    @Deprecated
    public static final String SERIES = "main";
    /**
     * The numeric format number for the networking protocol used by the release target of this game version.
     *
     * <p>This protocol version is used when this game version is a release version, not a {@linkplain #SNAPSHOT snapshots}. For snapshots, see {@link #SNAPSHOT_NETWORK_PROTOCOL_VERSION}.</p>
     *
     * @see #getProtocolVersion()
     * @deprecated Use {@link #getProtocolVersion()} instead.
     */
    @Deprecated
    public static final int RELEASE_NETWORK_PROTOCOL_VERSION = 773;
    /**
     * The numeric format number for the networking protocol used by the snapshot of this game version.
     *
     * <p>This protocol version is used when this game version is a {@linkplain #SNAPSHOT snapshot version}. For releases, see {@link #RELEASE_NETWORK_PROTOCOL_VERSION}.</p>
     *
     * <p>The actual networking protocol version used in snapshot versions is the combination (bitwise OR) of this number and the bit at the position marked by {@link #SNAPSHOT_PROTOCOL_BIT}.</p>
     *
     * @see #getProtocolVersion()
     * @deprecated Use {@link #getProtocolVersion()} instead.
     */
    @Deprecated
    public static final int SNAPSHOT_NETWORK_PROTOCOL_VERSION = 274;
    public static final int SNBT_NAG_VERSION = 4531;
    /**
     * The bit in the networking protocol version for denoting {@linkplain #SNAPSHOT snapshot versions}.
     *
     * @see #SNAPSHOT_NETWORK_PROTOCOL_VERSION
     */
    private static final int SNAPSHOT_PROTOCOL_BIT = 30;
    public static final boolean CRASH_EAGERLY = false;
    @Deprecated
    public static final int RESOURCE_PACK_FORMAT_MAJOR = 69;
    @Deprecated
    public static final int RESOURCE_PACK_FORMAT_MINOR = 0;
    @Deprecated
    public static final int DATA_PACK_FORMAT_MAJOR = 88;
    @Deprecated
    public static final int DATA_PACK_FORMAT_MINOR = 0;
    @Deprecated
    public static final int LANGUAGE_FORMAT = 1;
    public static final int REPORT_FORMAT_VERSION = 1;
    /**
     * The key for the NBT tag which contains the data version of some data, for use in datafixing.
     *
     * <p>For various objects, their stored data in a {@link net.minecraft.nbt.CompoundTag} will usually contain a {@link net.minecraft.nbt.IntTag} indexed with this key, which stores the data version when the data for that object was written out. This is used by the datafixer system to determine which fixers needs to be applied to the data.</p>
     *
     * @see com.mojang.bridge.game.GameVersion#getWorldVersion()
     */
    public static final String DATA_VERSION_TAG = "DataVersion";
    public static final String RPC_MANAGEMENT_SERVER_API_VERSION = "1.0.0";
    public static final String DEBUG_FLAG_PREFIX = "MC_DEBUG_";
    public static final boolean DEBUG_ENABLED = booleanProperty(prefixDebugFlagName("ENABLED"));
    private static final boolean DEBUG_PRINT_PROPERTIES = booleanProperty(prefixDebugFlagName("PRINT_PROPERTIES"));
    public static final boolean FIX_TNT_DUPE = false;
    public static final boolean FIX_SAND_DUPE = false;
    public static final boolean DEBUG_OPEN_INCOMPATIBLE_WORLDS = debugFlag("OPEN_INCOMPATIBLE_WORLDS");
    public static final boolean DEBUG_ALLOW_LOW_SIM_DISTANCE = debugFlag("ALLOW_LOW_SIM_DISTANCE");
    public static final boolean DEBUG_HOTKEYS = debugFlag("HOTKEYS");
    public static final boolean DEBUG_UI_NARRATION = debugFlag("UI_NARRATION");
    public static final boolean DEBUG_SHUFFLE_UI_RENDERING_ORDER = debugFlag("SHUFFLE_UI_RENDERING_ORDER");
    public static final boolean DEBUG_SHUFFLE_MODELS = debugFlag("SHUFFLE_MODELS");
    public static final boolean DEBUG_RENDER_UI_LAYERING_RECTANGLES = debugFlag("RENDER_UI_LAYERING_RECTANGLES");
    public static final boolean DEBUG_PATHFINDING = debugFlag("PATHFINDING");
    public static final boolean DEBUG_WATER = debugFlag("WATER");
    public static final boolean DEBUG_HEIGHTMAP = debugFlag("HEIGHTMAP");
    public static final boolean DEBUG_COLLISION = debugFlag("COLLISION");
    public static final boolean DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES = debugFlag("SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES");
    public static final boolean DEBUG_SUPPORT_BLOCKS = debugFlag("SUPPORT_BLOCKS");
    public static final boolean DEBUG_SHAPES = debugFlag("SHAPES");
    public static final boolean DEBUG_NEIGHBORSUPDATE = debugFlag("NEIGHBORSUPDATE");
    public static final boolean DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = debugFlag("EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER");
    public static final boolean DEBUG_STRUCTURES = debugFlag("STRUCTURES");
    public static final boolean DEBUG_LIGHT = debugFlag("LIGHT");
    public static final boolean DEBUG_SKY_LIGHT_SECTIONS = debugFlag("SKY_LIGHT_SECTIONS");
    public static final boolean DEBUG_SOLID_FACE = debugFlag("SOLID_FACE");
    public static final boolean DEBUG_CHUNKS = debugFlag("CHUNKS");
    public static final boolean DEBUG_GAME_EVENT_LISTENERS = debugFlag("GAME_EVENT_LISTENERS");
    public static final boolean DEBUG_DUMP_TEXTURE_ATLAS = debugFlag("DUMP_TEXTURE_ATLAS");
    public static final boolean DEBUG_DUMP_INTERPOLATED_TEXTURE_FRAMES = debugFlag("DUMP_INTERPOLATED_TEXTURE_FRAMES");
    public static final boolean DEBUG_STRUCTURE_EDIT_MODE = debugFlag("STRUCTURE_EDIT_MODE");
    public static final boolean DEBUG_SAVE_STRUCTURES_AS_SNBT = debugFlag("SAVE_STRUCTURES_AS_SNBT");
    public static final boolean DEBUG_SYNCHRONOUS_GL_LOGS = debugFlag("SYNCHRONOUS_GL_LOGS");
    public static final boolean DEBUG_VERBOSE_SERVER_EVENTS = debugFlag("VERBOSE_SERVER_EVENTS");
    public static final boolean DEBUG_NAMED_RUNNABLES = debugFlag("NAMED_RUNNABLES");
    public static final boolean DEBUG_GOAL_SELECTOR = debugFlag("GOAL_SELECTOR");
    public static final boolean DEBUG_VILLAGE_SECTIONS = debugFlag("VILLAGE_SECTIONS");
    public static final boolean DEBUG_BRAIN = debugFlag("BRAIN");
    public static final boolean DEBUG_POI = debugFlag("POI");
    public static final boolean DEBUG_BEES = debugFlag("BEES");
    public static final boolean DEBUG_RAIDS = debugFlag("RAIDS");
    public static final boolean DEBUG_BLOCK_BREAK = debugFlag("BLOCK_BREAK");
    public static final boolean DEBUG_MONITOR_TICK_TIMES = debugFlag("MONITOR_TICK_TIMES");
    public static final boolean DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN = debugFlag("KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN");
    public static final boolean DEBUG_DONT_SAVE_WORLD = debugFlag("DONT_SAVE_WORLD");
    public static final boolean DEBUG_LARGE_DRIPSTONE = debugFlag("LARGE_DRIPSTONE");
    public static final boolean DEBUG_CARVERS = debugFlag("CARVERS");
    public static final boolean DEBUG_ORE_VEINS = debugFlag("ORE_VEINS");
    public static final boolean DEBUG_SCULK_CATALYST = debugFlag("SCULK_CATALYST");
    public static final boolean DEBUG_BYPASS_REALMS_VERSION_CHECK = debugFlag("BYPASS_REALMS_VERSION_CHECK");
    public static final boolean DEBUG_SOCIAL_INTERACTIONS = debugFlag("SOCIAL_INTERACTIONS");
    public static final boolean DEBUG_VALIDATE_RESOURCE_PATH_CASE = debugFlag("VALIDATE_RESOURCE_PATH_CASE");
    public static final boolean DEBUG_UNLOCK_ALL_TRADES = debugFlag("UNLOCK_ALL_TRADES");
    public static final boolean DEBUG_BREEZE_MOB = debugFlag("BREEZE_MOB");
    public static final boolean DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS = debugFlag("TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS = debugFlag("VAULT_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_FORCE_ONBOARDING_SCREEN = debugFlag("FORCE_ONBOARDING_SCREEN");
    public static final boolean DEBUG_CURSOR_POS = debugFlag("CURSOR_POS");
    public static final boolean DEBUG_DEFAULT_SKIN_OVERRIDE = debugFlag("DEFAULT_SKIN_OVERRIDE");
    public static final boolean DEBUG_PANORAMA_SCREENSHOT = debugFlag("PANORAMA_SCREENSHOT");
    public static final boolean DEBUG_CHASE_COMMAND = debugFlag("CHASE_COMMAND");
    public static final boolean DEBUG_VERBOSE_COMMAND_ERRORS = debugFlag("VERBOSE_COMMAND_ERRORS");
    public static final boolean DEBUG_DEV_COMMANDS = debugFlag("DEV_COMMANDS");
    public static final boolean DEBUG_IGNORE_LOCAL_MOB_CAP = debugFlag("IGNORE_LOCAL_MOB_CAP");
    public static final boolean DEBUG_DISABLE_LIQUID_SPREADING = debugFlag("DISABLE_LIQUID_SPREADING");
    public static final boolean DEBUG_AQUIFERS = debugFlag("AQUIFERS");
    public static final boolean DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING = debugFlag("JFR_PROFILING_ENABLE_LEVEL_LOADING");
    public static final boolean DEBUG_ENTITY_BLOCK_INTERSECTION = debugFlag("ENTITY_BLOCK_INTERSECTION");
    public static boolean debugGenerateSquareTerrainWithoutNoise = debugFlag("GENERATE_SQUARE_TERRAIN_WITHOUT_NOISE");
    public static final boolean DEBUG_ONLY_GENERATE_HALF_THE_WORLD = debugFlag("ONLY_GENERATE_HALF_THE_WORLD");
    public static final boolean DEBUG_DISABLE_FLUID_GENERATION = debugFlag("DISABLE_FLUID_GENERATION");
    public static final boolean DEBUG_DISABLE_AQUIFERS = debugFlag("DISABLE_AQUIFERS");
    public static final boolean DEBUG_DISABLE_SURFACE = debugFlag("DISABLE_SURFACE");
    public static final boolean DEBUG_DISABLE_CARVERS = debugFlag("DISABLE_CARVERS");
    public static final boolean DEBUG_DISABLE_STRUCTURES = debugFlag("DISABLE_STRUCTURES");
    public static final boolean DEBUG_DISABLE_FEATURES = debugFlag("DISABLE_FEATURES");
    public static final boolean DEBUG_DISABLE_ORE_VEINS = debugFlag("DISABLE_ORE_VEINS");
    public static final boolean DEBUG_DISABLE_BLENDING = debugFlag("DISABLE_BLENDING");
    public static final boolean DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION = debugFlag("DISABLE_BELOW_ZERO_RETROGENERATION");
    /**
     * The default port used by Minecraft for communication between servers and clients.
     *
     * <p>This is not a registered port at the Internet Assigned Numbers Authority, and therefore may conflict with existing applications. Minecraft servers may be hosted at other ports, in which case the clients must supply the correct port when connecting to the server.</p>
     *
     * @see https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?&page=128 Internet Assigned Numbers Authority, Internet Service Name and Transport Protocol Port Number Registry
     */
    public static final int DEFAULT_MINECRAFT_PORT = 25565;
    public static final boolean DEBUG_SUBTITLES = debugFlag("SUBTITLES");
    public static final int DEBUG_FAKE_LATENCY_MS = debugIntValue("FAKE_LATENCY_MS");
    public static final int DEBUG_FAKE_JITTER_MS = debugIntValue("FAKE_JITTER_MS");
    public static final Level NETTY_LEAK_DETECTION = Level.DISABLED;
    public static final boolean COMMAND_STACK_TRACES = debugFlag("COMMAND_STACK_TRACES");
    public static final boolean DEBUG_WORLD_RECREATE = debugFlag("WORLD_RECREATE");
    public static final boolean DEBUG_SHOW_SERVER_DEBUG_VALUES = debugFlag("SHOW_SERVER_DEBUG_VALUES");
    public static final boolean DEBUG_FEATURE_COUNT = debugFlag("FEATURE_COUNT");
    public static final boolean DEBUG_FORCE_TELEMETRY = debugFlag("FORCE_TELEMETRY");
    public static final boolean DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND = debugFlag("DONT_SEND_TELEMETRY_TO_BACKEND");
    public static final long MAXIMUM_TICK_TIME_NANOS = Duration.ofMillis(300L).toNanos();
    public static final float MAXIMUM_BLOCK_EXPLOSION_RESISTANCE = 3600000.0F;
    public static final boolean USE_WORKFLOWS_HOOKS = false;
    public static final boolean USE_DEVONLY = false;
    public static boolean CHECK_DATA_FIXER_SCHEMA = true;
    public static boolean IS_RUNNING_IN_IDE = !net.neoforged.fml.loading.FMLEnvironment.isProduction();
    public static final boolean IS_RUNNING_WITH_JDWP = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(str -> str.startsWith("-agentlib:jdwp"));
    public static final int WORLD_RESOLUTION = 16;
    /**
     * The maximum length of a chat message that can be typed by a player.
     */
    public static final int MAX_CHAT_LENGTH = 256;
    public static final int MAX_USER_INPUT_COMMAND_LENGTH = 32500;
    public static final int MAX_FUNCTION_COMMAND_LENGTH = 2000000;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final int MAX_CHAINED_NEIGHBOR_UPDATES = 1000000;
    public static final int MAX_RENDER_DISTANCE = 32;
    /**
     * The characters which may not form part of a file's name. Used in various file processing methods to replace these illegal characters with valid characters, such as the underscore "{@code _}".
     *
     * <p>This collection of characters is a subset of the forbidden characters listed in the documentation for Microsoft Windows, with the addition of the backtick ({@code `}).
     *
     * @see <a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">Microsoft Corporation, "Naming Files, Paths, and Namespaces", &sect; Naming Conventions</a>
     */
    public static final char[] ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
    /**
     * The amount of game ticks within a real-life second.
     *
     * <p>This is not guaranteed by a running server to be an accurate measurement of real-life seconds. Heavy load on the server may cause the actual tick-to-second ratio to go below this defined ratio.</p>
     */
    public static final int TICKS_PER_SECOND = 20;
    public static final int MILLIS_PER_TICK = 50;
    /**
     * The amount of game ticks within a real-life minute. This is equal to {@code TICKS_PER_SECOND * 60}, and falls under the same guarantees and restrictions as {@link #TICKS_PER_SECOND}.
     *
     * @see #TICKS_PER_SECOND
     */
    public static final int TICKS_PER_MINUTE = 1200;
    /**
     * The amount of game ticks within a game day. A game day is defined as 20 real-life minutes, so this is equal to {@code TICKS_PER_MINUTE * 20}. This falls under the same guarantees and restrictions as {@link #TICKS_PER_SECOND}.
     */
    public static final int TICKS_PER_GAME_DAY = 24000;
    public static final float AVERAGE_GAME_TICKS_PER_RANDOM_TICK_PER_BLOCK = 1365.3334F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_MINUTE = 0.87890625F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_GAME_DAY = 17.578125F;
    public static final int WORLD_ICON_SIZE = 64;
    @Nullable
    private static WorldVersion CURRENT_VERSION;

    private static String prefixDebugFlagName(String name) {
        return "MC_DEBUG_" + name;
    }

    private static boolean booleanProperty(String name) {
        String s = System.getProperty(name);
        return s != null && (s.isEmpty() || Boolean.parseBoolean(s));
    }

    private static boolean debugFlag(String name) {
        if (!DEBUG_ENABLED) {
            return false;
        } else {
            String s = prefixDebugFlagName(name);
            if (DEBUG_PRINT_PROPERTIES) {
                System.out.println("Debug property available: " + s + ": bool");
            }

            return booleanProperty(s);
        }
    }

    private static int debugIntValue(String name) {
        if (!DEBUG_ENABLED) {
            return 0;
        } else {
            String s = prefixDebugFlagName(name);
            if (DEBUG_PRINT_PROPERTIES) {
                System.out.println("Debug property available: " + s + ": int");
            }

            return Integer.parseInt(System.getProperty(s, "0"));
        }
    }

    /**
     * Sets the world version, failing if a different world version is already present.
     *
     * @throws IllegalStateException if a different world version has already been set previously
     *
     * @param version the world version to set
     */
    public static void setVersion(WorldVersion version) {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = version;
        } else if (version != CURRENT_VERSION) {
            throw new IllegalStateException("Cannot override the current game version!");
        }
    }

    public static void tryDetectVersion() {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = DetectedVersion.tryDetectVersion();
        }
    }

    public static WorldVersion getCurrentVersion() {
        if (CURRENT_VERSION == null) {
            throw new IllegalStateException("Game version not set");
        } else {
            return CURRENT_VERSION;
        }
    }

    public static int getProtocolVersion() {
        return 773;
    }

    public static boolean debugVoidTerrain(ChunkPos chunkPos) {
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        if (DEBUG_ONLY_GENERATE_HALF_THE_WORLD) {
            return j < 0;
        } else {
            return !debugGenerateSquareTerrainWithoutNoise ? false : i > 8192 || i < 0 || j > 1024 || j < 0;
        }
    }

    static {
        if (System.getProperty("io.netty.leakDetection.level") == null) // Forge: allow level to be manually specified
        ResourceLeakDetector.setLevel(NETTY_LEAK_DETECTION);
        CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = COMMAND_STACK_TRACES;
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BrigadierExceptions();
    }
}
