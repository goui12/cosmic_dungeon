package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.door.DoorPassageData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class DoorCountCommand {
    private DoorCountCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("door")
                        .then(Commands.literal("count")
                                .requires(src -> src.hasPermission(0)) // anyone can use
                                .executes(ctx -> {
                                    final CommandSourceStack src = ctx.getSource();
                                    final ServerPlayer player = src.getPlayerOrException();
                                    final Level level = player.level(); // <-- fix: use level()

                                    final BlockHitResult hit = raycast(player, 5.0D);
                                    if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                                        src.sendFailure(Component.literal("Look at a door within 5 blocks."));
                                        return 0;
                                    }

                                    final BlockPos hitPos = hit.getBlockPos();
                                    final BlockState st = level.getBlockState(hitPos);
                                    if (!(st.getBlock() instanceof DoorBlock)) {
                                        src.sendFailure(Component.literal("That’s not a door."));
                                        return 0;
                                    }

                                    final DoubleBlockHalf half = st.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER);
                                    final BlockPos basePos = (half == DoubleBlockHalf.UPPER) ? hitPos.below() : hitPos;

                                    final int count = DoorPassageData.get(level).get(level, basePos);
                                    final String msg = "Door at " + basePos.getX() + " " + basePos.getY() + " " + basePos.getZ()
                                            + " has been passed through " + count + " time(s).";

                                    // Either use sendSuccess with a Supplier<Component> capturing only finals:
                                    src.sendSuccess(() -> Component.literal(msg), false);

                                    // Or, if you prefer no Supplier at all:
                                    // src.sendSystemMessage(Component.literal(msg));

                                    return 1;
                                })
                        )
        );
    }

    private static BlockHitResult raycast(ServerPlayer p, double range) {
        ClipContext ctx = new ClipContext(
                p.getEyePosition(),
                p.getEyePosition().add(p.getLookAngle().scale(range)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                p
        );
        HitResult hr = p.level().clip(ctx);
        return hr instanceof BlockHitResult bhr ? bhr : null;
    }
}
