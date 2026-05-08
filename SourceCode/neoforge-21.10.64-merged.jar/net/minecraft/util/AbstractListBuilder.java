package net.minecraft.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import java.util.function.UnaryOperator;

abstract class AbstractListBuilder<T, B> implements ListBuilder<T> {
    private final DynamicOps<T> ops;
    protected DataResult<B> builder = DataResult.success(this.initBuilder(), Lifecycle.stable());

    protected AbstractListBuilder(DynamicOps<T> ops) {
        this.ops = ops;
    }

    @Override
    public DynamicOps<T> ops() {
        return this.ops;
    }

    protected abstract B initBuilder();

    protected abstract B append(B builder, T value);

    protected abstract DataResult<T> build(B builder, T value);

    @Override
    public ListBuilder<T> add(T value) {
        this.builder = this.builder.map(p_412090_ -> this.append((B)p_412090_, value));
        return this;
    }

    @Override
    public ListBuilder<T> add(DataResult<T> value) {
        this.builder = this.builder.apply2stable(this::append, value);
        return this;
    }

    @Override
    public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
        this.builder = this.builder.flatMap(p_412423_ -> result.map(p_412737_ -> p_412423_));
        return this;
    }

    @Override
    public ListBuilder<T> mapError(UnaryOperator<String> onError) {
        this.builder = this.builder.mapError(onError);
        return this;
    }

    @Override
    public DataResult<T> build(T prefix) {
        DataResult<T> dataresult = this.builder.flatMap(p_412422_ -> this.build((B)p_412422_, prefix));
        this.builder = DataResult.success(this.initBuilder(), Lifecycle.stable());
        return dataresult;
    }
}
