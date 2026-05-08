package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerProfessionLayer<S extends LivingEntityRenderState & VillagerDataHolderRenderState, M extends EntityModel<S> & VillagerLikeModel>
    extends RenderLayer<S, M> {
    private static final Int2ObjectMap<ResourceLocation> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap<>(), p_349909_ -> {
        p_349909_.put(1, ResourceLocation.withDefaultNamespace("stone"));
        p_349909_.put(2, ResourceLocation.withDefaultNamespace("iron"));
        p_349909_.put(3, ResourceLocation.withDefaultNamespace("gold"));
        p_349909_.put(4, ResourceLocation.withDefaultNamespace("emerald"));
        p_349909_.put(5, ResourceLocation.withDefaultNamespace("diamond"));
    });
    private final Object2ObjectMap<ResourceKey<VillagerType>, VillagerMetadataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<ResourceKey<VillagerProfession>, VillagerMetadataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap<>();
    private final ResourceManager resourceManager;
    private final String path;
    private final M noHatModel;
    private final M noHatBabyModel;

    public VillagerProfessionLayer(RenderLayerParent<S, M> renderer, ResourceManager resourceManager, String path, M noHatModel, M noHatBabyModel) {
        super(renderer);
        this.resourceManager = resourceManager;
        this.path = path;
        this.noHatModel = noHatModel;
        this.noHatBabyModel = noHatBabyModel;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        if (!renderState.isInvisible) {
            VillagerData villagerdata = renderState.getVillagerData();
            if (villagerdata != null) {
                Holder<VillagerType> holder = villagerdata.type();
                Holder<VillagerProfession> holder1 = villagerdata.profession();
                VillagerMetadataSection.Hat villagermetadatasection$hat = this.getHatData(this.typeHatCache, "type", holder);
                VillagerMetadataSection.Hat villagermetadatasection$hat1 = this.getHatData(this.professionHatCache, "profession", holder1);
                M m = this.getParentModel();
                ResourceLocation resourcelocation = this.getResourceLocation("type", holder);
                boolean flag = villagermetadatasection$hat1 == VillagerMetadataSection.Hat.NONE
                    || villagermetadatasection$hat1 == VillagerMetadataSection.Hat.PARTIAL && villagermetadatasection$hat != VillagerMetadataSection.Hat.FULL;
                M m1 = renderState.isBaby ? this.noHatBabyModel : this.noHatModel;
                renderColoredCutoutModel(flag ? m : m1, resourcelocation, poseStack, nodeCollector, packedLight, renderState, -1, 1);
                if (!holder1.is(VillagerProfession.NONE) && !renderState.isBaby) {
                    ResourceLocation resourcelocation1 = this.getResourceLocation("profession", holder1);
                    renderColoredCutoutModel(m, resourcelocation1, poseStack, nodeCollector, packedLight, renderState, -1, 2);
                    if (!holder1.is(VillagerProfession.NITWIT)) {
                        ResourceLocation resourcelocation2 = this.getResourceLocation(
                            "profession_level", LEVEL_LOCATIONS.get(Mth.clamp(villagerdata.level(), 1, LEVEL_LOCATIONS.size()))
                        );
                        renderColoredCutoutModel(m, resourcelocation2, poseStack, nodeCollector, packedLight, renderState, -1, 3);
                    }
                }
            }
        }
    }

    private ResourceLocation getResourceLocation(String folder, ResourceLocation location) {
        return location.withPath(p_247944_ -> "textures/entity/" + this.path + "/" + folder + "/" + p_247944_ + ".png");
    }

    private ResourceLocation getResourceLocation(String folder, Holder<?> holder) {
        return holder.unwrapKey()
            .map(p_396316_ -> this.getResourceLocation(folder, p_396316_.location()))
            .orElse(MissingTextureAtlasSprite.getLocation());
    }

    public <K> VillagerMetadataSection.Hat getHatData(
        Object2ObjectMap<ResourceKey<K>, VillagerMetadataSection.Hat> cache, String folder, Holder<K> key
    ) {
        ResourceKey<K> resourcekey = key.unwrapKey().orElse(null);
        return resourcekey == null
            ? VillagerMetadataSection.Hat.NONE
            : cache.computeIfAbsent(
                resourcekey, p_396314_ -> this.resourceManager.getResource(this.getResourceLocation(folder, resourcekey.location())).flatMap(p_389338_ -> {
                    try {
                        return p_389338_.metadata().getSection(VillagerMetadataSection.TYPE).map(VillagerMetadataSection::hat);
                    } catch (IOException ioexception) {
                        return Optional.empty();
                    }
                }).orElse(VillagerMetadataSection.Hat.NONE)
            );
    }
}
