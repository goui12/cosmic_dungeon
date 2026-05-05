package net.goui.cosmicdungeon.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumMap;

public final class CosmicSpawnerPreset {
    public static final int PRESET_VERSION = 1;
    public enum Slot { MAINHAND(EquipmentSlot.MAINHAND,"mainhand"),OFFHAND(EquipmentSlot.OFFHAND,"offhand"),HEAD(EquipmentSlot.HEAD,"head"),CHEST(EquipmentSlot.CHEST,"chest"),LEGS(EquipmentSlot.LEGS,"legs"),FEET(EquipmentSlot.FEET,"feet");
        public final EquipmentSlot equipmentSlot; public final String id; Slot(EquipmentSlot s,String id){this.equipmentSlot=s;this.id=id;} public static Slot fromId(String id){for(var s:values()) if(s.id.equals(id)) return s; return null;}}
    private ResourceLocation entityTypeId = ResourceLocation.withDefaultNamespace("pig");
    private Component customName; private boolean customNameVisible,persistent,silent,glowing,noAi,noGravity;
    private final EnumMap<Slot, ItemStack> equipment = new EnumMap<>(Slot.class); private final EnumMap<Slot, Float> dropChances = new EnumMap<>(Slot.class);
    public CosmicSpawnerPreset(){for(var s:Slot.values()){equipment.put(s,ItemStack.EMPTY); dropChances.put(s,0.085F);}}
    public ResourceLocation getEntityTypeId(){return entityTypeId;} public void setEntityTypeId(ResourceLocation id){this.entityTypeId=id;}
    public void setCustomName(Component n){this.customName=n;} public Component getCustomName(){return customName;} public void setCustomNameVisible(boolean v){this.customNameVisible=v;} public void setPersistent(boolean v){this.persistent=v;} public void setSilent(boolean v){this.silent=v;} public void setGlowing(boolean v){this.glowing=v;} public void setNoAi(boolean v){this.noAi=v;} public void setNoGravity(boolean v){this.noGravity=v;}
    public ItemStack getEquipment(Slot s){return equipment.get(s);} public void setEquipment(Slot s, ItemStack st){equipment.put(s,st.copy());} public float getDropChance(Slot s){return dropChances.get(s);} public void setDropChance(Slot s,float f){dropChances.put(s,Math.max(0f,Math.min(1f,f)));}

    public void save(ValueOutput out){
        out.putInt("presetVersion", PRESET_VERSION); out.putString("entityType", entityTypeId.toString());
        if(customName!=null) out.store("customName", ComponentSerialization.flatRestrictedCodec(8192), customName);
        out.putBoolean("customNameVisible", customNameVisible); out.putBoolean("persistent", persistent); out.putBoolean("silent", silent); out.putBoolean("glowing", glowing); out.putBoolean("noAi", noAi); out.putBoolean("noGravity", noGravity);
        for (var s: Slot.values()) {
            ItemStack st=equipment.get(s); if(!st.isEmpty()) out.store("eq_"+s.id, ItemStack.CODEC, st);
            out.putFloat("drop_"+s.id, dropChances.get(s));
        }
    }
    public static CosmicSpawnerPreset load(ValueInput in){
        CosmicSpawnerPreset p=new CosmicSpawnerPreset();
        ResourceLocation rl=ResourceLocation.tryParse(in.getStringOr("entityType","minecraft:pig")); if(rl!=null) p.entityTypeId=rl;
        p.customName=in.read("customName", ComponentSerialization.flatRestrictedCodec(8192)).orElse(null);
        p.customNameVisible=in.getBooleanOr("customNameVisible",false); p.persistent=in.getBooleanOr("persistent",false); p.silent=in.getBooleanOr("silent",false); p.glowing=in.getBooleanOr("glowing",false); p.noAi=in.getBooleanOr("noAi",false); p.noGravity=in.getBooleanOr("noGravity",false);
        for (var s: Slot.values()) {
            p.equipment.put(s, in.read("eq_"+s.id, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            p.dropChances.put(s, Math.max(0f,Math.min(1f,in.getFloatOr("drop_"+s.id,0.085f))));
        }
        return p;
    }
    public void applyToEntity(Entity entity){ if(customName!=null) entity.setCustomName(customName); entity.setCustomNameVisible(customNameVisible); entity.setSilent(silent); entity.setGlowingTag(glowing); entity.setNoGravity(noGravity);
        if(entity instanceof Mob mob){ if(persistent) mob.setPersistenceRequired(); mob.setNoAi(noAi); for(var s:Slot.values()){mob.setItemSlot(s.equipmentSlot, equipment.get(s).copy()); mob.setDropChance(s.equipmentSlot,getDropChance(s));}}
    }
}
