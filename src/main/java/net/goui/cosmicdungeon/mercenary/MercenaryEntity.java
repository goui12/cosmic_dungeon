package net.goui.cosmicdungeon.mercenary;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import java.util.*;
/** Server-controlled hired companion. Death/respawn presentation is implemented separately. */
public final class MercenaryEntity extends PathfinderMob implements OwnableEntity {
    private MercenaryContract contract;
    private long run;
    private final MercenaryBrain brain=new MercenaryBrain();
    private MercenaryTimers timers;
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
        if(timers!=null)out.store("mercenary_timers",MercenaryTimers.CODEC,timers);
        ContainerHelper.saveAllItems(out.child("mercenary_supplies"),supplies);
    }
    @Override public void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        contract=in.read("mercenary_contract",MercenaryContract.CODEC).orElse(null);
        run=in.getLongOr("mercenary_run",0);
        timers=in.read("mercenary_timers",MercenaryTimers.CODEC).orElse(null);
        ContainerHelper.loadAllItems(in.childOrEmpty("mercenary_supplies"),supplies);
    }
}
