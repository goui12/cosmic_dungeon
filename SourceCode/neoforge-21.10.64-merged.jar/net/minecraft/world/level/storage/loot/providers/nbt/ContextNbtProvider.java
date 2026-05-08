package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A NbtProvider that provides either the {@linkplain LootContextParams#BLOCK_ENTITY block entity}'s NBT data or an entity's NBT data based on an {@link LootContext.EntityTarget}.
 */
public class ContextNbtProvider implements NbtProvider {
    private static final ExtraCodecs.LateBoundIdMapper<String, ContextNbtProvider.Source<?>> SOURCES = new ExtraCodecs.LateBoundIdMapper<>();
    private static final Codec<ContextNbtProvider.Source<?>> GETTER_CODEC;
    public static final MapCodec<ContextNbtProvider> MAP_CODEC;
    public static final Codec<ContextNbtProvider> INLINE_CODEC;
    private final ContextNbtProvider.Source<?> source;

    private ContextNbtProvider(ContextNbtProvider.Source<?> source) {
        this.source = source;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.CONTEXT;
    }

    @Nullable
    @Override
    public Tag get(LootContext lootContext) {
        return this.source.get(lootContext);
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    public static NbtProvider forContextEntity(LootContext.EntityTarget entityTarget) {
        return new ContextNbtProvider(new ContextNbtProvider.EntitySource(entityTarget.getParam()));
    }

    static {
        for (LootContext.EntityTarget lootcontext$entitytarget : LootContext.EntityTarget.values()) {
            SOURCES.put(lootcontext$entitytarget.getSerializedName(), new ContextNbtProvider.EntitySource(lootcontext$entitytarget.getParam()));
        }

        for (LootContext.BlockEntityTarget lootcontext$blockentitytarget : LootContext.BlockEntityTarget.values()) {
            SOURCES.put(lootcontext$blockentitytarget.getSerializedName(), new ContextNbtProvider.BlockEntitySource(lootcontext$blockentitytarget.getParam()));
        }

        GETTER_CODEC = SOURCES.codec(Codec.STRING);
        MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_298866_ -> p_298866_.group(GETTER_CODEC.fieldOf("target").forGetter(p_450913_ -> p_450913_.source)).apply(p_298866_, ContextNbtProvider::new)
        );
        INLINE_CODEC = GETTER_CODEC.xmap(ContextNbtProvider::new, p_450912_ -> p_450912_.source);
    }

    record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements ContextNbtProvider.Source<BlockEntity> {
        public Tag get(BlockEntity p_451111_) {
            return p_451111_.saveWithFullMetadata(p_451111_.getLevel().registryAccess());
        }
    }

    record EntitySource(ContextKey<? extends Entity> contextParam) implements ContextNbtProvider.Source<Entity> {
        public Tag get(Entity p_451381_) {
            return NbtPredicate.getEntityTagToCompare(p_451381_);
        }
    }

    interface Source<T> {
        ContextKey<? extends T> contextParam();

        @Nullable
        Tag get(T value);

        @Nullable
        default Tag get(LootContext context) {
            T t = context.getOptionalParameter((ContextKey<T>)this.contextParam());
            return t != null ? this.get(t) : null;
        }
    }
}
