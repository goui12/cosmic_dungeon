package net.goui.cosmicdungeon.playerclass.theurgist;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Q&A D14. Vanilla owns recipe, fuel, remainder and completion events. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TheurgistBrewingEvents {
    private TheurgistBrewingEvents() {}
    private static boolean allowed(ServerPlayer player) {
        return ClassKeys.CLASS_ID_THEURGIST.equals(ClassData.getClassId(player));
    }

    @SubscribeEvent
    public static void onOpenAttempt(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level().getBlockState(event.getPos()).getBlock() instanceof BrewingStandBlock)) return;
        if (allowed(player)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(Component.literal("Only Theurgists can use a brewing stand."), true);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getContainer() instanceof BrewingStandMenu && !allowed(player)) {
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof BrewingStandMenu menu)) return;
        if (!allowed(player)) { player.closeContainer(); return; }
        // No global block-entity scan or reflection. This menu addresses its own stand.
        // Only shorten an already-started vanilla brew, so fuel is consumed exactly once.
        if (Config.INSTANT_BREWING.get() && menu.stillValid(player)
                && menu.getBrewingTicks() > Config.BREW_TICKS.get()) {
            menu.setData(0, Config.BREW_TICKS.get());
        }
    }

    // TODO(D12/D42/D68/D82, D2+): source 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM,
    // Theurgist Items and Armor, 2026-04-05, D2/D3 sections explicitly deferred.
    // Portable brewing queues, tier speed 50/60/70/80%, custom cleansing/stasis effects,
    // conduit fatal-damage health/debuff/cooldown tiers, and resurrection after respawn
    // need their own reviewed lifecycle. D1 retains authored vanilla Undying totems.
    // Put all later effect/radius/duration modifiers in the Theurgist config section.
}
