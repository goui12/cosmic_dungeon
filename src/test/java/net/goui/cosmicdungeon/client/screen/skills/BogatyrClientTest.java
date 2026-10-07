package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.List;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Quote;
import net.goui.cosmicdungeon.network.BogatyrPayloads.View;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BogatyrClientTest {
    private final List<BogatyrPayloads.Action> sent=new ArrayList<>();
    private SkillsPanelModel base(){return SkillsPanelModel.initial("bogatyr");}
    private SkillsPanelModel.Action row(String id){
        return BogatyrClient.augment(base()).actions().stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();
    }
    private View view(long run,long revision){
        return new View(run,revision,7,List.of(new Quote(1,5,true),Quote.empty(),
                Quote.empty(),new Quote(2,10,true)));
    }
    @BeforeEach void setup(){BogatyrClient.clear();BogatyrClient.actions(sent::add);}
    @AfterEach void cleanup(){BogatyrClient.clear();}
    @Test void partialBreedAndHealDisplayExactServerQuotesWithoutInventingEligibility(){
        assertFalse(row(BogatyrClient.BREED).enabled());assertFalse(BogatyrClient.activate(BogatyrClient.BREED));
        BogatyrClient.accept(view(23,4));
        assertEquals("Breed 1 [5]",row(BogatyrClient.BREED).label());
        assertEquals("Heal 2 [10]",row(BogatyrClient.HEAL).label());
        assertTrue(row(BogatyrClient.BREED).enabled());assertTrue(row(BogatyrClient.HEAL).enabled());
        assertTrue(row(BogatyrClient.BREED).tooltip().getString().contains("even one wolf"));
        assertTrue(row(BogatyrClient.HEAL).tooltip().getString().contains("lowest health"));
        assertTrue(row(BogatyrClient.HEAL).tooltip().getString().contains("Selected wolves: 2"));
        assertTrue(row(BogatyrClient.HEAL).tooltip().getString().contains("Total cost: 10 Kibble"));
        assertFalse(row(BogatyrClient.SUMMON).enabled(),"A server placement rejection remains disabled");
        assertFalse(row(BogatyrClient.REGROUP).enabled(),"Loaded wolves alone are not partial Regroup consent");
        assertTrue(row(BogatyrClient.REGROUP).tooltip().getString().contains("entire affected pack"));
        assertTrue(row(BogatyrClient.REGROUP).tooltip().getString().contains("Loaded wolves in this dungeon: 7"));
        assertEquals("Regroup 0 [0]",row(BogatyrClient.REGROUP).label());
        assertFalse(BogatyrClient.activate(BogatyrClient.REGROUP));assertTrue(sent.isEmpty());
        assertTrue(BogatyrClient.activate(BogatyrClient.BREED));
        assertEquals(new BogatyrPayloads.Action(23,4,Kind.BREED),sent.getFirst());
    }
    @Test void summonAndWholePackRegroupUseTheCapturedServerQuote(){
        var quotes=List.of(Quote.empty(),new Quote(1,30,true),new Quote(7,7,true),Quote.empty());
        BogatyrClient.accept(new View(23,1,7,quotes));
        assertEquals("Summon 1 [30]",row(BogatyrClient.SUMMON).label());
        assertTrue(row(BogatyrClient.SUMMON).tooltip().getString().contains("only when safe placement succeeds"));
        assertTrue(BogatyrClient.activate(BogatyrClient.SUMMON));
        assertEquals(new BogatyrPayloads.Action(23,1,Kind.SUMMON),sent.getLast());
        BogatyrClient.accept(new View(23,2,7,quotes));
        assertEquals("Regroup 7 [7]",row(BogatyrClient.REGROUP).label());
        assertTrue(BogatyrClient.activate(BogatyrClient.REGROUP));
        assertEquals(new BogatyrPayloads.Action(23,2,Kind.REGROUP),sent.getLast());
    }
    @Test void eachCommandIsCapturedOnceUntilANewerAuthoritativeAcknowledgement(){
        var initial=view(23,10);BogatyrClient.accept(initial);
        assertTrue(BogatyrClient.activate(BogatyrClient.HEAL));assertTrue(BogatyrClient.pending());
        assertFalse(BogatyrClient.activate(BogatyrClient.BREED));assertFalse(row(BogatyrClient.HEAL).enabled());
        BogatyrClient.accept(initial);assertTrue(BogatyrClient.pending());
        BogatyrClient.accept(view(23,9));assertTrue(BogatyrClient.pending());
        assertTrue(row(BogatyrClient.HEAL).tooltip().getString().contains("Waiting for the server"));
        BogatyrClient.accept(view(23,11));assertFalse(BogatyrClient.pending());
        assertTrue(BogatyrClient.activate(BogatyrClient.HEAL));assertEquals(2,sent.size());
        assertEquals(new BogatyrPayloads.Action(23,11,Kind.HEAL),sent.getLast());
    }
    @Test void wolfpackHasTenEqualActionRowsWhileKeepingResourceAndSharedActions(){
        var snapshot=new ClassResourceSnapshot(23,"kibble",105,600,true,true,true,3);
        var generic=ClassResourcePresentation.panel("bogatyr",snapshot,false,true);
        BogatyrClient.accept(view(23,5));var model=BogatyrClient.augment(generic);
        assertEquals("Wolfpack",model.title());assertEquals(generic.resource(),model.resource());
        assertEquals(generic.resourceTooltip(),model.resourceTooltip());assertSame(snapshot,model.resourceSnapshot());
        assertEquals(12,model.actions().size());assertEquals(generic.actions().get(0),model.actions().get(0));
        assertEquals(generic.actions().get(1),model.actions().get(1));
        assertFalse(model.actions().stream().anyMatch(a->a.id().equals("guide")));
        assertEquals(List.of("Defensive","Stand Ground","Aggressive","Strategic","Search and Rescue","Danger Close"),
                model.actions().subList(6,12).stream().map(SkillsPanelModel.Action::label).toList());
        for(var mode:model.actions().subList(6,12)){
            assertFalse(mode.enabled());assertFalse(BogatyrClient.activate(mode.id()));
        }
        var panel=new SkillsPanelState(SharedInventoryLayout.of(320,240,72,37,176,166),null);
        panel.actions(model.actions().size());assertTrue(panel.scroll().max()>0);
        assertTrue(panel.scroll().thumbHeight()>0);assertTrue(sent.isEmpty());
    }
    @Test void runChangesClearPendingAndLogoutClearsAllCapabilities(){
        BogatyrClient.accept(view(23,100));assertTrue(BogatyrClient.activate(BogatyrClient.BREED));
        BogatyrClient.accept(view(24,101));assertFalse(BogatyrClient.pending());
        assertTrue(BogatyrClient.activate(BogatyrClient.HEAL));assertEquals(24,sent.getLast().run());
        assertEquals(101,sent.getLast().revision());
        BogatyrClient.accept(new View(0,102,0,List.of(new Quote(0,0,false),new Quote(0,0,false),
                new Quote(0,0,false),new Quote(0,0,false))));
        assertFalse(BogatyrClient.pending());assertFalse(BogatyrClient.activate(BogatyrClient.HEAL));
        BogatyrClient.accept(view(24,103));BogatyrClient.clear();
        assertFalse(row(BogatyrClient.BREED).enabled());assertFalse(BogatyrClient.pending());
        var other=SkillsPanelModel.initial("theurgist");assertSame(other,BogatyrClient.augment(other));
    }
    @Test void sameRunOldQuotesCannotReenableUnavailableCommands(){
        BogatyrClient.accept(new View(23,50,0,List.of(new Quote(0,0,false),Quote.empty(),
                new Quote(0,0,false),new Quote(0,0,false))));
        BogatyrClient.accept(view(23,49));
        assertFalse(row(BogatyrClient.BREED).enabled());assertFalse(BogatyrClient.activate(BogatyrClient.BREED));
        assertEquals("Breed 0 [0]",row(BogatyrClient.BREED).label());assertTrue(sent.isEmpty());
    }
    @Test void senderFailureReleasesPendingAndUnknownActionsNeverSend(){
        BogatyrClient.accept(view(23,1));assertFalse(BogatyrClient.activate("guide"));
        assertFalse(BogatyrClient.activate("request_supplies"));assertFalse(BogatyrClient.activate("bogatyr_mode_0"));
        BogatyrClient.actions(action->{throw new IllegalStateException("Disconnected");});
        assertThrows(IllegalStateException.class,()->BogatyrClient.activate(BogatyrClient.BREED));
        assertFalse(BogatyrClient.pending());assertTrue(sent.isEmpty());
    }
}
