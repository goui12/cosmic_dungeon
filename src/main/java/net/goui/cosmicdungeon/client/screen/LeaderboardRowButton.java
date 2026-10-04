package net.goui.cosmicdungeon.client.screen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Separate score column; full name and value remain available to narration and tooltip. */
final class LeaderboardRowButton extends Button{
    private String name="",score="";private boolean selected;
    LeaderboardRowButton(int x,int y,int width,OnPress press){super(x,y,width,17,Component.empty(),press,DEFAULT_NARRATION);}
    void row(String name,String score,boolean selected){
        this.name=name;this.score=score;this.selected=selected;setMessage(Component.literal(name+", "+score));
    }
    @Override public void renderString(GuiGraphics g,Font font,int color){
        String value=font.plainSubstrByWidth(score,getWidth()-32);int scoreWidth=font.width(value);
        String label=font.plainSubstrByWidth(name,Math.max(0,getWidth()-scoreWidth-16));
        int ink=selected?0xFF66E2CF:color;
        g.drawString(font,label,getX()+5,getY()+4,ink,false);
        g.drawString(font,value,getX()+getWidth()-5-scoreWidth,getY()+4,ink,false);
    }
}
