package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;

public class SnbtOperations {
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_STRING_UUID = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_string_uuid"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NUMBER_OR_BOOLEAN = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_number_or_boolean"))
    );
    public static final String BUILTIN_TRUE = "true";
    public static final String BUILTIN_FALSE = "false";
    public static final Map<SnbtOperations.BuiltinKey, SnbtOperations.BuiltinOperation> BUILTIN_OPERATIONS = Map.of(
        new SnbtOperations.BuiltinKey("bool", 1), new SnbtOperations.BuiltinOperation() {
            @Override
            public <T> T run(DynamicOps<T> ops, List<T> args, ParseState<StringReader> parseState) {
                Boolean obool = convert(ops, args.getFirst());
                if (obool == null) {
                    parseState.errorCollector().store(parseState.mark(), SnbtOperations.ERROR_EXPECTED_NUMBER_OR_BOOLEAN);
                    return null;
                } else {
                    return ops.createBoolean(obool);
                }
            }

            @Nullable
            private static <T> Boolean convert(DynamicOps<T> ops, T value) {
                Optional<Boolean> optional = ops.getBooleanValue(value).result();
                if (optional.isPresent()) {
                    return optional.get();
                } else {
                    Optional<Number> optional1 = ops.getNumberValue(value).result();
                    return optional1.isPresent() ? optional1.get().doubleValue() != 0.0 : null;
                }
            }
        }, new SnbtOperations.BuiltinKey("uuid", 1), new SnbtOperations.BuiltinOperation() {
            @Override
            public <T> T run(DynamicOps<T> p_411071_, List<T> p_411073_, ParseState<StringReader> p_411067_) {
                Optional<String> optional = p_411071_.getStringValue(p_411073_.getFirst()).result();
                if (optional.isEmpty()) {
                    p_411067_.errorCollector().store(p_411067_.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
                    return null;
                } else {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(optional.get());
                    } catch (IllegalArgumentException illegalargumentexception) {
                        p_411067_.errorCollector().store(p_411067_.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
                        return null;
                    }

                    return p_411071_.createIntList(IntStream.of(UUIDUtil.uuidToIntArray(uuid)));
                }
            }
        }
    );
    public static final SuggestionSupplier<StringReader> BUILTIN_IDS = new SuggestionSupplier<StringReader>() {
        private final Set<String> keys = Stream.concat(
                Stream.of("false", "true"), SnbtOperations.BUILTIN_OPERATIONS.keySet().stream().map(SnbtOperations.BuiltinKey::id)
            )
            .collect(Collectors.toSet());

        @Override
        public Stream<String> possibleValues(ParseState<StringReader> p_411084_) {
            return this.keys.stream();
        }
    };

    public record BuiltinKey(String id, int argCount) {
        @Override
        public String toString() {
            return this.id + "/" + this.argCount;
        }
    }

    public interface BuiltinOperation {
        @Nullable
        <T> T run(DynamicOps<T> ops, List<T> args, ParseState<StringReader> parseState);
    }
}
