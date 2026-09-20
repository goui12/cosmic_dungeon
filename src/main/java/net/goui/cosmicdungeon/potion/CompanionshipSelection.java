package net.goui.cosmicdungeon.potion;

/** Server-clock, run-bound capability; never serialized across logout/restart. */
public record CompanionshipSelection(long runId, long issued, long expires) {
    public static CompanionshipSelection create(long run, long now, int duration) {
        if (run <= 0 || now < 0 || duration <= 0 || now > Long.MAX_VALUE - duration)
            throw new IllegalArgumentException("Invalid companion selection");
        return new CompanionshipSelection(run, now, now + duration);
    }
    public boolean valid(long currentRun, long now) {
        return runId > 0 && currentRun == runId && issued >= 0 && expires > issued && now >= issued && now < expires;
    }
}
