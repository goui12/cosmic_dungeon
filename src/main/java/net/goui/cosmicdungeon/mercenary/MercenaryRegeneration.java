package net.goui.cosmicdungeon.mercenary;

/** Active ticks only: no offline catch-up, and reload/revival starts a fresh quiet period. */
public final class MercenaryRegeneration {
    private int quiet,healing;
    public void combat(){quiet=0;healing=0;}
    public float advance(int ticks,boolean fighting){
        if(ticks<0||ticks>20)throw new IllegalArgumentException("Invalid recovery step");
        if(fighting){combat();return 0;}
        int waiting=Math.min(ticks,300-quiet);quiet+=waiting;
        healing+=ticks-waiting;
        if(healing>=20){healing-=20;return .5F;}
        return 0;
    }
    public static void tick(MercenaryEntity entity,int ticks,boolean fighting){
        if(entity.dormant())return;
        float amount=entity.regeneration().advance(ticks,fighting);
        if(amount>0&&entity.getHealth()<entity.getMaxHealth())entity.heal(amount);
    }
}
