package net.minecraft.server.jsonrpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidRequestJsonRpcException;
import net.minecraft.server.jsonrpc.methods.MethodNotFoundJsonRpcException;
import net.minecraft.server.jsonrpc.methods.RemoteRpcErrorException;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

public class Connection extends SimpleChannelInboundHandler<JsonElement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger CONNECTION_ID_COUNTER = new AtomicInteger(0);
    private final JsonRpcLogger jsonRpcLogger;
    private final ClientInfo clientInfo;
    private final ManagementServer managementServer;
    private final Channel channel;
    private final MinecraftApi minecraftApi;
    private final AtomicInteger transactionId = new AtomicInteger();
    private final Int2ObjectMap<PendingRpcRequest<?>> pendingRequests = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public Connection(Channel channel, ManagementServer managementServer, MinecraftApi minecraftApi, JsonRpcLogger jsonRpcLogger) {
        this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
        this.managementServer = managementServer;
        this.minecraftApi = minecraftApi;
        this.channel = channel;
        this.jsonRpcLogger = jsonRpcLogger;
    }

    public void tick() {
        long i = Util.getMillis();
        this.pendingRequests
            .int2ObjectEntrySet()
            .removeIf(
                p_450810_ -> {
                    boolean flag = p_450810_.getValue().timedOut(i);
                    if (flag) {
                        p_450810_.getValue()
                            .resultFuture()
                            .completeExceptionally(
                                new ReadTimeoutException("RPC method " + p_450810_.getValue().method().key().location() + " timed out waiting for response")
                            );
                    }

                    return flag;
                }
            );
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
        super.channelActive(context);
        this.managementServer.onConnected(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
        super.channelInactive(context);
        this.managementServer.onDisconnected(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable exception) throws Exception {
        if (exception.getCause() instanceof JsonParseException) {
            this.channel.writeAndFlush(JsonRPCErrors.PARSE_ERROR.createWithUnknownId(exception.getMessage()));
        } else {
            super.exceptionCaught(context, exception);
            this.channel.close().awaitUninterruptibly();
        }
    }

    protected void channelRead0(ChannelHandlerContext context, JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject jsonobject = this.handleJsonObject(json.getAsJsonObject());
            if (jsonobject != null) {
                this.channel.writeAndFlush(jsonobject);
            }
        } else if (json.isJsonArray()) {
            this.channel.writeAndFlush(this.handleBatchRequest(json.getAsJsonArray().asList()));
        } else {
            this.channel.writeAndFlush(JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
        }
    }

    private JsonArray handleBatchRequest(List<JsonElement> requests) {
        JsonArray jsonarray = new JsonArray();
        requests.stream().map(p_448829_ -> this.handleJsonObject(p_448829_.getAsJsonObject())).filter(Objects::nonNull).forEach(jsonarray::add);
        return jsonarray;
    }

    public void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
        this.sendRequest(method, null, false);
    }

    public <Params> void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, Params params) {
        this.sendRequest(method, params, false);
    }

    public <Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> method) {
        return this.sendRequest(method, null, true);
    }

    public <Params, Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> method, Params params) {
        return this.sendRequest(method, params, true);
    }

    @Nullable
    @Contract("_,_,false->null;_,_,true->!null")
    private <Params, Result> CompletableFuture<Result> sendRequest(
        Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> method, @Nullable Params params, boolean expectResponse
    ) {
        List<JsonElement> list = params != null ? List.of(Objects.requireNonNull(method.value().encodeParams(params))) : List.of();
        if (expectResponse) {
            CompletableFuture<Result> completablefuture = new CompletableFuture<>();
            int i = this.transactionId.incrementAndGet();
            long j = Util.timeSource.get(TimeUnit.MILLISECONDS);
            this.pendingRequests.put(i, new PendingRpcRequest<>(method, completablefuture, j + 5000L));
            this.channel.writeAndFlush(JsonRPCUtils.createRequest(i, method.key().location(), list));
            return completablefuture;
        } else {
            this.channel.writeAndFlush(JsonRPCUtils.createRequest(null, method.key().location(), list));
            return null;
        }
    }

    @Nullable
    @VisibleForTesting
    JsonObject handleJsonObject(JsonObject json) {
        try {
            JsonElement jsonelement = JsonRPCUtils.getRequestId(json);
            String s = JsonRPCUtils.getMethodName(json);
            JsonElement jsonelement1 = JsonRPCUtils.getResult(json);
            JsonElement jsonelement2 = JsonRPCUtils.getParams(json);
            JsonObject jsonobject = JsonRPCUtils.getError(json);
            if (s != null && jsonelement1 == null && jsonobject == null) {
                return jsonelement != null && !isValidRequestId(jsonelement)
                    ? JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported")
                    : this.handleIncomingRequest(jsonelement, s, jsonelement2);
            } else if (s == null && jsonelement1 != null && jsonobject == null && jsonelement != null) {
                if (isValidResponseId(jsonelement)) {
                    this.handleRequestResponse(jsonelement.getAsInt(), jsonelement1);
                } else {
                    LOGGER.warn("Received respose {} with id {} we did not request", jsonelement1, jsonelement);
                }

                return null;
            } else {
                return s == null && jsonelement1 == null && jsonobject != null
                    ? this.handleError(jsonelement, jsonobject)
                    : JsonRPCErrors.INVALID_REQUEST.createWithoutData(Objects.requireNonNullElse(jsonelement, JsonNull.INSTANCE));
            }
        } catch (Exception exception) {
            LOGGER.error("Error while handling rpc request", (Throwable)exception);
            return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
        }
    }

    private static boolean isValidRequestId(JsonElement json) {
        return json.isJsonNull() || GsonHelper.isNumberValue(json) || GsonHelper.isStringValue(json);
    }

    private static boolean isValidResponseId(JsonElement json) {
        return GsonHelper.isNumberValue(json);
    }

    @Nullable
    private JsonObject handleIncomingRequest(@Nullable JsonElement requestId, String methodName, @Nullable JsonElement params) {
        boolean flag = requestId != null;

        try {
            JsonElement jsonelement = this.dispatchIncomingRequest(methodName, params);
            return jsonelement != null && flag ? JsonRPCUtils.createSuccessResult(requestId, jsonelement) : null;
        } catch (InvalidParameterJsonRpcException invalidparameterjsonrpcexception) {
            LOGGER.debug("Invalid parameter invocation {}: {}, {}", methodName, params, invalidparameterjsonrpcexception.getMessage());
            return flag ? JsonRPCErrors.INVALID_PARAMS.create(requestId, invalidparameterjsonrpcexception.getMessage()) : null;
        } catch (EncodeJsonRpcException encodejsonrpcexception) {
            LOGGER.error("Failed to encode json rpc response {}: {}", methodName, encodejsonrpcexception.getMessage());
            return flag ? JsonRPCErrors.INTERNAL_ERROR.create(requestId, encodejsonrpcexception.getMessage()) : null;
        } catch (InvalidRequestJsonRpcException invalidrequestjsonrpcexception) {
            return flag ? JsonRPCErrors.INVALID_REQUEST.create(requestId, invalidrequestjsonrpcexception.getMessage()) : null;
        } catch (MethodNotFoundJsonRpcException methodnotfoundjsonrpcexception) {
            return flag ? JsonRPCErrors.METHOD_NOT_FOUND.create(requestId, methodnotfoundjsonrpcexception.getMessage()) : null;
        } catch (Exception exception) {
            LOGGER.error("Error while dispatching rpc method {}", methodName, exception);
            return flag ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(requestId) : null;
        }
    }

    @Nullable
    public JsonElement dispatchIncomingRequest(String methodName, @Nullable JsonElement params) {
        ResourceLocation resourcelocation = ResourceLocation.tryParse(methodName);
        if (resourcelocation == null) {
            throw new InvalidRequestJsonRpcException("Failed to parse method value: " + methodName);
        } else {
            Optional<IncomingRpcMethod> optional = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(resourcelocation);
            if (optional.isEmpty()) {
                throw new MethodNotFoundJsonRpcException("Method not found: " + methodName);
            } else if (optional.get().attributes().runOnMainThread()) {
                try {
                    return this.minecraftApi.<JsonElement>submit(() -> optional.get().apply(this.minecraftApi, params, this.clientInfo)).join();
                } catch (CompletionException completionexception) {
                    if (completionexception.getCause() instanceof RuntimeException runtimeexception) {
                        throw runtimeexception;
                    } else {
                        throw completionexception;
                    }
                }
            } else {
                return optional.get().apply(this.minecraftApi, params, this.clientInfo);
            }
        }
    }

    private void handleRequestResponse(int requestId, JsonElement result) {
        PendingRpcRequest<?> pendingrpcrequest = this.pendingRequests.remove(requestId);
        if (pendingrpcrequest == null) {
            LOGGER.warn("Received unknown response (id: {}): {}", requestId, result);
        } else {
            pendingrpcrequest.accept(result);
        }
    }

    @Nullable
    private JsonObject handleError(@Nullable JsonElement requestId, JsonObject error) {
        if (requestId != null && isValidResponseId(requestId)) {
            PendingRpcRequest<?> pendingrpcrequest = this.pendingRequests.remove(requestId.getAsInt());
            if (pendingrpcrequest != null) {
                pendingrpcrequest.resultFuture().completeExceptionally(new RemoteRpcErrorException(requestId, error));
            }
        }

        LOGGER.error("Received error (id: {}): {}", requestId, error);
        return null;
    }
}
