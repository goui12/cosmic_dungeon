package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

public final class SpawnerEquipmentGameTests {
    private SpawnerEquipmentGameTests(){}
    private static View view(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket)
                .map(p->((ClientboundCustomPayloadPacket)p).payload()).filter(p->p instanceof View).map(p->(View)p).reduce((a,b)->b).orElseThrow();
    }
    private static CosmicSpawnerBlockEntity setup(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DEVELOPER);
        var pos=f.spawner(0,1,3);var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);be.setSpawnerEntityId("minecraft:zombie");
        f.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(pos));return be;
    }
    private static void clear(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).clearRank(f.player.getUUID());
        SpawnerEditorService.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(f.player));
    }
    private static Edit item(String id,boolean advanced,Enchant... enchants){return new Edit(Origin.ITEM,"minecraft:"+id,advanced,List.of(enchants));}
    private static Enchant ench(String id,int level){return new Enchant("minecraft:"+id,level);}
    private static void apply(CosmicSpawnerGameTests.Fixture f,View v,SpawnerEquipmentDraft equipment){
        SpawnerEditorService.request(f.player,new Request(v.session(),Kind.APPLY,v.draft(),equipment));
    }
    public static void applyAndPreserve(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-151)){
            try{
                var be=setup(f);var held=new ItemStack(Items.DIAMOND_SWORD);held.setDamageValue(19);
                held.set(DataComponents.CUSTOM_NAME,Component.literal("Cameron's blade"));
                held.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Authored lore"))));
                held.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),2);
                f.player.setItemSlot(EquipmentSlot.MAINHAND,held.copy());
                var before=be.saveWithoutMetadata(f.level.registryAccess());
                SpawnerEditorService.open(f.player);var v=view(f);
                f.check(ItemStack.matches(v.held(),held),"Trusted held snapshot preserves all components");
                var draft=SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,new Edit(Origin.HELD,"",false,List.of(ench("sharpness",3))))
                        .with(Slot.OFFHAND,item("shield",false)).with(Slot.HEAD,item("diamond_helmet",false))
                        .with(Slot.CHEST,item("diamond_chestplate",false)).with(Slot.LEGS,item("diamond_leggings",false)).with(Slot.FEET,item("diamond_boots",false));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Draft/open never changes live block");
                apply(f,v,draft);f.check(view(f).status()==Status.APPLIED,"All six valid slots apply");
                f.check(ItemStack.matches(held,f.player.getMainHandItem()),"Copy/Apply neither grants nor consumes inventory gear");
                var expected=held.copy();expected.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),3);
                f.check(ItemStack.matches(expected,be.getSpawnerPreset().getEquipment(Slot.MAINHAND)),"Name/lore/damage and unrelated enchant survive");
                var saved=be.saveWithoutMetadata(f.level.registryAccess());
                var fresh=new CosmicSpawnerBlockEntity(be.getBlockPos(),be.getBlockState());
                fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),saved));
                var mob=new Zombie(EntityType.ZOMBIE,f.level);fresh.getSpawnerPreset().applyToEntity(mob);
                for(var slot:Slot.values())f.check(ItemStack.matches(be.getSpawnerPreset().getEquipment(slot),mob.getItemBySlot(slot.equipmentSlot)),"Reload applies "+slot.id+" to native mob");
                f.check(saved.getCompoundOrEmpty("SpawnerPreset").getIntOr("dropMask",-1)==0,"Equipment editing never claims drop settings");
                SpawnerEditorService.open(f.player);v=view(f);var unchanged=be.saveWithoutMetadata(f.level.registryAccess());
                SpawnerEditorService.request(f.player,new Request(v.session(),Kind.CANCEL,v.draft(),draft.with(Slot.MAINHAND,new Edit(Origin.CLEAR,"",false,List.of()))));
                f.check(unchanged.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Cancel discards equipment draft");
                SpawnerEditorService.open(f.player);v=view(f);f.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));
                apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,new Edit(Origin.HELD,"",false,List.of())));
                f.check(view(f).status()==Status.INVALID&&unchanged.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Changed held item cannot substitute for copied snapshot");
                mob.discard();helper.succeed();
            }finally{clear(f);}
        }
    }
    public static void validation(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-152)){
            try{
                var be=setup(f);SpawnerEditorService.open(f.player);var v=view(f);var before=be.saveWithoutMetadata(f.level.registryAccess());
                List<Edit> invalid=List.of(item("not_an_item",false),item("diamond_sword",false,ench("sharpness",6)),
                        item("diamond_sword",false,ench("protection",1)),item("diamond_sword",false,ench("sharpness",1),ench("smite",1)),
                        item("diamond_sword",true,ench("sharpness",256)),item("diamond_sword",true,ench("sharpness",-1)),
                        item("diamond_sword",true,ench("not_an_enchantment",1)),item("diamond_sword",true,ench("sharpness",1),ench("sharpness",2)));
                for(var edit:invalid){
                    apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,edit));
                    f.check(view(f).status()==Status.INVALID&&before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Server atomically rejects invalid equipment/enchantment");
                }
                apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.HEAD,item("diamond_sword",false)));
                f.check(view(f).status()==Status.INVALID&&before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Normal slot compatibility enforced");
                var advanced=item("diamond_sword",true,ench("sharpness",255),ench("smite",5),ench("protection",1));
                apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,advanced));
                f.check(view(f).status()==Status.APPLIED,"Bounded Advanced deliberate conflicts/incompatibility accepted");
                var stack=be.getSpawnerPreset().getEquipment(Slot.MAINHAND);var registry=f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                f.check(stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(registry.getOrThrow(Enchantments.SHARPNESS))==255,"Native component preserves supported maximum255");
                SpawnerEditorService.open(f.player);v=view(f);apply(f,v,SpawnerEquipmentDraft.unchanged());
                f.check(view(f).status()==Status.APPLIED&&ItemStack.matches(stack,be.getSpawnerPreset().getEquipment(Slot.MAINHAND)),"Untouched Advanced authored item survives Normal/no-op");
                SpawnerEditorService.open(f.player);v=view(f);
                var repaired=new Edit(Origin.KEEP,"",false,List.of(ench("sharpness",5),ench("smite",0),ench("protection",0)));
                apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,repaired));
                f.check(view(f).status()==Status.APPLIED,"Level-zero removal can restore a legal Normal item");
                SpawnerEditorService.open(f.player);v=view(f);apply(f,v,SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,new Edit(Origin.CLEAR,"",false,List.of())));
                f.check(view(f).status()==Status.APPLIED&&be.getSpawnerPreset().getEquipment(Slot.MAINHAND).isEmpty(),"Explicit clear persists as owned empty slot");
                helper.succeed();
            }finally{clear(f);}
        }
    }
}
