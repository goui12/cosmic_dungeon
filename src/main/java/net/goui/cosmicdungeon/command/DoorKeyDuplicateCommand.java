package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class DoorKeyDuplicateCommand {
    private DoorKeyDuplicateCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("door").then(
                        Commands.literal("key").then(
                                Commands.literal("duplicate")
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            ItemStack held = p.getMainHandItem();
                                            UUID id = held.getOrDefault(ModDataComponents.DOOR_LOCK_ID.get(), null);
                                            if (id == null) {
                                                ctx.getSource().sendFailure(Component.literal("Hold a bound Door Key in your main hand."));
                                                return 0;
                                            }
                                            ItemStack copy = held.copy();
                                            copy.setCount(1);
                                            if (!p.addItem(copy)) {
                                                p.drop(copy, false);
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Duplicated key with ID " + id), false);
                                            return 1;
                                        })
                        )
                )
        );
    }
}
