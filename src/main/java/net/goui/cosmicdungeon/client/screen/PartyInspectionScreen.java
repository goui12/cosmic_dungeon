package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** One open, read-only member view; bounded refresh stops immediately when it closes. */
public final class PartyInspectionScreen extends Screen {
    private static long nextSequence;
    private final Screen parent;
    private final long run;
    private final UUID subject;
    private PartyInspectionPayloads.View view;
    private long requested, accepted;
    private int ticks, age, scroll;
    private boolean dragging;
    private double grab;
    PartyInspectionScreen(Screen parent,long run,PartyPayloads.Member member) {
        super(Component.literal(member.name()+" / "+ClassSelectorScreen.className(member.classId()).getString()));
        this.parent=parent;this.run=run;subject=UUID.fromString(member.memberId());
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose())
                .bounds(width/2-50,height-26,100,20).build());
        scroll=0;dragging=false;
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void tick() {
        if(minecraft.player==null||!D1PartyHud.inspectionCurrent(run,subject.toString())
                ||minecraft.player.containerMenu!=minecraft.player.inventoryMenu) {
            minecraft.setScreen(null);return;
        }
        if(++age>40)view=null;
        if(ticks++%20==0) {
            requested=++nextSequence;
            ModNetwork.sendToServer(new PartyInspectionPayloads.Request(run,requested,subject));
        }
    }
    public static void receive(PartyInspectionPayloads.View payload) {
        if(!(Minecraft.getInstance().screen instanceof PartyInspectionScreen screen))return;
        var request=payload.request();
        if(request.run()!=screen.run||!request.subject().equals(screen.subject)
                ||request.sequence()>screen.requested||request.sequence()<=screen.accepted
                ||!D1PartyHud.inspectionCurrent(screen.run,screen.subject.toString()))return;
        screen.accepted=request.sequence();screen.view=payload;screen.age=0;
    }
    private PartyHealthLayout box() {
        int w=Math.min(360,width-16);
        return new PartyHealthLayout((width-w)/2,32,w,Math.max(48,height-68),1,0);
    }
    private List<FormattedCharSequence> lines(PartyHealthLayout box) {
        var source=new ArrayList<Component>();
        if(view==null)source.add(Component.literal("Refreshing current-run details..."));
        else if(!view.allowed())source.add(Component.literal("This member is no longer available in your active run."));
        else {
            source.add(Component.literal(PartyHealthHud.status(view.vitals())));
            for(var reading:view.readings())source.add(Component.literal(reading.label()+": "+reading.value()));
            source.add(Component.literal("Effects"));
            if(view.vitals().effects().isEmpty())source.add(Component.literal(
                    view.vitals().state().equals("ACTIVE")?"None":"Unavailable"));
            for(var effect:view.vitals().effects()) {
                var holder=BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effect.id()));
                source.add(holder.<Component>map(h->h.value().getDisplayName().copy().append(" "+(effect.amplifier()+1)))
                        .orElse(Component.literal(effect.id()+" "+(effect.amplifier()+1))));
            }
            if(view.vitals().omittedEffects()>0)source.add(Component.literal("+"+view.vitals().omittedEffects()+" additional effects"));
        }
        var result=new ArrayList<FormattedCharSequence>();
        for(var line:source)result.addAll(font.split(line,box.width()-20));
        return result;
    }
    private int content(PartyHealthLayout box){return lines(box).size()*12;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick) {
        renderBackground(g,mx,my,partialTick);
        var box=box();var lines=lines(box);int total=lines.size()*12;
        scroll=box.clampScroll(scroll,total);
        Component heading=view!=null&&view.allowed()?Component.literal(view.name()+" / "+ClassSelectorScreen.className(view.classId()).getString()):title;
        g.drawCenteredString(font,heading,width/2,14,0xFFE4C98A);
        g.fill(box.x(),box.y(),box.x()+box.width(),box.y()+box.height(),0xE0181820);
        g.drawString(font,"Current run",box.x()+6,box.y()+6,0xFF90CAF9,false);
        g.enableScissor(box.x()+4,box.y()+PartyHealthLayout.HEADER,box.x()+box.width()-8,box.y()+box.height());
        int y=box.y()+PartyHealthLayout.HEADER-scroll;
        for(var line:lines){g.drawString(font,line,box.x()+6,y,0xFFEEEEEE,false);y+=12;}
        g.disableScissor();
        if(box.maxScroll(total)>0) {
            int x=box.x()+box.width()-6,top=box.thumbY(scroll,total);
            g.fill(x,box.y()+PartyHealthLayout.HEADER,x+4,box.y()+box.height(),0xFF363640);
            g.fill(x,top,x+4,top+box.thumbHeight(total),dragging?0xFFE4C98A:0xFF9B9BAA);
        }
        super.render(g,mx,my,partialTick);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        var box=box();
        if(box.contains(x,y)&&vertical!=0){scroll=box.clampScroll(scroll-(int)Math.copySign(24,vertical),content(box));return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        var box=box();int total=content(box);
        if(event.button()==0&&box.contains(event.x(),event.y())&&event.x()>=box.x()+box.width()-8
                &&event.y()>=box.y()+PartyHealthLayout.HEADER&&box.maxScroll(total)>0) {
            int top=box.thumbY(scroll,total),height=box.thumbHeight(total);
            grab=event.y()>=top&&event.y()<top+height?event.y()-top:height/2.0;
            dragging=true;scroll=box.scrollAt(event.y()-grab,total);return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if(dragging&&event.button()==0){var box=box();scroll=box.scrollAt(event.y()-grab,content(box));return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(event.button()==0&&dragging){dragging=false;return true;}
        return super.mouseReleased(event);
    }
}
