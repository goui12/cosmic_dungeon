package com.mojang.blaze3d.resource;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossFrameResourcePool implements GraphicsResourceAllocator, AutoCloseable {
    private final int framesToKeepResource;
    private final Deque<CrossFrameResourcePool.ResourceEntry<?>> pool = new ArrayDeque<>();

    public CrossFrameResourcePool(int framesToKeepResource) {
        this.framesToKeepResource = framesToKeepResource;
    }

    public void endFrame() {
        Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

        while (iterator.hasNext()) {
            CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
            if (resourceentry.framesToLive-- == 0) {
                resourceentry.close();
                iterator.remove();
            }
        }
    }

    @Override
    public <T> T acquire(ResourceDescriptor<T> descriptor) {
        T t = this.acquireWithoutPreparing(descriptor);
        descriptor.prepare(t);
        return t;
    }

    private <T> T acquireWithoutPreparing(ResourceDescriptor<T> descriptor) {
        Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

        while (iterator.hasNext()) {
            CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
            if (descriptor.canUsePhysicalResource(resourceentry.descriptor)) {
                iterator.remove();
                return (T)resourceentry.value;
            }
        }

        return descriptor.allocate();
    }

    @Override
    public <T> void release(ResourceDescriptor<T> descriptor, T value) {
        this.pool.addFirst(new CrossFrameResourcePool.ResourceEntry<>(descriptor, value, this.framesToKeepResource));
    }

    public void clear() {
        this.pool.forEach(CrossFrameResourcePool.ResourceEntry::close);
        this.pool.clear();
    }

    @Override
    public void close() {
        this.clear();
    }

    @VisibleForTesting
    protected Collection<CrossFrameResourcePool.ResourceEntry<?>> entries() {
        return this.pool;
    }

    @OnlyIn(Dist.CLIENT)
    @VisibleForTesting
    protected static final class ResourceEntry<T> implements AutoCloseable {
        final ResourceDescriptor<T> descriptor;
        final T value;
        int framesToLive;

        ResourceEntry(ResourceDescriptor<T> descriptor, T value, int framesToLive) {
            this.descriptor = descriptor;
            this.value = value;
            this.framesToLive = framesToLive;
        }

        @Override
        public void close() {
            this.descriptor.free(this.value);
        }
    }
}
