package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Feed Pack reads the same bone/WOLF_FOOD eligibility as BogatyrFeed, not retired Kibble tags. */
@EventBusSubscriber(modid="cosmicdungeon", value=Dist.CLIENT)
public final class ClassResourceHelp {
    private static final Map<String,List<HelpMenuContent.HelpBlock>> CACHE = new HashMap<>();
    private ClassResourceHelp() {}
    @SubscribeEvent public static void tags(TagsUpdatedEvent event) { Minecraft.getInstance().execute(CACHE::clear); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CACHE.clear(); }

    static List<HelpMenuContent.HelpBlock> blocks(HelpMenuContent.Page page) {
        if (page != HelpMenuContent.BOGATYR || Minecraft.getInstance().level == null) return page.blocks();
        return CACHE.computeIfAbsent(page.id(), key -> assemble(page, BuiltInRegistries.ITEM.stream()
                .filter(item -> item == Items.BONE || item.builtInRegistryHolder().is(ItemTags.WOLF_FOOD))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .map(item -> (Component) item.getName()).toList()));
    }

    static List<HelpMenuContent.HelpBlock> assemble(HelpMenuContent.Page page, List<Component> foods) {
        if (page != HelpMenuContent.BOGATYR) return page.blocks();
        var result = new ArrayList<>(page.blocks());
        result.add(HelpMenuContent.HelpBlock.heading("Feed Pack Foods on This Server"));
        result.add(HelpMenuContent.HelpBlock.paragraph("Bones and the server's wolf-food items are consumed directly by Feed Pack. This list does not convert items into Kibble."));
        for (var name : foods)
            result.add(new HelpMenuContent.HelpBlock(HelpMenuContent.Kind.BULLET, Component.empty(), name.copy()));
        if (foods.isEmpty())
            result.add(HelpMenuContent.HelpBlock.paragraph("No food names are available yet. The server checks actual items when you use Feed Pack."));
        return List.copyOf(result);
    }
}
