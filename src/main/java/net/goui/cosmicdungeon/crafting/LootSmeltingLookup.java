package net.goui.cosmicdungeon.crafting;

import java.util.Objects;
import java.util.function.Supplier;

/** Exempts only the exact native loot recipe query, never assembly or unrelated production. */
public final class LootSmeltingLookup {
    private static final ThreadLocal<Object> INPUT=new ThreadLocal<>();
    private LootSmeltingLookup(){}
    public static boolean permits(Object input){return input!=null&&INPUT.get()==input;}
    public static <T> T query(Object input,Supplier<T> lookup){
        Objects.requireNonNull(input,"loot recipe input");
        Object previous=INPUT.get();
        INPUT.set(input);
        try{return lookup.get();}
        finally{if(previous==null)INPUT.remove();else INPUT.set(previous);}
    }
}
