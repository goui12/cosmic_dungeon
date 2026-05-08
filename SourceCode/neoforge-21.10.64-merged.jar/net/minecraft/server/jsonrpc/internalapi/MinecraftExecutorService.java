package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MinecraftExecutorService {
    <V> CompletableFuture<V> submit(Supplier<V> task);

    CompletableFuture<Void> submit(Runnable task);
}
