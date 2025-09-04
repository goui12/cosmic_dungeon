package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.door.DoorLockData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class DoorKeyInfoCommand {
    private DoorKeyInfoCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("door").then(
                        Commands.literal("key").then(
                                Commands.literal("info")
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            ItemStack stack = findKeyInHands(p);
                                            if (stack.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Hold a Door Key in your hand."));
                                                return 0;
                                            }

                                            UUID id = stack.get(ModDataComponents.DOOR_LOCK_ID.get());
                                            if (id == null) {
                                                ctx.getSource().sendFailure(Component.literal("This key is unbound."));
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(() -> labeledCopyable("Key Lock ID", id.toString()), false);
                                            DoorLockData data = DoorLockData.get(p.level());
                                            Optional<DoorLockData.DoorRef> ref = data.findByLockId(id);
                                            ref.ifPresent(r -> {
                                                String posStr = r.lowerPos().toShortString();
                                                String dimStr = r.dimension().toString();

                                                MutableComponent line = Component.literal("Door: ").withStyle(ChatFormatting.YELLOW);
                                                line.append(copyableValue(posStr, posStr));
                                                line.append(Component.literal(" in ").withStyle(ChatFormatting.GRAY));
                                                line.append(copyableValue(dimStr, dimStr));
                                                ctx.getSource().sendSuccess(() -> line, false);
                                            });
// Show a small yellow action-bar toast


                                            return 1;
                                        })
                        )
                )
        );
    }

    private static ItemStack findKeyInHands(ServerPlayer p) {
        ItemStack main = p.getMainHandItem();
        if (main.get(ModDataComponents.DOOR_LOCK_ID.get()) != null) return main;
        ItemStack off = p.getOffhandItem();
        if (off.get(ModDataComponents.DOOR_LOCK_ID.get()) != null) return off;
        return ItemStack.EMPTY;
    }

    /* ---------- Helpers: click-to-copy components ---------- */

    private static MutableComponent labeledCopyable(String label, String valueToCopy) {
        MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.YELLOW);
        line.append(copyableValue(valueToCopy, valueToCopy));

        MutableComponent pill = Component.literal(" [Copy]").withStyle(
                Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(valueToCopy))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
        );
        return Component.literal("  ").append(line).append(pill);
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
