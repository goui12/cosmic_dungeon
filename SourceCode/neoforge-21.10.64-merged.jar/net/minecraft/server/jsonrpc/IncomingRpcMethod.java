package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.IllegalMethodDefinitionException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;

public interface IncomingRpcMethod {
    MethodInfo info();

    IncomingRpcMethod.Attributes attributes();

    JsonElement apply(MinecraftApi api, @Nullable JsonElement json, ClientInfo clientInfo);

    static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.ParameterlessMethod<Result>> method(
        IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier, Codec<Result> resultCodec
    ) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>((p_451610_, p_451611_) -> {
            if (p_451610_.params().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
            } else if (p_451610_.result().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method lacks result");
            } else {
                return new IncomingRpcMethod.ParameterlessMethod<>(p_451610_, p_451611_, resultCodec, supplier);
            }
        });
    }

    static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.Method<Params, Result>> method(
        IncomingRpcMethod.RpcMethodFunction<Params, Result> supplier, Codec<Params> paramsCodec, Codec<Result> resultCodec
    ) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>((p_451615_, p_451616_) -> {
            if (p_451615_.params().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
            } else if (p_451615_.result().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method lacks result");
            } else {
                return new IncomingRpcMethod.Method<>(p_451615_, p_451616_, paramsCodec, resultCodec, supplier);
            }
        });
    }

    static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<IncomingRpcMethod.ParameterlessMethod<Result>> method(
        Function<MinecraftApi, Result> supplier, Codec<Result> resultCodec
    ) {
        return new IncomingRpcMethod.IncomingRpcMethodBuilder<>((p_451606_, p_451607_) -> {
            if (p_451606_.params().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
            } else if (p_451606_.result().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method lacks result");
            } else {
                return new IncomingRpcMethod.ParameterlessMethod<>(p_451606_, p_451607_, resultCodec, (p_442688_, p_442938_) -> supplier.apply(p_442688_));
            }
        });
    }

    public record Attributes(boolean runOnMainThread, boolean discoverable) {
    }

    @FunctionalInterface
    public interface Factory<T extends IncomingRpcMethod> {
        T create(MethodInfo info, IncomingRpcMethod.Attributes attributes);
    }

    public static class IncomingRpcMethodBuilder<T extends IncomingRpcMethod> {
        private final IncomingRpcMethod.Factory<T> method;
        private String description = "";
        @Nullable
        private ParamInfo paramInfo;
        @Nullable
        private ResultInfo resultInfo;
        private boolean discoverable = true;
        private boolean runOnMainThread = true;

        public IncomingRpcMethodBuilder(IncomingRpcMethod.Factory<T> method) {
            this.method = method;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<T> response(ResultInfo resultInfo) {
            this.resultInfo = resultInfo;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<T> param(ParamInfo paramInfo) {
            this.paramInfo = paramInfo;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<T> undiscoverable() {
            this.discoverable = false;
            return this;
        }

        public IncomingRpcMethod.IncomingRpcMethodBuilder<T> notOnMainThread() {
            this.runOnMainThread = false;
            return this;
        }

        public T build() {
            MethodInfo methodinfo = new MethodInfo(this.description, this.paramInfo, this.resultInfo);
            return this.method.create(methodinfo, new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable));
        }

        public T register(Registry<IncomingRpcMethod> registry, String name) {
            return this.register(registry, ResourceLocation.withDefaultNamespace(name));
        }

        private T register(Registry<IncomingRpcMethod> registry, ResourceLocation name) {
            return Registry.register(registry, name, this.build());
        }
    }

    public record Method<Params, Result>(
        MethodInfo info,
        IncomingRpcMethod.Attributes attributes,
        Codec<Params> paramsCodec,
        Codec<Result> resultCodec,
        IncomingRpcMethod.RpcMethodFunction<Params, Result> function
    ) implements IncomingRpcMethod {
        @Override
        public JsonElement apply(MinecraftApi p_442932_, @Nullable JsonElement p_443044_, ClientInfo p_442918_) {
            if (p_443044_ != null && (p_443044_.isJsonArray() || p_443044_.isJsonObject())) {
                if (this.info.params().isEmpty()) {
                    throw new IllegalArgumentException("Method defined as having parameters without describing them");
                } else {
                    JsonElement jsonelement;
                    if (p_443044_.isJsonObject()) {
                        String s = this.info.params().get().name();
                        JsonElement jsonelement1 = p_443044_.getAsJsonObject().get(s);
                        if (jsonelement1 == null) {
                            throw new InvalidParameterJsonRpcException(
                                String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", s)
                            );
                        }

                        jsonelement = jsonelement1;
                    } else {
                        JsonArray jsonarray = p_443044_.getAsJsonArray();
                        if (jsonarray.isEmpty() || jsonarray.size() > 1) {
                            throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
                        }

                        jsonelement = jsonarray.get(0);
                    }

                    Params params = this.paramsCodec.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(InvalidParameterJsonRpcException::new);
                    Result result = this.function.apply(p_442932_, params, p_442918_);
                    return this.resultCodec.encodeStart(JsonOps.INSTANCE, result).getOrThrow(EncodeJsonRpcException::new);
                }
            } else {
                throw new InvalidParameterJsonRpcException("Expected params as array or named");
            }
        }
    }

    public record ParameterlessMethod<Result>(
        MethodInfo info, IncomingRpcMethod.Attributes attributes, Codec<Result> resultCodec, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier
    ) implements IncomingRpcMethod {
        @Override
        public JsonElement apply(MinecraftApi p_443197_, @Nullable JsonElement p_442828_, ClientInfo p_443323_) {
            if (p_442828_ == null || p_442828_.isJsonArray() && p_442828_.getAsJsonArray().isEmpty()) {
                if (this.info.params().isPresent()) {
                    throw new IllegalArgumentException("Method defined as not having parameters but is describing them");
                } else {
                    Result result = this.supplier.apply(p_443197_, p_443323_);
                    return this.resultCodec.encodeStart(JsonOps.INSTANCE, result).getOrThrow(InvalidParameterJsonRpcException::new);
                }
            } else {
                throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
            }
        }
    }

    @FunctionalInterface
    public interface ParameterlessRpcMethodFunction<Result> {
        Result apply(MinecraftApi api, ClientInfo clientInfo);
    }

    @FunctionalInterface
    public interface RpcMethodFunction<Params, Result> {
        Result apply(MinecraftApi api, Params params, ClientInfo clientInfo);
    }
}
