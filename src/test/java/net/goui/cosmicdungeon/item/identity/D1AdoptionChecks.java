package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.playerclass.d1.D1AmmunitionCatalog;
import net.goui.cosmicdungeon.playerclass.d1.D1AmmunitionCatalog.Facts;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Offline production-policy checks. Native ItemStack/menus remain licensed TEST acceptance. */
public final class D1AdoptionChecks {
    private static int checks;
    private static void check(boolean value,String label) {
        checks++; if(!value)throw new AssertionError(label);
    }
    private static Facts facts(String item,String marker,String name,String potion,int stars) {
        return new Facts(item,marker,name,potion,false,stars);
    }
    public static void main(String[] args) {
        check(D1AmmunitionCatalog.entries().size()==18,"D1-only ammunition inventory");
        for(var e:D1AmmunitionCatalog.entries()) {
            String potion=e.potionFamily().isEmpty()?"":"minecraft:"+e.potionFamily();
            var legacy=facts(e.baseItem(),null,e.name(),potion,e.stars());
            check(e.id().equals(D1AmmunitionCatalog.identify(legacy)),"canonical source name "+e.id());
            check(D1AmmunitionCatalog.canAdopt(legacy,e.id()),"explicit same-identity adoption "+e.id());
            check(e.id().equals(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),"Renamed by author",potion,e.stars()))),
                    "marker survives cosmetic name "+e.id());
            for(String invalid:List.of("", "future_ability", e.id().toUpperCase(Locale.ROOT))) {
                var marked=facts(e.baseItem(),invalid,e.name(),potion,e.stars());
                check(D1AmmunitionCatalog.identify(marked)==null,"invalid marker never falls back "+e.id());
                check(!D1AmmunitionCatalog.canAdopt(marked,e.id()),"never overwrites marker "+e.id());
            }
            check(D1AmmunitionCatalog.identify(facts("minecraft:stick",e.id(),e.name(),potion,e.stars()))==null,
                    "marker cannot turn other item into ammunition");
            check(D1AmmunitionCatalog.identify(facts("cosmicdungeon:shard_of_the_mad_strider",e.id(),e.name(),potion,e.stars()))==null,
                    "foreign registered item cannot impersonate D1 ammo");
            check(D1AmmunitionCatalog.identify(new Facts(e.baseItem(),e.id(),e.name(),potion,true,e.stars()))==null,
                    "custom effects rejected for vanilla adoption signatures");
            check(D1AmmunitionCatalog.bindingAllowed(e.id(),false,false,null,null,null),"unattuned authored stack allowed");
            for(String cls:e.classes()) {
                check(D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,cls,1,3),"D1 tier3 binding retained");
                check(D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,cls,1,4),"D1 tier4 binding retained");
                check(!D1AmmunitionCatalog.bindingAllowed(e.id(),true,false,cls,1,3),"partial binding held for review");
                check(!D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,cls,2,3),"D2 not reclassified");
                check(!D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,cls,1,1),"future/other tier not reclassified");
            }
            check(!D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,"metalmancer",1,3),"foreign class rejected");
            check(!D1AmmunitionCatalog.bindingAllowed(e.id(),true,true,null,1,3),"malformed class rejected");
            if(!potion.isEmpty()) {
                check(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),e.name(),"other:"+e.potionFamily(),0))==null,
                        "potion namespace retained");
                check(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),e.name(),"minecraft:water",0))==null,
                        "wrong potion cannot gain ability through marker");
                check(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),e.name(),"",0))==null,
                        "vanilla tipped arrow needs potion evidence");
            } else if(e.stars()>0) {
                check(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),e.name(),"",e.stars()-1))==null,
                        "wrong rocket payload rejected");
                check(D1AmmunitionCatalog.identify(facts(e.baseItem(),e.id(),e.name(),"",e.stars()+1))==null,
                        "oversized rocket payload rejected");
            }
        }
        var aliases=Map.of("Arrow of the Vapours","vapours","Arrow of Black Bubo","black_bubo",
                "Arrow of Melancholia","melancholia","Arrow of Deathly Stupor","deathly_stupor",
                "Veilpiercer","vielpiercer","Arrow of Mending Sting","mending_sting",
                "Spicule of Rend","spicule_rend","Spicule of Breach","spicule_breach");
        for(var alias:aliases.entrySet()) {
            var e=D1AmmunitionCatalog.find(alias.getValue());
            check(e.id().equals(D1AmmunitionCatalog.identify(facts(e.baseItem(),null,alias.getKey(),
                    e.potionFamily().isEmpty()?"":"minecraft:"+e.potionFamily(),0))),"saved alias retained "+alias.getKey());
        }
        for(String id:List.of("scintilla_vitalis","lux_vitalis","ebonsight","vielpiercer")) {
            check(id.equals(D1AmmunitionCatalog.identify(facts("cosmicdungeon:"+id,null,"","",0))),
                    "registered ID compatibility without potion component");
            check(D1AmmunitionCatalog.identify(facts("cosmicdungeon:"+id,"future",id,"",0))==null,"registered unknown marker blocked");
            check(D1AmmunitionCatalog.identify(facts("cosmicdungeon:"+id,"tree_viper",id,"",0))==null,"registered marker conflict blocked");
            check(!D1AmmunitionCatalog.canAdopt(facts("cosmicdungeon:"+id,null,"","",0),id),"no custom registry conversion");
        }
        for(String future:List.of("Cinderkiss","Cinderbight","Cinder Breeze","Whispered Cinder","Cinder Smash","Cinder Cleave")) {
            check(D1AmmunitionCatalog.identify(facts("minecraft:firework_rocket",null,future,"",4))==null,"no invented rocket alias");
        }
        var ordinary=facts("minecraft:tipped_arrow",null,"Ordinary poison arrow","minecraft:poison",0);
        check(D1AmmunitionCatalog.identify(ordinary)==null,"ordinary ammo unchanged");
        check(D1AmmunitionCatalog.canAdopt(ordinary,"tree_viper"),"developer explicit choice needs no display-name match");
        check(!D1AmmunitionCatalog.canAdopt(facts("minecraft:tipped_arrow",null,"Venom of the Bushmaster","minecraft:poison",0),"tree_viper"),
                "legacy identity cannot be silently replaced");
        check(!D1AmmunitionCatalog.canAdopt(ordinary,"future"),"unknown adoption rejected");

        check(D1LootCatalog.entries().size()==23,"named drop inventory");
        for(var e:D1LootCatalog.entries()) {
            var expected=D1LootSignatures.expected(e.id());
            check(expected!=null,"every named drop has explicit signature");
            check(D1LootSignatures.matches(e.id(),e.baseItem(),expected),"complete signature accepted");
            check(!D1LootSignatures.matches(e.id(),"minecraft:stick",expected),"wrong base rejected");
            var extra=new HashMap<>(expected);extra.put("minecraft:vanishing_curse",1);
            check(!D1LootSignatures.matches(e.id(),e.baseItem(),extra),"extra curse held for review");
            if(!expected.isEmpty()) {
                var key=expected.keySet().iterator().next();
                var wrong=new HashMap<>(expected);wrong.put(key,expected.get(key)+1);
                check(!D1LootSignatures.matches(e.id(),e.baseItem(),wrong),"wrong enchantment level rejected");
                wrong.remove(key);
                check(!D1LootSignatures.matches(e.id(),e.baseItem(),wrong),"missing enchantment rejected");
                wrong.put("other:"+key.substring("minecraft:".length()),expected.get(key));
                check(!D1LootSignatures.matches(e.id(),e.baseItem(),wrong),"foreign enchantment namespace rejected");
            }
        }
        check(!D1LootSignatures.matches("unknown","minecraft:bow",Map.of()),"unknown loot identity rejected");
        // Exact snapshot acceptance uses the same generic plan as production; these are native
        // NBT fixtures, NOT claims that a live ItemStack/menu or crash-persistent undo was tested.
        var before=new CompoundTag();before.putInt("count",19);
        var components=new CompoundTag();components.putString("custom_name","Private authored name");
        components.putInt("damage",7);components.putString("future:component","retain me");
        before.put("components",components);
        var after=before.copy();after.getCompoundOrEmpty("components").putString("cosmicdungeon:d1_ability","tree_viper");
        var plan=new ItemAuthoringPlan<>("token",100,before.copy(),after.copy());
        check(plan.accepts("token",99,true,before,false,CompoundTag::equals),"exact original accepted");
        for(String field:List.of("custom_name","future:component")) {
            var changed=before.copy();changed.getCompoundOrEmpty("components").putString(field,"changed");
            check(!plan.accepts("token",99,true,changed,false,CompoundTag::equals),"changed component blocks adoption");
        }
        var countChange=before.copy();countChange.putInt("count",18);
        check(!plan.accepts("token",99,true,countChange,false,CompoundTag::equals),"count change blocks adoption");
        check(!plan.accepts("token",99,false,before,false,CompoundTag::equals),"changed location or run blocks adoption");
        check(!plan.accepts("wrong",99,true,before,false,CompoundTag::equals),"wrong token blocked");
        check(!plan.accepts("token",100,true,before,false,CompoundTag::equals),"expiry blocked");
        plan.committed(false);
        check(!plan.accepts("token",99,true,before,false,CompoundTag::equals),"apply replay blocked");
        check(plan.accepts("token",99,true,after,true,CompoundTag::equals),"exact adopted stack allows undo");
        check(plan.image(true).equals(before),"undo retains count and unknown nested components");
        plan.committed(true);
        check(!plan.accepts("token",99,true,after,true,CompoundTag::equals),"undo replay blocked");
        System.out.println(checks+" D1 adoption checks passed");
    }
}
