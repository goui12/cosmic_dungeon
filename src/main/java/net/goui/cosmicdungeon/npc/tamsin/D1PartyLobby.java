package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;

/** Transient pre-entry state, owned by the server thread. Never stores an active dungeon roster. */
public final class D1PartyLobby {
    public enum Phase { ASSEMBLY, READY_CHECK, QUEUED, PREPARING }
    public record Anchor(UUID npc, String dimension, long selector) {}
    public record Invitation(String token, UUID inviter, UUID target, Anchor anchor, long expires, boolean accepted) {}
    public static final class Party {
        private final UUID id = UUID.randomUUID();
        private final UUID leader;
        private final Anchor anchor;
        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
        private final Set<UUID> ready = new HashSet<>();
        private Map<UUID, String> classes = Map.of();
        private Phase phase = Phase.ASSEMBLY;
        private long revision, queueOrder, countdownEnd = -1;
        private Party(UUID leader, Anchor anchor) { this.leader = leader; this.anchor = anchor; members.add(leader); }
        public UUID id() { return id; }
        public UUID leader() { return leader; }
        public Anchor anchor() { return anchor; }
        public List<UUID> members() { return List.copyOf(members); }
        public Set<UUID> ready() { return Set.copyOf(ready); }
        public Map<UUID, String> classes() { return classes; }
        public Phase phase() { return phase; }
        public long revision() { return revision; }
        public long countdownEnd() { return countdownEnd; }
    }
    private final Map<UUID, Party> groups = new LinkedHashMap<>();
    private final Map<UUID, Party> membership = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private long revision, queueSequence;

    public Party party(UUID member) { return membership.get(member); }
    public List<Party> parties() { return List.copyOf(groups.values()); }
    public Invitation invitation(UUID target) { return invitations.get(target); }
    public List<Invitation> invitations() { return List.copyOf(invitations.values()); }
    public long revision(UUID player) { var p = party(player); return p == null ? 0 : p.revision; }
    public void clear() { groups.clear(); membership.clear(); invitations.clear(); }
    private void changed(Party party) { party.revision = ++revision; }
    public boolean current(UUID player, long expected) { return revision(player) == expected; }

    /** Explicit opt-in; viewing a menu never creates or merges a party. */
    public boolean canStartSolo(UUID leader, int minimum, int capacity) {
        return leader != null && minimum == 1 && capacity >= 1 && capacity <= 6
                && party(leader) == null && invitation(leader) == null;
    }
    public String startSolo(UUID leader, Anchor anchor, long expected, int minimum, int capacity) {
        if (anchor == null || !canStartSolo(leader, minimum, capacity) || !current(leader, expected))
            return "Solo entry is unavailable; review the party limits and any invitation.";
        var party = new Party(leader, anchor);
        groups.put(party.id, party);
        membership.put(leader, party);
        changed(party);
        return null;
    }

