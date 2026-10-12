package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.network.TheurgistPayloads.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryResurrectionPresentationTest {
    @AfterEach void clear() { TheurgistClient.clear(); }
    private PartyPayloads.Mercenary mercenary(PartyPayloads.Resurrection resurrection) {
        return new PartyPayloads.Mercenary("Beatrice","Tester",20,20,0,"ACTIVE",
                PartyPayloads.Recovery.NONE,List.of(),resurrection,"theurgist");
    }
    @Test void sameRunExactIdentityShowsUnavailableReasonAlongsideCooldown() {
        UUID id=UUID.randomUUID();
        var caster=new Caster(id,"Beatrice",true,new Resurrection("HIRER_UNAVAILABLE",45));
        TheurgistClient.receive(new View(22,1,false,true,false,false,List.of(),null,Resurrection.UNAVAILABLE,List.of(caster)));
        var row=mercenary(new PartyPayloads.Resurrection(22,id.toString(),"",45));
        assertTrue(MercenaryHudLayout.tooltip(row).contains("Resurrection: Hirer unavailable / cooldown 0:45"));
        var newerRun=mercenary(new PartyPayloads.Resurrection(23,id.toString(),"",0));
        assertTrue(MercenaryHudLayout.tooltip(newerRun).contains("Resurrection: Ready"),"An older run cannot override the current HUD");
    }
    @Test void lockedAndAmbiguousNamesNeverBorrowAnotherMercenaryReadiness() {
        assertTrue(MercenaryHudLayout.tooltip(mercenary(PartyPayloads.Resurrection.LOCKED)).contains("Resurrection: Locked"));
        var first=new Caster(UUID.randomUUID(),"Beatrice",true,new Resurrection("READY",0));
        var second=new Caster(UUID.randomUUID(),"Beatrice",true,new Resurrection("COOLDOWN",180));
        TheurgistClient.receive(new View(22,1,false,true,false,false,List.of(),null,Resurrection.UNAVAILABLE,List.of(first,second)));
        assertTrue(MercenaryHudLayout.tooltip(mercenary(PartyPayloads.Resurrection.LOCKED)).contains("Resurrection: Locked"));
    }
}
