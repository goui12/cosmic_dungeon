package net.goui.cosmicdungeon.mercenary;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;
public final class MercenaryConfig {
    public static ModConfigSpec.IntValue HIRE_TRACE,BREW_TICKS,FALLBACK_TICKS,POTION_TICKS;
    public static ModConfigSpec.ConfigValue<List<? extends String>> POTION_COOLDOWNS;
    private MercenaryConfig(){}
    public static void define(ModConfigSpec.Builder b){
        b.push("mercenaries");
        HIRE_TRACE=b.comment("Trace per hire, reserved during entry and charged only after successful startup.")
                .defineInRange("hireTrace",500,0,100000000);
        BREW_TICKS=b.comment("Legacy brewing delay retained for configuration compatibility; Theurgists now brew instantly at nearby stands.")
                .defineInRange("brewTicks",400,20,1728000);
        FALLBACK_TICKS=b.comment("Active ticks per fallback splash healing potion; no offline catch-up.")
                .defineInRange("fallbackHealingTicks",3600,20,1728000);
        POTION_TICKS=b.comment("Default active-tick cooldown per potion identity, shared across drink/splash/lingering.")
                .defineInRange("potionCooldownTicks",200,20,1728000);
        POTION_COOLDOWNS=b.comment("Individual potion ID=ticks overrides. Custom effects use custom.")
                .defineListAllowEmpty("potionCooldowns",List.of("minecraft:healing=200","minecraft:strong_healing=300",
                        "minecraft:regeneration=400"),()->"minecraft:healing=200",MercenaryConfig::validOverride);
        b.pop();
    }
    public static boolean validOverride(Object value){
        if(!(value instanceof String text))return false;
        int split=text.lastIndexOf('=');if(split<1)return false;
        try{
            int ticks=Integer.parseInt(text.substring(split+1));String id=text.substring(0,split);
            return ticks>=20&&ticks<=1728000&&(id.equals("custom")||net.minecraft.resources.ResourceLocation.tryParse(id)!=null);
        }catch(NumberFormatException invalid){return false;}
    }
    public static int potionTicks(String id){
        for(String entry:POTION_COOLDOWNS.get()){
            int split=entry.lastIndexOf('=');if(entry.substring(0,split).equals(id))return Integer.parseInt(entry.substring(split+1));
        }
        return POTION_TICKS.get();
    }
}
