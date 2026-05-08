package net.minecraft;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public record TracingExecutor(ExecutorService service) implements Executor {
    public Executor forName(String name) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            return p_372951_ -> this.service.execute(() -> {
                Thread thread = Thread.currentThread();
                String s = thread.getName();
                thread.setName(name);

                try (Zone zone = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE)) {
                    p_372951_.run();
                } finally {
                    thread.setName(s);
                }
            });
        } else {
            return (Executor)(TracyClient.isAvailable() ? (Executor) p_372837_ -> this.service.execute(() -> {
                try (Zone zone = TracyClient.beginZone(name, SharedConstants.IS_RUNNING_IN_IDE)) {
                    p_372837_.run();
                }
            }) : this.service);
        }
    }

    @Override
    public void execute(Runnable task) {
        this.service.execute(wrapUnnamed(task));
    }

    public void shutdownAndAwait(long timeout, TimeUnit unit) {
        this.service.shutdown();

        boolean flag;
        try {
            flag = this.service.awaitTermination(timeout, unit);
        } catch (InterruptedException interruptedexception) {
            flag = false;
        }

        if (!flag) {
            this.service.shutdownNow();
        }
    }

    private static Runnable wrapUnnamed(Runnable task) {
        return !TracyClient.isAvailable() ? task : () -> {
            try (Zone zone = TracyClient.beginZone("task", SharedConstants.IS_RUNNING_IN_IDE)) {
                task.run();
            }
        };
    }
}
