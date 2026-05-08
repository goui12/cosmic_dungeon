package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public final class NbtUtils {
    private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(p_409141_ -> p_409141_.getIntOr(1, 0))
        .thenComparingInt(p_409129_ -> p_409129_.getIntOr(0, 0))
        .thenComparingInt(p_409149_ -> p_409149_.getIntOr(2, 0));
    private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(p_409131_ -> p_409131_.getDoubleOr(1, 0.0))
        .thenComparingDouble(p_409144_ -> p_409144_.getDoubleOr(0, 0.0))
        .thenComparingDouble(p_409143_ -> p_409143_.getDoubleOr(2, 0.0));
    private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
    public static final String SNBT_DATA_TAG = "data";
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
    private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INDENT = 2;
    private static final int NOT_FOUND = -1;

    private NbtUtils() {
    }

    @VisibleForTesting
    public static boolean compareNbt(@Nullable Tag p_tag, @Nullable Tag other, boolean compareListTag) {
        if (p_tag == other) {
            return true;
        } else if (p_tag == null) {
            return true;
        } else if (other == null) {
            return false;
        } else if (!p_tag.getClass().equals(other.getClass())) {
            return false;
        } else if (p_tag instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = (CompoundTag)other;
            if (compoundtag1.size() < compoundtag.size()) {
                return false;
            } else {
                for (Entry<String, Tag> entry : compoundtag.entrySet()) {
                    Tag tag2 = entry.getValue();
                    if (!compareNbt(tag2, compoundtag1.get(entry.getKey()), compareListTag)) {
                        return false;
                    }
                }

                return true;
            }
        } else if (p_tag instanceof ListTag listtag && compareListTag) {
            ListTag listtag1 = (ListTag)other;
            if (listtag.isEmpty()) {
                return listtag1.isEmpty();
            } else if (listtag1.size() < listtag.size()) {
                return false;
            } else {
                for (Tag tag : listtag) {
                    boolean flag = false;

                    for (Tag tag1 : listtag1) {
                        if (compareNbt(tag, tag1, compareListTag)) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return p_tag.equals(other);
        }
    }

    public static BlockState readBlockState(HolderGetter<Block> blockGetter, CompoundTag tag) {
        Optional<? extends Holder<Block>> optional = tag.read("Name", BLOCK_NAME_CODEC).flatMap(blockGetter::get);
        if (optional.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        } else {
            Block block = optional.get().value();
            BlockState blockstate = block.defaultBlockState();
            Optional<CompoundTag> optional1 = tag.getCompound("Properties");
            if (optional1.isPresent()) {
                StateDefinition<Block, BlockState> statedefinition = block.getStateDefinition();

                for (String s : optional1.get().keySet()) {
                    Property<?> property = statedefinition.getProperty(s);
                    if (property != null) {
                        blockstate = setValueHelper(blockstate, property, s, optional1.get(), tag);
                    }
                }
            }

            return blockstate;
        }
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
        S stateHolder, Property<T> property, String propertyName, CompoundTag propertiesTag, CompoundTag blockStateTag
    ) {
        Optional<T> optional = propertiesTag.getString(propertyName).flatMap(property::getValue);
        if (optional.isPresent()) {
            return stateHolder.setValue(property, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", propertyName, propertiesTag.get(propertyName), blockStateTag);
            return stateHolder;
        }
    }

    public static CompoundTag writeBlockState(BlockState state) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        Map<Property<?>, Comparable<?>> map = state.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    public static CompoundTag writeFluidState(FluidState state) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.FLUID.getKey(state.getType()).toString());
        Map<Property<?>, Comparable<?>> map = state.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    private static <T extends Comparable<T>> String getName(Property<T> property, Comparable<?> value) {
        return property.getName((T)value);
    }

    public static String prettyPrint(Tag tag) {
        return prettyPrint(tag, false);
    }

    public static String prettyPrint(Tag tag, boolean prettyPrintArray) {
        return prettyPrint(new StringBuilder(), tag, 0, prettyPrintArray).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder stringBuilder, Tag tag, int indentLevel, boolean prettyPrintArray) {
        return switch (tag) {
            case PrimitiveTag primitivetag -> stringBuilder.append(primitivetag);
            case EndTag endtag -> stringBuilder;
            case ByteArrayTag bytearraytag -> {
                byte[] abyte = bytearraytag.getAsByteArray();
                int i1 = abyte.length;
                indent(indentLevel, stringBuilder).append("byte[").append(i1).append("] {\n");
                if (prettyPrintArray) {
                    indent(indentLevel + 1, stringBuilder);

                    for (int k1 = 0; k1 < abyte.length; k1++) {
                        if (k1 != 0) {
                            stringBuilder.append(',');
                        }

                        if (k1 % 16 == 0 && k1 / 16 > 0) {
                            stringBuilder.append('\n');
                            if (k1 < abyte.length) {
                                indent(indentLevel + 1, stringBuilder);
                            }
                        } else if (k1 != 0) {
                            stringBuilder.append(' ');
                        }

                        stringBuilder.append(String.format(Locale.ROOT, "0x%02X", abyte[k1] & 255));
                    }
                } else {
                    indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                stringBuilder.append('\n');
                indent(indentLevel, stringBuilder).append('}');
                yield stringBuilder;
            }
            case ListTag listtag -> {
                int l = listtag.size();
                indent(indentLevel, stringBuilder).append("list").append("[").append(l).append("] [");
                if (l != 0) {
                    stringBuilder.append('\n');
                }

                for (int j1 = 0; j1 < l; j1++) {
                    if (j1 != 0) {
                        stringBuilder.append(",\n");
                    }

                    indent(indentLevel + 1, stringBuilder);
                    prettyPrint(stringBuilder, listtag.get(j1), indentLevel + 1, prettyPrintArray);
                }

                if (l != 0) {
                    stringBuilder.append('\n');
                }

                indent(indentLevel, stringBuilder).append(']');
                yield stringBuilder;
            }
            case IntArrayTag intarraytag -> {
                int[] aint = intarraytag.getAsIntArray();
                int l1 = 0;

                for (int i3 : aint) {
                    l1 = Math.max(l1, String.format(Locale.ROOT, "%X", i3).length());
                }

                int j2 = aint.length;
                indent(indentLevel, stringBuilder).append("int[").append(j2).append("] {\n");
                if (prettyPrintArray) {
                    indent(indentLevel + 1, stringBuilder);

                    for (int k2 = 0; k2 < aint.length; k2++) {
                        if (k2 != 0) {
                            stringBuilder.append(',');
                        }

                        if (k2 % 16 == 0 && k2 / 16 > 0) {
                            stringBuilder.append('\n');
                            if (k2 < aint.length) {
                                indent(indentLevel + 1, stringBuilder);
                            }
                        } else if (k2 != 0) {
                            stringBuilder.append(' ');
                        }

                        stringBuilder.append(String.format(Locale.ROOT, "0x%0" + l1 + "X", aint[k2]));
                    }
                } else {
                    indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                stringBuilder.append('\n');
                indent(indentLevel, stringBuilder).append('}');
                yield stringBuilder;
            }
            case CompoundTag compoundtag -> {
                List<String> list = Lists.newArrayList(compoundtag.keySet());
                Collections.sort(list);
                indent(indentLevel, stringBuilder).append('{');
                if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (indentLevel + 1)) {
                    stringBuilder.append('\n');
                    indent(indentLevel + 1, stringBuilder);
                }

                int i2 = list.stream().mapToInt(String::length).max().orElse(0);
                String s = Strings.repeat(" ", i2);

                for (int j = 0; j < list.size(); j++) {
                    if (j != 0) {
                        stringBuilder.append(",\n");
                    }

                    String s1 = list.get(j);
                    indent(indentLevel + 1, stringBuilder).append('"').append(s1).append('"').append(s, 0, s.length() - s1.length()).append(": ");
                    prettyPrint(stringBuilder, compoundtag.get(s1), indentLevel + 1, prettyPrintArray);
                }

                if (!list.isEmpty()) {
                    stringBuilder.append('\n');
                }

                indent(indentLevel, stringBuilder).append('}');
                yield stringBuilder;
            }
            case LongArrayTag longarraytag -> {
                long[] along = longarraytag.getAsLongArray();
                long i = 0L;

                for (long k : along) {
                    i = Math.max(i, (long)String.format(Locale.ROOT, "%X", k).length());
                }

                long l2 = along.length;
                indent(indentLevel, stringBuilder).append("long[").append(l2).append("] {\n");
                if (prettyPrintArray) {
                    indent(indentLevel + 1, stringBuilder);

                    for (int j3 = 0; j3 < along.length; j3++) {
                        if (j3 != 0) {
                            stringBuilder.append(',');
                        }

                        if (j3 % 16 == 0 && j3 / 16 > 0) {
                            stringBuilder.append('\n');
                            if (j3 < along.length) {
                                indent(indentLevel + 1, stringBuilder);
                            }
                        } else if (j3 != 0) {
                            stringBuilder.append(' ');
                        }

                        stringBuilder.append(String.format(Locale.ROOT, "0x%0" + i + "X", along[j3]));
                    }
                } else {
                    indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                stringBuilder.append('\n');
                indent(indentLevel, stringBuilder).append('}');
                yield stringBuilder;
            }
            default -> throw new MatchException(null, null);
        };
    }

    private static StringBuilder indent(int indentLevel, StringBuilder stringBuilder) {
        int i = stringBuilder.lastIndexOf("\n") + 1;
        int j = stringBuilder.length() - i;

        for (int k = 0; k < 2 * indentLevel - j; k++) {
            stringBuilder.append(' ');
        }

        return stringBuilder;
    }

    public static Component toPrettyComponent(Tag tag) {
        return new TextComponentTagVisitor("").visit(tag);
    }

    public static String structureToSnbt(CompoundTag tag) {
        return new SnbtPrinterTagVisitor().visit(packStructureTemplate(tag));
    }

    public static CompoundTag snbtToStructure(String text) throws CommandSyntaxException {
        return unpackStructureTemplate(TagParser.parseCompoundFully(text));
    }

    @VisibleForTesting
    static CompoundTag packStructureTemplate(CompoundTag tag) {
        Optional<ListTag> optional = tag.getList("palettes");
        ListTag listtag;
        if (optional.isPresent()) {
            listtag = optional.get().getListOrEmpty(0);
        } else {
            listtag = tag.getListOrEmpty("palette");
        }

        ListTag listtag1 = listtag.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
        tag.put("palette", listtag1);
        if (optional.isPresent()) {
            ListTag listtag2 = new ListTag();
            optional.get().stream().flatMap(p_409134_ -> p_409134_.asList().stream()).forEach(p_409140_ -> {
                CompoundTag compoundtag = new CompoundTag();

                for (int i = 0; i < p_409140_.size(); i++) {
                    compoundtag.putString(listtag1.getString(i).orElseThrow(), packBlockState(p_409140_.getCompound(i).orElseThrow()));
                }

                listtag2.add(compoundtag);
            });
            tag.put("palettes", listtag2);
        }

        Optional<ListTag> optional1 = tag.getList("entities");
        if (optional1.isPresent()) {
            ListTag listtag3 = optional1.get()
                .compoundStream()
                .sorted(Comparator.comparing(p_409130_ -> p_409130_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR)))
                .collect(Collectors.toCollection(ListTag::new));
            tag.put("entities", listtag3);
        }

        ListTag listtag4 = tag.getList("blocks")
            .stream()
            .flatMap(ListTag::compoundStream)
            .sorted(Comparator.comparing(p_409135_ -> p_409135_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR)))
            .peek(p_409148_ -> p_409148_.putString("state", listtag1.getString(p_409148_.getIntOr("state", 0)).orElseThrow()))
            .collect(Collectors.toCollection(ListTag::new));
        tag.put("data", listtag4);
        tag.remove("blocks");
        return tag;
    }

    @VisibleForTesting
    static CompoundTag unpackStructureTemplate(CompoundTag tag) {
        ListTag listtag = tag.getListOrEmpty("palette");
        Map<String, Tag> map = listtag.stream()
            .flatMap(p_409142_ -> p_409142_.asString().stream())
            .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
        Optional<ListTag> optional = tag.getList("palettes");
        if (optional.isPresent()) {
            tag.put(
                "palettes",
                optional.get()
                    .compoundStream()
                    .map(
                        p_409137_ -> map.keySet()
                            .stream()
                            .map(p_409146_ -> p_409137_.getString(p_409146_).orElseThrow())
                            .map(NbtUtils::unpackBlockState)
                            .collect(Collectors.toCollection(ListTag::new))
                    )
                    .collect(Collectors.toCollection(ListTag::new))
            );
            tag.remove("palette");
        } else {
            tag.put("palette", map.values().stream().collect(Collectors.toCollection(ListTag::new)));
        }

        Optional<ListTag> optional1 = tag.getList("data");
        if (optional1.isPresent()) {
            Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();
            object2intmap.defaultReturnValue(-1);

            for (int i = 0; i < listtag.size(); i++) {
                object2intmap.put(listtag.getString(i).orElseThrow(), i);
            }

            ListTag listtag1 = optional1.get();

            for (int j = 0; j < listtag1.size(); j++) {
                CompoundTag compoundtag = listtag1.getCompound(j).orElseThrow();
                String s = compoundtag.getString("state").orElseThrow();
                int k = object2intmap.getInt(s);
                if (k == -1) {
                    throw new IllegalStateException("Entry " + s + " missing from palette");
                }

                compoundtag.putInt("state", k);
            }

            tag.put("blocks", listtag1);
            tag.remove("data");
        }

        return tag;
    }

    @VisibleForTesting
    static String packBlockState(CompoundTag tag) {
        StringBuilder stringbuilder = new StringBuilder(tag.getString("Name").orElseThrow());
        tag.getCompound("Properties")
            .ifPresent(
                p_409133_ -> {
                    String s = p_409133_.entrySet()
                        .stream()
                        .sorted(Entry.comparingByKey())
                        .map(p_409128_ -> p_409128_.getKey() + ":" + p_409128_.getValue().asString().orElseThrow())
                        .collect(Collectors.joining(","));
                    stringbuilder.append('{').append(s).append('}');
                }
            );
        return stringbuilder.toString();
    }

    @VisibleForTesting
    static CompoundTag unpackBlockState(String blockStateText) {
        CompoundTag compoundtag = new CompoundTag();
        int i = blockStateText.indexOf(123);
        String s;
        if (i >= 0) {
            s = blockStateText.substring(0, i);
            CompoundTag compoundtag1 = new CompoundTag();
            if (i + 2 <= blockStateText.length()) {
                String s1 = blockStateText.substring(i + 1, blockStateText.indexOf(125, i));
                COMMA_SPLITTER.split(s1).forEach(p_178040_ -> {
                    List<String> list = COLON_SPLITTER.splitToList(p_178040_);
                    if (list.size() == 2) {
                        compoundtag1.putString(list.get(0), list.get(1));
                    } else {
                        LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", blockStateText);
                    }
                });
                compoundtag.put("Properties", compoundtag1);
            }
        } else {
            s = blockStateText;
        }

        compoundtag.putString("Name", s);
        return compoundtag;
    }

    public static CompoundTag addCurrentDataVersion(CompoundTag tag) {
        int i = SharedConstants.getCurrentVersion().dataVersion().version();
        return addDataVersion(tag, i);
    }

    public static CompoundTag addDataVersion(CompoundTag tag, int dataVersion) {
        tag.putInt("DataVersion", dataVersion);
        return tag;
    }

    public static Dynamic<Tag> addCurrentDataVersion(Dynamic<Tag> tag) {
        int i = SharedConstants.getCurrentVersion().dataVersion().version();
        return addDataVersion(tag, i);
    }

    public static Dynamic<Tag> addDataVersion(Dynamic<Tag> tag, int dataVersion) {
        return tag.set("DataVersion", tag.createInt(dataVersion));
    }

    public static void addCurrentDataVersion(ValueOutput output) {
        int i = SharedConstants.getCurrentVersion().dataVersion().version();
        addDataVersion(output, i);
    }

    public static void addDataVersion(ValueOutput output, int dataVersion) {
        output.putInt("DataVersion", dataVersion);
    }

    public static int getDataVersion(CompoundTag tag, int defaultValue) {
        return tag.getIntOr("DataVersion", defaultValue);
    }

    public static int getDataVersion(Dynamic<?> tag, int defaultValue) {
        return tag.get("DataVersion").asInt(defaultValue);
    }
}
