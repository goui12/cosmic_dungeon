package net.minecraft.client.gui.components.debug;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DebugScreenDisplayer {
    void addPriorityLine(String line);

    void addLine(String line);

    void addToGroup(ResourceLocation group, Collection<String> lines);

    void addToGroup(ResourceLocation group, String line);
}
