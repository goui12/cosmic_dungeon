package net.goui.cosmicdungeon.crafting;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.goui.cosmicdungeon.crafting.SharedResourcePlanner.*;

class SharedResourcePlannerTest {
    private static final UUID CRAFTER=new UUID(0,1), A=new UUID(0,2), B=new UUID(0,3), C=new UUID(0,4);
    private static Stock stock(UUID owner,long rank,Map<String,Integer> items){return new Stock(owner,rank,items);}
    private static int debit(Plan plan,UUID owner,String item) {
        return plan.debits().stream().filter(d->d.owner().equals(owner)&&d.ingredient().equals(item)).mapToInt(Debit::count).sum();
    }

    @Test void completeOwnRecipeDoesNotBorrowOrAdvanceDonationHistory() {
        var plan=plan(CRAFTER,Map.of("wart",1,"sugar",2),List.of(
                stock(CRAFTER,3,Map.of("wart",2,"sugar",4)),stock(A,0,Map.of("wart",20,"sugar",20))),7);
        assertNotNull(plan);assertFalse(plan.shared());assertEquals(7,plan.sequence());assertTrue(plan.donatedAfter().isEmpty());
        assertEquals(1,debit(plan,CRAFTER,"wart"));assertEquals(2,debit(plan,CRAFTER,"sugar"));
        assertEquals(0,debit(plan,A,"wart"));assertEquals(0,debit(plan,A,"sugar"));
    }
    @Test void partialOwnStockCombinesDifferentPlayersAndIngredients() {
        var plan=plan(CRAFTER,Map.of("wart",2,"sugar",2),List.of(
                stock(CRAFTER,0,Map.of("wart",1)),stock(A,0,Map.of("wart",1,"sugar",2)),
                stock(B,1,Map.of("sugar",2))),1);
        assertNotNull(plan);assertTrue(plan.shared());assertEquals(4,plan.sequence());
        assertEquals(1,debit(plan,CRAFTER,"wart"));assertEquals(1,debit(plan,A,"wart"));
        assertEquals(1,debit(plan,A,"sugar"));assertEquals(1,debit(plan,B,"sugar"));
        assertEquals(Map.of(A,4L,B,3L),plan.donatedAfter());
    }
    @Test void bulkCraftBalancesIndividualUnitsRatherThanDrainingFirstPlayer() {
        var plan=plan(CRAFTER,Map.of("wart",6),List.of(
                stock(A,0,Map.of("wart",6)),stock(B,0,Map.of("wart",6)),stock(C,0,Map.of("wart",6))),0);
        assertEquals(2,debit(plan,A,"wart"));assertEquals(2,debit(plan,B,"wart"));assertEquals(2,debit(plan,C,"wart"));
        assertEquals(Map.of(A,4L,B,5L,C,6L),plan.donatedAfter());assertEquals(6,plan.sequence());
    }
    @Test void repeatedSuccessfulCraftsRotateLeastRecentDonors() {
        var stocks=new ArrayList<>(List.of(stock(A,0,Map.of("wart",10)),stock(B,0,Map.of("wart",10))));
        var totals=new HashMap<UUID,Integer>();long sequence=0;
        for(int craft=0;craft<12;craft++) {
            var result=plan(CRAFTER,Map.of("wart",1),stocks,sequence);assertNotNull(result);sequence=result.sequence();
            for(var change:result.debits())totals.merge(change.owner(),change.count(),Integer::sum);
            var next=new ArrayList<Stock>();
            for(var owner:stocks)next.add(stock(owner.owner(),result.donatedAfter().getOrDefault(owner.owner(),owner.donated()),
                    Map.of("wart",owner.counts().get("wart")-debit(result,owner.owner(),"wart"))));
            stocks=next;
        }
        assertEquals(Map.of(A,6,B,6),totals);assertEquals(12,sequence);
    }
    @Test void missingIngredientSkipsDonorWithoutChangingTheirRank() {
        var result=plan(CRAFTER,Map.of("wart",2),List.of(
                stock(A,0,Map.of("sugar",40)),stock(B,5,Map.of("wart",4)),stock(C,9,Map.of("wart",4))),9);
        assertEquals(0,debit(result,A,"wart"));assertFalse(result.donatedAfter().containsKey(A));
        assertEquals(1,debit(result,B,"wart"));assertEquals(1,debit(result,C,"wart"));
        assertEquals(Map.of(B,10L,C,11L),result.donatedAfter());
    }
    @Test void tieBreakingAndIngredientOrderAreIndependentOfInputMapOrRosterOrder() {
        var first=new LinkedHashMap<String,Integer>();first.put("wart",1);first.put("sugar",1);
        var second=new LinkedHashMap<String,Integer>();second.put("sugar",1);second.put("wart",1);
        var a=stock(A,0,Map.of("wart",1,"sugar",1));var b=stock(B,0,Map.of("wart",1,"sugar",1));
        assertEquals(plan(CRAFTER,first,List.of(a,b),0),plan(CRAFTER,second,List.of(b,a),0));
        var result=plan(CRAFTER,first,List.of(b,a),0);
        assertEquals(1,debit(result,A,"sugar"));assertEquals(1,debit(result,B,"wart"));
    }
    @Test void insufficientRecipeReturnsNoPartialDebitAndLeavesInputsIntact() {
        var original=new LinkedHashMap<String,Integer>(Map.of("wart",4));var stock=stock(A,12,original);
        var needs=new LinkedHashMap<String,Integer>(Map.of("wart",1,"sugar",1));
        assertNull(plan(CRAFTER,needs,List.of(stock),12));
        assertEquals(Map.of("wart",4),stock.counts());assertEquals(12,stock.donated());
        assertEquals(Map.of("wart",1,"sugar",1),needs);assertEquals(Map.of("wart",4),original);
        assertEquals(0,capacity(needs,List.of(stock)));
    }
    @Test void capacityUsesEveryIngredientAndCapsWithoutIntegerOverflow() {
        assertEquals(3,capacity(Map.of("wart",2,"sugar",1),List.of(
                stock(A,0,Map.of("wart",4,"sugar",2)),stock(B,0,Map.of("wart",3,"sugar",2)))));
        assertEquals(64,capacity(Map.of("wart",1),List.of(stock(A,0,Map.of("wart",Integer.MAX_VALUE)),
                stock(B,0,Map.of("wart",Integer.MAX_VALUE)))));
        assertEquals(0,capacity(Map.of("wart",1),List.of()));
    }
    @Test void stockAndPlanSnapshotsAreDeeplyImmutable() {
        var map=new HashMap<String,Integer>();map.put("wart",2);var stock=stock(A,0,map);map.put("wart",20);
        assertEquals(2,stock.counts().get("wart"));
        assertThrows(UnsupportedOperationException.class,()->stock.counts().put("wart",0));
        var result=plan(CRAFTER,Map.of("wart",1),List.of(stock),0);
        assertThrows(UnsupportedOperationException.class,()->result.debits().clear());
        assertThrows(UnsupportedOperationException.class,()->result.donatedAfter().put(A,100L));
        assertEquals(2,stock.counts().get("wart"));
    }
    @Test void invalidOwnersKeysQuantitiesAndRecipeLimitsAreRejected() {
        var a=stock(A,0,Map.of("wart",1));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",1),List.of(a,a),0));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of(),List.of(a),0));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",0),List.of(a),0));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",-1),List.of(a),0));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",64,"sugar",1),List.of(a),0));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",1),List.of(a),-1));
        assertThrows(IllegalArgumentException.class,()->stock(A,-1,Map.of()));
        assertThrows(IllegalArgumentException.class,()->stock(A,0,Map.of("wart",-1)));
        assertThrows(IllegalArgumentException.class,()->stock(A,0,Map.of(" ",1)));
        assertThrows(IllegalArgumentException.class,()->stock(A,0,Map.of("x".repeat(129),1)));
        var keys=new HashMap<String,Integer>();for(int i=0;i<17;i++)keys.put("key"+i,1);
        assertThrows(IllegalArgumentException.class,()->stock(A,0,keys));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,keys,List.of(a),0));
        var owners=new ArrayList<Stock>();for(int i=0;i<7;i++)owners.add(stock(new UUID(1,i),0,Map.of("wart",1)));
        assertThrows(IllegalArgumentException.class,()->capacity(Map.of("wart",1),owners));
    }
    @Test void sequenceOverflowCannotReturnAPartialAllocation() {
        var a=stock(A,Long.MAX_VALUE,Map.of("wart",2));
        assertThrows(ArithmeticException.class,()->plan(CRAFTER,Map.of("wart",1),List.of(a),Long.MAX_VALUE));
        assertEquals(2,a.counts().get("wart"));assertEquals(Long.MAX_VALUE,a.donated());
        var own=plan(CRAFTER,Map.of("wart",1),List.of(stock(CRAFTER,0,Map.of("wart",1))),Long.MAX_VALUE);
        assertFalse(own.shared());assertEquals(Long.MAX_VALUE,own.sequence());
    }
    @Test void restoredCounterCannotMoveDonationRankBackwards() {
        var donor=stock(A,20,Map.of("wart",1));
        assertThrows(IllegalArgumentException.class,()->plan(CRAFTER,Map.of("wart",1),List.of(donor),2));
        assertEquals(20,donor.donated());assertEquals(1,donor.counts().get("wart"));
    }
}
