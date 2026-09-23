package net.goui.cosmicdungeon.item.custom;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

/** Source-defined presentation defaults; damage and radius remain server-configured. */
public final class D1RocketPayload {
    private D1RocketPayload() {}
    public static Fireworks defaults(boolean cindermaul) {
        var explosion = new FireworkExplosion(
                cindermaul ? FireworkExplosion.Shape.LARGE_BALL : FireworkExplosion.Shape.SMALL_BALL,
                cindermaul ? IntList.of(DyeColor.ORANGE.getFireworkColor(), DyeColor.RED.getFireworkColor())
                        : IntList.of(DyeColor.RED.getFireworkColor()),
                IntList.of(), cindermaul, cindermaul);
        return new Fireworks(1, Collections.nCopies(cindermaul ? 5 : 4, explosion));
    }

}
