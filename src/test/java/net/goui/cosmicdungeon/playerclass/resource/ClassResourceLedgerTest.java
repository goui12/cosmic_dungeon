package net.goui.cosmicdungeon.playerclass.resource;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind.*;
class ClassResourceLedgerTest {
    @Test void oldSaveDefaultsAdditivelyAndPreservesUnknownData(){
        var root=new CompoundTag();root.putString("class_id","theurgist");root.putString("future","retained");
        var ledger=ClassResourceLedger.forRun(root,7);
        assertEquals(0,ledger.amount(BREWING_SUPPLIES));assertEquals(0,ledger.amount(KIBBLE));
        assertFalse(root.contains(ClassResourceLedger.KEY));
        assertEquals("retained",ledger.applyTo(root).getStringOr("future",""));
    }
    @Test void roundTripKeepsBalancesRevisionAndUnknownLedgerFields(){
        var ledger=ClassResourceLedger.forRun(new CompoundTag(),7).credit(BREWING_SUPPLIES,12).credit(KIBBLE,31).nextRevision();
        var root=ledger.applyTo(new CompoundTag());var image=root.getCompoundOrEmpty(ClassResourceLedger.KEY);
        image.putString("future","keep");image.getCompoundOrEmpty("balances").putInt("future_resource",99);
        var decoded=ClassResourceLedger.forRun(root.copy(),7);
        assertEquals(12,decoded.amount(BREWING_SUPPLIES));assertEquals(31,decoded.amount(KIBBLE));
        assertEquals(1,decoded.revision());assertEquals("keep",decoded.image().getStringOr("future",""));
        assertEquals(99,decoded.image().getCompoundOrEmpty("balances").getIntOr("future_resource",0));
    }
    @Test void newRunResetsOnlyKnownBalancesAndToken(){
        var old=ClassResourceLedger.forRun(new CompoundTag(),7).credit(BREWING_SUPPLIES,600).credit(KIBBLE,600).nextRevision();
        var root=old.applyTo(new CompoundTag());root.getCompoundOrEmpty(ClassResourceLedger.KEY).putString("future","keep");
        var next=ClassResourceLedger.forRun(root,8);
        assertEquals(8,next.runId());assertEquals(0,next.revision());assertEquals(0,next.amount(KIBBLE));
        assertEquals(0,next.amount(BREWING_SUPPLIES));assertEquals("keep",next.image().getStringOr("future",""));
        assertEquals(600,ClassResourceLedger.forRun(root,7).amount(KIBBLE));
    }
    @Test void capIsOverflowSafeAndNegativeValuesFail(){
        var value=ClassResourceLedger.forRun(new CompoundTag(),1).credit(BREWING_SUPPLIES,599);
        assertEquals(600,value.credit(BREWING_SUPPLIES,Integer.MAX_VALUE).amount(BREWING_SUPPLIES));
        assertEquals(0,value.amount(KIBBLE));
        assertThrows(IllegalArgumentException.class,()->value.credit(KIBBLE,-1));
        assertThrows(IllegalArgumentException.class,()->value.withAmount(KIBBLE,601));
        assertThrows(IllegalArgumentException.class,()->ClassResourceLedger.forRun(new CompoundTag(),0));
    }
    @Test void futureOrCorruptSaveFailsClosedWithoutMutation(){
        var ledger=ClassResourceLedger.forRun(new CompoundTag(),1).credit(KIBBLE,9);
        for(int mode=0;mode<6;mode++){
            var root=ledger.applyTo(new CompoundTag());var image=root.getCompoundOrEmpty(ClassResourceLedger.KEY);
            switch(mode){
                case 0 -> image.putInt("schema",2);
                case 1 -> image.putString("balances","bad");
                case 2 -> image.putInt("run_id",1);
                case 3 -> image.getCompoundOrEmpty("balances").putInt("kibble",601);
                case 4 -> image.putLong("revision",-1);
                default -> image.getCompoundOrEmpty("balances").putString("kibble","9");
            }
            var before=root.copy();
            assertThrows(IllegalArgumentException.class,()->ClassResourceLedger.forRun(root,2));
            assertEquals(before,root);
        }
    }
    @Test void copyBoundariesAndMissingAdditiveFieldsAreSafe(){
        var ledger=ClassResourceLedger.forRun(new CompoundTag(),1);
        var image=ledger.image();image.putLong("run_id",2);assertEquals(1,ledger.runId());
        var root=ledger.applyTo(new CompoundTag());var saved=root.getCompoundOrEmpty(ClassResourceLedger.KEY);
        saved.remove("balances");saved.remove("revision");
        var legacy=ClassResourceLedger.forRun(root,1);assertEquals(0,legacy.amount(KIBBLE));assertEquals(0,legacy.revision());
        saved.putLong("revision",Long.MAX_VALUE);
        assertThrows(IllegalStateException.class,()->ClassResourceLedger.forRun(root,1).nextRevision());
    }
}
