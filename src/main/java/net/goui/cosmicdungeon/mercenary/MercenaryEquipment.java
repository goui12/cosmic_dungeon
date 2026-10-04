package net.goui.cosmicdungeon.mercenary;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
/** Stable chest order; transfer whole authored stacks without adding/changing components. */
public final class MercenaryEquipment {
    private MercenaryEquipment(){}
    public static EquipmentSlot preferred(ItemStack stack){
        var wearable=stack.get(DataComponents.EQUIPPABLE);
        if(wearable!=null&&Set.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET).contains(wearable.slot()))
            return wearable.slot();
        if(stack.is(Items.SHIELD))return EquipmentSlot.OFFHAND;
        if(stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem||stack.getItem() instanceof TridentItem
                ||stack.getItem() instanceof MaceItem||stack.is(net.minecraft.tags.ItemTags.SWORDS))return EquipmentSlot.MAINHAND;
        return null;
    }
    public record Plan(Map<EquipmentSlot,ItemStack> equipment,List<ItemStack> supplies){}
    public static Plan plan(List<ItemStack> stacks,UUID owner,String classId,int capacity){
        var equipment=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);
        var supplies=new ArrayList<ItemStack>();
        for(var stack:stacks){
            if(stack.isEmpty())continue;
            if(!net.goui.cosmicdungeon.item.identity.ClassItemOwnership.mayAcquire(stack,owner,classId))return null;
            var slot=preferred(stack);
            if(slot==EquipmentSlot.MAINHAND&&!net.goui.cosmicdungeon.playerclass.skill.ClassSkillRules.known(
                    classId,net.goui.cosmicdungeon.playerclass.skill.ClassSkills.weapon(stack)))slot=null;
            if(slot!=null&&!equipment.containsKey(slot))equipment.put(slot,stack.copy());
            else supplies.add(stack.copy());
        }
        return supplies.size()>capacity?null:new Plan(Map.copyOf(equipment),List.copyOf(supplies));
    }
    public static boolean equip(MercenaryEntity entity,List<net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity> chests){
        var stacks=new ArrayList<ItemStack>();
        for(var chest:chests)for(int i=0;i<chest.getContainerSize();i++)stacks.add(chest.getItem(i));
        var plan=plan(stacks,entity.contract().id(),entity.contract().classId(),entity.supplies().size());
        if(plan==null)return false;
        plan.equipment().forEach(entity::setItemSlot);
        for(int i=0;i<plan.supplies().size();i++)entity.supplies().set(i,plan.supplies().get(i));
        for(var chest:chests){chest.clearContent();chest.setChanged();}
        return true;
    }
}
