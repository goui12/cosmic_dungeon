package net.goui.cosmicdungeon.trade;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import java.util.*;
/** Both exact custody images share the authoritative account decision; cursors never change owners. */
public record TradeCommitPlan(UUID first,UUID second,CompoundTag firstBefore,CompoundTag secondBefore,boolean firstAck,boolean secondAck){
    public static final Codec<TradeCommitPlan> CODEC=RecordCodecBuilder.create(i->i.group(
        UUIDUtil.STRING_CODEC.fieldOf("first").forGetter(TradeCommitPlan::first),UUIDUtil.STRING_CODEC.fieldOf("second").forGetter(TradeCommitPlan::second),
        CompoundTag.CODEC.fieldOf("first_before").forGetter(TradeCommitPlan::firstBefore),CompoundTag.CODEC.fieldOf("second_before").forGetter(TradeCommitPlan::secondBefore),
        Codec.BOOL.optionalFieldOf("first_ack",false).forGetter(TradeCommitPlan::firstAck),Codec.BOOL.optionalFieldOf("second_ack",false).forGetter(TradeCommitPlan::secondAck)
    ).apply(i,TradeCommitPlan::new));
    public TradeCommitPlan{
        firstBefore=firstBefore.copy();secondBefore=secondBefore.copy();
        TradeCustodyImages.validate(firstBefore,first);TradeCustodyImages.validate(secondBefore,second);
        TradeCustodyImages.exchanged(firstBefore,secondBefore);
        String tx=firstBefore.getStringOr("transaction","");UUID.fromString(tx);
        if(!tx.equals(secondBefore.getStringOr("transaction","")))throw new IllegalArgumentException("Unpaired trade reservation");
    }
    @Override public CompoundTag firstBefore(){return firstBefore.copy();}
    @Override public CompoundTag secondBefore(){return secondBefore.copy();}
    public UUID transaction(){return UUID.fromString(firstBefore.getStringOr("transaction",""));}
    public CompoundTag before(UUID owner){if(first.equals(owner))return firstBefore();if(second.equals(owner))return secondBefore();throw new IllegalArgumentException("Foreign trade owner");}
    public boolean acknowledged(UUID owner){before(owner);return first.equals(owner)?firstAck:secondAck;}
    public TradeCommitPlan acknowledge(UUID owner){before(owner);return new TradeCommitPlan(first,second,firstBefore,secondBefore,firstAck||first.equals(owner),secondAck||second.equals(owner));}
    public CompoundTag project(UUID owner,CompoundTag current,boolean committed){
        if(acknowledged(owner)||!before(owner).equals(current))throw new IllegalStateException("Owner trade custody differs from verified reservation");
        return committed?TradeCustodyImages.exchanged(current,first.equals(owner)?secondBefore:firstBefore):current.copy();
    }
}
