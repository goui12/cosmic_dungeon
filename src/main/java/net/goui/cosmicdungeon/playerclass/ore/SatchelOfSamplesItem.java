// file: net/goui/cosmicdungeon/playerclass/ore/SatchelOfSamplesItem.java
package net.goui.cosmicdungeon.playerclass.ore;

import net.goui.cosmicdungeon.playerclass.api.ClassBoundItem;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class SatchelOfSamplesItem extends Item implements ClassBoundItem {
    public static final String KEY_ORE = "ore";
    public static final String KEY_CAP = "cap"; // optional capacity

    // TODO: when you add more satchel types (metal, blood, etc),
    // either move this into a shared base class or let each subtype define its own default.
    public static final int DEFAULT_CAPACITY = 100;

    public SatchelOfSamplesItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public String requiredClassId() {
        return ClassKeys.CLASS_ID_METALMANCER;
    }

    /* ---------- helpers (CustomData component) ---------- */

    private static CompoundTag readTag(ItemStack stack) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return cd.copyTag();
    }

    private static void writeTag(ItemStack stack, java.util.function.UnaryOperator<CompoundTag> mutator) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, old -> {
            CompoundTag tag = old.copyTag();
            tag = mutator.apply(tag);
            return CustomData.of(tag);
        });
    }

    /* ---------- API used by SatchelApi & HUD ---------- */

    public static int getOre(ItemStack s) {
        return readTag(s).getInt(KEY_ORE).orElse(0);
    }

    public static void setOre(ItemStack s, int v) {
        int clamped = Math.max(0, v);
        writeTag(s, tag -> {
            tag.putInt(KEY_ORE, clamped);
            return tag;
        });
    }

    public static int getCapacity(ItemStack s) {
        CompoundTag t = readTag(s);
        return t.getIntOr(KEY_CAP, DEFAULT_CAPACITY);
    }

    public static void setCapacity(ItemStack s, int cap) {
        int clamped = Math.max(1, cap);
        writeTag(s, tag -> {
            tag.putInt(KEY_CAP, clamped);
            return tag;
        });
    }

    /* ---------- tooltip (new 1.21.x signature) ---------- */

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay tooltipDisplay,
                                Consumer<Component> adder,
                                TooltipFlag flag) {
        int ore = getOre(stack);
        int cap = getCapacity(stack);
        adder.accept(Component.translatable("tooltip.cosmicdungeon.satchel.ore", ore, cap)
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.cosmicdungeon.satchel.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
