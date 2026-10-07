package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;

/** An immutable consent selection; counts and item ownership remain exclusively server-validated. */
public record SupplyRequestAction(long runId,long revision,Decision decision,List<UUID> requestIds) {
    public enum Decision {REQUEST,ACCEPT,DENY,ACCEPT_ALL,DENY_ALL}
    public SupplyRequestAction{
        Objects.requireNonNull(decision);requestIds=List.copyOf(requestIds);
        if(runId<=0||revision<0||requestIds.size()>SupplyRequestsSnapshot.MAX_CARDS
                ||requestIds.stream().distinct().count()!=requestIds.size()
                ||decision==Decision.REQUEST&&!requestIds.isEmpty()
                ||decision!=Decision.REQUEST&&requestIds.isEmpty()
                ||(decision==Decision.ACCEPT||decision==Decision.DENY)&&requestIds.size()!=1)
            throw new IllegalArgumentException("Invalid supply request action");
    }
}
