package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;

public record PendingRpcRequest<Result>(
    Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime
) {
    public void accept(JsonElement json) {
        try {
            Result result = (Result)this.method.value().decodeResult(json);
            this.resultFuture.complete(Objects.requireNonNull(result));
        } catch (Exception exception) {
            this.resultFuture.completeExceptionally(exception);
        }
    }

    public boolean timedOut(long time) {
        return time > this.timeoutTime;
    }
}
