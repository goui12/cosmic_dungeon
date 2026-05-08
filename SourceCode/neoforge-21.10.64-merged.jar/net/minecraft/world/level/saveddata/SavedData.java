package net.minecraft.world.level.saveddata;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

public abstract class SavedData {
    private boolean dirty;

    public void setDirty() {
        this.setDirty(true);
    }

    /**
     * Sets the dirty state of this {@code SavedData}, whether it needs saving to disk.
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public record Context(@Nullable ServerLevel level, long worldSeed) {
        public Context(ServerLevel p_401401_) {
            this(p_401401_, p_401401_.getSeed());
        }

        public ServerLevel levelOrThrow() {
            return Objects.requireNonNull(this.level);
        }
    }
}
