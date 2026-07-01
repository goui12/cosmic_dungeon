package net.goui.cosmicdungeon.trade;

public final class TradeFinalizationService {
    private TradeFinalizationService() {}

    public enum Result {
        SUCCESS,
        INSUFFICIENT_BALANCE,
        CANNOT_RECEIVE_CURRENCY,
        NOT_ENOUGH_INVENTORY_SPACE,
        CURRENCY_WITHDRAWAL_FAILED,
        CURRENCY_TRANSFER_FAILED
    }

    public interface OfferedItems {
        boolean canMoveInto(TradeParticipant receiver);
        void moveInto(TradeParticipant receiver);
    }

    public interface TradeParticipant {
        long offeredCurrencyTrace();
        long balanceTrace();
        long capacityTrace();
        boolean tryWithdraw(long traceAmount);
        boolean tryDeposit(long traceAmount);
        void setBalanceTrace(long traceAmount);
        boolean canReceiveItems(OfferedItems items);
        void receiveItems(OfferedItems items);
    }

    public static Result finalizeTrade(TradeParticipant a, TradeParticipant b, OfferedItems aOffer, OfferedItems bOffer) {
        long aCurrency = a.offeredCurrencyTrace();
        long bCurrency = b.offeredCurrencyTrace();
        long originalABalance = a.balanceTrace();
        long originalBBalance = b.balanceTrace();

        if (aCurrency < 0L || bCurrency < 0L || originalABalance < aCurrency || originalBBalance < bCurrency) {
            return Result.INSUFFICIENT_BALANCE;
        }
        if (!canReceiveCurrency(a, bCurrency, aCurrency) || !canReceiveCurrency(b, aCurrency, bCurrency)) {
            return Result.CANNOT_RECEIVE_CURRENCY;
        }
        if (!b.canReceiveItems(aOffer) || !a.canReceiveItems(bOffer)) {
            return Result.NOT_ENOUGH_INVENTORY_SPACE;
        }

        try {
            if (aCurrency > 0L && !a.tryWithdraw(aCurrency)) {
                rollbackCurrency(a, b, originalABalance, originalBBalance);
                return Result.CURRENCY_WITHDRAWAL_FAILED;
            }
            if (bCurrency > 0L && !b.tryWithdraw(bCurrency)) {
                rollbackCurrency(a, b, originalABalance, originalBBalance);
                return Result.CURRENCY_WITHDRAWAL_FAILED;
            }

            if (bCurrency > 0L && !a.tryDeposit(bCurrency)) {
                rollbackCurrency(a, b, originalABalance, originalBBalance);
                return Result.CURRENCY_TRANSFER_FAILED;
            }
            if (aCurrency > 0L && !b.tryDeposit(aCurrency)) {
                rollbackCurrency(a, b, originalABalance, originalBBalance);
                return Result.CURRENCY_TRANSFER_FAILED;
            }
        } catch (RuntimeException ex) {
            rollbackCurrency(a, b, originalABalance, originalBBalance);
            return Result.CURRENCY_TRANSFER_FAILED;
        }

        b.receiveItems(aOffer);
        a.receiveItems(bOffer);
        return Result.SUCCESS;
    }

    private static boolean canReceiveCurrency(TradeParticipant player, long incoming, long outgoing) {
        if (incoming < 0L || outgoing < 0L) return false;
        long balanceAfterWithdrawal = player.balanceTrace() - outgoing;
        if (balanceAfterWithdrawal < 0L) return false;
        return incoming <= player.capacityTrace() - balanceAfterWithdrawal;
    }

    private static void rollbackCurrency(TradeParticipant a, TradeParticipant b, long originalABalance, long originalBBalance) {
        a.setBalanceTrace(originalABalance);
        b.setBalanceTrace(originalBBalance);
    }
}
