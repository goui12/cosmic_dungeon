package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryLookingAtEntity implements DebugScreenEntry {
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("looking_at_entity");

    @Override
    public void display(DebugScreenDisplayer p_434750_, @Nullable Level p_433362_, @Nullable LevelChunk p_435164_, @Nullable LevelChunk p_432933_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.crosshairPickEntity;
        List<String> list = new ArrayList<>();
        if (entity != null) {
            list.add(ChatFormatting.UNDERLINE + "Targeted Entity");
            list.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
        }

        p_434750_.addToGroup(GROUP, list);
    }
}
