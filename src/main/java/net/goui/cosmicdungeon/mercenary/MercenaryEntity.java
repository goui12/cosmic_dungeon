package net.goui.cosmicdungeon.mercenary;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import java.util.*;
/** Server-controlled hired companion; death rests the original inventory-bearing entity. */
public final class MercenaryEntity extends PathfinderMob implements OwnableEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DORMANT=
        net.minecraft.network.syncher.SynchedEntityData.defineId(MercenaryEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private MercenaryRest rest;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(DORMANT,false);
    }
    public MercenaryRest rest(){return rest;}
    public boolean dormant(){return entityData.get(DORMANT);}
    private int restFlags(){
        return (isNoAi()?1:0)|(isNoGravity()?2:0)|(isInvisible()?4:0)|(isInvulnerable()?8:0)
            |(isCustomNameVisible()?16:0)|(isSilent()?32:0);
    }
    private void sleep(){
        entityData.set(DORMANT,true);setHealth(1);deathTime=0;setNoAi(true);setNoGravity(true);
        setInvisible(true);setInvulnerable(true);setSilent(true);setCustomNameVisible(false);noPhysics=true;
        getNavigation().stop();setTarget(null);stopRiding();ejectPassengers();
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);clearFire();fallDistance=0;
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(!(level() instanceof net.minecraft.server.level.ServerLevel level)||rest!=null)return;
        rest=new MercenaryRest(MercenaryRest.deadline(level.getServer().overworld().getGameTime()),
            level.dimension().location().toString(),blockPosition().asLong(),restFlags());
        sleep();MercenaryRespawns.remember(this);
    }
    void resumeAfterRest(){
        if(rest==null)return;
        var previous=rest;rest=null;entityData.set(DORMANT,false);noPhysics=false;
        setNoAi(previous.flag(1));setNoGravity(previous.flag(2));setInvisible(previous.flag(4));
        setInvulnerable(previous.flag(8));setCustomNameVisible(previous.flag(16));setSilent(previous.flag(32));
        removeAllEffects();clearFire();setAirSupply(getMaxAirSupply());setTicksFrozen(0);
        deathTime=0;hurtTime=0;invulnerableTime=0;setHealth(getMaxHealth());fallDistance=0;setPortalCooldown();
    }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.damagesource.DamageSource source,float amount){
        return !dormant()&&super.hurtServer(level,source,amount);
    }
    @Override public void tick(){
        if(dormant()){
            // No base tick while resting: no portals, effects, equipment callbacks, or native death removal.
            noPhysics=true;setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);setPortalCooldown();return;
        }
        super.tick();
    }
    @Override protected void onBelowWorld(){if(!dormant())die(damageSources().fellOutOfWorld());}
    @Override public boolean isPickable(){return !dormant()&&super.isPickable();}
    @Override public boolean isPushable(){return !dormant()&&super.isPushable();}
    @Override public boolean isPushedByFluid(){return !dormant()&&super.isPushedByFluid();}
    @Override public boolean isAffectedByPotions(){return !dormant()&&super.isAffectedByPotions();}
    @Override protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.damagesource.DamageSource source){}
    private MercenaryContract contract;
    private long run;
    private final MercenaryBrain brain=new MercenaryBrain();
    private MercenaryTimers timers;
    private MercenaryLootMemory lootMemory=new MercenaryLootMemory();
    public MercenaryLootMemory lootMemory(){return lootMemory;}
    boolean equipmentUpgrade(ItemStack stack,EquipmentSlot slot){
        return canReplaceCurrentItem(stack,getItemBySlot(slot),slot);
    }
    @Override public EntityReference<LivingEntity> getOwnerReference(){return contract==null?null:EntityReference.of(contract.hirer());}
    public MercenaryTimers timers(){
        if(timers==null)timers=new MercenaryTimers(MercenaryConfig.BREW_TICKS.get(),MercenaryConfig.FALLBACK_TICKS.get(),Map.of());
        return timers;
    }
    public void timers(MercenaryTimers value){timers=value;}
    @Override protected void customServerAiStep(net.minecraft.server.level.ServerLevel level){
        super.customServerAiStep(level);brain.tick(this,level);
    }
    private final NonNullList<ItemStack> supplies=NonNullList.withSize(54,ItemStack.EMPTY);
    public MercenaryEntity(EntityType<? extends PathfinderMob> type,Level level){
        super(type,level);setPersistenceRequired();setCanPickUpLoot(false);
        for(var slot:EquipmentSlot.values())setDropChance(slot,0);
    }
    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.3)
                .add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void registerGoals(){goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));}
    public void initialize(long run,MercenaryContract contract){
        if(this.contract!=null||run<=0)throw new IllegalStateException("Mercenary already initialized");
        this.run=run;this.contract=contract;setUUID(contract.id());
        setCustomName(net.minecraft.network.chat.Component.literal("Mercenary "+contract.classId()));
        setCustomNameVisible(true);
    }
    public MercenaryContract contract(){return contract;}
    public long runId(){return run;}
    public NonNullList<ItemStack> supplies(){return supplies;}
    @Override public void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        if(contract!=null)out.store("mercenary_contract",MercenaryContract.CODEC,contract);
        out.putLong("mercenary_run",run);
        if(rest!=null)out.store("mercenary_rest",MercenaryRest.CODEC,rest);
        if(timers!=null)out.store("mercenary_timers",MercenaryTimers.CODEC,timers);
        ContainerHelper.saveAllItems(out.child("mercenary_supplies"),supplies);
        lootMemory.save(out);
    }
    @Override public void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        contract=in.read("mercenary_contract",MercenaryContract.CODEC).orElse(null);
        run=in.getLongOr("mercenary_run",0);
        timers=in.read("mercenary_timers",MercenaryTimers.CODEC).orElse(null);
        ContainerHelper.loadAllItems(in.childOrEmpty("mercenary_supplies"),supplies);
        lootMemory=MercenaryLootMemory.load(in);
        rest=in.read("mercenary_rest",MercenaryRest.CODEC).orElse(null);
        if(rest!=null)sleep();
    }
}
