package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerRenderState, ShulkerModel> {
    private static final ResourceLocation DEFAULT_TEXTURE_LOCATION = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION
        .texture()
        .withPath(p_349906_ -> "textures/" + p_349906_ + ".png");
    private static final ResourceLocation[] TEXTURE_LOCATION = Sheets.SHULKER_TEXTURE_LOCATION
        .stream()
        .map(p_349907_ -> p_349907_.texture().withPath(p_349905_ -> "textures/" + p_349905_ + ".png"))
        .toArray(ResourceLocation[]::new);

    public ShulkerRenderer(EntityRendererProvider.Context p_174370_) {
        super(p_174370_, new ShulkerModel(p_174370_.bakeLayer(ModelLayers.SHULKER)), 0.0F);
    }

    public Vec3 getRenderOffset(ShulkerRenderState renderState) {
        return renderState.renderOffset;
    }

    public boolean shouldRender(Shulker livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntity, camera, camX, camY, camZ)) {
            return true;
        } else {
            Vec3 vec3 = livingEntity.getRenderPosition(0.0F);
            if (vec3 == null) {
                return false;
            } else {
                EntityType<?> entitytype = livingEntity.getType();
                float f = entitytype.getHeight() / 2.0F;
                float f1 = entitytype.getWidth() / 2.0F;
                Vec3 vec31 = Vec3.atBottomCenterOf(livingEntity.blockPosition());
                return camera.isVisible(new AABB(vec3.x, vec3.y + f, vec3.z, vec31.x, vec31.y + f, vec31.z).inflate(f1, f, f1));
            }
        }
    }

    public ResourceLocation getTextureLocation(ShulkerRenderState renderState) {
        return getTextureLocation(renderState.color);
    }

    public ShulkerRenderState createRenderState() {
        return new ShulkerRenderState();
    }

    public void extractRenderState(Shulker entity, ShulkerRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.renderOffset = Objects.requireNonNullElse(entity.getRenderPosition(partialTick), Vec3.ZERO);
        reusedState.color = entity.getColor();
        reusedState.peekAmount = entity.getClientPeekAmount(partialTick);
        reusedState.yHeadRot = entity.yHeadRot;
        reusedState.yBodyRot = entity.yBodyRot;
        reusedState.attachFace = entity.getAttachFace();
    }

    public static ResourceLocation getTextureLocation(@Nullable DyeColor color) {
        return color == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[color.getId()];
    }

    protected void setupRotations(ShulkerRenderState renderState, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(renderState, poseStack, bodyRot + 180.0F, scale);
        poseStack.rotateAround(renderState.attachFace.getOpposite().getRotation(), 0.0F, 0.5F, 0.0F);
    }
}
