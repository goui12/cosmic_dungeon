package net.goui.cosmicdungeon.playerclass.theurgist;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TheurgistBrewingEvents {
    private static final Field LEVEL_BLOCK_ENTITY_TICKERS = findField(Level.class, "blockEntityTickers");
    private static final Field BREWING_STAND_ITEMS = findField(BrewingStandBlockEntity.class, "items");
    private static final Field BREWING_STAND_BREW_TIME = findField(BrewingStandBlockEntity.class, "brewTime");
    private static final Method DO_BREW = findDoBrew();

    private TheurgistBrewingEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        for (TickingBlockEntity ticker : blockEntityTickers(level)) {
            if (ticker == null || ticker.isRemoved()) continue;
            BlockPos pos = ticker.getPos();
            if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity brewingStand)) continue;
            finishBrewing(level, pos, brewingStand);
        }
    }

    private static void finishBrewing(Level level, BlockPos pos, BrewingStandBlockEntity brewingStand) {
        if (BREWING_STAND_ITEMS == null || BREWING_STAND_BREW_TIME == null || DO_BREW == null) return;

        try {
            if (BREWING_STAND_BREW_TIME.getInt(brewingStand) <= 0) return;

            @SuppressWarnings("unchecked")
            NonNullList<ItemStack> items = (NonNullList<ItemStack>) BREWING_STAND_ITEMS.get(brewingStand);
            if (!isBrewable(level.potionBrewing(), items)) return;

            DO_BREW.invoke(null, level, pos, items);
            BREWING_STAND_BREW_TIME.setInt(brewingStand, 0);
            BlockState state = level.getBlockState(pos);
            brewingStand.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // If Mojang internals change, fail soft and leave vanilla brewing behavior intact.
        }
    }

    private static boolean isBrewable(PotionBrewing potionBrewing, NonNullList<ItemStack> items) {
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty() || !potionBrewing.isIngredient(ingredient)) return false;

        for (int i = 0; i < 3; i++) {
            ItemStack input = items.get(i);
            if (!input.isEmpty() && potionBrewing.hasMix(input, ingredient)) return true;
        }
        return false;
    }

    private static Iterable<TickingBlockEntity> blockEntityTickers(Level level) {
        if (LEVEL_BLOCK_ENTITY_TICKERS == null) return List.of();

        try {
            @SuppressWarnings("unchecked")
            List<TickingBlockEntity> tickers = (List<TickingBlockEntity>) LEVEL_BLOCK_ENTITY_TICKERS.get(level);
            return tickers;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findDoBrew() {
        try {
            Method method = BrewingStandBlockEntity.class.getDeclaredMethod("doBrew", Level.class, BlockPos.class, NonNullList.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
