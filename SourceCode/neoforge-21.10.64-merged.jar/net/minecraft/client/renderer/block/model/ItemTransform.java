package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Type;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record ItemTransform(Vector3fc rotation, Vector3fc translation, Vector3fc scale, Vector3fc rightRotation) {
    public static final ItemTransform NO_TRANSFORM = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

    public ItemTransform(Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
        this(rotation, translation, scale, new Vector3f());
    }

    public void apply(boolean leftHand, PoseStack.Pose pose) {
        if (this == NO_TRANSFORM) {
            pose.translate(-0.5F, -0.5F, -0.5F);
        } else {
            float f;
            float f1;
            float f2;
            if (leftHand) {
                f = -this.translation.x();
                f1 = -this.rotation.y();
                f2 = -this.rotation.z();
            } else {
                f = this.translation.x();
                f1 = this.rotation.y();
                f2 = this.rotation.z();
            }

            pose.translate(f, this.translation.y(), this.translation.z());
            pose.rotate(
                new Quaternionf().rotationXYZ(this.rotation.x() * (float) (Math.PI / 180.0), f1 * (float) (Math.PI / 180.0), f2 * (float) (Math.PI / 180.0))
            );
            pose.scale(this.scale.x(), this.scale.y(), this.scale.z());
            pose.rotate(net.neoforged.neoforge.common.util.TransformationHelper.quatFromXYZ(rightRotation.x(), rightRotation.y() * (leftHand ? -1 : 1), rightRotation.z() * (leftHand ? -1 : 1), true));
            pose.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<ItemTransform> {
        public static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
        public static final float MAX_TRANSLATION = 5.0F;
        public static final float MAX_SCALE = 4.0F;

        public ItemTransform deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            Vector3f vector3f = this.getVector3f(jsonobject, "rotation", DEFAULT_ROTATION);
            Vector3f vector3f1 = this.getVector3f(jsonobject, "translation", DEFAULT_TRANSLATION);
            vector3f1.mul(0.0625F);
            vector3f1.set(Mth.clamp(vector3f1.x, -5.0F, 5.0F), Mth.clamp(vector3f1.y, -5.0F, 5.0F), Mth.clamp(vector3f1.z, -5.0F, 5.0F));
            Vector3f vector3f2 = this.getVector3f(jsonobject, "scale", DEFAULT_SCALE);
            vector3f2.set(Mth.clamp(vector3f2.x, -4.0F, 4.0F), Mth.clamp(vector3f2.y, -4.0F, 4.0F), Mth.clamp(vector3f2.z, -4.0F, 4.0F));
            Vector3f rightRotation = this.getVector3f(jsonobject, "right_rotation", DEFAULT_ROTATION);
            return new ItemTransform(vector3f, vector3f1, vector3f2, rightRotation);
        }

        private Vector3f getVector3f(JsonObject json, String key, Vector3f fallback) {
            if (!json.has(key)) {
                return fallback;
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(json, key);
                if (jsonarray.size() != 3) {
                    throw new JsonParseException("Expected 3 " + key + " values, found: " + jsonarray.size());
                } else {
                    float[] afloat = new float[3];

                    for (int i = 0; i < afloat.length; i++) {
                        afloat[i] = GsonHelper.convertToFloat(jsonarray.get(i), key + "[" + i + "]");
                    }

                    return new Vector3f(afloat[0], afloat[1], afloat[2]);
                }
            }
        }
    }
}
