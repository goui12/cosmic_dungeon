package net.goui.cosmicdungeon.client.screen.skills;

import java.util.Objects;

/** Immutable server snapshot; the client never predicts regeneration or changes this balance on a click. */
public record ClassResourceSnapshot(long runId,String resourceId,int amount,int cap,
                                    boolean active,boolean alive,boolean recyclable,long revision) {
    public static final int CAP=600;
    public static final String BREWING="brewing_supplies",KIBBLE="kibble";
    public ClassResourceSnapshot {
        Objects.requireNonNull(resourceId);
        if(runId<0||revision<0||cap!=CAP||amount<0||amount>cap)
            throw new IllegalArgumentException("Invalid class resource snapshot");
        if(runId==0){
            if(!resourceId.isEmpty()||amount!=0||active||alive||recyclable)
                throw new IllegalArgumentException("Invalid empty class resource snapshot");
        }else if(!BREWING.equals(resourceId)&&!KIBBLE.equals(resourceId))
            throw new IllegalArgumentException("Unknown class resource");
    }
    public String classId(){return BREWING.equals(resourceId)?"theurgist":KIBBLE.equals(resourceId)?"bogatyr":"none";}
    public boolean matches(String classId){return runId>0&&classId().equals(classId);}
    public boolean canRecycle(){return runId>0&&active&&alive&&recyclable&&amount<cap;}
    public String title(){return BREWING.equals(resourceId)?"Brewing Supplies":"Kibble";}
    public String tooltipName(){return BREWING.equals(resourceId)?"brewing supplies":"Kibble";}
    public String count(){return amount+"/"+cap;}
    public int filledPixels(int width){return (int)((long)Math.max(0,width)*amount/cap);}
}
