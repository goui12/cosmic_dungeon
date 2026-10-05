package net.goui.cosmicdungeon.economy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** One immutable account operation per run, companion and death deadline. */
public final class MercenaryRevivePayment {
    public static final String KIND="mercenary_revive";
    private MercenaryRevivePayment(){}
    public static String related(UUID mercenary,UUID death,long deadline){return mercenary+":"+death+":"+deadline;}
    public static UUID id(long run,UUID mercenary,UUID death,long deadline){
        if(run<=0||mercenary==null||death==null||deadline<0)throw new IllegalArgumentException("Invalid revival identity");
        return UUID.nameUUIDFromBytes((KIND+":"+run+":"+related(mercenary,death,deadline)).getBytes(StandardCharsets.UTF_8));
    }
    public static boolean paid(PlayerCurrencyData accounts,long run,UUID mercenary,UUID death,long deadline){
        var op=accounts.operation(id(run,mercenary,death,deadline)).orElse(null);
        return op!=null&&op.kind().equals(KIND)&&op.run()==run&&op.related().equals(related(mercenary,death,deadline))
                &&op.status().equals(AccountTransfer.COMMITTED)&&op.acknowledged();
    }
}
