package net.goui.cosmicdungeon.vendor;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
/** Per-player daily counters share the delivered-item receipt. Old SavedData counts seed first use. */
public final class VendorStock {
    public static final String KEY="vendor_stock_v1";
    private VendorStock(){}
    private static String key(VendorProfile p,VendorOffer o){return p.id()+"|"+o.id();}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY);}
    public static int count(ServerPlayer p,VendorProfile profile,VendorOffer offer){
        var old=root(p).getCompoundOrEmpty(key(profile,offer));var legacy=VendorPurchaseLimitData.get(p.level().getServer());
        long day=legacy.stockDay();
        return count(old,day,legacy.getPurchaseCount(p.getUUID(),profile.id(),offer.id()));
    }
    public static int count(CompoundTag old,long day,int legacy){
        if(!old.isEmpty()&&(!(old.get("day") instanceof net.minecraft.nbt.NumericTag)||!(old.get("count") instanceof net.minecraft.nbt.NumericTag)||old.getIntOr("count",-1)<0))throw new IllegalArgumentException("Invalid saved vendor stock");
        return !old.isEmpty()&&old.getLongOr("day",Long.MIN_VALUE)>=day?old.getIntOr("count",0):Math.max(0,legacy);
    }
    public static CompoundTag next(CompoundTag old,long day,int legacy){
        var after=new CompoundTag();after.putLong("day",Math.max(old.getLongOr("day",Long.MIN_VALUE),day));after.putInt("count",Math.addExact(count(old,day,legacy),1));return after;
    }
    public static CompoundTag project(CompoundTag stock,CompoundTag details){
        if(!details.contains("stock_key"))return stock.copy();String key=details.getStringOr("stock_key","");
        if(key.isBlank()||!stock.getCompoundOrEmpty(key).equals(details.getCompoundOrEmpty("stock_before")))throw new IllegalStateException("Vendor stock differs from prepared owner image");
        var after=details.getCompoundOrEmpty("stock_after");if(after.isEmpty())throw new IllegalArgumentException("Missing stock result");count(after,Long.MIN_VALUE,0);
        var result=stock.copy();result.put(key,after.copy());return result;
    }
    public static boolean exhausted(ServerPlayer p,VendorProfile profile,VendorOffer offer){
        return offer.maxPurchasesPerPlayer()!=null&&offer.maxPurchasesPerPlayer()>0&&count(p,profile,offer)>=offer.maxPurchasesPerPlayer();
    }
    public static void describe(ServerPlayer p,VendorProfile profile,VendorOffer offer,CompoundTag details){
        String key=key(profile,offer);var before=root(p).getCompoundOrEmpty(key).copy();var legacy=VendorPurchaseLimitData.get(p.level().getServer());
        var after=next(before,legacy.stockDay(),legacy.getPurchaseCount(p.getUUID(),profile.id(),offer.id()));details.putString("stock_key",key);details.put("stock_before",before);details.put("stock_after",after);
    }
    public static void apply(ServerPlayer p,CompoundTag details){
        var stock=project(root(p),details);var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();root.put(KEY,stock);p.getPersistentData().put(ClassData.ROOT_TAG,root);
    }
}
