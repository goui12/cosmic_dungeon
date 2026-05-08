package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassAngle implements RangeSelectItemModelProperty {
    public static final MapCodec<CompassAngle> MAP_CODEC = CompassAngleState.MAP_CODEC.xmap(CompassAngle::new, p_388035_ -> p_388035_.state);
    private final CompassAngleState state;

    public CompassAngle(boolean wobble, CompassAngleState.CompassTarget compassTarget) {
        this(new CompassAngleState(wobble, compassTarget));
    }

    private CompassAngle(CompassAngleState state) {
        this.state = state;
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return this.state.get(stack, level, owner, seed);
    }

    @Override
    public MapCodec<CompassAngle> type() {
        return MAP_CODEC;
    }
}
