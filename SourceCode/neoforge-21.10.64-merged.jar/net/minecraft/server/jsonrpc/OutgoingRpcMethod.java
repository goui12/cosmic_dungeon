package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.methods.IllegalMethodDefinitionException;

public interface OutgoingRpcMethod<Params, Result> {
    String NOTIFICATION_PREFIX = "notification/";

    MethodInfo info();

    OutgoingRpcMethod.Attributes attributes();

    @Nullable
    default JsonElement encodeParams(Params params) {
        return null;
    }

    @Nullable
    default Result decodeResult(JsonElement json) {
        return null;
    }

    static OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.ParmeterlessNotification> notification() {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>((p_451623_, p_451624_) -> {
            if (p_451623_.params().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
            } else if (p_451623_.result().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having result but is describing it");
            } else {
                return new OutgoingRpcMethod.ParmeterlessNotification(p_451623_, p_451624_);
            }
        });
    }

    static <Params> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.Notification<Params>> notification(Codec<Params> paramsCodec) {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>((p_451621_, p_451622_) -> {
            if (p_451621_.params().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
            } else if (p_451621_.result().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having result but is describing it");
            } else {
                return new OutgoingRpcMethod.Notification<>(p_451621_, p_451622_, paramsCodec);
            }
        });
    }

    static <Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.ParameterlessMethod<Result>> request(Codec<Result> resultCodec) {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>((p_451618_, p_451619_) -> {
            if (p_451618_.params().isPresent()) {
                throw new IllegalMethodDefinitionException("Method defined as not having parameters but is describing them");
            } else if (p_451618_.result().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method lacks result");
            } else {
                return new OutgoingRpcMethod.ParameterlessMethod<>(p_451618_, p_451619_, resultCodec);
            }
        });
    }

    static <Params, Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<OutgoingRpcMethod.Method<Params, Result>> request(
        Codec<Params> paramsCodec, Codec<Result> resultCodec
    ) {
        return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>((p_451627_, p_451628_) -> {
            if (p_451627_.params().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method defined as having parameters without describing them");
            } else if (p_451627_.result().isEmpty()) {
                throw new IllegalMethodDefinitionException("Method lacks result");
            } else {
                return new OutgoingRpcMethod.Method<>(p_451627_, p_451628_, paramsCodec, resultCodec);
            }
        });
    }

    public record Attributes(boolean discoverable) {
    }

    @FunctionalInterface
    public interface Factory<T extends OutgoingRpcMethod<?, ?>> {
        T create(MethodInfo info, OutgoingRpcMethod.Attributes attributes);
    }

    public record Method<Params, Result>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Params> paramsCodec, Codec<Result> resultCodec)
        implements OutgoingRpcMethod<Params, Result> {
        @Nullable
        @Override
        public JsonElement encodeParams(Params p_442798_) {
            return this.paramsCodec.encodeStart(JsonOps.INSTANCE, p_442798_).getOrThrow();
        }

        @Override
        public Result decodeResult(JsonElement p_442832_) {
            return this.resultCodec.parse(JsonOps.INSTANCE, p_442832_).getOrThrow();
        }
    }

    public record Notification<Params>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Params> paramsCodec)
        implements OutgoingRpcMethod<Params, Void> {
        @Nullable
        @Override
        public JsonElement encodeParams(Params p_443326_) {
            return this.paramsCodec.encodeStart(JsonOps.INSTANCE, p_443326_).getOrThrow();
        }
    }

    public static class OutgoingRpcMethodBuilder<T extends OutgoingRpcMethod<?, ?>> {
        public static final OutgoingRpcMethod.Attributes DEFAULT_ATTRIBUTES = new OutgoingRpcMethod.Attributes(true);
        private final OutgoingRpcMethod.Factory<T> method;
        private String description = "";
        @Nullable
        private ParamInfo paramInfo;
        @Nullable
        private ResultInfo resultInfo;

        public OutgoingRpcMethodBuilder(OutgoingRpcMethod.Factory<T> method) {
            this.method = method;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> response(ResultInfo resultInfo) {
            this.resultInfo = resultInfo;
            return this;
        }

        public OutgoingRpcMethod.OutgoingRpcMethodBuilder<T> param(ParamInfo paramInfo) {
            this.paramInfo = paramInfo;
            return this;
        }

        private T build() {
            MethodInfo methodinfo = new MethodInfo(this.description, this.paramInfo, this.resultInfo);
            return this.method.create(methodinfo, DEFAULT_ATTRIBUTES);
        }

        public Holder.Reference<T> register(String name) {
            return this.register(ResourceLocation.withDefaultNamespace("notification/" + name));
        }

        private Holder.Reference<T> register(ResourceLocation name) {
            return Registry.registerForHolder(BuiltInRegistries.OUTGOING_RPC_METHOD, name, this.build());
        }
    }

    public record ParameterlessMethod<Result>(MethodInfo info, OutgoingRpcMethod.Attributes attributes, Codec<Result> resultCodec)
        implements OutgoingRpcMethod<Void, Result> {
        @Override
        public Result decodeResult(JsonElement p_442885_) {
            return this.resultCodec.parse(JsonOps.INSTANCE, p_442885_).getOrThrow();
        }
    }

    public record ParmeterlessNotification(MethodInfo info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Void> {
    }
}
