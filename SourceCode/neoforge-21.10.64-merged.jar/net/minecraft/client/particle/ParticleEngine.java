package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleEngine {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(
        ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS
    );
    protected ClientLevel level;
    private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
    private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
    private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap<>();
    private final ParticleResources resourceManager;
    private final RandomSource random = RandomSource.create();
    private final Map<ParticleRenderType, java.util.function.Function<ParticleEngine, ParticleGroup<?>>> particleGroupFactories;
    private final List<ParticleRenderType> particleRenderOrder;

    public ParticleEngine(ClientLevel level, ParticleResources resourceManager) {
        this.level = level;
        this.resourceManager = resourceManager;
        var particleGroupFactories = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<ParticleRenderType, java.util.function.Function<ParticleEngine, ParticleGroup<?>>>();
        var particleRenderOrder = new java.util.ArrayList<>(RENDER_ORDER);
        net.neoforged.fml.ModLoader.postEvent(new net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent(particleGroupFactories, particleRenderOrder));
        this.particleGroupFactories = it.unimi.dsi.fastutil.objects.Reference2ObjectMaps.unmodifiable(particleGroupFactories);
        this.particleRenderOrder = List.copyOf(particleRenderOrder);
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions particleData) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particleData));
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions data, int lifetime) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, data, lifetime));
    }

    @Nullable
    public Particle createParticle(
        ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
        Particle particle = this.makeParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {
            this.add(particle);
            return particle;
        } else {
            return null;
        }
    }

    @Nullable
    private <T extends ParticleOptions> Particle makeParticle(
        T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
        ParticleProvider<T> particleprovider = (ParticleProvider<T>)this.resourceManager
            .getProviders()
            .get(BuiltInRegistries.PARTICLE_TYPE.getKey(particleData.getType()));
        return particleprovider == null
            ? null
            : particleprovider.createParticle(particleData, this.level, x, y, z, xSpeed, ySpeed, zSpeed, this.random);
    }

    public void add(Particle effect) {
        Optional<ParticleLimit> optional = effect.getParticleLimit();
        if (optional.isPresent()) {
            if (this.hasSpaceInParticleLimit(optional.get())) {
                this.particlesToAdd.add(effect);
                this.updateCount(optional.get(), 1);
            }
        } else {
            this.particlesToAdd.add(effect);
        }
    }

    public void tick() {
        this.particles.forEach((p_445197_, p_445198_) -> {
            Profiler.get().push(p_445197_.name());
            p_445198_.tickParticles();
            Profiler.get().pop();
        });
        if (!this.trackingEmitters.isEmpty()) {
            List<TrackingEmitter> list = Lists.newArrayList();

            for (TrackingEmitter trackingemitter : this.trackingEmitters) {
                trackingemitter.tick();
                if (!trackingemitter.isAlive()) {
                    list.add(trackingemitter);
                }
            }

            this.trackingEmitters.removeAll(list);
        }

        Particle particle;
        if (!this.particlesToAdd.isEmpty()) {
            while ((particle = this.particlesToAdd.poll()) != null) {
                this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle);
            }
        }
    }

    private ParticleGroup<?> createParticleGroup(ParticleRenderType renderType) {
        if (renderType == ParticleRenderType.ITEM_PICKUP) {
            return new ItemPickupParticleGroup(this);
        } else if (renderType == ParticleRenderType.ELDER_GUARDIANS) {
            return new ElderGuardianParticleGroup(this);
        } else if (this.particleGroupFactories.containsKey(renderType)) {
            return this.particleGroupFactories.get(renderType).apply(this);
        } else {
            return (ParticleGroup<?>)(renderType == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, renderType));
        }
    }

    protected void updateCount(ParticleLimit limit, int count) {
        this.trackedParticleCounts.addTo(limit, count);
    }

    public void extract(ParticlesRenderState reusedState, Frustum frustum, Camera camera, float partialTick) {
        for (ParticleRenderType particlerendertype : this.particleRenderOrder) {
            ParticleGroup<?> particlegroup = this.particles.get(particlerendertype);
            if (particlegroup != null && !particlegroup.isEmpty()) {
                reusedState.add(particlegroup.extractRenderState(frustum, camera, partialTick));
            }
        }
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
        this.clearParticles();
        this.trackingEmitters.clear();
    }

    public String countParticles() {
        return String.valueOf(this.particles.values().stream().mapToInt(ParticleGroup::size).sum());
    }

    private boolean hasSpaceInParticleLimit(ParticleLimit limit) {
        return this.trackedParticleCounts.getInt(limit) < limit.limit();
    }

    public void clearParticles() {
        this.particles.clear();
        this.particlesToAdd.clear();
        this.trackingEmitters.clear();
        this.trackedParticleCounts.clear();
    }
}
