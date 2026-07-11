package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class CosmicAchievementIds {
    private CosmicAchievementIds() {}

    /** Visible as First Trace; legacy path preserved for advancement-file compatibility. */
    public static final ResourceLocation FIRST_TRACE = id("achievements/im_rich");
    public static final ResourceLocation PLANT_FLAGS = id("achievements/plant_flags");
    public static final ResourceLocation CONTRACT_FULFILLED = id("achievements/contract_fulfilled");
    public static final ResourceLocation ENTANGLED_OBLIGOR = id("achievements/entangled_obligor");
    public static final ResourceLocation VOTARY_OF_THE_IDOL = id("achievements/votary_of_the_idol");
    public static final ResourceLocation BOUND_DEVOTEE = id("achievements/bound_devotee");
    public static final ResourceLocation BONDED_THRALL = id("achievements/bonded_thrall");
    public static final ResourceLocation CONTRACT_ENFORCED = id("achievements/contract_enforced");
    public static final ResourceLocation BINDING_CREDITOR = id("achievements/binding_creditor");
    public static final ResourceLocation VOTARY_OF_THE_LEDGER = id("achievements/votary_of_the_ledger");
    public static final ResourceLocation WARDEN_OF_THE_LEDGER = id("achievements/warden_of_the_ledger");
    public static final ResourceLocation SOVEREIGN_OF_THE_BOND = id("achievements/sovereign_of_the_bond");
    public static final ResourceLocation TIRED_NOT_BROKEN = id("achievements/tired_not_broken");
    public static final ResourceLocation VITAL_EXCHANGE_1 = id("achievements/vital_exchange_1");
    public static final ResourceLocation VITAL_EXCHANGE_2 = id("achievements/vital_exchange_2");
    public static final ResourceLocation VITAL_EXCHANGE_3 = id("achievements/vital_exchange_3");
    public static final ResourceLocation VITAL_EXCHANGE_4 = id("achievements/vital_exchange_4");
    public static final ResourceLocation SIXFOLD_VIGIL = id("achievements/sixfold_vigil");
    public static final ResourceLocation SIXFOLD_VIGIL_AFTER_DISSOLUTION = id("achievements/sixfold_vigil_after_dissolution");
    public static final ResourceLocation SIXFOLD_VIGIL_LONE_ADVERSARY = id("achievements/sixfold_vigil_lone_adversary");
    public static final ResourceLocation SIXFOLD_VIGIL_TWIN_MANIFESTATION = id("achievements/sixfold_vigil_twin_manifestation");
    public static final ResourceLocation CYCLE_OF_RECORDED_SOUND = id("achievements/cycle_of_recorded_sound");
    public static final ResourceLocation SYNCHRONOUS_PEAL = id("achievements/synchronous_peal");
    public static final ResourceLocation FIRST_PLAYER_TRADE = id("achievements/first_player_trade");
    public static final ResourceLocation PYROCLAST_BOOM = id("pyroclast/boom");

    public static final List<ResourceLocation> ALL = List.of(
            FIRST_TRACE, PLANT_FLAGS, CONTRACT_FULFILLED, ENTANGLED_OBLIGOR, VOTARY_OF_THE_IDOL, BOUND_DEVOTEE, BONDED_THRALL,
            CONTRACT_ENFORCED, BINDING_CREDITOR, VOTARY_OF_THE_LEDGER, WARDEN_OF_THE_LEDGER, SOVEREIGN_OF_THE_BOND,
            TIRED_NOT_BROKEN, VITAL_EXCHANGE_1, VITAL_EXCHANGE_2, VITAL_EXCHANGE_3, VITAL_EXCHANGE_4, SIXFOLD_VIGIL,
            SIXFOLD_VIGIL_AFTER_DISSOLUTION, SIXFOLD_VIGIL_LONE_ADVERSARY, SIXFOLD_VIGIL_TWIN_MANIFESTATION,
            CYCLE_OF_RECORDED_SOUND, SYNCHRONOUS_PEAL, FIRST_PLAYER_TRADE, PYROCLAST_BOOM
    );

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }
}
