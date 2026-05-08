package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import org.slf4j.Logger;

public class ReloadableResourceManager implements ResourceManager, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CloseableResourceManager resources;
    private List<PreparableReloadListener> listeners = Lists.newArrayList();
    private final PackType type;

    public ReloadableResourceManager(PackType type) {
        this.type = type;
        this.resources = new MultiPackResourceManager(type, List.of());
    }

    @Override
    public void close() {
        this.resources.close();
    }

    /**
     * @deprecated Neo: Use {@link
     *             net.neoforged.neoforge.client.event.AddClientReloadListenerEvent}.
     * @throws UnsupportedOperationException if called after the event has been fired.
     */
    @Deprecated
    public void registerReloadListener(PreparableReloadListener listener) {
        this.listeners.add(listener);
    }

    public ReloadInstance createReload(Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> waitingFor, List<PackResources> resourcePacks) {
        LOGGER.info("Reloading ResourceManager: {}", LogUtils.defer(() -> resourcePacks.stream().map(PackResources::packId).collect(Collectors.joining(", "))));
        this.resources.close();
        this.resources = new MultiPackResourceManager(this.type, resourcePacks);
        return SimpleReloadInstance.create(this.resources, this.listeners, backgroundExecutor, gameExecutor, waitingFor, LOGGER.isDebugEnabled());
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        return this.resources.getResource(location);
    }

    @Override
    public Set<String> getNamespaces() {
        return this.resources.getNamespaces();
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation location) {
        return this.resources.getResourceStack(location);
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
        return this.resources.listResources(path, filter);
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
        return this.resources.listResourceStacks(path, filter);
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.resources.listPacks();
    }

    /**
     * Neo: Expose the reload listeners so they can be passed to the event.
     *
     * @return The (immutable) list of reload listeners.
     */
    public List<PreparableReloadListener> getListeners() {
        return this.listeners;
    }

    /**
     * Neo: Updates the {@link #listeners} with the sorted list from the event.
     *
     * @implNote The returned list is immutable, so after this method is called, {@link #registerReloadListener(PreparableReloadListener)} will throw.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public void updateListenersFrom(net.neoforged.neoforge.event.SortedReloadListenerEvent event) {
        this.listeners = net.neoforged.neoforge.resource.ReloadListenerSort.sort(event);
    }
}
