package net.goui.cosmicdungeon.npc.inn;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.economy.PlayerCurrencyData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import java.util.UUID;

public final class InnChecks {
    private static int checks;
    private static final UUID OWNER=new UUID(0,2701),OTHER=new UUID(0,2702);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private interface Work {void run()throws Exception;}
    private static void reject(Work work,String why){boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type)throws Exception{
        var field=type.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<T>)field.get(null);
    }
    private static <T> CompoundTag encode(Codec<T> codec,T data){return (CompoundTag)codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow();}
    public static void main(String[] args)throws Exception{
        var codec=codec(InnData.class);
        var old=new CompoundTag();old.putString("dimension","minecraft:overworld");
        old.put("minimum",BlockPos.CODEC.encodeStart(NbtOps.INSTANCE,new BlockPos(-4,60,-4)).getOrThrow());
        old.put("maximum",BlockPos.CODEC.encodeStart(NbtOps.INSTANCE,new BlockPos(4,70,4)).getOrThrow());
        var beds=new CompoundTag();beds.put(OWNER.toString(),BlockPos.CODEC.encodeStart(NbtOps.INSTANCE,new BlockPos(0,64,0)).getOrThrow());old.put("beds",beds);
        var data=codec.parse(NbtOps.INSTANCE,old).getOrThrow();
        check(data.bed(OWNER).equals(new BlockPos(0,64,0)),"Legacy bed retained");
        check(data.bedDimension(OWNER).equals("minecraft:overworld"),"Legacy dimension assigned without deleting bed");
        check(data.bed(OTHER)==null,"Inn binding is personal");
        check(!data.flushVerified(),"Detached test store cannot claim a verified write");
        for(int x:new int[]{-5,-4,0,4,5})for(int y:new int[]{59,60,64,70,71})for(int z:new int[]{-5,-4,0,4,5})
            check(data.contains("minecraft:overworld",new BlockPos(x,y,z))==(x>=-4&&x<=4&&y>=60&&y<=70&&z>=-4&&z<=4),"Inclusive Inn cuboid "+x+","+y+","+z);
        check(!data.contains("minecraft:the_nether",new BlockPos(0,64,0)),"Protection cannot cross dimensions");
        data.region("minecraft:the_nether",new BlockPos(4,70,4),new BlockPos(-4,60,-4));
        data=codec.parse(NbtOps.INSTANCE,encode(codec,data)).getOrThrow();
        check(data.bed(OWNER).equals(new BlockPos(0,64,0)),"Region edits preserve old binding evidence");
        check(data.bedDimension(OWNER).equals("minecraft:overworld"),"Old bed does not silently move into another dimension");
        data.bed(OWNER,new BlockPos(1,64,1));data=codec.parse(NbtOps.INSTANCE,encode(codec,data)).getOrThrow();
        check(data.bedDimension(OWNER).equals("minecraft:the_nether")&&data.bed(OWNER).equals(new BlockPos(1,64,1)),"Free bed reassignment persists new dimension");
        var invalid=old.copy();invalid.putString("dimension","Not a dimension!");
        reject(()->codec.parse(NbtOps.INSTANCE,invalid).getOrThrow(),"Bad native Inn dimension is held");
        var inverted=old.copy();inverted.put("minimum",BlockPos.CODEC.encodeStart(NbtOps.INSTANCE,new BlockPos(100,100,100)).getOrThrow());
        reject(()->codec.parse(NbtOps.INSTANCE,inverted).getOrThrow(),"Inverted native region is held");
        var foreign=old.copy();var dimensions=new CompoundTag();dimensions.putString(OTHER.toString(),"minecraft:overworld");foreign.put("bed_dimensions",dimensions);
        reject(()->codec.parse(NbtOps.INSTANCE,foreign).getOrThrow(),"Orphan bed metadata is held");
        var accountCodec=codec(PlayerCurrencyData.class);var accounts=accountCodec.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        accounts.setBalanceTrace(OWNER,30);
        check(accounts.change(OWNER,"Inn fixture",-15,"inn_bond","Beluzon",0,"inn:bond",false)==15,"Documented one-time fifteen Trace fee");
        accounts=accountCodec.parse(NbtOps.INSTANCE,encode(accountCodec,accounts)).getOrThrow();
        check(accounts.hasReceipt("inn:bond",OWNER)&&!accounts.hasReceipt("inn:bond",OTHER),"Personal permanent bond shares account save");
        accounts.change(OWNER,"Inn fixture",-15,"inn_bond","Beluzon",0,"inn:bond",false);
        check(accounts.getBalanceTrace(OWNER)==15,"Repeated confirmation cannot charge twice");
        accounts.setBalanceTrace(OTHER,14);
        check(accounts.change(OTHER,"Other",-15,"inn_bond","Beluzon",0,"inn:bond",false)<0&&!accounts.hasReceipt("inn:bond",OTHER),"Insufficient balance cannot establish bond");
        System.out.println("Inn checks passed: "+checks);
    }
}
