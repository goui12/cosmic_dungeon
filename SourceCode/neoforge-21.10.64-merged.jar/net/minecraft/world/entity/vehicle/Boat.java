package net.minecraft.world.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Boat extends AbstractBoat {
    public Boat(EntityType<? extends Boat> p_376173_, Level p_38293_, Supplier<Item> p_376917_) {
        super(p_376173_, p_38293_, p_376917_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_376273_) {
        return p_376273_.height() / 3.0F;
    }
}
