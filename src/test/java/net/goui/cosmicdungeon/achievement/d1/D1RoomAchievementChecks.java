package net.goui.cosmicdungeon.achievement.d1;

import java.util.*;

public final class D1RoomAchievementChecks {
    private static int checks;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        check(SharedAchievementRules.eligible(true,false,false,false,false,true,true), "Present member earns shared credit, alive or dead");
        check(SharedAchievementRules.eligible(true,false,false,false,false,false,false), "Briefly disconnected dungeon member retains credit");
        check(!SharedAchievementRules.eligible(false,false,false,false,false,true,true), "Nearby nonmember excluded");
        check(!SharedAchievementRules.eligible(true,true,false,false,false,false,false), "Completed exit excluded");
        check(!SharedAchievementRules.eligible(true,false,true,false,false,false,false), "Offline Village inventory excluded");
        check(!SharedAchievementRules.eligible(true,false,false,true,false,false,false), "Offline developer excluded by rank");
        check(!SharedAchievementRules.eligible(true,false,false,false,true,true,true), "Spectator observer excluded");
        check(!SharedAchievementRules.eligible(true,false,false,false,false,true,false), "Online member in another dimension excluded");
        var cursor = new RoomScanCursor(-2,-1,7,-1,1,10,24);
        check(cursor.volume()==24, "Inclusive irregular region volume");
        var visited = new HashSet<RoomScanCursor.Point>();
        int ticks = 0;
        while (cursor.hasNext()) {
            int used = 0;
            while (used < 5 && cursor.hasNext()) {
                var point = cursor.next(); used++;
                check(point.x()>=-2 && point.x()<=-1 && point.y()>=-1 && point.y()<=1 && point.z()>=7 && point.z()<=10,
                        "Every position stays inside authored bounds");
                check(visited.add(point), "No repeated/skipped cursor index");
            }
            check(used<=5,"Incremental work fits chosen budget"); ticks++;
        }
        check(visited.size()==24 && ticks==5, "Complete room is reached across successive ticks");
        var single = new RoomScanCursor(0,0,0,0,0,0,1);
        check(single.next().equals(new RoomScanCursor.Point(0,0,0)) && !single.hasNext(), "One-block boundary");
        boolean exhausted=false;
        try { single.next(); } catch (IllegalStateException expected) { exhausted=true; }
        check(exhausted, "Completed cursor cannot restart implicitly");
        boolean inverted=false,oversized=false,overflow=false;
        try { new RoomScanCursor(1,0,0,0,0,0,1); } catch (IllegalArgumentException expected) { inverted=true; }
        try { new RoomScanCursor(0,0,0,5,5,5,100); } catch (IllegalArgumentException expected) { oversized=true; }
        try { new RoomScanCursor(Integer.MIN_VALUE,Integer.MIN_VALUE,0,Integer.MAX_VALUE,Integer.MAX_VALUE,2,Long.MAX_VALUE); }
        catch (IllegalArgumentException expected) { overflow=true; }
        check(inverted && oversized && overflow, "Bad or enormous binding fails before traversal");
        System.out.println(checks + " room achievement checks passed");
    }
}
