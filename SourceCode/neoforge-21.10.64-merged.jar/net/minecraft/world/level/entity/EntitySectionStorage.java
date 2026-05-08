package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class EntitySectionStorage<T extends EntityAccess> {
    public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
    public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
    private final Class<T> entityClass;
    private final Long2ObjectFunction<Visibility> intialSectionVisibility;
    private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap<>();
    private final LongSortedSet sectionIds = new LongAVLTreeSet();

    public EntitySectionStorage(Class<T> entityClass, Long2ObjectFunction<Visibility> initialSectionVisibility) {
        this.entityClass = entityClass;
        this.intialSectionVisibility = initialSectionVisibility;
    }

    public void forEachAccessibleNonEmptySection(AABB boundingBox, AbortableIterationConsumer<EntitySection<T>> consumer) {
        int i = SectionPos.posToSectionCoord(boundingBox.minX - 2.0);
        int j = SectionPos.posToSectionCoord(boundingBox.minY - 4.0);
        int k = SectionPos.posToSectionCoord(boundingBox.minZ - 2.0);
        int l = SectionPos.posToSectionCoord(boundingBox.maxX + 2.0);
        int i1 = SectionPos.posToSectionCoord(boundingBox.maxY + 0.0);
        int j1 = SectionPos.posToSectionCoord(boundingBox.maxZ + 2.0);

        for (int k1 = i; k1 <= l; k1++) {
            long l1 = SectionPos.asLong(k1, 0, 0);
            long i2 = SectionPos.asLong(k1, -1, -1);
            LongIterator longiterator = this.sectionIds.subSet(l1, i2 + 1L).iterator();

            while (longiterator.hasNext()) {
                long j2 = longiterator.nextLong();
                int k2 = SectionPos.y(j2);
                int l2 = SectionPos.z(j2);
                if (k2 >= j && k2 <= i1 && l2 >= k && l2 <= j1) {
                    EntitySection<T> entitysection = this.sections.get(j2);
                    if (entitysection != null
                        && !entitysection.isEmpty()
                        && entitysection.getStatus().isAccessible()
                        && consumer.accept(entitysection).shouldAbort()) {
                        return;
                    }
                }
            }
        }
    }

    public LongStream getExistingSectionPositionsInChunk(long pos) {
        int i = ChunkPos.getX(pos);
        int j = ChunkPos.getZ(pos);
        LongSortedSet longsortedset = this.getChunkSections(i, j);
        if (longsortedset.isEmpty()) {
            return LongStream.empty();
        } else {
            OfLong oflong = longsortedset.iterator();
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(oflong, 1301), false);
        }
    }

    private LongSortedSet getChunkSections(int x, int z) {
        long i = SectionPos.asLong(x, 0, z);
        long j = SectionPos.asLong(x, -1, z);
        return this.sectionIds.subSet(i, j + 1L);
    }

    public Stream<EntitySection<T>> getExistingSectionsInChunk(long pos) {
        return this.getExistingSectionPositionsInChunk(pos).mapToObj(this.sections::get).filter(Objects::nonNull);
    }

    private static long getChunkKeyFromSectionKey(long pos) {
        return ChunkPos.asLong(SectionPos.x(pos), SectionPos.z(pos));
    }

    public EntitySection<T> getOrCreateSection(long sectionPos) {
        return this.sections.computeIfAbsent(sectionPos, this::createSection);
    }

    @Nullable
    public EntitySection<T> getSection(long sectionPos) {
        return this.sections.get(sectionPos);
    }

    private EntitySection<T> createSection(long sectionPos) {
        long i = getChunkKeyFromSectionKey(sectionPos);
        Visibility visibility = this.intialSectionVisibility.get(i);
        this.sectionIds.add(sectionPos);
        return new EntitySection<>(this.entityClass, visibility);
    }

    public LongSet getAllChunksWithExistingSections() {
        LongSet longset = new LongOpenHashSet();
        this.sections.keySet().forEach(p_156886_ -> longset.add(getChunkKeyFromSectionKey(p_156886_)));
        return longset;
    }

    public void getEntities(AABB bounds, AbortableIterationConsumer<T> consumer) {
        this.forEachAccessibleNonEmptySection(bounds, p_261459_ -> p_261459_.getEntities(bounds, consumer));
    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> test, AABB bounds, AbortableIterationConsumer<U> consumer) {
        this.forEachAccessibleNonEmptySection(bounds, p_261463_ -> p_261463_.getEntities(test, bounds, consumer));
    }

    public void remove(long sectionId) {
        this.sections.remove(sectionId);
        this.sectionIds.remove(sectionId);
    }

    @VisibleForDebug
    public int count() {
        return this.sectionIds.size();
    }
}
