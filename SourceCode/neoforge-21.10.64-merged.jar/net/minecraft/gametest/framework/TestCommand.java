package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableInt;

public class TestCommand {
    public static final int TEST_NEARBY_SEARCH_RADIUS = 15;
    public static final int TEST_FULL_SEARCH_RADIUS = 250;
    public static final int VERIFY_TEST_GRID_AXIS_SIZE = 10;
    public static final int VERIFY_TEST_BATCH_SIZE = 100;
    private static final int DEFAULT_CLEAR_RADIUS = 250;
    private static final int MAX_CLEAR_RADIUS = 1024;
    private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
    private static final int DEFAULT_X_SIZE = 5;
    private static final int DEFAULT_Y_SIZE = 5;
    private static final int DEFAULT_Z_SIZE = 5;
    private static final SimpleCommandExceptionType CLEAR_NO_TESTS = new SimpleCommandExceptionType(
        Component.translatable("commands.test.clear.error.no_tests")
    );
    private static final SimpleCommandExceptionType RESET_NO_TESTS = new SimpleCommandExceptionType(
        Component.translatable("commands.test.reset.error.no_tests")
    );
    private static final SimpleCommandExceptionType TEST_INSTANCE_COULD_NOT_BE_FOUND = new SimpleCommandExceptionType(
        Component.translatable("commands.test.error.test_instance_not_found")
    );
    private static final SimpleCommandExceptionType NO_STRUCTURES_TO_EXPORT = new SimpleCommandExceptionType(
        Component.literal("Could not find any structures to export")
    );
    private static final SimpleCommandExceptionType NO_TEST_INSTANCES = new SimpleCommandExceptionType(
        Component.translatable("commands.test.error.no_test_instances")
    );
    private static final Dynamic3CommandExceptionType NO_TEST_CONTAINING = new Dynamic3CommandExceptionType(
        (p_396466_, p_396467_, p_396468_) -> Component.translatableEscape("commands.test.error.no_test_containing_pos", p_396466_, p_396467_, p_396468_)
    );
    private static final DynamicCommandExceptionType TOO_LARGE = new DynamicCommandExceptionType(
        p_399389_ -> Component.translatableEscape("commands.test.error.too_large", p_399389_)
    );

    private static int reset(TestFinder testFinder) throws CommandSyntaxException {
        stopTests();
        int i = toGameTestInfos(testFinder.source(), RetryOptions.noRetries(), testFinder)
            .map(p_396435_ -> resetGameTestInfo(testFinder.source(), p_396435_))
            .toList()
            .size();
        if (i == 0) {
            throw CLEAR_NO_TESTS.create();
        } else {
            testFinder.source().sendSuccess(() -> Component.translatable("commands.test.reset.success", i), true);
            return i;
        }
    }

    private static int clear(TestFinder testFinder) throws CommandSyntaxException {
        stopTests();
        CommandSourceStack commandsourcestack = testFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        List<TestInstanceBlockEntity> list = testFinder.findTestPos()
            .flatMap(p_404172_ -> serverlevel.getBlockEntity(p_404172_, BlockEntityType.TEST_INSTANCE_BLOCK).stream())
            .toList();

        for (TestInstanceBlockEntity testinstanceblockentity : list) {
            StructureUtils.clearSpaceForStructure(testinstanceblockentity.getStructureBoundingBox(), serverlevel);
            testinstanceblockentity.removeBarriers();
            serverlevel.destroyBlock(testinstanceblockentity.getBlockPos(), false);
        }

        if (list.isEmpty()) {
            throw CLEAR_NO_TESTS.create();
        } else {
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.clear.success", list.size()), true);
            return list.size();
        }
    }

    private static int export(TestFinder testFinder) throws CommandSyntaxException {
        CommandSourceStack commandsourcestack = testFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        int i = 0;
        boolean flag = true;

        for (Iterator<BlockPos> iterator = testFinder.findTestPos().iterator(); iterator.hasNext(); i++) {
            BlockPos blockpos = iterator.next();
            if (!(serverlevel.getBlockEntity(blockpos) instanceof TestInstanceBlockEntity testinstanceblockentity)) {
                throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
            }

            if (!testinstanceblockentity.exportTest(commandsourcestack::sendSystemMessage)) {
                flag = false;
            }
        }

        if (i == 0) {
            throw NO_STRUCTURES_TO_EXPORT.create();
        } else {
            String s = "Exported " + i + " structures";
            testFinder.source().sendSuccess(() -> Component.literal(s), true);
            return flag ? 0 : 1;
        }
    }

