package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Only reachable, enabled guide pages are navigable; history is bounded and never runs a command. */
final class HelpGuideNavigation {
    static final int HISTORY_LIMIT = 64;
    private record Entry(HelpMenuContent.HelpNode node, HelpMenuContent.HelpNode parent) {}
    private record State(HelpMenuContent.Page page, HelpMenuContent.HelpNode directory) {}
    private final Map<String, Entry> pages = new LinkedHashMap<>();
    private final ArrayDeque<State> history = new ArrayDeque<>();
    private State state = new State(HelpMenuContent.GET_STARTED, HelpMenuContent.ROOT);
    HelpGuideNavigation() { index(HelpMenuContent.ROOT, HelpMenuContent.ROOT); }
    private void index(HelpMenuContent.HelpNode node, HelpMenuContent.HelpNode parent) {
        if (pages.put(node.page().id(), new Entry(node, parent)) != null) throw new IllegalStateException("Duplicate guide page");
        for (var child : node.children()) index(child, node);
    }
    HelpMenuContent.Page page() { return state.page(); }
    HelpMenuContent.HelpNode directory() { return state.directory(); }
    List<HelpMenuContent.HelpNode> children() { return directory().children(); }
    boolean canBack() { return !history.isEmpty(); }
    int historySize() { return history.size(); }
    Map<String, HelpMenuContent.Page> pages() {
        var result = new LinkedHashMap<String, HelpMenuContent.Page>();
        pages.forEach((id, entry) -> result.put(id, entry.node().page()));
        return Map.copyOf(result);
    }
    HelpMenuContent.Page resolve(String id) {
        var entry = pages.get(id);
        return entry == null || !entry.node().page().enabled() ? null : entry.node().page();
    }
    boolean open(String id) {
        var entry = pages.get(id);
        if (entry == null || !entry.node().page().enabled()) return false;
        var next = new State(entry.node().page(), entry.node().isDirectory() ? entry.node() : entry.parent());
        if (state.equals(next)) return false;
        if (history.size() == HISTORY_LIMIT) history.removeLast();
        history.push(state);
        state = next;
        return true;
    }
    boolean back() {
        if (history.isEmpty()) return false;
        state = history.pop();
        return true;
    }
}
