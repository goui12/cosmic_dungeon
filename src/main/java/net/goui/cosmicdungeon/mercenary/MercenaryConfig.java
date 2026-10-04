package net.goui.cosmicdungeon.mercenary;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class MercenaryConfig {
    public static ModConfigSpec.IntValue HIRE_TRACE;
    private MercenaryConfig(){}
    public static void define(ModConfigSpec.Builder b){
        b.push("mercenaries");
        HIRE_TRACE=b.comment("Trace per hire, reserved during entry and charged only after successful startup.")
                .defineInRange("hireTrace",500,0,100000000);
        b.pop();
    }
}
