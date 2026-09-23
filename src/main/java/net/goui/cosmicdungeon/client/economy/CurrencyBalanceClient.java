package net.goui.cosmicdungeon.client.economy;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.BalanceDisplayView;
import net.goui.cosmicdungeon.network.CurrencyBalancePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID,value=Dist.CLIENT)
public final class CurrencyBalanceClient {
    private CurrencyBalanceClient(){}
    public static final BalanceDisplayView VIEW=new BalanceDisplayView();
    public static void receive(CurrencyBalancePayload payload) {
        var player=Minecraft.getInstance().player;
        if(player!=null && VIEW.accept(player.getUUID(),payload.owner(),payload.revision(),payload.balance(),payload.available(),payload.classChestId()))
            CurrencyBalanceOverlay.update(VIEW.balance(),VIEW.available());
    }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){VIEW.clear();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){VIEW.clear();}
    // Same connection/UUID retains the snapshot over respawn; the server sends a newer revision.
}
