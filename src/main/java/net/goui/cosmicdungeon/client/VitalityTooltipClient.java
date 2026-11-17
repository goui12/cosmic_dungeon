package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class VitalityTooltipClient {
    private VitalityTooltipClient(){}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        VitalityPotions.in(event.getItemStack()).ifPresent(rank->{
            event.getToolTip().add(1,Component.literal("Max health +"+rank.percent()+"% of base health (45 minutes)")
                    .withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(2,Component.literal("Does not heal. Stronger rank replaces weaker; equal ranks do not refresh.")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}
