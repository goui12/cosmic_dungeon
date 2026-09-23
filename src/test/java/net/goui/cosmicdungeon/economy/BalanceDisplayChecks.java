package net.goui.cosmicdungeon.economy;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.network.CurrencyBalancePayload;
public final class BalanceDisplayChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var poll=new BalanceDisplayPoll();int polls=0,sends=0;
        for(int tick=0;tick<10000;tick++)if(poll.due(tick,20,false)){polls++;if(poll.changed(12345,12000,-1,false))sends++;}
        check(polls==500 && sends==1,"Bounded polling with no unchanged packets");
        check(poll.due(10001,20,true)&&poll.changed(12345,12000,7,true),"Menu open sends immediate class-chest identity");
        check(poll.changed(12345,10000,7,false),"Reservation-only change synchronized");
        check(poll.changed(12345,10000,-1,false),"Chest close clears panel eligibility");
        long revision=poll.revision();
        check(!poll.changed(12345,10000,-1,false)&&poll.revision()==revision,"Unchanged snapshot has no new revision");
        UUID self=UUID.randomUUID(),other=UUID.randomUUID();
        var view=new BalanceDisplayView();
        check(!view.ready(self),"No invented zero balance before synchronization");
        check(!view.accept(self,other,1,100,100,-1),"Other player's balance rejected");
        check(view.accept(self,self,2,12345,12000,7),"Owner snapshot accepted");
        check(view.ready(self)&&view.classChest(7)&&!view.classChest(8),"Panel bound to current server container");
        check(!view.accept(self,self,1,0,0,-1)&&!view.accept(self,self,2,0,0,-1),"Older/equal revision rejected");
        check(view.balance()==12345&&view.available()==12000,"Stale snapshot did not change display");
        check(!view.accept(self,self,3,-1,0,-1)&&!view.accept(self,self,3,100,101,-1),"Invalid values rejected");
        check(view.accept(self,self,3,11111,11111,-1)&&!view.classChest(7),"Fresh close/debit snapshot accepted");
        view.clear();
        check(!view.ready(self)&&view.accept(self,self,1,25,25,-1),"Reconnect resets revision and stale account state");
        Random random=new Random(9123);
        long[] units={10000,1000,100,10,1};
        for(int i=0;i<1000;i++){
            long value=i==0?Long.MAX_VALUE:random.nextLong()&Long.MAX_VALUE;
            long[] counts=BalanceDisplayView.denominations(value);long sum=0;
            for(int j=0;j<5;j++){sum=Math.addExact(sum,Math.multiplyExact(counts[j],units[j]));if(j>0)check(counts[j]>=0&&counts[j]<10,"Normalized lower denomination");}
            check(sum==value,"Every Trace represented exactly");
        }
        var packet=new CurrencyBalancePayload(self,999,Long.MAX_VALUE,100,100);
        var buffer=Unpooled.buffer();
        try{CurrencyBalancePayload.STREAM_CODEC.encode(buffer,packet);check(packet.equals(CurrencyBalancePayload.STREAM_CODEC.decode(buffer)),"Wire codec preserves long values and identity");check(buffer.readableBytes()==0,"Codec exact consumption");}
        finally{buffer.release();}
        System.out.println("Balance display checks passed: "+checks);
    }
}
