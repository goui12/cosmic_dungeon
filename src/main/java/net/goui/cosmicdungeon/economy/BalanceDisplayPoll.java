package net.goui.cosmicdungeon.economy;
/** Transient change detector, not a spendable account or persisted balance. */
public final class BalanceDisplayPoll {
    private long nextPoll, revision, balance, available;
    private int chestId=-1;
    private boolean initialized;
    public boolean due(long tick,int interval,boolean force) {
        if(!force && initialized && tick<nextPoll)return false;
        nextPoll=tick+Math.max(1,interval);return true;
    }
    public boolean changed(long balance,long available,int chestId,boolean force) {
        if(!force && initialized && this.balance==balance && this.available==available && this.chestId==chestId)return false;
        initialized=true;this.balance=balance;this.available=available;this.chestId=chestId;revision++;return true;
    }
    public long revision(){return revision;}
}
