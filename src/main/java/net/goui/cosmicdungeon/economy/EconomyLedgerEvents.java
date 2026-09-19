package net.goui.cosmicdungeon.economy;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class EconomyLedgerEvents {
    private EconomyLedgerEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();if(server.getTickCount()%D1EconomyConfig.LEDGER_FLUSH_TICKS.get()!=0)return;
        var data=PlayerCurrencyData.get(server);
        if(data.ledgerPending()&&!data.flushVerified())com.mojang.logging.LogUtils.getLogger().warn("Economy ledger backlog/save requires recovery; evidence retained");
        if(data.claimDailyReport(Math.floorDiv(System.currentTimeMillis(),86_400_000L)))report(server);
    }
    private static String report(MinecraftServer server){
        var report=PlayerCurrencyData.get(server).supplySnapshot();
        com.mojang.logging.LogUtils.getLogger().info("COSMIC_ECONOMY_REPORT {}",report);
        if(!report.getStringOr("difference","").equals("0"))com.mojang.logging.LogUtils.getLogger().warn("Unexplained currency supply difference: {}",report);
        return report.toString();
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("currency").then(Commands.literal("review").requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.argument("player",net.minecraft.commands.arguments.UuidArgument.uuid()).executes(context->{
                    var owner=net.minecraft.commands.arguments.UuidArgument.getUuid(context,"player");
                    var row=PlayerCurrencyData.get(context.getSource().getServer()).finalReviews().getCompoundOrEmpty(owner.toString());
                    context.getSource().sendSuccess(()->Component.literal(row.isEmpty()?"No final economic review record for "+owner:row.toString()),false);return 1;
                }))).then(Commands.literal("report").requires(AccessPolicy::requireDeveloperOrConsole)
                .executes(context->{String text=report(context.getSource().getServer());context.getSource().sendSuccess(()->Component.literal(text),false);return 1;})));
    }
}
