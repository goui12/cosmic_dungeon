package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class NumberRunParseRule implements Rule<StringReader, String> {
    private final DelayedException<CommandSyntaxException> noValueError;
    private final DelayedException<CommandSyntaxException> underscoreNotAllowedError;

    public NumberRunParseRule(DelayedException<CommandSyntaxException> noValueError, DelayedException<CommandSyntaxException> underscoreNotAllowedError) {
        this.noValueError = noValueError;
        this.underscoreNotAllowedError = underscoreNotAllowedError;
    }

    @Nullable
    public String parse(ParseState<StringReader> parseState) {
        StringReader stringreader = parseState.input();
        stringreader.skipWhitespace();
        String s = stringreader.getString();
        int i = stringreader.getCursor();
        int j = i;

        while (j < s.length() && this.isAccepted(s.charAt(j))) {
            j++;
        }

        int k = j - i;
        if (k == 0) {
            parseState.errorCollector().store(parseState.mark(), this.noValueError);
            return null;
        } else if (s.charAt(i) != '_' && s.charAt(j - 1) != '_') {
            stringreader.setCursor(j);
            return s.substring(i, j);
        } else {
            parseState.errorCollector().store(parseState.mark(), this.underscoreNotAllowedError);
            return null;
        }
    }

    protected abstract boolean isAccepted(char c);
}