    public String invite(UUID sender, UUID target, Anchor anchor, long now, int lifetimeTicks, int capacity) {
        capacity = Math.min(6, capacity);
        if (capacity < 2) return "This selector has no room for another player.";
        if (sender.equals(target)) return "You cannot invite yourself.";
        if (party(target) != null) return "That player already belongs to a group; groups cannot merge.";
        var p = party(sender);
        if (p != null && !p.leader.equals(sender)) return "Only the group leader can invite players.";
        if (p != null && (!p.anchor.equals(anchor) || p.phase == Phase.QUEUED || p.phase == Phase.PREPARING))
            return "Cancel queueing before inviting players at this Tamsin.";
        if (p != null && p.members.size() >= capacity) return "This group is full.";
        var existing = invitations.get(target);
        if (existing != null && existing.expires > now) return "That player already has a pending invitation.";
        invitations.put(target, new Invitation(UUID.randomUUID().toString(), sender, target, anchor,
                now + lifetimeTicks, false));
        return null;
    }
    public String accept(UUID target, String token, long now) {
        var invitation = invitations.get(target);
        if (invitation == null || !invitation.token.equals(token)) return "Invitation is no longer available.";
        if (now >= invitation.expires) { invitations.remove(target); return "Invitation expired."; }
        if (party(target) != null) { invitations.remove(target); return "You already belong to a group."; }
        invitations.put(target, new Invitation(token, invitation.inviter, target, invitation.anchor, invitation.expires, true));
        return null;
    }
    public boolean decline(UUID target, String token) {
        var i = invitations.get(target);
        if (i == null || !i.token.equals(token)) return false;
        invitations.remove(target); return true;
    }
    /** Only call with server-verified onboarding/eligibility; pending acceptance never reserves a slot. */
    public String joinAccepted(UUID target, long now, int capacity, boolean eligible) {
        capacity = Math.min(6, capacity);
        var i = invitations.get(target);
        if (i == null || !i.accepted) return null;
        if (now >= i.expires) { invitations.remove(target); return "Invitation expired."; }
        var p = party(i.inviter);
        if (party(target) != null || (p != null && (!p.leader.equals(i.inviter) || !p.anchor.equals(i.anchor)))) {
            invitations.remove(target); return "The inviting group changed.";
        }
        if (p != null && (p.phase == Phase.PREPARING || p.members.size() >= capacity)) {
            invitations.remove(target); return "That group is full or has started entry preparation.";
        }
        if (!eligible) return null;
        if (capacity < 2) { invitations.remove(target); return "This selector cannot admit a group."; }
        if (p == null) {
            p = new Party(i.inviter, i.anchor); groups.put(p.id, p); membership.put(i.inviter, p);
            invitations.remove(i.inviter);
        }
        p.members.add(target); membership.put(target, p); invitations.remove(target);
        cancel(p); return null;
    }
    public void cancel(Party p) {
        if (groups.get(p.id) != p) return;
        p.phase = Phase.ASSEMBLY; p.ready.clear(); p.classes = Map.of();
        p.countdownEnd = -1; p.queueOrder = 0; changed(p);
    }
    public String begin(UUID leader, long expected, Map<UUID, String> classes, int minimum, int capacity) {
        var p = party(leader);
        if (p == null || !p.leader.equals(leader)) return "Only the group leader can begin readiness.";
        if (!current(leader, expected) || p.phase != Phase.ASSEMBLY) return "Group changed; refresh before readying.";
        if (p.members.size() < Math.max(1, minimum) || p.members.size() > Math.min(6, capacity))
            return "The group must fit this selector's party limits.";
        if (!classes.keySet().equals(p.members) || classes.values().stream().anyMatch(c -> c == null || c.isBlank() || c.equals("none")))
            return "Every member must personally select a class.";
        p.classes = Map.copyOf(classes); p.ready.clear(); p.phase = Phase.READY_CHECK; changed(p);
        return null;
    }
    public String ready(UUID player, long expected) {
        var p = party(player);
        if (p == null || !current(player, expected) || p.phase != Phase.READY_CHECK) return "Wait for your leader's ready check.";
        p.ready.add(player); // Same ready-check revision permits simultaneous confirmations; duplicates are idempotent.
        return null;
    }
    public String queue(UUID leader, long expected) {
        var p = party(leader);
        if (p == null || !p.leader.equals(leader)) return "Only the group leader can submit the queue.";
        if (!current(leader, expected) || p.phase != Phase.READY_CHECK || !p.ready.equals(p.members))
            return "Every member must confirm ready first.";
        p.phase = Phase.QUEUED; p.queueOrder = ++queueSequence; changed(p); return null;
    }
    public List<Party> queued() {
        return groups.values().stream().filter(p -> p.phase == Phase.QUEUED)
                .sorted(Comparator.comparingLong(p -> p.queueOrder)).toList();
    }
    public int queuePosition(Party p) {
        if (p == null || p.phase != Phase.QUEUED) return 0;
        int position = 1;
        for (var other : groups.values())
            if (other.phase == Phase.QUEUED && other.queueOrder < p.queueOrder) position++;
        return position;
    }
    public void startCountdown(Party p, long now, int ticks) {
        if (p.phase == Phase.QUEUED && p.countdownEnd < 0) { p.countdownEnd = now + ticks; changed(p); }
    }
    public void waitForSlot(Party p) {
        if (p.phase == Phase.QUEUED && p.countdownEnd >= 0) { p.countdownEnd = -1; changed(p); }
    }
    public boolean prepare(Party p, long now) {
        if (p.phase != Phase.QUEUED || p.countdownEnd < 0 || now < p.countdownEnd) return false;
        p.phase = Phase.PREPARING; changed(p); return true;
    }
    public List<UUID> remove(UUID member) {
        var p = party(member);
        invitations.remove(member);
        invitations.values().removeIf(i -> i.inviter.equals(member));
        if (p == null || p.phase == Phase.PREPARING) return List.of();
        var affected = p.members();
        if (p.leader.equals(member)) complete(p);
        else { p.members.remove(member); membership.remove(member); cancel(p); }
        return affected;
    }
    public void complete(Party p) {
        if (groups.remove(p.id) == null) return;
        p.members.forEach(membership::remove);
        invitations.values().removeIf(i -> i.inviter.equals(p.leader));
    }
    public void expire(long now) { invitations.values().removeIf(i -> now >= i.expires); }
}
