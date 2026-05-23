package net.goui.cosmicdungeon.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class CosmicSpawnerIntrinsicDropEvents {
    private CosmicSpawnerIntrinsicDropEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;

        CosmicSpawnerPreset preset = null;
        for (String tag : event.getEntity().getTags()) {
            if (!tag.startsWith("cosmic_spawner_")) continue;
            BlockPos pos = parseSpawnerPos(tag);
            if (pos == null) continue;
            if (!(sl.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be)) continue;
            preset = be.getSpawnerPreset();
            if (preset != null) break;
        }
        if (preset == null || preset.getIntrinsicDropChances().isEmpty()) return;

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            Float chance = preset.getIntrinsicDropChances().get(itemId);
            if (chance == null) continue;
            if (sl.random.nextFloat() > chance) {
                stack.setCount(0);
            }
        }
        event.getDrops().removeIf(e -> e.getItem().isEmpty());
    }

    private static BlockPos parseSpawnerPos(String tag) {
        String rest = tag.substring("cosmic_spawner_".length());
        String[] parts = rest.split("_");
        if (parts.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
