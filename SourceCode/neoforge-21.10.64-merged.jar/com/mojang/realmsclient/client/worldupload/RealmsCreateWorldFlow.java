package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateWorldFlow {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void createWorld(
        Minecraft minecraft, Screen lastScreen, Screen resetWorldScreen, int slot, RealmsServer server, @Nullable RealmCreationTask realmCreationTask
    ) {
        CreateWorldScreen.openFresh(
            minecraft,
            () -> minecraft.setScreen(lastScreen),
            (p_448671_, p_448672_, p_448673_, p_448674_) -> {
                Path path;
                try {
                    path = createTemporaryWorldFolder(p_448672_, p_448673_, p_448674_);
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to create temporary world folder.");
                    minecraft.setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), resetWorldScreen));
                    return true;
                }

                RealmsWorldOptions realmsworldoptions = RealmsWorldOptions.createFromSettings(
                    p_448673_.getLevelSettings(), SharedConstants.getCurrentVersion().name()
                );
                RealmsSlot realmsslot = new RealmsSlot(
                    slot, realmsworldoptions, List.of(RealmsSetting.hardcoreSetting(p_448673_.getLevelSettings().hardcore()))
                );
                RealmsWorldUpload realmsworldupload = new RealmsWorldUpload(
                    path, realmsslot, minecraft.getUser(), server.id, RealmsWorldUploadStatusTracker.noOp()
                );
                minecraft.setScreenAndShow(
                    new AlertScreen(
                        realmsworldupload::cancel,
                        Component.translatable("mco.create.world.reset.title"),
                        Component.empty(),
                        CommonComponents.GUI_CANCEL,
                        false
                    )
                );
                if (realmCreationTask != null) {
                    realmCreationTask.run();
                }

                realmsworldupload.packAndUpload().handleAsync((p_438695_, p_438696_) -> {
                    if (p_438696_ != null) {
                        if (p_438696_ instanceof CompletionException completionexception) {
                            p_438696_ = completionexception.getCause();
                        }

                        if (p_438696_ instanceof RealmsUploadCanceledException) {
                            minecraft.setScreenAndShow(resetWorldScreen);
                        } else {
                            if (p_438696_ instanceof RealmsUploadFailedException realmsuploadfailedexception) {
                                LOGGER.warn("Failed to create realms world {}", realmsuploadfailedexception.getStatusMessage());
                            } else {
                                LOGGER.warn("Failed to create realms world {}", p_438696_.getMessage());
                            }

                            minecraft.setScreenAndShow(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), resetWorldScreen));
                        }
                    } else {
                        if (lastScreen instanceof RealmsConfigureWorldScreen realmsconfigureworldscreen) {
                            realmsconfigureworldscreen.fetchServerData(server.id);
                        }

                        if (realmCreationTask != null) {
                            RealmsMainScreen.play(server, lastScreen, true);
                        } else {
                            minecraft.setScreenAndShow(lastScreen);
                        }

                        RealmsMainScreen.refreshServerList();
                    }

                    return null;
                }, minecraft);
                return true;
            }
        );
    }

    private static Path createTemporaryWorldFolder(LayeredRegistryAccess<RegistryLayer> registryAccess, PrimaryLevelData levelData, @Nullable Path tempDatapackDir) throws IOException {
        Path path = Files.createTempDirectory("minecraft_realms_world_upload");
        if (tempDatapackDir != null) {
            Files.move(tempDatapackDir, path.resolve("datapacks"));
        }

        CompoundTag compoundtag = levelData.createTag(registryAccess.compositeAccess(), null);
        CompoundTag compoundtag1 = new CompoundTag();
        compoundtag1.put("Data", compoundtag);
        Path path1 = Files.createFile(path.resolve("level.dat"));
        NbtIo.writeCompressed(compoundtag1, path1);
        return path;
    }
}
