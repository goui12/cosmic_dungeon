package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Reusable renderer; the pure state owns identical bounds for drawing and pointer handling. */
public final class SkillsPanelComponent {
    private SkillsPanelComponent(){}
    private static void fill(GuiGraphics g,Rect r,int color){g.fill(r.x(),r.y(),r.right(),r.bottom(),color);}
    private static void text(GuiGraphics g,Font font,String text,Rect box,int color){
        g.drawString(font,font.plainSubstrByWidth(text,Math.max(0,box.width()-8)),box.x()+4,box.y()+5,color,false);
    }
    private static void tooltip(GuiGraphics g,Font font,String text,int x,int y){
        if(x>=0&&y>=0)g.setComponentTooltipForNextFrame(font,List.of(Component.literal(text)),x,y);
    }
    /** Reserve the topmost tooltip before vanilla schedules a covered inventory-slot tooltip. */
    public static void tooltip(GuiGraphics g,Font font,SkillsPanelModel model,SkillsPanelState state,int mouseX,int mouseY){
        var box=state.geometry();String tip=null;
        if(box.help().contains(mouseX,mouseY))tip="Open the class guide";
        else if(box.minimize().contains(mouseX,mouseY))tip=state.minimized()?"Expand Skills":"Minimize Skills";
        else if(box.header().contains(mouseX,mouseY))tip=model.title()+" — drag to move";
        else if(box.resource().contains(mouseX,mouseY))tip=model.resource()+"\n"+model.resourceTooltip();
        else if(box.body().contains(mouseX,mouseY)){
            tip="Skills";
            if(box.track().contains(mouseX,mouseY)&&state.scroll().max()>0)tip="Scroll Skills";
            else for(int i=0;i<model.actions().size();i++)if(box.row(i,state.scroll().offset()).contains(mouseX,mouseY)){
                var action=model.actions().get(i);tip=action.label()+"\n"+action.tooltip();break;
            }
        }
        if(tip!=null)tooltip(g,font,tip,mouseX,mouseY);
    }
    public static void draw(GuiGraphics g,Font font,SkillsPanelModel model,SkillsPanelState state,int mouseX,int mouseY){
        g.nextStratum();
        var box=state.geometry();fill(g,box.panel(),0xEE17212D);fill(g,box.header(),0xFF244861);
        var title=new Rect(box.header().x(),box.header().y(),Math.max(0,box.header().width()-38),box.header().height());
        text(g,font,model.title(),title,0xFFFFFFFF);
        fill(g,box.help(),0xFF267AC0);
        g.drawCenteredString(font,"i",box.help().x()+box.help().width()/2,box.help().y()+2,0xFFFFFFFF);
        fill(g,box.minimize(),0xFF344B5D);
        g.drawCenteredString(font,state.minimized()?"+":"−",box.minimize().x()+box.minimize().width()/2,box.minimize().y()+2,0xFFFFFFFF);
        fill(g,box.resource(),0xFF1E303F);text(g,font,model.resource(),box.resource(),0xFFB9DBED);
        if(box.body().height()>0){
            g.enableScissor(box.body().x(),box.body().y(),box.body().right(),box.body().bottom());
            try{
                if(model.actions().isEmpty())text(g,font,"No panel actions",box.body(),0xFFA6B0BC);
                for(int i=0;i<model.actions().size();i++){
                    var row=box.row(i,state.scroll().offset());
                    if(!row.intersects(box.body()))continue;
                    var action=model.actions().get(i);boolean hovered=box.body().contains(mouseX,mouseY)&&row.contains(mouseX,mouseY);
                    fill(g,row,!action.enabled()?0xFF26303A:hovered?0xFF42617B:0xFF31495E);
                    String label=font.plainSubstrByWidth(action.label(),Math.max(0,row.width()-8));
                    g.drawCenteredString(font,label,row.x()+row.width()/2,row.y()+6,action.enabled()?0xFFFFFFFF:0xFF89939E);

                }
            }finally{g.disableScissor();}
            if(state.scroll().max()>0){
                fill(g,box.track(),0xFF111820);
                fill(g,new Rect(box.track().x(),state.scroll().thumbTop(box.track().y()),box.track().width(),
                        state.scroll().thumbHeight()),state.scroll().captured()?0xFFA8D8FF:0xFF65A0C8);

            }
        }
        tooltip(g,font,model,state,mouseX,mouseY);
    }
}
