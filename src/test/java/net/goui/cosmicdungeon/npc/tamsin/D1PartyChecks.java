package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.dungeon.DungeonStartupSchematicPlan;

public final class D1PartyChecks {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static UUID id(int value) { return new UUID(0, value); }
    private static final D1PartyLobby.Anchor A = new D1PartyLobby.Anchor(id(900), "minecraft:overworld", 0);
    private static final D1PartyLobby.Anchor B = new D1PartyLobby.Anchor(id(901), "minecraft:overworld", 1);
    private static void join(D1PartyLobby lobby, UUID leader, UUID member, int capacity) {
        check(lobby.invite(leader, member, A, 0, 12000, capacity) == null, "Leader invitation accepted");
        var invitation = lobby.invitation(member);
        check(lobby.accept(member, invitation.token(), 1) == null, "Recipient accepts own invitation");
        check(lobby.joinAccepted(member, 2, capacity, true) == null, "Eligible recipient joins");
    }
    private static D1PartyLobby group(int leader, int count) {
        var lobby = new D1PartyLobby();
        for (int i = 1; i < count; i++) join(lobby, id(leader), id(leader+i), 6);
        return lobby;
    }
    private static Map<UUID, String> classes(D1PartyLobby.Party party) {
        var result = new LinkedHashMap<UUID, String>();
        for (UUID member : party.members()) result.put(member, "bogatyr");
        return result;
    }
    private static void readyAll(D1PartyLobby lobby, UUID leader) {
        var p = lobby.party(leader);
        check(lobby.begin(leader, p.revision(), classes(p), 3, 6) == null, "Leader begins complete ready check");
        long revision = p.revision();
        for (UUID member : p.members()) check(lobby.ready(member, revision) == null, "Simultaneous personal confirmation");
    }
    public static void main(String[] args) {
        var lobby = new D1PartyLobby();
        check(lobby.invite(id(1), id(1), A, 0, 120, 6) != null, "No self invitation");
        check(lobby.invite(id(1), id(2), A, 0, 120, 6) == null, "Ungrouped inviter can invite");
        check(lobby.party(id(1)) == null, "Sending alone never claims group leadership");
        var pending = lobby.invitation(id(2));
        check(lobby.accept(id(3), pending.token(), 1) != null, "Another UUID cannot accept target's invitation");
        check(lobby.accept(id(2), "bad", 1) != null, "Wrong invitation token rejected");
        check(lobby.invite(id(3), id(2), A, 1, 120, 6) != null, "Existing invitation cannot be overwritten");
        check(lobby.accept(id(2), pending.token(), 2) == null, "First-time recipient can accept");
        lobby.joinAccepted(id(2), 3, 6, false);
        check(lobby.party(id(2)) == null && lobby.invitation(id(2)).accepted(), "Onboarding remains pending without roster access");
        check(lobby.accept(id(2), pending.token(), 4) == null && lobby.invitation(id(2)).expires() == 120, "Replay never extends expiry");
        lobby.joinAccepted(id(2), 5, 6, true);
        var p = lobby.party(id(1));
        check(p != null && p.leader().equals(id(1)) && p.members().equals(List.of(id(1), id(2))), "Accepted sender leads stable ordered roster");
        check(lobby.accept(id(2), pending.token(), 6) != null, "Consumed invitation cannot replay");
        check(lobby.invite(id(2), id(3), A, 6, 120, 6) != null, "Regular members cannot invite");
        check(lobby.invite(id(3), id(1), A, 6, 120, 6) != null, "Inviting another group leader cannot merge");
        check(lobby.begin(id(2), p.revision(), classes(p), 3, 6) != null, "Nonleader cannot begin ready check");
        check(lobby.begin(id(1), p.revision(), classes(p), 3, 6) != null, "Two members cannot queue");
        check(lobby.invite(id(1), id(3), B, 6, 120, 6) != null, "Cannot change another group's selector anchor");
        join(lobby, id(1), id(3), 6);
        var missing = classes(p); missing.remove(id(3));
        check(lobby.begin(id(1), p.revision(), missing, 3, 6) != null, "Every roster member needs class snapshot");
        var noClass = classes(p); noClass.put(id(3), "none");
        check(lobby.begin(id(1), p.revision(), noClass, 3, 6) != null, "Unselected member blocks readiness");
        readyAll(lobby, id(1));
        check(p.phase() == D1PartyLobby.Phase.READY_CHECK, "All ready never automatically queues");
        check(lobby.ready(id(1), p.revision()) == null && p.ready().size() == 3, "Ready replay remains idempotent");
        check(lobby.queue(id(2), p.revision()) != null, "Member cannot submit group");
        long readyRevision = p.revision();
        check(lobby.queue(id(1), readyRevision) == null, "Leader explicitly queues complete roster");
        check(lobby.queue(id(1), readyRevision) != null, "Stale queue replay rejected");
        check(p.phase() == D1PartyLobby.Phase.QUEUED && p.members().size() == 3, "Queue holds exact complete roster");
        lobby.startCountdown(p, 100, 100);
        check(p.countdownEnd() == 200 && !lobby.prepare(p, 199), "No preparation before countdown ends");
        lobby.waitForSlot(p);
        check(p.countdownEnd() == -1 && p.phase() == D1PartyLobby.Phase.QUEUED, "Full instance pool waits without losing queue");
        lobby.startCountdown(p, 500, 100);
        check(lobby.prepare(p, 600), "Available head roster enters preparation");
        check(lobby.remove(id(2)).isEmpty() && p.members().size() == 3, "Preparing roster is locked");
        check(lobby.invite(id(1), id(4), A, 600, 120, 6) != null, "Preparing leader cannot add members");
        lobby.complete(p);
        check(lobby.party(id(1)) == null && lobby.party(id(2)) == null && lobby.queued().isEmpty(), "Successful entry consumes lobby only");

        lobby = group(10, 3); p = lobby.party(id(10));
        readyAll(lobby, id(10)); long stale = p.revision(); lobby.cancel(p);
        check(p.ready().isEmpty() && p.classes().isEmpty() && p.countdownEnd() == -1, "Withdrawal clears all confirmations/classes/countdown");
        check(lobby.ready(id(11), stale) != null && lobby.queue(id(10), stale) != null, "Old ready and queue packets cannot revive cancelled check");
        readyAll(lobby, id(10));
        join(lobby, id(10), id(13), 6);
        check(p.phase() == D1PartyLobby.Phase.ASSEMBLY && p.ready().isEmpty(), "Joining clears everyone's readiness");
        readyAll(lobby, id(10)); lobby.queue(id(10), p.revision()); lobby.remove(id(11));
        check(p.phase() == D1PartyLobby.Phase.ASSEMBLY && p.ready().isEmpty() && !p.members().contains(id(11)), "Member departure cancels queued preparation");
        lobby.remove(id(10));
        check(lobby.party(id(12)) == null && lobby.party(id(13)) == null, "Leader departure disbands rather than silently transferring leadership");

        lobby = group(20, 3); p = lobby.party(id(20));
        check(lobby.invite(id(20), id(23), A, 0, 12000, 6) == null, "Invite before queue");
        var token = lobby.invitation(id(23)).token();
        lobby.accept(id(23), token, 1);
        readyAll(lobby, id(20)); lobby.queue(id(20), p.revision());
        lobby.joinAccepted(id(23), 2, 6, true);
        check(p.members().size() == 4 && p.phase() == D1PartyLobby.Phase.ASSEMBLY && p.ready().isEmpty(),
                "Onboarding completed while queued invalidates entire ready check");

        lobby = group(30, 3);
        check(lobby.invite(id(30), id(36), A, 0, 12000, 6) == null, "Pending first-time invite");
        token = lobby.invitation(id(36)).token(); lobby.accept(id(36), token, 1);
        for (int n=33;n<=35;n++) join(lobby,id(30),id(n),6);
        check(lobby.joinAccepted(id(36),2,6,false) != null && lobby.party(id(36)) == null && lobby.invitation(id(36)) == null,
                "A full group closes the accepted invitation even before onboarding finishes");
        check(lobby.invite(id(30), id(37), A, 3, 120, 6) != null, "Full six-player roster rejects invitations");
        check(lobby.invite(id(30), id(37), A, 3, 120, 3) != null, "Existing selector capacity is respected");

        lobby = group(40, 3);
        for (int n=51;n<=52;n++) join(lobby,id(50),id(n),6);
        readyAll(lobby,id(40));readyAll(lobby,id(50));
        var first=lobby.party(id(50)); var second=lobby.party(id(40));
        lobby.queue(id(50),first.revision());lobby.queue(id(40),second.revision());
        check(lobby.queued().equals(List.of(first,second)), "FIFO follows explicit submission, not leader or click order");
        check(lobby.queuePosition(first)==1 && lobby.queuePosition(second)==2, "Queue positions are consistent");
        lobby.complete(first);
        check(lobby.queuePosition(second)==1 && second.members().size()==3, "Completing one party never clears another at the same selector");
        lobby.cancel(second);
        check(lobby.queued().isEmpty() && lobby.party(id(40))==second, "Cancelled queue keeps remaining assembled group");

        lobby = new D1PartyLobby();
        lobby.invite(id(70),id(71),A,0,100,6);token=lobby.invitation(id(71)).token();
        check(lobby.accept(id(71),token,100)!=null && lobby.invitation(id(71))==null, "Expiry boundary rejects acceptance");
        lobby.invite(id(70),id(71),A,0,100,6);
        check(!lobby.decline(id(71),"wrong") && lobby.invitation(id(71))!=null, "Wrong decline token preserves invite");
        check(lobby.decline(id(71),lobby.invitation(id(71)).token()), "Recipient can decline");
        lobby.invite(id(70),id(71),A,0,100,6);lobby.expire(100);
        check(lobby.invitations().isEmpty(), "Expiry prunes unattended invitations");
        lobby.invite(id(70),id(71),A,0,100,6);lobby.remove(id(70));
        check(lobby.invitations().isEmpty(), "Disconnect clears sender's invitations");
        lobby=group(80,3);lobby.clear();
        check(lobby.parties().isEmpty() && lobby.invitations().isEmpty() && lobby.party(id(80))==null, "Server-stop clears transient state");

        for (int count=3;count<=6;count++) {
            var plan=DungeonStartupSchematicPlan.buildPlan(java.util.Collections.nCopies(count,"bogatyr"));
            check(plan.requests().size()==36 && plan.normalizedClassSlots().size()==6, "Variable party preserves authored six-slot paste plan");
            check(plan.normalizedClassSlots().stream().filter("blankslot"::equals).count()==6-count, "Unused class slots stay blank");
        }
        var action=new PartyPayloads.Action(7,42,"accept","00000000-0000-0000-0000-000000000001");
        var buffer=io.netty.buffer.Unpooled.buffer();
        try {
            PartyPayloads.Action.STREAM_CODEC.encode(buffer,action);
            check(action.equals(PartyPayloads.Action.STREAM_CODEC.decode(buffer)), "Action codec retains container/revision/token");
        } finally {buffer.release();}
        var view=new PartyPayloads.View(7,new PartyPayloads.State(42,"QUEUED",true,6,2,-1),
                List.of(new PartyPayloads.Member("Leader","bogatyr",true,true)),new PartyPayloads.Invite("","",false,false));
        buffer=io.netty.buffer.Unpooled.buffer();
        try {
            PartyPayloads.View.STREAM_CODEC.encode(buffer,view);
            check(view.equals(PartyPayloads.View.STREAM_CODEC.decode(buffer)), "Roster codec roundtrip preserves state");
        } finally {buffer.release();}
        var oversized = new PartyPayloads.View(7, view.state(),
                java.util.Collections.nCopies(7, new PartyPayloads.Member("Player","bogatyr",false,false)), view.invitation());
        buffer = io.netty.buffer.Unpooled.buffer();
        boolean rejected = false;
        try { PartyPayloads.View.STREAM_CODEC.encode(buffer, oversized); }
        catch (RuntimeException expected) { rejected = true; }
        finally { buffer.release(); }
        check(rejected, "Roster codec rejects a seventh member");
        // An accepted invitation must not merge groups if its original sender joins another party.
        lobby = new D1PartyLobby();
        lobby.invite(id(90),id(91),A,0,12000,6);
        token = lobby.invitation(id(91)).token(); lobby.accept(id(91),token,1);
        join(lobby,id(92),id(90),6);
        check(lobby.joinAccepted(id(91),2,6,false) != null && lobby.party(id(91)) == null,
                "Sender becoming a member cancels the old pending invitation without a merge");
        System.out.println(checks+" D1 party invitation/readiness/queue checks passed");
    }
}
