package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public class AdventureModePredicate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<AdventureModePredicate> CODEC = ExtraCodecs.compactListCodec(
            BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())
        )
        .xmap(AdventureModePredicate::new, p_331329_ -> p_331329_.predicates);
    public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
        BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p_331439_ -> p_331439_.predicates, AdventureModePredicate::new
    );
    public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
    public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
    private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
    private final List<BlockPredicate> predicates;
    @Nullable
    private List<Component> cachedTooltip;
    @Nullable
    private BlockInWorld lastCheckedBlock;
    private boolean lastResult;
    private boolean checksBlockEntity;

    public AdventureModePredicate(List<BlockPredicate> predicates) {
        this.predicates = predicates;
    }

    private static boolean areSameBlocks(BlockInWorld first, @Nullable BlockInWorld second, boolean checkNbt) {
        if (second == null || first.getState() != second.getState()) {
            return false;
        } else if (!checkNbt) {
            return true;
        } else if (first.getEntity() == null && second.getEntity() == null) {
            return true;
        } else if (first.getEntity() != null && second.getEntity() != null) {
            boolean flag;
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                RegistryAccess registryaccess = first.getLevel().registryAccess();
                CompoundTag compoundtag = saveBlockEntity(first.getEntity(), registryaccess, problemreporter$scopedcollector);
                CompoundTag compoundtag1 = saveBlockEntity(second.getEntity(), registryaccess, problemreporter$scopedcollector);
                flag = Objects.equals(compoundtag, compoundtag1);
            }

            return flag;
        } else {
            return false;
        }
    }

    private static CompoundTag saveBlockEntity(BlockEntity blockEntity, RegistryAccess registryAccess, ProblemReporter problemReporter) {
        TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemReporter.forChild(blockEntity.problemPath()), registryAccess);
        blockEntity.saveWithId(tagvalueoutput);
        return tagvalueoutput.buildResult();
    }

    public boolean test(BlockInWorld block) {
        if (areSameBlocks(block, this.lastCheckedBlock, this.checksBlockEntity)) {
            return this.lastResult;
        } else {
            this.lastCheckedBlock = block;
            this.checksBlockEntity = false;

            for (BlockPredicate blockpredicate : this.predicates) {
                if (blockpredicate.matches(block)) {
                    this.checksBlockEntity = this.checksBlockEntity | blockpredicate.requiresNbt();
                    this.lastResult = true;
                    return true;
                }
            }

            this.lastResult = false;
            return false;
        }
    }

    private List<Component> tooltip() {
        if (this.cachedTooltip == null) {
            this.cachedTooltip = computeTooltip(this.predicates);
        }

        return this.cachedTooltip;
    }

    public void addToTooltip(Consumer<Component> tooltipAdder) {
        this.tooltip().forEach(tooltipAdder);
    }

    private static List<Component> computeTooltip(List<BlockPredicate> predicates) {
        for (BlockPredicate blockpredicate : predicates) {
            if (blockpredicate.blocks().isEmpty()) {
                return List.of(UNKNOWN_USE);
            }
        }

        return predicates.stream()
            .flatMap(p_330395_ -> p_330395_.blocks().orElseThrow().stream())
            .distinct()
            .map(p_331662_ -> (Component)p_331662_.value().getName().withStyle(ChatFormatting.DARK_GRAY))
            .toList();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            return other instanceof AdventureModePredicate adventuremodepredicate ? this.predicates.equals(adventuremodepredicate.predicates) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.predicates.hashCode();
    }

    @Override
    public String toString() {
        return "AdventureModePredicate{predicates=" + this.predicates + "}";
    }
}
