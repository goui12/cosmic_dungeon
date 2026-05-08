package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

public class SpawnArmorTrimsCommand {
    private static final List<ResourceKey<TrimPattern>> VANILLA_TRIM_PATTERNS = List.of(
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
    private static final List<ResourceKey<TrimMaterial>> VANILLA_TRIM_MATERIALS = List.of(
        TrimMaterials.QUARTZ,
        TrimMaterials.IRON,
        TrimMaterials.NETHERITE,
        TrimMaterials.REDSTONE,
        TrimMaterials.COPPER,
        TrimMaterials.GOLD,
        TrimMaterials.EMERALD,
        TrimMaterials.DIAMOND,
        TrimMaterials.LAPIS,
        TrimMaterials.AMETHYST,
        TrimMaterials.RESIN
    );
    private static final ToIntFunction<ResourceKey<TrimPattern>> TRIM_PATTERN_ORDER = Util.createIndexLookup(VANILLA_TRIM_PATTERNS);
    private static final ToIntFunction<ResourceKey<TrimMaterial>> TRIM_MATERIAL_ORDER = Util.createIndexLookup(VANILLA_TRIM_MATERIALS);
    private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType(
        p_399397_ -> Component.translatableEscape("Invalid pattern", p_399397_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spawn_armor_trims")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("*_lag_my_game")
                        .executes(p_399401_ -> spawnAllArmorTrims(p_399401_.getSource(), p_399401_.getSource().getPlayerOrException()))
                )
                .then(
                    Commands.argument("pattern", ResourceKeyArgument.key(Registries.TRIM_PATTERN))
                        .executes(
                            p_399400_ -> spawnArmorTrim(
                                p_399400_.getSource(),
                                p_399400_.getSource().getPlayerOrException(),
                                ResourceKeyArgument.getRegistryKey(p_399400_, "pattern", Registries.TRIM_PATTERN, ERROR_INVALID_PATTERN)
                            )
                        )
                )
        );
    }

    private static int spawnAllArmorTrims(CommandSourceStack source, Player player) {
        return spawnArmorTrims(source, player, source.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).listElements());
    }

    private static int spawnArmorTrim(CommandSourceStack source, Player player, ResourceKey<TrimPattern> pattern) {
        return spawnArmorTrims(
            source, player, Stream.of(source.getServer().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).get(pattern).orElseThrow())
        );
    }

    private static int spawnArmorTrims(CommandSourceStack source, Player player, Stream<Holder.Reference<TrimPattern>> patterns) {
        ServerLevel serverlevel = source.getLevel();
        List<Holder.Reference<TrimPattern>> list = patterns.sorted(Comparator.comparing(p_399395_ -> TRIM_PATTERN_ORDER.applyAsInt(p_399395_.key()))).toList();
        List<Holder.Reference<TrimMaterial>> list1 = serverlevel.registryAccess()
            .lookupOrThrow(Registries.TRIM_MATERIAL)
            .listElements()
            .sorted(Comparator.comparing(p_399396_ -> TRIM_MATERIAL_ORDER.applyAsInt(p_399396_.key())))
            .toList();
        List<Holder.Reference<Item>> list2 = findEquippableItemsWithAssets(serverlevel.registryAccess().lookupOrThrow(Registries.ITEM));
        BlockPos blockpos = player.blockPosition().relative(player.getDirection(), 5);
        double d0 = 3.0;

        for (int i = 0; i < list1.size(); i++) {
            Holder.Reference<TrimMaterial> reference = list1.get(i);

            for (int j = 0; j < list.size(); j++) {
                Holder.Reference<TrimPattern> reference1 = list.get(j);
                ArmorTrim armortrim = new ArmorTrim(reference, reference1);

                for (int k = 0; k < list2.size(); k++) {
                    Holder.Reference<Item> reference2 = list2.get(k);
                    double d1 = blockpos.getX() + 0.5 - k * 3.0;
                    double d2 = blockpos.getY() + 0.5 + i * 3.0;
                    double d3 = blockpos.getZ() + 0.5 + j * 10;
                    ArmorStand armorstand = new ArmorStand(serverlevel, d1, d2, d3);
                    armorstand.setYRot(180.0F);
                    armorstand.setNoGravity(true);
                    ItemStack itemstack = new ItemStack(reference2);
                    Equippable equippable = Objects.requireNonNull(itemstack.get(DataComponents.EQUIPPABLE));
                    itemstack.set(DataComponents.TRIM, armortrim);
                    armorstand.setItemSlot(equippable.slot(), itemstack);
                    if (k == 0) {
                        armorstand.setCustomName(
                            armortrim.pattern()
                                .value()
                                .copyWithStyle(armortrim.material())
                                .copy()
                                .append(" & ")
                                .append(armortrim.material().value().description())
                        );
                        armorstand.setCustomNameVisible(true);
                    } else {
                        armorstand.setInvisible(true);
                    }

                    serverlevel.addFreshEntity(armorstand);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("Armorstands with trimmed armor spawned around you"), true);
        return 1;
    }

    private static List<Holder.Reference<Item>> findEquippableItemsWithAssets(HolderLookup<Item> itemRegistry) {
        List<Holder.Reference<Item>> list = new ArrayList<>();
        itemRegistry.listElements().forEach(p_399399_ -> {
            Equippable equippable = p_399399_.value().components().get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && equippable.assetId().isPresent()) {
                list.add((Holder.Reference<Item>)p_399399_);
            }
        });
        return list;
    }
}
