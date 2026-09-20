package net.goui.cosmicdungeon.block.entity;

import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Fair, global work bound; each requested entity gets at most one maintenance visit per tick. */
public final class SpawnerMaintenanceQueue<K> {
    private static final class Work {
        long tick;
        int remaining;
        Work(long tick, int remaining) { this.tick = tick; this.remaining = remaining; }
    }
    private final LinkedHashMap<K, Work> queue = new LinkedHashMap<>();
    public void request(K key, long tick, int units) {
        if (units <= 0) { queue.remove(key); return; }
        var old = queue.get(key);
        if (old == null) queue.put(key, new Work(tick, units));
        else if (old.tick != tick) { old.tick = tick; old.remaining = units; }
    }
    public int run(long tick, int budget, Consumer<K> visit) {
        int inspected = 0;
        while (inspected < Math.max(0, budget) && !queue.isEmpty()) {
            var entry = queue.pollFirstEntry();
            inspected++;
            var work = entry.getValue();
            if (work.tick != tick) continue;
            visit.accept(entry.getKey());
            if (--work.remaining > 0) queue.putLast(entry.getKey(), work);
        }
        return inspected;
    }
    public void removeIf(Predicate<K> predicate) { queue.keySet().removeIf(predicate); }
    public void clear() { queue.clear(); }
    public int size() { return queue.size(); }
}
