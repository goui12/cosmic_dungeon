package net.goui.cosmicdungeon.client.screen.skills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Native shaded geometry: crisp herb sprigs / Kibble pieces at any GUI scale, with no texture or dependency. */
public final class ClassResourceBar {
    private ClassResourceBar(){}
    public static Rect world(int width,int height,int occupiedBottom){
        int w=Math.min(182,Math.max(0,width-8)),h=Math.min(18,Math.max(0,height));
        // Native stack height accounts for extra hearts, armor, mounts and air. Also clear selected-item/action text.
        long bottom=Math.max(68L,Math.max(0L,occupiedBottom)+9L);
        return new Rect(Math.max(0,(width-w)/2),(int)Math.max(0L,(long)height-bottom-h-8),w,h);
    }
    public static int textScalePercent(int measuredWidth,int availableWidth){
        return measuredWidth<=0?100:(int)Math.clamp((long)Math.max(0,availableWidth)*100/measuredWidth,1L,100L);
    }
    public static void draw(GuiGraphics g,Font font,Rect box,ClassResourceSnapshot value){
        if(box.width()<8||box.height()<8)return;
        boolean herb=ClassResourceSnapshot.BREWING.equals(value.resourceId());
        int x=box.x(),y=box.y(),w=box.width(),h=box.height();
        int dark=herb?0xFF102E26:0xFF322016,edge=herb?0xFF77B493:0xFFD7AD6B;
        g.fill(x,y,x+w,y+h,0xF011171C);
        g.fill(x,y,x+w,y+1,edge);g.fill(x,y+h-1,x+w,y+h,0xFF071014);
        g.fill(x,y+1,x+1,y+h-1,edge);g.fill(x+w-1,y+1,x+w,y+h-1,dark);
        boolean icon=w>=80&&h>=16;
        int start=x+(icon?19:3),barWidth=Math.max(0,x+w-3-start);
        g.fill(start,y+3,start+barWidth,y+h-3,dark);
        int filled=value.filledPixels(barWidth);
        g.fill(start,y+3,start+filled,y+h-3,herb?0xFF438C63:0xFFAA713B);
        if(filled>0){
            g.fill(start,y+3,start+filled,y+5,herb?0xFF8BD8A4:0xFFE8BD79);
            g.fill(start,y+h-5,start+filled,y+h-3,herb?0xFF2B6549:0xFF774621);
            for(int i=1;i<6;i++){
                int tick=start+barWidth*i/6;
                g.fill(tick,y+3,tick+1,y+h-3,0x44333A3D);
            }
        }
        if(icon){if(herb)herb(g,x+3,y+(h-14)/2);else kibble(g,x+3,y+(h-14)/2);}
        String label=w>=150?value.title()+" "+value.count():value.count();
        int measured=font.width(label),available=Math.max(1,barWidth-4);
        float scale=textScalePercent(measured,available)/100f;
        var pose=g.pose();pose.pushMatrix();
        try{
            pose.translate(start+barWidth/2f,y+(h-font.lineHeight*scale)/2f);
            pose.scale(scale,scale);
            g.drawString(font,label,-measured/2,0,0xFFFFFFFF,true);
        }finally{pose.popMatrix();}
    }
    private static void herb(GuiGraphics g,int x,int y){
        // Stem, paired pointed leaves, veins and a pale budding tip form a shaded sprig.
        g.fill(x+6,y+2,x+8,y+14,0xFF345C38);g.fill(x+7,y+3,x+8,y+13,0xFFB2C47D);
        leaf(g,x+1,y+7,false);leaf(g,x+7,y+4,true);leaf(g,x+2,y+2,false);
        g.fill(x+6,y,x+9,y+2,0xFFF0DFA7);g.fill(x+7,y-1,x+8,y+3,0xFFFDFADE);
    }
    private static void leaf(GuiGraphics g,int x,int y,boolean right){
        g.fill(x+1,y,x+4,y+1,0xFF78B980);g.fill(x,y+1,x+5,y+3,0xFF3D8654);
        g.fill(x+1,y+3,x+4,y+4,0xFF245D3E);g.fill(x+1,y+1,x+4,y+2,0xFFA1D59D);
        g.fill(x+(right?0:3),y+2,x+(right?2:5),y+4,0xFF7DAB63);
    }
    private static void kibble(GuiGraphics g,int x,int y){
        piece(g,x+1,y+1);piece(g,x+7,y+4);piece(g,x+2,y+8);
    }
    private static void piece(GuiGraphics g,int x,int y){
        g.fill(x+1,y,x+4,y+1,0xFFD9B277);g.fill(x,y+1,x+5,y+4,0xFF9A6538);
        g.fill(x+1,y+4,x+4,y+5,0xFF603D24);g.fill(x+1,y+1,x+4,y+2,0xFFF1CD8D);
        g.fill(x+2,y+2,x+4,y+4,0xFFBF8748);g.fill(x+1,y+3,x+2,y+4,0xFF784A29);
    }
}
