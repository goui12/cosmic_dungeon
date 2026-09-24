package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.npc.tamsin.TamsinAppearance;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TamsinAppearanceRenderTest {
    @Test
    @SuppressWarnings("unchecked")
    public void renderingCapturesFlagAndClearsItBeforeAnOrdinaryVillager() {
        var renderer = TamsinAppearanceRender.INSTANCE;
        var state = new VillagerRenderState();
        var holder = new AttachmentHolder() {};
        var type = (AttachmentType<Boolean>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(
                ResourceLocation.parse("cosmicdungeon:tamsin_appearance"));
        assertNotNull(type);
        assertFalse(renderer.isTamsin(state));
        holder.setData(type, true);
        renderer.extract(holder, state);
        assertTrue(renderer.isTamsin(state));

        holder.removeData(type);
        assertFalse(TamsinAppearance.INSTANCE.isApplied(holder));
        assertTrue(renderer.isTamsin(state), "Submitted render state does not retain a live holder");
        renderer.extract(holder, state);
        assertFalse(renderer.isTamsin(state), "Reused state cannot leak Tamsin's textures");
        assertFalse(holder.hasAttachments(), "Rendering must not recreate a default attachment");
    }
}
