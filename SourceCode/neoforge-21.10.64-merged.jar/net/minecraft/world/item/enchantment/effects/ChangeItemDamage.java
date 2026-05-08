package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ChangeItemDamage(LevelBasedValue amount) implements EnchantmentEntityEffect {
    public static final MapCodec<ChangeItemDamage> CODEC = RecordCodecBuilder.mapCodec(
        p_379434_ -> p_379434_.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(p_379561_ -> p_379561_.amount)).apply(p_379434_, ChangeItemDamage::new)
    );

    @Override
    public void apply(ServerLevel p_379674_, int p_379927_, EnchantedItemInUse p_380376_, Entity p_379570_, Vec3 p_380002_) {
        ItemStack itemstack = p_380376_.itemStack();
        if (itemstack.has(DataComponents.MAX_DAMAGE) && itemstack.has(DataComponents.DAMAGE)) {
            ServerPlayer serverplayer = p_380376_.owner() instanceof ServerPlayer serverplayer1 ? serverplayer1 : null;
            int i = (int)this.amount.calculate(p_379927_);
            itemstack.hurtAndBreak(i, p_379674_, serverplayer, p_380376_.onBreak());
        }
    }

    @Override
    public MapCodec<ChangeItemDamage> codec() {
        return CODEC;
    }
}
