// file: src/main/java/net/goui/cosmicdungeon/playerclass/ore/SatchelHudOverlay.java
package net.goui.cosmicdungeon.playerclass.ore;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class SatchelHudOverlay {
    private SatchelHudOverlay() {}

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        Player p = mc.player;
        if (p == null || !ClassNbtUtil.isMetalmancer(p)) return;

        // Optional: skip HUD while your custom Metalmancer inventory is open, if you want
        // if (mc.screen instanceof ExtraInventoryScreen) return;

        int ore = 0;
        int cap = 0;
        boolean foundSatchel = false;

        // 1) Try main inventory first
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof SatchelOfSamplesItem) {
                ore = SatchelOfSamplesItem.getOre(s);
                cap = SatchelOfSamplesItem.getCapacity(s);
                foundSatchel = true;
                break;
            }
        }

        // 2) If not found, try the client-side mirror of extra 3 slots in player PD
        if (!foundSatchel) {
            CompoundTag root = p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
            CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

            NonNullList<ItemStack> list = NonNullList.withSize(3, ItemStack.EMPTY);
            ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), extraTag);
            ContainerHelper.loadAllItems(in, list);

            for (ItemStack s : list) {
                if (s.getItem() instanceof SatchelOfSamplesItem) {
                    ore = SatchelOfSamplesItem.getOre(s);
                    cap = SatchelOfSamplesItem.getCapacity(s);
                    foundSatchel = true;
                    break;
                }
            }
        }

        if (!foundSatchel) return;

        // If a satchel exists but capacity wasn't read for some reason, fall back
        int max = cap > 0 ? cap : SatchelOfSamplesItem.DEFAULT_CAPACITY;
        if (max <= 0) return;

        GuiGraphics g = e.getGuiGraphics();
        int screenH = g.guiHeight();

        int barX = 10;
        int barY = screenH - 55;
        int barW = 81;
        int barH = 9;

        float pct = Mth.clamp(ore / (float) max, 0f, 1f);
        int fill = Math.round(barW * pct);

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xAA000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xAA333333);
        g.fill(barX, barY, barX + fill, barY + barH, 0xFF7F7F7F);

        String txt = cap > 0 ? (ore + " / " + cap) : String.valueOf(ore);
        int textW = mc.font.width(txt);
        int tx = barX + (barW - textW) / 2;
        int ty = barY + (barH - mc.font.lineHeight) / 2;
        g.drawString(mc.font, txt, tx, ty, 0xFFFFFFFF, false);
    }
}
