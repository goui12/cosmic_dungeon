package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public abstract class BlockEntity extends net.neoforged.neoforge.attachment.AttachmentHolder implements DebugValueSource, net.neoforged.neoforge.common.extensions.IBlockEntityExtension {
    private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated // Neo: always use getType()
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;
    @Nullable
    private CompoundTag customPersistentData;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this.type = type;
        this.worldPosition = pos.immutable();
        this.validateBlockState(blockState);
        this.blockState = blockState;
    }

    private void validateBlockState(BlockState state) {
        if (!this.isValidBlockState(state)) {
            throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + state);
        }
    }

    public boolean isValidBlockState(BlockState state) {
        return this.getType().isValid(state); // Neo: use getter so correct type is checked for modded subclasses
    }

    public static BlockPos getPosFromTag(ChunkPos chunkPos, CompoundTag tag) {
        int i = tag.getIntOr("x", 0);
        int j = tag.getIntOr("y", 0);
        int k = tag.getIntOr("z", 0);
        int l = SectionPos.blockToSectionCoord(i);
        int i1 = SectionPos.blockToSectionCoord(k);
        if (l != chunkPos.x || i1 != chunkPos.z) {
            LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", tag, chunkPos);
            i = chunkPos.getBlockX(SectionPos.sectionRelative(i));
            k = chunkPos.getBlockZ(SectionPos.sectionRelative(k));
        }

        return new BlockPos(i, j, k);
    }

    @Nullable
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    protected void loadAdditional(ValueInput input) {
        input.read("NeoForgeData", CompoundTag.CODEC).ifPresent(neoData -> this.customPersistentData = neoData);
        input.child(ATTACHMENTS_NBT_KEY).ifPresent(attachments -> this.deserializeAttachments(attachments));
    }

    public final void loadWithComponents(ValueInput input) {
        this.loadAdditional(input);
        this.components = input.read("components", DataComponentMap.CODEC).orElse(DataComponentMap.EMPTY);
    }

    public final void loadCustomOnly(ValueInput input) {
        this.loadAdditional(input);
    }

    protected void saveAdditional(ValueOutput output) {
        if (this.customPersistentData != null) output.store("NeoForgeData", CompoundTag.CODEC, this.customPersistentData.copy());
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        var attachments = output.child(ATTACHMENTS_NBT_KEY);
        serializeAttachments(attachments);
        if (attachments.isEmpty()) output.discard(ATTACHMENTS_NBT_KEY);
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider registries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, registries);
            this.saveWithFullMetadata(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveWithFullMetadata(ValueOutput output) {
        this.saveWithoutMetadata(output);
        this.saveMetadata(output);
    }

    public void saveWithId(ValueOutput output) {
        this.saveWithoutMetadata(output);
        this.saveId(output);
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, registries);
            this.saveWithoutMetadata(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveWithoutMetadata(ValueOutput output) {
        this.saveAdditional(output);
        output.store("components", DataComponentMap.CODEC, this.components);
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider registries) {
        CompoundTag compoundtag;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, registries);
            this.saveCustomOnly(tagvalueoutput);
            compoundtag = tagvalueoutput.buildResult();
        }

        return compoundtag;
    }

    public void saveCustomOnly(ValueOutput output) {
        this.saveAdditional(output);
    }

    private void saveId(ValueOutput output) {
        addEntityType(output, this.getType());
    }

    public static void addEntityType(ValueOutput output, BlockEntityType<?> entityType) {
        output.store("id", TYPE_CODEC, entityType);
    }

    private void saveMetadata(ValueOutput output) {
        this.saveId(output);
        output.putInt("x", this.worldPosition.getX());
        output.putInt("y", this.worldPosition.getY());
        output.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos pos, BlockState state, CompoundTag tag, HolderLookup.Provider registries) {
        BlockEntityType<?> blockentitytype = tag.read("id", TYPE_CODEC).orElse(null);
        if (blockentitytype == null) {
            LOGGER.error("Skipping block entity with invalid type: {}", tag.get("id"));
            return null;
        } else {
            BlockEntity blockentity;
            try {
                blockentity = blockentitytype.create(pos, state);
            } catch (Throwable throwable2) {
                LOGGER.error("Failed to create block entity {} for block {} at position {} ", blockentitytype, pos, state, throwable2);
                return null;
            }

            try {
                BlockEntity blockentity1;
                try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(blockentity.problemPath(), LOGGER)) {
                    blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, registries, tag));
                    blockentity1 = blockentity;
                }

                return blockentity1;
            } catch (Throwable throwable1) {
                LOGGER.error("Failed to load data for block entity {} for block {} at position {}", blockentitytype, pos, state, throwable1);
                return null;
            }
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }
    }

    protected static void setChanged(Level level, BlockPos pos, BlockState state) {
        level.blockEntityChanged(pos);
        if (!state.isAir()) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
        this.invalidateCapabilities();
        requestModelDataUpdate();
    }

    public void clearRemoved() {
        this.remove = false;
        // Neo: invalidate capabilities on block entity placement
        invalidateCapabilities();
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this instanceof Container container && this.level != null) {
            Containers.dropContents(this.level, pos, container);
        }
    }

    public boolean triggerEvent(int id, int type) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory reportCategory) {
        reportCategory.setDetail("Name", this::getNameForReporting);
        reportCategory.setDetail("Cached block", this.getBlockState()::toString);
        if (this.level == null) {
            reportCategory.setDetail("Block location", () -> this.worldPosition + " (world missing)");
        } else {
            reportCategory.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
            CrashReportCategory.populateBlockLocationDetails(reportCategory, this.level, this.worldPosition);
        }
    }

    public String getNameForReporting() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Override
    public CompoundTag getPersistentData() {
        if (this.customPersistentData == null)
            this.customPersistentData = new CompoundTag();
        return this.customPersistentData;
    }

    @Override
    @Nullable
    public final <T> T setData(net.neoforged.neoforge.attachment.AttachmentType<T> type, T data) {
        setChanged();
        return super.setData(type, data);
    }

    @Override
    @Nullable
    public final <T> T removeData(net.neoforged.neoforge.attachment.AttachmentType<T> type) {
        setChanged();
        return super.removeData(type);
    }

    @Override
    public final void syncData(net.neoforged.neoforge.attachment.AttachmentType<?> type) {
        net.neoforged.neoforge.attachment.AttachmentSync.syncBlockEntityUpdate(this, type);
    }

    @Deprecated
    public void setBlockState(BlockState blockState) {
        this.validateBlockState(blockState);
        this.blockState = blockState;
    }

    protected void applyImplicitComponents(DataComponentGetter componentGetter) {
    }

    public final void applyComponentsFromItemStack(ItemStack stack) {
        this.applyComponents(stack.getPrototype(), stack.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap components, DataComponentPatch patch) {
        final Set<DataComponentType<?>> set = new HashSet<>();
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        set.add(DataComponents.BLOCK_STATE);
        final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(components, patch);
        this.applyImplicitComponents(new DataComponentGetter() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_338266_) {
                set.add(p_338266_);
                return datacomponentmap.get(p_338266_);
            }

            @Override
            public <T> T getOrDefault(DataComponentType<? extends T> p_338358_, T p_338352_) {
                set.add(p_338358_);
                return datacomponentmap.getOrDefault(p_338358_, p_338352_);
            }
        });
        DataComponentPatch datacomponentpatch = patch.forget(set::contains);
        this.components = datacomponentpatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder components) {
    }

    @Deprecated
    public void removeComponentsFromTag(ValueOutput output) {
    }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
        datacomponentmap$builder.addAll(this.components);
        this.collectImplicitComponents(datacomponentmap$builder);
        return datacomponentmap$builder.build();
    }

    public DataComponentMap components() {
        return this.components;
    }

    public void setComponents(DataComponentMap components) {
        this.components = components;
    }

    @Nullable
    public static Component parseCustomNameSafe(ValueInput input, String customName) {
        return input.read(customName, ComponentSerialization.CODEC).orElse(null);
    }

    public ProblemReporter.PathElement problemPath() {
        return new BlockEntity.BlockEntityPathElement(this);
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
    }

    record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return this.blockEntity.getNameForReporting() + "@" + this.blockEntity.getBlockPos();
        }
    }
}
