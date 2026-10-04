package net.goui.cosmicdungeon.client.screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
/** Native focus border around teal-tinted, tiled vanilla stone; no new texture asset. */
public final class TealLeaderboardButton extends Button{
    private static final net.minecraft.resources.ResourceLocation STONE=net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/stone.png");
    public TealLeaderboardButton(int x,int y,int width,OnPress action){
        super(x,y,width,20,Component.literal("Leaderboard"),action,DEFAULT_NARRATION);
    }
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){
        int tint=active?(isHoveredOrFocused()?0xFF80E9DA:0xFF42B9AF):0xFF526D69;
        g.blitSprite(RenderPipelines.GUI_TEXTURED,SPRITES.get(active,isHoveredOrFocused()),getX(),getY(),getWidth(),getHeight(),tint);
        for(int offset=2;offset<getWidth()-2;offset+=16)
            g.blit(RenderPipelines.GUI_TEXTURED,STONE,getX()+offset,getY()+2,0,0,Math.min(16,getWidth()-2-offset),16,16,16,tint);
        renderString(g,Minecraft.getInstance().font,0xFFF1FFFC);
    }
}
