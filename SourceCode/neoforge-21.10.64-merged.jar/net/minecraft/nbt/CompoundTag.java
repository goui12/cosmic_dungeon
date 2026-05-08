package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

public final class CompoundTag implements Tag {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
        .comapFlatMap(
            p_311527_ -> {
                Tag tag = p_311527_.convert(NbtOps.INSTANCE).getValue();
                return tag instanceof CompoundTag compoundtag
                    ? DataResult.success(compoundtag == p_311527_.getValue() ? compoundtag.copy() : compoundtag)
                    : DataResult.error(() -> "Not a compound tag: " + tag);
            },
            p_311526_ -> new Dynamic<>(NbtOps.INSTANCE, p_311526_.copy())
        );
    private static final int SELF_SIZE_IN_BYTES = 48;
    private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
        public CompoundTag load(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            CompoundTag compoundtag;
            try {
                compoundtag = loadCompound(input, accounter);
            } finally {
                accounter.popDepth();
            }

            return compoundtag;
        }

        private static byte readNamedTagType(DataInput p_302338_, NbtAccounter p_302362_) throws IOException {
            p_302362_.accountBytes(2);
            return p_302338_.readByte();
        }

        private static CompoundTag loadCompound(DataInput input, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBytes(48L);
            Map<String, Tag> map = Maps.newHashMap();

            byte b0;
            while((b0 = readNamedTagType(input, nbtAccounter)) != 0) {
                String s = nbtAccounter.readUTF(input.readUTF());
                nbtAccounter.accountBytes(4); //Forge: 4 extra bytes for the object allocation.
                Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b0), s, input, nbtAccounter);
                if (map.put(s, tag) == null) {
                    nbtAccounter.accountBytes(36L);
                }
            }

            return new CompoundTag(map);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
            try {
                streamtagvisitor$valueresult = parseCompound(input, visitor, accounter);
            } finally {
                accounter.popDepth();
            }

            return streamtagvisitor$valueresult;
        }

        private static StreamTagVisitor.ValueResult parseCompound(DataInput input, StreamTagVisitor visitor, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBytes(48L);

            byte b0;
            label35:
            while ((b0 = input.readByte()) != 0) {
                TagType<?> tagtype = TagTypes.getType(b0);
                switch (visitor.visitEntry(tagtype)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        StringTag.skipString(input);
                        tagtype.skip(input, nbtAccounter);
                        break label35;
                    case SKIP:
                        StringTag.skipString(input);
                        tagtype.skip(input, nbtAccounter);
                        break;
                    default:
                        String s = readString(input, nbtAccounter);
                        switch (visitor.visitEntry(tagtype, s)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                                tagtype.skip(input, nbtAccounter);
                                break label35;
                            case SKIP:
                                tagtype.skip(input, nbtAccounter);
                                break;
                            default:
                                nbtAccounter.accountBytes(36L);
                                switch (tagtype.parse(input, visitor, nbtAccounter)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                }
                        }
                }
            }

            if (b0 != 0) {
                while ((b0 = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(b0).skip(input, nbtAccounter);
                }
            }

            return visitor.visitContainerEnd();
        }

        private static String readString(DataInput input, NbtAccounter accounter) throws IOException {
            String s = input.readUTF();
            accounter.accountBytes(28L);
            accounter.accountBytes(2L, s.length());
            return s;
        }

        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            byte b0;
            try {
                while ((b0 = input.readByte()) != 0) {
                    StringTag.skipString(input);
                    TagTypes.getType(b0).skip(input, accounter);
                }
            } finally {
                accounter.popDepth();
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    CompoundTag(Map<String, Tag> tags) {
        this.tags = tags;
    }

    public CompoundTag() {
        this(new HashMap<>());
    }

    /**
     * Neo: create a compound tag that is generally suitable to hold the given amount of entries
     * without needing to resize the internal map.
     *
     * @param expectedEntries the expected number of entries that the compound tag will have
     * @see HashMap#newHashMap(int)
     */
    public CompoundTag(int expectedEntries) {
        this(HashMap.newHashMap(expectedEntries));
    }

    @Override
    public void write(DataOutput output) throws IOException {
        for (String s : this.tags.keySet()) {
            Tag tag = this.tags.get(s);
            writeNamedTag(s, tag, output);
        }

        output.writeByte(0);
    }

    @Override
    public int sizeInBytes() {
        int i = 48;

        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            i += 28 + 2 * entry.getKey().length();
            i += 36;
            i += entry.getValue().sizeInBytes();
        }

        return i;
    }

    public Set<String> keySet() {
        return this.tags.keySet();
    }

    public Set<Entry<String, Tag>> entrySet() {
        return this.tags.entrySet();
    }

    public Collection<Tag> values() {
        return this.tags.values();
    }

    public void forEach(BiConsumer<String, Tag> action) {
        this.tags.forEach(action);
    }

    @Override
    public byte getId() {
        return 10;
    }

    @Override
    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    @Nullable
    public Tag put(String key, Tag value) {
        if (value == null) throw new IllegalArgumentException("Invalid null NBT value with key " + key);
        return this.tags.put(key, value);
    }

    public void putByte(String key, byte value) {
        this.tags.put(key, ByteTag.valueOf(value));
    }

    public void putShort(String key, short value) {
        this.tags.put(key, ShortTag.valueOf(value));
    }

    public void putInt(String key, int value) {
        this.tags.put(key, IntTag.valueOf(value));
    }

    public void putLong(String key, long value) {
        this.tags.put(key, LongTag.valueOf(value));
    }

    public void putFloat(String key, float value) {
        this.tags.put(key, FloatTag.valueOf(value));
    }

    public void putDouble(String key, double value) {
        this.tags.put(key, DoubleTag.valueOf(value));
    }

    public void putString(String key, String value) {
        this.tags.put(key, StringTag.valueOf(value));
    }

    public void putByteArray(String key, byte[] value) {
        this.tags.put(key, new ByteArrayTag(value));
    }

    public void putIntArray(String key, int[] value) {
        this.tags.put(key, new IntArrayTag(value));
    }

    public void putLongArray(String key, long[] value) {
        this.tags.put(key, new LongArrayTag(value));
    }

    public void putBoolean(String key, boolean value) {
        this.tags.put(key, ByteTag.valueOf(value));
    }

    @Nullable
    public Tag get(String key) {
        return this.tags.get(key);
    }

    public boolean contains(String key) {
        return this.tags.containsKey(key);
    }

    private Optional<Tag> getOptional(String key) {
        return Optional.ofNullable(this.tags.get(key));
    }

    public Optional<Byte> getByte(String key) {
        return this.getOptional(key).flatMap(Tag::asByte);
    }

    public byte getByteOr(String key, byte defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.byteValue() : defaultValue;
    }

    public Optional<Short> getShort(String key) {
        return this.getOptional(key).flatMap(Tag::asShort);
    }

    public short getShortOr(String key, short defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.shortValue() : defaultValue;
    }

    public Optional<Integer> getInt(String key) {
        return this.getOptional(key).flatMap(Tag::asInt);
    }

    public int getIntOr(String key, int defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.intValue() : defaultValue;
    }

    public Optional<Long> getLong(String key) {
        return this.getOptional(key).flatMap(Tag::asLong);
    }

    public long getLongOr(String key, long defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.longValue() : defaultValue;
    }

    public Optional<Float> getFloat(String key) {
        return this.getOptional(key).flatMap(Tag::asFloat);
    }

    public float getFloatOr(String key, float defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.floatValue() : defaultValue;
    }

    public Optional<Double> getDouble(String key) {
        return this.getOptional(key).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(String key, double defaultValue) {
        return this.tags.get(key) instanceof NumericTag numerictag ? numerictag.doubleValue() : defaultValue;
    }

    public Optional<String> getString(String key) {
        return this.getOptional(key).flatMap(Tag::asString);
    }

    public String getStringOr(String key, String defaultValue) {
        return this.tags.get(key) instanceof StringTag(String s) ? s : defaultValue;
    }

    public Optional<byte[]> getByteArray(String key) {
        return this.tags.get(key) instanceof ByteArrayTag bytearraytag ? Optional.of(bytearraytag.getAsByteArray()) : Optional.empty();
    }

    public Optional<int[]> getIntArray(String key) {
        return this.tags.get(key) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    public Optional<long[]> getLongArray(String key) {
        return this.tags.get(key) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
    }

    public Optional<CompoundTag> getCompound(String key) {
        return this.tags.get(key) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(String key) {
        return this.getCompound(key).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(String key) {
        return this.tags.get(key) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
    }

    public ListTag getListOrEmpty(String key) {
        return this.getList(key).orElseGet(ListTag::new);
    }

    public Optional<Boolean> getBoolean(String key) {
        return this.getOptional(key).flatMap(Tag::asBoolean);
    }

    public boolean getBooleanOr(String key, boolean defaultValue) {
        return this.getByteOr(key, (byte)(defaultValue ? 1 : 0)) != 0;
    }

    public void remove(String key) {
        this.tags.remove(key);
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitCompound(this);
        return stringtagvisitor.build();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    protected CompoundTag shallowCopy() {
        return new CompoundTag(new HashMap<>(this.tags));
    }

    public CompoundTag copy() {
        HashMap<String, Tag> hashmap = new HashMap<>();
        this.tags.forEach((p_411026_, p_411027_) -> hashmap.put(p_411026_, p_411027_.copy()));
        return new CompoundTag(hashmap);
    }

    @Override
    public Optional<CompoundTag> asCompound() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other ? true : other instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)other).tags);
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String name, Tag tag, DataOutput output) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() != 0) {
            output.writeUTF(name);
            tag.write(output);
        }
    }

    static Tag readNamedTagData(TagType<?> type, String name, DataInput input, NbtAccounter accounter) {
        try {
            return type.load(input, accounter);
        } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
            crashreportcategory.setDetail("Tag name", name);
            crashreportcategory.setDetail("Tag type", type.getName());
            throw new ReportedNbtException(crashreport);
        }
    }

    /**
     * Copies all the tags of {@code other} into this tag, then returns itself.
     * @see #copy()
     */
    public CompoundTag merge(CompoundTag other) {
        for (String s : other.tags.keySet()) {
            Tag tag = other.tags.get(s);
            if (tag instanceof CompoundTag compoundtag && this.tags.get(s) instanceof CompoundTag compoundtag1) {
                compoundtag1.merge(compoundtag);
            } else {
                this.put(s, tag.copy());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitCompound(this);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag tag = entry.getValue();
            TagType<?> tagtype = tag.getType();
            StreamTagVisitor.EntryResult streamtagvisitor$entryresult = visitor.visitEntry(tagtype);
            switch (streamtagvisitor$entryresult) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return visitor.visitContainerEnd();
                case SKIP:
                    break;
                default:
                    streamtagvisitor$entryresult = visitor.visitEntry(tagtype, entry.getKey());
                    switch (streamtagvisitor$entryresult) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return visitor.visitContainerEnd();
                        case SKIP:
                            break;
                        default:
                            StreamTagVisitor.ValueResult streamtagvisitor$valueresult = tag.accept(visitor);
                            switch (streamtagvisitor$valueresult) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return visitor.visitContainerEnd();
                            }
                    }
            }
        }

        return visitor.visitContainerEnd();
    }

    public <T> void store(String key, Codec<T> codec, T data) {
        this.store(key, codec, NbtOps.INSTANCE, data);
    }

    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T data) {
        if (data != null) {
            this.store(key, codec, data);
        }
    }

    public <T> void store(String key, Codec<T> codec, DynamicOps<Tag> ops, T data) {
        this.put(key, codec.encodeStart(ops, data).getOrThrow());
    }

    public <T> void storeNullable(String key, Codec<T> codec, DynamicOps<Tag> ops, @Nullable T data) {
        if (data != null) {
            this.store(key, codec, ops, data);
        }
    }

    public <T> void store(MapCodec<T> mapCodec, T data) {
        this.store(mapCodec, NbtOps.INSTANCE, data);
    }

    public <T> void store(MapCodec<T> mapCodec, DynamicOps<Tag> ops, T data) {
        this.merge((CompoundTag)mapCodec.encoder().encodeStart(ops, data).getOrThrow());
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        return this.read(key, codec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(String key, Codec<T> codec, DynamicOps<Tag> ops) {
        Tag tag = this.get(key);
        return tag == null
            ? Optional.empty()
            : codec.parse(ops, tag).resultOrPartial(p_400883_ -> LOGGER.error("Failed to read field ({}={}): {}", key, tag, p_400883_));
    }

    public <T> Optional<T> read(MapCodec<T> mapCodec) {
        return this.read(mapCodec, NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(MapCodec<T> mapCodec, DynamicOps<Tag> ops) {
        return mapCodec.decode(ops, ops.getMap(this).getOrThrow())
            .resultOrPartial(p_400884_ -> LOGGER.error("Failed to read value ({}): {}", this, p_400884_));
    }
}
