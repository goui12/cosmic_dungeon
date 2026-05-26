package net.goui.cosmicdungeon.achievement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class BindingIdolAchievements {
    private BindingIdolAchievements() {}

    private static final List<ThresholdGrant> RETURN_THRESHOLDS = List.of(
            new ThresholdGrant(1, CosmicAchievementIds.CONTRACT_FULFILLED),
            new ThresholdGrant(5, CosmicAchievementIds.ENTANGLED_OBLIGOR),
            new ThresholdGrant(10, CosmicAchievementIds.VOTARY_OF_THE_IDOL),
            new ThresholdGrant(50, CosmicAchievementIds.BOUND_DEVOTEE),
            new ThresholdGrant(100, CosmicAchievementIds.BONDED_THRALL)
    );

    private static final List<ThresholdGrant> PROVIDED_THRESHOLDS = List.of(
            new ThresholdGrant(1, CosmicAchievementIds.CONTRACT_ENFORCED),
            new ThresholdGrant(5, CosmicAchievementIds.BINDING_CREDITOR),
            new ThresholdGrant(10, CosmicAchievementIds.VOTARY_OF_THE_LEDGER),
            new ThresholdGrant(50, CosmicAchievementIds.WARDEN_OF_THE_LEDGER),
            new ThresholdGrant(100, CosmicAchievementIds.SOVEREIGN_OF_THE_BOND)
    );

    public static void recordReturnedThroughBindingIdol(ServerPlayer revivedPlayer) {
        var data = AchievementCounterData.get(revivedPlayer.level().getServer());
        var record = data.get(revivedPlayer.getUUID());

        int nextReturns = record.bindingIdolReturns() + 1;
        data.set(new AchievementCounterData.CounterRecord(
                record.playerId(),
                nextReturns,
                record.bindingIdolProvided(),
                record.vitalExchangeMask(),
                record.d1MusicDiscMask(),
                record.genericCounter1(),
                record.genericCounter2()
        ));

        grantThresholds(revivedPlayer, nextReturns, RETURN_THRESHOLDS);
    }

    public static void recordProvidedBindingIdol(ServerPlayer provider, ServerPlayer receiver) {
        // receiver parameter intentionally reserved for future gameplay/event wiring context.
        var data = AchievementCounterData.get(provider.level().getServer());
        var record = data.get(provider.getUUID());

        int nextProvided = record.bindingIdolProvided() + 1;
        data.set(new AchievementCounterData.CounterRecord(
                record.playerId(),
                record.bindingIdolReturns(),
                nextProvided,
                record.vitalExchangeMask(),
                record.d1MusicDiscMask(),
                record.genericCounter1(),
                record.genericCounter2()
        ));

        grantThresholds(provider, nextProvided, PROVIDED_THRESHOLDS);

        // TODO(binding-idol): Wire this API into the real binding idol transfer event when gameplay system exists.
    }

    private static void grantThresholds(ServerPlayer player, int count, List<ThresholdGrant> thresholds) {
        for (ThresholdGrant threshold : thresholds) {
            if (count >= threshold.threshold()) {
                CosmicAdvancementUtil.grant(player, threshold.achievementId());
            }
        }
    }

    private record ThresholdGrant(int threshold, ResourceLocation achievementId) {}
}
