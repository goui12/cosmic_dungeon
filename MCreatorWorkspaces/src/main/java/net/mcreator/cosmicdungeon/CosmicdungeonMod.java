package net.mcreator.cosmicdungeon;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.util.Tuple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import com.mojang.brigadier.CommandDispatcher;

@Mod("cosmicdungeon")
public class CosmicdungeonMod {
	public static final Logger LOGGER = LogManager.getLogger(CosmicdungeonMod.class);
	public static final String MODID = "cosmicdungeon";

	private static final DebugConfig DEBUG_CONFIG = new DebugConfig();
	private static final Map<DebugSystem, AtomicInteger> DEBUG_LINES_THIS_TICK = new ConcurrentHashMap<>();
	private static final Map<DebugSystem, Integer> DEBUG_AUTO_DISABLE_TICKS_LEFT = new ConcurrentHashMap<>();
	private static long serverTickCounter = 0;
	private static final ResourceLocation CURRENCY_INTRO_ACHIEVEMENT = ResourceLocation.fromNamespaceAndPath(MODID, "currency/im_rich");
	private static final String CURRENCY_BALANCE_KEY = MODID + ".currency.trace_balance";
	private static final long CURRENCY_INTRO_REWARD_TRACE = 5L;

	public CosmicdungeonMod(IEventBus modEventBus) {
		DEBUG_CONFIG.load();
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		debug(DebugSystem.CORE, () -> "Mod initialized. Debug config at: " + DEBUG_CONFIG.configPath);
		debug(DebugSystem.CONFIG, DEBUG_CONFIG::summary);
	}

	public enum DebugSystem {
		CORE,
		CONFIG,
		NETWORKING,
		WORK_QUEUE,
		RF_TRANSMITTER_RECEIVER,
		COSMIC_SPAWNER,
		RIFT_DESTINATIONS,
		RIFTS,
		DUNGEON_GENERATION,
		WORLDGEN,
		DIMENSION_TELEPORT,
		CHUNK_LOADING,
		BLOCK_ENTITY_TICKS,
		ENTITY_AI,
		LOOT_TABLES,
		COMMANDS,
		REGION_PROTECTION,
		PERFORMANCE
	}

	private static final class DebugConfig {
		private static final String FILE_NAME = "Cosmicdungeon.debug.config";
		private final Path configPath = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

		private volatile boolean debugEnabled = true;
		private volatile boolean includeWallClock = true;
		private volatile boolean includeThreadName = false;
		private volatile int maxOutputPerTick = 500;
		private volatile int autoDisableTicks = 20;
		private volatile int performanceSummaryIntervalTicks = 200;
		private final Map<DebugSystem, Boolean> enabledSystems = new ConcurrentHashMap<>();

		private void load() {
			Properties defaults = defaults();
			Properties properties = new Properties(defaults);
			if (Files.notExists(configPath))
				writeDefaults(defaults);
			try (var reader = Files.newBufferedReader(configPath)) {
				properties.load(reader);
			} catch (IOException e) {
				LOGGER.error("Failed reading debug config {}; using defaults", configPath, e);
			}
			debugEnabled = getBoolean(properties, "debug.enabled", debugEnabled);
			includeWallClock = getBoolean(properties, "debug.include_wall_clock", includeWallClock);
			includeThreadName = getBoolean(properties, "debug.include_thread_name", includeThreadName);
			maxOutputPerTick = Math.max(1, getInt(properties, "debug.output_per_tick", maxOutputPerTick));
			autoDisableTicks = Math.max(1, getInt(properties, "debug.auto_disable_ticks", autoDisableTicks));
			performanceSummaryIntervalTicks = Math.max(20, getInt(properties, "debug.performance_summary_interval_ticks", performanceSummaryIntervalTicks));
			for (DebugSystem system : DebugSystem.values()) {
				enabledSystems.put(system, getBoolean(properties, "debug.system." + key(system), defaultFor(system)));
			}
		}

		private String summary() {
			return "Loaded debug config: enabled=" + debugEnabled + ", output_per_tick=" + maxOutputPerTick + ", auto_disable_ticks=" + autoDisableTicks
				+ ", performance_summary_interval_ticks=" + performanceSummaryIntervalTicks + ", systems_enabled=" + enabledSystems;
		}

