package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class StructureBlockEntity extends BlockEntity implements BoundingBoxRenderable {
    private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
    public static final int MAX_OFFSET_PER_AXIS = 48;
    public static final int MAX_SIZE_PER_AXIS = 48;
    public static final String AUTHOR_TAG = "author";
    private static final String DEFAULT_AUTHOR = "";
    private static final String DEFAULT_METADATA = "";
    private static final BlockPos DEFAULT_POS = new BlockPos(0, 1, 0);
    private static final Vec3i DEFAULT_SIZE = Vec3i.ZERO;
    private static final Rotation DEFAULT_ROTATION = Rotation.NONE;
    private static final Mirror DEFAULT_MIRROR = Mirror.NONE;
    private static final boolean DEFAULT_IGNORE_ENTITIES = true;
    private static final boolean DEFAULT_STRICT = false;
    private static final boolean DEFAULT_POWERED = false;
    private static final boolean DEFAULT_SHOW_AIR = false;
    private static final boolean DEFAULT_SHOW_BOUNDING_BOX = true;
    private static final float DEFAULT_INTEGRITY = 1.0F;
    private static final long DEFAULT_SEED = 0L;
    @Nullable
    private ResourceLocation structureName;
    private String author = "";
    private String metaData = "";
    private BlockPos structurePos = DEFAULT_POS;
    private Vec3i structureSize = DEFAULT_SIZE;
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private StructureMode mode;
    private boolean ignoreEntities = true;
    private boolean strict = false;
    private boolean powered = false;
    private boolean showAir = false;
    private boolean showBoundingBox = true;
    private float integrity = 1.0F;
    private long seed = 0L;

    public StructureBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.STRUCTURE_BLOCK, pos, blockState);
        this.mode = blockState.getValue(StructureBlock.MODE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", this.getStructureName());
        output.putString("author", this.author);
        output.putString("metadata", this.metaData);
        output.putInt("posX", this.structurePos.getX());
        output.putInt("posY", this.structurePos.getY());
        output.putInt("posZ", this.structurePos.getZ());
        output.putInt("sizeX", this.structureSize.getX());
        output.putInt("sizeY", this.structureSize.getY());
        output.putInt("sizeZ", this.structureSize.getZ());
        output.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
        output.store("mirror", Mirror.LEGACY_CODEC, this.mirror);
        output.store("mode", StructureMode.LEGACY_CODEC, this.mode);
        output.putBoolean("ignoreEntities", this.ignoreEntities);
        output.putBoolean("strict", this.strict);
        output.putBoolean("powered", this.powered);
        output.putBoolean("showair", this.showAir);
        output.putBoolean("showboundingbox", this.showBoundingBox);
        output.putFloat("integrity", this.integrity);
        output.putLong("seed", this.seed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.setStructureName(input.getStringOr("name", ""));
        this.author = input.getStringOr("author", "");
        this.metaData = input.getStringOr("metadata", "");
        int i = Mth.clamp(input.getIntOr("posX", DEFAULT_POS.getX()), -48, 48);
        int j = Mth.clamp(input.getIntOr("posY", DEFAULT_POS.getY()), -48, 48);
        int k = Mth.clamp(input.getIntOr("posZ", DEFAULT_POS.getZ()), -48, 48);
        this.structurePos = new BlockPos(i, j, k);
        int l = Mth.clamp(input.getIntOr("sizeX", DEFAULT_SIZE.getX()), 0, 48);
        int i1 = Mth.clamp(input.getIntOr("sizeY", DEFAULT_SIZE.getY()), 0, 48);
        int j1 = Mth.clamp(input.getIntOr("sizeZ", DEFAULT_SIZE.getZ()), 0, 48);
        this.structureSize = new Vec3i(l, i1, j1);
        this.rotation = input.read("rotation", Rotation.LEGACY_CODEC).orElse(DEFAULT_ROTATION);
        this.mirror = input.read("mirror", Mirror.LEGACY_CODEC).orElse(DEFAULT_MIRROR);
        this.mode = input.read("mode", StructureMode.LEGACY_CODEC).orElse(StructureMode.DATA);
        this.ignoreEntities = input.getBooleanOr("ignoreEntities", true);
        this.strict = input.getBooleanOr("strict", false);
        this.powered = input.getBooleanOr("powered", false);
        this.showAir = input.getBooleanOr("showair", false);
        this.showBoundingBox = input.getBooleanOr("showboundingbox", true);
        this.integrity = input.getFloatOr("integrity", 1.0F);
        this.seed = input.getLongOr("seed", 0L);
        this.updateBlockState();
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPos blockpos = this.getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
                this.level.setBlock(blockpos, blockstate.setValue(StructureBlock.MODE, this.mode), 2);
            }
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public boolean usedBy(Player player) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        } else {
            if (player.level().isClientSide()) {
                player.openStructureBlock(this);
            }

            return true;
        }
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(@Nullable String structureName) {
        this.setStructureName(StringUtil.isNullOrEmpty(structureName) ? null : ResourceLocation.tryParse(structureName));
    }

    public void setStructureName(@Nullable ResourceLocation structureName) {
        this.structureName = structureName;
    }

    public void createdBy(LivingEntity author) {
        this.author = author.getPlainTextName();
    }

    public BlockPos getStructurePos() {
        return this.structurePos;
    }

    public void setStructurePos(BlockPos structurePos) {
        this.structurePos = structurePos;
    }

    public Vec3i getStructureSize() {
        return this.structureSize;
    }

    public void setStructureSize(Vec3i structureSize) {
        this.structureSize = structureSize;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public void setMirror(Mirror mirror) {
        this.mirror = mirror;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public String getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public StructureMode getMode() {
        return this.mode;
    }

    public void setMode(StructureMode mode) {
        this.mode = mode;
        BlockState blockstate = this.level.getBlockState(this.getBlockPos());
        if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock(this.getBlockPos(), blockstate.setValue(StructureBlock.MODE, mode), 2);
        }
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setIgnoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public void setIntegrity(float integrity) {
        this.integrity = integrity;
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean detectSize() {
        if (this.mode != StructureMode.SAVE) {
            return false;
        } else {
            BlockPos blockpos = this.getBlockPos();
            int i = 80;
            BlockPos blockpos1 = new BlockPos(blockpos.getX() - 80, this.level.getMinY(), blockpos.getZ() - 80);
            BlockPos blockpos2 = new BlockPos(blockpos.getX() + 80, this.level.getMaxY(), blockpos.getZ() + 80);
            Stream<BlockPos> stream = this.getRelatedCorners(blockpos1, blockpos2);
            return calculateEnclosingBoundingBox(blockpos, stream)
                .filter(
                    p_155790_ -> {
                        int j = p_155790_.maxX() - p_155790_.minX();
                        int k = p_155790_.maxY() - p_155790_.minY();
                        int l = p_155790_.maxZ() - p_155790_.minZ();
                        if (j > 1 && k > 1 && l > 1) {
                            this.structurePos = new BlockPos(
                                p_155790_.minX() - blockpos.getX() + 1, p_155790_.minY() - blockpos.getY() + 1, p_155790_.minZ() - blockpos.getZ() + 1
                            );
                            this.structureSize = new Vec3i(j - 1, k - 1, l - 1);
                            this.setChanged();
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            this.level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                            return true;
                        } else {
                            return false;
                        }
                    }
                )
                .isPresent();
        }
    }

    private Stream<BlockPos> getRelatedCorners(BlockPos minPos, BlockPos maxPos) {
        return BlockPos.betweenClosedStream(minPos, maxPos)
            .filter(p_272561_ -> this.level.getBlockState(p_272561_).is(Blocks.STRUCTURE_BLOCK))
            .map(this.level::getBlockEntity)
            .filter(p_155802_ -> p_155802_ instanceof StructureBlockEntity)
            .map(p_155785_ -> (StructureBlockEntity)p_155785_)
            .filter(p_155787_ -> p_155787_.mode == StructureMode.CORNER && Objects.equals(this.structureName, p_155787_.structureName))
            .map(BlockEntity::getBlockPos);
    }

    private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos pos, Stream<BlockPos> relatedCorners) {
        Iterator<BlockPos> iterator = relatedCorners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BlockPos blockpos = iterator.next();
            BoundingBox boundingbox = new BoundingBox(blockpos);
            if (iterator.hasNext()) {
                iterator.forEachRemaining(boundingbox::encapsulate);
            } else {
                boundingbox.encapsulate(pos);
            }

            return Optional.of(boundingbox);
        }
    }

    public boolean saveStructure() {
        return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
    }

    /**
     * Saves the template, either updating the local version or writing it to disk.
     *
     * @return true if the template was successfully saved.
     */
    public boolean saveStructure(boolean writeToDisk) {
        if (this.structureName != null && this.level instanceof ServerLevel serverlevel) {
            BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
            return saveStructure(serverlevel, this.structureName, blockpos, this.structureSize, this.ignoreEntities, this.author, writeToDisk, List.of());
        } else {
            return false;
        }
    }

    public static boolean saveStructure(
        ServerLevel level,
        ResourceLocation structureName,
        BlockPos pos,
        Vec3i size,
        boolean ignoreEntities,
        String author,
        boolean writeToDisk,
        List<Block> ignoredBlocks
    ) {
        StructureTemplateManager structuretemplatemanager = level.getStructureManager();

        StructureTemplate structuretemplate;
        try {
            structuretemplate = structuretemplatemanager.getOrCreate(structureName);
        } catch (ResourceLocationException resourcelocationexception1) {
            return false;
        }

        structuretemplate.fillFromWorld(
            level, pos, size, !ignoreEntities, Stream.concat(ignoredBlocks.stream(), Stream.of(Blocks.STRUCTURE_VOID)).toList()
        );
        structuretemplate.setAuthor(author);
        if (writeToDisk) {
            try {
                return structuretemplatemanager.save(structureName);
            } catch (ResourceLocationException resourcelocationexception) {
                return false;
            }
        } else {
            return true;
        }
    }

    public static RandomSource createRandom(long seed) {
        return seed == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(seed);
    }

    public boolean placeStructureIfSameSize(ServerLevel level) {
        if (this.mode == StructureMode.LOAD && this.structureName != null) {
            StructureTemplate structuretemplate = level.getStructureManager().get(this.structureName).orElse(null);
            if (structuretemplate == null) {
                return false;
            } else if (structuretemplate.getSize().equals(this.structureSize)) {
                this.placeStructure(level, structuretemplate);
                return true;
            } else {
                this.loadStructureInfo(structuretemplate);
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean loadStructureInfo(ServerLevel level) {
        StructureTemplate structuretemplate = this.getStructureTemplate(level);
        if (structuretemplate == null) {
            return false;
        } else {
            this.loadStructureInfo(structuretemplate);
            return true;
        }
    }

    private void loadStructureInfo(StructureTemplate structureTemplate) {
        this.author = !StringUtil.isNullOrEmpty(structureTemplate.getAuthor()) ? structureTemplate.getAuthor() : "";
        this.structureSize = structureTemplate.getSize();
        this.setChanged();
    }

    public void placeStructure(ServerLevel level) {
        StructureTemplate structuretemplate = this.getStructureTemplate(level);
        if (structuretemplate != null) {
            this.placeStructure(level, structuretemplate);
        }
    }

    @Nullable
    private StructureTemplate getStructureTemplate(ServerLevel level) {
        return this.structureName == null ? null : level.getStructureManager().get(this.structureName).orElse(null);
    }

    private void placeStructure(ServerLevel level, StructureTemplate structureTemplate) {
        this.loadStructureInfo(structureTemplate);
        StructurePlaceSettings structureplacesettings = new StructurePlaceSettings()
            .setMirror(this.mirror)
            .setRotation(this.rotation)
            .setIgnoreEntities(this.ignoreEntities)
            .setKnownShape(this.strict);
        if (this.integrity < 1.0F) {
            structureplacesettings.clearProcessors()
                .addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F)))
                .setRandom(createRandom(this.seed));
        }

        BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
        if (SharedConstants.DEBUG_STRUCTURE_EDIT_MODE) {
            BlockPos.betweenClosed(blockpos, blockpos.offset(this.structureSize))
                .forEach(p_155783_ -> level.setBlock(p_155783_, Blocks.STRUCTURE_VOID.defaultBlockState(), 2));
        }

        structureTemplate.placeInWorld(level, blockpos, blockpos, structureplacesettings, createRandom(this.seed), 2 | (this.strict ? 816 : 0));
    }

    public void unloadStructure() {
        if (this.structureName != null) {
            ServerLevel serverlevel = (ServerLevel)this.level;
            StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();
            structuretemplatemanager.remove(this.structureName);
        }
    }

    public boolean isStructureLoadable() {
        if (this.mode == StructureMode.LOAD && !this.level.isClientSide() && this.structureName != null) {
            ServerLevel serverlevel = (ServerLevel)this.level;
            StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

            try {
                return structuretemplatemanager.get(this.structureName).isPresent();
            } catch (ResourceLocationException resourcelocationexception) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean getShowAir() {
        return this.showAir;
    }

    public void setShowAir(boolean showAir) {
        this.showAir = showAir;
    }

    public boolean getShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    @Override
    public BoundingBoxRenderable.Mode renderMode() {
        if (this.mode != StructureMode.SAVE && this.mode != StructureMode.LOAD) {
            return BoundingBoxRenderable.Mode.NONE;
        } else if (this.mode == StructureMode.SAVE && this.showAir) {
            return BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS;
        } else {
            return this.mode != StructureMode.SAVE && !this.showBoundingBox ? BoundingBoxRenderable.Mode.NONE : BoundingBoxRenderable.Mode.BOX;
        }
    }

    @Override
    public BoundingBoxRenderable.RenderableBox getRenderableBox() {
        BlockPos blockpos = this.getStructurePos();
        Vec3i vec3i = this.getStructureSize();
        int i = blockpos.getX();
        int j = blockpos.getZ();
        int j1 = blockpos.getY();
        int i2 = j1 + vec3i.getY();
        int k;
        int l;
        switch (this.mirror) {
            case LEFT_RIGHT:
                k = vec3i.getX();
                l = -vec3i.getZ();
                break;
            case FRONT_BACK:
                k = -vec3i.getX();
                l = vec3i.getZ();
                break;
            default:
                k = vec3i.getX();
                l = vec3i.getZ();
        }

        int i1;
        int k1;
        int l1;
        int j2;
        switch (this.rotation) {
            case CLOCKWISE_90:
                i1 = l < 0 ? i : i + 1;
                k1 = k < 0 ? j + 1 : j;
                l1 = i1 - l;
                j2 = k1 + k;
                break;
            case CLOCKWISE_180:
                i1 = k < 0 ? i : i + 1;
                k1 = l < 0 ? j : j + 1;
                l1 = i1 - k;
                j2 = k1 - l;
                break;
            case COUNTERCLOCKWISE_90:
                i1 = l < 0 ? i + 1 : i;
                k1 = k < 0 ? j : j + 1;
                l1 = i1 + l;
                j2 = k1 - k;
                break;
            default:
                i1 = k < 0 ? i + 1 : i;
                k1 = l < 0 ? j + 1 : j;
                l1 = i1 + k;
                j2 = k1 + l;
        }

        return BoundingBoxRenderable.RenderableBox.fromCorners(i1, j1, k1, l1, i2, j2);
    }

    public static enum UpdateType {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
    }
}
