package net.minecraft.util.profiling;

import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Zone implements AutoCloseable {
    public static final Zone INACTIVE = new Zone(null);
    @Nullable
    private final ProfilerFiller profiler;

    Zone(@Nullable ProfilerFiller profiler) {
        this.profiler = profiler;
    }

    public Zone addText(String text) {
        if (this.profiler != null) {
            this.profiler.addZoneText(text);
        }

        return this;
    }

    public Zone addText(Supplier<String> text) {
        if (this.profiler != null) {
            this.profiler.addZoneText(text.get());
        }

        return this;
    }

    public Zone addValue(long value) {
        if (this.profiler != null) {
            this.profiler.addZoneValue(value);
        }

        return this;
    }

    public Zone setColor(int color) {
        if (this.profiler != null) {
            this.profiler.setZoneColor(color);
        }

        return this;
    }

    @Override
    public void close() {
        if (this.profiler != null) {
            this.profiler.pop();
        }
    }
}