		private Properties defaults() {
			Properties defaults = new Properties();
			defaults.setProperty("debug.enabled", "true");
			defaults.setProperty("debug.include_wall_clock", "true");
			defaults.setProperty("debug.include_thread_name", "false");
			defaults.setProperty("debug.output_per_tick", "500");
			defaults.setProperty("debug.auto_disable_ticks", "20");
			defaults.setProperty("debug.performance_summary_interval_ticks", "200");
			for (DebugSystem system : DebugSystem.values()) {
				defaults.setProperty("debug.system." + key(system), Boolean.toString(defaultFor(system)));
			}
			return defaults;
		}

		private void writeDefaults(Properties defaults) {
			try {
				Files.createDirectories(configPath.getParent());
				try (var writer = Files.newBufferedWriter(configPath)) {
					writer.write("# Cosmic Dungeon debug configuration\n");
					writer.write("# debug.enabled = master switch for every debug output\n");
					writer.write("# debug.system.<name> = per-system toggle\n");
					writer.write("# debug.output_per_tick = max lines per system in a single server tick\n");
					writer.write("# debug.auto_disable_ticks = cooldown after a system exceeds output_per_tick\n\n");
					defaults.forEach((k, v) -> {
						try {
							writer.write(k + "=" + v + "\n");
						} catch (IOException ignored) {
						}
					});
				}
			} catch (IOException e) {
				LOGGER.error("Failed writing default debug config {}", configPath, e);
			}
		}

		private boolean isEnabled(DebugSystem system) {
			if (!debugEnabled)
				return false;
			return enabledSystems.getOrDefault(system, defaultFor(system));
		}

		private boolean getBoolean(Properties properties, String key, boolean fallback) {
			String value = properties.getProperty(key);
			if (value == null)
				return fallback;
			return Boolean.parseBoolean(value.trim());
		}

		private int getInt(Properties properties, String key, int fallback) {
			String value = properties.getProperty(key);
			if (value == null)
				return fallback;
			try {
				return Integer.parseInt(value.trim());
			} catch (NumberFormatException ex) {
				LOGGER.warn("Invalid integer in debug config for {}: {}. Using {}.", key, value, fallback);
				return fallback;
			}
		}

		private String key(DebugSystem system) {
			return system.name().toLowerCase(Locale.ROOT);
		}

		private boolean defaultFor(DebugSystem system) {
			return switch (system) {
				case CORE, CONFIG, NETWORKING, WORK_QUEUE, RF_TRANSMITTER_RECEIVER, RIFTS, PERFORMANCE -> true;
				default -> false;
			};
		}
	}

	public static void debug(DebugSystem system, String message) {
		debug(system, () -> message);
	}

	public static void debug(DebugSystem system, java.util.function.Supplier<String> messageSupplier) {
		if (!DEBUG_CONFIG.isEnabled(system))
			return;
		if (DEBUG_AUTO_DISABLE_TICKS_LEFT.getOrDefault(system, 0) > 0)
			return;

		AtomicInteger counter = DEBUG_LINES_THIS_TICK.computeIfAbsent(system, ignored -> new AtomicInteger(0));
		int current = counter.incrementAndGet();
		if (current > DEBUG_CONFIG.maxOutputPerTick) {
			if (current == DEBUG_CONFIG.maxOutputPerTick + 1) {
				DEBUG_AUTO_DISABLE_TICKS_LEFT.put(system, DEBUG_CONFIG.autoDisableTicks);
				LOGGER.warn("[CosmicDebug][{}][tick={}] output_per_tick limit {} exceeded; silencing this system for {} ticks",
					system.name(), serverTickCounter, DEBUG_CONFIG.maxOutputPerTick, DEBUG_CONFIG.autoDisableTicks);
			}
			return;
		}

		String prefix = "[CosmicDebug][" + system.name() + "][tick=" + serverTickCounter + "]";
		if (DEBUG_CONFIG.includeWallClock)
			prefix += "[time=" + Instant.now() + "]";
		if (DEBUG_CONFIG.includeThreadName)
			prefix += "[thread=" + Thread.currentThread().getName() + "]";
		LOGGER.info("{} {}", prefix, messageSupplier.get());
	}

