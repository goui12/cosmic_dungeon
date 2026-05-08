package net.minecraft.server.dialog.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.functions.StringTemplate;

public class ParsedTemplate {
    public static final Codec<ParsedTemplate> CODEC = Codec.STRING.comapFlatMap(ParsedTemplate::parse, p_428259_ -> p_428259_.raw);
    public static final Codec<String> VARIABLE_CODEC = Codec.STRING
        .validate(
            p_428384_ -> StringTemplate.isValidVariableName(p_428384_)
                ? DataResult.success(p_428384_)
                : DataResult.error(() -> p_428384_ + " is not a valid input name")
        );
    private final String raw;
    private final StringTemplate parsed;

    private ParsedTemplate(String raw, StringTemplate parsed) {
        this.raw = raw;
        this.parsed = parsed;
    }

    private static DataResult<ParsedTemplate> parse(String input) {
        StringTemplate stringtemplate;
        try {
            stringtemplate = StringTemplate.fromString(input);
        } catch (Exception exception) {
            return DataResult.error(() -> "Failed to parse template " + input + ": " + exception.getMessage());
        }

        return DataResult.success(new ParsedTemplate(input, stringtemplate));
    }

    public String instantiate(Map<String, String> variables) {
        List<String> list = this.parsed.variables().stream().map(p_428551_ -> variables.getOrDefault(p_428551_, "")).toList();
        return this.parsed.substitute(list);
    }
}