    private static int verify(TestFinder testFinder) {
        stopTests();
        CommandSourceStack commandsourcestack = testFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        BlockPos blockpos = createTestPositionAround(commandsourcestack);
        Collection<GameTestInfo> collection = Stream.concat(
                toGameTestInfos(commandsourcestack, RetryOptions.noRetries(), testFinder),
                toGameTestInfo(commandsourcestack, RetryOptions.noRetries(), testFinder, 0)
            )
            .toList();
        FailedTestTracker.forgetFailedTests();
        Collection<GameTestBatch> collection1 = new ArrayList<>();

        for (GameTestInfo gametestinfo : collection) {
            for (Rotation rotation : Rotation.values()) {
                Collection<GameTestInfo> collection2 = new ArrayList<>();

                for (int i = 0; i < 100; i++) {
                    GameTestInfo gametestinfo1 = new GameTestInfo(gametestinfo.getTestHolder(), rotation, serverlevel, new RetryOptions(1, true));
                    gametestinfo1.setTestBlockPos(gametestinfo.getTestBlockPos());
                    collection2.add(gametestinfo1);
                }

                GameTestBatch gametestbatch = GameTestBatchFactory.toGameTestBatch(collection2, gametestinfo.getTest().batch(), rotation.ordinal());
                collection1.add(gametestbatch);
            }
        }

        StructureGridSpawner structuregridspawner = new StructureGridSpawner(blockpos, 10, true);
        GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(collection1, serverlevel)
            .batcher(GameTestBatchFactory.fromGameTestInfo(100))
            .newStructureSpawner(structuregridspawner)
            .existingStructureSpawner(structuregridspawner)
            .haltOnError()
            .clearBetweenBatches()
            .build();
        return trackAndStartRunner(commandsourcestack, gametestrunner);
    }

    private static int run(TestFinder testFinder, RetryOptions retryOptions, int rotationSteps, int testsPerRow) {
        stopTests();
        CommandSourceStack commandsourcestack = testFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        BlockPos blockpos = createTestPositionAround(commandsourcestack);
        Collection<GameTestInfo> collection = Stream.concat(
                toGameTestInfos(commandsourcestack, retryOptions, testFinder), toGameTestInfo(commandsourcestack, retryOptions, testFinder, rotationSteps)
            )
            .toList();
        if (collection.isEmpty()) {
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.no_tests"), false);
            return 0;
        } else {
            FailedTestTracker.forgetFailedTests();
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.run.running", collection.size()), false);
            GameTestRunner gametestrunner = GameTestRunner.Builder.fromInfo(collection, serverlevel)
                .newStructureSpawner(new StructureGridSpawner(blockpos, testsPerRow, false))
                .build();
            return trackAndStartRunner(commandsourcestack, gametestrunner);
        }
    }

