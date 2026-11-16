package net.goui.cosmicdungeon.mercenary;

/**
 * Independent per-recipient healing observation. Time is accumulated online observation ticks,
 * never wall time. Repeated damage means accepted hits no more than ten online seconds apart.
 * Looking, jumping and attacking are deliberately absent: only one block of X/Z displacement
 * recovers a blocked recipient. The caller owns run persistence and active-player admission.
 */
public record StationaryHealingState(long started,long lastDamage,double x,double z,
        int hits,boolean warned,boolean blocked) {
    public static final long DAMAGE_GAP=200,WARNING=600,REFUSAL=800;
    public StationaryHealingState {
        validPosition(x,z);
        if(started<0||lastDamage<started||hits<0||hits>2
                ||warned&&hits<2||blocked&&!warned)
            throw new IllegalArgumentException("Invalid stationary healing state");
    }
    public static StationaryHealingState begin(long now,double x,double z){
        validTime(now);
        return new StationaryHealingState(now,now,x,z,0,false,false);
    }
    public boolean healAllowed(){return !blocked;}
    private static void validTime(long now){
        if(now<0)throw new IllegalArgumentException("Negative observation clock");
    }
    private static void validPosition(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z))
            throw new IllegalArgumentException("Non-finite observation position");
    }
    private void validSample(long now,double currentX,double currentZ){
        validTime(now);validPosition(currentX,currentZ);
        if(now<lastDamage)throw new IllegalArgumentException("Observation clock moved backwards");
    }
    private boolean moved(double currentX,double currentZ){
        double dx=currentX-x,dz=currentZ-z;
        return dx*dx+dz*dz>=1;
    }
    /** Record actual successful damage, not attack attempts, canceled wounds or healing. */
    public StationaryHealingState damage(long now,double currentX,double currentZ){
        validSample(now,currentX,currentZ);
        if(moved(currentX,currentZ)||hits==0||!blocked&&now-lastDamage>DAMAGE_GAP)
            return new StationaryHealingState(now,now,currentX,currentZ,1,false,false);
        return new StationaryHealingState(started,now,x,z,Math.min(2,hits+1),warned,blocked)
                .sample(now,currentX,currentZ);
    }
    /** Called on bounded online observation; no damage is inferred from elapsed time. */
    public StationaryHealingState sample(long now,double currentX,double currentZ){
        validSample(now,currentX,currentZ);
        if(moved(currentX,currentZ))return begin(now,currentX,currentZ);
        if(blocked)return this;
        if(hits>0&&now-lastDamage>DAMAGE_GAP)return begin(now,currentX,currentZ);
        if(hits<2)return this;
        long elapsed=now-started;
        boolean nextBlocked=elapsed>=REFUSAL;
        boolean nextWarned=warned||elapsed>=WARNING;
        return nextBlocked==blocked&&nextWarned==warned?this:
                new StationaryHealingState(started,lastDamage,x,z,hits,nextWarned,nextBlocked);
    }
    /** Versioned scalar entry in existing run storage; no changes to any world schema. */
    public String serialize(){
        return "1|"+started+"|"+lastDamage+"|"+Double.toString(x)+"|"+Double.toString(z)
                +"|"+hits+"|"+warned+"|"+blocked;
    }
    /**
     * Missing storage should call begin. Malformed, future or unknown stored data fails closed
     * at the current position; moving one block still recovers without deleting other run data.
     */
    public static StationaryHealingState parse(String raw,long now,double x,double z){
        validTime(now);validPosition(x,z);
        if(raw!=null){
            try{
                String[] fields=raw.split("\\|",-1);
                if(fields.length==8&&fields[0].equals("1")){
                    var parsed=new StationaryHealingState(Long.parseLong(fields[1]),Long.parseLong(fields[2]),
                            Double.parseDouble(fields[3]),Double.parseDouble(fields[4]),
                            Integer.parseInt(fields[5]),flag(fields[6]),flag(fields[7]));
                    if(parsed.lastDamage<=now)return parsed;
                }
            }catch(IllegalArgumentException invalid){
                // Retain a safe, recoverable refusal rather than silently erase restrictions.
            }
        }
        return new StationaryHealingState(now,now,x,z,2,true,true);
    }
    private static boolean flag(String value){
        if(value.equals("true"))return true;
        if(value.equals("false"))return false;
        throw new IllegalArgumentException("Invalid observation flag");
    }
}
