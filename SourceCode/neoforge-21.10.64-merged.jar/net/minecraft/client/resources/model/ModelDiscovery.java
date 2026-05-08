package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelDiscovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Object2ObjectMap<ResourceLocation, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap<>();
    private final ModelDiscovery.ModelWrapper missingModel;
    private final Object2ObjectFunction<ResourceLocation, ModelDiscovery.ModelWrapper> uncachedResolver;
    private final ResolvableModel.Resolver resolver;
    private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

    public ModelDiscovery(Map<ResourceLocation, UnbakedModel> inputModels, UnbakedModel missingModel) {
        this.missingModel = new ModelDiscovery.ModelWrapper(MissingBlockModel.LOCATION, missingModel, true);
        this.modelWrappers.put(MissingBlockModel.LOCATION, this.missingModel);
        this.uncachedResolver = p_404130_ -> {
            ResourceLocation resourcelocation = (ResourceLocation)p_404130_;
            UnbakedModel unbakedmodel = inputModels.get(resourcelocation);
            if (unbakedmodel == null) {
                LOGGER.warn("Missing block model: {}", resourcelocation);
                return this.missingModel;
            } else {
                return this.createAndQueueWrapper(resourcelocation, unbakedmodel);
            }
        };
        this.resolver = this::getOrCreateModel;
    }

    private static boolean isRoot(UnbakedModel model) {
        return model.parent() == null;
    }

    private ModelDiscovery.ModelWrapper getOrCreateModel(ResourceLocation location) {
        // Neo: Remove computeIfAbsent, might cause problems with nested model discovery.
        var wrapper = this.modelWrappers.get(location);
        if (wrapper == null) {
            wrapper = this.uncachedResolver.get(location);
            this.modelWrappers.put(location, wrapper);
        }
        return wrapper;
    }

    private ModelDiscovery.ModelWrapper createAndQueueWrapper(ResourceLocation id, UnbakedModel model) {
        boolean flag = isRoot(model);
        ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = new ModelDiscovery.ModelWrapper(id, model, flag);
        if (!flag) {
            this.parentDiscoveryQueue.add(modeldiscovery$modelwrapper);
        }
        // Neo: Allow UnbakedModels to resolve additional dependencies
        model.resolveDependencies(this.resolver);

        return modeldiscovery$modelwrapper;
    }

    public void addRoot(ResolvableModel model) {
        model.resolveDependencies(this.resolver);
    }

    public void addSpecialModel(ResourceLocation id, UnbakedModel model) {
        if (!isRoot(model)) {
            LOGGER.warn("Trying to add non-root special model {}, ignoring", id);
        } else {
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = this.modelWrappers.put(id, this.createAndQueueWrapper(id, model));
            if (modeldiscovery$modelwrapper != null) {
                LOGGER.warn("Duplicate special model {}", id);
            }
        }
    }

    public ResolvedModel missingModel() {
        return this.missingModel;
    }

    public Map<ResourceLocation, ResolvedModel> resolve() {
        List<ModelDiscovery.ModelWrapper> list = new ArrayList<>();
        this.discoverDependencies(list);
        propagateValidity(list);
        Builder<ResourceLocation, ResolvedModel> builder = ImmutableMap.builder();
        this.modelWrappers.forEach((p_404132_, p_404133_) -> {
            if (p_404133_.valid) {
                builder.put(p_404132_, p_404133_);
            } else {
                LOGGER.warn("Model {} ignored due to cyclic dependency", p_404132_);
            }
        });
        return builder.build();
    }

    private void discoverDependencies(List<ModelDiscovery.ModelWrapper> wrappers) {
        ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper;
        while ((modeldiscovery$modelwrapper = this.parentDiscoveryQueue.poll()) != null) {
            ResourceLocation resourcelocation = Objects.requireNonNull(modeldiscovery$modelwrapper.wrapped.parent());
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper1 = this.getOrCreateModel(resourcelocation);
            modeldiscovery$modelwrapper.parent = modeldiscovery$modelwrapper1;
            if (modeldiscovery$modelwrapper1.valid) {
                modeldiscovery$modelwrapper.valid = true;
            } else {
                wrappers.add(modeldiscovery$modelwrapper);
            }
        }
    }

    private static void propagateValidity(List<ModelDiscovery.ModelWrapper> wrappers) {
        boolean flag = true;

        while (flag) {
            flag = false;
            Iterator<ModelDiscovery.ModelWrapper> iterator = wrappers.iterator();

            while (iterator.hasNext()) {
                ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = iterator.next();
                if (Objects.requireNonNull(modeldiscovery$modelwrapper.parent).valid) {
                    modeldiscovery$modelwrapper.valid = true;
                    iterator.remove();
                    flag = true;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ModelWrapper implements ResolvedModel {
        private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
        private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
        private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
        private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
        private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
        private static final ModelDiscovery.Slot<TextureAtlasSprite> KEY_PARTICLE_SPRITE = slot(5);
        private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
        // Neo: Cache additional properties too
        private static final ModelDiscovery.Slot<net.minecraft.util.context.ContextMap> KEY_ADDITIONAL_PROPERTIES = slot(7);
        private static final int SLOT_COUNT = 8;
        private final ResourceLocation id;
        boolean valid;
        @Nullable
        ModelDiscovery.ModelWrapper parent;
        final UnbakedModel wrapped;
        private final AtomicReferenceArray<Object> fixedSlots = new AtomicReferenceArray<>(SLOT_COUNT);
        private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

        private static <T> ModelDiscovery.Slot<T> slot(int index) {
            Objects.checkIndex(index, SLOT_COUNT);
            return new ModelDiscovery.Slot<>(index);
        }

        ModelWrapper(ResourceLocation id, UnbakedModel wrapped, boolean valid) {
            this.id = id;
            this.wrapped = wrapped;
            this.valid = valid;
        }

        @Override
        public UnbakedModel wrapped() {
            return this.wrapped;
        }

        @Nullable
        @Override
        public ResolvedModel parent() {
            return this.parent;
        }

        @Override
        public String debugName() {
            return this.id.toString();
        }

        @Nullable
        private <T> T getSlot(ModelDiscovery.Slot<T> slot) {
            return (T)this.fixedSlots.get(slot.index);
        }

        private <T> T updateSlot(ModelDiscovery.Slot<T> slot, T value) {
            T t = (T)this.fixedSlots.compareAndExchange(slot.index, null, value);
            return t == null ? value : t;
        }

        private <T> T getSimpleProperty(ModelDiscovery.Slot<T> slot, Function<ResolvedModel, T> propertyGetter) {
            T t = this.getSlot(slot);
            return t != null ? t : this.updateSlot(slot, propertyGetter.apply(this));
        }

        @Override
        public boolean getTopAmbientOcclusion() {
            return this.getSimpleProperty(KEY_AMBIENT_OCCLUSION, ResolvedModel::findTopAmbientOcclusion);
        }

        @Override
        public UnbakedModel.GuiLight getTopGuiLight() {
            return this.getSimpleProperty(KEY_GUI_LIGHT, ResolvedModel::findTopGuiLight);
        }

        @Override
        public ItemTransforms getTopTransforms() {
            return this.getSimpleProperty(KEY_TRANSFORMS, ResolvedModel::findTopTransforms);
        }

        @Override
        public UnbakedGeometry getTopGeometry() {
            return this.getSimpleProperty(KEY_GEOMETRY, ResolvedModel::findTopGeometry);
        }

        @Override
        public TextureSlots getTopTextureSlots() {
            return this.getSimpleProperty(KEY_TEXTURE_SLOTS, ResolvedModel::findTopTextureSlots);
        }

        @Override
        public TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker) {
            TextureAtlasSprite textureatlassprite = this.getSlot(KEY_PARTICLE_SPRITE);
            return textureatlassprite != null
                ? textureatlassprite
                : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleSprite(textureSlots, modelBaker, this));
        }

        private QuadCollection bakeDefaultState(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
            QuadCollection quadcollection = this.getSlot(KEY_DEFAULT_GEOMETRY);
            return quadcollection != null
                ? quadcollection
                : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(textureSlots, modelBaker, modelState, this, this.getTopAdditionalProperties()));
        }

        @Override
        public QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
            return modelState == BlockModelRotation.X0_Y0
                ? this.bakeDefaultState(textureSlots, modelBaker, modelState)
                : this.modelBakeCache.computeIfAbsent(modelState, p_405723_ -> {
                    UnbakedGeometry unbakedgeometry = this.getTopGeometry();
                    return unbakedgeometry.bake(textureSlots, modelBaker, p_405723_, this, this.getTopAdditionalProperties());
                });
        }

        @Override
        public net.minecraft.util.context.ContextMap getTopAdditionalProperties() {
            return this.getSimpleProperty(KEY_ADDITIONAL_PROPERTIES, net.neoforged.neoforge.client.extensions.ResolvedModelExtension::findTopAdditionalProperties);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Slot<T>(int index) {
    }
}
