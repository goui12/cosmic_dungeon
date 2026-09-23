package net.goui.cosmicdungeon.vendor;

import net.minecraft.nbt.*;
import net.minecraft.world.phys.*;
import java.util.Optional;

/** UUID/profile identity is authoritative; names and nearby unrelated entities are not. */
public final class VendorBindingRules {
    public static final String PROFILE="cosmicdungeon.vendor_profile_id";
    public static final String BEFORE="cosmicdungeon.vendor_binding_before";
    public static final String BELUZON="cosmicdungeon:d1/save_teleport_npc";
    public record Before(Tag name,boolean visible,boolean invulnerable,boolean noAi){}
    private VendorBindingRules(){}
    public static boolean canAssign(CompoundTag data,String requested,String entityType,boolean otherRole) {
        if(otherRole||requested==null||data.contains(BEFORE)&&!data.contains(PROFILE))return false;
        if(BELUZON.equals(requested)&&!entityType.equals("minecraft:creaking"))return false;
        return !data.contains(PROFILE)||data.getString(PROFILE).filter(requested::equals).isPresent();
    }
    public static void capture(CompoundTag data,Tag name,boolean visible,boolean invulnerable,boolean noAi) {
        if(data.contains(PROFILE)||data.contains(BEFORE))return;
        var prior=new CompoundTag();prior.putInt("schema",1);
        prior.putBoolean("has_name",name!=null);if(name!=null)prior.put("name",name.copy());
        prior.putBoolean("visible",visible);prior.putBoolean("invulnerable",invulnerable);prior.putBoolean("no_ai",noAi);
        data.put(BEFORE,prior);
    }
    public static Before before(CompoundTag data) {
        if(!data.contains(BEFORE))return null; // Older assignments have no trustworthy prior-state evidence.
        if(!(data.get(BEFORE) instanceof CompoundTag prior)||!(prior.get("schema") instanceof IntTag)
                ||prior.getIntOr("schema",0)!=1)throw new IllegalArgumentException("Unknown vendor binding snapshot");
        for(String key:new String[]{"has_name","visible","invulnerable","no_ai"})
            if(!(prior.get(key) instanceof ByteTag value)||(value.byteValue()!=0&&value.byteValue()!=1))
                throw new IllegalArgumentException("Malformed vendor binding snapshot");
        boolean named=prior.getBooleanOr("has_name",false);
        if(named!=prior.contains("name"))throw new IllegalArgumentException("Incomplete vendor name snapshot");
        return new Before(named?prior.get("name").copy():null,prior.getBooleanOr("visible",false),
                prior.getBooleanOr("invulnerable",false),prior.getBooleanOr("no_ai",false));
    }
    public static int tier(String system,int d1,int d2) {
        if(system==null||system.isBlank()||system.equalsIgnoreCase("D1"))return Math.max(0,d1);
        if(system.equalsIgnoreCase("D2"))return Math.max(0,d2);
        return 0;
    }
    /** Segment is already clipped at the first solid block. */
    public static Optional<Double> hit(Vec3 eye,Vec3 end,AABB box) {
        if(box.contains(eye))return Optional.of(0.0);
        return box.clip(eye,end).map(eye::distanceToSqr);
    }
    // TODO(M17/M41, licensed world review): Vendor Info 1aDUTh-_AmrB3kMHKeyTDQKdIeJHBtqdyp11FPBg3vmY
    // (2026-08-23) and Beluzon 1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA (2026-08-29).
    // Inspect UUID, dimension, exact profile and native type before repairing any old placement.
    // Legacy assignments without BEFORE keep their name/AI/protection flags when cleared.
    // Global presence must not grant personal access. Do not infer an NPC from its display name,
    // respawn unloaded villagers or automatically replace a Creaking/Heart. Cameron's 2026-09-20
    // explicit spawn/assign now replaces that profile's previous UUID, including stale chunks.
    // First Heart blocks remain authored independently. Gritch has no retained
    // role source: preserve its profile pending an explicit authored-world disposition.
}
