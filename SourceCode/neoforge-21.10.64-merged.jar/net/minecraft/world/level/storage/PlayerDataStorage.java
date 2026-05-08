package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class PlayerDataStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final File playerDir;
    protected final DataFixer fixerUpper;
    private static final DateTimeFormatter FORMATTER = FileNameDateFormatter.create();

    public PlayerDataStorage(LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer fixerUpper) {
        this.fixerUpper = fixerUpper;
        this.playerDir = levelStorageAccess.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile();
        this.playerDir.mkdirs();
    }

    public void save(Player player) {
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, player.registryAccess());
            player.saveWithoutId(tagvalueoutput);
            Path path = this.playerDir.toPath();
            Path path1 = Files.createTempFile(path, player.getStringUUID() + "-", ".dat");
            CompoundTag compoundtag = tagvalueoutput.buildResult();
            NbtIo.writeCompressed(compoundtag, path1);
            Path path2 = path.resolve(player.getStringUUID() + ".dat");
            Path path3 = path.resolve(player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(path2, path1, path3);
            net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(player, playerDir, player.getStringUUID());
        } catch (Exception exception) {
            LOGGER.warn("Failed to save player data for {}", player.getPlainTextName());
        }
    }

    private void backup(NameAndId nameAndId, String suffix) {
        Path path = this.playerDir.toPath();
        String s = nameAndId.id().toString();
        Path path1 = path.resolve(s + suffix);
        Path path2 = path.resolve(s + "_corrupted_" + LocalDateTime.now().format(FORMATTER) + suffix);
        if (Files.isRegularFile(path1)) {
            try {
                Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (Exception exception) {
                LOGGER.warn("Failed to copy the player.dat file for {}", nameAndId.name(), exception);
            }
        }
    }

    private Optional<CompoundTag> load(NameAndId nameAndId, String suffix) {
        File file1 = new File(this.playerDir, nameAndId.id().toString() + suffix);
        if (file1.exists() && file1.isFile()) {
            try {
                return Optional.of(NbtIo.readCompressed(file1.toPath(), NbtAccounter.unlimitedHeap()));
            } catch (Exception exception) {
                LOGGER.warn("Failed to load player data for {}", nameAndId.name());
            }
        }

        return Optional.empty();
    }

    public Optional<CompoundTag> load(NameAndId nameAndId) {
        Optional<CompoundTag> optional = this.load(nameAndId, ".dat");
        if (optional.isEmpty()) {
            this.backup(nameAndId, ".dat");
        }

        return optional.or(() -> this.load(nameAndId, ".dat_old")).map(p_432736_ -> {
            int i = NbtUtils.getDataVersion(p_432736_, -1);
            return DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, p_432736_, i);
        });
    }

    public File getPlayerDir() {
        return playerDir;
    }
}
