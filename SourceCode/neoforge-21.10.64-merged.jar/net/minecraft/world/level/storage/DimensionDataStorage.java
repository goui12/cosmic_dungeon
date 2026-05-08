package net.minecraft.world.level.storage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public class DimensionDataStorage implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SavedData.Context context;
    private final Map<SavedDataType<?>, Optional<SavedData>> cache = new HashMap<>();
    private final DataFixer fixerUpper;
    private final HolderLookup.Provider registries;
    private final Path dataFolder;
    private CompletableFuture<?> pendingWriteFuture = CompletableFuture.completedFuture(null);

    public DimensionDataStorage(SavedData.Context context, Path dataFolder, DataFixer fixerUpper, HolderLookup.Provider registries) {
        this.context = context;
        this.fixerUpper = fixerUpper;
        this.dataFolder = dataFolder;
        this.registries = registries;
    }

    private Path getDataFile(String filename) {
        return this.dataFolder.resolve(filename + ".dat");
    }

    public <T extends SavedData> T computeIfAbsent(SavedDataType<T> type) {
        T t = this.get(type);
        if (t != null) {
            return t;
        } else {
            T t1 = (T)type.constructor().apply(this.context);
            this.set(type, t1);
            return t1;
        }
    }

    @Nullable
    public <T extends SavedData> T get(SavedDataType<T> type) {
        Optional<SavedData> optional = this.cache.get(type);
        if (optional == null) {
            optional = Optional.ofNullable(this.readSavedData(type));
            this.cache.put(type, optional);
        }

        return (T)optional.orElse(null);
    }

    @Nullable
    private <T extends SavedData> T readSavedData(SavedDataType<T> type) {
        try {
            Path path = this.getDataFile(type.id());
            if (Files.exists(path)) {
                CompoundTag compoundtag = this.readTagFromDisk(
                    type.id(), type.dataFixType(), SharedConstants.getCurrentVersion().dataVersion().version()
                );
                RegistryOps<Tag> registryops = this.registries.createSerializationContext(NbtOps.INSTANCE);
                return type.codec()
                    .apply(this.context)
                    .parse(registryops, compoundtag.get("data"))
                    .resultOrPartial(p_400985_ -> LOGGER.error("Failed to parse saved data for '{}': {}", type, p_400985_))
                    .orElse(null);
            }
        } catch (Exception exception) {
            LOGGER.error("Error loading saved data: {}", type, exception);
        }

        return null;
    }

    public <T extends SavedData> void set(SavedDataType<T> type, T value) {
        this.cache.put(type, Optional.of(value));
        value.setDirty();
    }

    public CompoundTag readTagFromDisk(String filename, @Nullable DataFixTypes dataFixType, int version) throws IOException {
        CompoundTag compoundtag1;
        try (
            InputStream inputstream = Files.newInputStream(this.getDataFile(filename));
            PushbackInputStream pushbackinputstream = new PushbackInputStream(new FastBufferedInputStream(inputstream), 2);
        ) {
            CompoundTag compoundtag;
            if (this.isGzip(pushbackinputstream)) {
                compoundtag = NbtIo.readCompressed(pushbackinputstream, NbtAccounter.unlimitedHeap());
            } else {
                try (DataInputStream datainputstream = new DataInputStream(pushbackinputstream)) {
                    compoundtag = NbtIo.read(datainputstream);
                }
            }

            if (dataFixType != null) {
                int i = NbtUtils.getDataVersion(compoundtag, 1343);
                compoundtag1 = dataFixType.update(this.fixerUpper, compoundtag, i, version);
            } else {
                compoundtag1 = compoundtag;
            }
        }

        // Neo: delete any temporary files so that we don't inflate disk space unnecessarily.
        this.pendingWriteFuture = this.pendingWriteFuture.thenRunAsync(
                () -> net.neoforged.neoforge.common.IOUtilities.tryCleanupTempFiles(this.dataFolder, filename),
                Util.ioPool()
        );

        return compoundtag1;
    }

    private boolean isGzip(PushbackInputStream inputStream) throws IOException {
        byte[] abyte = new byte[2];
        boolean flag = false;
        int i = inputStream.read(abyte, 0, 2);
        if (i == 2) {
            int j = (abyte[1] & 255) << 8 | abyte[0] & 255;
            if (j == 35615) {
                flag = true;
            }
        }

        if (i != 0) {
            inputStream.unread(abyte, 0, i);
        }

        return flag;
    }

    public CompletableFuture<?> scheduleSave() {
        Map<SavedDataType<?>, CompoundTag> map = this.collectDirtyTagsToSave();
        if (map.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            int i = Util.maxAllowedExecutorThreads();
            int j = map.size();
            if (j > i) {
                this.pendingWriteFuture = this.pendingWriteFuture.thenCompose(p_400982_ -> {
                    List<CompletableFuture<?>> list = new ArrayList<>(i);
                    int k = Mth.positiveCeilDiv(j, i);

                    for (List<Entry<SavedDataType<?>, CompoundTag>> list1 : Iterables.partition(map.entrySet(), k)) {
                        list.add(CompletableFuture.runAsync(() -> {
                            for (Entry<SavedDataType<?>, CompoundTag> entry : list1) {
                                this.tryWrite(entry.getKey(), entry.getValue());
                            }
                        }, Util.ioPool()));
                    }

                    return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                });
            } else {
                this.pendingWriteFuture = this.pendingWriteFuture
                    .thenCompose(
                        p_400974_ -> CompletableFuture.allOf(
                            map.entrySet()
                                .stream()
                                .map(p_400983_ -> CompletableFuture.runAsync(() -> this.tryWrite(p_400983_.getKey(), p_400983_.getValue()), Util.ioPool()))
                                .toArray(CompletableFuture[]::new)
                        )
                    );
            }

            return this.pendingWriteFuture;
        }
    }

    private Map<SavedDataType<?>, CompoundTag> collectDirtyTagsToSave() {
        Map<SavedDataType<?>, CompoundTag> map = new Object2ObjectArrayMap<>();
        RegistryOps<Tag> registryops = this.registries.createSerializationContext(NbtOps.INSTANCE);
        this.cache.forEach((p_400971_, p_400972_) -> p_400972_.filter(SavedData::isDirty).ifPresent(p_400978_ -> {
            map.put(p_400971_, this.encodeUnchecked(p_400971_, p_400978_, registryops));
            p_400978_.setDirty(false);
        }));
        return map;
    }

    private <T extends SavedData> CompoundTag encodeUnchecked(SavedDataType<T> type, SavedData data, RegistryOps<Tag> ops) {
        Codec<T> codec = type.codec().apply(this.context);
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.put("data", codec.encodeStart(ops, (T)data).getOrThrow());
        NbtUtils.addCurrentDataVersion(compoundtag);
        return compoundtag;
    }

    private void tryWrite(SavedDataType<?> type, CompoundTag tag) {
        Path path = this.getDataFile(type.id());

        try {
            // Neo: ensure parent directories exist if the SavedData's path contains slashes
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
            }
            net.neoforged.neoforge.common.IOUtilities.writeNbtCompressed(tag, path);
        } catch (IOException ioexception) {
            LOGGER.error("Could not save data to {}", path.getFileName(), ioexception);
        }
    }

    public void saveAndJoin() {
        this.scheduleSave().join();
    }

    @Override
    public void close() {
        this.saveAndJoin();
    }
}
