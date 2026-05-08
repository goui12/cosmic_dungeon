package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The MusicManager class manages the playing of music in Minecraft.
 */
@OnlyIn(Dist.CLIENT)
public class MusicManager {
    /**
     * The delay before starting to play the next song.
     */
    private static final int STARTING_DELAY = 100;
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft;
    @Nullable
    private SoundInstance currentMusic;
    private MusicManager.MusicFrequency gameMusicFrequency;
    private float currentGain = 1.0F;
    /**
     * The delay until the next song starts.
     */
    private int nextSongDelay = 100;
    private boolean toastShown = false;

    public MusicManager(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.gameMusicFrequency = minecraft.options.musicFrequency().get();
    }

    public void tick() {
        MusicInfo musicinfo = net.neoforged.neoforge.client.ClientHooks.selectMusic(this.minecraft.getSituationalMusic(), this.currentMusic);
        if (musicinfo == null) {
            if (this.currentMusic != null) {
                this.stopPlaying();
            }
            this.nextSongDelay = 0;
            return;
        }

        float f = musicinfo.volume();
        if (this.currentMusic != null && this.currentGain != f) {
            boolean flag = this.fadePlaying(f);
            if (!flag) {
                return;
            }
        }

        Music music = musicinfo.music();
        if (music == null) {
            this.nextSongDelay = Math.max(this.nextSongDelay, 100);
        } else {
            if (this.currentMusic != null) {
                if (musicinfo.canReplace(this.currentMusic)) {
                    this.minecraft.getSoundManager().stop(this.currentMusic);
                    this.nextSongDelay = Mth.nextInt(this.random, 0, music.minDelay() / 2);
                }

                if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
                    this.currentMusic = null;
                    this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
                }
            }

            this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
            if (this.currentMusic == null && this.nextSongDelay-- <= 0) {
                this.startPlaying(musicinfo);
            }
        }
    }

    public void startPlaying(MusicInfo music) {
        SoundEvent soundevent = music.music().event().value();
        this.currentMusic = SimpleSoundInstance.forMusic(soundevent, music.volume());
        switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
            case STARTED:
                this.minecraft.getToastManager().showNowPlayingToast();
                this.toastShown = true;
                break;
            case STARTED_SILENTLY:
                this.toastShown = false;
        }

        this.nextSongDelay = Integer.MAX_VALUE;
        this.currentGain = music.volume();
    }

    public void showNowPlayingToastIfNeeded() {
        if (!this.toastShown) {
            this.minecraft.getToastManager().showNowPlayingToast();
            this.toastShown = true;
        }
    }

    /**
     * Stops playing the specified {@linkplain Music} selector.
     *
     * @param music the {@linkplain Music} selector to stop playing
     */
    public void stopPlaying(Music music) {
        if (this.isPlayingMusic(music)) {
            this.stopPlaying();
        }
    }

    public void stopPlaying() {
        if (this.currentMusic != null) {
            this.minecraft.getSoundManager().stop(this.currentMusic);
            this.currentMusic = null;
            this.minecraft.getToastManager().hideNowPlayingToast();
        }

        this.nextSongDelay += 100;
    }

    private boolean fadePlaying(float volume) {
        if (this.currentMusic == null) {
            return false;
        } else if (this.currentGain == volume) {
            return true;
        } else {
            if (this.currentGain < volume) {
                this.currentGain = this.currentGain + Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
                if (this.currentGain > volume) {
                    this.currentGain = volume;
                }
            } else {
                this.currentGain = 0.03F * volume + 0.97F * this.currentGain;
                if (Math.abs(this.currentGain - volume) < 1.0E-4F || this.currentGain < volume) {
                    this.currentGain = volume;
                }
            }

            this.currentGain = Mth.clamp(this.currentGain, 0.0F, 1.0F);
            if (this.currentGain <= 1.0E-4F) {
                this.stopPlaying();
                return false;
            } else {
                this.minecraft.getSoundManager().setVolume(this.currentMusic, this.currentGain);
                return true;
            }
        }
    }

    /**
     * {@return {@code true} if the {@linkplain Music} selector is currently playing, {@code false} otherwise}
     *
     * @param selector the {@linkplain Music} selector to check for
     */
    public boolean isPlayingMusic(Music selector) {
        return this.currentMusic == null ? false : selector.event().value().location().equals(this.currentMusic.getLocation());
    }

    @Nullable
    public String getCurrentMusicTranslationKey() {
        if (this.currentMusic != null) {
            Sound sound = this.currentMusic.getSound();
            if (sound != null) {
                return sound.getLocation().toShortLanguageKey();
            }
        }

        return null;
    }

    public void setMinutesBetweenSongs(MusicManager.MusicFrequency musicFrequency) {
        this.gameMusicFrequency = musicFrequency;
        this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic().music(), this.random);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum MusicFrequency implements OptionEnum, StringRepresentable {
        DEFAULT(20),
        FREQUENT(10),
        CONSTANT(0);

        public static final Codec<MusicManager.MusicFrequency> CODEC = StringRepresentable.fromEnum(MusicManager.MusicFrequency::values);
        private static final String KEY_PREPEND = "options.music_frequency.";
        private final int id;
        private final int maxFrequency;
        private final String key;

        private MusicFrequency(int maxFrequency) {
            this.id = maxFrequency;
            this.maxFrequency = maxFrequency * 1200;
            this.key = "options.music_frequency." + this.name().toLowerCase();
        }

        int getNextSongDelay(@Nullable Music music, RandomSource random) {
            if (music == null) {
                return this.maxFrequency;
            } else if (this == CONSTANT) {
                return 100;
            } else {
                int i = Math.min(music.minDelay(), this.maxFrequency);
                int j = Math.min(music.maxDelay(), this.maxFrequency);
                return Mth.nextInt(random, i, j);
            }
        }

        @Override
        public int getId() {
            return this.id;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public String getSerializedName() {
            return this.name();
        }
    }
}
