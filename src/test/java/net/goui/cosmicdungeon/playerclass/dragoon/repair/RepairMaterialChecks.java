package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import java.util.List;
public final class RepairMaterialChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(String raw:List.of("oak_planks","spruce_planks","cobblestone","leather","gold_ingot","copper_ingot","iron_ingot","diamond","netherite_ingot")){
            check(raw.equals(RepairMaterialRules.serviceKey("minecraft:"+raw,false,null,true,false)),"Approved unmarked "+raw);
            check(raw.equals(RepairMaterialRules.serviceKey("minecraft:"+raw,true,raw,true,false)),"Existing marked "+raw);
            check(RepairMaterialRules.serviceKey("minecraft:"+raw,true,null,true,false)==null,"Invalid marker never falls back");
            check(RepairMaterialRules.serviceKey("minecraft:"+raw,false,null,false,false)==null,"Damaged/enchanted rejected");
            check(RepairMaterialRules.serviceKey("minecraft:"+raw,false,null,true,true)==null,"Bound/provenanced supply rejected");
            check(RepairMaterialRules.serviceKey("anothermod:"+raw,false,null,true,false)==null,"Namespace matters");
        }
        for(String weapon:List.of("bow","crossbow","trident","mace")){
            check(RepairMaterialRules.serviceKey("minecraft:"+weapon,false,null,true,false)==null,"Ordinary weapon never a kit");
            check((weapon+"_repair_kit").equals(RepairMaterialRules.serviceKey("minecraft:"+weapon,true,weapon+"_repair_kit",true,false)),"Validated marked kit retained");
            check(RepairMaterialRules.serviceKey("minecraft:"+weapon,true,null,true,false)==null,"Malformed kit rejected");
        }
        for(String other:List.of("minecraft:birch_planks","minecraft:stone","minecraft:iron_block","minecraft:iron_sword","","iron_ingot")){
            check(RepairMaterialRules.serviceKey(other,false,null,true,false)==null,"No inferred repair substitutes");
        }
        check(RepairMaterialRules.serviceKey(null,false,null,true,false)==null,"Null safe");
        System.out.println("Repair material compatibility checks passed: "+checks);
    }
}
