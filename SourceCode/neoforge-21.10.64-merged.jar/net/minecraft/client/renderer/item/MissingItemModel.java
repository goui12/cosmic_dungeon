package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class MissingItemModel implements ItemModel {
    private final List<BakedQuad> quads;
    private final Supplier<Vector3f[]> extents;
    private final ModelRenderProperties properties;

    public MissingItemModel(List<BakedQuad> quads, ModelRenderProperties properties) {
        this.quads = quads;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(this.quads));
    }

    @Override
    public void update(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemModelResolver itemModelResolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        renderState.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = renderState.newLayer();
        itemstackrenderstate$layerrenderstate.setRenderType(Sheets.cutoutBlockSheet());
        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, displayContext);
        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
    }
}
