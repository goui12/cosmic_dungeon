package net.goui.cosmicdungeon.playerclass.d1;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;
/** These definitions join CosmicDungeon.config; no separate class config files. */
public final class D1AbilityConfig {
    public record Spell(ModConfigSpec.DoubleValue power,ModConfigSpec.IntValue duration){}
    public static final Map<String,Spell> SPELLS=new LinkedHashMap<>();
    public static ModConfigSpec.DoubleValue ROCKET_RADIUS,SPICULE_DEBUFF_SCALE;
    public static ModConfigSpec.IntValue SPICULE_DEBUFF_CAP,ROCKET_CANDIDATE_LIMIT;
    private D1AbilityConfig(){}
    public static void define(ModConfigSpec.Builder b){
        b.comment("D1 Theurgist overview 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM, 2026-04-05.",
                "This overview is newer than the April 4 Verdant Jolt internal document.").push("Theurgist");
        spell(b,"theurgist","mending_sting",2,100,"Total health points restored over five seconds.");
        spell(b,"theurgist","verdant_jolt",0.8,40,"Newest overview: 0.4 hearts total over two seconds; supersedes older 11-second effect.");
        spell(b,"theurgist","scintilla_vitalis",4,1,"Health restored to living targets, or magic damage to undead.");
        spell(b,"theurgist","lux_vitalis",8,1,"Health restored to living targets, or magic damage to undead.");
        b.pop();
        b.comment("Judicator overview 1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo, 2026-04-04.").push("Judicator");
        spell(b,"judicator","scintilla_vitalis",4,1,"Health points restored; damage instead for undead.");
        spell(b,"judicator","ebonsight",0,200,"Night vision duration; vanilla night vision also provides underwater visibility.");
        spell(b,"judicator","vielpiercer",0,200,"Glowing duration; Arrow Vielpiercer Internal.");
        b.pop();
        b.comment("Venefex overview 1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8, 2026-04-04.",
                "Custom effects preserve the stated totals and durations instead of approximating potion amplifiers.").push("Venefex");
        spell(b,"venefex","tree_viper",4,100,"Total poison health points over five seconds; poison cannot kill.");
        spell(b,"venefex","pestis",4,220,"Melee attack damage reduction in health points.");
        spell(b,"venefex","vapours",0.15,220,"Movement reduction as a fraction, 0.15 = fifteen percent.");
        spell(b,"venefex","spicule_breach",6,1,"Base instant magic damage to living targets.");
        spell(b,"venefex","bushmaster",8,220,"Total poison health points over eleven seconds.");
        spell(b,"venefex","fer_de_lance",3,40,"Total poison health points over two seconds.");
        spell(b,"venefex","black_bubo",4,600,"Melee attack damage reduction in health points.");
        spell(b,"venefex","melancholia",0.15,600,"Movement reduction as a fraction.");
        spell(b,"venefex","deathly_stupor",0.60,40,"Movement reduction as a fraction.");
        spell(b,"venefex","spicule_rend",12,1,"Base instant magic damage to living targets.");
        spell(b,"venefex","spicule_undead_healing",6,1,"Both Spicule arrows restore three hearts to undead.");
        SPICULE_DEBUFF_SCALE=b.comment("Implementation choice: ten percent extra per distinct harmful effect; docs specify scaling but no formula.")
                .defineInRange("spiculeDamagePerDebuff",0.10,0,10);
        SPICULE_DEBUFF_CAP=b.comment("Bounds the chosen scaling formula; adjust after Dad confirms the intended curve.")
                .defineInRange("spiculeDebuffLimit",10,0,128);
        b.pop();
        b.comment("Pyroclast overview 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE, March 26 14:08.",
                "Newer than rocket internal documents: Cindermaul maximum is 7.5 hearts, not seven.").push("Pyroclast");
        spell(b,"pyroclast","cinderbite",12,1,"Maximum explosion damage at the center; four-star authored vanilla rocket.");
        spell(b,"pyroclast","cindermaul",15,1,"Maximum explosion damage at the center; five-star authored vanilla rocket.");
        ROCKET_RADIUS=b.comment("Blocks. Vanilla square-root falloff and obstruction checks; no terrain damage.")
                .defineInRange("rocketExplosionRadius",5.0,0.5,32.0);
        ROCKET_CANDIDATE_LIMIT=b.comment("Maximum nearby living entities inspected per D1 explosion. At saturation some targets are omitted; at most two obstruction rays per candidate.")
                .defineInRange("rocketCandidateLimit",256,1,1024);
        b.pop();
    }
    private static void spell(ModConfigSpec.Builder b,String section,String id,double power,int ticks,String comment){
        b.comment(comment).push(id);
        var p=b.defineInRange("power",power,0,1024);
        var d=b.comment("20 server ticks = one second.").defineInRange("durationTicks",ticks,1,72000);
        SPELLS.put(section.toLowerCase(Locale.ROOT)+":"+id,new Spell(p,d));b.pop();
    }
    public static Spell get(String cls,String id){return SPELLS.get(cls+":"+id);}
}
