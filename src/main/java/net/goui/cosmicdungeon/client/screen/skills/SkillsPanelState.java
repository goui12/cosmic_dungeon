package net.goui.cosmicdungeon.client.screen.skills;

import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;

/** Pure geometry and pointer ownership. Item drags cannot acquire or extend a panel capture. */
public final class SkillsPanelState {
    public static final int HEADER=14,RESOURCE=16,ROW=16,BUTTON=15;
    public record Placement(double x,double y,boolean minimized){
        public Placement{
            if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Non-finite placement");
            x=Math.clamp(x,0,1);y=Math.clamp(y,0,1);
        }
    }
    public record Geometry(Rect panel,Rect header,Rect resource,Rect body,Rect help,Rect minimize,Rect track){
        public Rect row(int index,int scroll){
            int padding = track.width() == 0 ? 6 : 14;
            return new Rect(body.x()+3,body.y()+index*ROW-scroll,Math.max(0,body.width()-padding),BUTTON);
        }
    }
    public record Press(boolean consumed,boolean changed,boolean help,int action){
        static final Press NONE=new Press(false,false,false,-1);
    }
    private SharedInventoryLayout shared;private Placement placement;private boolean positioned,dirty;
    private boolean compactExpanded;
    private int preferredWidth = Integer.MAX_VALUE;
    private int groupWidth, groupBottom, groupMinimum;

    void reserveGroup(int width, int bottom, int minimum) {
        if (width == groupWidth && bottom == groupBottom && minimum == groupMinimum) return;
        groupWidth = width;
        groupBottom = bottom;
        groupMinimum = minimum;
        rebuild();
    }

    // When both free intervals are too short, snap the rendered panel to the nearer
    // legal edge. Placement retains the requested coordinates for later resizes.
    private int avoidSplittingGroup(int x, int y, int width, int height) {
        if (groupMinimum == 0 || x >= 8 + groupWidth || x + width <= 8) return y;
        int highestUnderGroup = 8 + groupMinimum + 4;
        int lowestAboveGroup = groupBottom - groupMinimum - 4 - height;
        if (y >= highestUnderGroup || y <= lowestAboveGroup) return y;
        if (highestUnderGroup > yLimit(height)) return Math.max(0, lowestAboveGroup);
        if (lowestAboveGroup < 0) return highestUnderGroup;
        return y - lowestAboveGroup <= highestUnderGroup - y ? lowestAboveGroup : highestUnderGroup;
    }
    private Geometry geometry;private int actions,auxiliaryButtons;private boolean moving,ownedPress;private double grabX,grabY;
    private final PanelScroll scroll=new PanelScroll();
    public SkillsPanelState(SharedInventoryLayout shared,Placement saved){
        placement=saved==null?new Placement(0,0,false):saved;positioned=saved!=null;resize(shared);
    }
    public void resize(SharedInventoryLayout value){
        if(value.equals(shared))return;
        release();shared=value;rebuild();
    }
    public void actions(int count){content(count,preferredWidth);}
    public void content(int count,int width){
        int nextCount=Math.clamp(count,0,128);
        if(nextCount==actions && width==preferredWidth)return;
        actions=nextCount;
        preferredWidth=width;
        rebuild();
    }
    public Geometry geometry(){return geometry;}
    public PanelScroll scroll(){return scroll;}
    public Placement placement(){return placement;}
    public boolean dirty(){return dirty;}
    public void saved(){dirty=false;}
    public boolean captured(){return moving||ownedPress||scroll.captured();}
    public boolean minimized(){return placement.minimized() || (shared.compact() && !compactExpanded);}
    private int xLimit(int width){
        return Math.max(4, shared.screenWidth() - width - 4);
    }
    private int yLimit(int height){return Math.max(4,shared.screenHeight()-height-4);}
    /** Use the nearest free edge of a right-side panel only when its actual rectangle is occupied. */
    private Rect avoidRightPanel(Rect desired) {
        Rect obstacle = shared.requests();
        if (obstacle.width() == 0 || obstacle.height() == 0 || !desired.intersects(obstacle)) return desired;
        int w = desired.width(), h = desired.height(), x = desired.x(), y = desired.y();
        Rect nearest = null;
        int distance = Integer.MAX_VALUE;
        for (Rect candidate : new Rect[]{
                new Rect(obstacle.x() - w, y, w, h), new Rect(obstacle.right(), y, w, h),
                new Rect(x, obstacle.y() - h, w, h), new Rect(x, obstacle.bottom(), w, h)}) {
            if (candidate.x() < 0 || candidate.y() < 0 || candidate.right() > shared.screenWidth()
                    || candidate.bottom() > shared.screenHeight()) continue;
            int move = Math.abs(candidate.x() - x) + Math.abs(candidate.y() - y);
            if (move < distance) {
                nearest = candidate;
                distance = move;
            }
        }
        return nearest == null ? desired : nearest;
    }
    private void rebuild(){
        int width=Math.min(shared.screenWidth(),Math.max(40,Math.min(preferredWidth,shared.skillsDefault().width())));
        int desired=HEADER+RESOURCE+(minimized()?0:Math.max(1,actions)*ROW);
        // Keep the group controls reachable in the same lane while the skill list scrolls.
        int heightLimit=Math.max(HEADER+RESOURCE+ROW,shared.screenHeight()-120);
        if (groupMinimum > 0)
            heightLimit = Math.min(heightLimit, Math.max(HEADER + RESOURCE + ROW, groupBottom - groupMinimum - 8));
        int height=Math.min(shared.screenHeight(),Math.min(desired,heightLimit));
        int x=positioned?4+(int)Math.round(placement.x()*(xLimit(width)-4)):shared.skillsDefault().x();
        int y=positioned?4+(int)Math.round(placement.y()*(yLimit(height)-4)):shared.skillsDefault().y();
        x=Math.clamp(x,0,Math.min(shared.screenWidth()-width,xLimit(width)));
        y=Math.clamp(y,0,Math.min(shared.screenHeight()-height,yLimit(height)));
        Rect free = avoidRightPanel(new Rect(x, y, width, height));
        x = free.x();
        y = avoidSplittingGroup(x, free.y(), width, height);
        int header=Math.min(HEADER,height),resource=Math.min(RESOURCE,height-header);
        var panel=new Rect(x,y,width,height);var head=new Rect(x,y,width,header);
        var res=new Rect(x,y+header,width,resource);var body=new Rect(x,y+header+resource,width,height-header-resource);
        geometry=new Geometry(panel,head,res,body,new Rect(Math.max(x,x+width-36),y+2,14,Math.max(0,header-4)),
                new Rect(Math.max(x,x+width-18),y+2,14,Math.max(0,header-4)),
                new Rect(Math.max(x,body.right()-9),body.y(),actions*ROW>body.height()?7:0,body.height()));
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
            boolean collapse = !minimized();
            compactExpanded = !collapse;
            placement=new Placement(placement.x(),placement.y(),collapse);positioned=true;dirty=true;
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
