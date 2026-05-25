package net.goui.cosmicdungeon.block.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class CosmicSpawnerPreset {
    public static final int PRESET_VERSION = 2;
    public static final ResourceLocation ILLAGER_CAPTAIN_ID = ResourceLocation.withDefaultNamespace("illager_captain");

    public enum Slot { MAINHAND(EquipmentSlot.MAINHAND,"mainhand"),OFFHAND(EquipmentSlot.OFFHAND,"offhand"),HEAD(EquipmentSlot.HEAD,"head"),CHEST(EquipmentSlot.CHEST,"chest"),LEGS(EquipmentSlot.LEGS,"legs"),FEET(EquipmentSlot.FEET,"feet");
        public final EquipmentSlot equipmentSlot; public final String id; Slot(EquipmentSlot s,String id){this.equipmentSlot=s;this.id=id;} public static Slot fromId(String id){for(var s:values()) if(s.id.equals(id)) return s; return null;}}
    private ResourceLocation entityTypeId = ResourceLocation.withDefaultNamespace("pig");
    private Component customName; private boolean customNameVisible,persistent,silent,glowing,noAi,noGravity,illagerCaptainVariant;
    private final EnumMap<Slot, ItemStack> equipment = new EnumMap<>(Slot.class); private final EnumMap<Slot, Float> dropChances = new EnumMap<>(Slot.class);
    private final Map<ResourceLocation, Float> intrinsicDropChances = new HashMap<>();
    public CosmicSpawnerPreset(){for(var s:Slot.values()){equipment.put(s,ItemStack.EMPTY); dropChances.put(s,0.0F);}}
    public ResourceLocation getEntityTypeId(){return entityTypeId;} public void setEntityTypeId(ResourceLocation id){this.entityTypeId=id; this.illagerCaptainVariant = false;}
    public boolean isIllagerCaptainVariant(){return illagerCaptainVariant;}
    public ResourceLocation getDisplayEntityTypeId(){return illagerCaptainVariant ? ILLAGER_CAPTAIN_ID : entityTypeId;}
    public void setIllagerCaptainVariant(){this.entityTypeId=ResourceLocation.withDefaultNamespace("pillager"); this.illagerCaptainVariant=true;}
    public void setCustomName(Component n){this.customName=n;} public Component getCustomName(){return customName;} public void setCustomNameVisible(boolean v){this.customNameVisible=v;} public void setPersistent(boolean v){this.persistent=v;} public void setSilent(boolean v){this.silent=v;} public void setGlowing(boolean v){this.glowing=v;} public void setNoAi(boolean v){this.noAi=v;} public void setNoGravity(boolean v){this.noGravity=v;}
    public ItemStack getEquipment(Slot s){return equipment.get(s);} public void setEquipment(Slot s, ItemStack st){equipment.put(s,st.copy());} public float getDropChance(Slot s){return dropChances.get(s);} public void setDropChance(Slot s,float f){dropChances.put(s,Math.max(0f,Math.min(1f,f)));}
    public Map<ResourceLocation, Float> getIntrinsicDropChances(){ return java.util.Collections.unmodifiableMap(intrinsicDropChances); }
    public void setIntrinsicDropChance(ResourceLocation itemId, float chance){ intrinsicDropChances.put(itemId, Math.max(0f, Math.min(1f, chance))); }
    public void clearIntrinsicDropChance(ResourceLocation itemId){ intrinsicDropChances.remove(itemId); }

    public void save(ValueOutput out){
        out.putInt("presetVersion", PRESET_VERSION); out.putString("entityType", entityTypeId.toString());
        if(customName!=null) out.store("customName", ComponentSerialization.flatRestrictedCodec(8192), customName);
        out.putBoolean("customNameVisible", customNameVisible); out.putBoolean("persistent", persistent); out.putBoolean("silent", silent); out.putBoolean("glowing", glowing); out.putBoolean("noAi", noAi); out.putBoolean("noGravity", noGravity); out.putBoolean("illagerCaptainVariant", illagerCaptainVariant);
        for (var s: Slot.values()) {
            ItemStack st=equipment.get(s); if(!st.isEmpty()) out.store("eq_"+s.id, ItemStack.CODEC, st);
            out.putFloat("drop_"+s.id, dropChances.get(s));
        }
        var intrinsic = out.child("intrinsicDrops");
        for (var e : intrinsicDropChances.entrySet()) intrinsic.putFloat(e.getKey().toString(), e.getValue());
    }
    public static CosmicSpawnerPreset load(ValueInput in){
        CosmicSpawnerPreset p=new CosmicSpawnerPreset();
        ResourceLocation rl=ResourceLocation.tryParse(in.getStringOr("entityType","minecraft:pig")); if(rl!=null) p.entityTypeId=rl;
        p.customName=in.read("customName", ComponentSerialization.flatRestrictedCodec(8192)).orElse(null);
        p.customNameVisible=in.getBooleanOr("customNameVisible",false); p.persistent=in.getBooleanOr("persistent",false); p.silent=in.getBooleanOr("silent",false); p.glowing=in.getBooleanOr("glowing",false); p.noAi=in.getBooleanOr("noAi",false); p.noGravity=in.getBooleanOr("noGravity",false); p.illagerCaptainVariant=in.getBooleanOr("illagerCaptainVariant",false);
        for (var s: Slot.values()) {
            p.equipment.put(s, in.read("eq_"+s.id, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            p.dropChances.put(s, Math.max(0f,Math.min(1f,in.getFloatOr("drop_"+s.id,0.0f))));
        }
        in.child("intrinsicDrops").ifPresent(child -> loadIntrinsicDrops(child, p));
        return p;
    }
    private static void loadIntrinsicDrops(ValueInput child, CosmicSpawnerPreset preset) {
        try {
            for (String key : child.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    preset.intrinsicDropChances.put(id, Math.max(0f, Math.min(1f, child.getFloatOr(key, 1.0f))));
                }
            }
        } catch (NoSuchElementException ignored) {
            // Malformed or non-compound intrinsicDrops data can appear in older/edited saves.
            // Ignore it so a bad tag cannot crash client/server when syncing block entity data.
        }
    }

    public void applyToEntity(Entity entity) {
        if (customName != null) entity.setCustomName(customName);
        entity.setCustomNameVisible(customNameVisible);
        entity.setSilent(silent);
        entity.setGlowingTag(glowing);
        entity.setNoGravity(noGravity);

        if (entity instanceof Mob mob) {
            if (illagerCaptainVariant) {
                applyIllagerCaptainVariant(mob);
            }
            if (persistent) mob.setPersistenceRequired();
            mob.setNoAi(noAi);

            boolean handEquipmentChanged = false;
            for (var s : Slot.values()) {
                ItemStack desired = equipment.get(s);
                if (illagerCaptainVariant && s == Slot.HEAD && desired.isEmpty()) {
                    continue;
                }
                if (!ItemStack.matches(mob.getItemBySlot(s.equipmentSlot), desired)) {
                    mob.setItemSlot(s.equipmentSlot, desired.copy());
                    if (s == Slot.MAINHAND || s == Slot.OFFHAND) {
                        handEquipmentChanged = true;
                    }
                }
                mob.setDropChance(s.equipmentSlot, getDropChance(s));
            }

            if (handEquipmentChanged) {
                reassessWeaponGoalIfPresent(mob);
            }
        }
    }

    private static void applyIllagerCaptainVariant(Mob mob) {
        if (mob instanceof PatrollingMonster patrollingMonster) {
            patrollingMonster.setPatrolLeader(true);
        }
        if (mob instanceof Raider raider) {
            raider.setCanJoinRaid(true);
        }
        mob.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(mob.level().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        mob.setDropChance(EquipmentSlot.HEAD, 2.0F);
        mob.setPersistenceRequired();
    }

    private static void reassessWeaponGoalIfPresent(Mob mob) {
        try {
            var method = mob.getClass().getMethod("reassessWeaponGoal");
            method.invoke(mob);
        } catch (ReflectiveOperationException ignored) {
            // Most mobs do not swap AI goals when weapons change. Skeleton-family mobs do,
            // and their public reassessWeaponGoal method is called here when available.
        }
    }
}
