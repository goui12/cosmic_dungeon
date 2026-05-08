package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

public class AppendLoot implements RuleBlockEntityModifier {
    public static final MapCodec<AppendLoot> CODEC = RecordCodecBuilder.mapCodec(
        p_404616_ -> p_404616_.group(LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(p_335311_ -> p_335311_.lootTable)).apply(p_404616_, AppendLoot::new)
    );
    private final ResourceKey<LootTable> lootTable;

    public AppendLoot(ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag tag) {
        CompoundTag compoundtag = tag == null ? new CompoundTag() : tag.copy();
        compoundtag.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
        compoundtag.putLong("LootTableSeed", random.nextLong());
        return compoundtag;
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_LOOT;
    }
}
