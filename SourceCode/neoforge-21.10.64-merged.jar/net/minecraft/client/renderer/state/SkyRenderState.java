package net.minecraft.client.renderer.state;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkyRenderState {
    public DimensionSpecialEffects.SkyType skyType = DimensionSpecialEffects.SkyType.NONE;
    public boolean isSunriseOrSunset;
    public boolean shouldRenderDarkDisc;
    public float sunAngle;
    public float timeOfDay;
    public float rainBrightness;
    public float starBrightness;
    public int sunriseAndSunsetColor;
    public int moonPhase;
    public int skyColor;
    public float endFlashIntensity;
    public float endFlashXAngle;
    public float endFlashYAngle;

    public void reset() {
        this.skyType = DimensionSpecialEffects.SkyType.NONE;
    }
}
