// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassItemRestrictionEvents.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassItemRestrictionEvents {
    private ClassItemRestrictionEvents() {}

    private static final ResourceLocation SATCHEL_ID =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "satchel_of_samples");

    private static boolean isRestrictedFor(ServerPlayer sp, ItemStack stack) {
        if (sp == null) return false;
        if (stack == null || stack.isEmpty()) return false;

        String cls = ClassNbtUtil.getClassId(sp);
        if (cls == null) cls = ClassKeys.CLASS_ID_NONE;

        Item satchel = BuiltInRegistries.ITEM.getValue(SATCHEL_ID);
        if (satchel != null && stack.getItem() == satchel) {
            // Only Metalmancer can use the Satchel of Samples
            return !ClassKeys.CLASS_ID_METALMANCER.equals(cls);
        }

        return false;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!isRestrictedFor(sp, e.getItemStack())) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
    }

    /**
     * NeoForge 1.21.x: PlayerTickEvent is abstract.
     * You must subscribe to PlayerTickEvent.Pre or PlayerTickEvent.Post.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        stripIfRestricted(sp, sp.getMainHandItem(), EquipmentSlot.MAINHAND);
        stripIfRestricted(sp, sp.getOffhandItem(), EquipmentSlot.OFFHAND);

        stripIfRestricted(sp, sp.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD);
        stripIfRestricted(sp, sp.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST);
        stripIfRestricted(sp, sp.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS);
        stripIfRestricted(sp, sp.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET);
    }

    private static void stripIfRestricted(ServerPlayer sp, ItemStack stack, EquipmentSlot slot) {
        if (sp == null) return;
        if (stack == null || stack.isEmpty()) return;
        if (!isRestrictedFor(sp, stack)) return;

        ItemStack copy = stack.copy();
        sp.setItemSlot(slot, ItemStack.EMPTY);

        Inventory inv = sp.getInventory();
        boolean inserted = inv.add(copy);
        if (!inserted) {
            sp.drop(copy, false);
        }
    }
}
