package net.goui.cosmicdungeon.client.screen;

import java.util.UUID;
import net.goui.cosmicdungeon.network.PartyInspectionPayloads;

/** One hovered identity, no cache of former members, at most one request per second across row changes. */
public final class PartyInspectionHoverState {
    private long run, sequence, firstSequence, requested, accepted, clock, nextRequest, received;
    private UUID subject;
    private PartyInspectionPayloads.View view;

    public void hover(long newRun, UUID newSubject) {
        if (run == newRun && java.util.Objects.equals(subject, newSubject)) return;
        run = newRun;
        subject = newSubject;
        firstSequence = sequence + 1;
        requested = accepted = 0;
        view = null;
    }
    public void clear() { hover(0, null); }
    public PartyInspectionPayloads.Request tick() {
        clock++;
        if (view != null && clock - received > 40) view = null;
        if (subject == null || run <= 0 || clock < nextRequest) return null;
        nextRequest = clock + 20;
        requested = ++sequence;
        return new PartyInspectionPayloads.Request(run, requested, subject);
    }
    public void receive(PartyInspectionPayloads.View payload) {
        var request = payload.request();
        if (subject == null || request.run() != run || !subject.equals(request.subject())
                || request.sequence() < firstSequence || request.sequence() > requested
                || request.sequence() <= accepted) return;
        accepted = request.sequence();
        view = payload;
        received = clock;
    }
    public PartyInspectionPayloads.View view(String memberId) {
        return subject != null && subject.toString().equals(memberId) ? view : null;
    }
}
