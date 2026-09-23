package net.goui.cosmicdungeon.client.branding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicInfo;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/** Uses Minecraft's music channel, streaming, volume sliders and lifecycle. */
public final class CosmicMenuMusic {
    private final MusicInfo menuMusic = new MusicInfo(new Music(SoundEvents.MUSIC_MENU, 20, 20, true));

    @SubscribeEvent
    public void selectMusic(SelectMusicEvent event) {
        MusicInfo selected = event.getMusic();
        if (Minecraft.getInstance().player == null && selected != null && selected.music() != null
                && selected.music().event().equals(SoundEvents.MUSIC_MENU)) {
            event.setMusic(menuMusic);
        }
    }

    @SubscribeEvent
    public void enterWorld(ClientPlayerNetworkEvent.LoggingIn event) {
        // Ordinary overworld music need not replace a playing song. Stop the menu track explicitly.
        Minecraft.getInstance().getMusicManager().stopPlaying(Musics.MENU);
    }
}
