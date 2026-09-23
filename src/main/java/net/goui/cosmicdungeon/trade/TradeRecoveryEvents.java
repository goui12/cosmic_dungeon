package net.goui.cosmicdungeon.trade;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class TradeRecoveryEvents {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player)TradeRecoveryData.get(player.level().getServer()).claim(player);
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("trade").then(Commands.literal("claim").executes(c->{
            var player=c.getSource().getPlayerOrException();int restored=TradeRecoveryData.get(c.getSource().getServer()).claim(player);
            player.sendSystemMessage(Component.literal("Returned "+restored+" stored trade items."));return restored;
        })));
    }
}
