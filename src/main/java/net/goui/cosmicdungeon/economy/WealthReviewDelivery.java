package net.goui.cosmicdungeon.economy;

import java.util.*;
import java.util.function.*;

/** Session-only delivery cursors. The durable inbox is cleared only by an explicit ledgered decision. */
public final class WealthReviewDelivery<T> {
    private final Map<T,Long> cursors=new WeakHashMap<>();
    private int nextRecipient;
    private boolean retryProof;
    public void forget(T recipient){cursors.remove(recipient);}
    public int deliver(List<T> recipients,WealthReviewState inbox,int budget,boolean changed,
                       BooleanSupplier verified,BiConsumer<T,WealthReviewState.Notice> send){
        if(budget<1||budget>128)throw new IllegalArgumentException("Invalid wealth delivery budget");
        cursors.keySet().removeIf(recipient->!recipients.contains(recipient));
        boolean pending=recipients.stream().anyMatch(r->!inbox.pending(cursors.getOrDefault(r,0L),1).isEmpty());
        if(!changed&&!pending&&!retryProof)return 0;
        retryProof=true;if(!verified.getAsBoolean())return 0;retryProof=false;
        int sent=0,empty=0;
        while(sent<budget&&!recipients.isEmpty()&&empty<recipients.size()){
            if(nextRecipient>=recipients.size())nextRecipient=0;
            T recipient=recipients.get(nextRecipient++);
            var rows=inbox.pending(cursors.getOrDefault(recipient,0L),1);
            if(rows.isEmpty()){empty++;continue;}empty=0;
            var notice=rows.getFirst();send.accept(recipient,notice);
            cursors.put(recipient,notice.sequence());sent++;
        }
        return sent;
    }
}
