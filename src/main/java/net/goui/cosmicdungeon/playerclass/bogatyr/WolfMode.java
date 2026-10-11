package net.goui.cosmicdungeon.playerclass.bogatyr;
/** Append-only wire identities. The first six entries are retired and cannot be selected. */
public enum WolfMode {
    DEFENSIVE, STAND_GROUND, AGGRESSIVE, STRATEGIC, SEARCH_AND_RESCUE, DANGER_CLOSE, PATROL, STAY;
    public boolean active(){return this==PATROL||this==STAY;}
}
