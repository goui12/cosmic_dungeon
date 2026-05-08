package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface TaskScheduler<R extends Runnable> extends AutoCloseable {
    String name();

    void schedule(R task);

    @Override
    default void close() {
    }

    R wrapRunnable(Runnable runnable);

    default <Source> CompletableFuture<Source> scheduleWithResult(Consumer<CompletableFuture<Source>> resultConsumer) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<>();
        this.schedule(this.wrapRunnable(() -> resultConsumer.accept(completablefuture)));
        return completablefuture;
    }

    static TaskScheduler<Runnable> wrapExecutor(final String name, final Executor executor) {
        return new TaskScheduler<Runnable>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void schedule(Runnable p_371439_) {
                executor.execute(p_371439_);
            }

            @Override
            public Runnable wrapRunnable(Runnable p_371242_) {
                return p_371242_;
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
