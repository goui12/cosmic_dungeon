package net.goui.cosmicdungeon.trade;
/** Pure final preflight shared with regression fixtures. Mutation belongs to TradeTransactions. */
public final class TradeFinalizationService {
    private TradeFinalizationService(){}
    public enum Result { SUCCESS, INSUFFICIENT_BALANCE, CANNOT_RECEIVE_CURRENCY, NOT_ENOUGH_INVENTORY_SPACE }
    public static Result validate(long aPays,long bPays,long aAvailable,long bAvailable,
            long aRoom,long bRoom,boolean aFits,boolean bFits){
        if(aPays<0||bPays<0||aPays>aAvailable||bPays>bAvailable)return Result.INSUFFICIENT_BALANCE;
        // Canon requires the complete incoming amount to fit, without netting outgoing payments.
        if(bPays>aRoom||aPays>bRoom)return Result.CANNOT_RECEIVE_CURRENCY;
        if(!aFits||!bFits)return Result.NOT_ENOUGH_INVENTORY_SPACE;
        return Result.SUCCESS;
    }
}
