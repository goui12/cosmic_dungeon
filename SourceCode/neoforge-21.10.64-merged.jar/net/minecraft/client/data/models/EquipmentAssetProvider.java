package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquipmentAssetProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public EquipmentAssetProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
    }

    protected void registerModels(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
        bootstrap(output);
    }

    private static void bootstrap(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> output) {
        output.accept(
            EquipmentAssets.LEATHER,
            EquipmentClientInfo.builder()
                .addHumanoidLayers(ResourceLocation.withDefaultNamespace("leather"), true)
                .addHumanoidLayers(ResourceLocation.withDefaultNamespace("leather_overlay"), false)
                .addLayers(
                    EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(ResourceLocation.withDefaultNamespace("leather"), true)
                )
                .build()
        );
        output.accept(EquipmentAssets.CHAINMAIL, onlyHumanoid("chainmail"));
        output.accept(EquipmentAssets.COPPER, humanoidAndHorse("copper"));
        output.accept(EquipmentAssets.IRON, humanoidAndHorse("iron"));
        output.accept(EquipmentAssets.GOLD, humanoidAndHorse("gold"));
        output.accept(EquipmentAssets.DIAMOND, humanoidAndHorse("diamond"));
        output.accept(
            EquipmentAssets.TURTLE_SCUTE,
            EquipmentClientInfo.builder().addMainHumanoidLayer(ResourceLocation.withDefaultNamespace("turtle_scute"), false).build()
        );
        output.accept(EquipmentAssets.NETHERITE, onlyHumanoid("netherite"));
        output.accept(
            EquipmentAssets.ARMADILLO_SCUTE,
            EquipmentClientInfo.builder()
                .addLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY,
                    EquipmentClientInfo.Layer.onlyIfDyed(ResourceLocation.withDefaultNamespace("armadillo_scute"), false)
                )
                .addLayers(
                    EquipmentClientInfo.LayerType.WOLF_BODY,
                    EquipmentClientInfo.Layer.onlyIfDyed(ResourceLocation.withDefaultNamespace("armadillo_scute_overlay"), true)
                )
                .build()
        );
        output.accept(
            EquipmentAssets.ELYTRA,
            EquipmentClientInfo.builder()
                .addLayers(
                    EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("elytra"), Optional.empty(), true)
                )
                .build()
        );
        EquipmentClientInfo.Layer equipmentclientinfo$layer = new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("saddle"));
        output.accept(
            EquipmentAssets.SADDLE,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, equipmentclientinfo$layer)
                .addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, equipmentclientinfo$layer)
                .build()
        );

        for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry : EquipmentAssets.HARNESSES.entrySet()) {
            DyeColor dyecolor = entry.getKey();
            ResourceKey<EquipmentAsset> resourcekey = entry.getValue();
            output.accept(
                resourcekey,
                EquipmentClientInfo.builder()
                    .addLayers(
                        EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
                        EquipmentClientInfo.Layer.onlyIfDyed(ResourceLocation.withDefaultNamespace(dyecolor.getSerializedName() + "_harness"), false)
                    )
                    .build()
            );
        }

        for (Entry<DyeColor, ResourceKey<EquipmentAsset>> entry1 : EquipmentAssets.CARPETS.entrySet()) {
            DyeColor dyecolor1 = entry1.getKey();
            ResourceKey<EquipmentAsset> resourcekey1 = entry1.getValue();
            output.accept(
                resourcekey1,
                EquipmentClientInfo.builder()
                    .addLayers(
                        EquipmentClientInfo.LayerType.LLAMA_BODY,
                        new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace(dyecolor1.getSerializedName()))
                    )
                    .build()
            );
        }

        output.accept(
            EquipmentAssets.TRADER_LLAMA,
            EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(ResourceLocation.withDefaultNamespace("trader_llama")))
                .build()
        );
    }

    public static EquipmentClientInfo onlyHumanoid(String name) {
        return EquipmentClientInfo.builder().addHumanoidLayers(ResourceLocation.parse(name)).build();
    }

    public static EquipmentClientInfo humanoidAndHorse(String name) {
        return EquipmentClientInfo.builder()
            .addHumanoidLayers(ResourceLocation.parse(name))
            .addLayers(
                EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(ResourceLocation.parse(name), false)
            )
            .build();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> map = new HashMap<>();
        registerModels((p_386976_, p_388942_) -> {
            if (map.putIfAbsent(p_386976_, p_388942_) != null) {
                throw new IllegalStateException("Tried to register equipment asset twice for id: " + p_386976_);
            }
        });
        return DataProvider.saveAll(output, EquipmentClientInfo.CODEC, this.pathProvider::json, map);
    }

    @Override
    public String getName() {
        return "Equipment Asset Definitions";
    }
}
