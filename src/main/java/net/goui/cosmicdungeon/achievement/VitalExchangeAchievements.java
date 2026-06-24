package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class VitalExchangeAchievements {
    private VitalExchangeAchievements() {}

    private static final List<VitalExchangeCheck> CHECKS = List.of(
            new VitalExchangeCheck(Items.ARROW, ClassKeys.CLASS_ID_THEURGIST, 1, 3, CosmicAchievementIds.VITAL_EXCHANGE_1),
            new VitalExchangeCheck(Items.ARROW, ClassKeys.CLASS_ID_THEURGIST, 1, 4, CosmicAchievementIds.VITAL_EXCHANGE_2)
    );

    public static void recordVitalExchange(ServerPlayer provider, ServerPlayer receiver, ItemStack providedStack) {
        if (provider == null || receiver == null || providedStack == null || providedStack.isEmpty()) return;
        if (providedStack.getCount() <= 0) return;
        if (!ClassNbtUtil.hasClass(receiver, ClassKeys.CLASS_ID_DEADEYE)) return;

        ResourceLocation achievementId = achievementFor(providedStack);
        if (achievementId == null) return;

        CosmicAdvancementUtil.grant(provider, achievementId);
        CosmicAdvancementUtil.grant(receiver, achievementId);
    }

    private static ResourceLocation achievementFor(ItemStack stack) {
        for (VitalExchangeCheck check : CHECKS) {
            if (check.matches(stack)) return check.achievementId();
        }
        return null;
    }

    private record VitalExchangeCheck(Item expectedItem, String classId, int dungeon, int tier, ResourceLocation achievementId) {
        boolean matches(ItemStack stack) {
            return ClassItemUtil.matchesAttunedItem(stack, expectedItem, classId, dungeon, tier);
        }
    }
}
