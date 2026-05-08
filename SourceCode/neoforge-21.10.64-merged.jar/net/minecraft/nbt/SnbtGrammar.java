package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPatternParseRule;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.NumberRunParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;

public class SnbtGrammar {
    private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new DynamicCommandExceptionType(
        p_410234_ -> Component.translatableEscape("snbt.parser.number_parse_failure", p_410234_)
    );
    static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new DynamicCommandExceptionType(
        p_410336_ -> Component.translatableEscape("snbt.parser.expected_hex_escape", p_410336_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new DynamicCommandExceptionType(
        p_411037_ -> Component.translatableEscape("snbt.parser.invalid_codepoint", p_411037_)
    );
    private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new DynamicCommandExceptionType(
        p_411036_ -> Component.translatableEscape("snbt.parser.no_such_operation", p_411036_)
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_integer_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_float_type"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_non_negative_number"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_character_name"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_array_element_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_unquoted_start"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_unquoted_string"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_string_contents"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_binary_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_UNDESCORE_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.underscore_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_decimal_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_hex_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.empty_key"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.leading_zero_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(
        new SimpleCommandExceptionType(Component.translatable("snbt.parser.infinity_not_allowed"))
    );
    private static final HexFormat HEX_ESCAPE = HexFormat.of().withUpperCase();
    private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_BINARY_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_409763_) {
            return switch (p_409763_) {
                case '0', '1', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_DECIMAL_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_410507_) {
            return switch (p_410507_) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_HEX_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char p_410332_) {
            return switch (p_410332_) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    };
    private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, ERROR_INVALID_STRING_CONTENTS) {
        @Override
        protected boolean isAccepted(char p_410102_) {
            return switch (p_410102_) {
                case '"', '\'', '\\' -> false;
                default -> true;
            };
        }
    };
    private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()) {
        @Override
        protected boolean isAccepted(char p_410066_) {
            return SnbtGrammar.canStartNumber(p_410066_);
        }
    };
    private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

