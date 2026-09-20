package net.goui.cosmicdungeon.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;
import static net.goui.cosmicdungeon.CosmicDungeonMod.MOD_ID;

@EventBusSubscriber(modid=MOD_ID)
public final class CurrencyPickupEvents {
    private static final long DENIAL_MESSAGE_COOLDOWN_MS=1_000L;
    private static final Map<UUID,Long> LAST_DENIAL_MESSAGE_MS=new HashMap<>();
    private CurrencyPickupEvents(){}
    @SubscribeEvent public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if(!(event.getPlayer() instanceof ServerPlayer player))return;
        var entity=event.getItemEntity();
        var kind=LegacyCurrencyPolicy.classify(BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString(),
                DeathCurrencyService.logical(entity));
        if(kind==LegacyCurrencyPolicy.Kind.ORDINARY)return;
        event.setCanPickup(TriState.FALSE);
        // Managed death drops retain their existing account-owned pickup journal.
        // Unverified legacy stacks are neither credited nor discarded by this handler.
        if(kind==LegacyCurrencyPolicy.Kind.LEGACY_REVIEW)
            denied(player,Component.literal("This old currency item needs developer review; no Trace was added."));
    }
    static void showCapacityDeniedMessage(ServerPlayer player) {
        denied(player,Component.translatable("message.cosmicdungeon.currency_pickup_denied_capacity"));
    }
    private static void denied(ServerPlayer player,Component message) {
        long now=System.currentTimeMillis(),last=LAST_DENIAL_MESSAGE_MS.getOrDefault(player.getUUID(),0L);
        if(now-last<DENIAL_MESSAGE_COOLDOWN_MS)return;
        LAST_DENIAL_MESSAGE_MS.put(player.getUUID(),now);
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED),true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_DENIAL_MESSAGE_MS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){LAST_DENIAL_MESSAGE_MS.clear();}
    // TODO(M03, explicit legacy migration): Economy Internal 2026-08-18 makes denominations
    // display-only. Use /currency legacy inspect or entity for read-only evidence, then review
    // complete save copies before any conversion. Never infer unpaid money from an item UUID:
    // old merged/split entities may already have funded an account. Native entity aging,
    // damage/despawn remain unchanged; this is not a world backup or preservation archive.
    // TODO(Cameron2026-09-20, chest currency): reviewed class-chest quick-move may transfer
    // existing physical stacks into inventory, but does not redeem them or debit the displayed
    // account. Before account-crediting treasure clicks, distinguish newly authored unpaid
    // rewards from old potentially paid denominations and use the existing durable ledger.
    // Held/Ender/nested/unloaded legacy stacks and ambiguous Chop custody require full-save
    // review. No auto-conversion, denomination exchange, guessed refund or bulk deletion.
}
