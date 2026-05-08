package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable BlockElementFace.UVs uvs, Quadrant rotation, @Nullable net.neoforged.neoforge.client.model.ExtraFaceData faceData, org.apache.commons.lang3.mutable.MutableObject<BlockElement> parent) {
    public static final int NO_TINT = -1;

    public static float getU(BlockElementFace.UVs uvs, Quadrant rotation, int vertexIndex) {
        return uvs.getVertexU(rotation.rotateVertexIndex(vertexIndex)) / 16.0F;
    }

    public static float getV(BlockElementFace.UVs uvs, Quadrant rotation, int vertexIndex) {
        return uvs.getVertexV(rotation.rotateVertexIndex(vertexIndex)) / 16.0F;
    }

    public BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, @Nullable BlockElementFace.UVs uvs, Quadrant rotation) {
        this(cullForDirection, tintIndex, texture, uvs, rotation, null, new org.apache.commons.lang3.mutable.MutableObject<>());
    }

    @Override
    public net.neoforged.neoforge.client.model.ExtraFaceData faceData() {
        if (this.faceData != null) {
            return this.faceData;
        } else if (this.parent.getValue() != null) {
            return this.parent.getValue().faceData();
        } else {
            return net.neoforged.neoforge.client.model.ExtraFaceData.DEFAULT;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockElementFace> {
        private static final int DEFAULT_TINT_INDEX = -1;
        private static final int DEFAULT_ROTATION = 0;

        public BlockElementFace deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            Direction direction = getCullFacing(jsonobject);
            int i = getTintIndex(jsonobject);
            String s = getTexture(jsonobject);
            BlockElementFace.UVs blockelementface$uvs = getUVs(jsonobject);
            Quadrant quadrant = getRotation(jsonobject);
            if (jsonobject.has("forge_data")) throw new JsonParseException("forge_data should be replaced by neoforge_data"); // TODO 1.22: Remove
            var faceData = net.neoforged.neoforge.client.model.ExtraFaceData.read(jsonobject.get("neoforge_data"), null);
            return new BlockElementFace(direction, i, s, blockelementface$uvs, quadrant, faceData, new org.apache.commons.lang3.mutable.MutableObject<>());
        }

        private static int getTintIndex(JsonObject json) {
            return GsonHelper.getAsInt(json, "tintindex", -1);
        }

        private static String getTexture(JsonObject json) {
            return GsonHelper.getAsString(json, "texture");
        }

        @Nullable
        private static Direction getCullFacing(JsonObject json) {
            String s = GsonHelper.getAsString(json, "cullface", "");
            return Direction.byName(s);
        }

        private static Quadrant getRotation(JsonObject json) {
            int i = GsonHelper.getAsInt(json, "rotation", 0);
            return Quadrant.parseJson(i);
        }

        @Nullable
        private static BlockElementFace.UVs getUVs(JsonObject json) {
            if (!json.has("uv")) {
                return null;
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(json, "uv");
                if (jsonarray.size() != 4) {
                    throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
                } else {
                    float f = GsonHelper.convertToFloat(jsonarray.get(0), "minU");
                    float f1 = GsonHelper.convertToFloat(jsonarray.get(1), "minV");
                    float f2 = GsonHelper.convertToFloat(jsonarray.get(2), "maxU");
                    float f3 = GsonHelper.convertToFloat(jsonarray.get(3), "maxV");
                    return new BlockElementFace.UVs(f, f1, f2, f3);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record UVs(float minU, float minV, float maxU, float maxV) {
        public float getVertexU(int vertexIndex) {
            return vertexIndex != 0 && vertexIndex != 1 ? this.maxU : this.minU;
        }

        public float getVertexV(int vertexIndex) {
            return vertexIndex != 0 && vertexIndex != 3 ? this.maxV : this.minV;
        }
    }
}
