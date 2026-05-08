package net.minecraft.world.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChestRaft extends AbstractChestBoat {
    public ChestRaft(EntityType<? extends ChestRaft> p_376801_, Level p_376516_, Supplier<Item> p_376360_) {
        super(p_376801_, p_376516_, p_376360_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_376488_) {
        return p_376488_.height() * 0.8888889F;
    }
}
