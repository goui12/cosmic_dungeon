package net.goui.cosmicdungeon.playerclass.d1;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.*;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

/** Actual native bow use/draw/spawn preserves authored rockets and normal ammunition consumption. */
public final class BowRocketGameTests {
    private BowRocketGameTests(){}
    private static ItemStack authored(String name,int stars,int flight){
        var rocket=new ItemStack(Items.FIREWORK_ROCKET,3);
        rocket.set(DataComponents.CUSTOM_NAME,Component.literal(name));
        rocket.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Authored rocket lore"))));
        var burst=new FireworkExplosion(FireworkExplosion.Shape.STAR,IntList.of(0x123456,0xabcdef),
                IntList.of(0x445566),true,true);
        rocket.set(DataComponents.FIREWORKS,new Fireworks(flight,Collections.nCopies(stars,burst)));
        CustomData.update(DataComponents.CUSTOM_DATA,rocket,t->t.putString("future_rocket_field","preserved"));
        return rocket;
    }
    private static List<Projectile> projectiles(CosmicSpawnerGameTests.Fixture f){
        return f.level.getEntitiesOfClass(Projectile.class,new AABB(f.origin,f.origin).inflate(4),
                p->p.getOwner()==f.player);
    }
    private static ItemStack bow(CosmicSpawnerGameTests.Fixture f,boolean infinity){
        var bow=new ItemStack(Items.BOW);
        if(infinity)bow.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.INFINITY),1);
        return bow;
    }
    private static Projectile fire(CosmicSpawnerGameTests.Fixture f,ItemStack bow,ItemStack ammo,boolean offhand){
        f.player.stopUsingItem();f.player.getInventory().clearContent();
        projectiles(f).forEach(Projectile::discard);
        f.player.setItemInHand(InteractionHand.MAIN_HAND,bow);
        if(offhand)f.player.setItemInHand(InteractionHand.OFF_HAND,ammo);
        else f.player.getInventory().setItem(9,ammo);
        f.check(bow.use(f.level,f.player,InteractionHand.MAIN_HAND).consumesAction()
                &&f.player.isUsingItem(),"Native bow starts drawing the actual available ammunition");
        boolean fired=bow.getItem().releaseUsing(bow,f.level,f.player,bow.getUseDuration(f.player)-20);
        f.player.stopUsingItem();
        var projectiles=projectiles(f);
        f.check(fired&&projectiles.size()==1,"Full native draw spawns exactly one projectile");
        f.check(bow.getDamageValue()==1,"Native bow durability remains one per shot");
        return projectiles.getFirst();
    }
    private static void rocket(CosmicSpawnerGameTests.Fixture f,ItemStack ammo,boolean infinity,boolean offhand){
        var before=ammo.copy();var bow=bow(f,infinity);
        var projectile=fire(f,bow,ammo,offhand);
        f.check(projectile instanceof FireworkRocketEntity,"Rocket ammunition creates a rocket instead of a fallback arrow");
        var rocket=(FireworkRocketEntity)projectile;
        f.check(rocket.getItem().getCount()==1&&ItemStack.isSameItemSameComponents(before,rocket.getItem()),
                "Fired rocket retains exact name, lore, flight, explosion and unknown components");
        f.check(ammo.getCount()==before.getCount()-1&&ItemStack.isSameItemSameComponents(before,ammo),
                "Native draw consumes one rocket even with Infinity, preserving every remaining component");
        f.check(rocket.getOwner()==f.player&&rocket.isShotAtAngle(),
                "Rocket retains the shooter and native aimed-flight behavior");
    }
    public static void loading(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-181)){
            try{
                rocket(f,authored("Cinderkiss",1,3),false,false);
                rocket(f,authored("Cinderbite",4,2),true,false);
                rocket(f,authored("Cindermaul",5,1),false,true);
                rocket(f,new ItemStack(ModItems.CINDERBITE.get(),3),false,false);
                rocket(f,new ItemStack(ModItems.CINDERMAUL.get(),3),true,false);
                var arrows=new ItemStack(Items.ARROW,3);
                f.check(fire(f,bow(f,false),arrows,false) instanceof AbstractArrow&&arrows.getCount()==2,
                        "Ordinary arrows still follow native projectile creation and consumption");
                arrows=new ItemStack(Items.ARROW,3);
                f.check(fire(f,bow(f,true),arrows,false) instanceof AbstractArrow&&arrows.getCount()==3,
                        "Infinity still preserves native ordinary arrows while rockets remain consumable");
            }finally{
                f.player.stopUsingItem();projectiles(f).forEach(Projectile::discard);
                clearPlayerCaches(f);
            }
        }
        helper.succeed();
    }
    private static void clearPlayerCaches(CosmicSpawnerGameTests.Fixture f){
        // Native bow awardStat lazily creates these owned fixture caches; remove only this UUID.
        try{
            for(String name:List.of("stats","advancements")){
                var field=PlayerList.class.getDeclaredField(name);field.setAccessible(true);
                var map=(Map<?,?>)field.get(f.level.getServer().getPlayerList());
                if(map.get(f.player.getUUID()) instanceof net.minecraft.server.PlayerAdvancements progress)
                    progress.stopListening();
                map.remove(f.player.getUUID());
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Bow fixture cleanup",error);}
    }
}