    static DelayedException<CommandSyntaxException> createNumberParseError(NumberFormatException numberFormatException) {
        return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, numberFormatException.getMessage());
    }

    @Nullable
    public static String escapeControlCharacters(char c) {
        return switch (c) {
            case '\b' -> "b";
            case '\t' -> "t";
            case '\n' -> "n";
            default -> c < ' ' ? "x" + HEX_ESCAPE.toHexDigits((byte)c) : null;
            case '\f' -> "f";
            case '\r' -> "r";
        };
    }

    private static boolean isAllowedToStartUnquotedString(char c) {
        return !canStartNumber(c);
    }

    static boolean canStartNumber(char c) {
        return switch (c) {
            case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }

    static boolean needsUnderscoreRemoval(String text) {
        return text.indexOf(95) != -1;
    }

    private static void cleanAndAppend(StringBuilder stringBuilder, String text) {
        cleanAndAppend(stringBuilder, text, needsUnderscoreRemoval(text));
    }

    static void cleanAndAppend(StringBuilder stringBuilder, String text, boolean removeUnderscores) {
        if (removeUnderscores) {
            for (char c0 : text.toCharArray()) {
                if (c0 != '_') {
                    stringBuilder.append(c0);
                }
            }
        } else {
            stringBuilder.append(text);
        }
    }

    static short parseUnsignedShort(String text, int radix) {
        int i = Integer.parseInt(text, radix);
        if (i >> 16 == 0) {
            return (short)i;
        } else {
            throw new NumberFormatException("out of range: " + i);
        }
    }

    @Nullable
    private static <T> T createFloat(
        DynamicOps<T> ops,
        SnbtGrammar.Sign sign,
        @Nullable String wholePart,
        @Nullable String fractionPart,
        @Nullable SnbtGrammar.Signed<String> exponentPart,
        @Nullable SnbtGrammar.TypeSuffix suffix,
        ParseState<?> parseState
    ) {
        StringBuilder stringbuilder = new StringBuilder();
        sign.append(stringbuilder);
        if (wholePart != null) {
            cleanAndAppend(stringbuilder, wholePart);
        }

        if (fractionPart != null) {
            stringbuilder.append('.');
            cleanAndAppend(stringbuilder, fractionPart);
        }

        if (exponentPart != null) {
            stringbuilder.append('e');
            exponentPart.sign().append(stringbuilder);
            cleanAndAppend(stringbuilder, exponentPart.value);
        }

        try {
            String s = stringbuilder.toString();

            return (T)(switch (suffix) {
                case null -> (Object)convertDouble(ops, parseState, s);
                case FLOAT -> (Object)convertFloat(ops, parseState, s);
                case DOUBLE -> (Object)convertDouble(ops, parseState, s);
                default -> {
                    parseState.errorCollector().store(parseState.mark(), ERROR_EXPECTED_FLOAT_TYPE);
                    yield null;
                }
            });
        } catch (NumberFormatException numberformatexception) {
            parseState.errorCollector().store(parseState.mark(), createNumberParseError(numberformatexception));
            return null;
        }
    }

    @Nullable
    private static <T> T convertFloat(DynamicOps<T> ops, ParseState<?> parseState, String value) {
        float f = Float.parseFloat(value);
        if (!Float.isFinite(f)) {
            parseState.errorCollector().store(parseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        } else {
            return ops.createFloat(f);
        }
    }

    @Nullable
    private static <T> T convertDouble(DynamicOps<T> ops, ParseState<?> parseState, String value) {
        double d0 = Double.parseDouble(value);
        if (!Double.isFinite(d0)) {
            parseState.errorCollector().store(parseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        } else {
            return ops.createDouble(d0);
        }
    }

    private static String joinList(List<String> list) {
        return switch (list.size()) {
            case 0 -> "";
            case 1 -> (String)list.getFirst();
            default -> String.join("", list);
        };
    }

    public static <T> Grammar<T> createParser(DynamicOps<T> ops) {
        T t = ops.createBoolean(true);
        T t1 = ops.createBoolean(false);
        T t2 = ops.emptyMap();
        T t3 = ops.emptyList();
        Dictionary<StringReader> dictionary = new Dictionary<>();
        Atom<SnbtGrammar.Sign> atom = Atom.of("sign");
        dictionary.put(
            atom,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('+'), Term.marker(atom, SnbtGrammar.Sign.PLUS)),
                Term.sequence(StringReaderTerms.character('-'), Term.marker(atom, SnbtGrammar.Sign.MINUS))
            ),
            p_410526_ -> p_410526_.getOrThrow(atom)
        );
        Atom<SnbtGrammar.IntegerSuffix> atom1 = Atom.of("integer_suffix");
        dictionary.put(
            atom1,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.characters('u', 'U'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(
                    StringReaderTerms.characters('s', 'S'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.BYTE))),
                Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.SHORT))),
                Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.INT))),
                Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(atom1, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.LONG)))
            ),
            p_409993_ -> p_409993_.getOrThrow(atom1)
        );
        Atom<String> atom2 = Atom.of("binary_numeral");
        dictionary.put(atom2, BINARY_NUMERAL);
        Atom<String> atom3 = Atom.of("decimal_numeral");
        dictionary.put(atom3, DECIMAL_NUMERAL);
        Atom<String> atom4 = Atom.of("hex_numeral");
        dictionary.put(atom4, HEX_NUMERAL);
        Atom<SnbtGrammar.IntegerLiteral> atom5 = Atom.of("integer_literal");
        NamedRule<StringReader, SnbtGrammar.IntegerLiteral> namedrule = dictionary.put(
            atom5,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        StringReaderTerms.character('0'),
                        Term.cut(),
                        Term.alternative(
                            Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), dictionary.named(atom4)),
                            Term.sequence(StringReaderTerms.characters('b', 'B'), dictionary.named(atom2)),
                            Term.sequence(dictionary.named(atom3), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)),
                            Term.marker(atom3, "0")
                        )
                    ),
                    dictionary.named(atom3)
                ),
                Term.optional(dictionary.named(atom1))
            ),
            p_409716_ -> {
                SnbtGrammar.IntegerSuffix snbtgrammar$integersuffix = p_409716_.getOrDefault(atom1, SnbtGrammar.IntegerSuffix.EMPTY);
                SnbtGrammar.Sign snbtgrammar$sign = p_409716_.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
                String s = p_409716_.get(atom3);
                if (s != null) {
                    return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.DECIMAL, s, snbtgrammar$integersuffix);
                } else {
                    String s1 = p_409716_.get(atom4);
                    if (s1 != null) {
                        return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.HEX, s1, snbtgrammar$integersuffix);
                    } else {
                        String s2 = p_409716_.getOrThrow(atom2);
                        return new SnbtGrammar.IntegerLiteral(snbtgrammar$sign, SnbtGrammar.Base.BINARY, s2, snbtgrammar$integersuffix);
                    }
                }
            }
        );
        Atom<SnbtGrammar.TypeSuffix> atom6 = Atom.of("float_type_suffix");
        dictionary.put(
            atom6,
            Term.alternative(
                Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(atom6, SnbtGrammar.TypeSuffix.FLOAT)),
                Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(atom6, SnbtGrammar.TypeSuffix.DOUBLE))
            ),
            p_410191_ -> p_410191_.getOrThrow(atom6)
        );
        Atom<SnbtGrammar.Signed<String>> atom7 = Atom.of("float_exponent_part");
        dictionary.put(
            atom7,
            Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(dictionary.named(atom)), dictionary.named(atom3)),
            p_410211_ -> new SnbtGrammar.Signed<>(p_410211_.getOrDefault(atom, SnbtGrammar.Sign.PLUS), p_410211_.getOrThrow(atom3))
        );
        Atom<String> atom8 = Atom.of("float_whole_part");
        Atom<String> atom9 = Atom.of("float_fraction_part");
        Atom<T> atom10 = Atom.of("float_literal");
        dictionary.putComplex(
            atom10,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        dictionary.namedWithAlias(atom3, atom8),
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        Term.optional(dictionary.namedWithAlias(atom3, atom9)),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        dictionary.namedWithAlias(atom3, atom9),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(dictionary.namedWithAlias(atom3, atom8), dictionary.named(atom7), Term.cut(), Term.optional(dictionary.named(atom6))),
                    Term.sequence(dictionary.namedWithAlias(atom3, atom8), Term.optional(dictionary.named(atom7)), dictionary.named(atom6))
                )
            ),
            p_410250_ -> {
                Scope scope = p_410250_.scope();
                SnbtGrammar.Sign snbtgrammar$sign = scope.getOrDefault(atom, SnbtGrammar.Sign.PLUS);
                String s = scope.get(atom8);
                String s1 = scope.get(atom9);
                SnbtGrammar.Signed<String> signed = scope.get(atom7);
                SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = scope.get(atom6);
                return createFloat(ops, snbtgrammar$sign, s, s1, signed, snbtgrammar$typesuffix, p_410250_);
            }
        );
        Atom<String> atom11 = Atom.of("string_hex_2");
        dictionary.put(atom11, new SnbtGrammar.SimpleHexLiteralParseRule(2));
        Atom<String> atom12 = Atom.of("string_hex_4");
        dictionary.put(atom12, new SnbtGrammar.SimpleHexLiteralParseRule(4));
        Atom<String> atom13 = Atom.of("string_hex_8");
        dictionary.put(atom13, new SnbtGrammar.SimpleHexLiteralParseRule(8));
        Atom<String> atom14 = Atom.of("string_unicode_name");
        dictionary.put(atom14, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
        Atom<String> atom15 = Atom.of("string_escape_sequence");
        dictionary.putComplex(
            atom15,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('b'), Term.marker(atom15, "\b")),
                Term.sequence(StringReaderTerms.character('s'), Term.marker(atom15, " ")),
                Term.sequence(StringReaderTerms.character('t'), Term.marker(atom15, "\t")),
                Term.sequence(StringReaderTerms.character('n'), Term.marker(atom15, "\n")),
                Term.sequence(StringReaderTerms.character('f'), Term.marker(atom15, "\f")),
                Term.sequence(StringReaderTerms.character('r'), Term.marker(atom15, "\r")),
                Term.sequence(StringReaderTerms.character('\\'), Term.marker(atom15, "\\")),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom15, "'")),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom15, "\"")),
                Term.sequence(StringReaderTerms.character('x'), dictionary.named(atom11)),
                Term.sequence(StringReaderTerms.character('u'), dictionary.named(atom12)),
                Term.sequence(StringReaderTerms.character('U'), dictionary.named(atom13)),
                Term.sequence(StringReaderTerms.character('N'), StringReaderTerms.character('{'), dictionary.named(atom14), StringReaderTerms.character('}'))
            ),
            p_411035_ -> {
                Scope scope = p_411035_.scope();
                String s = scope.getAny(atom15);
                if (s != null) {
                    return s;
                } else {
                    String s1 = scope.getAny(atom11, atom12, atom13);
                    if (s1 != null) {
                        int j = HexFormat.fromHexDigits(s1);
                        if (!Character.isValidCodePoint(j)) {
                            p_411035_.errorCollector()
                                .store(p_411035_.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", j)));
                            return null;
                        } else {
                            return Character.toString(j);
                        }
                    } else {
                        String s2 = scope.getOrThrow(atom14);

                        int i;
                        try {
                            i = Character.codePointOf(s2);
                        } catch (IllegalArgumentException illegalargumentexception) {
                            p_411035_.errorCollector().store(p_411035_.mark(), ERROR_INVALID_CHARACTER_NAME);
                            return null;
                        }

                        return Character.toString(i);
                    }
                }
            }
        );
        Atom<String> atom16 = Atom.of("string_plain_contents");
        dictionary.put(atom16, PLAIN_STRING_CHUNK);
        Atom<List<String>> atom17 = Atom.of("string_chunks");
        Atom<String> atom18 = Atom.of("string_contents");
        Atom<String> atom19 = Atom.of("single_quoted_string_chunk");
        NamedRule<StringReader, String> namedrule1 = dictionary.put(
            atom19,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom18, "\""))
            ),
            p_409868_ -> p_409868_.getOrThrow(atom18)
        );
        Atom<String> atom20 = Atom.of("single_quoted_string_contents");
        dictionary.put(atom20, Term.repeated(namedrule1, atom17), p_410457_ -> joinList(p_410457_.getOrThrow(atom17)));
        Atom<String> atom21 = Atom.of("double_quoted_string_chunk");
        NamedRule<StringReader, String> namedrule2 = dictionary.put(
            atom21,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom18, "'"))
            ),
            p_410420_ -> p_410420_.getOrThrow(atom18)
        );
        Atom<String> atom22 = Atom.of("double_quoted_string_contents");
        dictionary.put(atom22, Term.repeated(namedrule2, atom17), p_409731_ -> joinList(p_409731_.getOrThrow(atom17)));
        Atom<String> atom23 = Atom.of("quoted_string_literal");
        dictionary.put(
            atom23,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.character('"'), Term.cut(), Term.optional(dictionary.namedWithAlias(atom22, atom18)), StringReaderTerms.character('"')
                ),
                Term.sequence(StringReaderTerms.character('\''), Term.optional(dictionary.namedWithAlias(atom20, atom18)), StringReaderTerms.character('\''))
            ),
            p_410273_ -> p_410273_.getOrThrow(atom18)
        );
        Atom<String> atom24 = Atom.of("unquoted_string");
        dictionary.put(atom24, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
        Atom<T> atom25 = Atom.of("literal");
        Atom<List<T>> atom26 = Atom.of("arguments");
        dictionary.put(
            atom26,
            Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom26, StringReaderTerms.character(',')),
            p_410707_ -> p_410707_.getOrThrow(atom26)
        );
        Atom<T> atom27 = Atom.of("unquoted_string_or_builtin");
        dictionary.putComplex(
            atom27,
            Term.sequence(
                dictionary.named(atom24),
                Term.optional(Term.sequence(StringReaderTerms.character('('), dictionary.named(atom26), StringReaderTerms.character(')')))
            ),
            p_411043_ -> {
                Scope scope = p_411043_.scope();
                String s = scope.getOrThrow(atom24);
                if (!s.isEmpty() && isAllowedToStartUnquotedString(s.charAt(0))) {
                    List<T> list = scope.get(atom26);
                    if (list != null) {
                        SnbtOperations.BuiltinKey snbtoperations$builtinkey = new SnbtOperations.BuiltinKey(s, list.size());
                        SnbtOperations.BuiltinOperation snbtoperations$builtinoperation = SnbtOperations.BUILTIN_OPERATIONS.get(snbtoperations$builtinkey);
                        if (snbtoperations$builtinoperation != null) {
                            return snbtoperations$builtinoperation.run(ops, list, p_411043_);
                        } else {
                            p_411043_.errorCollector()
                                .store(p_411043_.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, snbtoperations$builtinkey.toString()));
                            return null;
                        }
                    } else if (s.equalsIgnoreCase("true")) {
                        return t;
                    } else {
                        return s.equalsIgnoreCase("false") ? t1 : ops.createString(s);
                    }
                } else {
                    p_411043_.errorCollector().store(p_411043_.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
                    return null;
                }
            }
        );
        Atom<String> atom28 = Atom.of("map_key");
        dictionary.put(atom28, Term.alternative(dictionary.named(atom23), dictionary.named(atom24)), p_410385_ -> p_410385_.getAnyOrThrow(atom23, atom24));
        Atom<Entry<String, T>> atom29 = Atom.of("map_entry");
        NamedRule<StringReader, Entry<String, T>> namedrule3 = dictionary.putComplex(
            atom29, Term.sequence(dictionary.named(atom28), StringReaderTerms.character(':'), dictionary.named(atom25)), p_410737_ -> {
                Scope scope = p_410737_.scope();
                String s = scope.getOrThrow(atom28);
                if (s.isEmpty()) {
                    p_410737_.errorCollector().store(p_410737_.mark(), ERROR_EMPTY_KEY);
                    return null;
                } else {
                    T t4 = scope.getOrThrow(atom25);
                    return Map.entry(s, t4);
                }
            }
        );
        Atom<List<Entry<String, T>>> atom30 = Atom.of("map_entries");
        dictionary.put(
            atom30, Term.repeatedWithTrailingSeparator(namedrule3, atom30, StringReaderTerms.character(',')), p_410706_ -> p_410706_.getOrThrow(atom30)
        );
        Atom<T> atom31 = Atom.of("map_literal");
        dictionary.put(atom31, Term.sequence(StringReaderTerms.character('{'), dictionary.named(atom30), StringReaderTerms.character('}')), p_409658_ -> {
            List<Entry<String, T>> list = p_409658_.getOrThrow(atom30);
            if (list.isEmpty()) {
                return t2;
            } else {
                Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(list.size());

                for (Entry<String, T> entry : list) {
                    builder.put(ops.createString(entry.getKey()), entry.getValue());
                }

                return ops.createMap(builder.buildKeepingLast());
            }
        });
        Atom<List<T>> atom32 = Atom.of("list_entries");
        dictionary.put(
            atom32,
            Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom32, StringReaderTerms.character(',')),
            p_410645_ -> p_410645_.getOrThrow(atom32)
        );
        Atom<SnbtGrammar.ArrayPrefix> atom33 = Atom.of("array_prefix");
        dictionary.put(
            atom33,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('B'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.BYTE)),
                Term.sequence(StringReaderTerms.character('L'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.LONG)),
                Term.sequence(StringReaderTerms.character('I'), Term.marker(atom33, SnbtGrammar.ArrayPrefix.INT))
            ),
            p_409638_ -> p_409638_.getOrThrow(atom33)
        );
        Atom<List<SnbtGrammar.IntegerLiteral>> atom34 = Atom.of("int_array_entries");
        dictionary.put(
            atom34, Term.repeatedWithTrailingSeparator(namedrule, atom34, StringReaderTerms.character(',')), p_411029_ -> p_411029_.getOrThrow(atom34)
        );
        Atom<T> atom35 = Atom.of("list_literal");
        dictionary.putComplex(
            atom35,
            Term.sequence(
                StringReaderTerms.character('['),
                Term.alternative(Term.sequence(dictionary.named(atom33), StringReaderTerms.character(';'), dictionary.named(atom34)), dictionary.named(atom32)),
                StringReaderTerms.character(']')
            ),
            p_410065_ -> {
                Scope scope = p_410065_.scope();
                SnbtGrammar.ArrayPrefix snbtgrammar$arrayprefix = scope.get(atom33);
                if (snbtgrammar$arrayprefix != null) {
                    List<SnbtGrammar.IntegerLiteral> list1 = scope.getOrThrow(atom34);
                    return list1.isEmpty() ? snbtgrammar$arrayprefix.create(ops) : snbtgrammar$arrayprefix.create(ops, list1, p_410065_);
                } else {
                    List<T> list = scope.getOrThrow(atom32);
                    return list.isEmpty() ? t3 : ops.createList(list.stream());
                }
            }
        );
        NamedRule<StringReader, T> namedrule4 = dictionary.putComplex(
            atom25,
            Term.alternative(
                Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(dictionary.namedWithAlias(atom10, atom25), dictionary.named(atom5))),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('"', '\'')), Term.cut(), dictionary.named(atom23)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), dictionary.namedWithAlias(atom31, atom25)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), dictionary.namedWithAlias(atom35, atom25)),
                dictionary.namedWithAlias(atom27, atom25)
            ),
            p_410054_ -> {
                Scope scope = p_410054_.scope();
                String s = scope.get(atom23);
                if (s != null) {
                    return ops.createString(s);
                } else {
                    SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral = scope.get(atom5);
                    return snbtgrammar$integerliteral != null ? snbtgrammar$integerliteral.create(ops, p_410054_) : scope.getOrThrow(atom25);
                }
            }
        );
        return new Grammar<>(dictionary, namedrule4);
    }

    static enum ArrayPrefix {
        BYTE(SnbtGrammar.TypeSuffix.BYTE) {
            private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

            @Override
            public <T> T create(DynamicOps<T> p_410524_) {
                return p_410524_.createByteList(EMPTY_BUFFER);
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_410319_, List<SnbtGrammar.IntegerLiteral> p_410656_, ParseState<?> p_410578_) {
                ByteList bytelist = new ByteArrayList();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_410656_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_410578_);
                    if (number == null) {
                        return null;
                    }

                    bytelist.add(number.byteValue());
                }

                return p_410319_.createByteList(ByteBuffer.wrap(bytelist.toByteArray()));
            }
        },
        INT(SnbtGrammar.TypeSuffix.INT, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT) {
            @Override
            public <T> T create(DynamicOps<T> p_409691_) {
                return p_409691_.createIntList(IntStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_409890_, List<SnbtGrammar.IntegerLiteral> p_410031_, ParseState<?> p_409844_) {
                java.util.stream.IntStream.Builder builder = IntStream.builder();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_410031_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_409844_);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.intValue());
                }

                return p_409890_.createIntList(builder.build());
            }
        },
        LONG(SnbtGrammar.TypeSuffix.LONG, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT, SnbtGrammar.TypeSuffix.INT) {
            @Override
            public <T> T create(DynamicOps<T> p_410179_) {
                return p_410179_.createLongList(LongStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> p_410327_, List<SnbtGrammar.IntegerLiteral> p_409793_, ParseState<?> p_410839_) {
                java.util.stream.LongStream.Builder builder = LongStream.builder();

                for (SnbtGrammar.IntegerLiteral snbtgrammar$integerliteral : p_409793_) {
                    Number number = this.buildNumber(snbtgrammar$integerliteral, p_410839_);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.longValue());
                }

                return p_410327_.createLongList(builder.build());
            }
        };

        private final SnbtGrammar.TypeSuffix defaultType;
        private final Set<SnbtGrammar.TypeSuffix> additionalTypes;

        ArrayPrefix(SnbtGrammar.TypeSuffix defaultType, SnbtGrammar.TypeSuffix... additionalTypes) {
            this.additionalTypes = Set.of(additionalTypes);
            this.defaultType = defaultType;
        }

        public boolean isAllowed(SnbtGrammar.TypeSuffix suffix) {
            return suffix == this.defaultType || this.additionalTypes.contains(suffix);
        }

        public abstract <T> T create(DynamicOps<T> ops);

        @Nullable
        public abstract <T> T create(DynamicOps<T> ops, List<SnbtGrammar.IntegerLiteral> values, ParseState<?> parseState);

        @Nullable
        protected Number buildNumber(SnbtGrammar.IntegerLiteral value, ParseState<?> parseState) {
            SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = this.computeType(value.suffix);
            if (snbtgrammar$typesuffix == null) {
                parseState.errorCollector().store(parseState.mark(), SnbtGrammar.ERROR_INVALID_ARRAY_ELEMENT_TYPE);
                return null;
            } else {
                return (Number)value.create(JavaOps.INSTANCE, snbtgrammar$typesuffix, parseState);
            }
        }

        @Nullable
        private SnbtGrammar.TypeSuffix computeType(SnbtGrammar.IntegerSuffix suffix) {
            SnbtGrammar.TypeSuffix snbtgrammar$typesuffix = suffix.type();
            if (snbtgrammar$typesuffix == null) {
                return this.defaultType;
            } else {
                return !this.isAllowed(snbtgrammar$typesuffix) ? null : snbtgrammar$typesuffix;
            }
        }
    }

    static enum Base {
        BINARY,
        DECIMAL,
        HEX;
    }

    record IntegerLiteral(SnbtGrammar.Sign sign, SnbtGrammar.Base base, String digits, SnbtGrammar.IntegerSuffix suffix) {
        private SnbtGrammar.SignedPrefix signedOrDefault() {
            if (this.suffix.signed != null) {
                return this.suffix.signed;
            } else {
                return switch (this.base) {
                    case BINARY, HEX -> SnbtGrammar.SignedPrefix.UNSIGNED;
                    case DECIMAL -> SnbtGrammar.SignedPrefix.SIGNED;
                };
            }
        }

        private String cleanupDigits(SnbtGrammar.Sign sign) {
            boolean flag = SnbtGrammar.needsUnderscoreRemoval(this.digits);
            if (sign != SnbtGrammar.Sign.MINUS && !flag) {
                return this.digits;
            } else {
                StringBuilder stringbuilder = new StringBuilder();
                sign.append(stringbuilder);
                SnbtGrammar.cleanAndAppend(stringbuilder, this.digits, flag);
                return stringbuilder.toString();
            }
        }

        @Nullable
        public <T> T create(DynamicOps<T> ops, ParseState<?> parseState) {
            return this.create(ops, Objects.requireNonNullElse(this.suffix.type, SnbtGrammar.TypeSuffix.INT), parseState);
        }

        @Nullable
        public <T> T create(DynamicOps<T> ops, SnbtGrammar.TypeSuffix typeSuffix, ParseState<?> parseState) {
            boolean flag = this.signedOrDefault() == SnbtGrammar.SignedPrefix.SIGNED;
            if (!flag && this.sign == SnbtGrammar.Sign.MINUS) {
                parseState.errorCollector().store(parseState.mark(), SnbtGrammar.ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
                return null;
            } else {
                String s = this.cleanupDigits(this.sign);

                int i = switch (this.base) {
                    case BINARY -> 2;
                    case DECIMAL -> 10;
                    case HEX -> 16;
                };

                try {
                    if (flag) {
                        return (T)(switch (typeSuffix) {
                            case BYTE -> (Object)ops.createByte(Byte.parseByte(s, i));
                            case SHORT -> (Object)ops.createShort(Short.parseShort(s, i));
                            case INT -> (Object)ops.createInt(Integer.parseInt(s, i));
                            case LONG -> (Object)ops.createLong(Long.parseLong(s, i));
                            default -> {
                                parseState.errorCollector().store(parseState.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                                yield null;
                            }
                        });
                    } else {
                        return (T)(switch (typeSuffix) {
                            case BYTE -> (Object)ops.createByte(UnsignedBytes.parseUnsignedByte(s, i));
                            case SHORT -> (Object)ops.createShort(SnbtGrammar.parseUnsignedShort(s, i));
                            case INT -> (Object)ops.createInt(Integer.parseUnsignedInt(s, i));
                            case LONG -> (Object)ops.createLong(Long.parseUnsignedLong(s, i));
                            default -> {
                                parseState.errorCollector().store(parseState.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                                yield null;
                            }
                        });
                    }
                } catch (NumberFormatException numberformatexception) {
                    parseState.errorCollector().store(parseState.mark(), SnbtGrammar.createNumberParseError(numberformatexception));
                    return null;
                }
            }
        }
    }

    record IntegerSuffix(@Nullable SnbtGrammar.SignedPrefix signed, @Nullable SnbtGrammar.TypeSuffix type) {
        public static final SnbtGrammar.IntegerSuffix EMPTY = new SnbtGrammar.IntegerSuffix(null, null);
    }

    static enum Sign {
        PLUS,
        MINUS;

        public void append(StringBuilder stringBuilder) {
            if (this == MINUS) {
                stringBuilder.append("-");
            }
        }
    }

    record Signed<T>(SnbtGrammar.Sign sign, T value) {
    }

    static enum SignedPrefix {
        SIGNED,
        UNSIGNED;
    }

    static class SimpleHexLiteralParseRule extends GreedyPredicateParseRule {
        public SimpleHexLiteralParseRule(int minSize) {
            super(minSize, minSize, DelayedException.create(SnbtGrammar.ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(minSize)));
        }

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    }

    static enum TypeSuffix {
        FLOAT,
        DOUBLE,
        BYTE,
        SHORT,
        INT,
        LONG;
    }
}
