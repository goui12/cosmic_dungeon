package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

public final class D1Command {
    private D1Command() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrCompanions.register(dispatcher);
        net.goui.cosmicdungeon.item.identity.D1ItemAuthoring.register(dispatcher);
         net.goui.cosmicdungeon.dungeon.ChopRecoveryCommand.register(dispatcher);
        net.goui.cosmicdungeon.achievement.d1.D1JournalAuthoring.register(dispatcher);
        net.goui.cosmicdungeon.npc.tamsin.TamsinService.register(dispatcher);
        net.goui.cosmicdungeon.npc.tamsin.D1PartyService.register(dispatcher);
        dispatcher.register(Commands.literal("d1")
                .then(Commands.literal("unready").executes(ctx->{
                    net.goui.cosmicdungeon.block.custom.ClassSelectorReadyManager.withdraw(ctx.getSource().getPlayerOrException());return 1;
                }))
                .then(Commands.literal("recover").executes(ctx -> {
                    int count = net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery.claim(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.literal("Returned " + count + " protected items."), false);
                    return count;
                }))
                .then(Commands.literal("stats").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    var totals = D1LifetimeData.get(ctx.getSource().getServer()).totals(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("D1 lifetime: " + totals.completions()
                            + " completions, " + totals.spectralBlooms() + " recovered Blooms, "
                            + totals.lesserBlooms() + " Lesser Blooms, " + totals.successfulKills() + " hostile kills in successful runs."), false);
                    return 1;
                }))
                .then(Commands.literal("claim").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    if (DungeonRunRegistryData.get(ctx.getSource().getServer()).findRunForPlayer(player.getUUID()).isPresent()) {
                        ctx.getSource().sendFailure(Component.literal("Finish the dungeon reset before claiming stored belongings."));
                        return 0;
                    }
                    int count = D1StoredInventoryData.get(ctx.getSource().getServer()).claim(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Returned " + count + " stored items."), false);
                    return count;
                }))
                .then(Commands.literal("watson").requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.literal("set").then(Commands.argument("position", BlockPosArgument.blockPos())
                                .executes(ctx -> {
                                    var source = ctx.getSource();
                                    var template = DungeonInstanceSlots.templateDimensionForPhysical(source.getServer(), source.getLevel().dimension());
                                    if (!DungeonDefinitions.DUNGEON_1.containsDimension(template)) {
                                        source.sendFailure(Component.literal("Set Watson in a D1 template or D1 instance."));
                                        return 0;
                                    }
                                    var pos = BlockPosArgument.getLoadedBlockPos(ctx, "position");
                                    D1WatsonData.get(source.getServer()).set(template.location().toString(), pos);
                                    source.sendSuccess(() -> Component.literal("Watson D1 placement: " + template.location() + " " + pos.toShortString()), true);
                                    return 1;
                                })))
                        .then(Commands.literal("status").executes(ctx -> {
                            var binding = D1WatsonData.get(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(binding.configured()
                                    ? binding.dimension() + " " + binding.pos().toShortString() : "Watson placement is not configured."), false);
                            return 1;
                        }))));
    }
}
