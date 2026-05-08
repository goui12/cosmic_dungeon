package net.minecraft.util.parsing.packrat;

import javax.annotation.Nullable;
import net.minecraft.Util;

public abstract class CachedParseState<S> implements ParseState<S> {
    private CachedParseState.PositionCache[] positionCache = new CachedParseState.PositionCache[256];
    private final ErrorCollector<S> errorCollector;
    private final Scope scope = new Scope();
    private CachedParseState.SimpleControl[] controlCache = new CachedParseState.SimpleControl[16];
    private int nextControlToReturn;
    private final CachedParseState<S>.Silent silent = new CachedParseState.Silent();

    protected CachedParseState(ErrorCollector<S> errorCollector) {
        this.errorCollector = errorCollector;
    }

    @Override
    public Scope scope() {
        return this.scope;
    }

    @Override
    public ErrorCollector<S> errorCollector() {
        return this.errorCollector;
    }

    @Nullable
    @Override
    public <T> T parse(NamedRule<S, T> rule) {
        int i = this.mark();
        CachedParseState.PositionCache cachedparsestate$positioncache = this.getCacheForPosition(i);
        int j = cachedparsestate$positioncache.findKeyIndex(rule.name());
        if (j != -1) {
            CachedParseState.CacheEntry<T> cacheentry = cachedparsestate$positioncache.getValue(j);
            if (cacheentry != null) {
                if (cacheentry == CachedParseState.CacheEntry.NEGATIVE) {
                    return null;
                }

                this.restore(cacheentry.markAfterParse);
                return cacheentry.value;
            }
        } else {
            j = cachedparsestate$positioncache.allocateNewEntry(rule.name());
        }

        T t = rule.value().parse(this);
        CachedParseState.CacheEntry<T> cacheentry1;
        if (t == null) {
            cacheentry1 = CachedParseState.CacheEntry.negativeEntry();
        } else {
            int k = this.mark();
            cacheentry1 = new CachedParseState.CacheEntry<>(t, k);
        }

        cachedparsestate$positioncache.setValue(j, cacheentry1);
        return t;
    }

    private CachedParseState.PositionCache getCacheForPosition(int position) {
        int i = this.positionCache.length;
        if (position >= i) {
            int j = Util.growByHalf(i, position + 1);
            CachedParseState.PositionCache[] acachedparsestate$positioncache = new CachedParseState.PositionCache[j];
            System.arraycopy(this.positionCache, 0, acachedparsestate$positioncache, 0, i);
            this.positionCache = acachedparsestate$positioncache;
        }

        CachedParseState.PositionCache cachedparsestate$positioncache = this.positionCache[position];
        if (cachedparsestate$positioncache == null) {
            cachedparsestate$positioncache = new CachedParseState.PositionCache();
            this.positionCache[position] = cachedparsestate$positioncache;
        }

        return cachedparsestate$positioncache;
    }

    @Override
    public Control acquireControl() {
        int i = this.controlCache.length;
        if (this.nextControlToReturn >= i) {
            int j = Util.growByHalf(i, this.nextControlToReturn + 1);
            CachedParseState.SimpleControl[] acachedparsestate$simplecontrol = new CachedParseState.SimpleControl[j];
            System.arraycopy(this.controlCache, 0, acachedparsestate$simplecontrol, 0, i);
            this.controlCache = acachedparsestate$simplecontrol;
        }

        int k = this.nextControlToReturn++;
        CachedParseState.SimpleControl cachedparsestate$simplecontrol = this.controlCache[k];
        if (cachedparsestate$simplecontrol == null) {
            cachedparsestate$simplecontrol = new CachedParseState.SimpleControl();
            this.controlCache[k] = cachedparsestate$simplecontrol;
        } else {
            cachedparsestate$simplecontrol.reset();
        }

        return cachedparsestate$simplecontrol;
    }

    @Override
    public void releaseControl() {
        this.nextControlToReturn--;
    }

    @Override
    public ParseState<S> silent() {
        return this.silent;
    }

    record CacheEntry<T>(@Nullable T value, int markAfterParse) {
        public static final CachedParseState.CacheEntry<?> NEGATIVE = new CachedParseState.CacheEntry(null, -1);

        public static <T> CachedParseState.CacheEntry<T> negativeEntry() {
            return (CachedParseState.CacheEntry<T>)NEGATIVE;
        }
    }

    static class PositionCache {
        public static final int ENTRY_STRIDE = 2;
        private static final int NOT_FOUND = -1;
        private Object[] atomCache = new Object[16];
        private int nextKey;

        public int findKeyIndex(Atom<?> atom) {
            for (int i = 0; i < this.nextKey; i += 2) {
                if (this.atomCache[i] == atom) {
                    return i;
                }
            }

            return -1;
        }

        public int allocateNewEntry(Atom<?> entry) {
            int i = this.nextKey;
            this.nextKey += 2;
            int j = i + 1;
            int k = this.atomCache.length;
            if (j >= k) {
                int l = Util.growByHalf(k, j + 1);
                Object[] aobject = new Object[l];
                System.arraycopy(this.atomCache, 0, aobject, 0, k);
                this.atomCache = aobject;
            }

            this.atomCache[i] = entry;
            return i;
        }

        @Nullable
        public <T> CachedParseState.CacheEntry<T> getValue(int index) {
            return (CachedParseState.CacheEntry<T>)this.atomCache[index + 1];
        }

        public void setValue(int index, CachedParseState.CacheEntry<?> value) {
            this.atomCache[index + 1] = value;
        }
    }

    class Silent implements ParseState<S> {
        private final ErrorCollector<S> silentCollector = new ErrorCollector.Nop<>();

        @Override
        public ErrorCollector<S> errorCollector() {
            return this.silentCollector;
        }

        @Override
        public Scope scope() {
            return CachedParseState.this.scope();
        }

        @Nullable
        @Override
        public <T> T parse(NamedRule<S, T> p_410584_) {
            return CachedParseState.this.parse(p_410584_);
        }

        @Override
        public S input() {
            return CachedParseState.this.input();
        }

        @Override
        public int mark() {
            return CachedParseState.this.mark();
        }

        @Override
        public void restore(int p_410357_) {
            CachedParseState.this.restore(p_410357_);
        }

        @Override
        public Control acquireControl() {
            return CachedParseState.this.acquireControl();
        }

        @Override
        public void releaseControl() {
            CachedParseState.this.releaseControl();
        }

        @Override
        public ParseState<S> silent() {
            return this;
        }
    }

    static class SimpleControl implements Control {
        private boolean hasCut;

        @Override
        public void cut() {
            this.hasCut = true;
        }

        @Override
        public boolean hasCut() {
            return this.hasCut;
        }

        public void reset() {
            this.hasCut = false;
        }
    }
}
