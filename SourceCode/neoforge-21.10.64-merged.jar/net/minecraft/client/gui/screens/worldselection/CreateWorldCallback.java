package net.minecraft.client.gui.screens.worldselection;

import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface CreateWorldCallback {
    boolean create(CreateWorldScreen createWorldScreen, LayeredRegistryAccess<RegistryLayer> registryAccess, PrimaryLevelData levelData, @Nullable Path tempDataPackDir);
}
