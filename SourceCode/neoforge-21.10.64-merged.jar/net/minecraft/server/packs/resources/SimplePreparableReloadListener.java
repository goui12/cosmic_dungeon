package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class SimplePreparableReloadListener<T> extends net.neoforged.neoforge.resource.ContextAwareReloadListener implements PreparableReloadListener {
    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState, Executor exectutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExectutor
    ) {
        ResourceManager resourcemanager = sharedState.resourceManager();
        return CompletableFuture.<T>supplyAsync(() -> this.prepare(resourcemanager, Profiler.get()), exectutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(p_372693_ -> this.apply((T)p_372693_, resourcemanager, Profiler.get()), applyExectutor);
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected abstract T prepare(ResourceManager resourceManager, ProfilerFiller profiler);

    protected abstract void apply(T object, ResourceManager resourceManager, ProfilerFiller profiler);
}
