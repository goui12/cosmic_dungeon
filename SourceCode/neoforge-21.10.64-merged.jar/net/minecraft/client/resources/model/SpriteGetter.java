package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
    TextureAtlasSprite get(Material material, ModelDebugName debugName);

    TextureAtlasSprite reportMissingReference(String name, ModelDebugName debugName);

    default TextureAtlasSprite resolveSlot(TextureSlots textureSlots, String name, ModelDebugName modelDebugName) {
        Material material = textureSlots.getMaterial(name);
        return material != null ? this.get(material, modelDebugName) : this.reportMissingReference(name, modelDebugName);
    }
}
