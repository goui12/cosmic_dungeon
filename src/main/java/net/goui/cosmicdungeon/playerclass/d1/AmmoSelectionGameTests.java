package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.ammunition.*;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.goui.cosmicdungeon.network.AmmoPayloads;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

/** Actual stock, native draw and loaded-projectile authority; no selection persistence or invented inventory. */
public final class AmmoSelectionGameTests {
    private AmmoSelectionGameTests(){}
    private static AmmoPayloads.View view(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket)
                .map(p->((ClientboundCustomPayloadPacket)p).payload()).filter(p->p instanceof AmmoPayloads.View)
                .map(p->(AmmoPayloads.View)p).reduce((a,b)->b).orElseThrow();
    }
    private static long views(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket c&&c.payload() instanceof AmmoPayloads.View).count();
    }
    private static void cycle(CosmicSpawnerGameTests.Fixture f,ItemStack weapon){
        f.player.tickCount+=5;
        AmmoSelectionService.cycle(f.player,new AmmoPayloads.Cycle(0,f.player.getInventory().getSelectedSlot(),weapon.copyWithCount(1)));
    }
    private static ItemStack named(Item item,String name,int count){
        var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;
    }
    private static void reset(CosmicSpawnerGameTests.Fixture f){
        f.player.stopUsingItem();f.player.getInventory().clearContent();AmmoSelectionService.clear(f.player);f.packets.clear();
    }
    public static void selection(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-182)){
            try{
                reset(f);var bow=new ItemStack(Items.BOW);f.player.setItemInHand(InteractionHand.MAIN_HAND,bow);
                var arrow=new ItemStack(Items.ARROW,4);var spectral=new ItemStack(Items.SPECTRAL_ARROW,3);
                var poison=PotionContents.createItemStack(Items.TIPPED_ARROW,Potions.POISON);poison.setCount(2);
                var longPoison=PotionContents.createItemStack(Items.TIPPED_ARROW,Potions.LONG_POISON);
                var named=arrow.copy();named.set(DataComponents.CUSTOM_NAME,Component.literal("Authored arrow"));
                var unknown=named.copy();CustomData.update(DataComponents.CUSTOM_DATA,unknown,t->t.putInt("future",17));
                f.player.setItemInHand(InteractionHand.OFF_HAND,arrow);
                var inventory=f.player.getInventory();
                inventory.setItem(9,arrow.copy());inventory.setItem(10,spectral);inventory.setItem(11,poison);
                inventory.setItem(12,longPoison);inventory.setItem(13,named);inventory.setItem(14,unknown);
                var choices=AmmoChoices.collect(f.player,bow,stack->AmmoSelectionService.permitted(f.player,stack));
                f.check(choices.size()==6&&choices.getFirst().stack()==arrow&&choices.getFirst().slot()==40,
                        "Exact component variants remain distinct; duplicate stacks group in deterministic native stock order");
                f.check(f.player.getProjectile(bow)==arrow,"First resolve returns the actual offhand stock reference");
                long count=views(f);
                for(int i=0;i<20;i++)f.player.getProjectile(bow);
                f.check(views(f)==count,"Unchanged repeated native ammo queries produce no packet spam");
                for(var expected:List.of(spectral,poison,longPoison,named,unknown,arrow)){
                    cycle(f,bow);
                    f.check(f.player.getProjectile(bow)==expected,"R cycles every legal exact variant and wraps deterministically");
                }
                long revision=view(f).revision();count=views(f);
                AmmoSelectionService.dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                        f.player,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
                AmmoSelectionService.dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                        f.player,net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.OVERWORLD));
                f.check(f.player.getProjectile(bow)==arrow&&views(f)==count+1&&view(f).revision()>revision,
                        "Dimension out-and-back without intermediate weapon queries still refreshes the client view");
                revision=view(f).revision();count=views(f);
                ClassNbtUtil.setClassId(f.player,"venefex");ClassNbtUtil.setClassId(f.player,"pyroclast");
                f.check(f.player.getProjectile(bow)==arrow&&views(f)==count+1&&view(f).revision()>revision,
                        "Class out-and-back also invalidates transient selection without resetting revision ordering");
                var raw=io.netty.buffer.Unpooled.buffer();
                try{
                    var buf=new net.minecraft.network.RegistryFriendlyByteBuf(raw,f.level.registryAccess());
                    var packet=new AmmoPayloads.Cycle(0,inventory.getSelectedSlot(),bow);
                    AmmoPayloads.Cycle.CODEC.encode(buf,packet);var decoded=AmmoPayloads.Cycle.CODEC.decode(buf);
                    f.check(decoded.hand()==0&&decoded.hotbar()==inventory.getSelectedSlot()&&ItemStack.matches(bow,decoded.weapon()),
                            "Registry-aware request wire retains exact weapon snapshot");
                    var packetView=new AmmoPayloads.View(75,0,f.level.dimension().location(),"pyroclast",14,unknown,1);
                    AmmoPayloads.View.CODEC.encode(buf,packetView);var decodedView=AmmoPayloads.View.CODEC.decode(buf);
                    f.check(decodedView.revision()==75&&decodedView.slot()==14&&AmmoChoices.same(unknown,decodedView.selected())&&buf.readableBytes()==0,
                            "Registry-aware view wire preserves unknown authored item components");
                }finally{raw.release();}
                var snapshot=choices.stream().map(c->c.stack().copy()).toList();
                cycle(f,bow); // spectral
                for(int i=0;i<choices.size();i++)f.check(ItemStack.matches(snapshot.get(i),choices.get(i).stack()),
                        "Cycling never consumes, rewrites or creates stock");
                var request=new AmmoPayloads.Cycle(0,inventory.getSelectedSlot(),bow);
                count=views(f);AmmoSelectionService.cycle(f.player,request);
                f.check(views(f)==count&&f.player.getProjectile(bow)==spectral,"Repeated request in same tick is throttled");
                inventory.setItem(10,ItemStack.EMPTY);inventory.setItem(15,spectral);count=views(f);
                f.check(f.player.getProjectile(bow)==spectral&&views(f)==count+1&&view(f).slot()==15&&view(f).notice()==0,
                        "Moving the selected exact stack silently refreshes its client slot");
                spectral.setCount(0);
                f.check(f.player.getProjectile(bow)==arrow&&view(f).notice()==3,"Exhaustion auto-switches and announces the fallback");
                // Stale weapon/hotbar/menu requests must not alter selection.
                count=views(f);f.player.tickCount+=5;
                AmmoSelectionService.cycle(f.player,new AmmoPayloads.Cycle(0,(inventory.getSelectedSlot()+1)%9,bow));
                AmmoSelectionService.cycle(f.player,new AmmoPayloads.Cycle(0,inventory.getSelectedSlot(),new ItemStack(Items.CROSSBOW)));
                f.player.containerMenu.setCarried(new ItemStack(Items.STONE));
                AmmoSelectionService.cycle(f.player,request);f.player.containerMenu.setCarried(ItemStack.EMPTY);
                f.check(views(f)==count&&f.player.getProjectile(bow)==arrow,"Stale or carried-item requests fail closed");
                reset(f);f.player.setItemInHand(InteractionHand.MAIN_HAND,bow);
                var attuned=named(Items.ARROW,"Authored attuned arrow",2);
                ClassItemUtil.attune(attuned,"pyroclast",1,1,0);
                ClassItemOwnership.bind(attuned,f.player.getUUID(),"pyroclast");
                inventory.setItem(9,attuned);
                f.check(f.player.getProjectile(bow)==attuned,"Matching class, owner and active dungeon admits real attuned stock");
                var original=attuned.copy();
                CustomData.update(DataComponents.CUSTOM_DATA,attuned,t->t.putString(ClassItemOwnership.KEY,UUID.randomUUID().toString()));
                f.check(f.player.getProjectile(bow).isEmpty(),"Foreign ownership is revalidated at actual projectile use");
                inventory.setItem(9,original);var run=f.runs.remove(f.id);
                f.check(f.player.getProjectile(bow).isEmpty(),"Ended run rejects still-stocked attuned ammunition");
                f.runs.put(f.id,run);original.set(ModDataComponents.CLASS_ATTUNEMENT.get(),"venefex");
                f.check(f.player.getProjectile(bow).isEmpty(),"Current class restriction is authoritative");
                original.set(ModDataComponents.CLASS_ATTUNEMENT.get(),"pyroclast");
                original.set(ModDataComponents.CLASS_ITEM_DUNGEON.get(),2);
                f.check(f.player.getProjectile(bow).isEmpty(),"Wrong dungeon attunement fails closed without rewriting it");
                inventory.setItem(9,ItemStack.EMPTY);f.player.tickCount+=20;cycle(f,bow);
                f.check(view(f).notice()==2&&view(f).selected().isEmpty(),"No stock reports out of ammo on a bounded explicit retry");
                count=views(f);for(int i=0;i<20;i++)f.player.getProjectile(bow);
                f.check(views(f)==count,"Empty repeated queries remain silent after the notice");
                helper.succeed();
            }finally{AmmoSelectionService.clear(f.player);}
        }
    }
    private static List<Projectile> shots(CosmicSpawnerGameTests.Fixture f){
        return f.level.getEntitiesOfClass(Projectile.class,new AABB(f.origin,f.origin).inflate(6),p->p.getOwner()==f.player);
    }
    private static void charge(CosmicSpawnerGameTests.Fixture f,ItemStack bow){
        f.check(bow.use(f.level,f.player,InteractionHand.MAIN_HAND).consumesAction()&&f.player.isUsingItem(),
                "Native crossbow begins charging with selected stock");
        bow.getItem().onUseTick(f.level,f.player,bow,bow.getUseDuration(f.player)-CrossbowItem.getChargeDuration(bow,f.player));
        f.player.stopUsingItem();
        f.check(CrossbowItem.isCharged(bow),"Native charge completes and owns loaded projectile components");
    }
    public static void crossbows(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-184)){
            try{
                int variant=0;
                for(var name:List.of("Cinderkiss","Cinderbite","Cindermaul")){
                    reset(f);shots(f).forEach(Projectile::discard);
                    var bow=new ItemStack(Items.CROSSBOW);
                    boolean multishot=variant==2;
                    if(multishot)bow.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT),1);
                    f.player.setItemInHand(InteractionHand.MAIN_HAND,bow);
                    var rocket=named(Items.FIREWORK_ROCKET,name,2);
                    rocket.set(DataComponents.FIREWORKS,new Fireworks(1+variant,List.of(FireworkExplosion.DEFAULT)));
                    rocket.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Keep authored visuals"))));
                    CustomData.update(DataComponents.CUSTOM_DATA,rocket,t->t.putString("future","preserved"));
                    var original=rocket.copyWithCount(1);
                    if(variant==1)f.player.setItemInHand(InteractionHand.OFF_HAND,rocket);
                    else f.player.getInventory().setItem(9,rocket);
                    charge(f,bow);
                    var charged=bow.get(DataComponents.CHARGED_PROJECTILES);
                    int expected=multishot?3:1;
                    f.check(rocket.getCount()==1&&charged.getItems().size()==expected,
                            "Native charge consumes one rocket including Multishot");
                    f.check(ItemStack.isSameItemSameComponents(charged.getItems().getFirst(),original),
                            "Charged rocket preserves authored components");
                    var arrow=new ItemStack(Items.SPECTRAL_ARROW,4);f.player.getInventory().setItem(10,arrow);
                    cycle(f,bow);
                    f.check(f.player.getProjectile(bow)==arrow&&bow.get(DataComponents.CHARGED_PROJECTILES).equals(charged),
                            "Cycling a charged crossbow selects next load without changing its present load");
                    ((CrossbowItem)bow.getItem()).performShooting(f.level,f.player,InteractionHand.MAIN_HAND,bow,1.6F,0,null);
                    var shots=shots(f);
                    f.check(shots.size()==expected&&shots.stream().allMatch(p->p instanceof FireworkRocketEntity),
                            "Native crossbow fires all loaded authored rockets");
                    var extra=original.copy();extra.set(DataComponents.INTANGIBLE_PROJECTILE,net.minecraft.util.Unit.INSTANCE);
                    long exact=shots.stream().filter(p->p instanceof FireworkRocketEntity r&&ItemStack.isSameItemSameComponents(original,r.getItem())).count();
                    long intangible=shots.stream().filter(p->p instanceof FireworkRocketEntity r&&ItemStack.isSameItemSameComponents(extra,r.getItem())).count();
                    f.check(exact==1&&intangible==expected-1,
                            "Authored payload survives; only native Multishot extras carry the intangible marker");
                    f.check(bow.getDamageValue()==3*expected&&arrow.getCount()==4&&!CrossbowItem.isCharged(bow),
                            "Native rocket wear, charged clearing and next-ammo stock remain intact");
                    variant++;
                }
                reset(f);shots(f).forEach(Projectile::discard);
                var bow=new ItemStack(Items.CROSSBOW);f.player.setItemInHand(InteractionHand.MAIN_HAND,bow);
                var attuned=named(Items.FIREWORK_ROCKET,"Cinderkiss",2);
                ClassItemUtil.attune(attuned,"pyroclast",1,1,0);ClassItemOwnership.bind(attuned,f.player.getUUID(),"pyroclast");
                f.player.getInventory().setItem(9,attuned);charge(f,bow);
                var loaded=bow.get(DataComponents.CHARGED_PROJECTILES);var run=f.runs.remove(f.id);
                ((CrossbowItem)bow.getItem()).performShooting(f.level,f.player,InteractionHand.MAIN_HAND,bow,1.6F,0,null);
                f.check(shots(f).isEmpty()&&bow.get(DataComponents.CHARGED_PROJECTILES).equals(loaded)&&bow.getDamageValue()==0,
                        "Run loss between charge and fire denies shooting while preserving the load and durability");
                f.runs.put(f.id,run);
                ((CrossbowItem)bow.getItem()).performShooting(f.level,f.player,InteractionHand.MAIN_HAND,bow,1.6F,0,null);
                f.check(shots(f).size()==1&&!CrossbowItem.isCharged(bow),"Restored valid authority can fire the preserved load once");
                helper.succeed();
            }finally{
                f.player.stopUsingItem();shots(f).forEach(Projectile::discard);AmmoSelectionService.clear(f.player);
                try{
                    for(String name:List.of("stats","advancements")){
                        var field=PlayerList.class.getDeclaredField(name);field.setAccessible(true);
                        var map=(Map<?,?>)field.get(f.level.getServer().getPlayerList());
                        if(map.get(f.player.getUUID()) instanceof net.minecraft.server.PlayerAdvancements a)a.stopListening();
                        map.remove(f.player.getUUID());
                    }
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
        }
    }
}
