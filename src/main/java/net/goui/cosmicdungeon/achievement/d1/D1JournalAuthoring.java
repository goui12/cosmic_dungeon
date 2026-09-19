package net.goui.cosmicdungeon.achievement.d1;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.item.identity.ItemAuthoringPlan;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1JournalAuthoring {
    private record Pending(int slot, String dimension, long scope, ItemAuthoringPlan<ItemStack> plan) {}
    private static final Map<UUID, Pending> PLANS = new HashMap<>();
    private D1JournalAuthoring() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("d1").then(Commands.literal("journal")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("create").then(Commands.argument("number", IntegerArgumentType.integer(1,3))
                        .executes(c -> create(c.getSource(), IntegerArgumentType.getInteger(c,"number")))))
                .then(Commands.literal("preview").then(Commands.argument("number", IntegerArgumentType.integer(1,3))
                        .executes(c -> preview(c.getSource(), IntegerArgumentType.getInteger(c,"number")))))
                .then(Commands.literal("apply").then(Commands.argument("token", StringArgumentType.word())
                        .executes(c -> apply(c.getSource(), StringArgumentType.getString(c,"token"), false))))
                .then(Commands.literal("undo").then(Commands.argument("token", StringArgumentType.word())
                        .executes(c -> apply(c.getSource(), StringArgumentType.getString(c,"token"), true))))));
    }
    private static ServerPlayer author(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null || !AccessPolicy.isDeveloper(player)) return null;
        if (player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) {
            source.sendFailure(Component.literal("Close the interface and hold the book first.")); return null;
        }
        return player;
    }
    private static int create(CommandSourceStack source, int number) {
        var player = author(source); if (player == null) return 0;
        if (player.getInventory().getFreeSlot() < 0) {
            source.sendFailure(Component.literal("Make room for the journal first.")); return 0;
        }
        var book = D1JournalService.create("journal_" + number);
        player.getInventory().add(book);
        if (!book.isEmpty()) return 0;
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal("Created canonical Journal " + number + " as a vanilla written book."), false);
        return 1;
    }
    private static int preview(CommandSourceStack source, int number) {
        var player = author(source); if (player == null) return 0;
        var before = player.getMainHandItem().copy();
        ItemStack after;
        try { after = D1JournalService.markCopy(before, "journal_" + number); }
        catch (IllegalArgumentException invalid) {
            source.sendFailure(Component.literal(invalid.getMessage())); return 0;
        }
        String token = UUID.randomUUID().toString();
        var plan = new ItemAuthoringPlan<>(token, System.currentTimeMillis() + Config.ITEM_AUTHORING_SECONDS.get()*1000L,
                before, after);
        PLANS.put(player.getUUID(), new Pending(player.getInventory().getSelectedSlot(),
                player.level().dimension().location().toString(), ProtectedItemRecovery.scope(player), plan));
        source.sendSuccess(() -> Component.literal("Canonical text matches. Add only the journal identity with /d1 journal apply "
                + token + ". Undo uses the same token before expiry."), false);
        return 1;
    }
    private static int apply(CommandSourceStack source, String token, boolean undo) {
        var player = author(source); if (player == null) return 0;
        var pending = PLANS.get(player.getUUID()); if (pending == null) return 0;
        boolean same = pending.slot() == player.getInventory().getSelectedSlot()
                && pending.dimension().equals(player.level().dimension().location().toString())
                && pending.scope() == ProtectedItemRecovery.scope(player);
        if (!pending.plan().accepts(token, System.currentTimeMillis(), same, player.getMainHandItem(),
                undo, ItemStack::matches)) {
            source.sendFailure(Component.literal("Preview expired or the exact held book changed.")); return 0;
        }
        player.getInventory().setItem(pending.slot(), pending.plan().image(undo).copy());
        pending.plan().committed(undo);
        if (undo) PLANS.remove(player.getUUID());
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal(undo ? "Restored the original book." : "Journal identity applied; all existing book data retained."), false);
        return 1;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PLANS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PLANS.clear(); }
}
