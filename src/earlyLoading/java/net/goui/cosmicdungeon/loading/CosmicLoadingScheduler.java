package net.goui.cosmicdungeon.loading;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Decorates construction tasks without changing NeoForge's thread or frame scheduling. */
final class CosmicLoadingScheduler extends AbstractExecutorService implements ScheduledExecutorService {
    private final ScheduledExecutorService delegate;
    private final Consumer<Object> afterConstruction;

    CosmicLoadingScheduler(ScheduledExecutorService delegate, Consumer<Object> afterConstruction) {
        this.delegate = delegate;
        this.afterConstruction = afterConstruction;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate.schedule(() -> {
            V result = callable.call();
            afterConstruction.accept(result);
            return result;
        }, delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(command, delay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(command, delay, period, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
