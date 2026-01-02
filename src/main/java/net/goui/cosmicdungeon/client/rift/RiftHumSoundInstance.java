package net.goui.cosmicdungeon.client.rift;

import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RiftHumSoundInstance extends AbstractTickableSoundInstance {

    private Vec3 anchor;
    private final double maxDistance;

    private boolean active = true;

    private static final float FADE_SPEED = 0.05f;
    private static final float MIN_VOL_TO_STOP = 0.0025f;

    public RiftHumSoundInstance(Vec3 anchor, double maxDistance) {
        super(ModSounds.RIFT_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.anchor = anchor;
        this.maxDistance = maxDistance;

        this.looping = true;
        this.delay = 0;

        // IMPORTANT: we start silent, so we must allow silent start
        this.volume = 0.0f;
        this.pitch = 1.0f;

        this.x = anchor.x;
        this.y = anchor.y;
        this.z = anchor.z;

        // (Optional, but usually correct for positional ambience)
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void setAnchor(Vec3 anchor) {
        this.anchor = anchor;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.stop();
            return;
        }

        this.x = anchor.x;
        this.y = anchor.y;
        this.z = anchor.z;

        double d = mc.player.position().distanceTo(anchor);

        float target;
        if (!active) {
            target = 0.0f;
        } else {
            float t = (float) Mth.clamp(1.0 - (d / maxDistance), 0.0, 1.0);
            target = t * t;
        }

        this.volume = Mth.lerp(FADE_SPEED, this.volume, target);

        if (target <= 0.0f && this.volume <= MIN_VOL_TO_STOP) {
            this.stop();
        }
    }
}
