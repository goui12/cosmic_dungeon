package net.goui.cosmicdungeon.block.entity;

import java.util.UUID;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassLockedChestOwnershipTest {
    private static RegistryAccess lookup() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
    private static ClassLockedChestBlockEntity chest() {
        return new ClassLockedChestBlockEntity(BlockPos.ZERO, ModBlocks.PYROCLAST_CHEST.get().defaultBlockState());
    }
    private static CompoundTag ownerTag() {
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup());
        new ClassChestOwnership(UUID.randomUUID(), "PlayerOne").save(out);
        return out.buildResult();
    }
    private static ItemStack authoredStack() {
        var stack = new ItemStack(Items.APPLE, 7);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Author's named item"));
        return stack;
    }

    @Test void ownershipAndAuthoredInventorySurviveNativeSaveRoundTrip() {
        var original = chest();
        ItemStack authored = authoredStack();
        original.setItem(4, authored.copy());
        var saved = original.saveWithoutMetadata(lookup());
        saved.merge(ownerTag());
        var restored = chest();
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, lookup(), saved));
        assertTrue(ItemStack.matches(authored, restored.getItem(4)));
        assertEquals("PlayerOne", restored.getSlotOwnerLabel().getString());
        assertFalse(restored.canPlaceItem(0, new ItemStack(Items.APPLE)));
        assertFalse(restored.canTakeItem(original, 4, restored.getItem(4)));

        var again = chest();
        again.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, lookup(),
                restored.saveWithoutMetadata(lookup())));
        assertTrue(ItemStack.matches(authored, again.getItem(4)));
        assertEquals(restored.getUpdateTag(lookup()), again.getUpdateTag(lookup()));
    }

    @Test void labelUpdatesDoNotResetInventoryAndNeverSendItsContents() {
        var chest = chest();
        ItemStack authored = authoredStack();
        chest.setItem(4, authored.copy());
        assertNull(chest.getSlotOwnerLabel());
        assertTrue(chest.canPlaceItem(0, authored));
        assertTrue(chest.canTakeItem(chest, 4, authored));
        chest.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING, lookup(), ownerTag()));
        assertEquals("PlayerOne", chest.getSlotOwnerLabel().getString());
        assertTrue(ItemStack.matches(authored, chest.getItem(4)));
        assertEquals(java.util.Set.of(ClassChestOwnership.TAG), chest.getUpdateTag(lookup()).keySet());

        chest.onDataPacket(null, TagValueInput.create(ProblemReporter.DISCARDING, lookup(), new CompoundTag()));
        assertNull(chest.getSlotOwnerLabel());
        assertTrue(ItemStack.matches(authored, chest.getItem(4)));
        assertTrue(chest.getUpdateTag(lookup()).isEmpty());
    }
}
