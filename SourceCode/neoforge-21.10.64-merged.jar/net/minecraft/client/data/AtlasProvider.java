package net.minecraft.client.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.data.AtlasIds;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AtlasProvider implements DataProvider {
    private static final ResourceLocation TRIM_PALETTE_KEY = ResourceLocation.withDefaultNamespace("trims/color_palettes/trim_palette");
    private static final Map<String, ResourceLocation> TRIM_PALETTE_VALUES = extractAllMaterialAssets()
        .collect(
            Collectors.toMap(
                MaterialAssetGroup.AssetInfo::suffix, p_400184_ -> ResourceLocation.withDefaultNamespace("trims/color_palettes/" + p_400184_.suffix())
            )
        );
    private static final List<ResourceKey<TrimPattern>> VANILLA_PATTERNS = List.of(
        TrimPatterns.SENTRY,
        TrimPatterns.DUNE,
        TrimPatterns.COAST,
        TrimPatterns.WILD,
        TrimPatterns.WARD,
        TrimPatterns.EYE,
        TrimPatterns.VEX,
        TrimPatterns.TIDE,
        TrimPatterns.SNOUT,
        TrimPatterns.RIB,
        TrimPatterns.SPIRE,
        TrimPatterns.WAYFINDER,
        TrimPatterns.SHAPER,
        TrimPatterns.SILENCE,
        TrimPatterns.RAISER,
        TrimPatterns.HOST,
        TrimPatterns.FLOW,
        TrimPatterns.BOLT
    );
    private static final List<EquipmentClientInfo.LayerType> HUMANOID_LAYERS = List.of(
        EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
    );
    private final PackOutput.PathProvider pathProvider;

    public AtlasProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "atlases");
    }

    private static List<ResourceLocation> patternTextures() {
        List<ResourceLocation> list = new ArrayList<>(VANILLA_PATTERNS.size() * HUMANOID_LAYERS.size());

        for (ResourceKey<TrimPattern> resourcekey : VANILLA_PATTERNS) {
            ResourceLocation resourcelocation = TrimPatterns.defaultAssetId(resourcekey);

            for (EquipmentClientInfo.LayerType equipmentclientinfo$layertype : HUMANOID_LAYERS) {
                list.add(resourcelocation.withPath(p_399824_ -> equipmentclientinfo$layertype.trimAssetPrefix() + "/" + p_399824_));
            }
        }

        return list;
    }

    private static SpriteSource forMaterial(Material material) {
        return new SingleFile(material.texture());
    }

    private static SpriteSource forMapper(MaterialMapper mapper) {
        return new DirectoryLister(mapper.prefix(), mapper.prefix() + "/");
    }

    private static List<SpriteSource> simpleMapper(MaterialMapper mapper) {
        return List.of(forMapper(mapper));
    }

    private static List<SpriteSource> noPrefixMapper(String path) {
        return List.of(new DirectoryLister(path, ""));
    }

    private static Stream<MaterialAssetGroup.AssetInfo> extractAllMaterialAssets() {
        return ItemModelGenerators.TRIM_MATERIAL_MODELS
            .stream()
            .map(ItemModelGenerators.TrimMaterialData::assets)
            .flatMap(p_400261_ -> Stream.concat(Stream.of(p_400261_.base()), p_400261_.overrides().values().stream()))
            .sorted(Comparator.comparing(MaterialAssetGroup.AssetInfo::suffix));
    }

    private static List<SpriteSource> armorTrims() {
        return List.of(new PalettedPermutations(patternTextures(), TRIM_PALETTE_KEY, TRIM_PALETTE_VALUES));
    }

    private static List<SpriteSource> blocksList() {
        return List.of(
            forMapper(Sheets.BLOCKS_MAPPER),
            forMapper(Sheets.ITEMS_MAPPER),
            forMapper(ConduitRenderer.MAPPER),
            forMaterial(BellRenderer.BELL_RESOURCE_LOCATION),
            forMaterial(Sheets.DECORATED_POT_SIDE),
            forMaterial(EnchantTableRenderer.BOOK_LOCATION),
            new PalettedPermutations(
                List.of(
                    ItemModelGenerators.TRIM_PREFIX_HELMET,
                    ItemModelGenerators.TRIM_PREFIX_CHESTPLATE,
                    ItemModelGenerators.TRIM_PREFIX_LEGGINGS,
                    ItemModelGenerators.TRIM_PREFIX_BOOTS
                ),
                TRIM_PALETTE_KEY,
                TRIM_PALETTE_VALUES
            )
        );
    }

    private static List<SpriteSource> bannerPatterns() {
        return List.of(forMaterial(ModelBakery.BANNER_BASE), forMapper(Sheets.BANNER_MAPPER));
    }

    private static List<SpriteSource> shieldPatterns() {
        return List.of(forMaterial(ModelBakery.SHIELD_BASE), forMaterial(ModelBakery.NO_PATTERN_SHIELD), forMapper(Sheets.SHIELD_MAPPER));
    }

    private static List<SpriteSource> guiSprites() {
        return List.of(new DirectoryLister("gui/sprites", ""), new DirectoryLister("mob_effect", "mob_effect/"));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.allOf(
            this.storeAtlas(output, AtlasIds.ARMOR_TRIMS, armorTrims()),
            this.storeAtlas(output, AtlasIds.BANNER_PATTERNS, bannerPatterns()),
            this.storeAtlas(output, AtlasIds.BEDS, simpleMapper(Sheets.BED_MAPPER)),
            this.storeAtlas(output, AtlasIds.BLOCKS, blocksList()),
            this.storeAtlas(output, AtlasIds.CHESTS, simpleMapper(Sheets.CHEST_MAPPER)),
            this.storeAtlas(output, AtlasIds.DECORATED_POT, simpleMapper(Sheets.DECORATED_POT_MAPPER)),
            this.storeAtlas(output, AtlasIds.GUI, guiSprites()),
            this.storeAtlas(output, AtlasIds.MAP_DECORATIONS, noPrefixMapper("map/decorations")),
            this.storeAtlas(output, AtlasIds.PAINTINGS, noPrefixMapper("painting")),
            this.storeAtlas(output, AtlasIds.PARTICLES, noPrefixMapper("particle")),
            this.storeAtlas(output, AtlasIds.SHIELD_PATTERNS, shieldPatterns()),
            this.storeAtlas(output, AtlasIds.SHULKER_BOXES, simpleMapper(Sheets.SHULKER_MAPPER)),
            this.storeAtlas(output, AtlasIds.SIGNS, simpleMapper(Sheets.SIGN_MAPPER))
        );
    }

    private CompletableFuture<?> storeAtlas(CachedOutput output, ResourceLocation atlasId, List<SpriteSource> sources) {
        return DataProvider.saveStable(output, SpriteSources.FILE_CODEC, sources, this.pathProvider.json(atlasId));
    }

    @Override
    public String getName() {
        return "Atlas Definitions";
    }
}
