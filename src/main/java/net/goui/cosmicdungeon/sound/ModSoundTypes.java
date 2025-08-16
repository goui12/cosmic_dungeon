package net.goui.cosmicdungeon.sound;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

public class ModSoundTypes {

    public static final SoundType CHICKEN = new SoundType(
            1.0f, 1.0f,
            ModSounds.CHICKEN_SOUND_1.get(), // break
            ModSounds.CHICKEN_SOUND_2.get(), // step
            ModSounds.CHICKEN_SOUND_3.get(), // place
            SoundEvents.STONE_HIT,   // hit
            ModSounds.CHICKEN_SOUND_5.get() // fall
            // land
    );
}
