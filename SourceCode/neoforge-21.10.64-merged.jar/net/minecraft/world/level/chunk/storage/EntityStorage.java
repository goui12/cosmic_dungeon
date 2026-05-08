package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_TAG = "Entities";
    private static final String POSITION_TAG = "Position";
    private final ServerLevel level;
    private final SimpleRegionStorage simpleRegionStorage;
    private final LongSet emptyChunks = new LongOpenHashSet();
    private final ConsecutiveExecutor entityDeserializerQueue;

    public EntityStorage(SimpleRegionStorage simpleRegionStorage, ServerLevel level, Executor executor) {
        this.simpleRegionStorage = simpleRegionStorage;
        this.level = level;
        this.entityDeserializerQueue = new ConsecutiveExecutor(executor, "entity-deserializer");
    }

    @Override
    public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos pos) {
        if (this.emptyChunks.contains(pos.toLong())) {
            return CompletableFuture.completedFuture(emptyChunk(pos));
        } else {
            CompletableFuture<Optional<CompoundTag>> completablefuture = this.simpleRegionStorage.read(pos);
            this.reportLoadFailureIfPresent(completablefuture, pos);
            return completablefuture.thenApplyAsync(
                p_421443_ -> {
                    if (p_421443_.isEmpty()) {
                        this.emptyChunks.add(pos.toLong());
                        return emptyChunk(pos);
                    } else {
                        try {
                            ChunkPos chunkpos = p_421443_.get().read("Position", ChunkPos.CODEC).orElseThrow();
                            if (!Objects.equals(pos, chunkpos)) {
                                LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", pos, pos, chunkpos);
                                this.level.getServer().reportMisplacedChunk(chunkpos, pos, this.simpleRegionStorage.storageInfo());
                            }
                        } catch (Exception exception) {
                            LOGGER.warn("Failed to parse chunk {} position info", pos, exception);
                            this.level.getServer().reportChunkLoadFailure(exception, this.simpleRegionStorage.storageInfo(), pos);
                        }

                        CompoundTag compoundtag = this.simpleRegionStorage.upgradeChunkTag(p_421443_.get(), -1);

                        ChunkEntities chunkentities;
                        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                                ChunkAccess.problemPath(pos), LOGGER
                            )) {
                            ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, this.level.registryAccess(), compoundtag);
                            ValueInput.ValueInputList valueinput$valueinputlist = valueinput.childrenListOrEmpty("Entities");
                            List<Entity> list = EntityType.loadEntitiesRecursive(valueinput$valueinputlist, this.level, EntitySpawnReason.LOAD).toList();
                            chunkentities = new ChunkEntities<>(pos, list);
                        }

                        return chunkentities;
                    }
                },
                this.entityDeserializerQueue::schedule
            );
        }
    }

    private static ChunkEntities<Entity> emptyChunk(ChunkPos pos) {
        return new ChunkEntities<>(pos, List.of());
    }

    @Override
    public void storeEntities(ChunkEntities<Entity> entities) {
        ChunkPos chunkpos = entities.getPos();
        if (entities.isEmpty()) {
            if (this.emptyChunks.add(chunkpos.toLong())) {
                this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, null), chunkpos);
            }
        } else {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
                    ChunkAccess.problemPath(chunkpos), LOGGER
                )) {
                ListTag listtag = new ListTag();
                entities.getEntities()
                    .forEach(
                        p_421446_ -> {
                            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(
                                problemreporter$scopedcollector.forChild(p_421446_.problemPath()), p_421446_.registryAccess()
                            );
                            try {
                            if (p_421446_.save(tagvalueoutput)) {
                                CompoundTag compoundtag1 = tagvalueoutput.buildResult();
                                listtag.add(compoundtag1);
                            }
                            } catch (Exception e) {
                                LOGGER.error("An Entity type {} has thrown an exception trying to write state. It will not persist. Report this to the mod author", p_421446_.getType(), e);
                            }
                        }
                    );
                CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
                compoundtag.put("Entities", listtag);
                compoundtag.store("Position", ChunkPos.CODEC, chunkpos);
                this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, compoundtag), chunkpos);
                this.emptyChunks.remove(chunkpos.toLong());
            }
        }
    }

    private void reportSaveFailureIfPresent(CompletableFuture<?> future, ChunkPos pos) {
        future.exceptionally(p_351986_ -> {
            LOGGER.error("Failed to store entity chunk {}", pos, p_351986_);
            this.level.getServer().reportChunkSaveFailure(p_351986_, this.simpleRegionStorage.storageInfo(), pos);
            return null;
        });
    }

    private void reportLoadFailureIfPresent(CompletableFuture<?> future, ChunkPos pos) {
        future.exceptionally(p_351990_ -> {
            LOGGER.error("Failed to load entity chunk {}", pos, p_351990_);
            this.level.getServer().reportChunkLoadFailure(p_351990_, this.simpleRegionStorage.storageInfo(), pos);
            return null;
        });
    }

    @Override
    public void flush(boolean synchronize) {
        this.simpleRegionStorage.synchronize(synchronize).join();
        this.entityDeserializerQueue.runAll();
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }
}
