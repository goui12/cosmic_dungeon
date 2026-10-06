package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

/** Active-run cards. World cards are read-only; inventory cards reveal every synchronized effect by scrolling. */
final class PartyHealthHud {
    private record Row(String label, PartyVitals vitals, List<String> details) {}
    private static int scroll;
    private static boolean dragging;
    private static double grab;
    private PartyHealthHud() {}
    static void reset() { scroll=0;dragging=false; }
    static PartyHealthLayout worldLayout(PartyPayloads.View view) {
        var window=Minecraft.getInstance().getWindow();
        return PartyHealthLayout.world(window.getGuiScaledWidth(),window.getGuiScaledHeight(),view.members().size()+view.mercenaries().size());
    }
    static PartyHealthLayout inventoryLayout(int left,int height) { return PartyHealthLayout.inventory(left,height); }
    private static List<Row> rows(PartyPayloads.View view) {
        var result=new ArrayList<Row>();
        for(var member:view.members()) {
            String label=(member.leader()?"* ":"")+member.name()+" / "+ClassSelectorScreen.className(member.classId()).getString();
            result.add(new Row(label,member.vitals(),List.of(label,status(member.vitals()))));
        }
        for(var hire:view.mercenaries()) result.add(new Row(MercenaryHudLayout.label(hire),hire.vitals(),MercenaryHudLayout.tooltip(hire)));
        return result;
    }
    static String status(PartyVitals v) {
        return switch(v.state()) {
            case "ACTIVE" -> String.format(Locale.ROOT,"%.1f/%.1f HP",v.health(),v.maxHealth());
            case "DEAD" -> "Dead";
            case "OFFLINE" -> "Offline";
            default -> "Unloaded / outside dungeon";
        };
    }
    private static int content(List<Row> rows,PartyHealthLayout box) {
        return rows.stream().mapToInt(r->box.rowHeight(r.vitals().effects().size())+(r.vitals().omittedEffects()>0?10:0)).sum();
    }
    static void draw(GuiGraphics g,PartyPayloads.View view,PartyHealthLayout box,boolean inventory,int mx,int my) {
        var font=Minecraft.getInstance().font;var rows=rows(view);
        int total=content(rows,box);scroll=box.clampScroll(scroll,total);
        for(int column=0;column<box.columns();column++) {
            int x=box.x()+column*(box.width()+4);
            g.fill(x,box.y(),x+box.width(),box.y()+box.height(),0xD0181820);
            g.drawString(font,font.plainSubstrByWidth(view.recruitment().groupName(),box.width()-8),x+4,box.y()+3,0xFFE4C98A,false);
            g.drawString(font,font.plainSubstrByWidth(view.difficulty()+" / Party",box.width()-8),x+4,box.y()+13,0xFF90CAF9,false);
        }
        if(inventory)g.enableScissor(box.x(),box.y()+PartyHealthLayout.HEADER,box.x()+box.width()-6,box.y()+box.height());
        int y=box.y()+PartyHealthLayout.HEADER-(inventory?scroll:0);
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);int x=box.x();
            int height=inventory?box.rowHeight(row.vitals().effects().size())+(row.vitals().omittedEffects()>0?10:0):PartyHealthLayout.ROW;
            if(!inventory) {
                x+=(i/box.rowsPerColumn())*(box.width()+4);
                y=box.y()+PartyHealthLayout.HEADER+(i%box.rowsPerColumn())*PartyHealthLayout.ROW;
            }
            if(!inventory||y+height>box.y()+PartyHealthLayout.HEADER&&y<box.y()+box.height())
                card(g,row,x,y,box.width(),height,inventory,
                        inventory&&my>=box.y()+PartyHealthLayout.HEADER&&my<box.y()+box.height()?mx:-1,my);
            if(inventory)y+=height;
        }
        if(inventory) {
            g.disableScissor();
            if(box.maxScroll(total)>0) {
                int x=box.x()+box.width()-5,ty=box.thumbY(scroll,total);
                g.fill(x,box.y()+PartyHealthLayout.HEADER,x+4,box.y()+box.height(),0xFF363640);
                g.fill(x,ty,x+4,ty+box.thumbHeight(total),dragging?0xFFE4C98A:0xFF9B9BAA);
            }
        }
    }
    private static void card(GuiGraphics g,Row row,int x,int y,int width,int height,boolean inventory,int mx,int my) {
        var font=Minecraft.getInstance().font;var v=row.vitals();int textWidth=width-(inventory?14:8);
        g.drawString(font,font.plainSubstrByWidth(row.label(),textWidth),x+4,y+1,0xFFEEEEEE,false);
        g.drawString(font,font.plainSubstrByWidth(status(v),textWidth),x+4,y+11,v.state().equals("ACTIVE")?0xFFAAFFAA:0xFFBBBBBB,false);
        g.fill(x+4,y+22,x+4+textWidth,y+24,0xFF553333);
        if(v.state().equals("ACTIVE")&&v.maxHealth()>0)
            g.fill(x+4,y+22,x+4+Math.round(textWidth*v.health()/v.maxHealth()),y+24,0xFF66CC88);
        int columns=Math.max(1,(width-16)/PartyHealthLayout.ICON);
        int visible=inventory?v.effects().size():Math.min(v.effects().size(),columns);
        boolean overflow=!inventory&&(v.effects().size()>columns||v.omittedEffects()>0);
        if(overflow)visible=Math.max(0,columns-1);
        boolean effectHovered=false;
        for(int i=0;i<visible;i++) {
            var effect=v.effects().get(i);int ix=x+4+(i%columns)*10,iy=y+27+(i/columns)*10;
            var holder=BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(effect.id()));
            if(holder.isPresent())g.blitSprite(RenderPipelines.GUI_TEXTURED,Gui.getMobEffectSprite(holder.get()),ix,iy,8,8);
            else g.drawString(font,"?",ix,iy,0xFFFFFFFF,false);
            if(mx>=ix&&mx<ix+10&&my>=iy&&my<iy+10) {
                var name=holder.<Component>map(h->h.value().getDisplayName().copy().append(" "+(effect.amplifier()+1)))
                        .orElse(Component.literal(effect.id()+" "+(effect.amplifier()+1)));
                g.setComponentTooltipForNextFrame(font,List.of(name),mx,my);effectHovered=true;
            }
        }
        if(overflow)g.drawString(font,"+",x+4+visible*10,y+27,0xFFE4C98A,false);
        if(inventory&&v.omittedEffects()>0)g.drawString(font,"+"+v.omittedEffects(),x+4,y+height-10,0xFFE4C98A,false);
        if(!effectHovered&&mx>=x&&mx<x+width-6&&my>=y&&my<y+height) {
            var details=new ArrayList<Component>();row.details().forEach(s->details.add(Component.literal(s)));
            if(v.omittedEffects()>0)details.add(Component.literal(v.omittedEffects()+" additional effects exceed the network display limit"));
            g.setComponentTooltipForNextFrame(font,details,mx,my);
        }
    }
    static boolean wheel(PartyPayloads.View view,PartyHealthLayout box,double mx,double my,double delta) {
        if(!box.contains(mx,my)||my<box.y()+PartyHealthLayout.HEADER||delta==0)return false;
        scroll=box.clampScroll(scroll-(int)Math.copySign(24,delta),content(rows(view),box));return true;
    }
    static boolean press(PartyPayloads.View view,PartyHealthLayout box,double mx,double my) {
        int total=content(rows(view),box);
        if(!box.contains(mx,my)||mx<box.x()+box.width()-6||my<box.y()+PartyHealthLayout.HEADER||box.maxScroll(total)==0)return false;
        int top=box.thumbY(scroll,total),height=box.thumbHeight(total);
        grab=my>=top&&my<top+height?my-top:height/2.0;
        dragging=true;scroll=box.scrollAt(my-grab,total);return true;
    }
    static boolean drag(PartyPayloads.View view,PartyHealthLayout box,double my) {
        if(!dragging)return false;scroll=box.scrollAt(my-grab,content(rows(view),box));return true;
    }
    static boolean release() { boolean was=dragging;dragging=false;return was; }
}
