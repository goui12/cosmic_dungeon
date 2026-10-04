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
    public static String status(PartyPayloads.Mercenary row){
        return switch(row.status()){
            case "ACTIVE" -> String.format(Locale.ROOT,"Health %.1f / %.1f",row.health(),row.maxHealth());
            case "RESPAWNING" -> row.respawnSeconds()==0?"Awaiting safe return":
                String.format(Locale.ROOT,"Respawn %d:%02d",row.respawnSeconds()/60,row.respawnSeconds()%60);
            default -> "Outside loaded area";
        };
    }
}
