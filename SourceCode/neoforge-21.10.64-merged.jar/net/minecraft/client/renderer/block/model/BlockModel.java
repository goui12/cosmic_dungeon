package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlockModel(
    @Nullable UnbakedGeometry geometry,
    @Nullable UnbakedModel.GuiLight guiLight,
    @Nullable Boolean ambientOcclusion,
    @Nullable ItemTransforms transforms,
    TextureSlots.Data textureSlots,
    @Nullable ResourceLocation parent,
    @Nullable com.mojang.math.Transformation rootTransform,
    net.neoforged.neoforge.client.RenderTypeGroup renderTypeGroup,
    java.util.Map<String, Boolean> partVisibility
) implements UnbakedModel {
    @VisibleForTesting
    public static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(UnbakedModel.class, new net.neoforged.neoforge.client.model.UnbakedModelParser.Deserializer())
        .registerTypeAdapter(BlockModel.class, new BlockModel.Deserializer())
        .registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer())
        .registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer())
        .registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer())
        .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer())
        .registerTypeAdapter(com.mojang.math.Transformation.class, new net.neoforged.neoforge.common.util.TransformationHelper.Deserializer())
        .create();

    /**
     * @deprecated Neo: use {@link
     *             net.neoforged.neoforge.client.model.UnbakedModelParser#parse(Reader
     *             )} instead
     */
    @Deprecated
    public static BlockModel fromStream(Reader reader) {
        return GsonHelper.fromJson(GSON, reader, BlockModel.class);
    }

    public BlockModel(
            @Nullable UnbakedGeometry geometry,
            @Nullable UnbakedModel.GuiLight guiLight,
            @Nullable Boolean ambientOcclusion,
            @Nullable ItemTransforms transforms,
            TextureSlots.Data textureSlots,
            @Nullable ResourceLocation parent
    ) {
        this(geometry, guiLight, ambientOcclusion, transforms, textureSlots, parent, null, net.neoforged.neoforge.client.RenderTypeGroup.EMPTY, java.util.Map.of());
    }

    @Override
    public void fillAdditionalProperties(net.minecraft.util.context.ContextMap.Builder propertiesBuilder) {
        net.neoforged.neoforge.client.model.NeoForgeModelProperties.fillRootTransformProperty(propertiesBuilder, this.rootTransform);
        net.neoforged.neoforge.client.model.NeoForgeModelProperties.fillRenderTypeProperty(propertiesBuilder, this.renderTypeGroup);
        net.neoforged.neoforge.client.model.NeoForgeModelProperties.fillPartVisibilityProperty(propertiesBuilder, this.partVisibility);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockModel> {
        public BlockModel deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            UnbakedGeometry unbakedgeometry = this.getElements(context, jsonobject);
            String s = this.getParentName(jsonobject);
            TextureSlots.Data textureslots$data = this.getTextureMap(jsonobject);
            Boolean obool = this.getAmbientOcclusion(jsonobject);
            ItemTransforms itemtransforms = null;
            if (jsonobject.has("display")) {
                JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "display");
                itemtransforms = context.deserialize(jsonobject1, ItemTransforms.class);
            }

            UnbakedModel.GuiLight unbakedmodel$guilight = null;
            if (jsonobject.has("gui_light")) {
                unbakedmodel$guilight = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(jsonobject, "gui_light"));
            }

            ResourceLocation resourcelocation = s.isEmpty() ? null : ResourceLocation.parse(s);

            var rootTransform = net.neoforged.neoforge.client.model.NeoForgeModelProperties.deserializeRootTransform(jsonobject, context);
            var renderTypeGroup = net.neoforged.neoforge.client.model.NeoForgeModelProperties.deserializeRenderType(jsonobject);
            var partVisibility = net.neoforged.neoforge.client.model.NeoForgeModelProperties.deserializePartVisibility(jsonobject);

            return new BlockModel(unbakedgeometry, unbakedmodel$guilight, obool, itemtransforms, textureslots$data, resourcelocation, rootTransform, renderTypeGroup, partVisibility);
        }

        private TextureSlots.Data getTextureMap(JsonObject json) {
            if (json.has("textures")) {
                JsonObject jsonobject = GsonHelper.getAsJsonObject(json, "textures");
                return TextureSlots.parseTextureMap(jsonobject, TextureAtlas.LOCATION_BLOCKS);
            } else {
                return TextureSlots.Data.EMPTY;
            }
        }

        private String getParentName(JsonObject json) {
            return GsonHelper.getAsString(json, "parent", "");
        }

        @Nullable
        protected Boolean getAmbientOcclusion(JsonObject json) {
            return json.has("ambientocclusion") ? GsonHelper.getAsBoolean(json, "ambientocclusion") : null;
        }

        @Nullable
        protected UnbakedGeometry getElements(JsonDeserializationContext context, JsonObject json) {
            if (!json.has("elements")) {
                return null;
            } else {
                List<BlockElement> list = new ArrayList<>();

                for (JsonElement jsonelement : GsonHelper.getAsJsonArray(json, "elements")) {
                    list.add(context.deserialize(jsonelement, BlockElement.class));
                }

                return new SimpleUnbakedGeometry(list);
            }
        }
    }
}
