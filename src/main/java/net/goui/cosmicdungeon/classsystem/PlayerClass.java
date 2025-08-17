// net.goui.cosmicdungeon.classsystem.PlayerClass.java
package net.goui.cosmicdungeon.classsystem;

public enum PlayerClass {
    JUDICATOR,
    THEURGIST,
    PYROCLAST,
    VENEFEX,
    BOGATUR,
    DRAGOON,
    METALMANCER,
    DEADEYE;

    public static PlayerClass from(String s) {
        try { return PlayerClass.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }
}
