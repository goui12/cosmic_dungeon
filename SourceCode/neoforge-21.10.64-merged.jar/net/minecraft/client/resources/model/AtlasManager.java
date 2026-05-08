package net.minecraft.client.resources.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AtlasManager implements PreparableReloadListener, MaterialSet, AutoCloseable {
    private static final List<AtlasManager.AtlasConfig> KNOWN_ATLASES = List.of(
        new AtlasManager.AtlasConfig(Sheets.ARMOR_TRIMS_SHEET, AtlasIds.ARMOR_TRIMS, false),
        new AtlasManager.AtlasConfig(Sheets.BANNER_SHEET, AtlasIds.BANNER_PATTERNS, false),
        new AtlasManager.AtlasConfig(Sheets.BED_SHEET, AtlasIds.BEDS, false),
        new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_BLOCKS, AtlasIds.BLOCKS, true),
        new AtlasManager.AtlasConfig(Sheets.CHEST_SHEET, AtlasIds.CHESTS, false),
        new AtlasManager.AtlasConfig(Sheets.DECORATED_POT_SHEET, AtlasIds.DECORATED_POT, false),
        new AtlasManager.AtlasConfig(Sheets.GUI_SHEET, AtlasIds.GUI, false, Set.of(GuiMetadataSection.TYPE)),
        new AtlasManager.AtlasConfig(Sheets.MAP_DECORATIONS_SHEET, AtlasIds.MAP_DECORATIONS, false),
        new AtlasManager.AtlasConfig(Sheets.PAINTINGS_SHEET, AtlasIds.PAINTINGS, false),
        new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_PARTICLES, AtlasIds.PARTICLES, false),
        new AtlasManager.AtlasConfig(Sheets.SHIELD_SHEET, AtlasIds.SHIELD_PATTERNS, false),
        new AtlasManager.AtlasConfig(Sheets.SHULKER_SHEET, AtlasIds.SHULKER_BOXES, false),
        new AtlasManager.AtlasConfig(Sheets.SIGN_SHEET, AtlasIds.SIGNS, false)
    );
    public static final PreparableReloadListener.StateKey<AtlasManager.PendingStitchResults> PENDING_STITCH = new PreparableReloadListener.StateKey<>();
    private final Map<ResourceLocation, AtlasManager.AtlasEntry> atlasByTexture = new HashMap<>();
    private final Map<ResourceLocation, AtlasManager.AtlasEntry> atlasById = new HashMap<>();
    private Map<Material, TextureAtlasSprite> materialLookup = Map.of();
    private int maxMipmapLevels;

    public AtlasManager(TextureManager textureManager, int maxMipmapLevels) {
        List<AtlasManager.AtlasConfig> KNOWN_ATLASES = net.neoforged.neoforge.client.ClientHooks.gatherTextureAtlases(AtlasManager.KNOWN_ATLASES);
        for (AtlasManager.AtlasConfig atlasmanager$atlasconfig : KNOWN_ATLASES) {
            TextureAtlas textureatlas = new TextureAtlas(atlasmanager$atlasconfig.textureId);
            textureManager.register(atlasmanager$atlasconfig.textureId, textureatlas);
            AtlasManager.AtlasEntry atlasmanager$atlasentry = new AtlasManager.AtlasEntry(textureatlas, atlasmanager$atlasconfig);
            this.atlasByTexture.put(atlasmanager$atlasconfig.textureId, atlasmanager$atlasentry);
            this.atlasById.put(atlasmanager$atlasconfig.definitionLocation, atlasmanager$atlasentry);
        }

        this.maxMipmapLevels = maxMipmapLevels;
    }

    public TextureAtlas getAtlasOrThrow(ResourceLocation id) {
        AtlasManager.AtlasEntry atlasmanager$atlasentry = this.atlasById.get(id);
        if (atlasmanager$atlasentry == null) {
            throw new IllegalArgumentException("Invalid atlas id: " + id);
        } else {
            return atlasmanager$atlasentry.atlas();
        }
    }

    public void forEach(BiConsumer<ResourceLocation, TextureAtlas> action) {
        this.atlasById.forEach((p_436508_, p_436509_) -> action.accept(p_436508_, p_436509_.atlas));
    }

    public void updateMaxMipLevel(int maxMipLevel) {
        this.maxMipmapLevels = maxMipLevel;
    }

    @Override
    public void close() {
        this.materialLookup = Map.of();
        this.atlasById.values().forEach(AtlasManager.AtlasEntry::close);
        this.atlasById.clear();
        this.atlasByTexture.clear();
    }

    @Override
    public TextureAtlasSprite get(Material material) {
        TextureAtlasSprite textureatlassprite = this.materialLookup.get(material);
        if (textureatlassprite != null) {
            return textureatlassprite;
        } else {
            ResourceLocation resourcelocation = material.atlasLocation();
            AtlasManager.AtlasEntry atlasmanager$atlasentry = this.atlasByTexture.get(resourcelocation);
            if (atlasmanager$atlasentry == null) {
                throw new IllegalArgumentException("Invalid atlas texture id: " + resourcelocation);
            } else {
                return atlasmanager$atlasentry.atlas().missingSprite();
            }
        }
    }

    @Override
    public void prepareSharedState(PreparableReloadListener.SharedState sharedState) {
        int i = this.atlasById.size();
        List<AtlasManager.PendingStitch> list = new ArrayList<>(i);
        Map<ResourceLocation, CompletableFuture<SpriteLoader.Preparations>> map = new HashMap<>(i);
        List<CompletableFuture<?>> list1 = new ArrayList<>(i);
        this.atlasById.forEach((p_435836_, p_434021_) -> {
            CompletableFuture<SpriteLoader.Preparations> completablefuture1 = new CompletableFuture<>();
            map.put(p_435836_, completablefuture1);
            list.add(new AtlasManager.PendingStitch(p_434021_, completablefuture1));
            list1.add(completablefuture1.thenCompose(SpriteLoader.Preparations::readyForUpload));
        });
        CompletableFuture<?> completablefuture = CompletableFuture.allOf(list1.toArray(CompletableFuture[]::new));
        sharedState.set(PENDING_STITCH, new AtlasManager.PendingStitchResults(list, map, completablefuture));
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState, Executor exectutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExectutor
    ) {
        AtlasManager.PendingStitchResults atlasmanager$pendingstitchresults = sharedState.get(PENDING_STITCH);
        ResourceManager resourcemanager = sharedState.resourceManager();
        atlasmanager$pendingstitchresults.pendingStitches
            .forEach(p_435220_ -> p_435220_.entry.scheduleLoad(resourcemanager, exectutor, this.maxMipmapLevels).whenComplete((p_434695_, p_433705_) -> {
                if (p_434695_ != null) {
                    p_435220_.preparations.complete(p_434695_);
                } else {
                    p_435220_.preparations.completeExceptionally(p_433705_);
                }
            }));
        return atlasmanager$pendingstitchresults.allReadyToUpload
            .thenCompose(barrier::wait)
            .thenAcceptAsync(p_435007_ -> this.materialLookup = atlasmanager$pendingstitchresults.joinAndUpload(), applyExectutor);
    }

    @OnlyIn(Dist.CLIENT)
    public record AtlasConfig(
        ResourceLocation textureId, ResourceLocation definitionLocation, boolean createMipmaps, Set<MetadataSectionType<?>> additionalMetadata
    ) {
        public AtlasConfig(ResourceLocation p_435325_, ResourceLocation p_435767_, boolean p_433479_) {
            this(p_435325_, p_435767_, p_433479_, Set.of());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record AtlasEntry(TextureAtlas atlas, AtlasManager.AtlasConfig config) implements AutoCloseable {
        @Override
        public void close() {
            this.atlas.clearTextureData();
        }

        CompletableFuture<SpriteLoader.Preparations> scheduleLoad(ResourceManager resourceManager, Executor executor, int mipLevel) {
            return SpriteLoader.create(this.atlas)
                .loadAndStitch(resourceManager, this.config.definitionLocation, this.config.createMipmaps ? mipLevel : 0, executor, this.config.additionalMetadata);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record PendingStitch(AtlasManager.AtlasEntry entry, CompletableFuture<SpriteLoader.Preparations> preparations) {
        public void joinAndUpload(Map<Material, TextureAtlasSprite> output) {
            SpriteLoader.Preparations spriteloader$preparations = this.preparations.join();
            this.entry.atlas.upload(spriteloader$preparations);
            spriteloader$preparations.regions()
                .forEach((p_434339_, p_435114_) -> output.put(new Material(this.entry.config.textureId, p_434339_), p_435114_));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class PendingStitchResults {
        final List<AtlasManager.PendingStitch> pendingStitches;
        private final Map<ResourceLocation, CompletableFuture<SpriteLoader.Preparations>> stitchFuturesById;
        final CompletableFuture<?> allReadyToUpload;

        PendingStitchResults(
            List<AtlasManager.PendingStitch> pendingStitches,
            Map<ResourceLocation, CompletableFuture<SpriteLoader.Preparations>> stitchFutureById,
            CompletableFuture<?> allReadyToUpload
        ) {
            this.pendingStitches = pendingStitches;
            this.stitchFuturesById = stitchFutureById;
            this.allReadyToUpload = allReadyToUpload;
        }

        public Map<Material, TextureAtlasSprite> joinAndUpload() {
            Map<Material, TextureAtlasSprite> map = new HashMap<>();
            this.pendingStitches.forEach(p_433127_ -> p_433127_.joinAndUpload(map));
            return map;
        }

        public CompletableFuture<SpriteLoader.Preparations> get(ResourceLocation id) {
            return Objects.requireNonNull(this.stitchFuturesById.get(id));
        }
    }
}
