package net.goui.cosmicdungeon.menu;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.goui.cosmicdungeon.network.VendorPayloads;

public final class MenuBalanceChecks {
    private static int checks;
    private static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    public static void main(String[] args) {
        UUID first=new UUID(1,1), reopened=new UUID(1,2);
        check(SessionMenu.matches(7,first,7,first),"Current opening accepted");
        check(!SessionMenu.matches(7,first,7,reopened),"Reused container rejects stale opening");
        check(!SessionMenu.matches(7,first,8,first),"Different container rejected");
        check(!SessionMenu.matches(7,null,7,first),"No active opening");
        check(!SessionMenu.matches(7,first,7,null),"Missing payload identity");

        var refresh=new MenuBalanceRefresh();
        check(refresh.due(0,20),"Immediate first snapshot");
        check(refresh.changed(12345,100000000),"Initial snapshot");
        check(!refresh.changed(12345,100000000),"No unchanged packet");
        for(int tick=1;tick<20;tick++)check(!refresh.due(tick,20),"No intermediate poll");
        check(refresh.due(20,20),"Next bounded poll");
        check(refresh.changed(12355,100000000),"External reward refresh");
        check(refresh.changed(12355,200000000),"Capacity-only refresh");
        check(!refresh.changed(12355,200000000),"No capacity packet spam");
        var another=new MenuBalanceRefresh();
        check(another.due(20,20)&&another.changed(12355,200000000),"New opening resynchronizes same balance");
        check(another.changed(Long.MAX_VALUE,0),"Whole long amount without narrowing");

        var balance = new VendorPayloads.S2C_VendorBalance(7, first, 12345L,100000000L);
        var bytes=Unpooled.buffer();
        try {
            VendorPayloads.S2C_VendorBalance.STREAM_CODEC.encode(bytes,balance);
            check(balance.equals(VendorPayloads.S2C_VendorBalance.STREAM_CODEC.decode(bytes))&&!bytes.isReadable(),"Scoped balance wire roundtrip");
            var result=new VendorPayloads.S2C_VendorPurchaseResult(7,reopened,false,"Capacity reached",Long.MAX_VALUE);
            VendorPayloads.S2C_VendorPurchaseResult.STREAM_CODEC.encode(bytes,result);
            check(result.equals(VendorPayloads.S2C_VendorPurchaseResult.STREAM_CODEC.decode(bytes))&&!bytes.isReadable(),"Scoped result long/session wire roundtrip");
        } finally {bytes.release();}
        System.out.println(checks+" menu session/balance checks passed");
    }
}
