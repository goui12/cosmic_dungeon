package net.goui.cosmicdungeon.playerclass.resource;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SupplyTransferPlanTest {
    private static final UUID A=new UUID(1,1),B=new UUID(2,2);
    static SupplyTransferPlan plan(){
        var inventory=new CompoundTag();inventory.putString("native_fixture","before");
        var after=new CompoundTag();after.putString("native_fixture","after");
        var ledger=ClassResourceLedger.forRun(new CompoundTag(),9).credit(ClassResourceKind.KIBBLE,595);
        return SupplyTransferPlan.create(A,B,9,ClassResourceKind.KIBBLE,5,inventory,after,ledger);
    }
    @Test void nativeNbtRoundTripPreservesUnknownFieldsAndExactOwnerEvidence(){
        var image=plan().image();image.putString("future","preserved");
        var p=SupplyTransferPlan.CODEC.parse(NbtOps.INSTANCE,image).getOrThrow();
        assertEquals(image,SupplyTransferPlan.CODEC.encodeStart(NbtOps.INSTANCE,p).getOrThrow());
        var reservation=p.reservation(A);assertTrue(p.commit().recoverable(A,reservation,new CompoundTag()));
        var changed=p.image();changed.getCompoundOrEmpty("inventory_after").putString("tampered","value");
        assertFalse(new SupplyTransferPlan(changed).commit().recoverable(A,reservation,new CompoundTag()));
        var copy=p.image();copy.putString("future","changed");assertEquals("preserved",p.image().getStringOr("future",""));
    }
    @Test void uncommittedCrashPrefixesCancelWithoutReconstructingItems(){
        var p=plan();var empty=new CompoundTag();
        assertTrue(p.recoverable(A,empty,empty));assertTrue(p.recoverable(B,empty,empty));
        assertTrue(p.recoverable(A,p.reservation(A),empty));
        assertTrue(p.recoverable(A,empty,p.receipt(A)));
        assertFalse(p.recoverable(A,p.reservation(B),empty));
        assertThrows(IllegalStateException.class,()->p.acknowledge(A).commit());
    }
    @Test void committedRecoveryRequiresExactPreparedOwnerOrSavedReceipt(){
        var p=plan().commit();var empty=new CompoundTag();
        assertFalse(p.recoverable(A,empty,empty));assertTrue(p.recoverable(A,p.reservation(A),empty));
        assertTrue(p.recoverable(A,empty,p.receipt(A)));assertFalse(p.recoverable(A,p.reservation(A),p.receipt(A)));
        assertFalse(p.recoverable(A,empty,p.receipt(B)));
        var acknowledged=p.acknowledge(A);assertFalse(acknowledged.recoverable(A,p.reservation(A),empty));
        assertTrue(acknowledged.recoverable(A,empty,p.receipt(A)));
        assertTrue(acknowledged.acknowledge(B).complete());
    }
    @Test void corruptFutureOrUnrelatedResourceChangesFailClosed(){
        for(int mode=0;mode<5;mode++){
            var image=plan().image();
            switch(mode){
                case 0 -> image.putInt("schema",2);
                case 1 -> image.putString("recipient",A.toString());
                case 2 -> image.putInt("yield",6);
                case 3 -> image.getCompoundOrEmpty("resource_after").getCompoundOrEmpty("balances").putInt("brewing_supplies",1);
                default -> image.putLong("run",0);
            }
            var before=image.copy();assertThrows(RuntimeException.class,()->new SupplyTransferPlan(image));assertEquals(before,image);
        }
    }
    @Test void additiveJournalDefaultsIndexesAcksAndFutureFailure(){
        var raw=new CompoundTag();raw.putInt("schema",1);raw.putString("future","kept");
        var data=new SupplyTransferData(raw);assertNull(data.pending(A));
        var p=plan();data.reserve(p);assertEquals(p.id(),data.pending(B).id());
        assertThrows(IllegalStateException.class,()->data.reserve(plan()));
        data.commit(p.id());data.acknowledge(p.id(),A);
        var reloaded=SupplyTransferData.CODEC.parse(NbtOps.INSTANCE,data.image()).getOrThrow();
        assertTrue(reloaded.pending(A).acknowledged(A));assertEquals("kept",reloaded.image().getStringOr("future",""));
        reloaded.acknowledge(p.id(),B);assertNull(reloaded.pending(A));assertNull(reloaded.pending(B));
        assertTrue(reloaded.image().getCompoundOrEmpty("pending").isEmpty());
        raw.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->new SupplyTransferData(raw));
    }
}
