package net.goui.cosmicdungeon.faction;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Separate UUID faction; legacy Watson faction values remain unchanged. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class NpcFactionService {
    private NpcFactionService() {}
    public static int value(ServerPlayer player) { return FactionService.getValue(player, FactionDefinitions.NPC_ID); }
    public static boolean canRetail(ServerPlayer player) { return value(player) >= 5; }
    public static void blooms(ServerPlayer player, int count) {
        long gain = (long)Math.max(0, count) * Config.NPC_BLOOM_GAIN.get();
        FactionService.setValue(player, FactionDefinitions.NPC_ID, (int)Math.min(100L, value(player) + gain));
    }
    public static long retail(ServerPlayer player, long subtotal) {
        if (subtotal < 0 || !canRetail(player)) return -1;
        if (subtotal == 0) return 0;
        int faction = value(player);
        double multiplier = faction >= 100 ? Config.ALLY_RETAIL.get()
                : faction >= 50 ? Config.WARM_RETAIL.get() : Config.CORDIAL_RETAIL.get();
        return adjusted(subtotal, multiplier);
    }
    public static long adjusted(long subtotal, double multiplier) {
        if (subtotal <= 0) return Math.max(-1, subtotal);
        var amount = BigDecimal.valueOf(subtotal).multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.HALF_UP);
        return amount.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : Math.max(1, amount.longValueExact());
    }
    @SubscribeEvent
    public static void killed(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var entity = event.getEntity();
        // Assigned vendor identity and our persistent Watson tag are authoritative, names are not.
        if (VendorAssignmentService.getProfileId(entity) == null
                && entity.getPersistentData().getLongOr("cosmicdungeon_d1_watson_run", 0L) <= 0) return;
        FactionService.adjust(player, FactionDefinitions.NPC_ID, -Config.NPC_KILL_LOSS.get(), "npc_death");
    }
}
