package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public class CloneCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (p_304194_, p_304195_) -> Component.translatableEscape("commands.clone.toobig", p_304194_, p_304195_)
    );
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
    public static final Predicate<BlockInWorld> FILTER_AIR = p_359534_ -> !p_359534_.getState().isAir();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("clone")
                .requires(Commands.hasPermission(2))
                .then(beginEndDestinationAndModeSuffix(context, p_264757_ -> p_264757_.getSource().getLevel()))
                .then(
                    Commands.literal("from")
                        .then(
                            Commands.argument("sourceDimension", DimensionArgument.dimension())
                                .then(beginEndDestinationAndModeSuffix(context, p_264743_ -> DimensionArgument.getDimension(p_264743_, "sourceDimension")))
                        )
                )
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
        CommandBuildContext buildContext, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> levelGetter
    ) {
        return Commands.argument("begin", BlockPosArgument.blockPos())
            .then(
                Commands.argument("end", BlockPosArgument.blockPos())
                    .then(destinationAndStrictSuffix(buildContext, levelGetter, p_264751_ -> p_264751_.getSource().getLevel()))
                    .then(
                        Commands.literal("to")
                            .then(
                                Commands.argument("targetDimension", DimensionArgument.dimension())
                                    .then(
                                        destinationAndStrictSuffix(
                                            buildContext, levelGetter, p_264756_ -> DimensionArgument.getDimension(p_264756_, "targetDimension")
                                        )
                                    )
                            )
                    )
            );
    }

    private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(
        CommandContext<CommandSourceStack> context, ServerLevel level, String name
    ) throws CommandSyntaxException {
        BlockPos blockpos = BlockPosArgument.getLoadedBlockPos(context, level, name);
        return new CloneCommands.DimensionAndPosition(level, blockpos);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(
        CommandBuildContext buildContext,
        InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> sourceLevelGetter,
        InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> destinationLevelGetter
    ) {
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction = p_396524_ -> getLoadedDimensionAndPosition(
            p_396524_, sourceLevelGetter.apply(p_396524_), "begin"
        );
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction1 = p_396503_ -> getLoadedDimensionAndPosition(
            p_396503_, sourceLevelGetter.apply(p_396503_), "end"
        );
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction2 = p_396505_ -> getLoadedDimensionAndPosition(
            p_396505_, destinationLevelGetter.apply(p_396505_), "destination"
        );
        return modeSuffix(
                buildContext, incommandfunction, incommandfunction1, incommandfunction2, false, Commands.argument("destination", BlockPosArgument.blockPos())
            )
            .then(modeSuffix(buildContext, incommandfunction, incommandfunction1, incommandfunction2, true, Commands.literal("strict")));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(
        CommandBuildContext buildContext,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> begin,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> end,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destination,
        boolean strict,
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder
    ) {
        return argumentBuilder.executes(
                p_396510_ -> clone(
                    p_396510_.getSource(),
                    begin.apply(p_396510_),
                    end.apply(p_396510_),
                    destination.apply(p_396510_),
                    p_180041_ -> true,
                    CloneCommands.Mode.NORMAL,
                    strict
                )
            )
            .then(wrapWithCloneMode(begin, end, destination, p_264738_ -> p_180033_ -> true, strict, Commands.literal("replace")))
            .then(wrapWithCloneMode(begin, end, destination, p_264744_ -> FILTER_AIR, strict, Commands.literal("masked")))
            .then(
                Commands.literal("filtered")
                    .then(
                        wrapWithCloneMode(
                            begin,
                            end,
                            destination,
                            p_264745_ -> BlockPredicateArgument.getBlockPredicate(p_264745_, "filter"),
                            strict,
                            Commands.argument("filter", BlockPredicateArgument.blockPredicate(buildContext))
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> begin,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> end,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> destination,
        InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter,
        boolean strict,
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder
    ) {
        return argumentBuilder.executes(
                p_396516_ -> clone(
                    p_396516_.getSource(),
                    begin.apply(p_396516_),
                    end.apply(p_396516_),
                    destination.apply(p_396516_),
                    filter.apply(p_396516_),
                    CloneCommands.Mode.NORMAL,
                    strict
                )
            )
            .then(
                Commands.literal("force")
                    .executes(
                        p_396530_ -> clone(
                            p_396530_.getSource(),
                            begin.apply(p_396530_),
                            end.apply(p_396530_),
                            destination.apply(p_396530_),
                            filter.apply(p_396530_),
                            CloneCommands.Mode.FORCE,
                            strict
                        )
                    )
            )
            .then(
                Commands.literal("move")
                    .executes(
                        p_396522_ -> clone(
                            p_396522_.getSource(),
                            begin.apply(p_396522_),
                            end.apply(p_396522_),
                            destination.apply(p_396522_),
                            filter.apply(p_396522_),
                            CloneCommands.Mode.MOVE,
                            strict
                        )
                    )
            )
            .then(
                Commands.literal("normal")
                    .executes(
                        p_396501_ -> clone(
                            p_396501_.getSource(),
                            begin.apply(p_396501_),
                            end.apply(p_396501_),
                            destination.apply(p_396501_),
                            filter.apply(p_396501_),
                            CloneCommands.Mode.NORMAL,
                            strict
                        )
                    )
            );
    }

    private static int clone(
        CommandSourceStack source,
        CloneCommands.DimensionAndPosition begin,
        CloneCommands.DimensionAndPosition end,
        CloneCommands.DimensionAndPosition destination,
        Predicate<BlockInWorld> filter,
        CloneCommands.Mode mode,
        boolean strict
    ) throws CommandSyntaxException {
        BlockPos blockpos = begin.position();
        BlockPos blockpos1 = end.position();
        BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
        BlockPos blockpos2 = destination.position();
        BlockPos blockpos3 = blockpos2.offset(boundingbox.getLength());
        BoundingBox boundingbox1 = BoundingBox.fromCorners(blockpos2, blockpos3);
        ServerLevel serverlevel = begin.dimension();
        ServerLevel serverlevel1 = destination.dimension();
        if (!mode.canOverlap() && serverlevel == serverlevel1 && boundingbox1.intersects(boundingbox)) {
            throw ERROR_OVERLAP.create();
        } else {
            int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
            int j = source.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
            if (i > j) {
                throw ERROR_AREA_TOO_LARGE.create(j, i);
            } else if (!serverlevel.hasChunksAt(blockpos, blockpos1) || !serverlevel1.hasChunksAt(blockpos2, blockpos3)) {
                throw BlockPosArgument.ERROR_NOT_LOADED.create();
            } else if (serverlevel1.isDebug()) {
                throw ERROR_FAILED.create();
            } else {
                List<CloneCommands.CloneBlockInfo> list = Lists.newArrayList();
                List<CloneCommands.CloneBlockInfo> list1 = Lists.newArrayList();
                List<CloneCommands.CloneBlockInfo> list2 = Lists.newArrayList();
                Deque<BlockPos> deque = Lists.newLinkedList();
                int k = 0;
                ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER);

                try {
                    BlockPos blockpos4 = new BlockPos(
                        boundingbox1.minX() - boundingbox.minX(), boundingbox1.minY() - boundingbox.minY(), boundingbox1.minZ() - boundingbox.minZ()
                    );

                    for (int l = boundingbox.minZ(); l <= boundingbox.maxZ(); l++) {
                        for (int i1 = boundingbox.minY(); i1 <= boundingbox.maxY(); i1++) {
                            for (int j1 = boundingbox.minX(); j1 <= boundingbox.maxX(); j1++) {
                                BlockPos blockpos5 = new BlockPos(j1, i1, l);
                                BlockPos blockpos6 = blockpos5.offset(blockpos4);
                                BlockInWorld blockinworld = new BlockInWorld(serverlevel, blockpos5, false);
                                BlockState blockstate = blockinworld.getState();
                                if (filter.test(blockinworld)) {
                                    BlockEntity blockentity = serverlevel.getBlockEntity(blockpos5);
                                    if (blockentity != null) {
                                        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(
                                            problemreporter$scopedcollector.forChild(blockentity.problemPath()), source.registryAccess()
                                        );
                                        blockentity.saveCustomOnly(tagvalueoutput);
                                        CloneCommands.CloneBlockEntityInfo clonecommands$cloneblockentityinfo = new CloneCommands.CloneBlockEntityInfo(
                                            tagvalueoutput.buildResult(), blockentity.components()
                                        );
                                        list1.add(
                                            new CloneCommands.CloneBlockInfo(
                                                blockpos6, blockstate, clonecommands$cloneblockentityinfo, serverlevel1.getBlockState(blockpos6)
                                            )
                                        );
                                        deque.addLast(blockpos5);
                                    } else if (!blockstate.isSolidRender() && !blockstate.isCollisionShapeFullBlock(serverlevel, blockpos5)) {
                                        list2.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null, serverlevel1.getBlockState(blockpos6)));
                                        deque.addFirst(blockpos5);
                                    } else {
                                        list.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null, serverlevel1.getBlockState(blockpos6)));
                                        deque.addLast(blockpos5);
                                    }
                                }
                            }
                        }
                    }

                    int l1 = 2 | (strict ? 816 : 0);
                    if (mode == CloneCommands.Mode.MOVE) {
                        for (BlockPos blockpos7 : deque) {
                            serverlevel.setBlock(blockpos7, Blocks.BARRIER.defaultBlockState(), l1 | 816);
                        }

                        int i2 = strict ? l1 : 3;

                        for (BlockPos blockpos8 : deque) {
                            serverlevel.setBlock(blockpos8, Blocks.AIR.defaultBlockState(), i2);
                        }
                    }

                    List<CloneCommands.CloneBlockInfo> list3 = Lists.newArrayList();
                    list3.addAll(list);
                    list3.addAll(list1);
                    list3.addAll(list2);
                    List<CloneCommands.CloneBlockInfo> list4 = Lists.reverse(list3);

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo : list4) {
                        serverlevel1.setBlock(clonecommands$cloneblockinfo.pos, Blocks.BARRIER.defaultBlockState(), l1 | 816);
                    }

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo1 : list3) {
                        if (serverlevel1.setBlock(clonecommands$cloneblockinfo1.pos, clonecommands$cloneblockinfo1.state, l1)) {
                            k++;
                        }
                    }

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo2 : list1) {
                        BlockEntity blockentity1 = serverlevel1.getBlockEntity(clonecommands$cloneblockinfo2.pos);
                        if (clonecommands$cloneblockinfo2.blockEntityInfo != null && blockentity1 != null) {
                            blockentity1.loadCustomOnly(
                                TagValueInput.create(
                                    problemreporter$scopedcollector.forChild(blockentity1.problemPath()),
                                    serverlevel1.registryAccess(),
                                    clonecommands$cloneblockinfo2.blockEntityInfo.tag
                                )
                            );
                            blockentity1.setComponents(clonecommands$cloneblockinfo2.blockEntityInfo.components);
                            blockentity1.setChanged();
                        }

                        serverlevel1.setBlock(clonecommands$cloneblockinfo2.pos, clonecommands$cloneblockinfo2.state, l1);
                    }

                    if (!strict) {
                        for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo3 : list4) {
                            serverlevel1.updateNeighboursOnBlockSet(clonecommands$cloneblockinfo3.pos, clonecommands$cloneblockinfo3.previousStateAtDestination);
                        }
                    }

                    serverlevel1.getBlockTicks().copyAreaFrom(serverlevel.getBlockTicks(), boundingbox, blockpos4);
                } catch (Throwable throwable1) {
                    try {
                        problemreporter$scopedcollector.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }

                    throw throwable1;
                }

                problemreporter$scopedcollector.close();
                if (k == 0) {
                    throw ERROR_FAILED.create();
                } else {
                    int k1 = k;
                    source.sendSuccess(() -> Component.translatable("commands.clone.success", k1), true);
                    return k;
                }
            }
        }
    }

    record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
    }

    record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo, BlockState previousStateAtDestination) {
    }

    record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
    }

    static enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean canOverlap;

        private Mode(boolean canOverlap) {
            this.canOverlap = canOverlap;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }
}
