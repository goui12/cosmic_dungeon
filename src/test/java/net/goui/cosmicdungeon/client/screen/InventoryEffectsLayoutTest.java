package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class InventoryEffectsLayoutTest {
    @Test void boundedIconsAndOverflowFitBetweenGuideAndResurrectionWithoutCoveringSlots() {
        for (int[] size : new int[][] {{320,240}, {426,240}, {640,360}, {854,480}})
            for (int height : new int[] {166,184})
                for (int count : new int[] {0,1,3,4,12,96}) {
                    int left = (size[0] - 176) / 2, top = (size[1] - height) / 2;
                    var layout = InventoryEffectsLayout.of(left,top,count);
                    assertTrue(layout.fits(size[0],size[1]));
                    assertEquals(Math.min(3,count),layout.icons().size());
                    assertEquals(Math.max(0,count-3),layout.hidden());
                    var protectedAreas = List.of(new Rect(left+98,top+6,72,12),
                            new Rect(left+98,top+49,73,20), new Rect(left+7,top+7,18,72),
                            new Rect(left+25,top+7,51,72), new Rect(left+76,top+61,18,18),
                            new Rect(left+7,top+83,162,76));
                    for (var area : protectedAreas) assertFalse(layout.row().intersects(area));
                    for (var icon : layout.icons()) {
                        assertTrue(layout.row().contains(icon.x(),icon.y()));
                        assertTrue(layout.row().contains(icon.right()-1,icon.bottom()-1));
                        assertFalse(icon.intersects(layout.overflow()));
                    }
                    if (count > 3) {
                        assertTrue(layout.row().contains(layout.overflow().x(),layout.overflow().y()));
                        assertEquals(layout.row().right(),layout.overflow().right());
                    } else assertEquals(0,layout.overflow().width());
                }
    }
    @Test void unsupportedViewportDoesNotSuppressNativeEffects() {
        assertFalse(InventoryEffectsLayout.of(-60,0,4).fits(100,120));
        assertFalse(InventoryEffectsLayout.of(0,-30,4).fits(176,120));
    }
    @Test void nativeNameRetainsPotionAmplifier() {
        var normal = new MobEffectInstance(MobEffects.SPEED,1200,0);
        var strong = new MobEffectInstance(MobEffects.SPEED,1200,1);
        assertEquals(normal.getEffect().value().getDisplayName(),InventoryEffectsClient.effectName(normal));
        assertTrue(InventoryEffectsClient.effectName(strong).getString().startsWith(
                strong.getEffect().value().getDisplayName().getString()));
        assertNotEquals(InventoryEffectsClient.effectName(normal),InventoryEffectsClient.effectName(strong));
    }
}
