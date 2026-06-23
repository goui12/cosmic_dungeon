// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassItemRestrictionEvents.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassItemRestrictionEvents {
    private ClassItemRestrictionEvents() {}

    private static boolean denyWrongClassUse(ServerPlayer sp, ItemStack stack) {
        if (ClassItemEquipmentGuard.canUse(sp, stack)) return false;
        ClassItemEquipmentGuard.denyUse(sp, stack);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (denyWrongClassUse(sp, sp.getMainHandItem())) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (denyWrongClassUse(sp, sp.getMainHandItem())) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (denyWrongClassUse(sp, e.getItemStack())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!denyWrongClassUse(sp, e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!denyWrongClassUse(sp, e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!denyWrongClassUse(sp, e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        rejectInvalidArmor(sp, EquipmentSlot.HEAD);
        rejectInvalidArmor(sp, EquipmentSlot.CHEST);
        rejectInvalidArmor(sp, EquipmentSlot.LEGS);
        rejectInvalidArmor(sp, EquipmentSlot.FEET);
    }

    private static void rejectInvalidArmor(ServerPlayer sp, EquipmentSlot slot) {
        ItemStack stack = sp.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) return;
        if (ClassItemEquipmentGuard.canWear(sp, stack)) return;
        ItemStack copy = stack.copy();
        sp.setItemSlot(slot, ItemStack.EMPTY);
        Inventory inv = sp.getInventory();
        boolean inserted = inv.add(copy);
        if (!inserted) sp.drop(copy, false);
        ClassItemEquipmentGuard.denyWear(sp, copy);
    }
}
