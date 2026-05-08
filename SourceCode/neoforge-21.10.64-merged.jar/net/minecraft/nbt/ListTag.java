package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public final class ListTag extends AbstractList<Tag> implements CollectionTag {
    private static final String WRAPPER_MARKER = "";
    private static final int SELF_SIZE_IN_BYTES = 36;
    public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
        public ListTag load(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            ListTag listtag;
            try {
                listtag = loadList(input, accounter);
            } finally {
                accounter.popDepth();
            }

            return listtag;
        }

        private static ListTag loadList(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(36L);
            byte b0 = input.readByte();
            int i = readListCount(input);
            if (b0 == 0 && i > 0) {
                throw new NbtFormatException("Missing type on ListTag");
            } else {
                accounter.accountBytes(4L, i);
                TagType<?> tagtype = TagTypes.getType(b0);
                ListTag listtag = new ListTag(new ArrayList<>(i));

                for (int j = 0; j < i; j++) {
                    listtag.addAndUnwrap(tagtype.load(input, accounter));
                }

                return listtag;
            }
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
            try {
                streamtagvisitor$valueresult = parseList(input, visitor, accounter);
            } finally {
                accounter.popDepth();
            }

            return streamtagvisitor$valueresult;
        }

        private static StreamTagVisitor.ValueResult parseList(DataInput input, StreamTagVisitor visitor, NbtAccounter accounter) throws IOException {
            accounter.accountBytes(36L);
            TagType<?> tagtype = TagTypes.getType(input.readByte());
            int i = readListCount(input);
            switch (visitor.visitList(tagtype, i)) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    tagtype.skip(input, i, accounter);
                    return visitor.visitContainerEnd();
                default:
                    accounter.accountBytes(4L, i);
                    int j = 0;

                    while (true) {
                        label41: {
                            if (j < i) {
                                switch (visitor.visitElement(tagtype, j)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                        tagtype.skip(input, accounter);
                                        break;
                                    case SKIP:
                                        tagtype.skip(input, accounter);
                                        break label41;
                                    default:
                                        switch (tagtype.parse(input, visitor, accounter)) {
                                            case HALT:
                                                return StreamTagVisitor.ValueResult.HALT;
                                            case BREAK:
                                                break;
                                            default:
                                                break label41;
                                        }
                                }
                            }

                            int k = i - 1 - j;
                            if (k > 0) {
                                tagtype.skip(input, k, accounter);
                            }

                            return visitor.visitContainerEnd();
                        }

                        j++;
                    }
            }
        }

        private static int readListCount(DataInput p_428726_) throws IOException {
            int i = p_428726_.readInt();
            if (i < 0) {
                throw new NbtFormatException("ListTag length cannot be negative: " + i);
            } else {
                return i;
            }
        }

        @Override
        public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            accounter.pushDepth();

            try {
                TagType<?> tagtype = TagTypes.getType(input.readByte());
                int i = input.readInt();
                tagtype.skip(input, i, accounter);
            } finally {
                accounter.popDepth();
            }
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public String getPrettyName() {
            return "TAG_List";
        }
    };
    private final List<Tag> list;

    public ListTag() {
        this(new ArrayList<>());
    }

    /**
     * Neo: create a list tag with the given initial capacity.
     *
     * @param initialCapacity the initial capacity of the list tag
     */
    public ListTag(int initialCapacity) {
        this(new ArrayList<>(initialCapacity));
    }

    ListTag(List<Tag> list) {
        this.list = list;
    }

    private static Tag tryUnwrap(CompoundTag p_tag) {
        if (p_tag.size() == 1) {
            Tag tag = p_tag.get("");
            if (tag != null) {
                return tag;
            }
        }

        return p_tag;
    }

    private static boolean isWrapper(CompoundTag tag) {
        return tag.size() == 1 && tag.contains("");
    }

    private static Tag wrapIfNeeded(byte elementType, Tag tag) {
        if (elementType != 10) {
            return tag;
        } else {
            return tag instanceof CompoundTag compoundtag && !isWrapper(compoundtag) ? compoundtag : wrapElement(tag);
        }
    }

    private static CompoundTag wrapElement(Tag tag) {
        return new CompoundTag(Map.of("", tag));
    }

    @Override
    public void write(DataOutput output) throws IOException {
        byte b0 = this.identifyRawElementType();
        output.writeByte(b0);
        output.writeInt(this.list.size());

        for (Tag tag : this.list) {
            wrapIfNeeded(b0, tag).write(output);
        }
    }

    @VisibleForTesting
    byte identifyRawElementType() {
        byte b0 = 0;

        for (Tag tag : this.list) {
            byte b1 = tag.getId();
            if (b0 == 0) {
                b0 = b1;
            } else if (b0 != b1) {
                return 10;
            }
        }

        return b0;
    }

    public void addAndUnwrap(Tag tag) {
        if (tag instanceof CompoundTag compoundtag) {
            this.add(tryUnwrap(compoundtag));
        } else {
            this.add(tag);
        }
    }

    @Override
    public int sizeInBytes() {
        int i = 36;
        i += 4 * this.list.size();

        for (Tag tag : this.list) {
            i += tag.sizeInBytes();
        }

        return i;
    }

    @Override
    public byte getId() {
        return 9;
    }

    @Override
    public TagType<ListTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitList(this);
        return stringtagvisitor.build();
    }

    @Override
    public Tag remove(int index) {
        return this.list.remove(index);
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Optional<CompoundTag> getCompound(int index) {
        return this.getNullable(index) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
    }

    public CompoundTag getCompoundOrEmpty(int index) {
        return this.getCompound(index).orElseGet(CompoundTag::new);
    }

    public Optional<ListTag> getList(int index) {
        return this.getNullable(index) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
    }

    public ListTag getListOrEmpty(int index) {
        return this.getList(index).orElseGet(ListTag::new);
    }

    public Optional<Short> getShort(int index) {
        return this.getOptional(index).flatMap(Tag::asShort);
    }

    public short getShortOr(int index, short defaultValue) {
        return this.getNullable(index) instanceof NumericTag numerictag ? numerictag.shortValue() : defaultValue;
    }

    public Optional<Integer> getInt(int index) {
        return this.getOptional(index).flatMap(Tag::asInt);
    }

    public int getIntOr(int index, int defaultValue) {
        return this.getNullable(index) instanceof NumericTag numerictag ? numerictag.intValue() : defaultValue;
    }

    public Optional<int[]> getIntArray(int index) {
        return this.getNullable(index) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
    }

    public Optional<long[]> getLongArray(int index) {
        return this.getNullable(index) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
    }

    public Optional<Double> getDouble(int index) {
        return this.getOptional(index).flatMap(Tag::asDouble);
    }

    public double getDoubleOr(int index, double defaultValue) {
        return this.getNullable(index) instanceof NumericTag numerictag ? numerictag.doubleValue() : defaultValue;
    }

    public Optional<Float> getFloat(int index) {
        return this.getOptional(index).flatMap(Tag::asFloat);
    }

    public float getFloatOr(int index, float defaultValue) {
        return this.getNullable(index) instanceof NumericTag numerictag ? numerictag.floatValue() : defaultValue;
    }

    public Optional<String> getString(int index) {
        return this.getOptional(index).flatMap(Tag::asString);
    }

    public String getStringOr(int index, String defaultValue) {
        return this.getNullable(index) instanceof StringTag(String s) ? s : defaultValue;
    }

    @Nullable
    private Tag getNullable(int index) {
        return index >= 0 && index < this.list.size() ? this.list.get(index) : null;
    }

    private Optional<Tag> getOptional(int index) {
        return Optional.ofNullable(this.getNullable(index));
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Tag get(int index) {
        return this.list.get(index);
    }

    public Tag set(int p_128760_, Tag p_128761_) {
        return this.list.set(p_128760_, p_128761_);
    }

    public void add(int p_128753_, Tag p_128754_) {
        this.list.add(p_128753_, p_128754_);
    }

    @Override
    public boolean setTag(int index, Tag nbt) {
        this.list.set(index, nbt);
        return true;
    }

    @Override
    public boolean addTag(int index, Tag nbt) {
        this.list.add(index, nbt);
        return true;
    }

    public ListTag copy() {
        List<Tag> list = new ArrayList<>(this.list.size());

        for (Tag tag : this.list) {
            list.add(tag.copy());
        }

        return new ListTag(list);
    }

    @Override
    public Optional<ListTag> asList() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other ? true : other instanceof ListTag && Objects.equals(this.list, ((ListTag)other).list);
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public Stream<Tag> stream() {
        return super.stream();
    }

    public Stream<CompoundTag> compoundStream() {
        return this.stream().mapMulti((p_410763_, p_410503_) -> {
            if (p_410763_ instanceof CompoundTag compoundtag) {
                p_410503_.accept(compoundtag);
            }
        });
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitList(this);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        byte b0 = this.identifyRawElementType();
        switch (visitor.visitList(TagTypes.getType(b0), this.list.size())) {
            case HALT:
                return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
                return visitor.visitContainerEnd();
            default:
                int i = 0;

                while (i < this.list.size()) {
                    Tag tag = wrapIfNeeded(b0, this.list.get(i));
                    switch (visitor.visitElement(tag.getType(), i)) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return visitor.visitContainerEnd();
                        default:
                            switch (tag.accept(visitor)) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return visitor.visitContainerEnd();
                            }
                        case SKIP:
                            i++;
                    }
                }

                return visitor.visitContainerEnd();
        }
    }
}
