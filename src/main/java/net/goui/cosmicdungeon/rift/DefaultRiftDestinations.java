package net.goui.cosmicdungeon.rift;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Optional;

public final class DefaultRiftDestinations {
    public static final String MAIN_VILLAGE = "main_village";
    private static final Logger LOGGER = LogUtils.getLogger();

    private DefaultRiftDestinations() {}

    public static void ensureDefaults(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("[DefaultRiftDestinations] Cannot seed default rift destinations: server is unavailable.");
            return;
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            LOGGER.warn("[DefaultRiftDestinations] Cannot seed {}: Overworld is unavailable.", MAIN_VILLAGE);
            return;
        }
        RiftRegistryData data;
        try {
            data = RiftRegistryData.get(overworld);
        } catch (Exception e) {
            LOGGER.warn("[DefaultRiftDestinations] Cannot seed {}: rift saved data is unavailable.", MAIN_VILLAGE, e);
            return;
        }
        if (data.destinationExists(MAIN_VILLAGE)) return;
        BlockPos pos = overworld.getLevelData().getRespawnData().pos();
        if (data.createDestination(MAIN_VILLAGE, overworld.dimension().location(), pos)) {
            LOGGER.info("[DefaultRiftDestinations] Created default rift destination '{}' at {} {}.", MAIN_VILLAGE, overworld.dimension().location(), pos.toShortString());
        }
    }

    public static Optional<ResolvedDestination> resolve(MinecraftServer server, String name) {
        if (server == null || name == null || name.isBlank()) return Optional.empty();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return Optional.empty();
        RiftRegistryData.DestinationRecord dest = RiftRegistryData.get(overworld).getDestination(name).orElse(null);
        if (dest == null) return Optional.empty();
        ResourceLocation dimId = ResourceLocation.tryParse(dest.dimensionId());
        if (dimId == null) return Optional.empty();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (level == null) return Optional.empty();
        return Optional.of(new ResolvedDestination(level, dest.pos()));
    }

    public static Optional<ResolvedDestination> resolveMainVillage(MinecraftServer server) {
        Optional<ResolvedDestination> main = resolve(server, MAIN_VILLAGE);
        if (main.isPresent()) return main;
        Optional<ResolvedDestination> village = resolve(server, "village");
        if (village.isPresent()) return village;
        return resolve(server, "Main Village");
    }

    public static boolean teleportToMainVillage(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return false;
        Optional<ResolvedDestination> resolved = resolveMainVillage(player.level().getServer());
        if (resolved.isEmpty()) {
            player.sendSystemMessage(Component.literal("Main Village destination is not configured.").withStyle(ChatFormatting.RED));
            return false;
        }
        ResolvedDestination dest = resolved.get();
        boolean ok = SafeTeleportUtil.teleportSafely(player, dest.level(), dest.pos(), player.getYRot(), player.getXRot());
        if (!ok) player.sendSystemMessage(Component.literal("Main Village destination is blocked or unsafe.").withStyle(ChatFormatting.RED));
        return ok;
    }

    public record ResolvedDestination(ServerLevel level, BlockPos pos) {}
}
