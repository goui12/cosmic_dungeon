package net.goui.cosmicdungeon.playerclass.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClassSkillConfig {
    private ClassSkillConfig() {}
    public static ModConfigSpec.IntValue CAP, BASE, STEP, KILL_XP, SUPPORT_XP, ACTION_CAP, SUPPORT_TICKS;
    public static ModConfigSpec.DoubleValue HEALING, DEBUFF, POTION, CHAIN, DRAW;
    public static final Map<String, ModConfigSpec.DoubleValue> DAMAGE = new LinkedHashMap<>();
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Persistent class weapon skills; approved initial balance, server authoritative.").push("ClassSkills");
        CAP=b.defineInRange("levelCap",50,1,100);
        BASE=b.defineInRange("baseXpPerLevel",250,1,100000);
        STEP=b.defineInRange("additionalXpPerLevel",50,0,100000);
        KILL_XP=b.defineInRange("weaponKillXp",10,0,1000);
        SUPPORT_XP=b.defineInRange("effectiveSupportXp",2,0,100);
        ACTION_CAP=b.defineInRange("potionActionXpCap",10,0,1000);
        SUPPORT_TICKS=b.defineInRange("supportRecipientEffectIntervalTicks",60,1,1200);
        HEALING=b.defineInRange("supportArrowHealingBonusAtCap",.25,0,5);
        DEBUFF=b.defineInRange("venefexArrowDurationBonusAtCap",.25,0,5);
        POTION=b.defineInRange("potionEffectBonusAtCap",.25,0,5);
        CHAIN=b.defineInRange("dragoonChainChanceBonusAtCap",.05,0,1);
        DRAW=b.defineInRange("deadeyeDrawReductionAtCap",.10,0,.75);
        b.push("DamageBonusAtCap");
        ClassSkillRules.DAMAGE.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            if (!e.getKey().equals("theurgist.potions"))
                DAMAGE.put(e.getKey(), b.defineInRange(e.getKey().replace('.', '_'),e.getValue(),0,5));
        });
        b.pop(2);
    }
    public static int level(int xp) { return ClassSkillRules.level(xp,CAP.get(),BASE.get(),STEP.get()); }
    public static long xpCap() { return ClassSkillRules.threshold(CAP.get(),BASE.get(),STEP.get()); }
}
