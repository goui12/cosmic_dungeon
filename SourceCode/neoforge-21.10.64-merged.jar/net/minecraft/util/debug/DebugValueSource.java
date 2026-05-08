package net.minecraft.util.debug;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

public interface DebugValueSource {
    void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration);

    public interface Registration {
        <T> void register(DebugSubscription<T> subscription, DebugValueSource.ValueGetter<T> getter);
    }

    public interface ValueGetter<T> {
        @Nullable
        T get();
    }
}
