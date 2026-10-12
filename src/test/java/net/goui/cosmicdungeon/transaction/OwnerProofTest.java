package net.goui.cosmicdungeon.transaction;

import java.util.List;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OwnerProofTest {
    @Test void missingProofRetainsPartialPreparationWithoutInstallingData(){
        var parent=new CompoundTag();parent.putString("future","retained");var before=parent.copy();
        assertTrue(PlayerSaveProof.ownerCompound(parent,"reservation").isEmpty());
        assertFalse(parent.contains("reservation"));assertEquals(before,parent);
    }
    @Test void validEmptyAndUnknownCompoundEvidenceRemainIntact(){
        var parent=new CompoundTag();var evidence=new CompoundTag();evidence.putString("future_owner","untouched");
        parent.put("receipt",evidence);var before=parent.copy();
        assertSame(evidence,PlayerSaveProof.ownerCompound(parent,"receipt"));assertEquals(before,parent);
        parent.put("empty",new CompoundTag());assertTrue(PlayerSaveProof.ownerCompound(parent,"empty").isEmpty());assertTrue(parent.contains("empty"));
    }
    @Test void presentNonCompoundEvidenceNeverBecomesAbsentOrChangesOriginalBytes(){
        for(Tag bad:List.<Tag>of(StringTag.valueOf("unsupported-proof"),new ListTag(),IntTag.valueOf(4))){
            var parent=new CompoundTag();parent.put("reservation",bad);parent.putString("foreign","preserved");var before=parent.copy();
            var error=assertThrows(IllegalStateException.class,()->PlayerSaveProof.ownerCompound(parent,"reservation"));
            assertTrue(error.getMessage().contains("reservation"));assertTrue(error.getMessage().contains("requires review"));assertEquals(before,parent);
        }
    }
    @Test void nestedMalformedOwnerRootFailsBeforeReadingItsProofFields(){
        var nativeData=new CompoundTag();nativeData.putString("class_root","unsupported");var before=nativeData.copy();
        assertThrows(IllegalStateException.class,()->PlayerSaveProof.ownerCompound(PlayerSaveProof.ownerCompound(nativeData,"class_root"),"receipt"));
        assertEquals(before,nativeData);
    }
    @Test void supportedPriorReceiptsKeepAllFieldsWhileFutureOrForeignEvidenceHolds(){
        var owner=new java.util.UUID(0,1);var other=new java.util.UUID(0,2);
        for(int schema=1;schema<=3;schema++){
            var proof=new CompoundTag();proof.putInt("schema",schema);proof.putString("owner",owner.toString());
            proof.putString("id",java.util.UUID.randomUUID().toString());proof.putString("future_compatible_field","retained");
            var parent=new CompoundTag();parent.put("receipt",proof);var before=parent.copy();
            assertSame(proof,PlayerSaveProof.ownerReceipt(parent,"receipt",owner,3,"id"));assertEquals(before,parent);
            proof.putInt("schema",99);var unsupported=parent.copy();
            assertThrows(IllegalStateException.class,()->PlayerSaveProof.ownerReceipt(parent,"receipt",owner,3,"id"));assertEquals(unsupported,parent);
            proof.putInt("schema",schema);proof.putString("owner",other.toString());var foreign=parent.copy();
            assertThrows(IllegalStateException.class,()->PlayerSaveProof.ownerReceipt(parent,"receipt",owner,3,"id"));assertEquals(foreign,parent);
        }
    }
    @Test void malformedReceiptIdentityNeverBecomesAnOldTransaction(){
        var owner=new java.util.UUID(0,1);var parent=new CompoundTag();var proof=new CompoundTag();
        proof.putInt("schema",1);proof.putString("owner",owner.toString());proof.putString("transaction","not-a-uuid");parent.put("receipt",proof);
        var before=parent.copy();assertThrows(IllegalStateException.class,()->PlayerSaveProof.ownerReceipt(parent,"receipt",owner,1,"transaction"));assertEquals(before,parent);
        assertTrue(PlayerSaveProof.ownerReceipt(new CompoundTag(),"receipt",owner,1,"transaction").isEmpty());
    }

}
