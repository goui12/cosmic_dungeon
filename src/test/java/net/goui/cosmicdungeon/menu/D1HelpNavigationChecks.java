package net.goui.cosmicdungeon.menu;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.HelpMenuContent;

/** Structural player-guide contract: only enabled D1 pages and six supported class entries. */
public final class D1HelpNavigationChecks {
    private static int checks;
    private static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    public static void main(String[] args) {
        var pending=new ArrayDeque<HelpMenuContent.HelpNode>();
        pending.add(HelpMenuContent.ROOT);
        var ids=new HashSet<String>();
        while(!pending.isEmpty()) {
            var node=pending.remove();
            check(ids.add(node.page().id()),"Unique navigation target");
            check(node.page().enabled(),"No deferred class reachable");
            pending.addAll(node.children());
        }
        check(!ids.contains("validation")&&!ids.contains("class.deadeye")&&!ids.contains("class.metalmancer"),"No developer/deferred pages");
        var classes=ids.stream().filter(id->id.startsWith("class.")).collect(java.util.stream.Collectors.toSet());
        check(classes.equals(Set.of("class.bogatyr","class.dragoon","class.judicator","class.pyroclast","class.theurgist","class.venefex")),"Six enabled D1 classes");
        check(ids.contains("npc.beluzon")&&ids.contains("vendor.teleport"),"Existing entry link and Inn service reachable");
        System.out.println(checks+" D1 help navigation checks passed");
    }
}
