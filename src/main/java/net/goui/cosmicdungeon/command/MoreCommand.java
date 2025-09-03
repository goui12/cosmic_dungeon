package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class MoreCommand {
    private static final SimpleCommandExceptionType ERROR_NO_ITEM =
            new SimpleCommandExceptionType(Component.translatable("commands.more.error.no_item"));

    private MoreCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("more")
                        .requires(src -> src.hasPermission(2)) // OP level 2
                        .executes(ctx -> execute(ctx, 64))     // default 64
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304)) // up to 36*64
                                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                        )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (held.isEmpty()) {
            throw ERROR_NO_ITEM.create();
        }

        // Full clone of the item in hand (all NBT/enchants/durability preserved)
        ItemStack base = held.copy();
        base.setCount(1);

        int toGive = amount;
        int given = 0;

        // First replace the held stack
        int firstStack = Math.min(64, toGive);
        ItemStack first = base.copy();
        first.setCount(firstStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, first);
        given += firstStack;
        toGive -= firstStack;

        // Then fill inventory with the rest
        while (toGive > 0) {
            int stackSize = Math.min(64, toGive);
            ItemStack extra = base.copy();
            extra.setCount(stackSize);
            if (!player.getInventory().add(extra)) {
                // Drop at feet if inventory full
                player.drop(extra, false);
            }
            given += stackSize;
            toGive -= stackSize;
        }
        String msg = "Gave you " + given + " of held item.";
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);


        return given;
    }
}
