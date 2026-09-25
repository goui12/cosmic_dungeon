package net.goui.cosmicdungeon.dungeon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One fixed-roster, expiring decision. Disconnects never lower the voting threshold. */
public final class DungeonForfeitBallot {
    public enum State { OPEN, PASSED, REJECTED, EXPIRED, CANCELLED }

    private final UUID id = UUID.randomUUID();
    private final Set<UUID> members;
    private final Map<UUID, Boolean> votes = new HashMap<>();
    private final long deadline;
    private final int required;
    private State state = State.OPEN;

    public DungeonForfeitBallot(Collection<UUID> roster, long deadline) {
        members = Set.copyOf(roster);
        if (members.isEmpty() || members.size() != roster.size())
            throw new IllegalArgumentException("A ballot requires a nonempty, unique roster");
        this.deadline = deadline;
        required = (int) ((2L * members.size() + 2L) / 3L);
    }

    public UUID id() { return id; }
    public Set<UUID> members() { return members; }
    public int required() { return required; }
    public int yesCount() { return (int) votes.values().stream().filter(Boolean::booleanValue).count(); }

    public State refresh(Collection<UUID> roster, long now) {
        if (state == State.OPEN) {
            if (!members.equals(Set.copyOf(roster))) state = State.CANCELLED;
            else if (now >= deadline) state = State.EXPIRED;
        }
        return state;
    }

    /** Each member explicitly casts one vote; even the initiator starts uncommitted. */
    public boolean vote(UUID member, boolean yes, Collection<UUID> roster, long now) {
        if (refresh(roster, now) != State.OPEN || !members.contains(member) || votes.containsKey(member))
            return false;
        votes.put(member, yes);
        int agreed = yesCount();
        if (agreed >= required) state = State.PASSED;
        else if (agreed + members.size() - votes.size() < required) state = State.REJECTED;
        return true;
    }

    public boolean matches(String expectedId) {
        return expectedId == null || id.toString().equals(expectedId);
    }
}
