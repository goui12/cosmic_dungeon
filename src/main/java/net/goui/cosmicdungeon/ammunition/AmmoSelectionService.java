package net.goui.cosmicdungeon.ammunition;

import java.util.Map;
import java.util.WeakHashMap;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.network.AmmoPayloads;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.goui.cosmicdungeon.playerclass.d1.*;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkills;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Event-driven, transient selection; no saved tags, inventory polling, stock mutation or creative grants. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class AmmoSelectionService {
    private static final Map<ServerPlayer,State> STATES=new WeakHashMap<>();
    private static final class State {
        ItemStack selected=ItemStack.EMPTY;
        String context="";
        int hand=-1,slot=-1,lastCycle=Integer.MIN_VALUE,lastNotice=Integer.MIN_VALUE,lastDenied=Integer.MIN_VALUE;
        long revision;
        boolean initialized;
    }
    private AmmoSelectionService(){}
    private static int hand(ServerPlayer player,ItemStack weapon){
        if(player.getMainHandItem()==weapon)return 0;
        if(player.getOffhandItem()==weapon)return 1;
        return -1;
    }
    private static boolean usable(ServerPlayer p,ItemStack weapon){
        return p.isAlive()&&!p.isSpectator()&&AmmoChoices.weapon(weapon)&&ClassItemEquipmentGuard.canUse(p,weapon);
    }
    public static boolean permitted(ServerPlayer p,ItemStack ammo){
        if(ammo.isEmpty()||!ClassItemEquipmentGuard.canUse(p,ammo))return false;
        String id=D1AbilityIdentity.identify(ammo);
        if(ammo.has(ModDataComponents.D1_ABILITY.get())&&id==null)return false;
        if(D1ProjectileAccess.permission(p,ammo,id)==D1CombatRules.Ammunition.DENIED)return false;
        if(ClassItemUtil.hasAnyAttunementMetadata(ammo)){
            var run=ClassSkills.run(p);
            if(!ClassItemUtil.hasCompleteValidAttunement(ammo)
                    ||!ClassData.getClassId(p).equals(ClassItemUtil.getClassAttunement(ammo))
                    ||run==null||!run.dungeonId().equals("dungeon_"+ClassItemUtil.getDungeon(ammo)))return false;
        }
        return true;
    }
    private static State state(ServerPlayer p,int hand){
        var state=STATES.computeIfAbsent(p,ignored->new State());
        var run=ClassSkills.run(p);
        String context=p.level().dimension().location()+"|"+ClassData.getClassId(p)+"|"+(run==null?0:run.runId());
        if(!context.equals(state.context)||state.hand!=hand){
            state.selected=ItemStack.EMPTY;state.initialized=false;state.context=context;state.hand=hand;
        }
        return state;
    }
    /** Native callers receive the real inventory reference, so native consumption remains authoritative. */
    public static ItemStack resolve(ServerPlayer p,ItemStack weapon,ItemStack nativeChoice,boolean cycle){
        int hand=hand(p,weapon);
        if(hand<0||!usable(p,weapon))return ItemStack.EMPTY;
        var state=state(p,hand);
        var choices=AmmoChoices.collect(p,weapon,stack->permitted(p,stack));
        ItemStack wanted=state.initialized?state.selected:nativeChoice;
        var choice=AmmoChoices.select(choices,wanted,cycle);
        if(choice.stack().isEmpty()&&p.hasInfiniteMaterials()){
            var creative=((ProjectileWeaponItem)weapon.getItem()).getDefaultCreativeAmmo(p,weapon);
            if(permitted(p,creative))choice=new AmmoChoices.Choice(-1,creative);
        }
        boolean changed=!state.initialized||!AmmoChoices.same(state.selected,choice.stack());
        boolean fallback=state.initialized&&!state.selected.isEmpty()&&AmmoChoices.index(choices,state.selected)<0;
        boolean reminder=cycle&&(long)p.tickCount-state.lastNotice>=20;
        if(changed||state.slot!=choice.slot()||reminder){
            int notice=changed||reminder?(choice.stack().isEmpty()?2:fallback?3:1):0;
            if(notice!=0)state.lastNotice=p.tickCount;
            state.selected=choice.stack().copyWithCount(choice.stack().isEmpty()?0:1);state.initialized=true;state.slot=choice.slot();
            PacketDistributor.sendToPlayer(p,new AmmoPayloads.View(++state.revision,hand,p.level().dimension().location(),
                    ClassData.getClassId(p),choice.slot(),state.selected,notice));
        }
        return choice.stack();
    }
    public static void cycle(ServerPlayer p,AmmoPayloads.Cycle request){
        var weapon=p.getItemInHand(request.hand()==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        if(!usable(p,weapon)||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()
                ||p.getInventory().getSelectedSlot()!=request.hotbar()
                ||!ItemStack.isSameItemSameComponents(weapon,request.weapon()))return;
        var state=state(p,request.hand());
        if((long)p.tickCount-state.lastCycle<4)return;
        state.lastCycle=p.tickCount;
        // First cycle starts after the same native-priority type that ordinary loading would select.
        var choices=AmmoChoices.collect(p,weapon,stack->permitted(p,stack));
        ItemStack first=choices.isEmpty()?ItemStack.EMPTY:choices.getFirst().stack();
        resolve(p,weapon,first,true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void projectile(LivingGetProjectileEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&AmmoChoices.weapon(event.getProjectileWeaponItemStack()))
            event.setProjectileItemStack(resolve(p,event.getProjectileWeaponItemStack(),event.getProjectileItemStack(),false));
    }
    public static boolean loadedAllowed(ServerPlayer p,ItemStack weapon){
        boolean allowed=usable(p,weapon)&&weapon.getOrDefault(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.EMPTY).getItems().stream().allMatch(ammo->permitted(p,ammo));
        if(!allowed){
            var state=state(p,Math.max(0,hand(p,weapon)));
            if((long)p.tickCount-state.lastDenied>=20){
                state.lastDenied=p.tickCount;
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Loaded ammunition is unavailable to your current class or run."),true);
            }
        }
        return allowed;
    }
    public static void afterDraw(ServerPlayer player,ItemStack weapon){
        resolve(player,weapon,ItemStack.EMPTY,false);
    }
    /** Invalidate across round trips even when no weapon is queried in the intervening context. */
    public static void invalidate(ServerPlayer player){
        var state=STATES.get(player);
        if(state!=null){state.context="";state.initialized=false;}
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer p)invalidate(p);
    }
    public static void clear(ServerPlayer player){STATES.remove(player);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)clear(p);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(event.getOriginal() instanceof ServerPlayer p)clear(p);
        if(event.getEntity() instanceof ServerPlayer p)clear(p);
    }
}
