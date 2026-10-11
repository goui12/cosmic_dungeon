package net.goui.cosmicdungeon.client.screen;
import net.goui.cosmicdungeon.network.PartyPayloads;
import java.util.Locale;
/** Shared stack reservation and text for world, inventory, and compact recipe-book presentation. */
public final class MercenaryHudLayout {
    private MercenaryHudLayout(){}
    public static int stackHeight(int count){return count<=0?0:6+Math.min(3,count)*28-2;}
    public static int healthWidth(PartyPayloads.Mercenary row,int width){
        return row.status().equals("ACTIVE")&&row.maxHealth()>0
            ?Math.round(Math.max(0,width)*row.health()/row.maxHealth()):0;
    }
    public static String label(PartyPayloads.Mercenary row){
        return row.classId().isEmpty() ? row.name() : row.name()+" / "
                +net.minecraft.network.chat.Component.translatable("playerclass.cosmicdungeon."+row.classId()).getString();
    }
    public static java.util.List<String> tooltip(PartyPayloads.Mercenary row){
        var lines=new java.util.ArrayList<String>();
        lines.add(label(row));lines.add(status(row));
        for(var skill:row.skills())lines.add(net.goui.cosmicdungeon.mercenary.MercenarySkill.description(skill.id(),skill.successes()));
        if(row.classId().equals("theurgist")||row.resurrection().seconds()>=0){
            var view=net.goui.cosmicdungeon.client.screen.skills.TheurgistClient.view();
            var matches=view.casters().stream().filter(c->c.mercenary()
                    &&(row.resurrection().mercenary().isEmpty()?c.name().equals(row.name()):
                    c.id().toString().equals(row.resurrection().mercenary()))).toList();
            var state=matches.size()==1&&(row.resurrection().run()==0||row.resurrection().run()==view.run())
                    ?matches.getFirst().resurrection():fallbackResurrection(row);
            lines.add("Resurrection: "+ResurrectionPresentation.status(state));
        }
        return java.util.List.copyOf(lines);
    }
    private static net.goui.cosmicdungeon.network.TheurgistPayloads.Resurrection fallbackResurrection(PartyPayloads.Mercenary row){
        int seconds=row.resurrection().seconds();
        String state=seconds<0?"LOCKED":!row.status().equals("ACTIVE")?"UNAVAILABLE":seconds>0?"COOLDOWN":"READY";
        return new net.goui.cosmicdungeon.network.TheurgistPayloads.Resurrection(state,seconds);
    }
    public static String status(PartyPayloads.Mercenary row){
        return switch(row.status()){
            case "ACTIVE" -> String.format(Locale.ROOT,"Health %.1f / %.1f",row.health(),row.maxHealth());
            case "RESPAWNING" -> row.respawnSeconds()==0?"Awaiting safe return":
                String.format(Locale.ROOT,"Respawn %d:%02d",row.respawnSeconds()/60,row.respawnSeconds()%60);
            default -> "Outside loaded area";
        };
    }
}
