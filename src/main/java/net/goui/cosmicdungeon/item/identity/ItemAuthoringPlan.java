package net.goui.cosmicdungeon.item.identity;

import java.util.Objects;
import java.util.function.BiPredicate;

/** A bounded, exact pre/post-image edit: stale actions never merge, guess identity, or undo
 * someone else's later changes. ItemStack copies are supplied by the server adapter. */
public final class ItemAuthoringPlan<T> {
    private final String token;
    private final long expires;
    private final T before, after;
    private boolean applied, finished;
    public ItemAuthoringPlan(String token, long expires, T before, T after) {
        this.token = Objects.requireNonNull(token); this.expires = expires;
        this.before = before; this.after = after;
    }
    public String token() { return token; }
    public boolean expired(long now) { return now >= expires; }
    public boolean applied() { return applied; }
    public T image(boolean undo) { return undo ? before : after; }
    public boolean accepts(String supplied, long now, boolean sameTarget, T actual,
                           boolean undo, BiPredicate<T,T> equals) {
        return !finished && token.equals(supplied) && !expired(now) && sameTarget && applied == undo
                && equals.test(actual, undo ? after : before);
    }
    public void committed(boolean undo) { applied = !undo; finished = undo; }
}
