package net.minecraft.world.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class Raft extends AbstractBoat {
    public Raft(EntityType<? extends Raft> p_376832_, Level p_376623_, Supplier<Item> p_376712_) {
        super(p_376832_, p_376623_, p_376712_);
    }

    @Override
    protected double rideHeight(EntityDimensions p_376161_) {
        return p_376161_.height() * 0.8888889F;
    }
}
