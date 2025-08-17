package net.goui.cosmicdungeon.classsystem;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.Optional;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassPerkHooks {
    private static final String KEY_CLASS = "cosmicdungeon_class";
    private static final String KEY_DEADEYE_ORIGIN = "cosmicdungeon_deadeye_origin";

    private ClassPerkHooks() {}

    /* =========================
       Helpers
    ========================== */

    private static PlayerClass getClassOf(LivingEntity e) {
        if (e instanceof ServerPlayer p) {
            // In your 1.21.8 mappings, NBT getters return Optional<T>
            String raw = p.getPersistentData().getString(KEY_CLASS).orElse("");
            return PlayerClass.from(raw);
        }
        return null;
    }

    private static boolean isMelee(ServerPlayer p) {
        var item = p.getMainHandItem();
        return item.isEmpty() || (!item.is(Items.BOW) && !item.is(Items.CROSSBOW));
    }

    /* =========================
       Judicator
       - Small holy thorns on shield block
       - Reduced knockback
    ========================== */

    @SubscribeEvent
    public static void judicatorThornsOnBlock(LivingShieldBlockEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.JUDICATOR) return;

        var src = e.getDamageSource(); // available on LivingShieldBlockEvent
        var atk = src.getEntity();
        if (atk instanceof LivingEntity le) {
            le.hurt(p.damageSources().magic(), 2.0f);
        }
    }

    @SubscribeEvent
    public static void judicatorLessKnockback(LivingKnockBackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.JUDICATOR) return;
        e.setStrength(e.getStrength() * 0.7f);
    }

    /* =========================
       Sanctified Theurgist
       - +15% heals received
       - 20% less damage from UNDEAD attackers
    ========================== */

    @SubscribeEvent
    public static void theurgistHealsBetter(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.THEURGIST) return;
        e.setAmount(e.getAmount() * 1.15f);
    }

    @SubscribeEvent
    public static void theurgistVsUndead(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.THEURGIST) return;

        var srcEnt = e.getSource().getEntity();
        if (srcEnt instanceof LivingEntity le && le.getType().is(EntityTypeTags.UNDEAD)) {
            e.setAmount(e.getAmount() * 0.8f);
        }
    }

    /* =========================
       Pyroclast
       - Sets foes on fire on hit
       - Heavy reduction to fire/lava damage
       - Tames explosion block damage nearby
    ========================== */

    // Switch to IncomingDamage so we can read DamageSource without needing getDamageSource() on LivingDamageEvent
    @SubscribeEvent
    public static void pyroclastIgniteOnHit(LivingIncomingDamageEvent e) {
        var srcEnt = e.getSource().getEntity();
        if (srcEnt instanceof ServerPlayer p && getClassOf(p) == PlayerClass.PYROCLAST) {
            e.getEntity().igniteForSeconds(3); // 1.21.8 name
        }
    }

    @SubscribeEvent
    public static void pyroclastFireResist(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.PYROCLAST) return;

        var ds = e.getSource();
        if (ds.is(DamageTypes.IN_FIRE)
                || ds.is(DamageTypes.ON_FIRE)
                || ds.is(DamageTypes.LAVA)
                || ds.is(DamageTypes.HOT_FLOOR)) {
            e.setAmount(e.getAmount() * 0.25f);
        }
    }

    @SubscribeEvent
    public static void pyroclastTampExplosions(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;

        // Avoid getPosition()/x()/y()/z(). Instead, use the affected-entities list:
        // if any Pyroclast player is in the blast’s entity list, trim the block damage.
        boolean pyroNear = e.getAffectedEntities().stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .anyMatch(pl -> getClassOf(pl) == PlayerClass.PYROCLAST);

        if (pyroNear) {
            var blocks = e.getAffectedBlocks();
            if (blocks.size() > 3) {
                blocks.subList(0, blocks.size() / 3).clear(); // trim ~1/3
            }
        }
    }

    /* =========================
       Venefex
       - Melee hits apply poison + slowness
       - Mitigate magic damage while poisoned
    ========================== */

    @SubscribeEvent
    public static void venefexDebuffs(LivingIncomingDamageEvent e) {
        var srcEnt = e.getSource().getEntity();
        if (!(srcEnt instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.VENEFEX || !isMelee(p)) return;

        e.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        e.getEntity().addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
    }

    @SubscribeEvent
    public static void venefexMagicMitigation(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.VENEFEX) return;
        if (p.hasEffect(MobEffects.POISON) && e.getSource().is(DamageTypes.MAGIC)) {
            e.setAmount(e.getAmount() * 0.6f);
        }
    }

    /* =========================
       Bogatur
       - +20% melee damage when below 50% HP
       - Reduced knockback taken
    ========================== */

    @SubscribeEvent
    public static void bogaturBloodied(LivingIncomingDamageEvent e) {
        var srcEnt = e.getSource().getEntity();
        if (!(srcEnt instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.BOGATUR || !isMelee(p)) return;

        if (p.getHealth() <= 0.5f * p.getMaxHealth()) {
            e.setAmount(e.getAmount() * 1.20f);
        }
    }

    @SubscribeEvent
    public static void bogaturAnchored(LivingKnockBackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.BOGATUR) return;
        e.setStrength(e.getStrength() * 0.6f);
    }

    /* =========================
       Dragoon
       - Negate fall damage; small AoE slam on landing
    ========================== */

    @SubscribeEvent
    public static void dragoonSlam(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.DRAGOON) return;

        e.setCanceled(true); // no fall damage
        if (p.level() instanceof ServerLevel sl) {
            var area = p.getBoundingBox().inflate(2.5, 1.0, 2.5);
            for (var le : sl.getEntitiesOfClass(LivingEntity.class, area, t -> t != p)) {
                le.hurt(p.damageSources().playerAttack(p), 4.0f);
                le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
            }
        }
    }

    /* =========================
       Metalmancer
       - Brief slowness on “armored” targets when hit
       - Small flat DR
    ========================== */

    @SubscribeEvent
    public static void metalmancerShackles(LivingIncomingDamageEvent e) {
        var srcEnt = e.getSource().getEntity();
        if (!(srcEnt instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.METALMANCER) return;

        LivingEntity tgt = e.getEntity();
        var inst = tgt.getAttributes().getInstance(Attributes.ARMOR);
        double armor = inst != null ? inst.getValue() : 0.0;
        if (armor >= 5.0) {
            tgt.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        }
    }

    @SubscribeEvent
    public static void metalmancerFlatDR(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.METALMANCER) return;
        e.setAmount(Math.max(0f, e.getAmount() - 1.0f));
    }

    /* =========================
       Deadeye
       - Long-range arrow bonus without touching arrow internals
         * tag arrow with shooter origin on spawn
         * when arrow damages, scale e.setAmount by distance
    ========================== */

    @SubscribeEvent
    public static void deadeyeMarkArrow(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer p)) return;
        if (getClassOf(p) != PlayerClass.DEADEYE) return;

        CompoundTag t = arrow.getPersistentData();
        CompoundTag origin = new CompoundTag();
        origin.putDouble("ox", p.getX());
        origin.putDouble("oy", p.getY());
        origin.putDouble("oz", p.getZ());
        t.put(KEY_DEADEYE_ORIGIN, origin);
    }

    @SubscribeEvent
    public static void deadeyeScaleOnHit(LivingIncomingDamageEvent e) {
        var src = e.getSource();
        if (!(src.getDirectEntity() instanceof AbstractArrow arrow)) return;

        Optional<CompoundTag> originOpt = arrow.getPersistentData().getCompound(KEY_DEADEYE_ORIGIN);
        if (originOpt.isEmpty()) return;
        CompoundTag origin = originOpt.get();

        double ox = origin.getDouble("ox").orElse(Double.NaN);
        double oy = origin.getDouble("oy").orElse(Double.NaN);
        double oz = origin.getDouble("oz").orElse(Double.NaN);
        if (Double.isNaN(ox)) return;

        var pos = arrow.position();
        double dx = pos.x - ox, dy = pos.y - oy, dz = pos.z - oz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double bonus = Math.min(0.5, Math.max(0.0, (dist - 30.0) / 120.0)); // up to +50%
        e.setAmount((float)(e.getAmount() * (1.0 + bonus)));
    }
}
