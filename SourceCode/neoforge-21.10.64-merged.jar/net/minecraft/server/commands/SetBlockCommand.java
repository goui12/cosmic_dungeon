package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class SetBlockCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        Predicate<BlockInWorld> predicate = p_180517_ -> p_180517_.getLevel().isEmptyBlock(p_180517_.getPos());
        dispatcher.register(
            Commands.literal("setblock")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(
                            Commands.argument("block", BlockStateArgument.block(buildContext))
                                .executes(
                                    p_392756_ -> setBlock(
                                        p_392756_.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(p_392756_, "pos"),
                                        BlockStateArgument.getBlock(p_392756_, "block"),
                                        SetBlockCommand.Mode.REPLACE,
                                        null,
                                        false
                                    )
                                )
                                .then(
                                    Commands.literal("destroy")
                                        .executes(
                                            p_392755_ -> setBlock(
                                                p_392755_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_392755_, "pos"),
                                                BlockStateArgument.getBlock(p_392755_, "block"),
                                                SetBlockCommand.Mode.DESTROY,
                                                null,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("keep")
                                        .executes(
                                            p_392758_ -> setBlock(
                                                p_392758_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_392758_, "pos"),
                                                BlockStateArgument.getBlock(p_392758_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                predicate,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("replace")
                                        .executes(
                                            p_392760_ -> setBlock(
                                                p_392760_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_392760_, "pos"),
                                                BlockStateArgument.getBlock(p_392760_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                null,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("strict")
                                        .executes(
                                            p_392759_ -> setBlock(
                                                p_392759_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_392759_, "pos"),
                                                BlockStateArgument.getBlock(p_392759_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                null,
                                                true
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int setBlock(
        CommandSourceStack source,
        BlockPos pos,
        BlockInput block,
        SetBlockCommand.Mode mode,
        @Nullable Predicate<BlockInWorld> filter,
        boolean strict
    ) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        if (serverlevel.isDebug()) {
            throw ERROR_FAILED.create();
        } else if (filter != null && !filter.test(new BlockInWorld(serverlevel, pos, true))) {
            throw ERROR_FAILED.create();
        } else {
            boolean flag;
            if (mode == SetBlockCommand.Mode.DESTROY) {
                serverlevel.destroyBlock(pos, true);
                flag = !block.getState().isAir() || !serverlevel.getBlockState(pos).isAir();
            } else {
                flag = true;
            }

            BlockState blockstate = serverlevel.getBlockState(pos);
            if (flag && !block.place(serverlevel, pos, 2 | (strict ? 816 : 256))) {
                throw ERROR_FAILED.create();
            } else {
                if (!strict) {
                    serverlevel.updateNeighboursOnBlockSet(pos, blockstate);
                }

                source.sendSuccess(() -> Component.translatable("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ()), true);
                return 1;
            }
        }
    }

    public static enum Mode {
        REPLACE,
        DESTROY;
    }
}
