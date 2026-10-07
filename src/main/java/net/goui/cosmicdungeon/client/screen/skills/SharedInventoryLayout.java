package net.goui.cosmicdungeon.client.screen.skills;

/** Shared scaled-pixel reservations; never moves inventory slots or requests server changes. */
public record SharedInventoryLayout(Rect group,Rect requests,Rect skillsDefault,Rect account,
                                    Rect worldResource,int screenWidth,int screenHeight,boolean compact) {
    public record Rect(int x,int y,int width,int height){
        public Rect{if(width<0||height<0)throw new IllegalArgumentException("Negative rectangle");}
        public int right(){return x+width;}
        public int bottom(){return y+height;}
        public boolean contains(double x,double y){return x>=this.x&&x<right()&&y>=this.y&&y<bottom();}
        public boolean intersects(Rect other){return x<other.right()&&right()>other.x&&y<other.bottom()&&bottom()>other.y;}
    }
    private static Rect bounded(int x,int y,int width,int height,int sw,int sh){
        width=Math.clamp(width,0,sw);height=Math.clamp(height,0,sh);
        return new Rect(Math.clamp(x,0,sw-width),Math.clamp(y,0,sh-height),width,height);
    }
    public static SharedInventoryLayout of(int screenWidth,int screenHeight,int guiLeft,int guiTop,int imageWidth,int imageHeight){
        int sw=Math.max(1,screenWidth),sh=Math.max(1,screenHeight);
        int left=Math.max(0,guiLeft-16),right=guiLeft+imageWidth+8;
        boolean compact=left<40||sh<180;
        int skillWidth=compact?Math.min(112,sw-8):Math.min(176,left);
        int skillHeight=compact?36:Math.clamp(sh/3,72,128);
        var skills=bounded(8,8,skillWidth,skillHeight,sw,sh);
        int groupY=skills.bottom()+8;
        var group=bounded(8,groupY,Math.min(224,left),Math.max(24,sh-groupY-32),sw,sh);
        var requests=bounded(right,Math.max(8,guiTop),Math.min(160,Math.max(0,sw-right-8)),
                Math.min(Math.max(36,imageHeight),Math.max(0,sh-16)),sw,sh);
        var account=bounded(guiLeft,Math.max(2,guiTop-32),Math.min(imageWidth,176),29,sw,sh);
        var world=bounded((sw-182)/2,sh-56,Math.min(182,sw-8),12,sw,sh);
        return new SharedInventoryLayout(group,requests,skills,account,world,sw,sh,compact);
    }
}
