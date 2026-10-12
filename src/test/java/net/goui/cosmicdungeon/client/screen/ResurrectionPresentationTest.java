package net.goui.cosmicdungeon.client.screen;

import java.util.HashSet;
import java.util.UUID;
import net.goui.cosmicdungeon.network.TheurgistPayloads.Caster;
import net.goui.cosmicdungeon.network.TheurgistPayloads.Resurrection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ResurrectionPresentationTest {
    @Test void unavailableCastersKeepTheirKnownCooldownWithoutClaimingReadiness() {
        assertEquals("Ready", ResurrectionPresentation.status(new Resurrection("READY",0)));
        assertEquals("Cooldown: 3:00", ResurrectionPresentation.status(new Resurrection("COOLDOWN",180)));
        assertEquals("Dead / cooldown 0:01", ResurrectionPresentation.status(new Resurrection("DEAD",1)));
        assertEquals("Offline / cooldown 2:59", ResurrectionPresentation.status(new Resurrection("OFFLINE",179)));
        assertEquals("Outside this dungeon / cooldown ready", ResurrectionPresentation.status(new Resurrection("OUT_OF_RUN",0)));
        assertEquals("Different dimension / cooldown 1:00", ResurrectionPresentation.status(new Resurrection("DIFFERENT_DIMENSION",60)));
        assertEquals("Locked", ResurrectionPresentation.status(new Resurrection("LOCKED",-1)));
        assertEquals("Resurrection pending / cooldown ready", ResurrectionPresentation.status(new Resurrection("PENDING",0)));
        assertEquals("Hirer unavailable", ResurrectionPresentation.status(new Resurrection("HIRER_UNAVAILABLE",-1)));
        assertEquals("Unavailable", ResurrectionPresentation.status(new Resurrection("UNAVAILABLE",-1)));
    }
    @Test void namedRowsPreserveEachCasterAndTwoRowPaginationCanReachAll67() {
        var caster = new Caster(UUID.randomUUID(),"Beatrice",true,new Resurrection("COOLDOWN",61));
        assertEquals("Beatrice: Cooldown: 1:01",ResurrectionPresentation.line(caster));
        var reached = new HashSet<Integer>();
        for (int page = 0; page < 34; page++) {
            var slice = ResurrectionPresentation.page(page,67,2);
            assertEquals(34,slice.pages()); assertEquals(page,slice.index());
            assertTrue(slice.end()-slice.start()<=2);
            for (int i=slice.start();i<slice.end();i++) assertTrue(reached.add(i));
        }
        assertEquals(67,reached.size());
    }
    @Test void pagesClampAfterRosterShrinkAndHandleEmptyAndOverflowedRequests() {
        assertEquals(new ResurrectionPresentation.Page(0,1,0,0),ResurrectionPresentation.page(100,0,2));
        assertEquals(new ResurrectionPresentation.Page(0,1,0,1),ResurrectionPresentation.page(20,1,2));
        assertEquals(new ResurrectionPresentation.Page(0,3,0,2),ResurrectionPresentation.page(-10,5,2));
        assertEquals(new ResurrectionPresentation.Page(2,3,4,5),ResurrectionPresentation.page(Integer.MAX_VALUE,5,2));
        assertThrows(IllegalArgumentException.class,()->ResurrectionPresentation.page(0,-1,2));
        assertThrows(IllegalArgumentException.class,()->ResurrectionPresentation.page(0,1,0));
    }
}
