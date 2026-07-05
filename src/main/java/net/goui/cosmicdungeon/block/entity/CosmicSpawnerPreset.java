package net.goui.cosmicdungeon.block.entity;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

public final class CosmicSpawnerPreset {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int PRESET_VERSION = 3;
    public static final int LEGACY_1_5_0_PRESET_VERSION = 2;
    public static final String LEGACY_INTRINSIC_DROPS_KEY = "intrinsicDrops";
    public static final String INTRINSIC_DROP_RULES_KEY = "intrinsicDropRules";
    public static final ResourceLocation ILLAGER_CAPTAIN_ID = ResourceLocation.withDefaultNamespace("illager_captain");

    public enum Slot { MAINHAND(EquipmentSlot.MAINHAND,"mainhand"),OFFHAND(EquipmentSlot.OFFHAND,"offhand"),HEAD(EquipmentSlot.HEAD,"head"),CHEST(EquipmentSlot.CHEST,"chest"),LEGS(EquipmentSlot.LEGS,"legs"),FEET(EquipmentSlot.FEET,"feet");
        public final EquipmentSlot equipmentSlot; public final String id; Slot(EquipmentSlot s,String id){this.equipmentSlot=s;this.id=id;} public static Slot fromId(String id){for(var s:values()) if(s.id.equals(id)) return s; return null;}}
    private ResourceLocation entityTypeId = ResourceLocation.withDefaultNamespace("pig");
    private Component customName; private boolean illagerCaptainVariant;
    private final EnumMap<Slot, ItemStack> equipment = new EnumMap<>(Slot.class); private final EnumMap<Slot, Float> dropChances = new EnumMap<>(Slot.class);
    private final Map<ResourceLocation, IntrinsicDropRule> intrinsicDropRules = new HashMap<>();

    /**
     * 1.5.1-ready configured intrinsic drop rule. Phase 1 intentionally keeps
     * legacy entries in UNKNOWN_CONFIGURED form until Phase 2 can classify them
     * against the active server loot table as either DEFAULT_OVERRIDE or
     * CUSTOM_ADDITION. In all cases chance is a final per-spawner chance.
     */
    public record IntrinsicDropRule(ResourceLocation itemId, float chance, Kind kind) {
        public IntrinsicDropRule {
            chance = clampChance(chance);
            kind = kind == null ? Kind.UNKNOWN_CONFIGURED : kind;
        }

        public enum Kind {
            UNKNOWN_CONFIGURED,
            DEFAULT_OVERRIDE,
            CUSTOM_ADDITION
        }
    }
    public CosmicSpawnerPreset(){for(var s:Slot.values()){equipment.put(s,ItemStack.EMPTY); dropChances.put(s,0.0F);}}
    public ResourceLocation getEntityTypeId(){return entityTypeId;} public void setEntityTypeId(ResourceLocation id){this.entityTypeId=id; this.illagerCaptainVariant = false;}
    public boolean isIllagerCaptainVariant(){return illagerCaptainVariant;}
    public ResourceLocation getDisplayEntityTypeId(){return illagerCaptainVariant ? ILLAGER_CAPTAIN_ID : entityTypeId;}
    public void setIllagerCaptainVariant(){this.entityTypeId=ResourceLocation.withDefaultNamespace("pillager"); this.illagerCaptainVariant=true;}
    public void setCustomName(Component n){this.customName=n;} public Component getCustomName(){return customName;}
    public ItemStack getEquipment(Slot s){return equipment.get(s);} public void setEquipment(Slot s, ItemStack st){equipment.put(s,st.copy());} public float getDropChance(Slot s){return dropChances.get(s);} public void setDropChance(Slot s,float f){dropChances.put(s,Math.max(0f,Math.min(1f,f)));}
    public Map<ResourceLocation, Float> getIntrinsicDropChances(){
        Map<ResourceLocation, Float> chances = new HashMap<>();
        intrinsicDropRules.forEach((id, rule) -> chances.put(id, rule.chance()));
        return java.util.Collections.unmodifiableMap(chances);
    }
    public Map<ResourceLocation, IntrinsicDropRule> getConfiguredIntrinsicDropRules(){ return java.util.Collections.unmodifiableMap(intrinsicDropRules); }
    public void setIntrinsicDropChance(ResourceLocation itemId, float chance){ setConfiguredIntrinsicDropRule(itemId, chance, IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED); }
    public void setConfiguredIntrinsicDropRule(ResourceLocation itemId, float chance, IntrinsicDropRule.Kind kind){ if (itemId != null) intrinsicDropRules.put(itemId, new IntrinsicDropRule(itemId, chance, kind)); }
    public void resetIntrinsicDropRuleToDefault(ResourceLocation itemId){ clearIntrinsicDropChance(itemId); }
    public void clearIntrinsicDropChance(ResourceLocation itemId){ intrinsicDropRules.remove(itemId); }
    private static float clampChance(float chance) { return Math.max(0f, Math.min(1f, chance)); }

