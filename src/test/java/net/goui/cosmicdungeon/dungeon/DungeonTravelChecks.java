package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.potion.CompanionshipSelection;
import java.util.*;
public final class DungeonTravelChecks {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        // Real allowed member plus each independent disqualifier. No client state is an input.
        var good = new DungeonTravelRules.Member(42,true,true,false,false,false,false,true,true,false,false,false,true);
        check(DungeonTravelRules.inside(good), "Active inside owner with settled inventory can travel");
        check(!DungeonTravelRules.inside(null), "No membership fails closed");
        var fields = DungeonTravelRules.Member.class.getRecordComponents();
        Class<?>[] types = Arrays.stream(fields).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);
        var constructor = DungeonTravelRules.Member.class.getDeclaredConstructor(types);
        Object[] original = new Object[fields.length];
        for (int i=0;i<fields.length;i++) original[i]=fields[i].getAccessor().invoke(good);
        for (int i=1;i<fields.length;i++) {
            Object[] bad=original.clone();bad[i]=!(boolean)bad[i];
            check(!DungeonTravelRules.inside(constructor.newInstance(bad)), "Travel rejects changed prerequisite: "+fields[i].getName());
        }
        for(long id:new long[]{0,-1,Long.MIN_VALUE}) {
            Object[] bad=original.clone();bad[0]=id;
            check(!DungeonTravelRules.inside(constructor.newInstance(bad)), "Invalid run ID rejected");
        }
        for(boolean developer:new boolean[]{false,true}) {
            check(DungeonTravelRules.riftBoundary(false,false,false,developer), "Ordinary outside travel retained");
            check(DungeonTravelRules.riftBoundary(true,true,false,developer), "Internal dungeon portal retained");
            check(DungeonTravelRules.riftBoundary(true,false,true,developer), "Reset exit remains possible");
            check(DungeonTravelRules.riftBoundary(true,false,false,developer)==developer, "Unjournaled dungeon escape rejected");
            check(DungeonTravelRules.riftBoundary(true,true,true,developer)==developer, "Internal reset destination rejected");
        }
        for(String name:List.of("main_village","village","Main Village"," MAIN_VILLAGE ")) {
            check(DungeonTravelRules.villageName(name), "Known Main Village alias recognized");
            check(!DungeonTravelRules.village(true,false,false), "Locked player cannot use named village");
            check(DungeonTravelRules.village(true,true,false), "Personal unlocked access retained");
            check(DungeonTravelRules.village(true,false,true), "Developer authored-world access retained");
        }
        check(!DungeonTravelRules.villageName(null)&&!DungeonTravelRules.villageName("spawn"), "Spawn is not guessed to be village");
        check(DungeonTravelRules.village(false,false,false), "Unrelated destinations do not inherit village gate");
        var s=CompanionshipSelection.create(42,1000,100);
        check(s.valid(42,1000)&&s.valid(42,1099), "Selection uses original run and server clock window");
        check(!s.valid(42,1100)&&!s.valid(42,999), "Expiry and clock rollback invalidate selection");
        check(!s.valid(43,1050)&&!s.valid(0,1050), "Later run and absent run cannot reuse selection");
        check(!new CompanionshipSelection(42,1000,1000).valid(42,1000), "Malformed zero-length selection rejected");
        for(long run:new long[]{0,-1}) {
            boolean rejected=false;try{CompanionshipSelection.create(run,100,20);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"Invalid run cannot create selection");
        }
        for(long now:new long[]{-1,Long.MAX_VALUE}) {
            boolean rejected=false;try{CompanionshipSelection.create(42,now,20);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"Invalid/overflow clock rejected");
        }
        for(int duration:new int[]{0,-1}) {
            boolean rejected=false;try{CompanionshipSelection.create(42,100,duration);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"Nonpositive selection duration rejected");
        }
        System.out.println(checks+" travel/session checks passed");
    }
}
