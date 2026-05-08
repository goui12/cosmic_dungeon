package net.minecraft.network.codec;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface ByteBufCodecs {
    int MAX_INITIAL_COLLECTION_SIZE = 65536;
    StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<ByteBuf, Boolean>() {
        public Boolean decode(ByteBuf p_320813_) {
            return p_320813_.readBoolean();
        }

        public void encode(ByteBuf p_319896_, Boolean p_320251_) {
            p_319896_.writeBoolean(p_320251_);
        }
    };
    StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>() {
        public Byte decode(ByteBuf p_320628_) {
            return p_320628_.readByte();
        }

        public void encode(ByteBuf p_320364_, Byte p_320618_) {
            p_320364_.writeByte(p_320618_);
        }
    };
    StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
    StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>() {
        public Short decode(ByteBuf p_320513_) {
            return p_320513_.readShort();
        }

        public void encode(ByteBuf p_320028_, Short p_320388_) {
            p_320028_.writeShort(p_320388_);
        }
    };
    StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>() {
        public Integer decode(ByteBuf p_320319_) {
            return p_320319_.readUnsignedShort();
        }

        public void encode(ByteBuf p_320669_, Integer p_320205_) {
            p_320669_.writeShort(p_320205_);
        }
    };
    StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>() {
        public Integer decode(ByteBuf p_320253_) {
            return p_320253_.readInt();
        }

        public void encode(ByteBuf p_320753_, Integer p_330380_) {
            p_320753_.writeInt(p_330380_);
        }
    };
    StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>() {
        public Integer decode(ByteBuf p_320759_) {
            return VarInt.read(p_320759_);
        }

        public void encode(ByteBuf p_320314_, Integer p_341414_) {
            VarInt.write(p_320314_, p_341414_);
        }
    };
    StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
        p_378955_ -> p_378955_ == 0 ? OptionalInt.empty() : OptionalInt.of(p_378955_ - 1), p_378954_ -> p_378954_.isPresent() ? p_378954_.getAsInt() + 1 : 0
    );
    StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>() {
        public Long decode(ByteBuf p_320635_) {
            return p_320635_.readLong();
        }

        public void encode(ByteBuf p_320545_, Long p_341419_) {
            p_320545_.writeLong(p_341419_);
        }
    };
    StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>() {
        public Long decode(ByteBuf p_320259_) {
            return VarLong.read(p_320259_);
        }

        public void encode(ByteBuf p_320199_, Long p_376100_) {
            VarLong.write(p_320199_, p_376100_);
        }
    };
    StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>() {
        public Float decode(ByteBuf p_320599_) {
            return p_320599_.readFloat();
        }

        public void encode(ByteBuf p_320880_, Float p_376495_) {
            p_320880_.writeFloat(p_376495_);
        }
    };
    StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>() {
        public Double decode(ByteBuf p_319947_) {
            return p_319947_.readDouble();
        }

        public void encode(ByteBuf p_320370_, Double p_376267_) {
            p_320370_.writeDouble(p_376267_);
        }
    };
    StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>() {
        public byte[] decode(ByteBuf buffer) {
            return FriendlyByteBuf.readByteArray(buffer);
        }

        public void encode(ByteBuf buffer, byte[] value) {
            FriendlyByteBuf.writeByteArray(buffer, value);
        }
    };
    StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>() {
        public long[] decode(ByteBuf p_341393_) {
            return FriendlyByteBuf.readLongArray(p_341393_);
        }

        public void encode(ByteBuf p_340857_, long[] p_404761_) {
            FriendlyByteBuf.writeLongArray(p_340857_, p_404761_);
        }
    };
    StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    StreamCodec<ByteBuf, Tag> TAG = tagCodec(() -> NbtAccounter.create(2097152L));
    StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(NbtAccounter::unlimitedHeap);
    StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(() -> NbtAccounter.create(2097152L));
    StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(NbtAccounter::unlimitedHeap);
    StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>() {
        public Optional<CompoundTag> decode(ByteBuf p_324220_) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(p_324220_));
        }

        public void encode(ByteBuf p_323874_, Optional<CompoundTag> p_428393_) {
            FriendlyByteBuf.writeNbt(p_323874_, p_428393_.orElse(null));
        }
    };
    StreamCodec<ByteBuf, Vector3f> VECTOR3F = new StreamCodec<ByteBuf, Vector3f>() {
        public Vector3f decode(ByteBuf p_331901_) {
            return FriendlyByteBuf.readVector3f(p_331901_);
        }

        public void encode(ByteBuf p_331539_, Vector3f p_428266_) {
            FriendlyByteBuf.writeVector3f(p_331539_, p_428266_);
        }
    };
    StreamCodec<ByteBuf, Quaternionf> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionf>() {
        public Quaternionf decode(ByteBuf p_332082_) {
            return FriendlyByteBuf.readQuaternion(p_332082_);
        }

        public void encode(ByteBuf p_331172_, Quaternionf p_428267_) {
            FriendlyByteBuf.writeQuaternion(p_331172_, p_428267_);
        }
    };
    StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>() {
        public Integer decode(ByteBuf p_340809_) {
            return FriendlyByteBuf.readContainerId(p_340809_);
        }

        public void encode(ByteBuf p_341417_, Integer p_428195_) {
            FriendlyByteBuf.writeContainerId(p_341417_, p_428195_);
        }
    };
    StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>() {
        private static final int MAX_PROPERTY_NAME_LENGTH = 64;
        private static final int MAX_PROPERTY_VALUE_LENGTH = 32767;
        private static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
        private static final int MAX_PROPERTIES = 16;

        public PropertyMap decode(ByteBuf p_415628_) {
            int i = ByteBufCodecs.readCount(p_415628_, 16);
            Builder<String, Property> builder = ImmutableMultimap.builder();

            for (int j = 0; j < i; j++) {
                String s = Utf8String.read(p_415628_, 64);
                String s1 = Utf8String.read(p_415628_, 32767);
                String s2 = FriendlyByteBuf.readNullable(p_415628_, p_428194_ -> Utf8String.read(p_428194_, 1024));
                Property property = new Property(s, s1, s2);
                builder.put(property.name(), property);
            }

            return new PropertyMap(builder.build());
        }

        public void encode(ByteBuf p_415915_, PropertyMap p_428306_) {
            ByteBufCodecs.writeCount(p_415915_, p_428306_.size(), 16);

            for (Property property : p_428306_.values()) {
                Utf8String.write(p_415915_, property.name(), 64);
                Utf8String.write(p_415915_, property.value(), 32767);
                FriendlyByteBuf.writeNullable(p_415915_, property.signature(), (p_428520_, p_428307_) -> Utf8String.write(p_428520_, p_428307_, 1024));
            }
        }
    };
    StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
    StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new
    );
    StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>() {
        public Integer decode(ByteBuf p_422404_) {
            return ARGB.color(p_422404_.readByte() & 0xFF, p_422404_.readByte() & 0xFF, p_422404_.readByte() & 0xFF);
        }

        public void encode(ByteBuf p_422062_, Integer p_440662_) {
            p_422062_.writeByte(ARGB.red(p_440662_));
            p_422062_.writeByte(ARGB.green(p_440662_));
            p_422062_.writeByte(ARGB.blue(p_440662_));
        }
    };

    static StreamCodec<ByteBuf, byte[]> byteArray(final int maxSize) {
        return new StreamCodec<ByteBuf, byte[]>() {
            public byte[] decode(ByteBuf p_320167_) {
                return FriendlyByteBuf.readByteArray(p_320167_, maxSize);
            }

            public void encode(ByteBuf p_320240_, byte[] p_341316_) {
                if (p_341316_.length > maxSize) {
                    throw new EncoderException("ByteArray with size " + p_341316_.length + " is bigger than allowed " + maxSize);
                } else {
                    FriendlyByteBuf.writeByteArray(p_320240_, p_341316_);
                }
            }
        };
    }

    static StreamCodec<ByteBuf, String> stringUtf8(final int maxLength) {
        return new StreamCodec<ByteBuf, String>() {
            public String decode(ByteBuf p_376272_) {
                return Utf8String.read(p_376272_, maxLength);
            }

            public void encode(ByteBuf p_376553_, String p_404954_) {
                Utf8String.write(p_376553_, p_404954_, maxLength);
            }
        };
    }

    static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(final Supplier<NbtAccounter> accounter) {
        return new StreamCodec<ByteBuf, Optional<Tag>>() {
            public Optional<Tag> decode(ByteBuf p_404669_) {
                return Optional.ofNullable(FriendlyByteBuf.readNbt(p_404669_, accounter.get()));
            }

            public void encode(ByteBuf p_405814_, Optional<Tag> p_428357_) {
                FriendlyByteBuf.writeNbt(p_405814_, p_428357_.orElse(null));
            }
        };
    }

    static StreamCodec<ByteBuf, Tag> tagCodec(final Supplier<NbtAccounter> accounter) {
        return new StreamCodec<ByteBuf, Tag>() {
            public Tag decode(ByteBuf p_421862_) {
                Tag tag = FriendlyByteBuf.readNbt(p_421862_, accounter.get());
                if (tag == null) {
                    throw new DecoderException("Expected non-null compound tag");
                } else {
                    return tag;
                }
            }

            public void encode(ByteBuf p_422136_, Tag p_428274_) {
                if (p_428274_ == EndTag.INSTANCE) {
                    throw new EncoderException("Expected non-null compound tag");
                } else {
                    FriendlyByteBuf.writeNbt(p_422136_, p_428274_);
                }
            }
        };
    }

    static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> accounterSupplier) {
        return tagCodec(accounterSupplier).map(p_339405_ -> {
            if (p_339405_ instanceof CompoundTag compoundtag) {
                return compoundtag;
            } else {
                throw new DecoderException("Not a compound tag: " + p_339405_);
            }
        }, p_330975_ -> (Tag)p_330975_);
    }

    static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) {
        return fromCodec(codec, NbtAccounter::unlimitedHeap);
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return fromCodec(codec, () -> NbtAccounter.create(2097152L));
    }

    static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(DynamicOps<T> ops, Codec<V> codec) {
        return p_428110_ -> new StreamCodec<B, V>() {
            public V decode(B p_428561_) {
                T t = (T)p_428110_.decode(p_428561_);
                return (V)codec.parse(ops, t).getOrThrow(p_421833_ -> new DecoderException("Failed to decode: " + p_421833_ + " " + t));
            }

            public void encode(B p_428515_, V p_422051_) {
                T t = (T)codec.encodeStart(ops, p_422051_)
                    .getOrThrow(p_421600_ -> new EncoderException("Failed to encode: " + p_421600_ + " " + p_422051_));
                p_428110_.encode(p_428515_, t);
            }
        };
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> accounterSupplier) {
        return tagCodec(accounterSupplier).apply(fromCodec(NbtOps.INSTANCE, codec));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> codec) {
        return fromCodecWithRegistries(codec, NbtAccounter::unlimitedHeap);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return fromCodecWithRegistries(codec, () -> NbtAccounter.create(2097152L));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> codec, Supplier<NbtAccounter> accounterSupplier) {
        final StreamCodec<ByteBuf, Tag> streamcodec = tagCodec(accounterSupplier);
        return new StreamCodec<RegistryFriendlyByteBuf, T>() {
            public T decode(RegistryFriendlyByteBuf p_428534_) {
                Tag tag = streamcodec.decode(p_428534_);
                RegistryOps<Tag> registryops = p_428534_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                return codec.parse(registryops, tag).getOrThrow(p_428554_ -> new DecoderException("Failed to decode: " + p_428554_ + " " + tag));
            }

            public void encode(RegistryFriendlyByteBuf p_428528_, T p_428469_) {
                RegistryOps<Tag> registryops = p_428528_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                Tag tag = codec.encodeStart(registryops, p_428469_)
                    .getOrThrow(p_428492_ -> new EncoderException("Failed to encode: " + p_428492_ + " " + p_428469_));
                streamcodec.encode(p_428528_, tag);
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> codec) {
        return new StreamCodec<B, Optional<V>>() {
            public Optional<V> decode(B p_363037_) {
                return p_363037_.readBoolean() ? Optional.of(codec.decode(p_363037_)) : Optional.empty();
            }

            public void encode(B p_364013_, Optional<V> p_428441_) {
                if (p_428441_.isPresent()) {
                    p_364013_.writeBoolean(true);
                    codec.encode(p_364013_, p_428441_.get());
                } else {
                    p_364013_.writeBoolean(false);
                }
            }
        };
    }

    static int readCount(ByteBuf buffer, int maxSize) {
        int i = VarInt.read(buffer);
        if (i > maxSize) {
            throw new DecoderException(i + " elements exceeded max size of: " + maxSize);
        } else {
            return i;
        }
    }

    static void writeCount(ByteBuf buffer, int count, int maxSize) {
        if (count > maxSize) {
            throw new EncoderException(count + " elements exceeded max size of: " + maxSize);
        } else {
            VarInt.write(buffer, count);
        }
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec) {
        return collection(factory, codec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
        final IntFunction<C> factory, final StreamCodec<? super B, V> codec, final int maxSize
    ) {
        return new StreamCodec<B, C>() {
            public C decode(B p_376474_) {
                int i = ByteBufCodecs.readCount(p_376474_, maxSize);
                C c = factory.apply(Math.min(i, 65536));

                for (int j = 0; j < i; j++) {
                    c.add(codec.decode(p_376474_));
                }

                return c;
            }

            public void encode(B p_376188_, C p_428244_) {
                ByteBufCodecs.writeCount(p_376188_, p_428244_.size(), maxSize);

                for (V v : p_428244_) {
                    codec.encode(p_376188_, v);
                }
            }
        };
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
        return p_319785_ -> collection(factory, p_319785_);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return p_320272_ -> collection(ArrayList::new, p_320272_);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return p_329871_ -> collection(ArrayList::new, p_329871_, maxSize);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
        IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec
    ) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
        final IntFunction<? extends M> factory, final StreamCodec<? super B, K> keyCodec, final StreamCodec<? super B, V> valueCodec, final int maxSize
    ) {
        return new StreamCodec<B, M>() {
            public void encode(B p_405748_, M p_428450_) {
                ByteBufCodecs.writeCount(p_405748_, p_428450_.size(), maxSize);
                p_428450_.forEach((p_428117_, p_428118_) -> {
                    keyCodec.encode(p_405748_, (K)p_428117_);
                    valueCodec.encode(p_405748_, (V)p_428118_);
                });
            }

            public M decode(B p_405291_) {
                int i = ByteBufCodecs.readCount(p_405291_, maxSize);
                M m = (M)factory.apply(Math.min(i, 65536));

                for (int j = 0; j < i; j++) {
                    K k = keyCodec.decode(p_405291_);
                    V v = valueCodec.decode(p_405291_);
                    m.put(k, v);
                }

                return m;
            }
        };
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> leftCodec, final StreamCodec<? super B, R> rightCodec) {
        return new StreamCodec<B, Either<L, R>>() {
            public Either<L, R> decode(B p_412599_) {
                return p_412599_.readBoolean() ? Either.left(leftCodec.decode(p_412599_)) : Either.right(rightCodec.decode(p_412599_));
            }

            public void encode(B p_412292_, Either<L, R> p_428253_) {
                p_428253_.ifLeft(p_428205_ -> {
                    p_412292_.writeBoolean(true);
                    leftCodec.encode(p_412292_, (L)p_428205_);
                }).ifRight(p_428354_ -> {
                    p_412292_.writeBoolean(false);
                    rightCodec.encode(p_412292_, (R)p_428354_);
                });
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(int maxLength, BiFunction<B, ByteBuf, B> function) {
        return p_428113_ -> new StreamCodec<B, V>() {
            public V decode(B p_422567_) {
                int i = VarInt.read(p_422567_);
                if (i > maxLength) {
                    throw new DecoderException("Buffer size " + i + " is larger than allowed limit of " + maxLength);
                } else {
                    int j = p_422567_.readerIndex();
                    B b = (B)((ByteBuf)function.apply(p_422567_, p_422567_.slice(j, i)));
                    p_422567_.readerIndex(j + i);
                    return (V)p_428113_.decode(b);
                }
            }

            public void encode(B p_422636_, V p_412065_) {
                B b = (B)((ByteBuf)function.apply(p_422636_, p_422636_.alloc().buffer()));

                try {
                    p_428113_.encode(b, p_412065_);
                    int i = b.readableBytes();
                    if (i > maxLength) {
                        throw new EncoderException("Buffer size " + i + " is  larger than allowed limit of " + maxLength);
                    }

                    VarInt.write(p_422636_, i);
                    p_422636_.writeBytes(b);
                } finally {
                    b.release();
                }
            }
        };
    }

    static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int length) {
        return lengthPrefixed(length, (p_428322_, p_428403_) -> p_428403_);
    }

    static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(int length) {
        return lengthPrefixed(length, (p_412025_, p_412026_) -> new RegistryFriendlyByteBuf(p_412026_, p_412025_.registryAccess()));
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> idLookup, final ToIntFunction<T> idGetter) {
        return new StreamCodec<ByteBuf, T>() {
            public T decode(ByteBuf p_428190_) {
                int i = VarInt.read(p_428190_);
                return idLookup.apply(i);
            }

            public void encode(ByteBuf p_428271_, T p_421951_) {
                int i = idGetter.applyAsInt(p_421951_);
                VarInt.write(p_428271_, i);
            }
        };
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> idMap) {
        return idMapper(idMap::byIdOrThrow, idMap::getIdOrThrow);
    }

    private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(
        final ResourceKey<? extends Registry<T>> registryKey, final Function<Registry<T>, IdMap<R>> idGetter
    ) {
        return new StreamCodec<RegistryFriendlyByteBuf, R>() {
            private IdMap<R> getRegistryOrThrow(RegistryFriendlyByteBuf p_421864_) {
                return idGetter.apply(getSyncableRegistryOrThrow(p_421864_, registryKey));
            }

            public R decode(RegistryFriendlyByteBuf p_412249_) {
                int i = VarInt.read(p_412249_);
                return (R)this.getRegistryOrThrow(p_412249_).byIdOrThrow(i);
            }

            public void encode(RegistryFriendlyByteBuf p_412573_, R p_428521_) {
                int i = this.getRegistryOrThrow(p_412573_).getIdOrThrow(p_428521_);
                VarInt.write(p_412573_, i);
            }
        };
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> registryKey) {
        return registry(registryKey, p_428107_ -> p_428107_);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        return registry(registryKey, Registry::asHolderIdMap);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(
        final ResourceKey<? extends Registry<T>> registryKey, final StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>() {
            private static final int DIRECT_HOLDER_ID = 0;

            private IdMap<Holder<T>> getRegistryOrThrow(RegistryFriendlyByteBuf p_428365_) {
                return getSyncableRegistryOrThrow(p_428365_, registryKey).asHolderIdMap();
            }

            public Holder<T> decode(RegistryFriendlyByteBuf p_421902_) {
                int i = VarInt.read(p_421902_);
                return i == 0 ? Holder.direct(codec.decode(p_421902_)) : (Holder)this.getRegistryOrThrow(p_421902_).byIdOrThrow(i - 1);
            }

            public void encode(RegistryFriendlyByteBuf p_422033_, Holder<T> p_428456_) {
                switch (p_428456_.kind()) {
                    case REFERENCE:
                        int i = this.getRegistryOrThrow(p_422033_).getIdOrThrow(p_428456_);
                        VarInt.write(p_422033_, i + 1);
                        break;
                    case DIRECT:
                        VarInt.write(p_422033_, 0);
                        codec.encode(p_422033_, p_428456_.value());
                }
            }
        };
    }

    private static <T> Registry<T> getSyncableRegistryOrThrow(RegistryFriendlyByteBuf buffer, ResourceKey<? extends Registry<T>> registryKey) {
        var registry = buffer.registryAccess().lookupOrThrow(registryKey);
        if (net.neoforged.neoforge.registries.RegistryManager.isNonSyncedBuiltInRegistry(registry)) {
            throw new io.netty.handler.codec.CodecException("Cannot use ID syncing for non-synced built-in registry: " + registry.key());
        }
        return registry;
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(final ResourceKey<? extends Registry<T>> registryKey) {
        return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>() {
            private static final int NAMED_SET = -1;
            private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = ByteBufCodecs.holderRegistry(registryKey);

            private final Map<net.neoforged.neoforge.registries.holdersets.HolderSetType, StreamCodec<RegistryFriendlyByteBuf, ? extends net.neoforged.neoforge.registries.holdersets.ICustomHolderSet<T>>> holderSetCodecs = new java.util.concurrent.ConcurrentHashMap<>();

            private StreamCodec<RegistryFriendlyByteBuf, ? extends net.neoforged.neoforge.registries.holdersets.ICustomHolderSet<T>> holderSetCodec(net.neoforged.neoforge.registries.holdersets.HolderSetType type) {
                return this.holderSetCodecs.computeIfAbsent(type, key -> key.makeStreamCodec(registryKey));
            }

            private <H extends net.neoforged.neoforge.registries.holdersets.ICustomHolderSet<T>> H cast(net.neoforged.neoforge.registries.holdersets.ICustomHolderSet<T> holderSet) {
                return (H) holderSet;
            }

            public HolderSet<T> decode(RegistryFriendlyByteBuf p_428283_) {
                int i = VarInt.read(p_428283_) - 1;
                // Neo: Co-opt negative VarInt values within the HolderSet codec as an HolderSetType id.
                // Vanilla uses 0 for tag and [1, Integer.MAX_VALUE] for list size [0, Integer.MAX_VALUE - 1].
                // So we may encode the registry id for custom holder set types in [Integer.MIN_VALUE + 1, -1] (local variable i must not be underflow).
                // The registry id for custom holder set types is (-1 - network id), while local variable i is (network id - 1), so the registry id would be (-2 - i).
                if (i < -1) {
                    return this.holderSetCodec(net.neoforged.neoforge.registries.NeoForgeRegistries.HOLDER_SET_TYPES.byIdOrThrow(-2 - i)).decode(p_428283_);
                }
                if (i == -1) {
                    Registry<T> registry = p_428283_.registryAccess().lookupOrThrow(registryKey);
                    return registry.get(TagKey.create(registryKey, ResourceLocation.STREAM_CODEC.decode(p_428283_))).orElseThrow();
                } else {
                    List<Holder<T>> list = new ArrayList<>(Math.min(i, 65536));

                    for (int j = 0; j < i; j++) {
                        list.add(this.holderCodec.decode(p_428283_));
                    }

                    return HolderSet.direct(list);
                }
            }

            public void encode(RegistryFriendlyByteBuf p_428187_, HolderSet<T> p_428305_) {
                // Neo: Co-opt negative VarInt values within the HolderSet codec as an HolderSetType id.
                // Vanilla uses 0 for tag and [1, Integer.MAX_VALUE] for list size [0, Integer.MAX_VALUE - 1] (local variable i in decode() must not be underflow).
                // So we may encode the registry id for custom holder set types in [Integer.MIN_VALUE + 1, -1].
                // The network id for custom holder set types is (-1 - registry id)
                if (p_428187_.getConnectionType().isNeoForge() && p_428305_ instanceof net.neoforged.neoforge.registries.holdersets.ICustomHolderSet<T> customHolderSet) {
                    VarInt.write(p_428187_, -1 - net.neoforged.neoforge.registries.NeoForgeRegistries.HOLDER_SET_TYPES.getId(customHolderSet.type()));
                    this.holderSetCodec(customHolderSet.type()).encode(p_428187_, cast(customHolderSet));
                    return;
                }
                Optional<TagKey<T>> optional = p_428305_.unwrapKey();
                if (optional.isPresent()) {
                    VarInt.write(p_428187_, 0);
                    ResourceLocation.STREAM_CODEC.encode(p_428187_, optional.get().location());
                } else {
                    VarInt.write(p_428187_, p_428305_.size() + 1);

                    for (Holder<T> holder : p_428305_) {
                        this.holderCodec.encode(p_428187_, holder);
                    }
                }
            }
        };
    }

    static StreamCodec<ByteBuf, JsonElement> lenientJson(final int maxLength) {
        return new StreamCodec<ByteBuf, JsonElement>() {
            private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

            public JsonElement decode(ByteBuf p_422595_) {
                String s = Utf8String.read(p_422595_, maxLength);

                try {
                    return LenientJsonParser.parse(s);
                } catch (JsonSyntaxException jsonsyntaxexception) {
                    throw new DecoderException("Failed to parse JSON", jsonsyntaxexception);
                }
            }

            public void encode(ByteBuf p_422247_, JsonElement p_439988_) {
                String s = GSON.toJson(p_439988_);
                Utf8String.write(p_422247_, s, maxLength);
            }
        };
    }
}
