package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomSequences extends SavedData {
    public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<>(
        "random_sequences",
        p_400904_ -> new RandomSequences(p_400904_.worldSeed()),
        p_400907_ -> codec(p_400907_.worldSeed()),
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );
    private final long worldSeed;
    private int salt;
    private boolean includeWorldSeed = true;
    private boolean includeSequenceId = true;
    private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap<>();

    public RandomSequences(long seed) {
        this.worldSeed = seed;
    }

    private RandomSequences(long worldSeed, int salt, boolean includeWorldSeed, boolean includeSequenceId, Map<ResourceLocation, RandomSequence> sequences) {
        this.worldSeed = worldSeed;
        this.salt = salt;
        this.includeWorldSeed = includeWorldSeed;
        this.includeSequenceId = includeSequenceId;
        this.sequences.putAll(sequences);
    }

    public static Codec<RandomSequences> codec(long worldSeed) {
        return RecordCodecBuilder.create(
            p_400903_ -> p_400903_.group(
                    RecordCodecBuilder.point(worldSeed),
                    Codec.INT.fieldOf("salt").forGetter(p_400908_ -> p_400908_.salt),
                    Codec.BOOL.optionalFieldOf("include_world_seed", true).forGetter(p_400909_ -> p_400909_.includeWorldSeed),
                    Codec.BOOL.optionalFieldOf("include_sequence_id", true).forGetter(p_400905_ -> p_400905_.includeSequenceId),
                    Codec.unboundedMap(ResourceLocation.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(p_400906_ -> p_400906_.sequences)
                )
                .apply(p_400903_, RandomSequences::new)
        );
    }

    public RandomSource get(ResourceLocation location) {
        RandomSource randomsource = this.sequences.computeIfAbsent(location, this::createSequence).random();
        return new RandomSequences.DirtyMarkingRandomSource(randomsource);
    }

    private RandomSequence createSequence(ResourceLocation location) {
        return this.createSequence(location, this.salt, this.includeWorldSeed, this.includeSequenceId);
    }

    private RandomSequence createSequence(ResourceLocation location, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        long i = (includeWorldSeed ? this.worldSeed : 0L) ^ salt;
        return new RandomSequence(i, includeSequenceId ? Optional.of(location) : Optional.empty());
    }

    public void forAllSequences(BiConsumer<ResourceLocation, RandomSequence> action) {
        this.sequences.forEach(action);
    }

    public void setSeedDefaults(int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        this.salt = salt;
        this.includeWorldSeed = includeWorldSeed;
        this.includeSequenceId = includeSequenceId;
    }

    public int clear() {
        int i = this.sequences.size();
        this.sequences.clear();
        return i;
    }

    public void reset(ResourceLocation sequence) {
        this.sequences.put(sequence, this.createSequence(sequence));
    }

    public void reset(ResourceLocation sequence, int seed, boolean includeWorldSeed, boolean includeSequenceId) {
        this.sequences.put(sequence, this.createSequence(sequence, seed, includeWorldSeed, includeSequenceId));
    }

    class DirtyMarkingRandomSource implements RandomSource {
        private final RandomSource random;

        DirtyMarkingRandomSource(RandomSource random) {
            this.random = random;
        }

        @Override
        public RandomSource fork() {
            RandomSequences.this.setDirty();
            return this.random.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            RandomSequences.this.setDirty();
            return this.random.forkPositional();
        }

        @Override
        public void setSeed(long seed) {
            RandomSequences.this.setDirty();
            this.random.setSeed(seed);
        }

        @Override
        public int nextInt() {
            RandomSequences.this.setDirty();
            return this.random.nextInt();
        }

        @Override
        public int nextInt(int bound) {
            RandomSequences.this.setDirty();
            return this.random.nextInt(bound);
        }

        @Override
        public long nextLong() {
            RandomSequences.this.setDirty();
            return this.random.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            RandomSequences.this.setDirty();
            return this.random.nextBoolean();
        }

        @Override
        public float nextFloat() {
            RandomSequences.this.setDirty();
            return this.random.nextFloat();
        }

        @Override
        public double nextDouble() {
            RandomSequences.this.setDirty();
            return this.random.nextDouble();
        }

        @Override
        public double nextGaussian() {
            RandomSequences.this.setDirty();
            return this.random.nextGaussian();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else {
                return other instanceof RandomSequences.DirtyMarkingRandomSource randomsequences$dirtymarkingrandomsource
                    ? this.random.equals(randomsequences$dirtymarkingrandomsource.random)
                    : false;
            }
        }
    }
}
