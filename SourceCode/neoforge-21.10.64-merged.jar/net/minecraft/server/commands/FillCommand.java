package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FillCommand {
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (p_304218_, p_304219_) -> Component.translatableEscape("commands.fill.toobig", p_304218_, p_304219_)
    );
    static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
            Commands.literal("fill")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("from", BlockPosArgument.blockPos())
                        .then(
                            Commands.argument("to", BlockPosArgument.blockPos())
                                .then(
                                    wrapWithMode(
                                            buildContext,
                                            Commands.argument("block", BlockStateArgument.block(buildContext)),
                                            p_392711_ -> BlockPosArgument.getLoadedBlockPos(p_392711_, "from"),
                                            p_392705_ -> BlockPosArgument.getLoadedBlockPos(p_392705_, "to"),
                                            p_392692_ -> BlockStateArgument.getBlock(p_392692_, "block"),
                                            p_392685_ -> null
                                        )
                                        .then(
                                            Commands.literal("replace")
                                                .executes(
                                                    p_392698_ -> fillBlocks(
                                                        p_392698_.getSource(),
                                                        BoundingBox.fromCorners(
                                                            BlockPosArgument.getLoadedBlockPos(p_392698_, "from"),
                                                            BlockPosArgument.getLoadedBlockPos(p_392698_, "to")
                                                        ),
                                                        BlockStateArgument.getBlock(p_392698_, "block"),
                                                        FillCommand.Mode.REPLACE,
                                                        null,
                                                        false
                                                    )
                                                )
                                                .then(
                                                    wrapWithMode(
                                                        buildContext,
                                                        Commands.argument("filter", BlockPredicateArgument.blockPredicate(buildContext)),
                                                        p_392704_ -> BlockPosArgument.getLoadedBlockPos(p_392704_, "from"),
                                                        p_392717_ -> BlockPosArgument.getLoadedBlockPos(p_392717_, "to"),
                                                        p_392718_ -> BlockStateArgument.getBlock(p_392718_, "block"),
                                                        p_392684_ -> BlockPredicateArgument.getBlockPredicate(p_392684_, "filter")
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("keep")
                                                .executes(
                                                    p_392691_ -> fillBlocks(
                                                        p_392691_.getSource(),
                                                        BoundingBox.fromCorners(
                                                            BlockPosArgument.getLoadedBlockPos(p_392691_, "from"),
                                                            BlockPosArgument.getLoadedBlockPos(p_392691_, "to")
                                                        ),
                                                        BlockStateArgument.getBlock(p_392691_, "block"),
                                                        FillCommand.Mode.REPLACE,
                                                        p_180225_ -> p_180225_.getLevel().isEmptyBlock(p_180225_.getPos()),
                                                        false
                                                    )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithMode(
        CommandBuildContext buildContext,
        ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> from,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> to,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> block,
        FillCommand.NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> filter
    ) {
        return argumentBuilder.executes(
                p_396538_ -> fillBlocks(
                    p_396538_.getSource(),
                    BoundingBox.fromCorners(from.apply(p_396538_), to.apply(p_396538_)),
                    block.apply(p_396538_),
                    FillCommand.Mode.REPLACE,
                    filter.apply(p_396538_),
                    false
                )
            )
            .then(
                Commands.literal("outline")
                    .executes(
                        p_396553_ -> fillBlocks(
                            p_396553_.getSource(),
                            BoundingBox.fromCorners(from.apply(p_396553_), to.apply(p_396553_)),
                            block.apply(p_396553_),
                            FillCommand.Mode.OUTLINE,
                            filter.apply(p_396553_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("hollow")
                    .executes(
                        p_396548_ -> fillBlocks(
                            p_396548_.getSource(),
                            BoundingBox.fromCorners(from.apply(p_396548_), to.apply(p_396548_)),
                            block.apply(p_396548_),
                            FillCommand.Mode.HOLLOW,
                            filter.apply(p_396548_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("destroy")
                    .executes(
                        p_396543_ -> fillBlocks(
                            p_396543_.getSource(),
                            BoundingBox.fromCorners(from.apply(p_396543_), to.apply(p_396543_)),
                            block.apply(p_396543_),
                            FillCommand.Mode.DESTROY,
                            filter.apply(p_396543_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("strict")
                    .executes(
                        p_396558_ -> fillBlocks(
                            p_396558_.getSource(),
                            BoundingBox.fromCorners(from.apply(p_396558_), to.apply(p_396558_)),
                            block.apply(p_396558_),
                            FillCommand.Mode.REPLACE,
                            filter.apply(p_396558_),
                            true
                        )
                    )
            );
    }

    private static int fillBlocks(
        CommandSourceStack source,
        BoundingBox box,
        BlockInput block,
        FillCommand.Mode mode,
        @Nullable Predicate<BlockInWorld> filter,
        boolean strict
    ) throws CommandSyntaxException {
        int i = box.getXSpan() * box.getYSpan() * box.getZSpan();
        int j = source.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
        if (i > j) {
            throw ERROR_AREA_TOO_LARGE.create(j, i);
        } else {
            record UpdatedPosition(BlockPos pos, BlockState oldState) {
            }

            List<UpdatedPosition> list = Lists.newArrayList();
            ServerLevel serverlevel = source.getLevel();
            if (serverlevel.isDebug()) {
                throw ERROR_FAILED.create();
            } else {
                int k = 0;

                for (BlockPos blockpos : BlockPos.betweenClosed(
                    box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()
                )) {
                    if (filter == null || filter.test(new BlockInWorld(serverlevel, blockpos, true))) {
                        BlockState blockstate = serverlevel.getBlockState(blockpos);
                        boolean flag = false;
                        if (mode.affector.affect(serverlevel, blockpos)) {
                            flag = true;
                        }

                        BlockInput blockinput = mode.filter.filter(box, blockpos, block, serverlevel);
                        if (blockinput == null) {
                            if (flag) {
                                k++;
                            }
                        } else if (!blockinput.place(serverlevel, blockpos, 2 | (strict ? 816 : 256))) {
                            if (flag) {
                                k++;
                            }
                        } else {
                            if (!strict) {
                                list.add(new UpdatedPosition(blockpos.immutable(), blockstate));
                            }

                            k++;
                        }
                    }
                }

                for (UpdatedPosition fillcommand$1updatedposition : list) {
                    serverlevel.updateNeighboursOnBlockSet(fillcommand$1updatedposition.pos, fillcommand$1updatedposition.oldState);
                }

                if (k == 0) {
                    throw ERROR_FAILED.create();
                } else {
                    int l = k;
                    source.sendSuccess(() -> Component.translatable("commands.fill.success", l), true);
                    return k;
                }
            }
        }
    }

    @FunctionalInterface
    public interface Affector {
        FillCommand.Affector NOOP = (p_393846_, p_393551_) -> false;

        boolean affect(ServerLevel level, BlockPos pos);
    }

    @FunctionalInterface
    public interface Filter {
        FillCommand.Filter NOOP = (p_394191_, p_393566_, p_394115_, p_394030_) -> p_394115_;

        @Nullable
        BlockInput filter(BoundingBox box, BlockPos pos, BlockInput block, ServerLevel level);
    }

    static enum Mode {
        REPLACE(FillCommand.Affector.NOOP, FillCommand.Filter.NOOP),
        OUTLINE(
            FillCommand.Affector.NOOP,
            (p_137428_, p_137429_, p_137430_, p_137431_) -> p_137429_.getX() != p_137428_.minX()
                    && p_137429_.getX() != p_137428_.maxX()
                    && p_137429_.getY() != p_137428_.minY()
                    && p_137429_.getY() != p_137428_.maxY()
                    && p_137429_.getZ() != p_137428_.minZ()
                    && p_137429_.getZ() != p_137428_.maxZ()
                ? null
                : p_137430_
        ),
        HOLLOW(
            FillCommand.Affector.NOOP,
            (p_137423_, p_137424_, p_137425_, p_137426_) -> p_137424_.getX() != p_137423_.minX()
                    && p_137424_.getX() != p_137423_.maxX()
                    && p_137424_.getY() != p_137423_.minY()
                    && p_137424_.getY() != p_137423_.maxY()
                    && p_137424_.getZ() != p_137423_.minZ()
                    && p_137424_.getZ() != p_137423_.maxZ()
                ? FillCommand.HOLLOW_CORE
                : p_137425_
        ),
        DESTROY((p_392719_, p_392720_) -> p_392719_.destroyBlock(p_392720_, true), FillCommand.Filter.NOOP);

        public final FillCommand.Filter filter;
        public final FillCommand.Affector affector;

        private Mode(FillCommand.Affector affector, FillCommand.Filter filter) {
            this.affector = affector;
            this.filter = filter;
        }
    }

    @FunctionalInterface
    interface NullableCommandFunction<T, R> {
        @Nullable
        R apply(T context) throws CommandSyntaxException;
    }
}
