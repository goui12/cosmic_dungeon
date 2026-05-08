package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.Unit;

public class SimpleReloadInstance<S> implements ReloadInstance {
    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
    @Nullable
    private CompletableFuture<List<S>> allDone;
    final Set<PreparableReloadListener> preparingListeners;
    private final int listenerCount;
    private final AtomicInteger startedTasks = new AtomicInteger();
    private final AtomicInteger finishedTasks = new AtomicInteger();
    private final AtomicInteger startedReloads = new AtomicInteger();
    private final AtomicInteger finishedReloads = new AtomicInteger();

    public static ReloadInstance of(
        ResourceManager resourceManager, List<PreparableReloadListener> listeners, Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> alsoWaitedFor
    ) {
        SimpleReloadInstance<Void> simplereloadinstance = new SimpleReloadInstance<>(listeners);
        simplereloadinstance.startTasks(backgroundExecutor, gameExecutor, resourceManager, listeners, SimpleReloadInstance.StateFactory.SIMPLE, alsoWaitedFor);
        return simplereloadinstance;
    }

    protected SimpleReloadInstance(List<PreparableReloadListener> preparingListeners) {
        this.listenerCount = preparingListeners.size();
        this.preparingListeners = new HashSet<>(preparingListeners);
    }

    protected void startTasks(
        Executor backgroundExecutor,
        Executor gameExecutor,
        ResourceManager resourceManager,
        List<PreparableReloadListener> listeners,
        SimpleReloadInstance.StateFactory<S> stateFactory,
        CompletableFuture<?> alsoWaitedFor
    ) {
        this.allDone = this.prepareTasks(backgroundExecutor, gameExecutor, resourceManager, listeners, stateFactory, alsoWaitedFor);
    }

    protected CompletableFuture<List<S>> prepareTasks(
        Executor backgroundExecutor,
        Executor gameExecutor,
        ResourceManager resourceManager,
        List<PreparableReloadListener> listeners,
        SimpleReloadInstance.StateFactory<S> stateFactory,
        CompletableFuture<?> alsoWaitedFor
    ) {
        Executor executor = p_404224_ -> {
            this.startedTasks.incrementAndGet();
            backgroundExecutor.execute(() -> {
                p_404224_.run();
                this.finishedTasks.incrementAndGet();
            });
        };
        Executor executor1 = p_404227_ -> {
            this.startedReloads.incrementAndGet();
            gameExecutor.execute(() -> {
                p_404227_.run();
                this.finishedReloads.incrementAndGet();
            });
        };
        this.startedTasks.incrementAndGet();
        alsoWaitedFor.thenRun(this.finishedTasks::incrementAndGet);
        PreparableReloadListener.SharedState preparablereloadlistener$sharedstate = new PreparableReloadListener.SharedState(resourceManager);
        listeners.forEach(p_432493_ -> p_432493_.prepareSharedState(preparablereloadlistener$sharedstate));
        CompletableFuture<?> completablefuture = alsoWaitedFor;
        List<CompletableFuture<S>> list = new ArrayList<>();

        for (PreparableReloadListener preparablereloadlistener : listeners) {
            PreparableReloadListener.PreparationBarrier preparablereloadlistener$preparationbarrier = this.createBarrierForListener(
                preparablereloadlistener, completablefuture, gameExecutor
            );
            CompletableFuture<S> completablefuture1 = stateFactory.create(
                preparablereloadlistener$sharedstate, preparablereloadlistener$preparationbarrier, preparablereloadlistener, executor, executor1
            );
            list.add(completablefuture1);
            completablefuture = completablefuture1;
        }

        return Util.sequenceFailFast(list);
    }

    private PreparableReloadListener.PreparationBarrier createBarrierForListener(
        final PreparableReloadListener listener, final CompletableFuture<?> alsoWaitedFor, final Executor executor
    ) {
        return new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T p_10858_) {
                executor.execute(() -> {
                    SimpleReloadInstance.this.preparingListeners.remove(listener);
                    if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                        SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                    }
                });
                return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)alsoWaitedFor, (p_10861_, p_10862_) -> p_10858_);
            }
        };
    }

    @Override
    public CompletableFuture<?> done() {
        return Objects.requireNonNull(this.allDone, "not started");
    }

    @Override
    public float getActualProgress() {
        int i = this.listenerCount - this.preparingListeners.size();
        float f = weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), i);
        float f1 = weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);
        return f / f1;
    }

    private static int weightProgress(int tasks, int reloads, int listeners) {
        return tasks * 2 + reloads * 2 + listeners * 1;
    }

    public static ReloadInstance create(
        ResourceManager resourceManager,
        List<PreparableReloadListener> listeners,
        Executor backgroundExecutor,
        Executor gameExecutor,
        CompletableFuture<Unit> alsoWaitedFor,
        boolean profiled
    ) {
        return profiled
            ? ProfiledReloadInstance.of(resourceManager, listeners, backgroundExecutor, gameExecutor, alsoWaitedFor)
            : of(resourceManager, listeners, backgroundExecutor, gameExecutor, alsoWaitedFor);
    }

    @FunctionalInterface
    protected interface StateFactory<S> {
        SimpleReloadInstance.StateFactory<Void> SIMPLE = (p_432494_, p_432495_, p_432496_, p_432497_, p_432498_) -> p_432496_.reload(
            p_432494_, p_432497_, p_432495_, p_432498_
        );

        CompletableFuture<S> create(
            PreparableReloadListener.SharedState sharedState,
            PreparableReloadListener.PreparationBarrier barrier,
            PreparableReloadListener listener,
            Executor executor,
            Executor applyExecutor
        );
    }
}