    private static int locate(TestFinder testFinder) throws CommandSyntaxException {
        testFinder.source().sendSystemMessage(Component.translatable("commands.test.locate.started"));
        MutableInt mutableint = new MutableInt(0);
        BlockPos blockpos = BlockPos.containing(testFinder.source().getPosition());
        testFinder.findTestPos()
            .forEach(
                p_396478_ -> {
                    if (testFinder.source().getLevel().getBlockEntity(p_396478_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
                        Direction direction = testinstanceblockentity.getRotation().rotate(Direction.NORTH);
                        BlockPos $$8 = testinstanceblockentity.getBlockPos().relative(direction, 2);
                        int $$9 = (int)direction.getOpposite().toYRot();
                        String $$10 = String.format(Locale.ROOT, "/tp @s %d %d %d %d 0", $$8.getX(), $$8.getY(), $$8.getZ(), $$9);
                        int $$11 = blockpos.getX() - p_396478_.getX();
                        int $$12 = blockpos.getZ() - p_396478_.getZ();
                        int $$13 = Mth.floor(Mth.sqrt($$11 * $$11 + $$12 * $$12));
                        MutableComponent $$14 = ComponentUtils.wrapInSquareBrackets(
                                Component.translatable("chat.coordinates", p_396478_.getX(), p_396478_.getY(), p_396478_.getZ())
                            )
                            .withStyle(
                                p_396438_ -> p_396438_.withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.SuggestCommand($$10))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                            );
                        testFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.found", $$14, $$13), false);
                        mutableint.increment();
                    }
                }
            );
        int i = mutableint.intValue();
        if (i == 0) {
            throw NO_TEST_INSTANCES.create();
        } else {
            testFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.done", i), true);
            return i;
        }
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
        InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter,
        Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> modifier
    ) {
        return argumentBuilder.executes(p_396451_ -> run(finderGetter.apply(p_396451_), RetryOptions.noRetries(), 0, 8))
            .then(
                Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                    .executes(
                        p_396459_ -> run(finderGetter.apply(p_396459_), new RetryOptions(IntegerArgumentType.getInteger(p_396459_, "numberOfTimes"), false), 0, 8)
                    )
                    .then(
                        modifier.apply(
                            Commands.argument("untilFailed", BoolArgumentType.bool())
                                .executes(
                                    p_396440_ -> run(
                                        finderGetter.apply(p_396440_),
                                        new RetryOptions(
                                            IntegerArgumentType.getInteger(p_396440_, "numberOfTimes"), BoolArgumentType.getBool(p_396440_, "untilFailed")
                                        ),
                                        0,
                                        8
                                    )
                                )
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter
    ) {
        return runWithRetryOptions(argumentBuilder, finderGetter, p_319485_ -> p_319485_);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> finderGetter
    ) {
        return runWithRetryOptions(
            argumentBuilder,
            finderGetter,
            p_319482_ -> p_319482_.then(
                Commands.argument("rotationSteps", IntegerArgumentType.integer())
                    .executes(
                        p_396457_ -> run(
                            finderGetter.apply(p_396457_),
                            new RetryOptions(IntegerArgumentType.getInteger(p_396457_, "numberOfTimes"), BoolArgumentType.getBool(p_396457_, "untilFailed")),
                            IntegerArgumentType.getInteger(p_396457_, "rotationSteps"),
                            8
                        )
                    )
                    .then(
                        Commands.argument("testsPerRow", IntegerArgumentType.integer())
                            .executes(
                                p_396448_ -> run(
                                    finderGetter.apply(p_396448_),
                                    new RetryOptions(
                                        IntegerArgumentType.getInteger(p_396448_, "numberOfTimes"), BoolArgumentType.getBool(p_396448_, "untilFailed")
                                    ),
                                    IntegerArgumentType.getInteger(p_396448_, "rotationSteps"),
                                    IntegerArgumentType.getInteger(p_396448_, "testsPerRow")
                                )
                            )
                    )
            )
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        ArgumentBuilder<CommandSourceStack, ?> argumentbuilder = runWithRetryOptionsAndBuildInfo(
            Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
            p_396444_ -> TestFinder.builder().failedTests(p_396444_, BoolArgumentType.getBool(p_396444_, "onlyRequiredTests"))
        );
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("test")
            .requires(Commands.hasPermission(2))
            .then(
                Commands.literal("run")
                    .then(
                        runWithRetryOptionsAndBuildInfo(
                            Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE)),
                            p_425189_ -> TestFinder.builder().byResourceSelection(p_425189_, ResourceSelectorArgument.getSelectedResources(p_425189_, "tests"))
                        )
                    )
            )
            .then(
                Commands.literal("runmultiple")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_425190_ -> run(
                                    TestFinder.builder().byResourceSelection(p_425190_, ResourceSelectorArgument.getSelectedResources(p_425190_, "tests")),
                                    RetryOptions.noRetries(),
                                    0,
                                    8
                                )
                            )
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(
                                        p_425191_ -> run(
                                            TestFinder.builder()
                                                .createMultipleCopies(IntegerArgumentType.getInteger(p_425191_, "amount"))
                                                .byResourceSelection(p_425191_, ResourceSelectorArgument.getSelectedResources(p_425191_, "tests")),
                                            RetryOptions.noRetries(),
                                            0,
                                            8
                                        )
                                    )
                            )
                    )
            )
            .then(runWithRetryOptions(Commands.literal("runthese"), TestFinder.builder()::allNearby))
            .then(runWithRetryOptions(Commands.literal("runclosest"), TestFinder.builder()::nearest))
            .then(runWithRetryOptions(Commands.literal("runthat"), TestFinder.builder()::lookedAt))
            .then(runWithRetryOptionsAndBuildInfo(Commands.literal("runfailed").then(argumentbuilder), TestFinder.builder()::failedTests))
            .then(
                Commands.literal("verify")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_425188_ -> verify(
                                    TestFinder.builder().byResourceSelection(p_425188_, ResourceSelectorArgument.getSelectedResources(p_425188_, "tests"))
                                )
                            )
                    )
            )
            .then(
                Commands.literal("locate")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(buildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_425187_ -> locate(
                                    TestFinder.builder().byResourceSelection(p_425187_, ResourceSelectorArgument.getSelectedResources(p_425187_, "tests"))
                                )
                            )
                    )
            )
            .then(Commands.literal("resetclosest").executes(p_396461_ -> reset(TestFinder.builder().nearest(p_396461_))))
            .then(Commands.literal("resetthese").executes(p_396453_ -> reset(TestFinder.builder().allNearby(p_396453_))))
            .then(Commands.literal("resetthat").executes(p_396462_ -> reset(TestFinder.builder().lookedAt(p_396462_))))
            .then(Commands.literal("clearthat").executes(p_396455_ -> clear(TestFinder.builder().lookedAt(p_396455_))))
            .then(Commands.literal("clearthese").executes(p_396442_ -> clear(TestFinder.builder().allNearby(p_396442_))))
            .then(
                Commands.literal("clearall")
                    .executes(p_396431_ -> clear(TestFinder.builder().radius(p_396431_, 250)))
                    .then(
                        Commands.argument("radius", IntegerArgumentType.integer())
                            .executes(
                                p_396425_ -> clear(
                                    TestFinder.builder().radius(p_396425_, Mth.clamp(IntegerArgumentType.getInteger(p_396425_, "radius"), 0, 1024))
                                )
                            )
                    )
            )
            .then(Commands.literal("stop").executes(p_319497_ -> stopTests()))
            .then(
                Commands.literal("pos")
                    .executes(p_128023_ -> showPos(p_128023_.getSource(), "pos"))
                    .then(
                        Commands.argument("var", StringArgumentType.word())
                            .executes(p_128021_ -> showPos(p_128021_.getSource(), StringArgumentType.getString(p_128021_, "var")))
                    )
            )
            .then(
                Commands.literal("create")
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .suggests(TestCommand::suggestTestFunction)
                            .executes(p_396424_ -> createNewStructure(p_396424_.getSource(), ResourceLocationArgument.getId(p_396424_, "id"), 5, 5, 5))
                            .then(
                                Commands.argument("width", IntegerArgumentType.integer())
                                    .executes(
                                        p_396469_ -> createNewStructure(
                                            p_396469_.getSource(),
                                            ResourceLocationArgument.getId(p_396469_, "id"),
                                            IntegerArgumentType.getInteger(p_396469_, "width"),
                                            IntegerArgumentType.getInteger(p_396469_, "width"),
                                            IntegerArgumentType.getInteger(p_396469_, "width")
                                        )
                                    )
                                    .then(
                                        Commands.argument("height", IntegerArgumentType.integer())
                                            .then(
                                                Commands.argument("depth", IntegerArgumentType.integer())
                                                    .executes(
                                                        p_396465_ -> createNewStructure(
                                                            p_396465_.getSource(),
                                                            ResourceLocationArgument.getId(p_396465_, "id"),
                                                            IntegerArgumentType.getInteger(p_396465_, "width"),
                                                            IntegerArgumentType.getInteger(p_396465_, "height"),
                                                            IntegerArgumentType.getInteger(p_396465_, "depth")
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
            );
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            literalargumentbuilder = literalargumentbuilder.then(
                    Commands.literal("export")
                        .then(
                            Commands.argument("test", ResourceArgument.resource(buildContext, Registries.TEST_INSTANCE))
                                .executes(
                                    p_396460_ -> exportTestStructure(
                                        p_396460_.getSource(), ResourceArgument.getResource(p_396460_, "test", Registries.TEST_INSTANCE)
                                    )
                                )
                        )
                )
                .then(Commands.literal("exportclosest").executes(p_396473_ -> export(TestFinder.builder().nearest(p_396473_))))
                .then(Commands.literal("exportthese").executes(p_396433_ -> export(TestFinder.builder().allNearby(p_396433_))))
                .then(Commands.literal("exportthat").executes(p_396423_ -> export(TestFinder.builder().lookedAt(p_396423_))));
        }

        dispatcher.register(literalargumentbuilder);
    }

    public static CompletableFuture<Suggestions> suggestTestFunction(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Stream<String> stream = context.getSource().registryAccess().lookupOrThrow(Registries.TEST_FUNCTION).listElements().map(Holder::getRegisteredName);
        return SharedSuggestionProvider.suggest(stream, builder);
    }

    private static int resetGameTestInfo(CommandSourceStack source, GameTestInfo testInfo) {
        TestInstanceBlockEntity testinstanceblockentity = testInfo.getTestInstanceBlockEntity();
        testinstanceblockentity.resetTest(source::sendSystemMessage);
        return 1;
    }

    private static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack source, RetryOptions retryOptions, TestPosFinder posFinder) {
        return posFinder.findTestPos().map(p_396472_ -> createGameTestInfo(p_396472_, source, retryOptions)).flatMap(Optional::stream);
    }

    private static Stream<GameTestInfo> toGameTestInfo(CommandSourceStack source, RetryOptions retryOptions, TestInstanceFinder finder, int rotationSteps) {
        return finder.findTests()
            .filter(p_396427_ -> verifyStructureExists(source, p_396427_.value().structure()))
            .map(
                p_396422_ -> new GameTestInfo(
                    (Holder.Reference<GameTestInstance>)p_396422_, StructureUtils.getRotationForRotationSteps(rotationSteps), source.getLevel(), retryOptions
                )
            );
    }

    private static Optional<GameTestInfo> createGameTestInfo(BlockPos pos, CommandSourceStack source, RetryOptions retryOptions) {
        ServerLevel serverlevel = source.getLevel();
        if (serverlevel.getBlockEntity(pos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            Optional<Holder.Reference<GameTestInstance>> optional = testinstanceblockentity.test()
                .flatMap(source.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)::get);
            if (optional.isEmpty()) {
                source.sendFailure(Component.translatable("commands.test.error.non_existant_test", testinstanceblockentity.getTestName()));
                return Optional.empty();
            } else {
                Holder.Reference<GameTestInstance> reference = optional.get();
                GameTestInfo gametestinfo = new GameTestInfo(reference, testinstanceblockentity.getRotation(), serverlevel, retryOptions);
                gametestinfo.setTestBlockPos(pos);
                return !verifyStructureExists(source, gametestinfo.getStructure()) ? Optional.empty() : Optional.of(gametestinfo);
            }
        } else {
            source.sendFailure(
                Component.translatable("commands.test.error.test_instance_not_found.position", pos.getX(), pos.getY(), pos.getZ())
            );
            return Optional.empty();
        }
    }

    private static int createNewStructure(CommandSourceStack source, ResourceLocation id, int width, int height, int depth) throws CommandSyntaxException {
        if (width <= 48 && height <= 48 && depth <= 48) {
            ServerLevel serverlevel = source.getLevel();
            BlockPos blockpos = createTestPositionAround(source);
            TestInstanceBlockEntity testinstanceblockentity = StructureUtils.createNewEmptyTest(
                id, blockpos, new Vec3i(width, height, depth), Rotation.NONE, serverlevel
            );
            BlockPos blockpos1 = testinstanceblockentity.getStructurePos();
            BlockPos blockpos2 = blockpos1.offset(width - 1, 0, depth - 1);
            BlockPos.betweenClosedStream(blockpos1, blockpos2)
                .forEach(p_414995_ -> serverlevel.setBlockAndUpdate(p_414995_, Blocks.BEDROCK.defaultBlockState()));
            source.sendSuccess(() -> Component.translatable("commands.test.create.success", testinstanceblockentity.getTestName()), true);
            return 1;
        } else {
            throw TOO_LARGE.create(48);
        }
    }

    private static int showPos(CommandSourceStack source, String variableName) throws CommandSyntaxException {
        ServerPlayer serverplayer = source.getPlayerOrException();
        BlockHitResult blockhitresult = (BlockHitResult)serverplayer.pick(10.0, 1.0F, false);
        BlockPos blockpos = blockhitresult.getBlockPos();
        ServerLevel serverlevel = source.getLevel();
        Optional<BlockPos> optional = StructureUtils.findTestContainingPos(blockpos, 15, serverlevel);
        if (optional.isEmpty()) {
            optional = StructureUtils.findTestContainingPos(blockpos, 250, serverlevel);
        }

        if (optional.isEmpty()) {
            throw NO_TEST_CONTAINING.create(blockpos.getX(), blockpos.getY(), blockpos.getZ());
        } else if (serverlevel.getBlockEntity(optional.get()) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            BlockPos blockpos2 = testinstanceblockentity.getStructurePos();
            BlockPos blockpos1 = blockpos.subtract(blockpos2);
            String $$11 = blockpos1.getX() + ", " + blockpos1.getY() + ", " + blockpos1.getZ();
            String $$12 = testinstanceblockentity.getTestName().getString();
            MutableComponent $$13 = Component.translatable("commands.test.coordinates", blockpos1.getX(), blockpos1.getY(), blockpos1.getZ())
                .setStyle(
                    Style.EMPTY
                        .withBold(true)
                        .withColor(ChatFormatting.GREEN)
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.test.coordinates.copy")))
                        .withClickEvent(new ClickEvent.CopyToClipboard("final BlockPos " + variableName + " = new BlockPos(" + $$11 + ");"))
                );
            source.sendSuccess(() -> Component.translatable("commands.test.relative_position", $$12, $$13), false);
            serverplayer.connection.send(new ClientboundGameTestHighlightPosPacket(blockpos, blockpos1));
            return 1;
        } else {
            throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
        }
    }

    private static int stopTests() {
        GameTestTicker.SINGLETON.clear();
        return 1;
    }

    public static int trackAndStartRunner(CommandSourceStack source, GameTestRunner testRunner) {
        testRunner.addListener(new TestCommand.TestBatchSummaryDisplayer(source));
        MultipleTestTracker multipletesttracker = new MultipleTestTracker(testRunner.getTestInfos());
        multipletesttracker.addListener(new TestCommand.TestSummaryDisplayer(source, multipletesttracker));
        multipletesttracker.addFailureListener(p_396449_ -> FailedTestTracker.rememberFailedTest(p_396449_.getTestHolder()));
        testRunner.start();
        return 1;
    }

    private static int exportTestStructure(CommandSourceStack source, Holder<GameTestInstance> testInstance) {
        return !TestInstanceBlockEntity.export(source.getLevel(), testInstance.value().structure(), source::sendSystemMessage) ? 0 : 1;
    }

    private static boolean verifyStructureExists(CommandSourceStack source, ResourceLocation structure) {
        if (source.getLevel().getStructureManager().get(structure).isEmpty()) {
            source.sendFailure(Component.translatable("commands.test.error.structure_not_found", Component.translationArg(structure)));
            return false;
        } else {
            return true;
        }
    }

    private static BlockPos createTestPositionAround(CommandSourceStack source) {
        BlockPos blockpos = BlockPos.containing(source.getPosition());
        int i = source.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockpos).getY();
        return new BlockPos(blockpos.getX(), i, blockpos.getZ() + 3);
    }

    record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
        @Override
        public void testBatchStarting(GameTestBatch p_319827_) {
            this.source
                .sendSuccess(() -> Component.translatable("commands.test.batch.starting", p_319827_.environment().getRegisteredName(), p_319827_.index()), true);
        }

        @Override
        public void testBatchFinished(GameTestBatch p_320779_) {
        }
    }

    public record TestSummaryDisplayer(CommandSourceStack source, MultipleTestTracker tracker) implements GameTestListener {
        @Override
        public void testStructureLoaded(GameTestInfo p_128064_) {
        }

        @Override
        public void testPassed(GameTestInfo p_177797_, GameTestRunner p_320726_) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testFailed(GameTestInfo p_128066_, GameTestRunner p_320567_) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testAddedForRerun(GameTestInfo p_319856_, GameTestInfo p_320528_, GameTestRunner p_319832_) {
            this.tracker.addTestToTrack(p_320528_);
        }

        private void showTestSummaryIfAllDone() {
            if (this.tracker.isDone()) {
                this.source
                    .sendSuccess(() -> Component.translatable("commands.test.summary", this.tracker.getTotalCount()).withStyle(ChatFormatting.WHITE), true);
                if (this.tracker.hasFailedRequired()) {
                    this.source.sendFailure(Component.translatable("commands.test.summary.failed", this.tracker.getFailedRequiredCount()));
                } else {
                    this.source.sendSuccess(() -> Component.translatable("commands.test.summary.all_required_passed").withStyle(ChatFormatting.GREEN), true);
                }

                if (this.tracker.hasFailedOptional()) {
                    this.source.sendSystemMessage(Component.translatable("commands.test.summary.optional_failed", this.tracker.getFailedOptionalCount()));
                }
            }
        }
    }
}
