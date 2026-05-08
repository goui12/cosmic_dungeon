package net.minecraft.client.renderer.item;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackingItemStackRenderState extends ItemStackRenderState {
    private final List<Object> modelIdentityElements = new ArrayList<>();

    @Override
    public void appendModelIdentityElement(Object p_428832_) {
        this.modelIdentityElements.add(p_428832_);
    }

    public Object getModelIdentity() {
        return this.modelIdentityElements;
    }
}
