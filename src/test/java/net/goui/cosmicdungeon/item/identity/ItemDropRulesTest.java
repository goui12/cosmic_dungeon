package net.goui.cosmicdungeon.item.identity;

import java.util.List;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.economy.DeathCurrencyRecord;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.d1.D1AbilityIdentity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ItemDropRulesTest {
    @Test void modernMarkedAndLegacyAmmunitionDropWithoutChangingAuthoredComponents() {
        var modern = new ItemStack(ModItems.MENDING_STING.get(), 12);
        var legacy = new ItemStack(Items.TIPPED_ARROW, 12);
        legacy.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));
        legacy.set(DataComponents.CUSTOM_NAME, Component.literal("Mending Sting"));
        var marked = legacy.copy();
        marked.set(ModDataComponents.D1_ABILITY.get(), "mending_sting");
        for (var stack : List.of(modern, legacy, marked)) {
            assertEquals("mending_sting", D1AbilityIdentity.identify(stack));
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("authored_unknown", "keep"));
            var before = stack.copy();
            assertFalse(ItemMovementRules.flags(stack).noDrop());
            assertTrue(ItemMovementRules.flags(stack).privateStorage());
            assertTrue(ItemStack.matches(before, stack));
        }
    }

    @Test void ordinaryAndExplicitlyAttunedGearDropAndLegacyMarkersRemainPrivate() {
        for (var item : List.of(Items.ARROW, Items.SPLASH_POTION, Items.POTION, Items.IRON_CHESTPLATE,
                Items.IRON_SWORD, Items.NETHER_WART)) {
            var stack = new ItemStack(item);
            assertFalse(ItemMovementRules.flags(stack).noDrop());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putBoolean("no_drop", true);
                tag.putBoolean("class_issued", true);
                tag.putString("unknown_future", "preserve");
            });
            var before = stack.copy();
            assertFalse(ItemMovementRules.flags(stack).noDrop());
            assertTrue(ItemMovementRules.flags(stack).privateStorage());
            assertTrue(ItemStack.matches(before, stack));
        }
        var attuned = new ItemStack(Items.DIAMOND_SWORD);
        ClassItemUtil.attune(attuned, "theurgist", 1, 3, 25);
        assertFalse(ItemMovementRules.flags(attuned).noDrop());
        assertTrue(ItemMovementRules.flags(attuned).privateStorage());
    }

    @Test void currencyTokensAndNestedTokensKeepTheirNoDropGuard() {
        var token = new ItemStack(Items.PAPER);
        CustomData.update(DataComponents.CUSTOM_DATA, token, tag -> tag.putString(DeathCurrencyRecord.MARKER, "saved-token"));
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(token.copy())));
        var box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(token.copy())));
        for (var stack : List.of(token, bundle, box)) {
            var before = stack.copy();
            assertTrue(ItemMovementRules.flags(stack).noDrop());
            assertTrue(ItemMovementRules.flags(stack).privateStorage());
            assertTrue(ItemStack.matches(before, stack));
        }
    }

    @Test void uninspectableNestedContentsStillFailClosed() {
        var nested = new ItemStack(Items.APPLE);
        for (int i = 0; i < 10; i++) {
            var parent = new ItemStack(Items.SHULKER_BOX);
            parent.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nested)));
            nested = parent;
        }
        assertTrue(ItemMovementRules.flags(nested).noDrop());
        assertTrue(ItemMovementRules.flags(nested).privateStorage());
    }
}