    public void save(ValueOutput out){
        out.putInt("presetVersion", PRESET_VERSION); out.putString("entityType", entityTypeId.toString());
        if(customName!=null) out.store("customName", ComponentSerialization.flatRestrictedCodec(8192), customName);
        out.putBoolean("illagerCaptainVariant", illagerCaptainVariant);
        for (var s: Slot.values()) {
            ItemStack st=equipment.get(s); if(!st.isEmpty()) out.store("eq_"+s.id, ItemStack.CODEC, st);
            out.putFloat("drop_"+s.id, dropChances.get(s));
        }
        var rules = out.child(INTRINSIC_DROP_RULES_KEY);
        var legacyIntrinsic = out.child(LEGACY_INTRINSIC_DROPS_KEY);
        for (var e : intrinsicDropRules.entrySet()) {
            IntrinsicDropRule rule = e.getValue();
            var ruleOut = rules.child(e.getKey().toString());
            ruleOut.putFloat("chance", rule.chance());
            ruleOut.putString("kind", rule.kind().name());
            // Keep a legacy mirror during the 1.5.1 transition so externally edited
            // tooling that still reads intrinsicDrops sees the same final chances.
            legacyIntrinsic.putFloat(e.getKey().toString(), rule.chance());
        }
    }
    public static CosmicSpawnerPreset load(ValueInput in){
        CosmicSpawnerPreset p=new CosmicSpawnerPreset();
        int presetVersion = in.getIntOr("presetVersion", LEGACY_1_5_0_PRESET_VERSION);
        if (presetVersion < PRESET_VERSION) {
            LOGGER.info("Upgrading Cosmic Spawner preset data from version {} to {}", presetVersion, PRESET_VERSION);
        }
        ResourceLocation rl=ResourceLocation.tryParse(in.getStringOr("entityType","minecraft:pig")); if(rl!=null) p.entityTypeId=rl;
        p.customName=in.read("customName", ComponentSerialization.flatRestrictedCodec(8192)).orElse(null);
        p.illagerCaptainVariant=in.getBooleanOr("illagerCaptainVariant",false);
        for (var s: Slot.values()) {
            p.equipment.put(s, in.read("eq_"+s.id, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            p.dropChances.put(s, Math.max(0f,Math.min(1f,in.getFloatOr("drop_"+s.id,0.0f))));
        }
        in.child(INTRINSIC_DROP_RULES_KEY).ifPresent(child -> loadIntrinsicDropRules(child, p));
        in.child(LEGACY_INTRINSIC_DROPS_KEY).ifPresent(child -> loadLegacyIntrinsicDrops(child, p));
        return p;
    }
    private static void loadIntrinsicDropRules(ValueInput child, CosmicSpawnerPreset preset) {
        try {
            for (String key : child.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) continue;
                child.child(key).ifPresent(ruleIn -> {
                    float chance = ruleIn.getFloatOr("chance", 1.0f);
                    IntrinsicDropRule.Kind kind = parseRuleKind(ruleIn.getStringOr("kind", IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED.name()));
                    preset.setConfiguredIntrinsicDropRule(id, chance, kind);
                });
            }
        } catch (NoSuchElementException ignored) {
            // Malformed or non-compound intrinsicDropRules data can appear in edited saves.
            // Ignore it so a bad tag cannot crash client/server when syncing block entity data.
        }
    }

    private static IntrinsicDropRule.Kind parseRuleKind(String name) {
        try {
            return IntrinsicDropRule.Kind.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED;
        }
    }

    private static void loadLegacyIntrinsicDrops(ValueInput child, CosmicSpawnerPreset preset) {
        try {
            for (String key : child.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    preset.intrinsicDropRules.putIfAbsent(id, new IntrinsicDropRule(id, child.getFloatOr(key, 1.0f), IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED));
                }
            }
        } catch (NoSuchElementException ignored) {
            // Malformed or non-compound intrinsicDrops data can appear in older/edited saves.
            // Ignore it so a bad tag cannot crash client/server when syncing block entity data.
        }
    }

    public void applyToEntity(Entity entity) {
        CosmicSpawnerEntityIntrinsicDropData.write(entity, intrinsicDropRules);
        if (customName != null) entity.setCustomName(customName);

        if (entity instanceof Mob mob) {
            if (illagerCaptainVariant) {
                applyIllagerCaptainVariant(mob);
            }

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
