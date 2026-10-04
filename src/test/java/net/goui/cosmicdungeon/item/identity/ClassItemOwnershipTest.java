package net.goui.cosmicdungeon.item.identity;

import java.util.UUID;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.economy.pricing.ItemTransferRules;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassItemOwnershipTest {
    private static ItemStack authored(int count) {
        var stack = new ItemStack(Items.DIAMOND_SWORD, count);
        ClassItemUtil.attune(stack, "bogatyr", 1, 3, 123);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Unchanged authored sword"));
        stack.setDamageValue(17);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("authored", "preserve"));
        return stack;
    }
    @Test void firstMatchingClassBindsOnceAndOtherPlayersCannotCollect() {
        var stack = authored(1);
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        assertFalse(ClassItemOwnership.bind(stack, other, "judicator"));
        assertFalse(ClassItemOwnership.present(stack));
        assertTrue(ClassItemOwnership.bind(stack, owner, "bogatyr"));
        assertFalse(ClassItemOwnership.bind(stack, other, "bogatyr"));
        assertTrue(ClassItemOwnership.mayAcquire(stack, owner, "judicator"));
        assertFalse(ClassItemOwnership.mayAcquire(stack, other, "bogatyr"));
        assertEquals(owner, ClassItemOwnership.owner(stack));
    }
    @Test void onlyOwnershipChangesAndNativeCodecPreservesIt() {
        var original = authored(1);
        var stack = original.copy();
        UUID owner = UUID.randomUUID();
        assertTrue(ClassItemOwnership.bind(stack, owner, "bogatyr"));
        var ops = RegistryOps.create(NbtOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        assertTrue(ItemStack.matches(stack, restored));
        assertEquals(owner, ClassItemOwnership.owner(restored));
        ClassItemOwnership.unbindRemainder(restored, owner);
        assertTrue(ItemStack.matches(original, restored));
    }
    @Test void partialPickupLeavesUncollectedRemainderUnbound() {
        var stack = new ItemStack(Items.ARROW, 30);
        ClassItemUtil.attune(stack, "bogatyr", 1, 3, 0);
        UUID owner = UUID.randomUUID();
        assertTrue(ClassItemOwnership.bind(stack, owner, "bogatyr"));
        var inserted = stack.split(7);
        ClassItemOwnership.unbindRemainder(stack, owner);
        assertEquals(7, inserted.getCount());
        assertEquals(owner, ClassItemOwnership.owner(inserted));
        assertEquals(23, stack.getCount());
        assertFalse(ClassItemOwnership.present(stack));
    }
    @Test void inventoryFailureRestoresExactOriginalAndOtherOwnerIsNeverRemoved() {
        var stack = authored(1);
        var before = stack.copy();
        UUID owner = UUID.randomUUID();
        assertTrue(ClassItemOwnership.bind(stack, owner, "bogatyr"));
        ClassItemOwnership.unbindRemainder(stack, UUID.randomUUID());
        assertEquals(owner, ClassItemOwnership.owner(stack));
        ClassItemOwnership.unbindRemainder(stack, owner);
        assertTrue(ItemStack.matches(before, stack));
    }
    @Test void malformedOwnerAndMalformedAttunementFailClosed() {
        var stack = authored(1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(ClassItemOwnership.KEY, "broken"));
        assertFalse(ClassItemOwnership.bind(stack, UUID.randomUUID(), "bogatyr"));
        assertFalse(ClassItemOwnership.mayAcquire(stack, UUID.randomUUID(), "bogatyr"));
        assertEquals("broken", stack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr(ClassItemOwnership.KEY, ""));
        var malformed = new ItemStack(Items.DIAMOND_SWORD);
        malformed.set(ModDataComponents.CLASS_ATTUNEMENT.get(), "unknown_future_class");
        assertFalse(ClassItemOwnership.mayAcquire(malformed, UUID.randomUUID(), "bogatyr"));
    }
    @Test void attunedDropsStayPrivateAndCannotBeTraded() {
        var stack = authored(1);
        assertFalse(ItemMovementRules.flags(stack).noDrop());
        assertTrue(ItemMovementRules.flags(stack).privateStorage());
        assertTrue(ClassItemOwnership.bind(stack, UUID.randomUUID(), "bogatyr"));
        assertFalse(ItemTransferRules.tradeEligible(stack));
        ClassItemUtil.clearAttunement(stack);
        assertTrue(ClassItemOwnership.present(stack));
        assertTrue(ItemMovementRules.flags(stack).privateStorage());
        assertFalse(ItemTransferRules.tradeEligible(stack));
    }
    @Test void ordinaryQuestNoDropAndOrdinaryLootKeepTheirRules() {
        var ordinary = new ItemStack(Items.APPLE);
        assertFalse(ClassItemOwnership.bind(ordinary, UUID.randomUUID(), "bogatyr"));
        assertFalse(ItemMovementRules.flags(ordinary).noDrop());
        CustomData.update(DataComponents.CUSTOM_DATA, ordinary, tag -> tag.putBoolean("no_drop", true));
        assertTrue(ItemMovementRules.flags(ordinary).noDrop());
    }
}
