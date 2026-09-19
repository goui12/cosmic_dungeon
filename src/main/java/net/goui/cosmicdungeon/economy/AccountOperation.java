package net.goui.cosmicdungeon.economy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
/** One account plus its immutable item/service plan, in the existing authoritative account save. */
public record AccountOperation(UUID owner,long delta,String kind,String related,long run,String status,long timestamp,
        long before,long after,CompoundTag plan,boolean prepared,boolean acknowledged){
    public static final Codec<AccountOperation> CODEC=RecordCodecBuilder.create(i->i.group(
        UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(AccountOperation::owner),Codec.LONG.fieldOf("delta").forGetter(AccountOperation::delta),
        Codec.STRING.fieldOf("kind").forGetter(AccountOperation::kind),Codec.STRING.fieldOf("related").forGetter(AccountOperation::related),
        Codec.LONG.fieldOf("run").forGetter(AccountOperation::run),Codec.STRING.fieldOf("status").forGetter(AccountOperation::status),
        Codec.LONG.fieldOf("timestamp").forGetter(AccountOperation::timestamp),Codec.LONG.fieldOf("before").forGetter(AccountOperation::before),
        Codec.LONG.fieldOf("after").forGetter(AccountOperation::after),CompoundTag.CODEC.fieldOf("plan").forGetter(AccountOperation::plan),
        Codec.BOOL.optionalFieldOf("prepared",false).forGetter(AccountOperation::prepared),Codec.BOOL.optionalFieldOf("acknowledged",false).forGetter(AccountOperation::acknowledged)
    ).apply(i,AccountOperation::new));
    public AccountOperation{
        Objects.requireNonNull(owner);plan=plan.copy();
        if(delta==Long.MIN_VALUE||run<0||before<0||after<0||kind.isBlank()||kind.length()>64||related.length()>256
                ||!Set.of(AccountTransfer.RESERVED,AccountTransfer.COMMITTED,AccountTransfer.CANCELLED,AccountTransfer.REJECTED).contains(status))throw new IllegalArgumentException("Invalid account operation");
        if(status.equals(AccountTransfer.COMMITTED)){
            if(!prepared||Math.addExact(before,delta)!=after)throw new IllegalArgumentException("Unprepared or mismatched account commit");
        }else if(before!=after)throw new IllegalArgumentException("Uncommitted balance changed");
        if(status.equals(AccountTransfer.RESERVED)&&acknowledged)throw new IllegalArgumentException("Undecided operation acknowledged");
        if(plan.isEmpty()&&!acknowledged)throw new IllegalArgumentException("Missing item plan");
    }
    @Override public CompoundTag plan(){return plan.copy();}
    public boolean reserved(){return status.equals(AccountTransfer.RESERVED);}
    public AccountOperation armed(){if(!reserved())throw new IllegalStateException("Terminal operation");return new AccountOperation(owner,delta,kind,related,run,status,timestamp,before,after,plan,true,false);}
    public AccountOperation decide(String result,long balance,long now){return new AccountOperation(owner,delta,kind,related,run,result,now,balance,result.equals(AccountTransfer.COMMITTED)?Math.addExact(balance,delta):balance,plan,prepared,false);}
    public AccountOperation acknowledge(){if(reserved())throw new IllegalStateException("Undecided operation");return new AccountOperation(owner,delta,kind,related,run,status,timestamp,before,after,new CompoundTag(),prepared,true);}
}
