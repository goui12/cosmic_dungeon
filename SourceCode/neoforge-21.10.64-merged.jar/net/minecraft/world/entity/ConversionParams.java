package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.world.scores.PlayerTeam;

public record ConversionParams(ConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable PlayerTeam team) {
    public static ConversionParams single(Mob mob, boolean keepEquipment, boolean preserveCanPickUpLoot) {
        return new ConversionParams(ConversionType.SINGLE, keepEquipment, preserveCanPickUpLoot, mob.getTeam());
    }

    @FunctionalInterface
    public interface AfterConversion<T extends Mob> {
        void finalizeConversion(T mob);
    }
}
