package net.goui.cosmicdungeon.client.screen.skills;

import java.util.*;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.goui.cosmicdungeon.network.TheurgistPayloads.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

final class TheurgistClientTest {
    private final List<TheurgistPayloads.Action> sent=new ArrayList<>();
    private SkillsPanelModel base(){return SkillsPanelModel.initial("theurgist");}
    private SkillsPanelModel.Action row(String id){
        return TheurgistClient.augment(base()).actions().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();
    }
    @BeforeEach void setup(){TheurgistClient.clear();TheurgistClient.actions(sent::add);}
    @AfterEach void cleanup(){TheurgistClient.clear();}
    @Test void onlyAuthoritativeCapabilitiesEnableCraftingAndUnchangedAcknowledgementReleasesPending(){
        assertFalse(row(TheurgistClient.CRAFT).enabled());
        assertFalse(TheurgistClient.activate(TheurgistClient.CRAFT));
        var view=new View(22,8,true,true,true,false,List.of(),null);TheurgistClient.receive(view);
        assertTrue(row(TheurgistClient.CRAFT).enabled());assertFalse(row(TheurgistClient.EPIC).enabled());
        assertTrue(row(TheurgistClient.CRAFT).label().contains("[20]"));assertTrue(row(TheurgistClient.EPIC).label().contains("[40]"));
        assertTrue(TheurgistClient.activate(TheurgistClient.CRAFT));
        assertEquals(new TheurgistPayloads.Action(22,8,Kind.CRAFT,TheurgistPayloads.NONE,TheurgistPayloads.NONE),sent.getFirst());
        assertTrue(TheurgistClient.pending());assertFalse(row(TheurgistClient.CRAFT).enabled());
        assertFalse(TheurgistClient.activate(TheurgistClient.CRAFT));assertEquals(1,sent.size());
        TheurgistClient.receive(view);assertFalse(TheurgistClient.pending());assertTrue(row(TheurgistClient.CRAFT).enabled());
        TheurgistClient.receive(new View(22,9,true,true,true,true,List.of(),null));
        assertTrue(TheurgistClient.activate(TheurgistClient.EPIC));assertEquals(Kind.EPIC,sent.getLast().kind());
    }
    @Test void resurrectionUsesTheExactDisplayedDeathAndAvailabilityWithoutCooldown(){
        var target=new Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),true);
        TheurgistClient.receive(new View(22,10,true,true,false,false,List.of(target),null));
        String original=TheurgistClient.actionId(target);
        assertTrue(row(original).enabled());assertEquals("Resurrect Teammate [120]",row(original).label());
        assertTrue(row(original).tooltip().getString().contains("only if they accept"));
        var replacement=new Target(target.player(),target.name(),UUID.randomUUID(),true);
        TheurgistClient.receive(new View(22,11,true,true,false,false,List.of(replacement),null));
        assertFalse(TheurgistClient.activate(original));assertTrue(sent.isEmpty());
        assertTrue(TheurgistClient.activate(TheurgistClient.actionId(replacement)));
        assertEquals(new TheurgistPayloads.Action(22,11,Kind.OFFER,replacement.player(),replacement.death()),sent.getFirst());
        TheurgistClient.receive(new View(22,12,true,true,false,false,List.of(replacement),null));
        assertTrue(TheurgistClient.activate(TheurgistClient.actionId(replacement)),"Acknowledgement, not an invented cooldown, gates the next action");
        TheurgistClient.receive(new View(22,13,true,true,false,false,
                List.of(new Target(target.player(),target.name(),replacement.death(),false)),null));
        assertFalse(TheurgistClient.activate(TheurgistClient.actionId(replacement)));
    }
    @Test void deathOffersUseCasterAndOfferIdWithExplicitAcceptDeclineAndHonestPayer(){
        var offer=new Offer(UUID.randomUUID(),UUID.randomUUID(),"Caster",UUID.randomUUID());
        var view=new View(22,20,false,false,false,false,List.of(),offer);TheurgistClient.receive(view);
        assertEquals(offer,TheurgistClient.offer());assertTrue(TheurgistClient.canRespond());
        assertTrue(TheurgistClient.acceptOffer());
        assertEquals(new TheurgistPayloads.Action(22,20,Kind.ACCEPT,offer.caster(),offer.id()),sent.getFirst());
        assertFalse(TheurgistClient.declineOffer());assertEquals(offer,TheurgistClient.offer());
        assertTrue(TheurgistClient.offerTooltip(offer).getString().contains("Caster pays 120 Brewing Supplies only if resurrection succeeds"));
        TheurgistClient.receive(view);assertTrue(TheurgistClient.declineOffer());
        assertEquals(Kind.DECLINE,sent.getLast().kind());assertEquals(offer.id(),sent.getLast().token());
        TheurgistClient.receive(new View(22,21,false,false,false,false,List.of(),null));
        assertNull(TheurgistClient.offer());assertFalse(TheurgistClient.acceptOffer());
        TheurgistClient.receive(new View(22,22,false,true,false,false,List.of(),offer));
        assertNull(TheurgistClient.offer());assertFalse(TheurgistClient.acceptOffer(),"Living clients cannot accept a death offer");
    }
    @Test void sameRunRevisionsNeverRegressAndRunChangesLogoutAndMissingEligibilityClearActions(){
        TheurgistClient.receive(new View(22,50,true,true,true,true,List.of(),null));
        TheurgistClient.activate(TheurgistClient.CRAFT);
        TheurgistClient.receive(new View(22,49,true,true,false,false,List.of(),null));
        assertFalse(TheurgistClient.pending());assertTrue(row(TheurgistClient.EPIC).enabled());
        TheurgistClient.receive(new View(23,1,true,true,false,true,List.of(),null));
        assertTrue(TheurgistClient.activate(TheurgistClient.EPIC));assertEquals(23,sent.getLast().run());assertEquals(1,sent.getLast().revision());
        TheurgistClient.receive(View.empty(51));assertFalse(TheurgistClient.activate(TheurgistClient.EPIC));
        assertFalse(row(TheurgistClient.CRAFT).enabled());assertFalse(row("theurgist_resurrect_none").enabled());
        TheurgistClient.receive(new View(23,2,true,true,true,true,List.of(),null));TheurgistClient.clear();
        assertFalse(TheurgistClient.activate(TheurgistClient.CRAFT));assertNull(TheurgistClient.offer());
        var other=SkillsPanelModel.initial("bogatyr");assertSame(other,TheurgistClient.augment(other));
    }
    @Test void allDeadTeammatesRemainReviewableThroughExistingPanelOverflow(){
        var targets=new ArrayList<Target>();
        for(int i=0;i<TheurgistPayloads.MAX_TARGETS;i++)targets.add(new Target(UUID.randomUUID(),"Teammate "+i,UUID.randomUUID(),i%2==0));
        TheurgistClient.receive(new View(22,1,true,true,true,true,targets,null));var model=TheurgistClient.augment(base());
        assertEquals(67,model.actions().size());assertEquals(base().actions().getFirst(),model.actions().getFirst());
        assertEquals(64,model.actions().stream().filter(a->a.id().startsWith(TheurgistClient.RESURRECT)).count());
        var panel=new SkillsPanelState(SharedInventoryLayout.of(320,240,72,37,176,166),null);panel.actions(model.actions().size());
        assertTrue(panel.scroll().max()>0);assertTrue(panel.scroll().thumbHeight()>0);
        assertEquals(32,model.actions().stream().filter(a->a.id().startsWith(TheurgistClient.RESURRECT)&&a.enabled()).count());
    }
}
