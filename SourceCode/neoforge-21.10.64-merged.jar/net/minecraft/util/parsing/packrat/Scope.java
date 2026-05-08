package net.minecraft.util.parsing.packrat;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;

public final class Scope {
    private static final int NOT_FOUND = -1;
    private static final Object FRAME_START_MARKER = new Object() {
        @Override
        public String toString() {
            return "frame";
        }
    };
    private static final int ENTRY_STRIDE = 2;
    private Object[] stack = new Object[128];
    private int topEntryKeyIndex = 0;
    private int topMarkerKeyIndex = 0;

    public Scope() {
        this.stack[0] = FRAME_START_MARKER;
        this.stack[1] = null;
    }

    private int valueIndex(Atom<?> name) {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
            Object object = this.stack[i];

            assert object instanceof Atom;

            if (object == name) {
                return i + 1;
            }
        }

        return -1;
    }

    public int valueIndexForAny(Atom<?>... names) {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
            Object object = this.stack[i];

            assert object instanceof Atom;

            for (Atom<?> atom : names) {
                if (atom == object) {
                    return i + 1;
                }
            }
        }

        return -1;
    }

    private void ensureCapacity(int requiredCapacity) {
        int i = this.stack.length;
        int j = this.topEntryKeyIndex + 1;
        int k = j + requiredCapacity * 2;
        if (k >= i) {
            int l = Util.growByHalf(i, k + 1);
            Object[] aobject = new Object[l];
            System.arraycopy(this.stack, 0, aobject, 0, i);
            this.stack = aobject;
        }

        assert this.validateStructure();
    }

    private void setupNewFrame() {
        this.topEntryKeyIndex += 2;
        this.stack[this.topEntryKeyIndex] = FRAME_START_MARKER;
        this.stack[this.topEntryKeyIndex + 1] = this.topMarkerKeyIndex;
        this.topMarkerKeyIndex = this.topEntryKeyIndex;
    }

    public void pushFrame() {
        this.ensureCapacity(1);
        this.setupNewFrame();

        assert this.validateStructure();
    }

    private int getPreviousMarkerIndex(int markerIndex) {
        return (Integer)this.stack[markerIndex + 1];
    }

    public void popFrame() {
        assert this.topMarkerKeyIndex != 0;

        this.topEntryKeyIndex = this.topMarkerKeyIndex - 2;
        this.topMarkerKeyIndex = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);

        assert this.validateStructure();
    }

    public void splitFrame() {
        int i = this.topMarkerKeyIndex;
        int j = (this.topEntryKeyIndex - this.topMarkerKeyIndex) / 2;
        this.ensureCapacity(j + 1);
        this.setupNewFrame();
        int k = i + 2;
        int l = this.topEntryKeyIndex;

        for (int i1 = 0; i1 < j; i1++) {
            l += 2;
            Object object = this.stack[k];

            assert object != null;

            this.stack[l] = object;
            this.stack[l + 1] = null;
            k += 2;
        }

        this.topEntryKeyIndex = l;

        assert this.validateStructure();
    }

    public void clearFrameValues() {
        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
            assert this.stack[i] instanceof Atom;

            this.stack[i + 1] = null;
        }

        assert this.validateStructure();
    }

    public void mergeFrame() {
        int i = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);
        int j = i;
        int k = this.topMarkerKeyIndex;

        while (k < this.topEntryKeyIndex) {
            j += 2;
            k += 2;
            Object object = this.stack[k];

            assert object instanceof Atom;

            Object object1 = this.stack[k + 1];
            Object object2 = this.stack[j];
            if (object2 != object) {
                this.stack[j] = object;
                this.stack[j + 1] = object1;
            } else if (object1 != null) {
                this.stack[j + 1] = object1;
            }
        }

        this.topEntryKeyIndex = j;
        this.topMarkerKeyIndex = i;

        assert this.validateStructure();
    }

    public <T> void put(Atom<T> atom, @Nullable T value) {
        int i = this.valueIndex(atom);
        if (i != -1) {
            this.stack[i] = value;
        } else {
            this.ensureCapacity(1);
            this.topEntryKeyIndex += 2;
            this.stack[this.topEntryKeyIndex] = atom;
            this.stack[this.topEntryKeyIndex + 1] = value;
        }

        assert this.validateStructure();
    }

    @Nullable
    public <T> T get(Atom<T> atom) {
        int i = this.valueIndex(atom);
        return (T)(i != -1 ? this.stack[i] : null);
    }

    public <T> T getOrThrow(Atom<T> atom) {
        int i = this.valueIndex(atom);
        if (i == -1) {
            throw new IllegalArgumentException("No value for atom " + atom);
        } else {
            return (T)this.stack[i];
        }
    }

    public <T> T getOrDefault(Atom<T> atom, T defaultValue) {
        int i = this.valueIndex(atom);
        return (T)(i != -1 ? this.stack[i] : defaultValue);
    }

    @Nullable
    @SafeVarargs
    public final <T> T getAny(Atom<? extends T>... atoms) {
        int i = this.valueIndexForAny(atoms);
        return (T)(i != -1 ? this.stack[i] : null);
    }

    @SafeVarargs
    public final <T> T getAnyOrThrow(Atom<? extends T>... atoms) {
        int i = this.valueIndexForAny(atoms);
        if (i == -1) {
            throw new IllegalArgumentException("No value for atoms " + Arrays.toString((Object[])atoms));
        } else {
            return (T)this.stack[i];
        }
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder();
        boolean flag = true;

        for (int i = 0; i <= this.topEntryKeyIndex; i += 2) {
            Object object = this.stack[i];
            Object object1 = this.stack[i + 1];
            if (object == FRAME_START_MARKER) {
                stringbuilder.append('|');
                flag = true;
            } else {
                if (!flag) {
                    stringbuilder.append(',');
                }

                flag = false;
                stringbuilder.append(object).append(':').append(object1);
            }
        }

        return stringbuilder.toString();
    }

    @VisibleForTesting
    public Map<Atom<?>, ?> lastFrame() {
        HashMap<Atom<?>, Object> hashmap = new HashMap<>();

        for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
            Object object = this.stack[i];
            Object object1 = this.stack[i + 1];
            hashmap.put((Atom<?>)object, object1);
        }

        return hashmap;
    }

    public boolean hasOnlySingleFrame() {
        for (int i = this.topEntryKeyIndex; i > 0; i--) {
            if (this.stack[i] == FRAME_START_MARKER) {
                return false;
            }
        }

        if (this.stack[0] != FRAME_START_MARKER) {
            throw new IllegalStateException("Corrupted stack");
        } else {
            return true;
        }
    }

    private boolean validateStructure() {
        assert this.topMarkerKeyIndex >= 0;

        assert this.topEntryKeyIndex >= this.topMarkerKeyIndex;

        for (int i = 0; i <= this.topEntryKeyIndex; i += 2) {
            Object object = this.stack[i];
            if (object != FRAME_START_MARKER && !(object instanceof Atom)) {
                return false;
            }
        }

        for (int j = this.topMarkerKeyIndex; j != 0; j = this.getPreviousMarkerIndex(j)) {
            Object object1 = this.stack[j];
            if (object1 != FRAME_START_MARKER) {
                return false;
            }
        }

        return true;
    }
}
