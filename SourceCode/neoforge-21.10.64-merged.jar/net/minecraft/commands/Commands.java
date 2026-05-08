package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.server.commands.AttributeCommand;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.DamageCommand;
import net.minecraft.server.commands.DataPackCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.DebugCommand;
import net.minecraft.server.commands.DebugConfigCommand;
import net.minecraft.server.commands.DebugMobSpawningCommand;
import net.minecraft.server.commands.DebugPathCommand;
import net.minecraft.server.commands.DefaultGameModeCommands;
import net.minecraft.server.commands.DialogCommand;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.server.commands.FetchProfileCommand;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.commands.HelpCommand;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.JfrCommand;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.PermissionCheck;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.RaidCommand;
import net.minecraft.server.commands.RandomCommand;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.commands.ReturnCommand;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.server.commands.RotateCommand;
import net.minecraft.server.commands.SaveAllCommand;
import net.minecraft.server.commands.SaveOffCommand;
import net.minecraft.server.commands.SaveOnCommand;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.commands.ScheduleCommand;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.commands.SeedCommand;
import net.minecraft.server.commands.ServerPackCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.SetPlayerIdleTimeoutCommand;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.commands.SetWorldSpawnCommand;
import net.minecraft.server.commands.SpawnArmorTrimsCommand;
import net.minecraft.server.commands.SpectateCommand;
import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.commands.StopCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.TagCommand;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.commands.TickCommand;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.commands.TransferCommand;
import net.minecraft.server.commands.TriggerCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.server.commands.WardenSpawnTrackerCommand;
import net.minecraft.server.commands.WaypointCommand;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class Commands {
    public static final String COMMAND_PREFIX = "/";
    private static final ThreadLocal<ExecutionContext<CommandSourceStack>> CURRENT_EXECUTION_CONTEXT = new ThreadLocal<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATORS = 1;
    public static final int LEVEL_GAMEMASTERS = 2;
    public static final int LEVEL_ADMINS = 3;
    public static final int LEVEL_OWNERS = 4;
    private static final ClientboundCommandsPacket.NodeInspector<CommandSourceStack> COMMAND_NODE_INSPECTOR = new ClientboundCommandsPacket.NodeInspector<CommandSourceStack>() {
        @Nullable
        @Override
        public ResourceLocation suggestionId(ArgumentCommandNode<CommandSourceStack, ?> p_425984_) {
            SuggestionProvider<CommandSourceStack> suggestionprovider = p_425984_.getCustomSuggestions();
            return suggestionprovider != null ? SuggestionProviders.getName(suggestionprovider) : null;
        }

        @Override
        public boolean isExecutable(CommandNode<CommandSourceStack> p_425797_) {
            return p_425797_.getCommand() != null;
        }

        @Override
        public boolean isRestricted(CommandNode<CommandSourceStack> p_426014_) {
            return p_426014_.getRequirement() instanceof PermissionCheck<?> permissioncheck && permissioncheck.requiredLevel() > 0;
        }
    };
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    public Commands(Commands.CommandSelection selection, CommandBuildContext context) {
        AdvancementCommands.register(this.dispatcher);
        AttributeCommand.register(this.dispatcher, context);
        ExecuteCommand.register(this.dispatcher, context);
        BossBarCommands.register(this.dispatcher, context);
        ClearInventoryCommands.register(this.dispatcher, context);
        CloneCommands.register(this.dispatcher, context);
        DamageCommand.register(this.dispatcher, context);
        DataCommands.register(this.dispatcher);
        DataPackCommand.register(this.dispatcher, context);
        DebugCommand.register(this.dispatcher);
        DefaultGameModeCommands.register(this.dispatcher);
        DialogCommand.register(this.dispatcher, context);
        DifficultyCommand.register(this.dispatcher);
        EffectCommands.register(this.dispatcher, context);
        EmoteCommands.register(this.dispatcher);
        EnchantCommand.register(this.dispatcher, context);
        ExperienceCommand.register(this.dispatcher);
        FillCommand.register(this.dispatcher, context);
        FillBiomeCommand.register(this.dispatcher, context);
        ForceLoadCommand.register(this.dispatcher);
        FunctionCommand.register(this.dispatcher);
        GameModeCommand.register(this.dispatcher);
        GameRuleCommand.register(this.dispatcher, context);
        GiveCommand.register(this.dispatcher, context);
        HelpCommand.register(this.dispatcher);
        ItemCommands.register(this.dispatcher, context);
        KickCommand.register(this.dispatcher);
        KillCommand.register(this.dispatcher);
        ListPlayersCommand.register(this.dispatcher);
        LocateCommand.register(this.dispatcher, context);
        LootCommand.register(this.dispatcher, context);
        MsgCommand.register(this.dispatcher);
        ParticleCommand.register(this.dispatcher, context);
        PlaceCommand.register(this.dispatcher);
        PlaySoundCommand.register(this.dispatcher);
        RandomCommand.register(this.dispatcher);
        ReloadCommand.register(this.dispatcher);
        RecipeCommand.register(this.dispatcher);
        FetchProfileCommand.register(this.dispatcher);
        ReturnCommand.register(this.dispatcher);
        RideCommand.register(this.dispatcher);
        RotateCommand.register(this.dispatcher);
        SayCommand.register(this.dispatcher);
        ScheduleCommand.register(this.dispatcher);
        ScoreboardCommand.register(this.dispatcher, context);
        SeedCommand.register(this.dispatcher, selection != Commands.CommandSelection.INTEGRATED);
        VersionCommand.register(this.dispatcher, selection != Commands.CommandSelection.INTEGRATED);
        SetBlockCommand.register(this.dispatcher, context);
        SetSpawnCommand.register(this.dispatcher);
        SetWorldSpawnCommand.register(this.dispatcher);
        SpectateCommand.register(this.dispatcher);
        SpreadPlayersCommand.register(this.dispatcher);
        StopSoundCommand.register(this.dispatcher);
        SummonCommand.register(this.dispatcher, context);
        TagCommand.register(this.dispatcher);
        TeamCommand.register(this.dispatcher, context);
        TeamMsgCommand.register(this.dispatcher);
        TeleportCommand.register(this.dispatcher);
        TellRawCommand.register(this.dispatcher, context);
        TestCommand.register(this.dispatcher, context);
        TickCommand.register(this.dispatcher);
        TimeCommand.register(this.dispatcher);
        TitleCommand.register(this.dispatcher, context);
        TriggerCommand.register(this.dispatcher);
        WaypointCommand.register(this.dispatcher, context);
        WeatherCommand.register(this.dispatcher);
        WorldBorderCommand.register(this.dispatcher);
        if (JvmProfiler.INSTANCE.isAvailable()) {
            JfrCommand.register(this.dispatcher);
        }

        if (SharedConstants.DEBUG_CHASE_COMMAND) {
            ChaseCommand.register(this.dispatcher);
        }

        if (SharedConstants.DEBUG_DEV_COMMANDS || SharedConstants.IS_RUNNING_IN_IDE) {
            RaidCommand.register(this.dispatcher, context);
            DebugPathCommand.register(this.dispatcher);
            DebugMobSpawningCommand.register(this.dispatcher);
            WardenSpawnTrackerCommand.register(this.dispatcher);
            SpawnArmorTrimsCommand.register(this.dispatcher);
            ServerPackCommand.register(this.dispatcher);
            if (selection.includeDedicated) {
                DebugConfigCommand.register(this.dispatcher, context);
            }
        }

        if (selection.includeDedicated) {
            BanIpCommands.register(this.dispatcher);
            BanListCommands.register(this.dispatcher);
            BanPlayerCommands.register(this.dispatcher);
            DeOpCommands.register(this.dispatcher);
            OpCommand.register(this.dispatcher);
            PardonCommand.register(this.dispatcher);
            PardonIpCommand.register(this.dispatcher);
            PerfCommand.register(this.dispatcher);
            SaveAllCommand.register(this.dispatcher);
            SaveOffCommand.register(this.dispatcher);
            SaveOnCommand.register(this.dispatcher);
            SetPlayerIdleTimeoutCommand.register(this.dispatcher);
            StopCommand.register(this.dispatcher);
            TransferCommand.register(this.dispatcher);
            WhitelistCommand.register(this.dispatcher);
        }

        if (selection.includeIntegrated) {
            PublishCommand.register(this.dispatcher);
        }
        net.neoforged.neoforge.event.EventHooks.onCommandRegister(this.dispatcher, selection, context);

        this.dispatcher.setConsumer(ExecutionCommandSource.resultConsumer());
    }

    public static <S> ParseResults<S> mapSource(ParseResults<S> parseResults, UnaryOperator<S> mapper) {
        CommandContextBuilder<S> commandcontextbuilder = parseResults.getContext();
        CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder.withSource(mapper.apply(commandcontextbuilder.getSource()));
        return new ParseResults<>(commandcontextbuilder1, parseResults.getReader(), parseResults.getExceptions());
    }

    public void performPrefixedCommand(CommandSourceStack source, String command) {
        command = trimOptionalPrefix(command);
        this.performCommand(this.dispatcher.parse(command, source), command);
    }

    public static String trimOptionalPrefix(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    public void performCommand(ParseResults<CommandSourceStack> parseResults, String command) {
        CommandSourceStack commandsourcestack = parseResults.getContext().getSource();
        net.neoforged.neoforge.event.CommandEvent event = new net.neoforged.neoforge.event.CommandEvent(parseResults);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) {
            if (event.getException() != null) {
                commandsourcestack.sendFailure(Component.literal(Util.describeError(event.getException())));
                LOGGER.error("'/{}' threw an exception", command, event.getException());
            }
            return;
        }
        parseResults = event.getParseResults();

        Profiler.get().push(() -> "/" + command);
        ContextChain<CommandSourceStack> contextchain = finishParsing(parseResults, command, commandsourcestack);

        try {
            if (contextchain != null) {
                executeCommandInContext(
                    commandsourcestack,
                    p_309417_ -> ExecutionContext.queueInitialCommandExecution(
                        p_309417_, command, contextchain, commandsourcestack, CommandResultCallback.EMPTY
                    )
                );
            }
        } catch (Exception exception) {
            MutableComponent mutablecomponent = Component.literal(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Command exception: /{}", command, exception);
                StackTraceElement[] astacktraceelement = exception.getStackTrace();

                for (int i = 0; i < Math.min(astacktraceelement.length, 3); i++) {
                    mutablecomponent.append("\n\n")
                        .append(astacktraceelement[i].getMethodName())
                        .append("\n ")
                        .append(astacktraceelement[i].getFileName())
                        .append(":")
                        .append(String.valueOf(astacktraceelement[i].getLineNumber()));
                }
            }

            commandsourcestack.sendFailure(
                Component.translatable("command.failed").withStyle(p_392579_ -> p_392579_.withHoverEvent(new HoverEvent.ShowText(mutablecomponent)))
            );
            if (SharedConstants.DEBUG_VERBOSE_COMMAND_ERRORS || SharedConstants.IS_RUNNING_IN_IDE) {
                commandsourcestack.sendFailure(Component.literal(Util.describeError(exception)));
                LOGGER.error("'/{}' threw an exception", command, exception);
            }
        } finally {
            Profiler.get().pop();
        }
    }

    @Nullable
    private static ContextChain<CommandSourceStack> finishParsing(ParseResults<CommandSourceStack> parseResults, String command, CommandSourceStack source) {
        try {
            validateParseResults(parseResults);
            return ContextChain.tryFlatten(parseResults.getContext().build(command))
                .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parseResults.getReader()));
        } catch (CommandSyntaxException commandsyntaxexception) {
            source.sendFailure(ComponentUtils.fromMessage(commandsyntaxexception.getRawMessage()));
            if (commandsyntaxexception.getInput() != null && commandsyntaxexception.getCursor() >= 0) {
                int i = Math.min(commandsyntaxexception.getInput().length(), commandsyntaxexception.getCursor());
                MutableComponent mutablecomponent = Component.empty()
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(p_392577_ -> p_392577_.withClickEvent(new ClickEvent.SuggestCommand("/" + command)));
                if (i > 10) {
                    mutablecomponent.append(CommonComponents.ELLIPSIS);
                }

                mutablecomponent.append(commandsyntaxexception.getInput().substring(Math.max(0, i - 10), i));
                if (i < commandsyntaxexception.getInput().length()) {
                    Component component = Component.literal(commandsyntaxexception.getInput().substring(i))
                        .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
                    mutablecomponent.append(component);
                }

                mutablecomponent.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                source.sendFailure(mutablecomponent);
            }

            return null;
        }
    }

    public static void executeCommandInContext(CommandSourceStack source, Consumer<ExecutionContext<CommandSourceStack>> contextConsumer) {
        MinecraftServer minecraftserver = source.getServer();
        ExecutionContext<CommandSourceStack> executioncontext = CURRENT_EXECUTION_CONTEXT.get();
        boolean flag = executioncontext == null;
        if (flag) {
            int i = Math.max(1, minecraftserver.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH));
            int j = minecraftserver.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_FORK_COUNT);

            try (ExecutionContext<CommandSourceStack> executioncontext1 = new ExecutionContext<>(i, j, Profiler.get())) {
                CURRENT_EXECUTION_CONTEXT.set(executioncontext1);
                contextConsumer.accept(executioncontext1);
                executioncontext1.runCommandQueue();
            } finally {
                CURRENT_EXECUTION_CONTEXT.set(null);
            }
        } else {
            contextConsumer.accept(executioncontext);
        }
    }

    public void sendCommands(ServerPlayer player) {
        Map<CommandNode<CommandSourceStack>, CommandNode<CommandSourceStack>> map = new HashMap<>();
        RootCommandNode<CommandSourceStack> rootcommandnode = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), rootcommandnode);
        // Neo: Use our own command node merging method to handle redirect nodes properly, see issue Forge-7551
        net.neoforged.neoforge.server.command.CommandHelper.mergeCommandNode(this.dispatcher.getRoot(), rootcommandnode, map, player.createCommandSourceStack(), ctx -> 0, suggest -> SuggestionProviders.cast((com.mojang.brigadier.suggestion.SuggestionProvider<SharedSuggestionProvider>) (com.mojang.brigadier.suggestion.SuggestionProvider<?>) suggest));
        player.connection.send(new ClientboundCommandsPacket(rootcommandnode, COMMAND_NODE_INSPECTOR));
    }

    // Neo: Replaced by our own command merging logic
    private static <S> void fillUsableCommands(CommandNode<S> root, CommandNode<S> current, S source, Map<CommandNode<S>, CommandNode<S>> output) {
        for (CommandNode<S> commandnode : root.getChildren()) {
            if (commandnode.canUse(source)) {
                ArgumentBuilder<S, ?> argumentbuilder = commandnode.createBuilder();
                if (argumentbuilder.getRedirect() != null) {
                    argumentbuilder.redirect(output.get(argumentbuilder.getRedirect()));
                }

                CommandNode<S> commandnode1 = argumentbuilder.build();
                output.put(commandnode, commandnode1);
                current.addChild(commandnode1);
                if (!commandnode.getChildren().isEmpty()) {
                    fillUsableCommands(commandnode, commandnode1, source, output);
                }
            }
        }
    }

    /**
     * Creates a new argument. Intended to be imported statically. The benefit of this over the brigadier {@link LiteralArgumentBuilder#literal} method is that it is typed to {@link CommandSource}.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * Creates a new argument. Intended to be imported statically. The benefit of this over the brigadier {@link RequiredArgumentBuilder#argument} method is that it is typed to {@link CommandSource}.
     */
    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static Predicate<String> createValidator(Commands.ParseFunction parser) {
        return p_82124_ -> {
            try {
                parser.parse(new StringReader(p_82124_));
                return true;
            } catch (CommandSyntaxException commandsyntaxexception) {
                return false;
            }
        };
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.dispatcher;
    }

    public static <S> void validateParseResults(ParseResults<S> parseResults) throws CommandSyntaxException {
        CommandSyntaxException commandsyntaxexception = getParseException(parseResults);
        if (commandsyntaxexception != null) {
            throw commandsyntaxexception;
        }
    }

    @Nullable
    public static <S> CommandSyntaxException getParseException(ParseResults<S> result) {
        if (!result.getReader().canRead()) {
            return null;
        } else if (result.getExceptions().size() == 1) {
            return result.getExceptions().values().iterator().next();
        } else {
            return result.getContext().getRange().isEmpty()
                ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(result.getReader())
                : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(result.getReader());
        }
    }

    public static CommandBuildContext createValidationContext(final HolderLookup.Provider provider) {
        return new CommandBuildContext() {
            @Override
            public FeatureFlagSet enabledFeatures() {
                return FeatureFlags.REGISTRY.allFlags();
            }

            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return provider.listRegistryKeys();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_425855_) {
                return provider.lookup(p_425855_).map(this::createLookup);
            }

            private <T> HolderLookup.RegistryLookup.Delegate<T> createLookup(final HolderLookup.RegistryLookup<T> p_425639_) {
                return new HolderLookup.RegistryLookup.Delegate<T>() {
                    @Override
                    public HolderLookup.RegistryLookup<T> parent() {
                        return p_425639_;
                    }

                    @Override
                    public Optional<HolderSet.Named<T>> get(TagKey<T> p_426087_) {
                        return Optional.of(this.getOrThrow(p_426087_));
                    }

                    @Override
                    public HolderSet.Named<T> getOrThrow(TagKey<T> p_425964_) {
                        Optional<HolderSet.Named<T>> optional = this.parent().get(p_425964_);
                        return optional.orElseGet(() -> HolderSet.emptyNamed(this.parent(), p_425964_));
                    }
                };
            }
        };
    }

    public static void validate() {
        CommandBuildContext commandbuildcontext = createValidationContext(VanillaRegistries.createLookup());
        CommandDispatcher<CommandSourceStack> commanddispatcher = new Commands(Commands.CommandSelection.ALL, commandbuildcontext).getDispatcher();
        RootCommandNode<CommandSourceStack> rootcommandnode = commanddispatcher.getRoot();
        commanddispatcher.findAmbiguities(
            (p_230947_, p_230948_, p_230949_, p_230950_) -> LOGGER.warn(
                "Ambiguity between arguments {} and {} with inputs: {}", commanddispatcher.getPath(p_230948_), commanddispatcher.getPath(p_230949_), p_230950_
            )
        );
        Set<ArgumentType<?>> set = ArgumentUtils.findUsedArgumentTypes(rootcommandnode);
        Set<ArgumentType<?>> set1 = set.stream().filter(p_339316_ -> !ArgumentTypeInfos.isClassRecognized(p_339316_.getClass())).collect(Collectors.toSet());
        if (!set1.isEmpty()) {
            LOGGER.warn(
                "Missing type registration for following arguments:\n {}", set1.stream().map(p_339315_ -> "\t" + p_339315_).collect(Collectors.joining(",\n"))
            );
            throw new IllegalStateException("Unregistered argument types");
        }
    }

    public static <T extends PermissionSource> PermissionCheck<T> hasPermission(int level) {
        return new PermissionSource.Check<>(level);
    }

    public static enum CommandSelection {
        ALL(true, true),
        DEDICATED(false, true),
        INTEGRATED(true, false);

        final boolean includeIntegrated;
        final boolean includeDedicated;

        private CommandSelection(boolean includeIntegrated, boolean includeDedicated) {
            this.includeIntegrated = includeIntegrated;
            this.includeDedicated = includeDedicated;
        }
    }

    @FunctionalInterface
    public interface ParseFunction {
        void parse(StringReader input) throws CommandSyntaxException;
    }
}
