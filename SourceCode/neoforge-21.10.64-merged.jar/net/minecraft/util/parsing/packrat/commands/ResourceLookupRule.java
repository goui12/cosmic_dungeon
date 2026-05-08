package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class ResourceLookupRule<C, V> implements Rule<StringReader, V>, ResourceSuggestion {
    private final NamedRule<StringReader, ResourceLocation> idParser;
    protected final C context;
    private final DelayedException<CommandSyntaxException> error;

    protected ResourceLookupRule(NamedRule<StringReader, ResourceLocation> idParser, C context) {
        this.idParser = idParser;
        this.context = context;
        this.error = DelayedException.create(ResourceLocation.ERROR_INVALID);
    }

    @Nullable
    @Override
    public V parse(ParseState<StringReader> parseState) {
        parseState.input().skipWhitespace();
        int i = parseState.mark();
        ResourceLocation resourcelocation = parseState.parse(this.idParser);
        if (resourcelocation != null) {
            try {
                return this.validateElement(parseState.input(), resourcelocation);
            } catch (Exception exception) {
                parseState.errorCollector().store(i, this, exception);
                return null;
            }
        } else {
            parseState.errorCollector().store(i, this, this.error);
            return null;
        }
    }

    protected abstract V validateElement(ImmutableStringReader reader, ResourceLocation elementType) throws Exception;
}
