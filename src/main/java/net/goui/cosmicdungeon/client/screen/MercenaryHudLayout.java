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
        if(row.resurrection().seconds()>=0){
            int seconds=row.resurrection().seconds();
            lines.add(seconds>0?String.format(Locale.ROOT,"Resurrection: %d:%02d",seconds/60,seconds%60)
                    :row.status().equals("ACTIVE")?"Resurrection: Ready":"Resurrection: Unavailable");
        }
        return java.util.List.copyOf(lines);
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
