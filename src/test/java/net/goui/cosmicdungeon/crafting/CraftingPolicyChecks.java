package net.goui.cosmicdungeon.crafting;
import java.util.*;
public final class CraftingPolicyChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var defaults=CraftingRules.parse(true,CraftingRules.DEFAULT_PLAYERS,List.of());
        for(String id:List.of("minecraft:fermented_spider_eye","minecraft:magma_cream","minecraft:glistering_melon_slice")){
            check(defaults.player(id,"theurgist"),"Theurgist approved reagent");
            for(String other:List.of("judicator","bogatyr","dragoon","pyroclast","venefex","","developer")){
                check(!defaults.player(id,other),"No implicit wrong-class/developer bypass");
            }
            check(!defaults.player(id,null),"Unselected class denied");
            check(!defaults.automated(id),"Player permission is not machine permission");
        }
        for(String id:List.of("minecraft:cake","minecraft:bread","minecraft:sugar_from_honey_bottle","minecraft:iron_ingot","cosmicdungeon:hidebound_wolf_armor")){
            check(!defaults.known(id),"Unapproved conversions/D2 denied");
        }
        var independent=CraftingRules.parse(true,List.of("minecraft:cake|theurgist"),List.of("minecraft:magma_cream"));
        check(independent.known("minecraft:magma_cream") && independent.automated("minecraft:magma_cream"),"Explicit machine recipe");
        check(!independent.player("minecraft:magma_cream","theurgist"),"Automation is not player permission");
        check(!independent.automated("minecraft:cake"),"Class-owned recipe never attributed to a machine");
        check(!independent.known("minecraft:fermented_spider_eye"),"Replacing policy revokes old entry");
        check(defaults.player("minecraft:fermented_spider_eye","theurgist"),"Published old snapshot remains immutable");
        var many=CraftingRules.parse(true,List.of("minecraft:cake|theurgist","minecraft:cake|pyroclast","minecraft:bread|*"),List.of());
        check(many.player("minecraft:cake","theurgist")&&many.player("minecraft:cake","pyroclast"),"Multiple explicit classes");
        check(!many.player("minecraft:cake","dragoon")&&many.player("minecraft:bread",null),"Explicit all-player token");
        for(Object value:List.of("minecraft:*|theurgist","minecraft:cake|","cake|theurgist","Minecraft:cake|theurgist","minecraft:cake|theurgist|*","minecraft:cake",42)){
            check(!CraftingRules.playerEntry(value),"Malformed/wildcard entry rejected");
        }
        boolean rejected=false;
        try{CraftingRules.parse(true,List.of("minecraft:*|theurgist"),List.of());}catch(IllegalArgumentException ex){rejected=true;}
        check(rejected,"Bad snapshot cannot silently widen policy");
        rejected=false;
        try{CraftingRules.parse(true,Collections.nCopies(4097,"minecraft:cake|*"),List.of());}catch(IllegalArgumentException ex){rejected=true;}
        check(rejected,"Config work bound");
        var off=CraftingRules.parse(false,List.of(),List.of());
        check(off.player("minecraft:cake",null)&&off.automated("minecraft:cake"),"Explicit disable restores recipe access");
        var empty=CraftingRules.parse(true,List.of(),List.of());
        check(!empty.player("minecraft:cake","theurgist")&&!empty.automated("minecraft:cake"),"Empty lists deny production");
        System.out.println("Crafting policy checks passed: "+checks);
    }
}
