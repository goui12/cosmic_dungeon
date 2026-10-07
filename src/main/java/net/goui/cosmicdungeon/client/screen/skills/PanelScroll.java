package net.goui.cosmicdungeon.client.screen.skills;

/** Pure reusable scrollbar state: callers own hit testing and event cancellation. */
public final class PanelScroll {
    private int content,viewport,offset;private boolean captured;private double grab;
    public void configure(int contentHeight,int viewportHeight){
        content=Math.max(0,contentHeight);viewport=Math.max(0,viewportHeight);
        offset=Math.clamp(offset,0,max());if(max()==0)captured=false;
    }
    public int offset(){return offset;}
    public int max(){return Math.max(0,content-viewport);}
    public int thumbHeight(){return Math.min(viewport,Math.max(12,(int)((long)viewport*viewport/Math.max(1,content))));}
    public int thumbTop(int trackY){
        return trackY+(max()==0?0:(int)((long)offset*(viewport-thumbHeight())/max()));
    }
    public boolean wheel(double delta){
        if(max()==0||!Double.isFinite(delta))return false;
        offset=(int)Math.clamp(Math.round(offset-delta*24),0L,(long)max());return true;
    }
    public boolean press(int trackY,double mouseY){
        if(max()==0||!Double.isFinite(mouseY))return false;
        int top=thumbTop(trackY),height=thumbHeight();
        grab=mouseY>=top&&mouseY<top+height?mouseY-top:height/2.0;
        captured=true;drag(trackY,mouseY);return true;
    }
    public boolean drag(int trackY,double mouseY){
        if(!captured||!Double.isFinite(mouseY))return false;
        int travel=viewport-thumbHeight();
        offset=travel<=0?0:(int)Math.clamp(Math.round((mouseY-grab-trackY)*max()/travel),0L,(long)max());
        return true;
    }
    public boolean release(){boolean was=captured;captured=false;return was;}
    public boolean captured(){return captured;}
    public void reset(){offset=0;captured=false;}
}
