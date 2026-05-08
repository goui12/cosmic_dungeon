package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_319945_) {
                return decoder.decode(p_319945_);
            }

            @Override
            public void encode(B p_320538_, V p_320754_) {
                encoder.encode(p_320538_, p_320754_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_320797_) {
                return decoder.decode(p_320797_);
            }

            @Override
            public void encode(B p_319939_, V p_320568_) {
                encoder.encode(p_320568_, p_319939_);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(final V expectedValue) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B p_320572_) {
                return expectedValue;
            }

            @Override
            public void encode(B p_320044_, V p_320328_) {
                if (!p_320328_.equals(expectedValue)) {
                    throw new IllegalStateException("Can't encode '" + p_320328_ + "', expected '" + expectedValue + "'");
                }
            }
        };
    }

    default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> factory, final Function<? super O, ? extends V> getter) {
        return new StreamCodec<B, O>() {
            @Override
            public O decode(B p_320534_) {
                return (O)factory.apply(StreamCodec.this.decode(p_320534_));
            }

            @Override
            public void encode(B p_319798_, O p_320273_) {
                StreamCodec.this.encode(p_319798_, (V)getter.apply(p_320273_));
            }
        };
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(final Function<O, ? extends B> bufferFactory) {
        return new StreamCodec<O, V>() {
            public V decode(O p_319818_) {
                B b = (B)bufferFactory.apply(p_319818_);
                return StreamCodec.this.decode(b);
            }

            public void encode(O p_319973_, V p_319843_) {
                B b = (B)bufferFactory.apply(p_319973_);
                StreamCodec.this.encode(b, p_319843_);
            }
        };
    }

    default <U> StreamCodec<B, U> dispatch(
        final Function<? super U, ? extends V> keyGetter, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter
    ) {
        return new StreamCodec<B, U>() {
            @Override
            public U decode(B p_320094_) {
                V v = StreamCodec.this.decode(p_320094_);
                StreamCodec<? super B, ? extends U> streamcodec = (StreamCodec<? super B, ? extends U>)codecGetter.apply(v);
                return (U)streamcodec.decode(p_320094_);
            }

            @Override
            public void encode(B p_320767_, U p_320010_) {
                V v = (V)keyGetter.apply(p_320010_);
                StreamCodec<B, U> streamcodec = (StreamCodec<B, U>)codecGetter.apply(v);
                StreamCodec.this.encode(p_320767_, v);
                streamcodec.encode(p_320767_, p_320010_);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec, final Function<C, T1> getter, final Function<T1, C> factory) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320924_) {
                T1 t1 = codec.decode(p_320924_);
                return factory.apply(t1);
            }

            @Override
            public void encode(B p_320798_, C p_320749_) {
                codec.encode(p_320798_, getter.apply(p_320749_));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final BiFunction<T1, T2, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320168_) {
                T1 t1 = codec1.decode(p_320168_);
                T2 t2 = codec2.decode(p_320168_);
                return factory.apply(t1, t2);
            }

            @Override
            public void encode(B p_320592_, C p_320163_) {
                codec1.encode(p_320592_, getter1.apply(p_320163_));
                codec2.encode(p_320592_, getter2.apply(p_320163_));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final Function3<T1, T2, T3, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_320842_) {
                T1 t1 = codec1.decode(p_320842_);
                T2 t2 = codec2.decode(p_320842_);
                T3 t3 = codec3.decode(p_320842_);
                return factory.apply(t1, t2, t3);
            }

            @Override
            public void encode(B p_320737_, C p_320439_) {
                codec1.encode(p_320737_, getter1.apply(p_320439_));
                codec2.encode(p_320737_, getter2.apply(p_320439_));
                codec3.encode(p_320737_, getter3.apply(p_320439_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final Function4<T1, T2, T3, T4, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_323859_) {
                T1 t1 = codec1.decode(p_323859_);
                T2 t2 = codec2.decode(p_323859_);
                T3 t3 = codec3.decode(p_323859_);
                T4 t4 = codec4.decode(p_323859_);
                return factory.apply(t1, t2, t3, t4);
            }

            @Override
            public void encode(B p_323667_, C p_323469_) {
                codec1.encode(p_323667_, getter1.apply(p_323469_));
                codec2.encode(p_323667_, getter2.apply(p_323469_));
                codec3.encode(p_323667_, getter3.apply(p_323469_));
                codec4.encode(p_323667_, getter4.apply(p_323469_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final Function5<T1, T2, T3, T4, T5, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_324610_) {
                T1 t1 = codec1.decode(p_324610_);
                T2 t2 = codec2.decode(p_324610_);
                T3 t3 = codec3.decode(p_324610_);
                T4 t4 = codec4.decode(p_324610_);
                T5 t5 = codec5.decode(p_324610_);
                return factory.apply(t1, t2, t3, t4, t5);
            }

            @Override
            public void encode(B p_323786_, C p_323619_) {
                codec1.encode(p_323786_, getter1.apply(p_323619_));
                codec2.encode(p_323786_, getter2.apply(p_323619_));
                codec3.encode(p_323786_, getter3.apply(p_323619_));
                codec4.encode(p_323786_, getter4.apply(p_323619_));
                codec5.encode(p_323786_, getter5.apply(p_323619_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final Function6<T1, T2, T3, T4, T5, T6, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_330310_) {
                T1 t1 = codec1.decode(p_330310_);
                T2 t2 = codec2.decode(p_330310_);
                T3 t3 = codec3.decode(p_330310_);
                T4 t4 = codec4.decode(p_330310_);
                T5 t5 = codec5.decode(p_330310_);
                T6 t6 = codec6.decode(p_330310_);
                return factory.apply(t1, t2, t3, t4, t5, t6);
            }

            @Override
            public void encode(B p_332052_, C p_331912_) {
                codec1.encode(p_332052_, getter1.apply(p_331912_));
                codec2.encode(p_332052_, getter2.apply(p_331912_));
                codec3.encode(p_332052_, getter3.apply(p_331912_));
                codec4.encode(p_332052_, getter4.apply(p_331912_));
                codec5.encode(p_332052_, getter5.apply(p_331912_));
                codec6.encode(p_332052_, getter6.apply(p_331912_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_330903_) {
                T1 t1 = codec1.decode(p_330903_);
                T2 t2 = codec2.decode(p_330903_);
                T3 t3 = codec3.decode(p_330903_);
                T4 t4 = codec4.decode(p_330903_);
                T5 t5 = codec5.decode(p_330903_);
                T6 t6 = codec6.decode(p_330903_);
                T7 t7 = codec7.decode(p_330903_);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(B p_331641_, C p_330634_) {
                codec1.encode(p_331641_, getter1.apply(p_330634_));
                codec2.encode(p_331641_, getter2.apply(p_330634_));
                codec3.encode(p_331641_, getter3.apply(p_330634_));
                codec4.encode(p_331641_, getter4.apply(p_330634_));
                codec5.encode(p_331641_, getter5.apply(p_330634_));
                codec6.encode(p_331641_, getter6.apply(p_330634_));
                codec7.encode(p_331641_, getter7.apply(p_330634_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_373035_) {
                T1 t1 = codec1.decode(p_373035_);
                T2 t2 = codec2.decode(p_373035_);
                T3 t3 = codec3.decode(p_373035_);
                T4 t4 = codec4.decode(p_373035_);
                T5 t5 = codec5.decode(p_373035_);
                T6 t6 = codec6.decode(p_373035_);
                T7 t7 = codec7.decode(p_373035_);
                T8 t8 = codec8.decode(p_373035_);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }

            @Override
            public void encode(B p_372928_, C p_372897_) {
                codec1.encode(p_372928_, getter1.apply(p_372897_));
                codec2.encode(p_372928_, getter2.apply(p_372897_));
                codec3.encode(p_372928_, getter3.apply(p_372897_));
                codec4.encode(p_372928_, getter4.apply(p_372897_));
                codec5.encode(p_372928_, getter5.apply(p_372897_));
                codec6.encode(p_372928_, getter6.apply(p_372897_));
                codec7.encode(p_372928_, getter7.apply(p_372897_));
                codec8.encode(p_372928_, getter8.apply(p_372897_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_381156_) {
                T1 t1 = codec1.decode(p_381156_);
                T2 t2 = codec2.decode(p_381156_);
                T3 t3 = codec3.decode(p_381156_);
                T4 t4 = codec4.decode(p_381156_);
                T5 t5 = codec5.decode(p_381156_);
                T6 t6 = codec6.decode(p_381156_);
                T7 t7 = codec7.decode(p_381156_);
                T8 t8 = codec8.decode(p_381156_);
                T9 t9 = codec9.decode(p_381156_);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
            }

            @Override
            public void encode(B p_380991_, C p_381087_) {
                codec1.encode(p_380991_, getter1.apply(p_381087_));
                codec2.encode(p_380991_, getter2.apply(p_381087_));
                codec3.encode(p_380991_, getter3.apply(p_381087_));
                codec4.encode(p_380991_, getter4.apply(p_381087_));
                codec5.encode(p_380991_, getter5.apply(p_381087_));
                codec6.encode(p_380991_, getter6.apply(p_381087_));
                codec7.encode(p_380991_, getter7.apply(p_381087_));
                codec8.encode(p_380991_, getter8.apply(p_381087_));
                codec9.encode(p_380991_, getter9.apply(p_381087_));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
        final StreamCodec<? super B, T1> codec1,
        final Function<C, T1> getter1,
        final StreamCodec<? super B, T2> codec2,
        final Function<C, T2> getter2,
        final StreamCodec<? super B, T3> codec3,
        final Function<C, T3> getter3,
        final StreamCodec<? super B, T4> codec4,
        final Function<C, T4> getter4,
        final StreamCodec<? super B, T5> codec5,
        final Function<C, T5> getter5,
        final StreamCodec<? super B, T6> codec6,
        final Function<C, T6> getter6,
        final StreamCodec<? super B, T7> codec7,
        final Function<C, T7> getter7,
        final StreamCodec<? super B, T8> codec8,
        final Function<C, T8> getter8,
        final StreamCodec<? super B, T9> codec9,
        final Function<C, T9> getter9,
        final StreamCodec<? super B, T10> codec10,
        final Function<C, T10> getter10,
        final StreamCodec<? super B, T11> codec11,
        final Function<C, T11> getter11,
        final Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B p_397196_) {
                T1 t1 = codec1.decode(p_397196_);
                T2 t2 = codec2.decode(p_397196_);
                T3 t3 = codec3.decode(p_397196_);
                T4 t4 = codec4.decode(p_397196_);
                T5 t5 = codec5.decode(p_397196_);
                T6 t6 = codec6.decode(p_397196_);
                T7 t7 = codec7.decode(p_397196_);
                T8 t8 = codec8.decode(p_397196_);
                T9 t9 = codec9.decode(p_397196_);
                T10 t10 = codec10.decode(p_397196_);
                T11 t11 = codec11.decode(p_397196_);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
            }

            @Override
            public void encode(B p_397968_, C p_397982_) {
                codec1.encode(p_397968_, getter1.apply(p_397982_));
                codec2.encode(p_397968_, getter2.apply(p_397982_));
                codec3.encode(p_397968_, getter3.apply(p_397982_));
                codec4.encode(p_397968_, getter4.apply(p_397982_));
                codec5.encode(p_397968_, getter5.apply(p_397982_));
                codec6.encode(p_397968_, getter6.apply(p_397982_));
                codec7.encode(p_397968_, getter7.apply(p_397982_));
                codec8.encode(p_397968_, getter8.apply(p_397982_));
                codec9.encode(p_397968_, getter9.apply(p_397982_));
                codec10.encode(p_397968_, getter10.apply(p_397982_));
                codec11.encode(p_397968_, getter11.apply(p_397982_));
            }
        };
    }

    static <B, T> StreamCodec<B, T> recursive(final UnaryOperator<StreamCodec<B, T>> modifier) {
        return new StreamCodec<B, T>() {
            private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> modifier.apply(this));

            @Override
            public T decode(B p_425697_) {
                return this.inner.get().decode(p_425697_);
            }

            @Override
            public void encode(B p_425873_, T p_425600_) {
                this.inner.get().encode(p_425873_, p_425600_);
            }
        };
    }

    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>)this;
    }

    @FunctionalInterface
    public interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
