package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;

public class Score implements ReadOnlyScoreInfo {
    public static final MapCodec<Score> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_400990_ -> p_400990_.group(
                Codec.INT.optionalFieldOf("Score", 0).forGetter(Score::value),
                Codec.BOOL.optionalFieldOf("Locked", false).forGetter(Score::isLocked),
                ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(p_400988_ -> Optional.ofNullable(p_400988_.display)),
                NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(p_400989_ -> Optional.ofNullable(p_400989_.numberFormat))
            )
            .apply(p_400990_, Score::new)
    );
    private int value;
    private boolean locked = true;
    @Nullable
    private Component display;
    @Nullable
    private NumberFormat numberFormat;

    public Score() {
    }

    private Score(int value, boolean locked, Optional<Component> display, Optional<NumberFormat> numberFormat) {
        this.value = value;
        this.locked = locked;
        this.display = display.orElse(null);
        this.numberFormat = numberFormat.orElse(null);
    }

    @Override
    public int value() {
        return this.value;
    }

    public void value(int value) {
        this.value = value;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Nullable
    public Component display() {
        return this.display;
    }

    public void display(@Nullable Component display) {
        this.display = display;
    }

    @Nullable
    @Override
    public NumberFormat numberFormat() {
        return this.numberFormat;
    }

    public void numberFormat(@Nullable NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }
}
