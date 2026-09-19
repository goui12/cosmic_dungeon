package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Tamsin Vane (2026-08-19), retained NPC!A13: personal Yes/No agreement, interface
 * map marked Base Camp / -JHW, then D1 class selection. Never sells items or charges a fee.
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TamsinService {
    private TamsinService() {}
    public static boolean accepted(ServerPlayer player) {
        return TamsinData.get(player.level().getServer()).accepted(player.getUUID());
    }
    private static boolean selected(ServerPlayer player) {
        return ClassSelectorTeleportUtil.isReadyEligibleClass(ClassData.getClassId(player));
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (handle(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handle(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    private static boolean handle(Player actor, Entity npc, InteractionHand hand) {
        if (!(actor instanceof ServerPlayer player)) return false;
        var binding = TamsinData.get(player.level().getServer()).binding(npc.getUUID());
        if (binding == null) return false;
        if (hand != InteractionHand.MAIN_HAND) return true;
        if (AccessPolicy.isDeveloper(player)) {
            player.sendSystemMessage(Component.literal("Tamsin is bound to a D1 selector. Use /d1 tamsin status to inspect bindings."));
            return true;
        }
        if (!player.isAlive() || player.isSpectator() || !npc.isAlive()
                || !binding.dimension().equals(player.level().dimension().location().toString())
                || DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent())
            return true;
        var pos = BlockPos.of(binding.selector());
        double range = Config.SELECTOR_RANGE.get();
        if (player.distanceToSqr(npc) > range * range
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > range * range
                || !player.level().hasChunkAt(pos)
                || !(player.level().getBlockEntity(pos) instanceof ClassSelectorBlockEntity)) {
            player.sendSystemMessage(Component.literal("Tamsin's nearby D1 selector is unavailable."));
            return true;
        }
        // The two interaction event variants must not reopen the same live conversation.
        if (player.containerMenu instanceof ClassSelectorMenu menu
                && npc.getUUID().equals(menu.tamsinNpc()) && menu.stillValid(player)) return true;
        ClassSelectorTeleportUtil.markPendingSelectorSource(player, player.level(), pos);
        var stage = TamsinTaxProgress.available(player) ? TamsinFlow.Stage.TAX
                : TamsinFlow.initial(accepted(player), selected(player));
        if (stage == TamsinFlow.Stage.TAX)
            net.goui.cosmicdungeon.block.custom.ClassSelectorReadyManager.withdraw(player);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new ClassSelectorMenu(id, inventory, npc.getUUID(), binding, stage),
                Component.literal("Tamsin Vane")));
        return true;
    }
    public static boolean validNpcSession(ServerPlayer player, ClassSelectorMenu menu) {
        if (menu.tamsinNpc() == null) return true;
        var binding = TamsinData.get(player.level().getServer()).binding(menu.tamsinNpc());
        if (binding == null || !binding.equals(menu.tamsinBinding())
                || !binding.dimension().equals(player.level().dimension().location().toString())) return false;
        var npc = player.level().getEntity(menu.tamsinNpc());
        double range = Config.SELECTOR_RANGE.get();
        return npc instanceof LivingEntity && npc.isAlive() && player.distanceToSqr(npc) <= range * range;
    }
    public static void action(ServerPlayer player, int containerId, String action) {
        if (!(player.containerMenu instanceof ClassSelectorMenu menu) || menu.containerId != containerId
                || menu.tamsinNpc() == null || !menu.stillValid(player)) return;
        if (menu.stage() == TamsinFlow.Stage.AGREEMENT && action.equals("no")) {
            player.closeContainer(); return;
        }
        var next = TamsinFlow.advance(menu.stage(), action, selected(player));
        if (next == menu.stage()) return;
        if (menu.stage() == TamsinFlow.Stage.AGREEMENT) {
            TamsinData.get(player.level().getServer()).accept(player.getUUID());
        } else if (!accepted(player)) return;
        menu.setStage(next);
        ClassNet.sendSelectorDataTo(player);
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("d1")
                .then(Commands.literal("tamsin").requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.literal("bind").then(Commands.argument("npc", EntityArgument.entity())
                                .then(Commands.argument("selector", BlockPosArgument.blockPos()).executes(ctx -> {
                                    var source = ctx.getSource();
                                    var npc = EntityArgument.getEntity(ctx, "npc");
                                    var pos = BlockPosArgument.getLoadedBlockPos(ctx, "selector");
                                    var template = DungeonInstanceSlots.templateDimensionForPhysical(source.getServer(), source.getLevel().dimension());
                                    if (!(npc instanceof LivingEntity) || npc instanceof Player || !npc.isAlive()
                                            || npc.level() != source.getLevel()
                                            || DungeonDefinitions.DUNGEON_1.containsDimension(template)
                                            || !(source.getLevel().getBlockEntity(pos) instanceof ClassSelectorBlockEntity)
                                            || npc.distanceToSqr(Vec3.atCenterOf(pos)) > Config.SELECTOR_RANGE.get() * Config.SELECTOR_RANGE.get()) {
                                        source.sendFailure(Component.literal("Choose a living NPC beside a D1 selector in the starting area."));
                                        return 0;
                                    }
                                    TamsinData.get(source.getServer()).bind(npc.getUUID(),
                                            new TamsinData.Binding(source.getLevel().dimension().location().toString(), pos.asLong()));
                                    source.sendSuccess(() -> Component.literal("Tamsin bound: " + npc.getUUID() + " -> " + pos.toShortString()), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("unbind").then(Commands.argument("npc", net.minecraft.commands.arguments.UuidArgument.uuid()).executes(ctx -> {
                            boolean removed = TamsinData.get(ctx.getSource().getServer()).unbind(
                                    net.minecraft.commands.arguments.UuidArgument.getUuid(ctx, "npc"));
                            ctx.getSource().sendSuccess(() -> Component.literal(removed ? "Tamsin binding removed." : "That NPC is not bound."), true);
                            return removed ? 1 : 0;
                        })))
                        .then(Commands.literal("status").executes(ctx -> {
                            var data = TamsinData.get(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("Tamsin NPC bindings: " + data.bindingCount()), false);
                            return data.bindingCount();
                        }))));
    }
    // TODO(M37, licensed TEST acceptance): exercise D1PartyService invitations, pending onboarding,
    // personal ready checks, FIFO leader queue and preparation cancellation with 3-6 players.
    // Q&A D24 forbids party merges; active instance rosters remain governed by existing lifecycle.
    // TODO(M36/M41): verify the authored Starting Area NPC and selected destination in the
    // licensed TEST world. Bind explicitly; never spawn, rename, move, or replace map NPCs.
}
