package net.goui.cosmicdungeon.playerclass.d1;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;
/** Compatibility for already-authored vanilla gear. Never used as a sale price or provenance check. */
public final class D1AbilityIdentity {
    private static final Map<String,String> ALIASES=new HashMap<>();
    private static final Set<String> IDS=Set.of("mending_sting","verdant_jolt","scintilla_vitalis","lux_vitalis",
            "ebonsight","vielpiercer","tree_viper","pestis","vapours","spicule_breach","bushmaster","fer_de_lance",
            "black_bubo","melancholia","deathly_stupor","spicule_rend","cinderbite","cindermaul");
    static {
        for(String id:IDS)ALIASES.put(normalize(id.replace('_',' ')),id);
        alias("Venom of the Tree Viper","tree_viper");alias("Arrow of Pestis","pestis");alias("Arrow of the Vapours","vapours");
        alias("Venom of the Bushmaster","bushmaster");alias("Venom of the Fer-de-Lance","fer_de_lance");
        alias("Arrow of Black Bubo","black_bubo");alias("Arrow of Melancholia","melancholia");
        alias("Arrow of Deathly Stupor","deathly_stupor");alias("Spicule of Breach","spicule_breach");alias("Spicule of Rend","spicule_rend");
        alias("Arrow of Mending Sting","mending_sting");alias("Arrow of Verdant Jolt","verdant_jolt");
        alias("Veilpiercer","vielpiercer"); // Historical spelling; registry IDs remain unchanged.
    }
    private D1AbilityIdentity(){}
    private static String normalize(String name){return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static void alias(String name,String id){ALIASES.put(normalize(name),id);}
    public static String identify(ItemStack stack){
        if(stack==null||stack.isEmpty())return null;
        String explicit=stack.get(ModDataComponents.D1_ABILITY.get());
        String id=IDS.contains(explicit==null?"":explicit)?explicit:null;
        var registry=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(id==null&&registry.getNamespace().equals("cosmicdungeon")&&IDS.contains(registry.getPath()))id=registry.getPath();
        if(id==null&&stack.has(DataComponents.CUSTOM_NAME))id=ALIASES.get(normalize(stack.get(DataComponents.CUSTOM_NAME).getString()));
        if(id==null)return null;
        if(id.startsWith("cinder")){
            var fireworks=stack.get(DataComponents.FIREWORKS);
            return stack.is(Items.FIREWORK_ROCKET)&&fireworks!=null
                    &&fireworks.explosions().size()==(id.equals("cinderbite")?4:5)?id:null;
        }
        if(!stack.is(Items.TIPPED_ARROW)&&!stack.is(Items.SPECTRAL_ARROW)&&!registry.getNamespace().equals("cosmicdungeon"))return null;
        if(explicit!=null||registry.getNamespace().equals("cosmicdungeon"))return id;
        if(id.equals("vielpiercer"))return stack.is(Items.SPECTRAL_ARROW)?id:null;
        var potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion==null||!potion.customEffects().isEmpty()||potion.potion().isEmpty())return null;
        String type=potion.potion().get().unwrapKey().map(k->k.location().getPath()).orElse("");
        String expected=switch(id){
            case "mending_sting","verdant_jolt"->"regeneration";
            case "scintilla_vitalis","lux_vitalis"->"healing";
            case "tree_viper","bushmaster","fer_de_lance"->"poison";
            case "pestis","black_bubo"->"weakness";
            case "vapours","melancholia","deathly_stupor"->"slowness";
            case "spicule_breach","spicule_rend"->"harming";
            case "ebonsight"->"night_vision";
            default->"";
        };
        return type.equals(expected)||type.equals("strong_"+expected)||type.equals("long_"+expected)?id:null;
    }
    // TODO(M72, authoring): migrate future authoring to the stable cosmicdungeon:d1_ability component.
    // Existing vanilla chest stacks are intentionally not rewritten. The compatibility path requires
    // canonical name, vanilla ammunition type and matching potion/star signature; anvil renaming into
    // a different ability is blocked. Commands/creative editors remain trusted authoring tools.
}
