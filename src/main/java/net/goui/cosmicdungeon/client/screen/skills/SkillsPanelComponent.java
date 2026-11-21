package net.goui.cosmicdungeon.client.screen.skills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Reusable renderer; the pure state owns identical bounds for drawing and pointer handling. */
public final class SkillsPanelComponent {
    private SkillsPanelComponent(){}
    private static void fill(GuiGraphics g,Rect r,int color){g.fill(r.x(),r.y(),r.right(),r.bottom(),color);}
    private static void border(GuiGraphics g,Rect r,int color){
        if(r.width()<=0||r.height()<=0)return;
        g.fill(r.x(),r.y(),r.right(),r.y()+1,color);
        g.fill(r.x(),r.bottom()-1,r.right(),r.bottom(),color);
        g.fill(r.x(),r.y(),r.x()+1,r.bottom(),color);
        g.fill(r.right()-1,r.y(),r.right(),r.bottom(),color);
    }
    private static void text(GuiGraphics g,Font font,String text,Rect box,int color){
        int width = (int)(Math.max(0, box.width() - 6) / SkillBindingLabels.TEXT_SCALE);
        int offset = Math.max(0, (box.height() - (int)Math.ceil(font.lineHeight * SkillBindingLabels.TEXT_SCALE)) / 2);
        SkillBindingLabels.drawText(g, font, font.plainSubstrByWidth(text,width),box.x()+3,box.y()+offset,color);
    }
    private static void tooltip(GuiGraphics g,Font font,Component text,int x,int y){
        if (x < 0 || y < 0) return;
        int wrap = Math.max(1, Math.min(250, g.guiWidth() - 24));
        g.setTooltipForNextFrame(font, font.split(text, wrap), x, y);
    }
    /** Use the same row and control bounds as pointer input, including while scrolled. */
    private static void tooltip(GuiGraphics g,Font font,SkillsPanelModel model,SkillsPanelState state,int mouseX,int mouseY){
        var box=state.geometry();Component tip=null;
        if(box.help().contains(mouseX,mouseY))tip=Component.literal("Open the class guide");
        else if(box.minimize().contains(mouseX,mouseY))tip=Component.literal(state.minimized()?"Expand Skills":"Minimize Skills");
        else if(box.header().contains(mouseX,mouseY))tip=Component.literal("Drag the title bar to move Skills to either side of the inventory.");
        else if(box.resource().contains(mouseX,mouseY))tip=model.resourceTooltip();
        else if(box.body().contains(mouseX,mouseY)){
            if(box.track().contains(mouseX,mouseY)&&state.scroll().max()>0)tip=Component.literal("Scroll Skills");
            else for(int i=0;i<model.actions().size();i++)if(box.row(i,state.scroll().offset()).contains(mouseX,mouseY)){
                var action=model.actions().get(i);
                tip=SkillBindingLabels.tooltip(action.id(),action.label(),action.tooltip());
                if (!action.enabled() || action.selected()) {
                    var status = action.selected() ? "Selected" : "Unavailable";
                    var color = action.selected() ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY;
                    tip = tip.copy().append(Component.literal("\n" + status).withStyle(color));
                }
                break;
            }
        }
        if(tip!=null)tooltip(g,font,tip,mouseX,mouseY);
    }
    public static void draw(GuiGraphics g,Font font,SkillsPanelModel model,SkillsPanelState state,int mouseX,int mouseY){
        g.nextStratum();
        var box=state.geometry();
        fill(g,box.panel(),0xF0111D2A);
        fill(g,box.header(),box.header().contains(mouseX,mouseY) ? 0xFF305278 : 0xFF233D58);
        var title=new Rect(box.header().x(),box.header().y(),Math.max(0,box.header().width()-38),box.header().height());
        text(g,font,model.title(),title,0xFFFFFFFF);
        fill(g,box.help(),box.help().contains(mouseX,mouseY) ? 0xFF437FB8 : 0xFF23588D);
        border(g,box.help(),0xFF89BBE3);
        g.drawCenteredString(font,"i",box.help().x()+box.help().width()/2,box.help().y()+2,0xFFFFFFFF);
        fill(g,box.minimize(),box.minimize().contains(mouseX,mouseY) ? 0xFF506F89 : 0xFF30495E);
        border(g,box.minimize(),0xFF93AFBF);
        g.drawCenteredString(font,state.minimized()?"+":"−",box.minimize().x()+box.minimize().width()/2,box.minimize().y()+2,0xFFFFFFFF);
        if(model.resourceSnapshot()!=null)ClassResourceBar.draw(g,font,box.resource(),model.resourceSnapshot());
        else {fill(g,box.resource(),0xFF1E303F);text(g,font,model.resource(),box.resource(),0xFFB9DBED);}
        if(box.body().height()>0){
            g.enableScissor(box.body().x(),box.body().y(),box.body().right(),box.body().bottom());
            try{
                if(model.actions().isEmpty())text(g,font,"No panel actions",box.body(),0xFFA6B0BC);
                for(int i=0;i<model.actions().size();i++){
                    var row=box.row(i,state.scroll().offset());
                    if(!row.intersects(box.body()))continue;
                    var action=model.actions().get(i);boolean hovered=box.body().contains(mouseX,mouseY)&&row.contains(mouseX,mouseY);
                    int surface = action.enabled() ? (hovered ? 0xFF176F76 : 0xFF174952) : 0xFF27323E;
                    int edge = action.selected() ? 0xFFFFCC72 : hovered ? 0xFFC3F8F2
                            : action.enabled() ? 0xFF52949B : 0xFF485765;
                    fill(g,row,surface);
                    border(g,row,edge);
                    SkillBindingLabels.draw(g,font,action.id(),action.label(),row,
                            action.enabled()?0xFFFFFFFF:0xFFB7C4CF);
                }
            }finally{g.disableScissor();}
            if(state.scroll().max()>0){
                fill(g,box.track(),0xFF111820);
                fill(g,new Rect(box.track().x(),state.scroll().thumbTop(box.track().y()),box.track().width(),
                        state.scroll().thumbHeight()),state.scroll().captured()?0xFFA8D8FF:0xFF65A0C8);

            }
        }
        border(g,box.panel(),0xFF62829A);
        tooltip(g,font,model,state,mouseX,mouseY);
    }
}
