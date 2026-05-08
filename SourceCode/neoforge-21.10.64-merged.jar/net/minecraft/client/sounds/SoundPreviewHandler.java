package net.minecraft.client.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SoundPreviewHandler {
    @Nullable
    private static SoundInstance activePreview;
    @Nullable
    private static SoundSource previousCategory;

    public static void preview(SoundManager soundManager, SoundSource source, float volume) {
        stopOtherCategoryPreview(soundManager, source);
        if (canPlaySound(soundManager)) {
            SoundEvent soundevent = switch (source) {
                case RECORDS -> (SoundEvent)SoundEvents.NOTE_BLOCK_GUITAR.value();
                case WEATHER -> SoundEvents.LIGHTNING_BOLT_THUNDER;
                case BLOCKS -> SoundEvents.GRASS_PLACE;
                case HOSTILE -> SoundEvents.ZOMBIE_AMBIENT;
                case NEUTRAL -> SoundEvents.COW_AMBIENT;
                case PLAYERS -> (SoundEvent)SoundEvents.GENERIC_EAT.value();
                case AMBIENT -> (SoundEvent)SoundEvents.AMBIENT_CAVE.value();
                case UI -> (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
                default -> SoundEvents.EMPTY;
            };
            if (soundevent != SoundEvents.EMPTY) {
                activePreview = SimpleSoundInstance.forUI(soundevent, 1.0F, volume);
                soundManager.play(activePreview);
            }
        }
    }

    private static void stopOtherCategoryPreview(SoundManager soundManager, SoundSource soundSource) {
        if (previousCategory != soundSource) {
            previousCategory = soundSource;
            if (activePreview != null) {
                soundManager.stop(activePreview);
            }
        }
    }

    private static boolean canPlaySound(SoundManager soundManager) {
        return activePreview == null || !soundManager.isActive(activePreview);
    }
}
