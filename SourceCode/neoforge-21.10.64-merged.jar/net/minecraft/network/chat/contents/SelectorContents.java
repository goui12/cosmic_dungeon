package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

public record SelectorContents(SelectorPattern selector, Optional<Component> separator) implements ComponentContents {
    public static final MapCodec<SelectorContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_359468_ -> p_359468_.group(
                SelectorPattern.CODEC.fieldOf("selector").forGetter(SelectorContents::selector),
                ComponentSerialization.CODEC.optionalFieldOf("separator").forGetter(SelectorContents::separator)
            )
            .apply(p_359468_, SelectorContents::new)
    );

    @Override
    public MapCodec<SelectorContents> codec() {
        return MAP_CODEC;
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack nbtPathPattern, @Nullable Entity entity, int recursionDepth) throws CommandSyntaxException {
        if (nbtPathPattern == null) {
            return Component.empty();
        } else {
            Optional<? extends Component> optional = ComponentUtils.updateForEntity(nbtPathPattern, this.separator, entity, recursionDepth);
            return ComponentUtils.formatList(this.selector.resolved().findEntities(nbtPathPattern), optional, Entity::getDisplayName);
        }
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledContentConsumer, Style style) {
        return styledContentConsumer.accept(style, this.selector.pattern());
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> contentConsumer) {
        return contentConsumer.accept(this.selector.pattern());
    }

    @Override
    public String toString() {
        return "pattern{" + this.selector + "}";
    }
}
