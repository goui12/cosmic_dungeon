package net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadCollection {
    public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    private final List<BakedQuad> all;
    private final List<BakedQuad> unculled;
    private final List<BakedQuad> north;
    private final List<BakedQuad> south;
    private final List<BakedQuad> east;
    private final List<BakedQuad> west;
    private final List<BakedQuad> up;
    private final List<BakedQuad> down;

    QuadCollection(
        List<BakedQuad> all,
        List<BakedQuad> unculled,
        List<BakedQuad> north,
        List<BakedQuad> south,
        List<BakedQuad> east,
        List<BakedQuad> west,
        List<BakedQuad> up,
        List<BakedQuad> down
    ) {
        this.all = all;
        this.unculled = unculled;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.up = up;
        this.down = down;
    }

    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return switch (direction) {
            case null -> this.unculled;
            case NORTH -> this.north;
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            case UP -> this.up;
            case DOWN -> this.down;
        };
    }

    public List<BakedQuad> getAll() {
        return this.all;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder implements net.neoforged.neoforge.client.extensions.QuadCollectionBuilderExtension {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

        public QuadCollection.Builder addCulledFace(Direction direction, BakedQuad quad) {
            this.culledFaces.put(direction, quad);
            return this;
        }

        public QuadCollection.Builder addUnculledFace(BakedQuad quad) {
            this.unculledFaces.add(quad);
            return this;
        }

        private static QuadCollection createFromSublists(
            List<BakedQuad> quads, int unculledSize, int northSize, int southSize, int eastSize, int westSize, int upSize, int downSize
        ) {
            int i = 0;
            int j;
            List<BakedQuad> list = quads.subList(i, j = i + unculledSize);
            List<BakedQuad> list1 = quads.subList(j, i = j + northSize);
            int k;
            List<BakedQuad> list2 = quads.subList(i, k = i + southSize);
            List<BakedQuad> list3 = quads.subList(k, i = k + eastSize);
            int l;
            List<BakedQuad> list4 = quads.subList(i, l = i + westSize);
            List<BakedQuad> list5 = quads.subList(l, i = l + upSize);
            List<BakedQuad> list6 = quads.subList(i, i + downSize);
            return new QuadCollection(quads, list, list1, list2, list3, list4, list5, list6);
        }

        public QuadCollection build() {
            ImmutableList<BakedQuad> immutablelist = this.unculledFaces.build();
            if (this.culledFaces.isEmpty()) {
                return immutablelist.isEmpty()
                    ? QuadCollection.EMPTY
                    : new QuadCollection(immutablelist, immutablelist, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            } else {
                ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                builder.addAll(immutablelist);
                Collection<BakedQuad> collection = this.culledFaces.get(Direction.NORTH);
                builder.addAll(collection);
                Collection<BakedQuad> collection1 = this.culledFaces.get(Direction.SOUTH);
                builder.addAll(collection1);
                Collection<BakedQuad> collection2 = this.culledFaces.get(Direction.EAST);
                builder.addAll(collection2);
                Collection<BakedQuad> collection3 = this.culledFaces.get(Direction.WEST);
                builder.addAll(collection3);
                Collection<BakedQuad> collection4 = this.culledFaces.get(Direction.UP);
                builder.addAll(collection4);
                Collection<BakedQuad> collection5 = this.culledFaces.get(Direction.DOWN);
                builder.addAll(collection5);
                return createFromSublists(
                    builder.build(),
                    immutablelist.size(),
                    collection.size(),
                    collection1.size(),
                    collection2.size(),
                    collection3.size(),
                    collection4.size(),
                    collection5.size()
                );
            }
        }
    }
}
