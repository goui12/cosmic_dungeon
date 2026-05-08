package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
    public static final MapCodec<EntityDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_442700_ -> p_442700_.group(Codec.STRING.fieldOf("entity").forGetter(EntityDataSource::selectorPattern)).apply(p_442700_, EntityDataSource::new)
    );

    public EntityDataSource(String p_443417_) {
        this(p_443417_, compileSelector(p_443417_));
    }

    @Nullable
    private static EntitySelector compileSelector(String selectorPattern) {
        try {
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(selectorPattern), true);
            return entityselectorparser.parse();
        } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
        }
    }

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack source) throws CommandSyntaxException {
        if (this.compiledSelector != null) {
            List<? extends Entity> list = this.compiledSelector.findEntities(source);
            return list.stream().map(NbtPredicate::getEntityTagToCompare);
        } else {
            return Stream.empty();
        }
    }

    @Override
    public MapCodec<EntityDataSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "entity=" + this.selectorPattern;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
            ? true
            : other instanceof EntityDataSource entitydatasource && this.selectorPattern.equals(entitydatasource.selectorPattern);
    }

    @Override
    public int hashCode() {
        return this.selectorPattern.hashCode();
    }
}
