package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** D27 override: zero permanent; positive minutes count loaded, non-archived companion ticks. */
public final class BogatyrDuration {
    static final String ACTIVE_TICKS="cosmicdungeon.bogatyr_active_ticks";
    private BogatyrDuration(){}
    public static void tick(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level)||!wolf.isAlive()||!wolf.isTame()
                ||!BogatyrWolfEvents.managed(wolf)||BogatyrRecovery.held(wolf))return;
        int minutes=Config.WOLF_DURATION_MINUTES.get();
        if(minutes==0)return;
        long elapsed=WolfBehaviourRules.activeTick(wolf.getPersistentData().getLongOr(ACTIVE_TICKS,0),minutes,false);
        wolf.getPersistentData().putLong(ACTIVE_TICKS,elapsed);
        if(!WolfBehaviourRules.expired(elapsed,minutes)||wolf.tickCount%Config.WOLF_EXPIRY_POLL_TICKS.get()!=0)return;
        var owner=BogatyrCompanions.owner(wolf);if(owner==null)return;
        // Return exact vanilla body armor at the wolf's location, preserving dungeon/escrow boundaries.
        // A rejected entity insertion leaves the companion and its armor intact for the next attempt.
        var armor=wolf.getBodyArmorItem().copy();
        if(!armor.isEmpty()){
            var dropped=new ItemEntity(level,wolf.getX(),wolf.getY(),wolf.getZ(),armor.copy());
            dropped.setTarget(owner);dropped.setDefaultPickUpDelay();
            wolf.setBodyArmorItem(ItemStack.EMPTY);
            try{
                if(!level.addFreshEntity(dropped)){wolf.setBodyArmorItem(armor);return;}
            }catch(RuntimeException error){
                if(dropped.isAddedToLevel())dropped.discard();
                wolf.setBodyArmorItem(armor);
                com.mojang.logging.LogUtils.getLogger().warn("Timed wolf armor return rejected for "+wolf.getUUID(),error);
                return;
            }
        }
        long run=wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);
        wolf.setTarget(null);wolf.getNavigation().stop();wolf.stopBeingAngry();
        wolf.setOwnerReference(null);wolf.setTame(false,true);wolf.setOrderedToSit(false);
        wolf.getPersistentData().remove(BogatyrWolfEvents.RUN);
        wolf.getPersistentData().remove(BogatyrWolfEvents.OWNER);
        wolf.getPersistentData().remove(ACTIVE_TICKS);
        // Entity.getType erases the living bound; this receiver is already verified as a Wolf.
        @SuppressWarnings("unchecked")
        var wolfType=(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>)wolf.getType();
        var defaults=DefaultAttributes.getSupplier(wolfType);
        if(defaults!=null)for(var attribute:java.util.List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,
                Attributes.MOVEMENT_SPEED,Attributes.FOLLOW_RANGE)){
            var instance=wolf.getAttribute(attribute);
            if(instance!=null&&defaults.hasAttribute(attribute))instance.setBaseValue(defaults.getBaseValue(attribute));
        }
        wolf.setHealth(Math.min(wolf.getHealth(),wolf.getMaxHealth()));
        BogatyrCompanionData.get(level.getServer()).retired(BogatyrIdentity.id(wolf),owner);
        D1RunData.get(level.getServer()).removeUnique(run,"wolves:"+owner,BogatyrIdentity.id(wolf).toString());
        var player=level.getServer().getPlayerList().getPlayer(owner);
        if(player!=null)player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Your timed wolf companion has been released. Its armor, if worn, is an owner-only drop at its location."));
    }
}
