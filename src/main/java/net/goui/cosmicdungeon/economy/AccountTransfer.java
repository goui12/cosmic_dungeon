package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;

/** Immutable paired transfer terms and terminal receipts, saved beside BOTH account balances. */
public record AccountTransfer(Terms terms,String status,long timestamp,
                              long firstBefore,long secondBefore,long firstAfter,long secondAfter,String reason) {
    public static final String RESERVED="reserved",COMMITTED="committed",CANCELLED="cancelled",REJECTED="rejected";
    public record Terms(UUID first,UUID second,long firstPays,long secondPays,String type,long run,String fingerprint){
        public static final Codec<Terms> CODEC=RecordCodecBuilder.create(i->i.group(
                UUIDUtil.STRING_CODEC.fieldOf("first").forGetter(Terms::first),
                UUIDUtil.STRING_CODEC.fieldOf("second").forGetter(Terms::second),
                Codec.LONG.fieldOf("first_pays").forGetter(Terms::firstPays),
                Codec.LONG.fieldOf("second_pays").forGetter(Terms::secondPays),
                Codec.STRING.fieldOf("type").forGetter(Terms::type),
                Codec.LONG.optionalFieldOf("run",0L).forGetter(Terms::run),
                Codec.STRING.fieldOf("fingerprint").forGetter(Terms::fingerprint)
        ).apply(i,Terms::new));
        public Terms {
            Objects.requireNonNull(first);Objects.requireNonNull(second);
            if(first.equals(second)||firstPays<0||secondPays<0||run<0||type==null||type.isBlank()
                    ||type.length()>64||fingerprint==null||fingerprint.isBlank()||fingerprint.length()>256)
                throw new IllegalArgumentException("Invalid paired account transfer terms");
        }
    }
    public static final Codec<AccountTransfer> CODEC=RecordCodecBuilder.create(i->i.group(
            Terms.CODEC.fieldOf("terms").forGetter(AccountTransfer::terms),
            Codec.STRING.fieldOf("status").forGetter(AccountTransfer::status),
            Codec.LONG.fieldOf("timestamp").forGetter(AccountTransfer::timestamp),
            Codec.LONG.fieldOf("first_before").forGetter(AccountTransfer::firstBefore),
            Codec.LONG.fieldOf("second_before").forGetter(AccountTransfer::secondBefore),
            Codec.LONG.fieldOf("first_after").forGetter(AccountTransfer::firstAfter),
            Codec.LONG.fieldOf("second_after").forGetter(AccountTransfer::secondAfter),
            Codec.STRING.optionalFieldOf("reason","").forGetter(AccountTransfer::reason)
    ).apply(i,AccountTransfer::new));
    public AccountTransfer {
        Objects.requireNonNull(terms);Objects.requireNonNull(reason);
        if(!Set.of(RESERVED,COMMITTED,CANCELLED,REJECTED).contains(status)
                ||timestamp<0||firstBefore<0||secondBefore<0||firstAfter<0||secondAfter<0)
            throw new IllegalArgumentException("Invalid account transfer receipt");
        if(!status.equals(COMMITTED)&&(firstBefore!=firstAfter||secondBefore!=secondAfter))
            throw new IllegalArgumentException("Uncommitted transfer cannot change balances");
        if(status.equals(COMMITTED)){
            if(firstBefore<terms.firstPays()||secondBefore<terms.secondPays())
                throw new IllegalArgumentException("Transfer spends unavailable funds");
            java.math.BigInteger expectedFirst=java.math.BigInteger.valueOf(firstBefore)
                    .subtract(java.math.BigInteger.valueOf(terms.firstPays())).add(java.math.BigInteger.valueOf(terms.secondPays()));
            java.math.BigInteger expectedSecond=java.math.BigInteger.valueOf(secondBefore)
                    .subtract(java.math.BigInteger.valueOf(terms.secondPays())).add(java.math.BigInteger.valueOf(terms.firstPays()));
            if(!expectedFirst.equals(java.math.BigInteger.valueOf(firstAfter))
                    ||!expectedSecond.equals(java.math.BigInteger.valueOf(secondAfter)))
                throw new IllegalArgumentException("Paired transfer does not conserve Trace");
        }
    }
    public boolean reserved(){return status.equals(RESERVED);}
}
