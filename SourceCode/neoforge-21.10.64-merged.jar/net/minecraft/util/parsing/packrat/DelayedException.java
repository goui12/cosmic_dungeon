package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
    T create(String message, int cursor);

    static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType exception) {
        return (p_410118_, p_410691_) -> exception.createWithContext(StringReaderTerms.createReader(p_410118_, p_410691_));
    }

    static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType exception, String argument) {
        return (p_410506_, p_410266_) -> exception.createWithContext(StringReaderTerms.createReader(p_410506_, p_410266_), argument);
    }
}
