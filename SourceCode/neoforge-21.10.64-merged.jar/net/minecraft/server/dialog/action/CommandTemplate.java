package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;

public record CommandTemplate(ParsedTemplate template) implements Action {
    public static final MapCodec<CommandTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_428549_ -> p_428549_.group(ParsedTemplate.CODEC.fieldOf("template").forGetter(CommandTemplate::template)).apply(p_428549_, CommandTemplate::new)
    );

    @Override
    public MapCodec<CommandTemplate> codec() {
        return MAP_CODEC;
    }

    @Override
    public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> p_428383_) {
        String s = this.template.instantiate(Action.ValueGetter.getAsTemplateSubstitutions(p_428383_));
        return Optional.of(new ClickEvent.RunCommand(s));
    }
}
