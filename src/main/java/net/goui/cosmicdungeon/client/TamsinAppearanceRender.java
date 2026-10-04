package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.npc.tamsin.TamsinAppearance;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Captures the synced flag during extraction; rendering never queries a live entity or world. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class TamsinAppearanceRender {
    public static final TamsinAppearanceRender INSTANCE = new TamsinAppearanceRender();
    public static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(
            CosmicDungeonMod.MOD_ID, "textures/entity/tamsin/tamsin_villager.png");
    public static final ResourceLocation CLOTHING = ResourceLocation.fromNamespaceAndPath(
            CosmicDungeonMod.MOD_ID, "textures/entity/tamsin/tamsin_plains.png");
    private static final ContextKey<Boolean> APPEARANCE = new ContextKey<>(
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "tamsin_appearance"));

    private TamsinAppearanceRender() {}

    @SubscribeEvent
    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(VillagerRenderer.class,
                (villager, state) -> INSTANCE.extract(villager, state));
    }

    public void extract(IAttachmentHolder entity, VillagerRenderState state) {
        // Clear on every extraction: vanilla may reuse this state for an ordinary villager.
        state.setRenderData(APPEARANCE, TamsinAppearance.INSTANCE.isApplied(entity) ? Boolean.TRUE : null);
    }

    public boolean isTamsin(EntityRenderState state) {
        return state instanceof VillagerRenderState && Boolean.TRUE.equals(state.getRenderData(APPEARANCE));
    }
}
