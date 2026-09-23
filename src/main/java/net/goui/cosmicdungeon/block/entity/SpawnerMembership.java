package net.goui.cosmicdungeon.block.entity;

import java.util.*;

/** Derived loaded-entity membership; no world storage and no scans of unrelated entities. */
public final class SpawnerMembership<E> {
    private final Map<UUID, Member<E>> members = new HashMap<>();
    private final Map<String, Group<E>> groups = new HashMap<>();
    private static final class Member<E> {
        final E value;
        final Set<String> tags;
        boolean alive;
        Member(E value, Set<String> tags, boolean alive) { this.value = value; this.tags = tags; this.alive = alive; }
    }
    private static final class Group<E> {
        final LinkedHashMap<UUID, Member<E>> members = new LinkedHashMap<>();
        int alive;
    }
    public void put(UUID id, E value, Set<String> tags, boolean alive) {
        remove(id);
        if (tags.isEmpty()) return;
        var member = new Member<>(value, Set.copyOf(tags), alive);
        members.put(id, member);
        for (String tag : member.tags) {
            var group = groups.computeIfAbsent(tag, ignored -> new Group<>());
            group.members.put(id, member);
            if (alive) group.alive++;
        }
    }
    public void alive(UUID id, E value, boolean alive) {
        var member = members.get(id);
        if (member == null || member.value != value || member.alive == alive) return;
        member.alive = alive;
        for (String tag : member.tags) groups.get(tag).alive += alive ? 1 : -1;
    }
    public void remove(UUID id, E value) {
        var member = members.get(id);
        if (member != null && member.value == value) remove(id);
    }
    private void remove(UUID id) {
        var old = members.remove(id);
        if (old == null) return;
        for (String tag : old.tags) {
            var group = groups.get(tag);
            group.members.remove(id);
            if (old.alive) group.alive--;
            if (group.members.isEmpty()) groups.remove(tag);
        }
    }
    public int alive(String tag) { var group = groups.get(tag); return group == null ? 0 : group.alive; }
    public int size(String tag) { var group = groups.get(tag); return group == null ? 0 : group.members.size(); }
    public int size() { return members.size(); }
    public E next(String tag) {
        var group = groups.get(tag);
        if (group == null) return null;
        var entry = group.members.pollFirstEntry();
        group.members.putLast(entry.getKey(), entry.getValue());
        return entry.getValue().value;
    }
}
