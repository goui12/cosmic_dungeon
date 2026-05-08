package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executor;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TelemetryEventLog implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final JsonEventLog<TelemetryEventInstance> log;
    private final ConsecutiveExecutor consecutiveExecutor;

    public TelemetryEventLog(FileChannel channel, Executor dispatcher) {
        this.log = new JsonEventLog<>(TelemetryEventInstance.CODEC, channel);
        this.consecutiveExecutor = new ConsecutiveExecutor(dispatcher, "telemetry-event-log");
    }

    public TelemetryEventLogger logger() {
        return p_370368_ -> this.consecutiveExecutor.schedule(() -> {
            try {
                this.log.write(p_370368_);
            } catch (IOException ioexception) {
                LOGGER.error("Failed to write telemetry event to log", (Throwable)ioexception);
            }
        });
    }

    @Override
    public void close() {
        this.consecutiveExecutor.schedule(() -> IOUtils.closeQuietly(this.log));
        this.consecutiveExecutor.close();
    }
}
