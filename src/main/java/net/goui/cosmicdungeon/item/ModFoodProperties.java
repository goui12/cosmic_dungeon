package net.goui.cosmicdungeon.item;

import net.minecraft.world.food.FoodProperties;

public class ModFoodProperties {
    public static final FoodProperties RADISH = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.25f)
            .build();
}