	private static void beginTickDebugAccounting() {
		serverTickCounter++;
		DEBUG_LINES_THIS_TICK.clear();
		DEBUG_AUTO_DISABLE_TICKS_LEFT.replaceAll((system, ticksLeft) -> Math.max(0, ticksLeft - 1));
	}

	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();
	private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
		debug(DebugSystem.NETWORKING, () -> "Queued network message registration: " + id.id().toString().toLowerCase(Locale.ROOT));
	}

	public static void debugState(DebugSystem system, String operation, Map<String, ?> state) {
		debug(system, () -> operation + " state=" + state);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
		debug(DebugSystem.NETWORKING, () -> "Registered payload handlers. Count=" + MESSAGES.size());
	}

	public static void queueServerWork(int tick, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			workQueue.add(new Tuple<>(action, tick));
			debug(DebugSystem.WORK_QUEUE, () -> "Queued server work. delayTicks=" + tick + ", queueSize=" + workQueue.size());
		}
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		registerCurrencyCommand(event.getDispatcher());
	}

	private static void registerCurrencyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("currency")
			.then(Commands.literal("balance")
				.executes(context -> showCurrencyBalance(context.getSource(), context.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player())
					.requires(source -> source.hasPermission(2))
					.executes(context -> showCurrencyBalance(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
	}

	private static int showCurrencyBalance(CommandSourceStack source, ServerPlayer player) {
		long traceBalance = getCurrencyBalance(player);
		source.sendSuccess(() -> Component.empty()
			.append(player.getDisplayName())
			.append(Component.literal(" has ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(formatCurrencyBalance(traceBalance)).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" in /currency balance.").withStyle(ChatFormatting.GRAY)), false);
		return 1;
	}


	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		queueServerWork(20, () -> grantCurrencyIntroAchievement(player));
	}

	private static void grantCurrencyIntroAchievement(ServerPlayer player) {
		AdvancementHolder advancement = player.server.getAdvancements().get(CURRENCY_INTRO_ACHIEVEMENT);
		if (advancement == null) {
			LOGGER.warn("Could not find currency intro achievement {}", CURRENCY_INTRO_ACHIEVEMENT);
			return;
		}

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (progress.isDone())
			return;

		for (String criterion : progress.getRemainingCriteria())
			player.getAdvancements().award(advancement, criterion);

		if (!player.getAdvancements().getOrStartProgress(advancement).isDone())
			return;

		addTraceToCurrencyBalance(player, CURRENCY_INTRO_REWARD_TRACE);
		player.sendSystemMessage(Component.empty()
			.append(Component.literal("Reward: ").withStyle(ChatFormatting.GOLD))
			.append(Component.literal("5 Trace!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
			.append(Component.literal(" (Check achievements for more information.)").withStyle(ChatFormatting.LIGHT_PURPLE)));
	}

	private static long getCurrencyBalance(ServerPlayer player) {
		return player.getPersistentData().getLong(CURRENCY_BALANCE_KEY);
	}

	private static void addTraceToCurrencyBalance(ServerPlayer player, long traceAmount) {
		player.getPersistentData().putLong(CURRENCY_BALANCE_KEY, getCurrencyBalance(player) + traceAmount);
	}

	private static String formatCurrencyBalance(long traceBalance) {
		long anchors = traceBalance / 10000L;
		traceBalance %= 10000L;
		long crowns = traceBalance / 1000L;
		traceBalance %= 1000L;
		long seals = traceBalance / 100L;
		traceBalance %= 100L;
		long marks = traceBalance / 10L;
		long traces = traceBalance % 10L;

		List<String> parts = new ArrayList<>();
		if (anchors > 0)
			parts.add(anchors + "A");
		if (crowns > 0)
			parts.add(crowns + "C");
		if (seals > 0)
			parts.add(seals + "S");
		if (marks > 0)
			parts.add(marks + "M");
		if (traces > 0 || parts.isEmpty())
			parts.add(traces + "T");
		return String.join(" ", parts) + " (" + traceBalanceTotal(anchors, crowns, seals, marks, traces) + " Trace)";
	}

	private static long traceBalanceTotal(long anchors, long crowns, long seals, long marks, long traces) {
		return anchors * 10000L + crowns * 1000L + seals * 100L + marks * 10L + traces;
	}

	@SubscribeEvent
	public void tick(ServerTickEvent.Post event) {
		beginTickDebugAccounting();
		debug(DebugSystem.CORE, () -> "Server tick start. workQueueSize=" + workQueue.size());
		List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
		workQueue.forEach(work -> {
			work.setB(work.getB() - 1);
			if (work.getB() == 0)
				actions.add(work);
		});
		actions.forEach(e -> e.getA().run());
		workQueue.removeAll(actions);
		debug(DebugSystem.WORK_QUEUE, () -> "Executed actions=" + actions.size() + ", queueSizeAfter=" + workQueue.size());
		if (serverTickCounter % DEBUG_CONFIG.performanceSummaryIntervalTicks == 0) {
			debug(DebugSystem.PERFORMANCE, () -> "perf-summary active_debug_cooldowns=" + DEBUG_AUTO_DISABLE_TICKS_LEFT + ", queuedWork=" + workQueue.size());
		}
	}
}
