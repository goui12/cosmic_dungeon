// file: net/goui/cosmicdungeon/playerclass/ore/SatchelHudOverlay.java
package net.goui.cosmicdungeon.playerclass.ore;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class SatchelHudOverlay {

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer p = mc.player;
        if (p == null) return;

        // Optional: skip HUD while your custom Metalmancer inventory is open, if you want
        // if (mc.screen instanceof ExtraInventoryScreen) return;

        int ore = 0, cap = 0;

        // 1) Try main inventory first
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            // TODO: when adding more satchels, consider a shared interface / base:
            // if (s.getItem() instanceof AbstractSatchelItem satchel) { ... }
            if (s.getItem() instanceof SatchelOfSamplesItem) {
                ore = SatchelOfSamplesItem.getOre(s);
                cap = SatchelOfSamplesItem.getCapacity(s);
                break;
            }
        }

        // 2) If not found, try the client-side mirror of extra 3 slots in player PD
        if (ore == 0 && cap == 0) {
            CompoundTag root = p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
            CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

            NonNullList<ItemStack> list = NonNullList.withSize(3, ItemStack.EMPTY);
            ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), extraTag);
            ContainerHelper.loadAllItems(in, list);

            for (ItemStack s : list) {
                if (s.getItem() instanceof SatchelOfSamplesItem) {
                    ore = SatchelOfSamplesItem.getOre(s);
                    cap = SatchelOfSamplesItem.getCapacity(s);
                    break;
                }
            }
        }

        if (cap <= 0 && ore <= 0) return; // no satchel anywhere

        GuiGraphics g = e.getGuiGraphics();
        int screenH = g.guiHeight();

        int barX = 10;
        int barY = screenH - 55;
        int barW = 81;
        int barH = 9;

        int max = (cap > 0 ? cap : SatchelOfSamplesItem.DEFAULT_CAPACITY);
        float pct = max == 0 ? 0f : Math.max(0f, Math.min(1f, ore / (float) max));
        int fill = Math.round(barW * pct);

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xAA000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xAA333333);
        g.fill(barX, barY, barX + fill, barY + barH, 0xFF7F7F7F);

        String txt = (cap > 0 ? (ore + " / " + cap) : String.valueOf(ore));
        int textW = mc.font.width(txt);
        int tx = barX + (barW - textW) / 2;
        int ty = barY + (barH - mc.font.lineHeight) / 2;
        g.drawString(mc.font, txt, tx, ty, 0xFFFFFFFF, false);
    }
}
