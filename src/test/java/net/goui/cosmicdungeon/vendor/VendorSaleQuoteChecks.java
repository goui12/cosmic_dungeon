package net.goui.cosmicdungeon.vendor;

import java.util.List;
import net.goui.cosmicdungeon.economy.pricing.VendorPrice;

/** Exercises production quote rules with mutable stacks, without starting the FML runtime. */
public final class VendorSaleQuoteChecks {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static final class Stack {
        private final String item;
        private final String components;
        private int count;
        Stack(String item, int count, String components) { this.item=item; this.count=count; this.components=components; }
        Stack copy() { return new Stack(item,count,components); }
        boolean same(Stack other) { return item.equals(other.item) && count==other.count && components.equals(other.components); }
    }
    private static VendorSaleQuote.Line<Stack> line(int slot, Stack stack, long price) {
        return new VendorSaleQuote.Line<>(slot,stack,price,Stack::copy,Stack::same);
    }
    private static VendorSaleQuote<Stack> quote() {
        return new VendorSaleQuote<>(7,100,600,List.of(line(0,new Stack("stone",2,""),0)));
    }
    public static void main(String[] args) {
        check(!new VendorPrice(0,"restricted").approved(),"Restricted zero rejected");
        check(!new VendorPrice(0,"unpriced").approved(),"Unpriced zero rejected");
        check(new VendorPrice(0,"catalog:stone",true).approved(),"Approved zero remains eligible");
        var q=quote();
        check(q.total()==0,"Zero quote total");
        check(q.consume(q.token(),100),"Initial confirmation");
        check(!q.consume(q.token(),101),"Duplicate confirmation rejected");
        q=quote();
        check(!q.consume("wrong",100),"Wrong token rejected");
        check(q.consume(q.token(),699),"Quote valid before deadline");
        q=quote(); check(!q.consume(q.token(),700),"Deadline inclusive rejection");
        q=quote(); check(!q.consume(q.token(),99),"Clock rollback rejected");
        q=quote(); check(!q.consume(q.token(),701),"Expired quote rejected");
        check(!q.consume(q.token(),101),"Expired quote cannot revive");
        Stack original=new Stack("stone",2,"");
        var line=line(0,original,5);
        original.count=3;
        check(line.stack().count==2,"Input mutation cannot change quote");
        line.stack().count=4;
        check(line.stack().count==2,"Accessor mutation cannot change quote");
        check(line.matches(new Stack("stone",2,""),5),"Unchanged item accepted");
        check(!line.matches(new Stack("stone",3,""),5),"Changed count rejected");
        check(!line.matches(new Stack("dirt",2,""),5),"Changed identity rejected");
        check(!line.matches(new Stack("stone",2,"renamed"),5),"Changed components rejected");
        check(!line.matches(new Stack("stone",2,""),6),"Changed price rejected");
        boolean duplicate=false;
        try { new VendorSaleQuote<>(7,0,600,List.of(line,line)); }
        catch(IllegalArgumentException expected) { duplicate=true; }
        check(duplicate,"Duplicate slot rejected");
        boolean overflow=false;
        try { new VendorSaleQuote<>(7,0,600,List.of(line(0,original,Long.MAX_VALUE),line(1,original,1))); }
        catch(ArithmeticException expected) { overflow=true; }
        check(overflow,"Aggregate overflow rejected");
        System.out.println(checks+" vendor quote checks passed");
    }
}
