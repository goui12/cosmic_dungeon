package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.door.DoorLockData;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class DoorLockCommand {
    private DoorLockCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("door").then(
                        Commands.literal("lock")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    BlockHitResult hit = raycast(p, 5.0);
                                    if (hit == null || hit.getType() == HitResult.Type.MISS) {
                                        ctx.getSource().sendFailure(Component.literal("Look at a door within 5 blocks."));
                                        return 0;
                                    }

                                    Level level = p.level();
                                    BlockPos pos = hit.getBlockPos();
                                    BlockState state = level.getBlockState(pos);
                                    if (!(state.getBlock() instanceof DoorBlock)) {
                                        ctx.getSource().sendFailure(Component.literal("Target block is not a door."));
                                        return 0;
                                    }

                                    // vanilla-only guard
                                    ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                                    if (!"minecraft".equals(key.getNamespace())) {
                                        ctx.getSource().sendFailure(Component.literal("Only vanilla doors can be locked."));
                                        return 0;
                                    }

                                    // normalize to lower half
                                    if (state.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER) {
                                        pos = pos.below();
                                    }

                                    // make it final for lambdas
                                    final BlockPos fpos = pos;

                                    DoorLockData data = DoorLockData.get(level);
                                    if (data.isLocked(level, fpos)) {
                                        DoorLockData.LockInfo info = data.getLock(level, fpos);
                                        ctx.getSource().sendFailure(Component.literal("Door already locked. Lock ID: " + info.lockId));
                                        return 0;
                                    }

                                    DoorLockData.LockInfo info = data.lock(level, fpos, p.getUUID());

                                    // give bound key
                                    ItemStack keyStack = new ItemStack(ModItems.DOOR_KEY.get());
                                    keyStack.set(ModDataComponents.DOOR_LOCK_ID.get(), info.lockId);
                                    if (!p.addItem(keyStack)) {
                                        p.drop(keyStack, false);
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Locked door at " + fpos.toShortString() + " with ID " + info.lockId),
                                            true);
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
