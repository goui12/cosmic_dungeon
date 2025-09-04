package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.door.DoorLockData;
import net.goui.cosmicdungeon.door.DoorPassageData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DoorInfoCommand {
    private DoorInfoCommand() {}

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("door").then(
                        Commands.literal("info")
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

                                    // Normalize to LOWER half
                                    if (state.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER) {
                                        pos = pos.below();
                                    }

                                    final BlockPos fpos = pos;

                                    DoorPassageData passData = DoorPassageData.get(level);
                                    int count = passData.get(level, fpos);
                                    Integer limit = passData.getLimit(level, fpos);
                                    String passageText = limit == null ? String.valueOf(count) : (count + "/" + limit);

                                    DoorLockData lockData = DoorLockData.get(level);
                                    DoorLockData.LockInfo info = lockData.getLock(level, fpos);

                                    ctx.getSource().sendSuccess(() -> {
                                        MutableComponent line = Component.literal("Door @ ")
                                                .withStyle(ChatFormatting.DARK_AQUA);
                                        line.append(copyableValue(fpos.toShortString(), fpos.toShortString()));
                                        return line;
                                    }, false);

                                    ctx.getSource().sendSuccess(() -> labeledCopyable("Passages", passageText), false);

                                    if (info == null) {
                                        ctx.getSource().sendSuccess(() -> labeledCopyable("Locked", "false"), false);
                                    } else {
                                        ctx.getSource().sendSuccess(() -> labeledCopyable("Locked", "true"), false);
                                        ctx.getSource().sendSuccess(() -> labeledCopyable("Lock ID", info.lockId.toString()), false);
                                        ctx.getSource().sendSuccess(() -> labeledCopyable("Owner", info.owner.toString()), false);
                                        String createdStr = FMT.format(Instant.ofEpochMilli(info.createdAtEpochMillis));
                                        ctx.getSource().sendSuccess(() -> labeledCopyable("Created", createdStr), false);
                                    }
// Show a small yellow action-bar toast

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

    /* ---------- Helpers: click-to-copy components ---------- */

    private static MutableComponent labeledCopyable(String label, String valueToCopy) {
        MutableComponent line = Component.literal("  " + label + ": ")
                .withStyle(ChatFormatting.YELLOW);
        line.append(copyableValue(valueToCopy, valueToCopy));

        // Add a small clickable [Copy] pill
        MutableComponent pill = Component.literal(" [Copy]").withStyle(
                Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(valueToCopy))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
        );
        line.append(pill);
        return line;
    }

    private static MutableComponent copyableValue(String visible, String toCopy) {
        return Component.literal(visible).withStyle(
                Style.EMPTY
                        .withColor(ChatFormatting.WHITE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(toCopy))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
        );
    }
}
