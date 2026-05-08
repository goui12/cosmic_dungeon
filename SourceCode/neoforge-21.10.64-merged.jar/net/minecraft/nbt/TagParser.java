package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class TagParser<T> {
    public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType(Component.translatable("argument.nbt.trailing"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new SimpleCommandExceptionType(
        Component.translatable("argument.nbt.expected.compound")
    );
    public static final char ELEMENT_SEPARATOR = ',';
    public static final char NAME_VALUE_SEPARATOR = ':';
    private static final TagParser<Tag> NBT_OPS_PARSER = create(NbtOps.INSTANCE);
    public static final Codec<CompoundTag> FLATTENED_CODEC = Codec.STRING
        .comapFlatMap(
            p_409150_ -> {
                try {
                    Tag tag = NBT_OPS_PARSER.parseFully(p_409150_);
                    return tag instanceof CompoundTag compoundtag
                        ? DataResult.success(compoundtag, Lifecycle.stable())
                        : DataResult.error(() -> "Expected compound tag, got " + tag);
                } catch (CommandSyntaxException commandsyntaxexception) {
                    return DataResult.error(commandsyntaxexception::getMessage);
                }
            },
            CompoundTag::toString
        );
    public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
    private final DynamicOps<T> ops;
    private final Grammar<T> grammar;

    private TagParser(DynamicOps<T> ops, Grammar<T> grammar) {
        this.ops = ops;
        this.grammar = grammar;
    }

    public DynamicOps<T> getOps() {
        return this.ops;
    }

    public static <T> TagParser<T> create(DynamicOps<T> ops) {
        return new TagParser<>(ops, SnbtGrammar.createParser(ops));
    }

    private static CompoundTag castToCompoundOrThrow(StringReader reader, Tag tag) throws CommandSyntaxException {
        if (tag instanceof CompoundTag compoundtag) {
            return compoundtag;
        } else {
            throw ERROR_EXPECTED_COMPOUND.createWithContext(reader);
        }
    }

    public static CompoundTag parseCompoundFully(String data) throws CommandSyntaxException {
        StringReader stringreader = new StringReader(data);
        return castToCompoundOrThrow(stringreader, NBT_OPS_PARSER.parseFully(stringreader));
    }

    public T parseFully(String text) throws CommandSyntaxException {
        return this.parseFully(new StringReader(text));
    }

    public T parseFully(StringReader reader) throws CommandSyntaxException {
        T t = this.grammar.parseForCommands(reader);
        reader.skipWhitespace();
        if (reader.canRead()) {
            throw ERROR_TRAILING_DATA.createWithContext(reader);
        } else {
            return t;
        }
    }

    public T parseAsArgument(StringReader reader) throws CommandSyntaxException {
        return this.grammar.parseForCommands(reader);
    }

    public static CompoundTag parseCompoundAsArgument(StringReader reader) throws CommandSyntaxException {
        Tag tag = NBT_OPS_PARSER.parseAsArgument(reader);
        return castToCompoundOrThrow(reader, tag);
    }
}
