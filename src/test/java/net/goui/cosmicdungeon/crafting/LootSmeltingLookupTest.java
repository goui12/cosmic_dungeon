package net.goui.cosmicdungeon.crafting;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LootSmeltingLookupTest {
    @Test void onlyTheExactQueryInputIsExempt(){
        var input=new String("same");var equalInput=new String("same");
        assertFalse(LootSmeltingLookup.permits(input));
        assertEquals("result",LootSmeltingLookup.query(input,()->{
            assertTrue(LootSmeltingLookup.permits(input));
            assertFalse(LootSmeltingLookup.permits(equalInput));
            assertFalse(LootSmeltingLookup.permits(null));
            return "result";
        }));
        assertFalse(LootSmeltingLookup.permits(input));
    }
    @Test void nestedAndExceptionalQueriesRestoreThenClearTheOriginalScope(){
        var outer=new Object();var inner=new Object();var failure=new IllegalStateException("fixture");
        assertSame(failure,assertThrows(IllegalStateException.class,()->LootSmeltingLookup.query(outer,()->{
            assertSame(failure,assertThrows(IllegalStateException.class,()->LootSmeltingLookup.query(inner,()->{
                assertTrue(LootSmeltingLookup.permits(inner));
                assertFalse(LootSmeltingLookup.permits(outer));
                throw failure;
            })));
            assertTrue(LootSmeltingLookup.permits(outer));
            assertFalse(LootSmeltingLookup.permits(inner));
            throw failure;
        })));
        assertFalse(LootSmeltingLookup.permits(outer));
        assertFalse(LootSmeltingLookup.permits(inner));
    }
    @Test void unrelatedThreadNeverInheritsLootAuthority(){
        var input=new Object();var inherited=new AtomicBoolean();
        LootSmeltingLookup.query(input,()->{
            var other=new Thread(()->inherited.set(LootSmeltingLookup.permits(input)));
            other.start();
            try{other.join();}
            catch(InterruptedException error){Thread.currentThread().interrupt();throw new AssertionError(error);}
            assertTrue(LootSmeltingLookup.permits(input));
            return null;
        });
        assertFalse(inherited.get());
        assertFalse(LootSmeltingLookup.permits(input));
    }
}
