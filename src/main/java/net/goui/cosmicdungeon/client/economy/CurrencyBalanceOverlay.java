package net.goui.cosmicdungeon.client.economy;

import java.util.List;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.BalanceDisplayView;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Uses the same five registered currency icons as TradeScreen. No new assets or interactive slots. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID,value=Dist.CLIENT)
public final class CurrencyBalanceOverlay {
    private CurrencyBalanceOverlay(){}
    private static ItemStack[] icons;
    private static final String[] NAMES={"Anchors","Crowns","Seals","Marks","Trace"};
    private static final String[] counts={"0","0","0","0","0"};
    private static String total="0",available="0";
    public static void update(long balance,long spendable) {
        long[] values=BalanceDisplayView.denominations(balance);
        for(int i=0;i<5;i++)counts[i]=Long.toString(values[i]);
        total=Long.toString(balance);available=Long.toString(spendable);
    }
    private static boolean ready() {
        var mc=Minecraft.getInstance();
        return mc.player!=null && CurrencyBalanceClient.VIEW.ready(mc.player.getUUID());
    }
    private static void icons() {
        if(icons==null) icons=new ItemStack[]{new ItemStack(ModItems.ATTUNEMENT_ANCHOR.get()),
                new ItemStack(ModItems.ATTUNEMENT_CROWN.get()),new ItemStack(ModItems.ATTUNEMENT_SEAL.get()),
                new ItemStack(ModItems.ATTUNEMENT_MARK.get()),new ItemStack(ModItems.ATTUNEMENT_TRACE.get())};
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!ready()||mc.options.hideGui||mc.screen!=null||mc.getDebugOverlay().showDebugScreen())return;
        draw(event.getGuiGraphics(),8,8,mc.getWindow().getGuiScaledWidth()-16,false,-1,-1);
    }
    @SubscribeEvent public static void screen(ScreenEvent.Render.Post event) {
        if(!ready()||!(event.getScreen() instanceof AbstractContainerScreen<?> screen))return;
        boolean inventory=screen instanceof InventoryScreen;
        if(!inventory && !CurrencyBalanceClient.VIEW.classChest(screen.getMenu().containerId))return;
        int width=screen.width;
        // Header keeps currency outside slots, equipment, recipe book and right-hand potion effects.
        // Vanilla's minimum 320x240 GUI leaves this space above both inventory and three-row class chests.
        int y=Math.max(2,screen.getGuiTop()-32);
        draw(event.getGuiGraphics(),8,y,width-16,true,event.getMouseX(),event.getMouseY());
    }
    private static void draw(GuiGraphics g,int x,int y,int maxWidth,boolean tooltip,int mouseX,int mouseY) {
        icons();
        var font=Minecraft.getInstance().font;
        int width=12;
        for(String count:counts)width+=22+font.width(count);
        width=Math.min(width,Math.max(100,maxWidth));
        int remaining=width-12;
        g.fill(x,y,x+width,y+29,0xC0181820);
        g.drawString(font,Component.translatable("hud.cosmicdungeon.account"),x+5,y+2,0xFFE4C98A,false);
        int ix=x+5;
        for(int i=0;i<5;i++){
            int cell=Math.max(20,Math.min(22+font.width(counts[i]),remaining-(4-i)*20));
            remaining-=cell;
            g.renderItem(icons[i],ix,y+12);
            String shown=font.plainSubstrByWidth(counts[i],Math.max(5,cell-20));
            g.drawString(font,shown,ix+18,y+16,0xFFFFFFFF,false);
            if(tooltip && mouseX>=ix&&mouseX<ix+cell&&mouseY>=y+11&&mouseY<y+29)
                g.setComponentTooltipForNextFrame(font,List.of(Component.literal(counts[i]+" "+NAMES[i]),
                        Component.literal(total+" Trace total"),Component.literal(available+" Trace available")),mouseX,mouseY);
            ix+=cell;
        }
        if(tooltip && mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+11)
            g.setComponentTooltipForNextFrame(font,List.of(Component.literal(total+" Trace total"),
                    Component.literal(available+" Trace available"),
                    Component.translatable("hud.cosmicdungeon.account.read_only")),mouseX,mouseY);
    }
    // TODO(native TEST): verify GUI scales/recipe-book positioning, resource packs and two licensed clients;
    // exercise deposits, reserved payments, cancellation, death, respawn, reconnect and dungeon travel.
}
