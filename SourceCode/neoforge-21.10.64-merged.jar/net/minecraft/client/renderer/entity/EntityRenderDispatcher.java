package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
    /**
     * lists the various player skin types with their associated Renderer class instances.
     */
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers = Map.of();
    private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers = Map.of();
    public final TextureManager textureManager;
    @Nullable
    public Camera camera;
    public Entity crosshairPickEntity;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemInHandRenderer itemInHandRenderer;
    private final AtlasManager atlasManager;
    private final Font font;
    public final Options options;
    private final Supplier<EntityModelSet> entityModels;
    private final EquipmentAssetManager equipmentAssets;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public <E extends Entity> int getPackedLightCoords(E entity, float partialTicks) {
        return this.getRenderer(entity).getPackedLightCoords(entity, partialTicks);
    }

    public EntityRenderDispatcher(
        Minecraft minecraft,
        TextureManager textureManager,
        ItemModelResolver itemModelResolver,
        ItemRenderer itemRenderer,
        MapRenderer mapRenderer,
        BlockRenderDispatcher blockRenderDispatcher,
        AtlasManager atlasManager,
        Font font,
        Options options,
        Supplier<EntityModelSet> entityModels,
        EquipmentAssetManager equipmentAssets,
        PlayerSkinRenderCache playerSkinRenderCache
    ) {
        this.textureManager = textureManager;
        this.itemModelResolver = itemModelResolver;
        this.mapRenderer = mapRenderer;
        this.atlasManager = atlasManager;
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.itemInHandRenderer = new ItemInHandRenderer(minecraft, this, itemRenderer, itemModelResolver);
        this.blockRenderDispatcher = blockRenderDispatcher;
        this.font = font;
        this.options = options;
        this.entityModels = entityModels;
        this.equipmentAssets = equipmentAssets;
    }

    public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity) {
        return (EntityRenderer<? super T, ?>)(switch (entity) {
            case AbstractClientPlayer abstractclientplayer -> this.getAvatarRenderer(
                this.playerRenderers, abstractclientplayer
            );
            case ClientMannequin clientmannequin -> this.getAvatarRenderer(this.mannequinRenderers, clientmannequin);
            default -> (EntityRenderer)this.renderers.get(entity.getType());
        });
    }

    public AvatarRenderer<AbstractClientPlayer> getPlayerRenderer(AbstractClientPlayer player) {
        return this.getAvatarRenderer(this.playerRenderers, player);
    }

    private <T extends Avatar & ClientAvatarEntity> AvatarRenderer<T> getAvatarRenderer(Map<PlayerModelType, AvatarRenderer<T>> renderers, T avatar) {
        PlayerModelType playermodeltype = avatar.getSkin().model();
        AvatarRenderer<T> avatarrenderer = renderers.get(playermodeltype);
        return avatarrenderer != null ? avatarrenderer : renderers.get(PlayerModelType.WIDE);
    }

    public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S renderState) {
        if (renderState instanceof AvatarRenderState avatarrenderstate) {
            PlayerModelType playermodeltype = avatarrenderstate.skin.model();
            EntityRenderer<? extends Avatar, ?> entityrenderer = (EntityRenderer<? extends Avatar, ?>)this.playerRenderers.get(playermodeltype);
            return (EntityRenderer<?, ? super S>)(entityrenderer != null ? entityrenderer : (EntityRenderer)this.playerRenderers.get(PlayerModelType.WIDE));
        } else {
            return (EntityRenderer<?, ? super S>)this.renderers.get(renderState.entityType);
        }
    }

    public void prepare(Camera camera, Entity crosshairPickEntitty) {
        this.camera = camera;
        this.crosshairPickEntity = crosshairPickEntitty;
    }

    public <E extends Entity> boolean shouldRender(E entity, Frustum frustum, double camX, double camY, double camZ) {
        EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(entity);
        return entityrenderer.shouldRender(entity, frustum, camX, camY, camZ);
    }

    public <E extends Entity> EntityRenderState extractEntity(E entity, float partialTick) {
        EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(entity);

        try {
            return entityrenderer.createRenderState(entity, partialTick);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Extracting render state for an entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being extracted");
            entity.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = this.fillRendererDetails(entityrenderer, crashreport);
            crashreportcategory1.setDetail("Delta", partialTick);
            throw new ReportedException(crashreport);
        }
    }

    public <S extends EntityRenderState> void submit(
        S renderState, CameraRenderState cameraRenderState, double camX, double camY, double camZ, PoseStack poseStack, SubmitNodeCollector nodeCollector
    ) {
        EntityRenderer<?, ? super S> entityrenderer = this.getRenderer(renderState);

        try {
            Vec3 vec3 = entityrenderer.getRenderOffset(renderState);
            double d2 = camX + vec3.x();
            double d0 = camY + vec3.y();
            double d1 = camZ + vec3.z();
            poseStack.pushPose();
            poseStack.translate(d2, d0, d1);
            entityrenderer.submit(renderState, poseStack, nodeCollector, cameraRenderState);
            if (renderState.displayFireAnimation) {
                nodeCollector.submitFlame(poseStack, renderState, Mth.rotationAroundAxis(Mth.Y_AXIS, cameraRenderState.orientation, new Quaternionf()));
            }

            if (renderState instanceof AvatarRenderState) {
                poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            }

            if (!renderState.shadowPieces.isEmpty()) {
                nodeCollector.submitShadow(poseStack, renderState.shadowRadius, renderState.shadowPieces);
            }

            if (!(renderState instanceof AvatarRenderState)) {
                poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            }

            if (renderState.hitboxesRenderState != null) {
                nodeCollector.submitHitbox(poseStack, renderState, renderState.hitboxesRenderState);
            }

            poseStack.popPose();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("EntityRenderState being rendered");
            renderState.fillCrashReportCategory(crashreportcategory);
            this.fillRendererDetails(entityrenderer, crashreport);
            throw new ReportedException(crashreport);
        }
    }

    private <S extends EntityRenderState> CrashReportCategory fillRendererDetails(EntityRenderer<?, S> renderer, CrashReport crashReport) {
        CrashReportCategory crashreportcategory = crashReport.addCategory("Renderer details");
        crashreportcategory.setDetail("Assigned renderer", renderer);
        return crashreportcategory;
    }

    public void resetCamera() {
        this.camera = null;
    }

    public double distanceToSqr(Entity entity) {
        return this.camera.getPosition().distanceToSqr(entity.position());
    }

    public ItemInHandRenderer getItemInHandRenderer() {
        return this.itemInHandRenderer;
    }

    public Map<PlayerModelType, EntityRenderer<? extends Avatar, ?>> getPlayerRenderers() {
        return java.util.Collections.unmodifiableMap(playerRenderers);
    }

    public Map<PlayerModelType, EntityRenderer<? extends Avatar, ?>> getMannequinRenderers() {
        return java.util.Collections.unmodifiableMap(mannequinRenderers);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
            this,
            this.itemModelResolver,
            this.mapRenderer,
            this.blockRenderDispatcher,
            resourceManager,
            this.entityModels.get(),
            this.equipmentAssets,
            this.atlasManager,
            this.font,
            this.playerSkinRenderCache
        );
        this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
        this.playerRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
        this.mannequinRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
        net.neoforged.fml.ModLoader.postEvent(new net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers(
                renderers, playerRenderers, mannequinRenderers, entityrendererprovider$context
        ));
    }
}
