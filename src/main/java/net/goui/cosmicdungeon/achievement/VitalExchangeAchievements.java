package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VitalExchangeAchievements {
    private VitalExchangeAchievements() {}

    private static final ResourceLocation MENDING_STING_ID =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "mending_sting");
    private static final ResourceLocation VERDANT_JOLT_ID =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "verdant_jolt");

    private static final Map<ResourceLocation, ResourceLocation> ITEM_TO_ACHIEVEMENT = new LinkedHashMap<>();

    static {
        ITEM_TO_ACHIEVEMENT.put(ModItems.SCINTILLA_VITALIS.getId(), CosmicAchievementIds.VITAL_EXCHANGE_1);
        ITEM_TO_ACHIEVEMENT.put(ModItems.LUX_VITALIS.getId(), CosmicAchievementIds.VITAL_EXCHANGE_2);
        ITEM_TO_ACHIEVEMENT.put(MENDING_STING_ID, CosmicAchievementIds.VITAL_EXCHANGE_3); // TODO(v1.5.0): replace with ModItems constant when item is registered.
        ITEM_TO_ACHIEVEMENT.put(VERDANT_JOLT_ID, CosmicAchievementIds.VITAL_EXCHANGE_4); // TODO(v1.5.0): replace with ModItems constant when item is registered.
    }

    public static void recordVitalExchange(ServerPlayer provider, ServerPlayer receiver, ItemStack providedStack) {
        if (provider == null || receiver == null || providedStack == null || providedStack.isEmpty()) return;
        if (providedStack.getCount() <= 0) return;
        if (!ClassNbtUtil.hasClass(receiver, ClassKeys.CLASS_ID_DEADEYE)) return;

        ResourceLocation itemId = providedStack.getItemHolder().unwrapKey().map(k -> k.location()).orElse(null);
        if (itemId == null) return;

        ResourceLocation achievementId = ITEM_TO_ACHIEVEMENT.get(itemId);
        if (achievementId == null) return;

        CosmicAdvancementUtil.grant(provider, achievementId);
        CosmicAdvancementUtil.grant(receiver, achievementId);
    }
}
