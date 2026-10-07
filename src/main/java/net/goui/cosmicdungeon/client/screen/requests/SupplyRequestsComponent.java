package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestAction.Decision;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native boxed cards: every quoted stack remains visible through the shared scrollbar, including custom components. */
public final class SupplyRequestsComponent {
    public static final String ACCEPT_ALL_TOOLTIP="Accept these requests in order, using only the supplies and capacity still available.";
    private SupplyRequestsComponent(){}
    private static void fill(GuiGraphics g,Rect r,int color){g.fill(r.x(),r.y(),r.right(),r.bottom(),color);}
    private static void text(GuiGraphics g,Font font,String value,Rect box,int color,boolean scale){
        int available=Math.max(0,box.width()-4);if(available==0||box.height()<1)return;
        String label=scale?value:font.plainSubstrByWidth(value,available);int measured=font.width(label);
        float factor=scale&&measured>available?available/(float)measured:1;
        var pose=g.pose();pose.pushMatrix();
        try{pose.translate(box.x()+box.width()/2f,box.y()+(box.height()-font.lineHeight*factor)/2f);pose.scale(factor,factor);
            g.drawString(font,label,-measured/2,0,color,false);
        }finally{pose.popMatrix();}
    }
    private static void button(GuiGraphics g,Font font,Rect box,String label,boolean enabled,int mouseX,int mouseY){
        fill(g,box,!enabled?0xFF29333C:box.contains(mouseX,mouseY)?0xFF416A83:0xFF315068);
        text(g,font,label,box,enabled?0xFFFFFFFF:0xFF929AA4,true);
    }
    public static List<Component> details(SupplyRequestsSnapshot.Card card){
        return List.of(Component.literal(card.requesterName()).withStyle(ChatFormatting.WHITE,ChatFormatting.BOLD),
                Component.literal(card.className()).withStyle(ChatFormatting.GRAY),
                Component.literal("Credits "+card.yield()+" "+card.resourceName()).withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD));
    }
    public static void draw(GuiGraphics g,Font font,SupplyRequestsState state,int mouseX,int mouseY){
        var box=state.geometry();if(!state.visible())return;
        g.nextStratum();fill(g,box.panel(),0xEF17212D);fill(g,box.header(),0xFF244861);
        text(g,font,"Requests",box.header(),0xFFFFFFFF,true);
        if(box.body().height()>0){
            g.enableScissor(box.body().x(),box.body().y(),box.body().right(),box.body().bottom());
            try{
                if(state.snapshot().cards().isEmpty())text(g,font,"No requests",new Rect(box.body().x()+2,box.body().y()+6,
                        Math.max(0,box.body().width()-10),18),0xFFADB8C5,true);
                for(int i=0;i<state.snapshot().cards().size();i++){
                    if(!state.cardBounds(i).intersects(box.body()))continue;
                    var card=state.snapshot().cards().get(i);var row=state.card(i);
                    fill(g,row.panel(),0xFF263749);fill(g,new Rect(row.panel().x(),row.panel().y(),row.panel().width(),1),0xFF6390AE);
                    text(g,font,card.requesterName(),new Rect(row.heading().x(),row.heading().y(),row.heading().width(),11),0xFFFFFFFF,false);
                    text(g,font,card.className(),new Rect(row.heading().x(),row.heading().y()+11,row.heading().width(),11),0xFFAFCCDF,false);
                    if(card.ingredients().isEmpty())text(g,font,"No supplies",new Rect(row.panel().x()+4,row.panel().y()+28,
                            Math.max(0,row.panel().width()-8),18),0xFFAFB8C1,true);
                    for(int j=0;j<card.ingredients().size();j++){
                        var icon=row.ingredients().get(j);if(!icon.intersects(box.body()))continue;
                        var stack=card.ingredients().get(j).stack();
                        g.renderItem(stack,icon.x(),icon.y());g.renderItemDecorations(font,stack,icon.x(),icon.y());
                    }
                    text(g,font,"+"+card.yield(),row.yield(),0xFF8EBBFF,true);
                    var id=card.requestId();
                    button(g,font,row.accept(),"Accept",state.enabled(Decision.ACCEPT,id),mouseX,mouseY);
                    button(g,font,row.deny(),"Deny",state.enabled(Decision.DENY,id),mouseX,mouseY);
                }
            }finally{g.disableScissor();}
            if(state.scroll().max()>0){
                fill(g,box.track(),0xFF111820);
                fill(g,new Rect(box.track().x(),state.scroll().thumbTop(box.track().y()),box.track().width(),state.scroll().thumbHeight()),
                        state.scroll().captured()?0xFFA8D8FF:0xFF65A0C8);
            }
        }
        button(g,font,box.acceptAll(),"Accept All",state.enabled(Decision.ACCEPT_ALL,null),mouseX,mouseY);
        button(g,font,box.denyAll(),"Deny All",state.enabled(Decision.DENY_ALL,null),mouseX,mouseY);
    }
    /** Called before vanilla schedules a covered slot tooltip; rich details use only the authoritative quote. */
    public static void tooltip(GuiGraphics g,Font font,SupplyRequestsState state,int x,int y){
        var box=state.geometry();if(!state.visible()||!box.panel().contains(x,y))return;
        List<Component> lines;
        if(box.acceptAll().contains(x,y))lines=List.of(Component.literal(ACCEPT_ALL_TOOLTIP).withStyle(ChatFormatting.YELLOW));
        else if(box.denyAll().contains(x,y))lines=List.of(Component.literal("Deny all displayed requests without giving any supplies.").withStyle(ChatFormatting.YELLOW));
        else if(box.body().contains(x,y)&&!box.track().contains(x,y)){
            for(int i=0;i<state.snapshot().cards().size();i++){
                if(!state.cardBounds(i).contains(x,y))continue;
                var card=state.snapshot().cards().get(i);var row=state.card(i);var detail=new ArrayList<>(details(card));
                for(int j=0;j<row.ingredients().size();j++)if(row.ingredients().get(j).contains(x,y)){
                    var stack=card.ingredients().get(j).stack();
                    detail.add(Component.literal("Count: "+stack.getCount()).withStyle(ChatFormatting.YELLOW));
                    detail.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(),stack));
                    g.setComponentTooltipForNextFrame(font,detail,x,y,stack);return;
                }
                if(row.accept().contains(x,y))detail.add(Component.literal("Give the shown supplies.").withStyle(ChatFormatting.YELLOW));
                else if(row.deny().contains(x,y))detail.add(Component.literal("Deny without giving supplies.").withStyle(ChatFormatting.YELLOW));
                if(state.pending())detail.add(Component.literal("Waiting for the server.").withStyle(ChatFormatting.GRAY));
                else if(!card.canAccept())detail.add(Component.literal("No eligible transfer available.").withStyle(ChatFormatting.GRAY));
                g.setComponentTooltipForNextFrame(font,detail,x,y);return;
            }
            lines=List.of(Component.literal(state.pending()?"Waiting for the server.":"No requests."));
        }else lines=List.of(Component.literal("Supply requests from your group."));
        g.setComponentTooltipForNextFrame(font,lines,x,y);
    }
}
