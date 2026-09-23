package net.goui.cosmicdungeon.economy;
import java.util.UUID;
/** Disposable client presentation state; cannot debit, credit or persist anything. */
public final class BalanceDisplayView {
    private UUID owner;
    private long revision=-1,balance,available;
    private int chestId=-1;
    public boolean accept(UUID expected,UUID owner,long revision,long balance,long available,int chestId) {
        if(expected==null||!expected.equals(owner)||revision<0||balance<0||available<0||available>balance
                ||chestId< -1||chestId>100)return false;
        if(this.owner!=null && (!this.owner.equals(owner)||revision<=this.revision))return false;
        this.owner=owner;this.revision=revision;this.balance=balance;this.available=available;this.chestId=chestId;return true;
    }
    public boolean ready(UUID player){return owner!=null&&owner.equals(player);}
    public long balance(){return balance;}
    public long available(){return available;}
    public boolean classChest(int id){return chestId>=0&&chestId==id;}
    public void clear(){owner=null;revision=-1;balance=available=0;chestId=-1;}
    public static long[] denominations(long balance) {
        long value=Math.max(0,balance);
        long[] result=new long[5],units={10000,1000,100,10,1};
        for(int i=0;i<5;i++){result[i]=value/units[i];value%=units[i];}
        return result;
    }
}
