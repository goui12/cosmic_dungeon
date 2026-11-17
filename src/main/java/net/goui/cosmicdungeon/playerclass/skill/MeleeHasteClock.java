package net.goui.cosmicdungeon.playerclass.skill;

/** Transient AI cooldown in tenths of a tick; preserves fractional haste without faster AI polling. */
public final class MeleeHasteClock {
    private int remaining;
    private boolean melee;
    public boolean ready(){return remaining<=0;}
    public void advance(int elapsedTicks,boolean hasted){
        if(elapsedTicks<0||elapsedTicks>20)throw new IllegalArgumentException("Invalid decision interval");
        // A ready interval that did not produce an attack cannot become an idle attack bank.
        if(remaining<=0){remaining=0;return;}
        remaining-=elapsedTicks*(melee&&hasted?11:10);
    }
    public void restart(int baseTicks,boolean melee,boolean successful){
        if(baseTicks<=0||baseTicks>1000)throw new IllegalArgumentException("Invalid attack cooldown");
        int carry=melee&&this.melee&&successful?Math.min(remaining,0):0;
        remaining=baseTicks*10+carry;this.melee=melee;
    }
    public void discardCredit(){remaining=Math.max(remaining,0);}
    public void clear(){remaining=0;melee=false;}
}
