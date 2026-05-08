package net.minecraft.client.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MusicInfo(@Nullable Music music, float volume) {
    public MusicInfo(Music p_383136_) {
        this(p_383136_, 1.0F);
    }

    public boolean canReplace(SoundInstance soundInstance) {
        return this.music == null ? false : this.music.replaceCurrentMusic() && !this.music.event().value().location().equals(soundInstance.getLocation());
    }
}
