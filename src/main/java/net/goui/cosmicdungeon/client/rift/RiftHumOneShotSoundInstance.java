package net.goui.cosmicdungeon.client.rift;

import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class RiftHumOneShotSoundInstance extends AbstractSoundInstance {

    public RiftHumOneShotSoundInstance(Vec3 anchor, float volume, float pitch) {
        super(ModSounds.RIFT_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());

        this.looping = false;
        this.delay = 0;

        this.x = anchor.x;
        this.y = anchor.y;
        this.z = anchor.z;

        this.volume = volume;
        this.pitch = pitch;

        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }
}
