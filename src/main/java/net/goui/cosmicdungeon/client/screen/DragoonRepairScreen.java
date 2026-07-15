package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.network.DragoonRepairPayloads;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairMenu;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public class DragoonRepairScreen extends AbstractContainerScreen<DragoonRepairMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/container/repair_affinity_window.png");
    public DragoonRepairScreen(DragoonRepairMenu menu, Inventory inv, Component title) { super(menu, inv, title); imageWidth=256; imageHeight=256; inventoryLabelY=162; }
    @Override protected void init(){ super.init(); int x=leftPos, y=topPos; addRenderableWidget(Button.builder(Component.literal("-M"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("mark",-1))).bounds(x+24,y+92,28,18).build()); addRenderableWidget(Button.builder(Component.literal("+M"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("mark",1))).bounds(x+54,y+92,28,18).build()); addRenderableWidget(Button.builder(Component.literal("-T"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("trace",-1))).bounds(x+84,y+92,28,18).build()); addRenderableWidget(Button.builder(Component.literal("+T"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("trace",1))).bounds(x+114,y+92,28,18).build()); for(int i=1;i<=4;i++){ final int u=i; addRenderableWidget(Button.builder(Component.literal(label(i)), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_SelectUnits(u))).bounds(x+24+(i-1)*52,y+116,50,18).build()); } addRenderableWidget(Button.builder(Component.literal("Ready"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_TargetReady(true))).bounds(x+34,y+140,64,18).build()); addRenderableWidget(Button.builder(Component.literal("Repair"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_Repair())).bounds(x+158,y+140,64,18).build()); addRenderableWidget(Button.builder(Component.literal("Cancel"), b->ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_Cancel())).bounds(x+96,y+8,64,18).build()); }
    private static String label(int i){ return switch(i){case 1->"Light";case 2->"Standard";case 3->"Heavy";default->"Full";}; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){ g.blit(RenderPipelines.GUI_TEXTURED, BG,leftPos,topPos,0.0F,0.0F,imageWidth,imageHeight,imageWidth,imageHeight); }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){ RepairClientState.View v=RepairClientState.current(); g.drawString(font,"Repair Affinity",88,16,0x404040,false); if(v==null){ g.drawString(font,"Waiting for server...",70,72,0xAA0000,false); return; } g.drawString(font,v.viewerDragoon()?"You are the Dragoon":"You are the customer",22,30,0x404040,false); g.drawString(font,"Customer: "+v.targetName(),22,64,0x404040,false); g.drawString(font,"Fee: "+ CurrencyAmount.ofTrace(v.offeredFeeTrace()).formatNormalized(),22,82,0x404040,false); g.drawString(font,"Dragoon: "+v.dragoonName(),132,48,0x404040,false); g.drawString(font,"Material: "+v.materialDisplay()+" x"+v.requiredMaterialCount(),132,66,v.dragoonHasMaterial()?0x007700:0xAA0000,false); g.drawString(font,"Units: "+v.selectedUnits()+"/"+v.requiredUnitsToFull(),94,111,0x404040,false); g.drawString(font,v.targetReady()?"Customer ready":"Customer not ready",132,84,v.targetReady()?0x007700:0xAA0000,false); g.drawString(font,v.statusMessage(),22,154,0x404040,false); }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){ renderBackground(g,mouseX,mouseY,partial); super.render(g,mouseX,mouseY,partial); renderTooltip(g,mouseX,mouseY); }
    public static final class RepairClientState { private static View current; public static void set(View v){current=v;} public static View current(){return current;} public record View(int containerId, UUID sessionId, String dragoonName, String targetName, boolean viewerDragoon, long offeredFeeTrace, long targetBalanceTrace, long dragoonCapacityTrace, int selectedUnits, int requiredUnitsToFull, String materialItemId, String materialDisplay, int requiredMaterialCount, boolean dragoonHasMaterial, boolean targetReady, boolean dragoonRepairing, String statusMessage){} }
}
