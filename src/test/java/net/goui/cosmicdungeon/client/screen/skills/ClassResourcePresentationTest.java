package net.goui.cosmicdungeon.client.screen.skills;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassResourcePresentationTest {
    private record Span(String text,Style style){}
    private List<Span> spans(Component component){
        var result=new ArrayList<Span>();
        component.visit((style,text)->{
            if(!text.isEmpty())result.add(new Span(text,style));
            return Optional.<Void>empty();
        },Style.EMPTY);
        return result;
    }
    private void blueBold(Style style){
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE),style.getColor());assertTrue(style.isBold());
    }
    private ClassResourceSnapshot snapshot(String resource,int amount,boolean alive){
        return new ClassResourceSnapshot(23,resource,amount,600,true,alive,true,9);
    }
    @Test void exactTooltipTextRetainsItsRequiredStylesForBothResources(){
        for(String resource:List.of("brewing_supplies","kibble")){
            var value=snapshot(resource,147,true);String name=value.tooltipName();
            var hover=ClassResourcePresentation.resourceTooltip(value);
            assertEquals(name+" <147 / 600>",hover.getString());blueBold(hover.getStyle());
            var request=spans(ClassResourcePresentation.requestTooltip(name));
            assertEquals(3,request.size());
            assertEquals("Click to request supplies from your group. If they accept, their supplies will be automatically recycled into your ",request.get(0).text());
            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW),request.get(0).style().getColor());
            assertFalse(request.get(0).style().isBold());assertEquals(name,request.get(1).text());blueBold(request.get(1).style());
            assertEquals("\nNo supply requests available.",request.get(2).text());assertFalse(request.get(2).style().isBold());
            var recycle=spans(ClassResourcePresentation.recycleTooltip(name));
            assertEquals(2,recycle.size());assertEquals("Click to convert all items into ",recycle.get(0).text());
            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW),recycle.get(0).style().getColor());
            assertEquals(name,recycle.get(1).text());blueBold(recycle.get(1).style());
        }
    }
    @Test void onlyMatchingSnapshotsSupplyCountsAndRequestNeverPretendsToSend(){
        for(String id:List.of("theurgist","bogatyr")){
            var unknown=ClassResourcePresentation.panel(id,null,false);
            assertEquals("Resource: —",unknown.resource());assertNull(unknown.resourceSnapshot());
            assertFalse(unknown.actions().get(0).enabled());assertFalse(unknown.actions().get(1).enabled());
            var value=snapshot(id.equals("theurgist")?"brewing_supplies":"kibble",147,true);
            var panel=ClassResourcePresentation.panel(id,value,false);
            assertSame(value,panel.resourceSnapshot());assertEquals(value.title()+": 147/600",panel.resource());
            assertEquals(List.of("request_supplies","recycle","guide"),panel.actions().stream().map(SkillsPanelModel.Action::id).toList());
            assertFalse(panel.actions().get(0).enabled());assertTrue(panel.actions().get(1).enabled());assertTrue(panel.actions().get(2).enabled());
            blueBold(panel.resourceTooltip().getStyle());blueBold(spans(panel.actions().get(1).tooltip()).get(1).style());
            assertFalse(ClassResourcePresentation.panel(id,value,true).actions().get(1).enabled());
            var wrong=ClassResourcePresentation.panel(id.equals("theurgist")?"bogatyr":"theurgist",value,false);
            assertNull(wrong.resourceSnapshot());assertEquals("Resource: —",wrong.resource());
        }
        for(String id:List.of("none","judicator","dragoon","pyroclast","venefex")){
            var panel=ClassResourcePresentation.panel(id,snapshot("brewing_supplies",147,true),false);
            assertTrue(panel.actions().isEmpty());assertNull(panel.resourceSnapshot());
        }
    }
    @Test void deathAndMinimizationKeepTheAuthoritativeResourceWithoutChangingPlacement(){
        var model=ClassResourcePresentation.panel("theurgist",snapshot("brewing_supplies",148,false),false);
        assertEquals("Brewing Supplies: 148/600",model.resource());assertFalse(model.actions().get(1).enabled());
        var layout=SharedInventoryLayout.of(640,480,232,157,176,166);
        var state=new SkillsPanelState(layout,null);state.actions(model.actions().size());
        var before=state.geometry().resource();var toggle=state.geometry().minimize();
        assertTrue(state.press(toggle.x()+2,toggle.y()+2,0,false).changed());state.release();
        assertEquals(before,state.geometry().resource());assertEquals(0,state.geometry().body().height());
        var refreshed=ClassResourcePresentation.panel("theurgist",snapshot("brewing_supplies",149,false),false);
        assertEquals("Brewing Supplies: 149/600",refreshed.resource());assertEquals(before,state.geometry().resource());
        assertThrows(IllegalArgumentException.class,()->new SkillsPanelModel("bogatyr","Skills","149",Component.literal("x"),List.of(),refreshed.resourceSnapshot()));
    }
}
