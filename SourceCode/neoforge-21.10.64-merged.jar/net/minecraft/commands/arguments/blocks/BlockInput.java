package net.minecraft.commands.arguments.blocks;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public class BlockInput implements Predicate<BlockInWorld> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockState state;
    private final Set<Property<?>> properties;
    @Nullable
    private final CompoundTag tag;

    public BlockInput(BlockState state, Set<Property<?>> properties, @Nullable CompoundTag tag) {
        this.state = state;
        this.properties = properties;
        this.tag = tag;
    }

    public BlockState getState() {
        return this.state;
    }

    public Set<Property<?>> getDefinedProperties() {
        return this.properties;
    }

    public boolean test(BlockInWorld block) {
        BlockState blockstate = block.getState();
        if (!blockstate.is(this.state.getBlock())) {
            return false;
        } else {
            for (Property<?> property : this.properties) {
                if (blockstate.getValue(property) != this.state.getValue(property)) {
                    return false;
                }
            }

            if (this.tag == null) {
                return true;
            } else {
                BlockEntity blockentity = block.getEntity();
                return blockentity != null && NbtUtils.compareNbt(this.tag, blockentity.saveWithFullMetadata(block.getLevel().registryAccess()), true);
            }
        }
    }

    public boolean test(ServerLevel level, BlockPos pos) {
        return this.test(new BlockInWorld(level, pos, false));
    }

    public boolean place(ServerLevel level, BlockPos pos, int flags) {
        BlockState blockstate = (flags & 16) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, level, pos);
        if (blockstate.isAir()) {
            blockstate = this.state;
        }

        blockstate = this.overwriteWithDefinedProperties(blockstate);
        boolean flag = false;
        if (level.setBlock(pos, blockstate, flags)) {
            flag = true;
        }

        if (this.tag != null) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity != null) {
                try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                    HolderLookup.Provider holderlookup$provider = level.registryAccess();
                    ProblemReporter problemreporter = problemreporter$scopedcollector.forChild(blockentity.problemPath());
                    TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter.forChild(() -> "(before)"), holderlookup$provider);
                    blockentity.saveWithoutMetadata(tagvalueoutput);
                    CompoundTag compoundtag = tagvalueoutput.buildResult();
                    blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, holderlookup$provider, this.tag));
                    TagValueOutput tagvalueoutput1 = TagValueOutput.createWithContext(problemreporter.forChild(() -> "(after)"), holderlookup$provider);
                    blockentity.saveWithoutMetadata(tagvalueoutput1);
                    CompoundTag compoundtag1 = tagvalueoutput1.buildResult();
                    if (!compoundtag1.equals(compoundtag)) {
                        flag = true;
                        blockentity.setChanged();
                        level.getChunkSource().blockChanged(pos);
                    }
                }
            }
        }

        return flag;
    }

    private BlockState overwriteWithDefinedProperties(BlockState state) {
        if (state == this.state) {
            return state;
        } else {
            for (Property<?> property : this.properties) {
                state = copyProperty(state, this.state, property);
            }

            return state;
        }
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target, Property<T> property) {
        return source.trySetValue(property, target.getValue(property));
    }
}
