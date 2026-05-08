package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface CommandArgumentParser<T> {
    T parseForCommands(StringReader reader) throws CommandSyntaxException;

    CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder builder);

    default <S> CommandArgumentParser<S> mapResult(final Function<T, S> mapper) {
        return new CommandArgumentParser<S>() {
            @Override
            public S parseForCommands(StringReader p_410798_) throws CommandSyntaxException {
                return mapper.apply((T)CommandArgumentParser.this.parseForCommands(p_410798_));
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_409972_) {
                return CommandArgumentParser.this.parseForSuggestions(p_409972_);
            }
        };
    }

    default <T, O> CommandArgumentParser<T> withCodec(
        final DynamicOps<O> ops, final CommandArgumentParser<O> parser, final Codec<T> codec, final DynamicCommandExceptionType error
    ) {
        return new CommandArgumentParser<T>() {
            @Override
            public T parseForCommands(StringReader p_409591_) throws CommandSyntaxException {
                int i = p_409591_.getCursor();
                O o = parser.parseForCommands(p_409591_);
                DataResult<T> dataresult = codec.parse(ops, o);
                return dataresult.getOrThrow(p_409985_ -> {
                    p_409591_.setCursor(i);
                    return error.createWithContext(p_409591_, p_409985_);
                });
            }

            @Override
            public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_410111_) {
                return CommandArgumentParser.this.parseForSuggestions(p_410111_);
            }
        };
    }
}
