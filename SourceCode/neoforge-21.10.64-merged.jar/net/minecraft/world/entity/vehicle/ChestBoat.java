package net.minecraft.world.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestBoat extends AbstractChestBoat {
    public ChestBoat(EntityType<? extends ChestBoat> p_376154_, Level p_219872_, Supplier<Item> p_376136_) {
        super(p_376154_, p_219872_, p_376136_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_376737_) {
        return p_376737_.height() / 3.0F;
    }
}
