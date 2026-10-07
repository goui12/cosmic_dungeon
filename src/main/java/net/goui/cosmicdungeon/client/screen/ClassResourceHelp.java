package net.goui.cosmicdungeon.client.screen;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The guide reads the same server-synchronized tags as recycling, cached until a tag/session change. */
@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class ClassResourceHelp {
    private static final Map<ClassResourceKind,List<HelpMenuContent.HelpBlock>> CACHE=new EnumMap<>(ClassResourceKind.class);
    private ClassResourceHelp(){}
    @SubscribeEvent public static void tags(TagsUpdatedEvent event){Minecraft.getInstance().execute(CACHE::clear);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){CACHE.clear();}
    static List<HelpMenuContent.HelpBlock> blocks(HelpMenuContent.Page page){
        ClassResourceKind kind=page==HelpMenuContent.classGuide("theurgist")?ClassResourceKind.BREWING_SUPPLIES:
                page==HelpMenuContent.classGuide("bogatyr")?ClassResourceKind.KIBBLE:null;
        if(kind==null||Minecraft.getInstance().level==null)return page.blocks();
        return CACHE.computeIfAbsent(kind,key->assemble(page,key,BuiltInRegistries.ITEM.stream()
                .filter(item->item.builtInRegistryHolder().is(key.tag()))
                .sorted(Comparator.comparing(item->BuiltInRegistries.ITEM.getKey(item).toString()))
                .map(item->(Component)item.getName()).toList()));
    }
    static List<HelpMenuContent.HelpBlock> assemble(HelpMenuContent.Page page,ClassResourceKind kind,List<Component> items){
        var result=new ArrayList<>(page.blocks());
        result.add(HelpMenuContent.HelpBlock.heading(kind.title()));
        result.add(HelpMenuContent.HelpBlock.paragraph("Start each dungeon at 0 / 600. Gain 1 per second while online in the active run, including after death. Your balance survives reconnecting; time offline adds nothing."));
        result.add(HelpMenuContent.HelpBlock.paragraph("Recycle converts each eligible item into 1 resource, up to 600. Items above the remaining capacity stay in your inventory."));
        result.add(HelpMenuContent.HelpBlock.heading("Group supplies"));
        result.add(HelpMenuContent.HelpBlock.paragraph("Request Supplies asks the other living players in your active dungeon group. Their inventory Requests column previews the items and resource yield; nothing is taken unless they accept. Accept All follows the displayed order and uses only remaining supplies and capacity."));
        result.add(HelpMenuContent.HelpBlock.paragraph("Each requester has one pending request per resource and teammate. Death, logout or leaving the run cancels pending consent. Changed inventory previews refresh before an acceptance can take items."));
        result.add(HelpMenuContent.HelpBlock.heading("Convertible supplies"));
        for(var name:items)result.add(new HelpMenuContent.HelpBlock(HelpMenuContent.Kind.BULLET,Component.empty(),name.copy()));
        if(items.isEmpty())result.add(HelpMenuContent.HelpBlock.paragraph("No convertible supplies are enabled by this server."));
        return List.copyOf(result);
    }
}
