package net.goui.cosmicdungeon.economy;

import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.goui.cosmicdungeon.CosmicDungeonMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public final class CurrencyPickupEvents {
    private static final long DENIAL_MESSAGE_COOLDOWN_MS = 1_000L;
    private static final Map<UUID, Long> LAST_DENIAL_MESSAGE_MS = new ConcurrentHashMap<>();

    private CurrencyPickupEvents() {}

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if(DeathCurrencyService.logical(itemEntity)){event.setCanPickup(TriState.FALSE);return;}
        CurrencyDenomination denomination = denominationForItem(stack.getItem());
        if (denomination == null) return;

        long traceAmount;
        try {
            traceAmount = denomination.toTrace(stack.getCount());
        } catch (ArithmeticException ex) {
            event.setCanPickup(TriState.FALSE);
            return;
        }

        if (!CurrencyService.canDeposit(serverPlayer, traceAmount)) {
            event.setCanPickup(TriState.FALSE);
            showCapacityDeniedMessage(serverPlayer);
            return;
        }

        // TODO(M03, Gear Trading and Vendor Sales 2.0, 2026-08-18): replace this older
        // denomination deposit/discard pair with verified account/entity custody and receipts.
        // Preserve exact count/components/UUID/owner target; cover merge/split, chunk versus
        // player save order, cap rejection, held/Ender Chest variants and interrupted restart.
        // One receipt for a merged entity does not prove its earlier source entities were
        // durably removed. Batch26's logical death-drop journal covers only managed new drops.
        // Review complete save copies; never delete physical currency to guess a migration.
        if (CurrencyService.tryDeposit(serverPlayer, traceAmount)) {
            event.setCanPickup(TriState.FALSE);
            itemEntity.discard();
        }
    }

    static void showCapacityDeniedMessage(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long last = LAST_DENIAL_MESSAGE_MS.getOrDefault(player.getUUID(), 0L);
        if (now - last < DENIAL_MESSAGE_COOLDOWN_MS) return;

        LAST_DENIAL_MESSAGE_MS.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.translatable("message.cosmicdungeon.currency_pickup_denied_capacity").withStyle(ChatFormatting.RED),
                true
        );
    }

    private static CurrencyDenomination denominationForItem(Item item) {
        if (item == ModItems.ATTUNEMENT_TRACE.get()) return CurrencyDenomination.TRACE;
        if (item == ModItems.ATTUNEMENT_MARK.get()) return CurrencyDenomination.MARK;
        if (item == ModItems.ATTUNEMENT_SEAL.get()) return CurrencyDenomination.SEAL;
        if (item == ModItems.ATTUNEMENT_CROWN.get()) return CurrencyDenomination.CROWN;
        if (item == ModItems.ATTUNEMENT_ANCHOR.get()) return CurrencyDenomination.ANCHOR;
        return null;
    }
}
