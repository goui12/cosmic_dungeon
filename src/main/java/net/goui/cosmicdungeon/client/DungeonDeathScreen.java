package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** The same server-validated vote remains usable without leaving the death screen. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class DungeonDeathScreen {
    private static DeathScreen current;
    private static Button resurrection,playerAccept,playerDecline;
    private static TheurgistPayloads.Offer displayedPlayerOffer;
    private static int pendingTicks;
    private DungeonDeathScreen() {}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        var client=Minecraft.getInstance();
        if(current!=client.screen){current=null;resurrection=null;playerAccept=null;playerDecline=null;displayedPlayerOffer=null;pendingTicks=0;return;}
        if(pendingTicks>0)pendingTicks--;
        updateResurrection();
    }
    private static void updateResurrection(){
        if(resurrection==null)return;
        var client=Minecraft.getInstance();
        boolean dead=client.player!=null&&client.player.isDeadOrDying()&&client.getConnection()!=null;
        var playerOffer=TheurgistClient.offer();
        playerAccept.visible=playerDecline.visible=dead&&playerOffer!=null;
        playerAccept.active=playerDecline.active=playerAccept.visible&&TheurgistClient.canRespond();
        if(!java.util.Objects.equals(playerOffer,displayedPlayerOffer)){
            displayedPlayerOffer=playerOffer;
            if(playerOffer!=null){
                playerAccept.setMessage(Component.literal("Accept resurrection from "+playerOffer.name()));
                playerAccept.setTooltip(Tooltip.create(TheurgistClient.offerTooltip(playerOffer)));
                playerDecline.setTooltip(Tooltip.create(Component.literal("Decline resurrection from "+playerOffer.name()+". No supplies are spent.")));
            }
        }
        var offer=net.goui.cosmicdungeon.client.screen.D1PartyHud.resurrectionOffer();
        resurrection.visible=dead&&playerOffer==null&&offer!=null;
        resurrection.active=resurrection.visible&&pendingTicks==0;
        if(offer!=null)resurrection.setMessage(Component.literal("Accept Resurrection from "+offer.name()));
    }

    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen death)) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null
                || DungeonInstanceSlots.slotOf(client.player.level().dimension()).isEmpty()) return;
        current=death;pendingTicks=0;displayedPlayerOffer=null;
        int width=Math.min(300,death.width-16);
        resurrection=Button.builder(Component.literal("Accept Resurrection"),button->{
            var offer=net.goui.cosmicdungeon.client.screen.D1PartyHud.resurrectionOffer();
            if(TheurgistClient.offer()!=null||offer==null||client.player==null||!client.player.isDeadOrDying()||client.getConnection()==null)return;
            var revive=offer.resurrection();pendingTicks=20;button.active=false;
            net.goui.cosmicdungeon.network.ModNetwork.sendToServer(new net.goui.cosmicdungeon.network.PartyPayloads.Resurrect(
                    revive.run(),java.util.UUID.fromString(revive.mercenary()),java.util.UUID.fromString(revive.death())));
        }).bounds((death.width-width)/2,death.height/4+144,width,20).build();
        int x=(death.width-width)/2,y=death.height/4+144,declineWidth=Math.min(64,width/3);
        playerAccept=Button.builder(Component.literal("Accept resurrection"),button->{
            if(client.player==null||!client.player.isDeadOrDying()||client.getConnection()==null
                    ||displayedPlayerOffer==null||!displayedPlayerOffer.equals(TheurgistClient.offer())){updateResurrection();return;}
            TheurgistClient.acceptOffer();updateResurrection();
        }).bounds(x,y,width-declineWidth-4,20).build();
        playerDecline=Button.builder(Component.literal("Decline"),button->{
            if(client.player==null||!client.player.isDeadOrDying()||client.getConnection()==null
                    ||displayedPlayerOffer==null||!displayedPlayerOffer.equals(TheurgistClient.offer())){updateResurrection();return;}
            TheurgistClient.declineOffer();updateResurrection();
        }).bounds(x+width-declineWidth,y,declineWidth,20).build();
        event.addListener(resurrection);event.addListener(playerAccept);event.addListener(playerDecline);updateResurrection();
        event.addListener(Button.builder(Component.literal("Forfeit"), button -> {
            client.setScreen(new ConfirmScreen(yes -> {
                client.setScreen(death);
                if (yes && client.player != null && client.player.isDeadOrDying()
                        && client.getConnection() != null) {
                    // Ordered commands reuse membership, expiry, threshold and duplicate-vote guards.
                    client.getConnection().sendCommand("ff");
                    client.getConnection().sendCommand("ff yes");
                }
            }, Component.literal("Vote to forfeit?"),
                    Component.literal("Start or vote Yes on the current group forfeit. If two-thirds agree, "
                            + "everyone returns alive with their saved outside belongings. Dungeon loot is lost."),
                    Component.literal("Yes, forfeit"), Component.literal("Back")) {
                @Override public boolean isPauseScreen() { return false; }
                @Override public boolean shouldCloseOnEsc() { return false; }
                @Override public void tick() {
                    super.tick();
                    if (client.player != null && !client.player.isDeadOrDying()) client.setScreen(null);
                }
            });
        }).bounds(death.width / 2 - 100, death.height / 4 + 120, 200, 20).build());
    }
}
