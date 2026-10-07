package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.warden.Warden;

/** Native projectile, beam and spell capabilities, including mobs without the ranged-goal interface. */
final class BogatyrRanged {
    private BogatyrRanged(){}
    static boolean ranged(Mob mob){
        return mob instanceof RangedAttackMob||mob instanceof Blaze||mob instanceof Ghast
                ||mob instanceof Guardian||mob instanceof Breeze||mob instanceof Shulker
                ||mob instanceof Evoker||mob instanceof Warden||mob instanceof EnderDragon;
    }
}
