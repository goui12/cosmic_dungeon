package net.goui.cosmicdungeon.playerclass.bogatyr;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/** Wolf Internal 2026-04-25; uses authored vanilla spawn eggs, armor, bones and meat. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class BogatyrWolfEvents {
    static final String RUN="cosmicdungeon.bogatyr_run", OWNER="cosmicdungeon.bogatyr_owner";
    private BogatyrWolfEvents(){}
    public static boolean managed(Wolf wolf){return !net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf)
            && wolf.getPersistentData().getLongOr(RUN,0)>0;}
    static java.util.Optional<net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.RunRecord> currentRun(ServerLevel level){
        return net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(level.getServer()).findRunForInstanceDimension(level.dimension())
                .filter(r->r.stateEnum()==net.goui.cosmicdungeon.dungeon.DungeonRunState.ACTIVE)
                .filter(r->!r.dungeonId().equals("dungeon_1")||!D1RunData.get(level.getServer()).sealed(r.runId()));
    }
    private static boolean bogatyr(ServerPlayer player){return "bogatyr".equals(ClassData.getClassId(player));}
    public static boolean owned(Wolf wolf) {
        return !net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf) && wolf.isTame() && (managed(wolf) || wolf.getPersistentData().contains(OWNER)
                || wolf.getOwner() instanceof ServerPlayer player && bogatyr(player));
    }
    public static boolean isBogatyrWolfEgg(net.minecraft.world.entity.LivingEntity source,
            net.minecraft.world.item.ItemStack stack) {
        return source instanceof ServerPlayer player && bogatyr(player)
                && !net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(player)
                && stack != null && stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem egg
                && egg.spawnsEntity(stack, net.minecraft.world.entity.EntityType.WOLF);
    }
    public static void tameEgg(net.minecraft.world.entity.LivingEntity source, net.minecraft.world.entity.Entity entity) {
        if (!(source instanceof ServerPlayer player) || !(entity instanceof Wolf wolf) || !bogatyr(player)
                || net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf)) return;
        wolf.tame(player);
        wolf.getPersistentData().putString(OWNER, player.getUUID().toString());
        wolf.setOrderedToSit(false); wolf.setInSittingPose(false); wolf.setTarget(null); wolf.stopBeingAngry();
        // The existing onAddedToLevel path registers the actual successful in-instance spawn.
    }
    private static UUID ownerId(Wolf wolf){
        return wolf.getOwnerReference()==null?null:wolf.getOwnerReference().getUUID();
    }
    private static boolean enrolled(Wolf wolf,long run){
        UUID owner=ownerId(wolf);
        return managed(wolf) && wolf.isTame() && owner!=null && wolf.getPersistentData().getLongOr(RUN,0)==run
                && owner.toString().equals(wolf.getPersistentData().getStringOr(OWNER,""));
    }
    static void register(Wolf wolf,UUID owner,long run){
        if(net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf))return; // Summoned run companions are not permanent player pets.
        if(!(wolf.level() instanceof ServerLevel level)||owner==null||!owner.equals(ownerId(wolf)))return;
        if(managed(wolf)&&!BogatyrRunLifecycle.admit(wolf))return;
        if(!managed(wolf))BogatyrIdentity.fresh(wolf);
        if(!BogatyrIdentity.observe(wolf))return;
        long oldRun=wolf.getPersistentData().getLongOr(RUN,0);
        String oldOwner=wolf.getPersistentData().getStringOr(OWNER,"");
        var data=D1RunData.get(level.getServer());
        if(oldRun>0 && (oldRun!=run||!oldOwner.equals(owner.toString())))
            data.removeUnique(oldRun,"wolves:"+oldOwner,BogatyrIdentity.id(wolf).toString());
        wolf.getPersistentData().putLong(RUN,run);wolf.getPersistentData().putString(OWNER,owner.toString());
        data.recordUnique(run,"wolves:"+owner,BogatyrIdentity.id(wolf).toString());
        tune(wolf);BogatyrCompanions.track(wolf,true);
    }
    /** Player packs are uncapped; breeding still validates run membership and health without scans. */
    public static boolean breedingAllowed(Wolf first,Wolf second){
        if(net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(first)
                ||net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(second))return false;
        if(!managed(first)&&!managed(second))return true;
        if(!(first.level() instanceof ServerLevel level)||second.level()!=level
                ||BogatyrRecovery.held(first)||BogatyrRecovery.held(second))return false;
        var run=currentRun(level).orElse(null);
        if(run==null||!enrolled(first,run.runId())||!enrolled(second,run.runId())
                ||!run.containsPlayer(ownerId(first))||!run.containsPlayer(ownerId(second))
                ||run.isCompletionExited(ownerId(first))||run.isCompletionExited(ownerId(second)))return false;
        return WolfCareRules.healthy(first.getHealth(),first.getMaxHealth())
                && WolfCareRules.healthy(second.getHealth(),second.getMaxHealth());
    }
    private static void tune(Wolf wolf){
        if(wolf.getAttribute(Attributes.MAX_HEALTH)!=null)wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.WOLF_HEALTH.get());
        if(wolf.getAttribute(Attributes.ATTACK_DAMAGE)!=null)wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Config.WOLF_DAMAGE.get());
        if(wolf.getAttribute(Attributes.MOVEMENT_SPEED)!=null)wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.WOLF_SPEED.get());
        if(wolf.getAttribute(Attributes.FOLLOW_RANGE)!=null)wolf.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Config.WOLF_FOLLOW_RANGE.get());
        wolf.setHealth(Math.min(wolf.getHealth(),wolf.getMaxHealth()));
        BogatyrThreats.install(wolf);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getTarget() instanceof Wolf wolf)
                ||net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf))return;
        if(managed(wolf)&&BogatyrRecovery.held(wolf)){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;
        }
        var run=currentRun(player.level()).orElse(null);
        if(run==null||!D1Members.inside(player,run))return;
        var stack=event.getItemStack();
        if(!wolf.isTame()&&stack.is(Items.BONE)){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
            if(!bogatyr(player)){player.displayClientMessage(Component.literal("D1 wolf companions belong to the Bogatyr."),true);return;}
            if(wolf.isAngry()){player.displayClientMessage(Component.literal("This wolf is angry."),true);return;}
            stack.consume(1,player);
            boolean success=player.getRandom().nextDouble()<Config.WOLF_TAME_CHANCE.get()
                    && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(wolf,player);
            if(success){
                wolf.tame(player);wolf.setComponent(net.minecraft.core.component.DataComponents.WOLF_COLLAR,DyeColor.RED);wolf.setOrderedToSit(true);wolf.setTarget(null);
                wolf.getNavigation().stop();register(wolf,player.getUUID(),run.runId());wolf.setHealth(wolf.getMaxHealth());
            }
            player.level().broadcastEntityEvent(wolf,success?(byte)7:(byte)6);
        }else if(wolf.isTame()){
            // Adopt only the actual owner's in-instance player companion. Joining/feeding never manufactures a pet.
            if(wolf.isOwnedBy(player)&&bogatyr(player))register(wolf,player.getUUID(),run.runId());
            if(!enrolled(wolf,run.runId())||!wolf.isFood(stack))return;
            boolean wounded=wolf.getHealth()<wolf.getMaxHealth();
            int growth=WolfCareRules.growthTicks(wolf.getAge(),Config.WOLF_PUP_GROWTH.get());
            if(wounded||wolf.isBaby()){
                event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
                if(!wounded&&growth==0)return;
                String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if(wounded)wolf.heal((float)(item.startsWith("cooked_")?Config.WOLF_COOKED_HEAL.get():Config.WOLF_RAW_HEAL.get()).doubleValue());
                if(growth>0)((BogatyrGrowthAccess)wolf).cosmicdungeon$growByTicks(growth);
                stack.consume(1,player);
                wolf.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(event.getEntity() instanceof Wolf wolf&&event.getLevel() instanceof ServerLevel&&!BogatyrRunLifecycle.admit(wolf))
            event.setCanceled(true);
    }
    public static void added(Wolf wolf){
        if(net.goui.cosmicdungeon.mercenary.MercenaryWolves.managed(wolf)
                ||!(wolf.level() instanceof ServerLevel level))return;
        if(managed(wolf)&&!BogatyrRunLifecycle.admit(wolf))return;
        if(managed(wolf))tune(wolf);
        var run=currentRun(level).orElse(null);if(run==null)return;
        UUID owner=ownerId(wolf);
        // Re-register the saved identity without requiring the owner to be online.
        if(owner!=null&&enrolled(wolf,run.runId())&&run.containsPlayer(owner)&&!run.isCompletionExited(owner)){
            register(wolf,owner,run.runId());return;
        }
        if(wolf.getOwner() instanceof ServerPlayer player&&player.level()==level
                &&bogatyr(player)&&D1Members.inside(player,run))register(wolf,player.getUUID(),run.runId());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void baby(BabyEntitySpawnEvent event){
        if(!(event.getParentA() instanceof Wolf parent)||!(event.getParentB() instanceof Wolf mate)
                ||(!managed(parent)&&!managed(mate)))return;
        if(!(event.getChild() instanceof Wolf child)||!breedingAllowed(parent,mate)){
            event.setCanceled(true);return;
        }
        var run=currentRun((ServerLevel)parent.level()).orElseThrow();
        UUID owner=ownerId(parent);
        // Vanilla assigns the first parent's owner. Keep that UUID and its collar, even offline.
        // Enrollment happens on actual onAddedToLevel, not a possibly canceled birth event.
        child.setOwnerReference(parent.getOwnerReference());
        child.setTame(true,true);
        child.setComponent(net.minecraft.core.component.DataComponents.WOLF_COLLAR,parent.getCollarColor());
        BogatyrIdentity.fresh(child);
        child.getPersistentData().putLong(RUN,run.runId());
        child.getPersistentData().putString(OWNER,owner.toString());
        tune(child);child.setHealth(child.getMaxHealth());
    }
    // TODO(D26-D28, D2+): Bogatyr Items/Armor 1EBc7RDMA5Sm8TQ1uEG4kkPeiFRHBwOygAW_WLtGiUjg:
    // D2 T1 Hidebound wolf armor; T2 Brittle Scapula 13% temporary recruitment and Frenzy.
    // D3 T3 Untested Scapula 22% / Unchained Pack; T4 Hardened Scapula 32% / Wild Fury.
    // Add relic lifetime, health/armor/bleed/lifesteal/regeneration and strongest-aura-only rules
    // from the newest linked documents. Keep vanilla dyed D1 wolf armor and authored chests intact.
    // Run-only lifetime supersedes old permanent-pet archive/retrieval behavior.
}
