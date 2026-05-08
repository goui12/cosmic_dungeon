package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public interface ResourceManagerReloadListener extends PreparableReloadListener {
    @Override
    default CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState, Executor exectutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExectutor
    ) {
        ResourceManager resourcemanager = sharedState.resourceManager();
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("listener");
            this.onResourceManagerReload(resourcemanager);
            profilerfiller.pop();
        }, applyExectutor);
    }

    void onResourceManagerReload(ResourceManager resourceManager);
}
