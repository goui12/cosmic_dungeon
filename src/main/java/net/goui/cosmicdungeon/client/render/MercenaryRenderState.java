package net.goui.cosmicdungeon.client.render;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;

/** Capture a texture per entity; rendering a later hire must not change a submitted state. */
public final class MercenaryRenderState extends HumanoidRenderState {
    // Built-in wide variants match the existing humanoid model and armor geometry.
    private static final List<ResourceLocation> SKINS = List.of(
            "alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri")
            .stream().map(name -> ResourceLocation.withDefaultNamespace(
                    "textures/entity/player/wide/" + name + ".png")).toList();
    private ResourceLocation texture = SKINS.get(6);

    public void captureIdentity(UUID id) {
        texture = SKINS.get(Math.floorMod(id.hashCode(), SKINS.size()));
    }

    public ResourceLocation texture() { return texture; }
}
