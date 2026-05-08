package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ChargedProjectiles implements TooltipProvider {
    public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
    public static final Codec<ChargedProjectiles> CODEC = ItemStack.CODEC.listOf().xmap(ChargedProjectiles::new, p_331186_ -> p_331186_.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStack.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(ChargedProjectiles::new, p_330917_ -> p_330917_.items);
    private final List<ItemStack> items;

    private ChargedProjectiles(List<ItemStack> items) {
        this.items = items;
    }

    public static ChargedProjectiles of(ItemStack stack) {
        return new ChargedProjectiles(List.of(stack.copy()));
    }

    public static ChargedProjectiles of(List<ItemStack> stack) {
        return new ChargedProjectiles(List.copyOf(Lists.transform(stack, ItemStack::copy)));
    }

    public boolean contains(Item item) {
        for (ItemStack itemstack : this.items) {
            if (itemstack.is(item)) {
                return true;
            }
        }

        return false;
    }

    public List<ItemStack> getItems() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
            ? true
            : other instanceof ChargedProjectiles chargedprojectiles && ItemStack.listMatches(this.items, chargedprojectiles.items);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    @Override
    public String toString() {
        return "ChargedProjectiles[items=" + this.items + "]";
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
        ItemStack itemstack = null;
        int i = 0;

        for (ItemStack itemstack1 : this.items) {
            if (itemstack == null) {
                itemstack = itemstack1;
                i = 1;
            } else if (ItemStack.matches(itemstack, itemstack1)) {
                i++;
            } else {
                addProjectileTooltip(context, tooltipAdder, itemstack, i);
                itemstack = itemstack1;
                i = 1;
            }
        }

        if (itemstack != null) {
            addProjectileTooltip(context, tooltipAdder, itemstack, i);
        }
    }

    private static void addProjectileTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, ItemStack stack, int count) {
        if (count == 1) {
            tooltipAdder.accept(Component.translatable("item.minecraft.crossbow.projectile.single", stack.getDisplayName()));
        } else {
            tooltipAdder.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", count, stack.getDisplayName()));
        }

        TooltipDisplay tooltipdisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        stack.addDetailsToTooltip(
            context,
            tooltipdisplay,
            null,
            TooltipFlag.NORMAL,
            p_399415_ -> tooltipAdder.accept(Component.literal("  ").append(p_399415_).withStyle(ChatFormatting.GRAY))
        );
    }
}
