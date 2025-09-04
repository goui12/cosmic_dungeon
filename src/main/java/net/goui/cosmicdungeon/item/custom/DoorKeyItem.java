package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class DoorKeyItem extends Item {
    public DoorKeyItem(Properties props) {
        super(props);
    }

    // Use the Level-based signature; omit @Override so this compiles across mappings
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        UUID id = stack.get(ModDataComponents.DOOR_LOCK_ID.get());
        if (id != null) {
            tooltip.add(Component.literal("Lock ID: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
        } else {
            tooltip.add(Component.literal("Unbound Key").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }
}
