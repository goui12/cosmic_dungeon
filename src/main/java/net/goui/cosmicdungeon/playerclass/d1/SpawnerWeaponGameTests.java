package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.ammunition.AmmoSelectionService;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Native block interaction and skeleton equipment behavior, with exact authored stack preservation. */
public final class SpawnerWeaponGameTests {
    private SpawnerWeaponGameTests(){}
    private static InteractionResult click(CosmicSpawnerGameTests.Fixture f,BlockPos pos){
        return f.player.gameMode.useItemOn(f.player,f.level,f.player.getMainHandItem(),InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
    }
    private static void pass(CosmicSpawnerGameTests.Fixture f,BlockPos pos,Item item,boolean developer,boolean shift){
        if(developer)RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DEVELOPER);
        else RankStore.get(f.level).clearRank(f.player.getUUID());
        f.player.setShiftKeyDown(shift);f.player.stopUsingItem();
        var weapon=new ItemStack(item);f.player.setItemInHand(InteractionHand.MAIN_HAND,weapon);
        f.player.getInventory().setItem(9,new ItemStack(Items.ARROW,3));
        var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);
        var before=be.saveWithoutMetadata(f.level.registryAccess());
        f.check(click(f,pos)==InteractionResult.PASS,"Ordinary spawner click passes through without consuming weapon use");
        f.check(f.player.gameMode.useItem(f.player,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction()
                &&f.player.isUsingItem(),"Native item fallback can draw the bow or charge the crossbow");
        f.player.stopUsingItem();
        f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Ordinary and non-Developer Shift clicks preserve exact preset data");
    }
    private static void breaking(CosmicSpawnerGameTests.Fixture f,BlockPos pos){
        f.player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.NORTH,f.level.getMaxY(),0);
        CosmicSpawnerFeedback.tick(f.player);
    }
    private static void advance(CosmicSpawnerGameTests.Fixture f,int ticks){
        f.player.tickCount+=ticks;CosmicSpawnerFeedback.tick(f.player);
    }
    private static long titles(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundSetTitleTextPacket).count();
    }
    public static void interactions(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-183)){
            Skeleton skeleton=null;
            try{
                var pos=f.spawner(0,1,2);var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);
                be.setSpawnerEntityId("minecraft:skeleton");
                for(var item:List.of(Items.BOW,Items.CROSSBOW)){
                    pass(f,pos,item,false,false);pass(f,pos,item,true,false);pass(f,pos,item,false,true);
                }
                var authored=new ItemStack(Items.BOW);authored.setDamageValue(17);
                authored.set(DataComponents.CUSTOM_NAME,Component.literal("Authored skeleton bow"));
                authored.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Preserve this lore"))));
                authored.enchant(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),3);
                CustomData.update(DataComponents.CUSTOM_DATA,authored,t->t.putString("future_bow_field","preserved"));
                f.player.setItemInHand(InteractionHand.MAIN_HAND,authored);
                RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DEVELOPER);f.player.setShiftKeyDown(true);
                var before=be.saveWithoutMetadata(f.level.registryAccess());
                Consumer<PlayerInteractEvent.RightClickBlock> veto=e->{
                    if(e.getEntity()==f.player&&e.getPos().equals(pos))e.setUseBlock(TriState.FALSE);
                };
                NeoForge.EVENT_BUS.addListener(veto);
                try{
                    click(f,pos);
                    f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Protection's block-use veto prevents developer copying");
                }finally{NeoForge.EVENT_BUS.unregister(veto);}
                f.check(click(f,pos).consumesAction(),"Developer Shift-click explicitly copies held equipment");
                f.check(ItemStack.matches(authored,f.player.getMainHandItem())
                        &&ItemStack.matches(authored,be.getSpawnerPreset().getEquipment(Slot.MAINHAND)),
                        "Explicit copy preserves exact bow components without consuming or replacing the held stack");
                f.check(f.player.gameMode.useItem(f.player,f.level,authored,InteractionHand.MAIN_HAND).consumesAction()
                        &&!f.player.isUsingItem(),"The client's immediate item fallback cannot draw the copied bow");
                f.player.setShiftKeyDown(false);
                f.check(f.player.gameMode.useItem(f.player,f.level,authored,InteractionHand.MAIN_HAND).consumesAction()
                        &&f.player.isUsingItem(),"Later ordinary weapon use is not blocked by the one-shot copy marker");
                f.player.stopUsingItem();
                var saved=be.saveWithoutMetadata(f.level.registryAccess());
                var loaded=new CosmicSpawnerBlockEntity(pos,be.getBlockState());
                loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),saved));
                skeleton=EntityType.SKELETON.create(f.level,EntitySpawnReason.COMMAND);
                f.check(skeleton!=null,"Native skeleton is created");
                skeleton.setPos(f.origin.add(-2,0,0));skeleton.setNoAi(true);
                loaded.getSpawnerPreset().applyToEntity(skeleton);f.level.addFreshEntity(skeleton);
                f.check(ItemStack.matches(authored,skeleton.getMainHandItem()),"Save/load/apply preserves the authored skeleton bow");
                f.check(skeleton.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof RangedBowAttackGoal),
                        "Native equipment callback installs the skeleton's ranged bow goal");
                skeleton.performRangedAttack(f.player,1);
                var owner=skeleton;
                var arrows=f.level.getEntitiesOfClass(Arrow.class,new AABB(f.origin,f.origin).inflate(4),a->a.getOwner()==owner);
                f.check(arrows.size()==1&&arrows.getFirst().getDeltaMovement().lengthSqr()>0,
                        "Equipped skeleton's native ranged attack spawns one moving owned arrow");

                var crossbow=new ItemStack(Items.CROSSBOW);
                crossbow.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.of(new ItemStack(Items.ARROW)));
                var charged=crossbow.copy();
                f.player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);f.player.setShiftKeyDown(true);
                f.check(click(f,pos).consumesAction(),"Developer can explicitly copy an already-loaded crossbow");
                f.player.gameMode.useItem(f.player,f.level,crossbow,InteractionHand.MAIN_HAND);
                f.check(ItemStack.matches(charged,crossbow)&&!f.player.isUsingItem()
                        &&f.level.getEntitiesOfClass(Arrow.class,new AABB(f.origin,f.origin).inflate(4),a->a.getOwner()==f.player).isEmpty(),
                        "Immediate copy fallback preserves the loaded projectile without firing or drawing");
                f.player.setShiftKeyDown(false);
                f.player.gameMode.useItem(f.player,f.level,crossbow,InteractionHand.MAIN_HAND);
                f.check(crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY).isEmpty()
                        &&f.level.getEntitiesOfClass(Arrow.class,new AABB(f.origin,f.origin).inflate(4),a->a.getOwner()==f.player).size()==1,
                        "Later ordinary use fires the retained crossbow load exactly once");

                RankStore.get(f.level).clearRank(f.player.getUUID());f.player.setShiftKeyDown(false);f.packets.clear();
                f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));
                breaking(f,pos);advance(f,60);f.check(titles(f)==0,"Holding a bow at a spawner never emits mining warnings");
                f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
                f.player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.CROSSBOW));
                breaking(f,pos);f.player.startUsingItem(InteractionHand.OFF_HAND);advance(f,60);
                f.check(titles(f)==0,"An actively used offhand crossbow suppresses mining warnings");
                f.player.stopUsingItem();f.player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                breaking(f,pos);advance(f,19);
                var renamed=f.player.getMainHandItem().copy();renamed.set(DataComponents.CUSTOM_NAME,Component.literal("Changed tool"));
                f.player.setItemInHand(InteractionHand.MAIN_HAND,renamed);advance(f,1);advance(f,19);
                f.check(titles(f)==0,"Changing the tool's components restarts the full warning interval");
                advance(f,1);f.check(titles(f)==1,"Uninterrupted wrong-tool mining still warns once after twenty ticks");
                f.player.getInventory().setItem(1,renamed.copy());f.player.getInventory().setSelectedSlot(1);
                advance(f,1);advance(f,19);f.check(titles(f)==1,"Changing hotbar slots restarts the warning interval even for matching tools");
                advance(f,1);f.check(titles(f)==2,"A new uninterrupted hold may warn once");
                advance(f,80);f.check(titles(f)==2,"The same hold cannot spam warnings");
                f.check(f.remains(pos),"All weapon and warning operations preserve the spawner");
            }finally{
                var owner=skeleton;
                f.level.getEntitiesOfClass(Arrow.class,new AABB(f.origin,f.origin).inflate(4),
                        a->a.getOwner()==f.player||owner!=null&&a.getOwner()==owner).forEach(Arrow::discard);
                if(owner!=null)owner.discard();
                f.player.stopUsingItem();f.player.setShiftKeyDown(false);
                RankStore.get(f.level).clearRank(f.player.getUUID());AmmoSelectionService.clear(f.player);
                clearPlayerCaches(f);
            }
        }
        helper.succeed();
    }
    private static void clearPlayerCaches(CosmicSpawnerGameTests.Fixture f){
        try{
            for(String name:List.of("stats","advancements")){
                var field=PlayerList.class.getDeclaredField(name);field.setAccessible(true);
                var map=(Map<?,?>)field.get(f.level.getServer().getPlayerList());
                if(map.get(f.player.getUUID()) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                map.remove(f.player.getUUID());
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Spawner weapon fixture cleanup",error);}
    }
}
