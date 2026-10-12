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
    @Test void retiredRandomPotionActionsNeverAppearOrSendEvenFromLegacyViews(){
        TheurgistClient.receive(new View(22,8,true,true,true,true,List.of(),null));
        assertFalse(TheurgistClient.activate(TheurgistClient.CRAFT));
        assertFalse(TheurgistClient.activate(TheurgistClient.EPIC));
        assertTrue(TheurgistClient.augment(base()).actions().stream().noneMatch(action ->
                action.id().equals(TheurgistClient.CRAFT)||action.id().equals(TheurgistClient.EPIC)));
        assertTrue(sent.isEmpty());
    }
    @Test void resurrectionUsesTheExactDisplayedDeathAndAuthoritativeAvailability(){
        var target=new Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),true);
        TheurgistClient.receive(new View(22,10,true,true,false,false,List.of(target),null));
        String original=TheurgistClient.actionId(target);
        assertTrue(row(original).enabled());assertEquals("Resurrect Teammate",row(original).label());
        assertTrue(row(original).tooltip().getString().contains("teammate must accept"));
        var replacement=new Target(target.player(),target.name(),UUID.randomUUID(),true);
        TheurgistClient.receive(new View(22,11,true,true,false,false,List.of(replacement),null));
        assertFalse(TheurgistClient.activate(original));assertTrue(sent.isEmpty());
        assertTrue(TheurgistClient.activate(TheurgistClient.actionId(replacement)));
        assertEquals(new TheurgistPayloads.Action(22,11,Kind.OFFER,replacement.player(),replacement.death()),sent.getFirst());
        TheurgistClient.receive(new View(22,12,true,true,false,false,List.of(replacement),null));
        assertTrue(TheurgistClient.activate(TheurgistClient.actionId(replacement)),"Only the authoritative availability, never a client timer, permits another offer");
        TheurgistClient.receive(new View(22,13,true,true,false,false,
                List.of(new Target(target.player(),target.name(),replacement.death(),false)),null));
        assertFalse(TheurgistClient.activate(TheurgistClient.actionId(replacement)));
    }
    @Test void deathOffersUseCasterAndOfferIdWithExplicitAcceptDeclineAndNoCost(){
        var offer=new Offer(UUID.randomUUID(),UUID.randomUUID(),"Caster",UUID.randomUUID());
        var view=new View(22,20,false,false,false,false,List.of(),offer);TheurgistClient.receive(view);
        assertEquals(offer,TheurgistClient.offer());assertTrue(TheurgistClient.canRespond());
        assertTrue(TheurgistClient.acceptOffer());
        assertEquals(new TheurgistPayloads.Action(22,20,Kind.ACCEPT,offer.caster(),offer.id()),sent.getFirst());
        assertFalse(TheurgistClient.declineOffer());assertEquals(offer,TheurgistClient.offer());
        assertTrue(TheurgistClient.offerTooltip(offer).getString().contains("No item or resource cost. Caster starts a 3-minute cooldown only if resurrection succeeds"));
        TheurgistClient.receive(view);assertTrue(TheurgistClient.declineOffer());
        assertEquals(Kind.DECLINE,sent.getLast().kind());assertEquals(offer.id(),sent.getLast().token());
        TheurgistClient.receive(new View(22,21,false,false,false,false,List.of(),null));
        assertNull(TheurgistClient.offer());assertFalse(TheurgistClient.acceptOffer());
        TheurgistClient.receive(new View(22,22,false,true,false,false,List.of(),offer));
        assertNull(TheurgistClient.offer());assertFalse(TheurgistClient.acceptOffer(),"Living clients cannot accept a death offer");
    }
    @Test void sameRunRevisionsRunChangesLogoutAndMissingEligibilityPreserveResurrection(){
        var target=new Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),true);
        var id=TheurgistClient.actionId(target);
        TheurgistClient.receive(new View(22,50,true,true,false,false,List.of(target),null));
        assertTrue(TheurgistClient.activate(id));assertTrue(TheurgistClient.pending());
        TheurgistClient.receive(new View(22,49,true,true,false,false,List.of(),null));
        assertFalse(TheurgistClient.pending());assertEquals(50,TheurgistClient.view().revision());
        assertTrue(row(id).enabled());
        TheurgistClient.receive(new View(23,1,true,true,false,false,List.of(target),null));
        assertTrue(TheurgistClient.activate(id));assertEquals(23,sent.getLast().run());assertEquals(1,sent.getLast().revision());
        TheurgistClient.receive(View.empty(51));assertFalse(TheurgistClient.activate(id));assertFalse(TheurgistClient.canOpenPicker());
        TheurgistClient.receive(new View(23,2,true,true,false,false,List.of(target),null));TheurgistClient.clear();
        assertFalse(TheurgistClient.activate(id));assertNull(TheurgistClient.offer());
        var other=SkillsPanelModel.initial("bogatyr");assertSame(other,TheurgistClient.augment(other));
    }
    @Test void allDeadTeammatesRemainReviewableThroughExistingPanelOverflow(){
        var targets=new ArrayList<Target>();
        for(int i=0;i<TheurgistPayloads.MAX_TARGETS;i++)targets.add(new Target(UUID.randomUUID(),"Teammate "+i,UUID.randomUUID(),i%2==0));
        TheurgistClient.receive(new View(22,1,true,true,true,true,targets,null));var model=TheurgistClient.augment(base());
        assertEquals(65,model.actions().size());assertEquals(base().actions().getFirst(),model.actions().getFirst());
        assertEquals(64,model.actions().stream().filter(a->a.id().startsWith(TheurgistClient.RESURRECT)).count());
        var panel=new SkillsPanelState(SharedInventoryLayout.of(320,240,72,37,176,166),null);panel.actions(model.actions().size());
        assertTrue(panel.scroll().max()>0);assertTrue(panel.scroll().thumbHeight()>0);
        assertEquals(32,model.actions().stream().filter(a->a.id().startsWith(TheurgistClient.RESURRECT)&&a.enabled()).count());
    }
    @Test void pickerAvailabilityAndCooldownAreAuthoritativeAndDoNotInventAnUnlock(){
        var ready=new Resurrection("READY",0);
        TheurgistClient.receive(new View(22,1,true,true,false,false,List.of(),null,ready,List.of()));
        assertTrue(TheurgistClient.canOpenPicker());assertTrue(TheurgistClient.resurrectionTooltip().getString().contains("No item or resource cost"));
        var target=new Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),false);
        TheurgistClient.receive(new View(22,2,true,true,false,false,List.of(target),null,new Resurrection("COOLDOWN",180),List.of()));
        assertTrue(TheurgistClient.canOpenPicker(),"Cooldown still allows reviewing the picker");
        assertTrue(TheurgistClient.resurrectionTooltip().getString().contains("3:00"));
        assertFalse(TheurgistClient.activate(TheurgistClient.actionId(target)));
        TheurgistClient.receive(new View(22,3,true,true,false,false,List.of(target),null,new Resurrection("READY",0),List.of()));
        assertFalse(TheurgistClient.activate(TheurgistClient.actionId(target)),"A zero cooldown alone never overrides target capability");
        TheurgistClient.receive(new View(22,4,true,false,false,false,List.of(),null,new Resurrection("DEAD",30),List.of()));
        assertFalse(TheurgistClient.canOpenPicker());TheurgistClient.clear();assertFalse(TheurgistClient.canOpenPicker());
    }
}
