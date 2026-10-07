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
        if(kind==ClassResourceKind.BREWING_SUPPLIES){
            result.add(HelpMenuContent.HelpBlock.heading("Theurgist Skills"));
            result.add(HelpMenuContent.HelpBlock.paragraph("Craft a potion spends 20 Brewing Supplies for one random positive tier I splash potion: night vision, invisibility, fire resistance, swiftness, healing, regeneration, strength or luck. Craft an epic potion spends 40 for a tier II swiftness, healing, regeneration or strength splash potion. No bottles, fuel or stand are required. A full inventory consumes no supplies."));
            result.add(HelpMenuContent.HelpBlock.paragraph("Your positive thrown potions benefit allies, their pets and mercenaries, but do not affect hostile mobs, including healing damage against undead. You can still manually brew negative potions at a brewing stand."));
            result.add(HelpMenuContent.HelpBlock.paragraph("When a teammate dies, open your inventory and choose Resurrect <player>. Their death screen offers Accept and Decline. Successful acceptance spends 120 of your Brewing Supplies and returns them to their latest death position with five seconds of protection. There is no player cooldown or extra skill-level requirement; you must remain alive in the same active dungeon and able to pay. Declined, stale and failed offers cost nothing. Mercenary resurrection keeps its separate level and cooldown."));
        }
        if(kind==ClassResourceKind.KIBBLE){
            result.add(HelpMenuContent.HelpBlock.heading("Wolfpack"));
            result.add(HelpMenuContent.HelpBlock.paragraph("Breed spends 5 Kibble per healthy eligible adult placed in love mode, including an affordable single wolf. Pups are yours and tame; juveniles and breeding cooldowns are respected. Wolves stand up and mate naturally."));
            result.add(HelpMenuContent.HelpBlock.paragraph("Summon spends 30 Kibble for one successfully placed tamed wolf. Regroup costs 1 per living loaded wolf moved in your current dungeon and requires enough for the whole affected pack. It never retrieves unloaded, dead, archived or other-dimension wolves. Heal costs 5 per injured living loaded wolf, fully healing the lowest health first as far as your balance allows."));
            result.add(HelpMenuContent.HelpBlock.paragraph("Buttons preview the exact affected count and total cost. Changed eligibility refreshes the preview; failed actions cost nothing. Packs remain uncapped and last only for this dungeon run. Normal saves and reconnects retain active packs; completion, forfeit and reset retire them. The six mode buttons are currently unavailable."));
        }
        result.add(HelpMenuContent.HelpBlock.heading("Convertible supplies"));
        for(var name:items)result.add(new HelpMenuContent.HelpBlock(HelpMenuContent.Kind.BULLET,Component.empty(),name.copy()));
        if(items.isEmpty())result.add(HelpMenuContent.HelpBlock.paragraph("No convertible supplies are enabled by this server."));
        return List.copyOf(result);
    }
}
