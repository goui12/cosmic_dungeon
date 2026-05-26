package net.goui.cosmicdungeon.achievement;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class CosmicAdvancementUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_CRITERION = "triggered";

    private CosmicAdvancementUtil() {}

    public static void grant(ServerPlayer sp, ResourceLocation advancementId) {
        grant(sp, advancementId, DEFAULT_CRITERION);
    }

    public static void grant(ServerPlayer sp, ResourceLocation advancementId, String criterionName) {
        if (sp == null || advancementId == null || criterionName == null || criterionName.isBlank()) return;
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;

        AdvancementHolder holder = server.getAdvancements().get(advancementId);
        if (holder == null) {
            LOGGER.debug("Advancement {} not found; cannot grant to {}", advancementId, sp.getGameProfile().getName());
            return;
        }

        sp.getAdvancements().award(holder, criterionName);
    }
}
