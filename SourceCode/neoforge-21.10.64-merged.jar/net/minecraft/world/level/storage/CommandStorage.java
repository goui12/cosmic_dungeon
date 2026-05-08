package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CommandStorage {
    private static final String ID_PREFIX = "command_storage_";
    private final Map<String, CommandStorage.Container> namespaces = new HashMap<>();
    private final DimensionDataStorage storage;

    public CommandStorage(DimensionDataStorage storage) {
        this.storage = storage;
    }

    public CompoundTag get(ResourceLocation id) {
        CommandStorage.Container commandstorage$container = this.getContainer(id.getNamespace());
        return commandstorage$container != null ? commandstorage$container.get(id.getPath()) : new CompoundTag();
    }

    @Nullable
    private CommandStorage.Container getContainer(String namespace) {
        CommandStorage.Container commandstorage$container = this.namespaces.get(namespace);
        if (commandstorage$container != null) {
            return commandstorage$container;
        } else {
            CommandStorage.Container commandstorage$container1 = this.storage.get(CommandStorage.Container.type(namespace));
            if (commandstorage$container1 != null) {
                this.namespaces.put(namespace, commandstorage$container1);
            }

            return commandstorage$container1;
        }
    }

    private CommandStorage.Container getOrCreateContainer(String namespace) {
        CommandStorage.Container commandstorage$container = this.namespaces.get(namespace);
        if (commandstorage$container != null) {
            return commandstorage$container;
        } else {
            CommandStorage.Container commandstorage$container1 = this.storage.computeIfAbsent(CommandStorage.Container.type(namespace));
            this.namespaces.put(namespace, commandstorage$container1);
            return commandstorage$container1;
        }
    }

    public void set(ResourceLocation id, CompoundTag nbt) {
        this.getOrCreateContainer(id.getNamespace()).put(id.getPath(), nbt);
    }

    public Stream<ResourceLocation> keys() {
        return this.namespaces.entrySet().stream().flatMap(p_164841_ -> p_164841_.getValue().getKeys(p_164841_.getKey()));
    }

    static String createId(String namespace) {
        return "command_storage_" + namespace;
    }

    static class Container extends SavedData {
        public static final Codec<CommandStorage.Container> CODEC = RecordCodecBuilder.create(
            p_400967_ -> p_400967_.group(
                    Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(p_400968_ -> p_400968_.storage)
                )
                .apply(p_400967_, CommandStorage.Container::new)
        );
        private final Map<String, CompoundTag> storage;

        private Container(Map<String, CompoundTag> storage) {
            this.storage = new HashMap<>(storage);
        }

        private Container() {
            this(new HashMap<>());
        }

        public static SavedDataType<CommandStorage.Container> type(String namespace) {
            return new SavedDataType<>(CommandStorage.createId(namespace), CommandStorage.Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        }

        public CompoundTag get(String id) {
            CompoundTag compoundtag = this.storage.get(id);
            return compoundtag != null ? compoundtag : new CompoundTag();
        }

        public void put(String id, CompoundTag nbt) {
            if (nbt.isEmpty()) {
                this.storage.remove(id);
            } else {
                this.storage.put(id, nbt);
            }

            this.setDirty();
        }

        public Stream<ResourceLocation> getKeys(String namespace) {
            return this.storage.keySet().stream().map(p_350257_ -> ResourceLocation.fromNamespaceAndPath(namespace, p_350257_));
        }
    }
}
