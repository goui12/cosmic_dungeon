package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import java.util.*;
/** Pending item decision lives in the SAME SavedData payload as its paired account receipt.
 * Full images are retained only until both players' own receipts have been saved and acknowledged. */
public record RepairCommitPlan(UUID customer,UUID provider,CompoundTag customerBefore,
        CompoundTag providerBefore,CompoundTag repairedItem,int units,long readyDeadline,
        long startedTick,int durationTicks,boolean customerAck,boolean providerAck){
    public static final Codec<RepairCommitPlan> CODEC=RecordCodecBuilder.create(i->i.group(
            UUIDUtil.STRING_CODEC.fieldOf("customer").forGetter(RepairCommitPlan::customer),
            UUIDUtil.STRING_CODEC.fieldOf("provider").forGetter(RepairCommitPlan::provider),
            CompoundTag.CODEC.fieldOf("customer_before").forGetter(RepairCommitPlan::customerBefore),
            CompoundTag.CODEC.fieldOf("provider_before").forGetter(RepairCommitPlan::providerBefore),
            CompoundTag.CODEC.fieldOf("repaired_item").forGetter(RepairCommitPlan::repairedItem),
            Codec.INT.fieldOf("units").forGetter(RepairCommitPlan::units),
            Codec.LONG.fieldOf("ready_deadline").forGetter(RepairCommitPlan::readyDeadline),
            Codec.LONG.fieldOf("started_tick").forGetter(RepairCommitPlan::startedTick),
            Codec.INT.fieldOf("duration_ticks").forGetter(RepairCommitPlan::durationTicks),
            Codec.BOOL.optionalFieldOf("customer_ack",false).forGetter(RepairCommitPlan::customerAck),
            Codec.BOOL.optionalFieldOf("provider_ack",false).forGetter(RepairCommitPlan::providerAck)
    ).apply(i,RepairCommitPlan::new));
    public RepairCommitPlan{
        Objects.requireNonNull(customer);Objects.requireNonNull(provider);
        customerBefore=customerBefore.copy();providerBefore=providerBefore.copy();repairedItem=repairedItem.copy();
        RepairCustodyImages.validate(customerBefore,customer);RepairCustodyImages.validate(providerBefore,provider);
        if(customer.equals(provider)||!customerBefore.getBooleanOr("customer",false)
                ||providerBefore.getBooleanOr("customer",true)||units<1||units>4||durationTicks<1
                ||readyDeadline<0||startedTick< -1||startedTick>=readyDeadline
                ||!customerBefore.getStringOr("session","").equals(providerBefore.getStringOr("session","")))
            throw new IllegalArgumentException("Invalid repair participants/timing");
        String transaction=customerBefore.getStringOr("transaction","");UUID.fromString(transaction);
        if(!transaction.equals(providerBefore.getStringOr("transaction",""))
                ||RepairCustodyImages.held(providerBefore,true).isEmpty()
                ||!durabilityOnly(customerBefore.getCompoundOrEmpty("target"),repairedItem))
            throw new IllegalArgumentException("Repair plan changes ownership, identity or unrelated item data");
    }
    @Override public CompoundTag customerBefore(){return customerBefore.copy();}
    @Override public CompoundTag providerBefore(){return providerBefore.copy();}
    @Override public CompoundTag repairedItem(){return repairedItem.copy();}
    public UUID transaction(){return UUID.fromString(customerBefore.getStringOr("transaction",""));}
    public CompoundTag before(UUID owner){
        if(customer.equals(owner))return customerBefore();if(provider.equals(owner))return providerBefore();
        throw new IllegalArgumentException("Not a repair participant");
    }
    public boolean acknowledged(UUID owner){
        if(customer.equals(owner))return customerAck;if(provider.equals(owner))return providerAck;
        throw new IllegalArgumentException("Not a repair participant");
    }
    public RepairCommitPlan acknowledge(UUID owner){
        before(owner);return new RepairCommitPlan(customer,provider,customerBefore,providerBefore,repairedItem,units,
                readyDeadline,startedTick,durationTicks,customerAck||customer.equals(owner),providerAck||provider.equals(owner));
    }
    public RepairCommitPlan start(long now){
        if(startedTick>=0)return this;
        if(now<0||now>=readyDeadline)throw new IllegalStateException("Repair Ready deadline passed");
        return new RepairCommitPlan(customer,provider,customerBefore,providerBefore,repairedItem,units,readyDeadline,
                now,durationTicks,customerAck,providerAck);
    }
    public boolean elapsed(long now){return startedTick>=0&&now>=startedTick&&now-startedTick>=durationTicks;}
    public CompoundTag project(UUID owner,CompoundTag current,boolean committed){
        if(acknowledged(owner)||!before(owner).equals(current))throw new IllegalStateException("Repair custody differs from verified reservation");
        var next=current.copy();
        if(committed){
            if(customer.equals(owner))next.put("target",repairedItem());else next.put("components",new ListTag());
        }
        return next;
    }
    public static boolean durabilityOnly(CompoundTag before,CompoundTag after){
        if(before.isEmpty()||after.isEmpty())return false;
        var left=before.copy();var right=after.copy();
        var lc=left.getCompoundOrEmpty("components").copy();var rc=right.getCompoundOrEmpty("components").copy();
        int oldDamage=lc.getIntOr("minecraft:damage",0),newDamage=rc.getIntOr("minecraft:damage",0);
        if(oldDamage<=0||newDamage<0||newDamage>=oldDamage)return false;
        lc.remove("minecraft:damage");rc.remove("minecraft:damage");
        if(lc.isEmpty())left.remove("components");else left.put("components",lc);
        if(rc.isEmpty())right.remove("components");else right.put("components",rc);
        return left.equals(right);
    }
}
