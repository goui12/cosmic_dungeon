package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public record SerializableChunkData(
    PalettedContainerFactory containerFactory,
    ChunkPos chunkPos,
    int minSectionY,
    long lastUpdateTime,
    long inhabitedTime,
    ChunkStatus chunkStatus,
    @Nullable BlendingData.Packed blendingData,
    @Nullable BelowZeroRetrogen belowZeroRetrogen,
    UpgradeData upgradeData,
    @Nullable long[] carvingMask,
    Map<Heightmap.Types, long[]> heightmaps,
    ChunkAccess.PackedTicks packedTicks,
    ShortList[] postProcessingSections,
    boolean lightCorrect,
    List<SerializableChunkData.SectionData> sectionData,
    List<CompoundTag> entities,
    List<CompoundTag> blockEntities,
    CompoundTag structureData,
    @Nullable CompoundTag attachmentData,
    @Nullable ListTag auxLightData
) {
    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";
    public static final String X_POS_TAG = "xPos";
    public static final String Z_POS_TAG = "zPos";
    public static final String HEIGHTMAPS_TAG = "Heightmaps";
    public static final String IS_LIGHT_ON_TAG = "isLightOn";
    public static final String SECTIONS_TAG = "sections";
    public static final String BLOCK_LIGHT_TAG = "BlockLight";
    public static final String SKY_LIGHT_TAG = "SkyLight";

    /**
     * @deprecated Neo: use constructor with additional data instead
     */
    @Deprecated
    SerializableChunkData(
            PalettedContainerFactory containerFactory,
            ChunkPos chunkPos,
            int minSectionY,
            long lastUpdateTime,
            long inhabitedTime,
            ChunkStatus chunkStatus,
            @Nullable BlendingData.Packed blendingData,
            @Nullable BelowZeroRetrogen belowZeroRetrogen,
            UpgradeData upgradeData,
            @Nullable long[] carvingMask,
            Map<Heightmap.Types, long[]> heightmaps,
            ChunkAccess.PackedTicks packedTicks,
            ShortList[] postProcessingSections,
            boolean lightCorrect,
            List<SerializableChunkData.SectionData> sectionData,
            List<CompoundTag> entities,
            List<CompoundTag> blockEntities,
            CompoundTag structureData
    ) {
        this(containerFactory, chunkPos, minSectionY, lastUpdateTime, inhabitedTime, chunkStatus, blendingData, belowZeroRetrogen, upgradeData, carvingMask, heightmaps, packedTicks, postProcessingSections, lightCorrect, sectionData, entities, blockEntities, structureData, null, null);
    }

    @Nullable
    public static SerializableChunkData parse(LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, CompoundTag tag) {
        if (tag.getString("Status").isEmpty()) {
            return null;
        } else {
            ChunkPos chunkpos = new ChunkPos(tag.getIntOr("xPos", 0), tag.getIntOr("zPos", 0));
            long i = tag.getLongOr("LastUpdate", 0L);
            long j = tag.getLongOr("InhabitedTime", 0L);
            ChunkStatus chunkstatus = tag.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
            UpgradeData upgradedata = tag.getCompound("UpgradeData").map(p_409546_ -> new UpgradeData(p_409546_, levelHeightAccessor)).orElse(UpgradeData.EMPTY);
            boolean flag = tag.getBooleanOr("isLightOn", false);
            BlendingData.Packed blendingdata$packed = tag.read("blending_data", BlendingData.Packed.CODEC).orElse(null);
            BelowZeroRetrogen belowzeroretrogen = tag.read("below_zero_retrogen", BelowZeroRetrogen.CODEC).orElse(null);
            long[] along = tag.getLongArray("carving_mask").orElse(null);
            Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);
            tag.getCompound("Heightmaps").ifPresent(p_409536_ -> {
                for (Heightmap.Types heightmap$types : chunkstatus.heightmapsAfter()) {
                    p_409536_.getLongArray(heightmap$types.getSerializationKey()).ifPresent(p_409544_ -> map.put(heightmap$types, p_409544_));
                }
            });
            List<SavedTick<Block>> list = SavedTick.filterTickListForChunk(tag.read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), chunkpos);
            List<SavedTick<Fluid>> list1 = SavedTick.filterTickListForChunk(tag.read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), chunkpos);
            ChunkAccess.PackedTicks chunkaccess$packedticks = new ChunkAccess.PackedTicks(list, list1);
            ListTag listtag = tag.getListOrEmpty("PostProcessing");
            ShortList[] ashortlist = new ShortList[listtag.size()];

            for (int k = 0; k < listtag.size(); k++) {
                ListTag listtag1 = listtag.getListOrEmpty(k);
                ShortList shortlist = new ShortArrayList(listtag1.size());

                for (int l = 0; l < listtag1.size(); l++) {
                    shortlist.add(listtag1.getShortOr(l, (short)0));
                }

                ashortlist[k] = shortlist;
            }

            List<CompoundTag> list3 = tag.getList("entities").stream().flatMap(ListTag::compoundStream).toList();
            List<CompoundTag> list4 = tag.getList("block_entities").stream().flatMap(ListTag::compoundStream).toList();
            CompoundTag compoundtag1 = tag.getCompoundOrEmpty("structures");
            ListTag listtag2 = tag.getListOrEmpty("sections");
            List<SerializableChunkData.SectionData> list2 = new ArrayList<>(listtag2.size());
            Codec<PalettedContainerRO<Holder<Biome>>> codec = palettedContainerFactory.biomeContainerCodec();
            Codec<PalettedContainer<BlockState>> codec1 = palettedContainerFactory.blockStatesContainerCodec();

            for (int i1 = 0; i1 < listtag2.size(); i1++) {
                Optional<CompoundTag> optional = listtag2.getCompound(i1);
                if (!optional.isEmpty()) {
                    CompoundTag compoundtag = optional.get();
                    int j1 = compoundtag.getByteOr("Y", (byte)0);
                    LevelChunkSection levelchunksection;
                    if (j1 >= levelHeightAccessor.getMinSectionY() && j1 <= levelHeightAccessor.getMaxSectionY()) {
                        PalettedContainer<BlockState> palettedcontainer = compoundtag.getCompound("block_states")
                            .map(
                                p_445364_ -> codec1.parse(NbtOps.INSTANCE, p_445364_)
                                    .promotePartial(p_361842_ -> logErrors(chunkpos, j1, p_361842_))
                                    .getOrThrow(SerializableChunkData.ChunkReadException::new)
                            )
                            .orElseGet(palettedContainerFactory::createForBlockStates);
                        PalettedContainerRO<Holder<Biome>> palettedcontainerro = compoundtag.getCompound("biomes")
                            .map(
                                p_409533_ -> codec.parse(NbtOps.INSTANCE, p_409533_)
                                    .promotePartial(p_361282_ -> logErrors(chunkpos, j1, p_361282_))
                                    .getOrThrow(SerializableChunkData.ChunkReadException::new)
                            )
                            .orElseGet(palettedContainerFactory::createForBiomes);
                        levelchunksection = new LevelChunkSection(palettedcontainer, palettedcontainerro);
                    } else {
                        levelchunksection = null;
                    }

                    DataLayer datalayer = compoundtag.getByteArray("BlockLight").map(DataLayer::new).orElse(null);
                    DataLayer datalayer1 = compoundtag.getByteArray("SkyLight").map(DataLayer::new).orElse(null);
                    list2.add(new SerializableChunkData.SectionData(j1, levelchunksection, datalayer, datalayer1));
                }
            }

            CompoundTag attachmentData = tag.getCompound(net.neoforged.neoforge.attachment.AttachmentHolder.ATTACHMENTS_NBT_KEY).orElse(null);
            ListTag auxLightData = tag.getList(net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager.LIGHT_NBT_KEY).orElse(null);

            return new SerializableChunkData(
                palettedContainerFactory,
                chunkpos,
                levelHeightAccessor.getMinSectionY(),
                i,
                j,
                chunkstatus,
                blendingdata$packed,
                belowzeroretrogen,
                upgradedata,
                along,
                map,
                chunkaccess$packedticks,
                ashortlist,
                flag,
                list2,
                list3,
                list4,
                compoundtag1,
                attachmentData,
                auxLightData
            );
        }
    }

    public ProtoChunk read(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos) {
        if (!Objects.equals(pos, this.chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, this.chunkPos);
            level.getServer().reportMisplacedChunk(this.chunkPos, pos, regionStorageInfo);
        }

        int i = level.getSectionsCount();
        LevelChunkSection[] alevelchunksection = new LevelChunkSection[i];
        boolean flag = level.dimensionType().hasSkyLight();
        ChunkSource chunksource = level.getChunkSource();
        LevelLightEngine levellightengine = chunksource.getLightEngine();
        PalettedContainerFactory palettedcontainerfactory = level.palettedContainerFactory();
        boolean flag1 = false;

        for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
            SectionPos sectionpos = SectionPos.of(pos, serializablechunkdata$sectiondata.y);
            if (serializablechunkdata$sectiondata.chunkSection != null) {
                alevelchunksection[level.getSectionIndexFromSectionY(serializablechunkdata$sectiondata.y)] = serializablechunkdata$sectiondata.chunkSection;
                poiManager.checkConsistencyWithBlocks(sectionpos, serializablechunkdata$sectiondata.chunkSection);
            }

            boolean flag2 = serializablechunkdata$sectiondata.blockLight != null;
            boolean flag3 = flag && serializablechunkdata$sectiondata.skyLight != null;
            if (flag2 || flag3) {
                if (!flag1) {
                    levellightengine.retainData(pos, true);
                    flag1 = true;
                }

                if (flag2) {
                    levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, serializablechunkdata$sectiondata.blockLight);
                }

                if (flag3) {
                    levellightengine.queueSectionData(LightLayer.SKY, sectionpos, serializablechunkdata$sectiondata.skyLight);
                }
            }
        }

        ChunkType chunktype = this.chunkStatus.getChunkType();
        ChunkAccess chunkaccess;
        if (chunktype == ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> levelchunkticks = new LevelChunkTicks<>(this.packedTicks.blocks());
            LevelChunkTicks<Fluid> levelchunkticks1 = new LevelChunkTicks<>(this.packedTicks.fluids());
            chunkaccess = new LevelChunk(
                level.getLevel(),
                pos,
                this.upgradeData,
                levelchunkticks,
                levelchunkticks1,
                this.inhabitedTime,
                alevelchunksection,
                postLoadChunk(level, this.entities, this.blockEntities),
                BlendingData.unpack(this.blendingData)
            );
            if (this.auxLightData != null) {
                ((LevelChunk) chunkaccess).getAuxLightManager(chunkPos).deserializeNBT(this.auxLightData);
            }
        } else {
            ProtoChunkTicks<Block> protochunkticks = ProtoChunkTicks.load(this.packedTicks.blocks());
            ProtoChunkTicks<Fluid> protochunkticks1 = ProtoChunkTicks.load(this.packedTicks.fluids());
            ProtoChunk protochunk1 = new ProtoChunk(
                pos,
                this.upgradeData,
                alevelchunksection,
                protochunkticks,
                protochunkticks1,
                level,
                palettedcontainerfactory,
                BlendingData.unpack(this.blendingData)
            );
            chunkaccess = protochunk1;
            protochunk1.setInhabitedTime(this.inhabitedTime);
            if (this.belowZeroRetrogen != null) {
                protochunk1.setBelowZeroRetrogen(this.belowZeroRetrogen);
            }

            protochunk1.setPersistedStatus(this.chunkStatus);
            if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                protochunk1.setLightEngine(levellightengine);
            }
        }

        chunkaccess.setLightCorrect(this.lightCorrect);
        EnumSet<Heightmap.Types> enumset = EnumSet.noneOf(Heightmap.Types.class);

        for (Heightmap.Types heightmap$types : chunkaccess.getPersistedStatus().heightmapsAfter()) {
            long[] along = this.heightmaps.get(heightmap$types);
            if (along != null) {
                chunkaccess.setHeightmap(heightmap$types, along);
            } else {
                enumset.add(heightmap$types);
            }
        }

        Heightmap.primeHeightmaps(chunkaccess, enumset);
        chunkaccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(level), this.structureData, level.getSeed()));
        chunkaccess.setAllReferences(unpackStructureReferences(level.registryAccess(), pos, this.structureData));

        for (int j = 0; j < this.postProcessingSections.length; j++) {
            chunkaccess.addPackedPostProcess(this.postProcessingSections[j], j);
        }

        if (this.attachmentData != null) {
            chunkaccess.readAttachmentsFromNBT(level.registryAccess(), this.attachmentData);
        }

        if (chunktype == ChunkType.LEVELCHUNK) {
            return new ImposterProtoChunk((LevelChunk)chunkaccess, false);
        } else {
            ProtoChunk protochunk = (ProtoChunk)chunkaccess;

            for (CompoundTag compoundtag : this.entities) {
                protochunk.addEntity(compoundtag);
            }

            for (CompoundTag compoundtag1 : this.blockEntities) {
                protochunk.setBlockEntityNbt(compoundtag1);
            }

            if (this.carvingMask != null) {
                protochunk.setCarvingMask(new CarvingMask(this.carvingMask, chunkaccess.getMinY()));
            }

            return protochunk;
        }
    }

    private static void logErrors(ChunkPos chunkPos, int sectionY, String error) {
        LOGGER.error("Recoverable errors when loading section [{}, {}, {}]: {}", chunkPos.x, sectionY, chunkPos.z, error);
    }

    public static SerializableChunkData copyOf(ServerLevel level, ChunkAccess chunk) {
        if (!chunk.canBeSerialized()) {
            throw new IllegalArgumentException("Chunk can't be serialized: " + chunk);
        } else {
            ChunkPos chunkpos = chunk.getPos();
            List<SerializableChunkData.SectionData> list = new ArrayList<>();
            LevelChunkSection[] alevelchunksection = chunk.getSections();
            LevelLightEngine levellightengine = level.getChunkSource().getLightEngine();

            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
                int j = chunk.getSectionIndexFromSectionY(i);
                boolean flag = j >= 0 && j < alevelchunksection.length;
                DataLayer datalayer = levellightengine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkpos, i));
                DataLayer datalayer1 = levellightengine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkpos, i));
                DataLayer datalayer2 = datalayer != null && !datalayer.isEmpty() ? datalayer.copy() : null;
                DataLayer datalayer3 = datalayer1 != null && !datalayer1.isEmpty() ? datalayer1.copy() : null;
                if (flag || datalayer2 != null || datalayer3 != null) {
                    LevelChunkSection levelchunksection = flag ? alevelchunksection[j].copy() : null;
                    list.add(new SerializableChunkData.SectionData(i, levelchunksection, datalayer2, datalayer3));
                }
            }

            List<CompoundTag> list1 = new ArrayList<>(chunk.getBlockEntitiesPos().size());

            for (BlockPos blockpos : chunk.getBlockEntitiesPos()) {
                CompoundTag compoundtag = chunk.getBlockEntityNbtForSaving(blockpos, level.registryAccess());
                if (compoundtag != null) {
                    list1.add(compoundtag);
                }
            }

            List<CompoundTag> list2 = new ArrayList<>();
            long[] along = null;
            if (chunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
                ProtoChunk protochunk = (ProtoChunk)chunk;
                list2.addAll(protochunk.getEntities());
                CarvingMask carvingmask = protochunk.getCarvingMask();
                if (carvingmask != null) {
                    along = carvingmask.toArray();
                }
            }

            Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);

            for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
                if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                    long[] along1 = entry.getValue().getRawData();
                    map.put(entry.getKey(), (long[])along1.clone());
                }
            }

            ChunkAccess.PackedTicks chunkaccess$packedticks = chunk.getTicksForSerialization(level.getGameTime());
            ShortList[] ashortlist = Arrays.stream(chunk.getPostProcessing())
                .map(p_363794_ -> p_363794_ != null ? new ShortArrayList(p_363794_) : null)
                .toArray(ShortList[]::new);
            CompoundTag compoundtag1 = packStructureData(
                StructurePieceSerializationContext.fromLevel(level), chunkpos, chunk.getAllStarts(), chunk.getAllReferences()
            );

            CompoundTag attachmentData = null;
            try {
                attachmentData = chunk.writeAttachmentsToNBT(level.registryAccess());
            } catch (Exception exception) {
                LOGGER.error("Failed to write chunk attachments. An attachment has likely thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
            }
            ListTag auxLightData = null;
            if (chunk instanceof LevelChunk levelChunk) {
                auxLightData = levelChunk.getAuxLightManager(chunkpos).serializeNBT();
            }

            return new SerializableChunkData(
                level.palettedContainerFactory(),
                chunkpos,
                chunk.getMinSectionY(),
                level.getGameTime(),
                chunk.getInhabitedTime(),
                chunk.getPersistedStatus(),
                Optionull.map(chunk.getBlendingData(), BlendingData::pack),
                chunk.getBelowZeroRetrogen(),
                chunk.getUpgradeData().copy(),
                along,
                map,
                chunkaccess$packedticks,
                ashortlist,
                chunk.isLightCorrect(),
                list,
                list2,
                list1,
                compoundtag1,
                attachmentData,
                auxLightData
            );
        }
    }

    public CompoundTag write() {
        CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        compoundtag.putInt("xPos", this.chunkPos.x);
        compoundtag.putInt("yPos", this.minSectionY);
        compoundtag.putInt("zPos", this.chunkPos.z);
        compoundtag.putLong("LastUpdate", this.lastUpdateTime);
        compoundtag.putLong("InhabitedTime", this.inhabitedTime);
        compoundtag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(this.chunkStatus).toString());
        compoundtag.storeNullable("blending_data", BlendingData.Packed.CODEC, this.blendingData);
        compoundtag.storeNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, this.belowZeroRetrogen);
        if (!this.upgradeData.isEmpty()) {
            compoundtag.put("UpgradeData", this.upgradeData.write());
        }

        ListTag listtag = new ListTag();
        Codec<PalettedContainer<BlockState>> codec = this.containerFactory.blockStatesContainerCodec();
        Codec<PalettedContainerRO<Holder<Biome>>> codec1 = this.containerFactory.biomeContainerCodec();

        for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
            CompoundTag compoundtag1 = new CompoundTag();
            LevelChunkSection levelchunksection = serializablechunkdata$sectiondata.chunkSection;
            if (levelchunksection != null) {
                compoundtag1.store("block_states", codec, levelchunksection.getStates());
                compoundtag1.store("biomes", codec1, levelchunksection.getBiomes());
            }

            if (serializablechunkdata$sectiondata.blockLight != null) {
                compoundtag1.putByteArray("BlockLight", serializablechunkdata$sectiondata.blockLight.getData());
            }

            if (serializablechunkdata$sectiondata.skyLight != null) {
                compoundtag1.putByteArray("SkyLight", serializablechunkdata$sectiondata.skyLight.getData());
            }

            if (!compoundtag1.isEmpty()) {
                compoundtag1.putByte("Y", (byte)serializablechunkdata$sectiondata.y);
                listtag.add(compoundtag1);
            }
        }

        compoundtag.put("sections", listtag);
        if (this.lightCorrect) {
            compoundtag.putBoolean("isLightOn", true);
        }

        ListTag listtag1 = new ListTag();
        listtag1.addAll(this.blockEntities);
        compoundtag.put("block_entities", listtag1);
        if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
            ListTag listtag2 = new ListTag();
            listtag2.addAll(this.entities);
            compoundtag.put("entities", listtag2);
            if (this.carvingMask != null) {
                compoundtag.putLongArray("carving_mask", this.carvingMask);
            }
        }

        saveTicks(compoundtag, this.packedTicks);
        compoundtag.put("PostProcessing", packOffsets(this.postProcessingSections));
        CompoundTag compoundtag2 = new CompoundTag();
        this.heightmaps.forEach((p_362472_, p_363515_) -> compoundtag2.put(p_362472_.getSerializationKey(), new LongArrayTag(p_363515_)));
        compoundtag.put("Heightmaps", compoundtag2);
        compoundtag.put("structures", this.structureData);

        if (attachmentData != null) {
            compoundtag.put(net.neoforged.neoforge.attachment.AttachmentHolder.ATTACHMENTS_NBT_KEY, attachmentData);
        }
        if (auxLightData != null) {
            compoundtag.put(net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager.LIGHT_NBT_KEY, auxLightData);
        }

        return compoundtag;
    }

    private static void saveTicks(CompoundTag tag, ChunkAccess.PackedTicks ticks) {
        tag.store("block_ticks", BLOCK_TICKS_CODEC, ticks.blocks());
        tag.store("fluid_ticks", FLUID_TICKS_CODEC, ticks.fluids());
    }

    public static ChunkStatus getChunkStatusFromTag(@Nullable CompoundTag tag) {
        return tag != null ? tag.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY) : ChunkStatus.EMPTY;
    }

    @Nullable
    private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel level, List<CompoundTag> entities, List<CompoundTag> blockEntities) {
        return entities.isEmpty() && blockEntities.isEmpty()
            ? null
            : p_421450_ -> {
                if (!entities.isEmpty()) {
                    try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_421450_.problemPath(), LOGGER)) {
                        level.addLegacyChunkEntities(
                            EntityType.loadEntitiesRecursive(
                                TagValueInput.create(problemreporter$scopedcollector, level.registryAccess(), entities), level, EntitySpawnReason.LOAD
                            )
                        );
                    }
                }

                for (CompoundTag compoundtag : blockEntities) {
                    boolean flag = compoundtag.getBooleanOr("keepPacked", false);
                    if (flag) {
                        p_421450_.setBlockEntityNbt(compoundtag);
                    } else {
                        BlockPos blockpos = BlockEntity.getPosFromTag(p_421450_.getPos(), compoundtag);
                        BlockEntity blockentity = BlockEntity.loadStatic(blockpos, p_421450_.getBlockState(blockpos), compoundtag, level.registryAccess());
                        if (blockentity != null) {
                            p_421450_.setBlockEntity(blockentity);
                        }
                    }
                }
            };
    }

    private static CompoundTag packStructureData(
        StructurePieceSerializationContext context, ChunkPos pos, Map<Structure, StructureStart> structureStarts, Map<Structure, LongSet> references
    ) {
        CompoundTag compoundtag = new CompoundTag();
        CompoundTag compoundtag1 = new CompoundTag();
        Registry<Structure> registry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        for (Entry<Structure, StructureStart> entry : structureStarts.entrySet()) {
            ResourceLocation resourcelocation = registry.getKey(entry.getKey());
            compoundtag1.put(resourcelocation.toString(), entry.getValue().createTag(context, pos));
        }

        compoundtag.put("starts", compoundtag1);
        CompoundTag compoundtag2 = new CompoundTag();

        for (Entry<Structure, LongSet> entry1 : references.entrySet()) {
            if (!entry1.getValue().isEmpty()) {
                ResourceLocation resourcelocation1 = registry.getKey(entry1.getKey());
                compoundtag2.putLongArray(resourcelocation1.toString(), entry1.getValue().toLongArray());
            }
        }

        compoundtag.put("References", compoundtag2);
        return compoundtag;
    }

    private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext context, CompoundTag tag, long seed) {
        Map<Structure, StructureStart> map = Maps.newHashMap();
        Registry<Structure> registry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compoundtag = tag.getCompoundOrEmpty("starts");

        for (String s : compoundtag.keySet()) {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(s);
            Structure structure = registry.getValue(resourcelocation);
            if (structure == null) {
                LOGGER.error("Unknown structure start: {}", resourcelocation);
            } else {
                StructureStart structurestart = StructureStart.loadStaticStart(context, compoundtag.getCompoundOrEmpty(s), seed);
                if (structurestart != null) {
                    map.put(structure, structurestart);
                }
            }
        }

        return map;
    }

    private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess registries, ChunkPos pos, CompoundTag tag) {
        Map<Structure, LongSet> map = Maps.newHashMap();
        Registry<Structure> registry = registries.lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compoundtag = tag.getCompoundOrEmpty("References");
        compoundtag.forEach((p_409540_, p_409541_) -> {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(p_409540_);
            Structure structure = registry.getValue(resourcelocation);
            if (structure == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", resourcelocation, pos);
            } else {
                Optional<long[]> optional = p_409541_.asLongArray();
                if (!optional.isEmpty()) {
                    map.put(structure, new LongOpenHashSet(Arrays.stream(optional.get()).filter(p_360765_ -> {
                        ChunkPos chunkpos = new ChunkPos(p_360765_);
                        if (chunkpos.getChessboardDistance(pos) > 8) {
                            LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", resourcelocation, chunkpos, pos);
                            return false;
                        } else {
                            return true;
                        }
                    }).toArray()));
                }
            }
        });
        return map;
    }

    private static ListTag packOffsets(ShortList[] offsets) {
        ListTag listtag = new ListTag();

        for (ShortList shortlist : offsets) {
            ListTag listtag1 = new ListTag();
            if (shortlist != null) {
                for (int i = 0; i < shortlist.size(); i++) {
                    listtag1.add(ShortTag.valueOf(shortlist.getShort(i)));
                }
            }

            listtag.add(listtag1);
        }

        return listtag;
    }

    public static class ChunkReadException extends NbtException {
        public ChunkReadException(String p_361971_) {
            super(p_361971_);
        }
    }

    public record SectionData(int y, @Nullable LevelChunkSection chunkSection, @Nullable DataLayer blockLight, @Nullable DataLayer skyLight) {
    }
}
