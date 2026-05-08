package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record IsKeybindDown(KeyMapping keybind) implements ConditionalItemModelProperty {
    private static final Codec<KeyMapping> KEYBIND_CODEC = Codec.STRING.comapFlatMap(p_389642_ -> {
        KeyMapping keymapping = KeyMapping.get(p_389642_);
        return keymapping != null ? DataResult.success(keymapping) : DataResult.error(() -> "Invalid keybind: " + p_389642_);
    }, KeyMapping::getName);
    public static final MapCodec<IsKeybindDown> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_389649_ -> p_389649_.group(KEYBIND_CODEC.fieldOf("keybind").forGetter(IsKeybindDown::keybind)).apply(p_389649_, IsKeybindDown::new)
    );

    @Override
    public boolean get(ItemStack p_389550_, @Nullable ClientLevel p_389440_, @Nullable LivingEntity p_389703_, int p_389439_, ItemDisplayContext p_389599_) {
        return this.keybind.isDown();
    }

    @Override
    public MapCodec<IsKeybindDown> type() {
        return MAP_CODEC;
    }
}
