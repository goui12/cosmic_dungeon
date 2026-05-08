package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;

public record TextInput(int width, Component label, boolean labelVisible, String initial, int maxLength, Optional<TextInput.MultilineOptions> multiline)
    implements InputControl {
    public static final MapCodec<TextInput> MAP_CODEC = RecordCodecBuilder.<TextInput>mapCodec(
            p_426971_ -> p_426971_.group(
                    Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(TextInput::width),
                    ComponentSerialization.CODEC.fieldOf("label").forGetter(TextInput::label),
                    Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(TextInput::labelVisible),
                    Codec.STRING.optionalFieldOf("initial", "").forGetter(TextInput::initial),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_length", 32).forGetter(TextInput::maxLength),
                    TextInput.MultilineOptions.CODEC.optionalFieldOf("multiline").forGetter(TextInput::multiline)
                )
                .apply(p_426971_, TextInput::new)
        )
        .validate(
            p_426970_ -> p_426970_.initial.length() > p_426970_.maxLength()
                ? DataResult.error(() -> "Default text length exceeds allowed size")
                : DataResult.success(p_426970_)
        );

    @Override
    public MapCodec<TextInput> mapCodec() {
        return MAP_CODEC;
    }

    public record MultilineOptions(Optional<Integer> maxLines, Optional<Integer> height) {
        public static final int MAX_HEIGHT = 512;
        public static final Codec<TextInput.MultilineOptions> CODEC = RecordCodecBuilder.create(
            p_427389_ -> p_427389_.group(
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_lines").forGetter(TextInput.MultilineOptions::maxLines),
                    ExtraCodecs.intRange(1, 512).optionalFieldOf("height").forGetter(TextInput.MultilineOptions::height)
                )
                .apply(p_427389_, TextInput.MultilineOptions::new)
        );
    }
}
