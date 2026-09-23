package net.goui.cosmicdungeon.client.render;

import java.util.Map;
import java.util.stream.Collectors;
import net.goui.cosmicdungeon.entity.D1ArrowEntity;
import net.goui.cosmicdungeon.playerclass.d1.D1AmmunitionCatalog;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Uses the vanilla arrow geometry and one texture selected when entity render state is extracted.
 * Cameron's edited D1 PNGs were imported September 23, 2026: 18 item icons,
 * 16 projectile skins and 10 effect icons; dimensions and native UV/alpha masks retained.
 * TODO(D1 licensed TEST): verify inventory/flight/effect rendering at multiple GUI scales,
 * embedded-arrow save/reload and the corresponding server-configured effects.
 * Source scope: retained D1 Theurgist/Judicator/Venefex/Pyroclast documents, not later tiers.
 */
public final class D1ArrowRenderer extends ArrowRenderer<D1ArrowEntity, D1ArrowRenderer.State> {
    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final Map<String, ResourceLocation> TEXTURES = D1AmmunitionCatalog.entries().stream()
            .filter(e -> e.stars() == 0).collect(Collectors.toUnmodifiableMap(
                    D1AmmunitionCatalog.Entry::id, e -> ResourceLocation.fromNamespaceAndPath(
                            "cosmicdungeon", "textures/entity/projectiles/" + e.id() + ".png")));

    public D1ArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    public static final class State extends ArrowRenderState { ResourceLocation texture = FALLBACK; }
    @Override public State createRenderState() { return new State(); }
    @Override protected ResourceLocation getTextureLocation(State state) { return state.texture; }

    @Override
    public void extractRenderState(D1ArrowEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.texture = TEXTURES.getOrDefault(entity.visualIdentity(), FALLBACK);
    }
}
