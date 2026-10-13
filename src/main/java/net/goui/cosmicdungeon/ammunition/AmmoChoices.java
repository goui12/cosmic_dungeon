package net.goui.cosmicdungeon.ammunition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Bounded native stock order: offhand, selected hand, then slots0-35. Groups exact components, not names. */
public final class AmmoChoices {
    private AmmoChoices(){}
    public record Choice(int slot,ItemStack stack){}
    public static boolean weapon(ItemStack stack){return stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem;}
    public static boolean same(ItemStack a,ItemStack b){
        return a.isEmpty()?b.isEmpty():!b.isEmpty()&&ItemStack.isSameItemSameComponents(a,b);
    }
    public static List<Choice> collect(Player player,ItemStack weapon,Predicate<ItemStack> permitted){
        if(!weapon(weapon))return List.of();
        var item=(ProjectileWeaponItem)weapon.getItem();
        var choices=new ArrayList<Choice>(37);
        add(choices,40,player.getOffhandItem(),item.getSupportedHeldProjectiles(weapon),permitted);
        int selected=player.getInventory().getSelectedSlot();
        add(choices,selected,player.getMainHandItem(),item.getSupportedHeldProjectiles(weapon),permitted);
        for(int slot=0;slot<36;slot++)
            add(choices,slot,player.getInventory().getItem(slot),item.getAllSupportedProjectiles(weapon),permitted);
        return choices;
    }
    private static void add(List<Choice> choices,int slot,ItemStack stack,Predicate<ItemStack> supported,Predicate<ItemStack> permitted){
        if(stack.isEmpty()||!supported.test(stack)||!permitted.test(stack))return;
        for(var choice:choices)if(same(choice.stack,stack))return;
        choices.add(new Choice(slot,stack));
    }
    public static int index(List<Choice> choices,ItemStack wanted){
        for(int i=0;i<choices.size();i++)if(same(choices.get(i).stack,wanted))return i;
        return -1;
    }
    public static Choice select(List<Choice> choices,ItemStack wanted,boolean cycle){
        if(choices.isEmpty())return new Choice(-1,ItemStack.EMPTY);
        int index=index(choices,wanted);
        return choices.get(index<0?0:cycle?(index+1)%choices.size():index);
    }
}
