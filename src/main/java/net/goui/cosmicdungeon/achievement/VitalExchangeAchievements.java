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
            // TODO(M74, D2+): Vital Exchange I-IV starts after D1; Deadeye is unavailable here.
            // Wire actual eligible restorative-arrow transfers for all four source tiers when D2
            // identities/progression are enabled. D1 attuned ordinary arrows must grant none.
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
