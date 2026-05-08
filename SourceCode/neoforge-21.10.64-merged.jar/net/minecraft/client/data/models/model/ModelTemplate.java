package net.minecraft.client.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelTemplate {
    public final Optional<ResourceLocation> model;
    public final Set<TextureSlot> requiredSlots;
    public final Optional<String> suffix;

    public ModelTemplate(Optional<ResourceLocation> model, Optional<String> suffix, TextureSlot... requiredSlots) {
        this.model = model;
        this.suffix = suffix;
        this.requiredSlots = ImmutableSet.copyOf(requiredSlots);
    }

    public ResourceLocation getDefaultModelLocation(Block block) {
        return ModelLocationUtils.getModelLocation(block, this.suffix.orElse(""));
    }

    public ResourceLocation create(Block block, TextureMapping textureMapping, BiConsumer<ResourceLocation, ModelInstance> output) {
        return this.create(ModelLocationUtils.getModelLocation(block, this.suffix.orElse("")), textureMapping, output);
    }

    public ResourceLocation createWithSuffix(Block block, String suffix, TextureMapping textureMapping, BiConsumer<ResourceLocation, ModelInstance> output) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix + this.suffix.orElse("")), textureMapping, output);
    }

    public ResourceLocation createWithOverride(
        Block block, String suffix, TextureMapping textureMapping, BiConsumer<ResourceLocation, ModelInstance> output
    ) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix), textureMapping, output);
    }

    public ResourceLocation create(Item item, TextureMapping textureMapping, BiConsumer<ResourceLocation, ModelInstance> output) {
        return this.create(ModelLocationUtils.getModelLocation(item, this.suffix.orElse("")), textureMapping, output);
    }

    public ResourceLocation create(ResourceLocation modelLocation, TextureMapping textureMapping, BiConsumer<ResourceLocation, ModelInstance> output) {
        Map<TextureSlot, ResourceLocation> map = this.createMap(textureMapping);
        output.accept(modelLocation, () -> {
            return createBaseTemplate(modelLocation, map);
        });
        return modelLocation;
    }

    // Neo: Reintroduced to allow subclasses to customize the serialization logic, many implementations just delegating
    public JsonObject createBaseTemplate(ResourceLocation p_388380_, Map<TextureSlot, ResourceLocation> map) {
            JsonObject jsonobject = new JsonObject();
            this.model.ifPresent(p_388657_ -> jsonobject.addProperty("parent", p_388657_.toString()));
            if (!map.isEmpty()) {
                JsonObject jsonobject1 = new JsonObject();
                map.forEach((p_387287_, p_386479_) -> jsonobject1.addProperty(p_387287_.getId(), p_386479_.toString()));
                jsonobject.add("textures", jsonobject1);
            }

            return jsonobject;
    }

    private Map<TextureSlot, ResourceLocation> createMap(TextureMapping textureMapping) {
        return Streams.concat(this.requiredSlots.stream(), textureMapping.getForced()).collect(ImmutableMap.toImmutableMap(Function.identity(), textureMapping::get));
    }

    // Neo: Allows modders to modify this template by adding new elements, custom loader, render types and other modifiers
    public net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder extend() {
        return net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder.of(this);
    }
}
