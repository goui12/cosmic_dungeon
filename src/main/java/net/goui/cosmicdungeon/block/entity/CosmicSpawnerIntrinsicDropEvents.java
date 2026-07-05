package net.goui.cosmicdungeon.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CosmicSpawnerIntrinsicDropEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CosmicSpawnerIntrinsicDropEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        Map<String, CosmicSpawnerPreset.IntrinsicDropRule> rules = CosmicSpawnerEntityIntrinsicDropData.readRules(event.getEntity()).orElseGet(() -> findRulesFromSpawnerBlock(sl, event));
        if (rules.isEmpty()) return;
        removeOverriddenIntrinsicDrops(event, rules);
        rollConfiguredIntrinsicDrops(sl, event, rules);
        event.getDrops().removeIf(e -> e.getItem().isEmpty());
    }

    private static Map<String, CosmicSpawnerPreset.IntrinsicDropRule> findRulesFromSpawnerBlock(ServerLevel sl, LivingDropsEvent event) {
        for (String tag : event.getEntity().getTags()) {
            if (!tag.startsWith(CosmicSpawnerBlockEntity.COSMIC_SPAWNER_TAG_PREFIX)) continue;
            BlockPos pos = parseSpawnerPos(tag);
            if (pos == null) continue;
            if (!(sl.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be)) continue;
            CosmicSpawnerPreset preset = be.getSpawnerPreset();
            if (preset != null && !preset.getConfiguredIntrinsicDropRules().isEmpty()) return new LinkedHashMap<>(preset.getConfiguredIntrinsicDropRules());
        }
        return Map.of();
    }

    private static void removeOverriddenIntrinsicDrops(LivingDropsEvent event, Map<String, CosmicSpawnerPreset.IntrinsicDropRule> rules) {
        Set<ResourceLocation> itemIds = rules.values().stream().map(CosmicSpawnerPreset.IntrinsicDropRule::itemId).collect(Collectors.toSet());
        event.getDrops().removeIf(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!itemIds.contains(itemId)) return false;
            return !looksLikeEquipmentDrop(event, stack);
        });
    }

    private static boolean looksLikeEquipmentDrop(LivingDropsEvent event, ItemStack stack) {
        if (!(event.getEntity() instanceof Mob mob)) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = mob.getItemBySlot(slot);
            if (!equipped.isEmpty() && ItemStack.matches(equipped, stack)) return true;
        }
        return false;
    }

    private static void rollConfiguredIntrinsicDrops(ServerLevel sl, LivingDropsEvent event, Map<String, CosmicSpawnerPreset.IntrinsicDropRule> rules) {
        for (CosmicSpawnerPreset.IntrinsicDropRule rule : rules.values()) {
            float chance = Math.max(0f, Math.min(1f, rule.chance()));
            if (chance <= 0f || sl.random.nextFloat() > chance) continue;
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(rule.itemId());
            if (item.isEmpty()) { LOGGER.warn("CosmicSpawner: skipping unknown intrinsic drop item '{}' on {}", rule.itemId(), event.getEntity().getStringUUID()); continue; }
            int remaining = CosmicSpawnerPreset.clampCount(rule.count());
            int max = Math.max(1, item.get().getDefaultMaxStackSize());
            while (remaining > 0) {
                int stackCount = Math.min(max, remaining);
                remaining -= stackCount;
                ItemStack stack = new ItemStack(item.get(), stackCount);
                if (!stack.isEmpty()) event.getDrops().add(new ItemEntity(sl, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack));
            }
        }
    }

    private static BlockPos parseSpawnerPos(String tag) {
        String rest = tag.substring(CosmicSpawnerBlockEntity.COSMIC_SPAWNER_TAG_PREFIX.length());
        String[] parts = rest.split("_");
        if (parts.length != 3) return null;
        try { return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])); }
        catch (NumberFormatException ignored) { return null; }
    }
}
