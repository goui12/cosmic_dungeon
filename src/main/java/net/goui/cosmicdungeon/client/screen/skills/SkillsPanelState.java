package net.goui.cosmicdungeon.client.screen.skills;

import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Pure geometry and pointer ownership. Item drags cannot acquire or extend a panel capture. */
public final class SkillsPanelState {
    public static final int HEADER=18,RESOURCE=18,ROW=24,BUTTON=20;
    public record Placement(double x,double y,boolean minimized){
        public Placement{
            if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Non-finite placement");
            x=Math.clamp(x,0,1);y=Math.clamp(y,0,1);
        }
    }
    public record Geometry(Rect panel,Rect header,Rect resource,Rect body,Rect help,Rect minimize,Rect track){
        public Rect row(int index,int scroll){
            return new Rect(body.x()+4,body.y()+index*ROW-scroll,Math.max(0,body.width()-16),BUTTON);
        }
    }
    public record Press(boolean consumed,boolean changed,boolean help,int action){
        static final Press NONE=new Press(false,false,false,-1);
    }
    private SharedInventoryLayout shared;private Placement placement;private boolean positioned,dirty;
    private Geometry geometry;private int actions,auxiliaryButtons;private boolean moving,ownedPress;private double grabX,grabY;
    private final PanelScroll scroll=new PanelScroll();
    public SkillsPanelState(SharedInventoryLayout shared,Placement saved){
        placement=saved==null?new Placement(0,0,false):saved;positioned=saved!=null;resize(shared);
    }
    public void resize(SharedInventoryLayout value){
        if(value.equals(shared))return;
        release();shared=value;rebuild();
    }
    public void actions(int count){actions=Math.clamp(count,0,128);scroll.configure(actions*ROW,geometry.body.height());}
    public Geometry geometry(){return geometry;}
    public PanelScroll scroll(){return scroll;}
    public Placement placement(){return placement;}
    public boolean dirty(){return dirty;}
    public void saved(){dirty=false;}
    public boolean captured(){return moving||ownedPress||scroll.captured();}
    public boolean minimized(){return placement.minimized()||shared.compact();}
    private int xLimit(int width){
        int right=shared.requests().width()>0?shared.requests().x()-4:shared.screenWidth()-4;
        return Math.max(4,right-width);
    }
    private int yLimit(int height){return Math.max(4,shared.screenHeight()-height-4);}
    private void rebuild(){
        int width=Math.min(shared.screenWidth(),Math.max(40,shared.skillsDefault().width()));
        int height=Math.min(shared.screenHeight(),minimized()?HEADER+RESOURCE:Math.max(60,shared.skillsDefault().height()));
        int x=positioned?4+(int)Math.round(placement.x()*(xLimit(width)-4)):shared.skillsDefault().x();
        int y=positioned?4+(int)Math.round(placement.y()*(yLimit(height)-4)):shared.skillsDefault().y();
        x=Math.clamp(x,0,Math.min(shared.screenWidth()-width,xLimit(width)));
        y=Math.clamp(y,0,Math.min(shared.screenHeight()-height,yLimit(height)));
        int header=Math.min(HEADER,height),resource=Math.min(RESOURCE,height-header);
        var panel=new Rect(x,y,width,height);var head=new Rect(x,y,width,header);
        var res=new Rect(x,y+header,width,resource);var body=new Rect(x,y+header+resource,width,height-header-resource);
        geometry=new Geometry(panel,head,res,body,new Rect(Math.max(x,x+width-36),y+2,14,Math.max(0,header-4)),
                new Rect(Math.max(x,x+width-18),y+2,14,Math.max(0,header-4)),
                new Rect(Math.max(x,body.right()-9),body.y(),7,body.height()));
        scroll.configure(actions*ROW,body.height());
    }
    private void locate(double x,double y){
        var panel=geometry.panel();int maxX=xLimit(panel.width()),maxY=yLimit(panel.height());
        double nx=maxX==4?0:(Math.clamp(x,4,maxX)-4)/(maxX-4);
        double ny=maxY==4?0:(Math.clamp(y,4,maxY)-4)/(maxY-4);
        var next=new Placement(nx,ny,placement.minimized());
        if(!positioned||!next.equals(placement)){placement=next;positioned=true;dirty=true;rebuild();}
    }
    public Press press(double x,double y,int button,boolean carrying){
        if(carrying||!Double.isFinite(x)||!Double.isFinite(y)||!geometry.panel.contains(x,y))return Press.NONE;
        if(button!=0){
            if(button>0&&button<8)auxiliaryButtons|=1<<button;
            return new Press(true,false,false,-1);
        }
        ownedPress=true;
        if(geometry.minimize.contains(x,y)){
            // Preserve the current top-left when the panel's height changes.
            int px=geometry.panel.x(),py=geometry.panel.y();
            placement=new Placement(placement.x(),placement.y(),!placement.minimized());positioned=true;dirty=true;
            rebuild();locate(px,py);scroll.reset();return new Press(true,true,false,-1);
        }
        if(geometry.help.contains(x,y))return new Press(true,false,true,-1);
        if(geometry.header.contains(x,y)){
            moving=true;grabX=x-geometry.panel.x();grabY=y-geometry.panel.y();return new Press(true,false,false,-1);
        }
        if(geometry.track.contains(x,y)&&scroll.press(geometry.track.y(),y))return new Press(true,false,false,-1);
        if(geometry.body.contains(x,y)){
            int index=(int)((y-geometry.body.y()+scroll.offset())/ROW);
            if(index>=0&&index<actions&&geometry.row(index,scroll.offset()).contains(x,y))
                return new Press(true,false,false,index);
        }
        return new Press(true,false,false,-1);
    }
    public boolean drag(double x,double y,int button,boolean carrying){
        if(!captured()||button!=0)return false;
        if(carrying){release();return false;}
        if(moving){if(Double.isFinite(x)&&Double.isFinite(y))locate(x-grabX,y-grabY);return true;}
        scroll.drag(geometry.track.y(),y);return true;
    }
    public boolean wheel(double x,double y,double delta,boolean carrying){
        if(carrying||!geometry.panel.contains(x,y)||!Double.isFinite(delta))return false;
        if(geometry.body.contains(x,y))scroll.wheel(delta);
        return true; // Even at the endpoint, the pane underneath must not also scroll.
    }
    public boolean release(int button){
        if(button==0)return release();
        if(button<0||button>=8)return false;
        int mask=1<<button;boolean owned=(auxiliaryButtons&mask)!=0;auxiliaryButtons&=~mask;return owned;
    }
    public boolean release(){boolean was=captured();moving=false;ownedPress=false;auxiliaryButtons=0;scroll.release();return was;}
}
