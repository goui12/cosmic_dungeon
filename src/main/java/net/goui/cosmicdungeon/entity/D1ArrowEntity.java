package net.goui.cosmicdungeon.entity;

import javax.annotation.Nullable;
import net.goui.cosmicdungeon.playerclass.d1.D1AbilityIdentity;
import net.goui.cosmicdungeon.playerclass.d1.D1ArrowAbilities;
import net.goui.cosmicdungeon.playerclass.d1.D1AmmunitionCatalog;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Native arrow physics, weapon enchantments and exact pickup/save stack, with D1 server effects.
 * Only the bounded visual identity is synchronized; gameplay always resolves the server stack.
 * There is no vanilla potion payload to decay into an ordinary arrow after thirty seconds.
 */
public final class D1ArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<String> VISUAL =
            SynchedEntityData.defineId(D1ArrowEntity.class, EntityDataSerializers.STRING);

    public D1ArrowEntity(EntityType<? extends D1ArrowEntity> type, Level level) { super(type, level); }

    public D1ArrowEntity(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.D1_ARROW.get(), owner, level, ammo, weapon);
        refreshVisual();
    }

    public D1ArrowEntity(Level level, double x, double y, double z, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.D1_ARROW.get(), x, y, z, level, ammo, weapon);
        refreshVisual();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VISUAL, "");
    }

    public String visualIdentity() { return entityData.get(VISUAL); }

    private void refreshVisual() {
        String id = D1AbilityIdentity.identify(getPickupItemStackOrigin());
        var entry = D1AmmunitionCatalog.find(id);
        entityData.set(VISUAL, entry != null && entry.stars() == 0 ? id : "");
    }

    @Override
    protected void setPickupItemStack(ItemStack stack) {
        super.setPickupItemStack(stack);
        refreshVisual();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        refreshVisual();
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        D1ArrowAbilities.apply(this, target);
    }

    @Override
    protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }

    // TODO(D1 ammunition native TEST): save/reload an embedded arrow, then verify its exact
    // pickup item/components, owner permission, custom flight texture and effect duration.
    // Support/instant arrows are consumed at impact by D1ArrowAbilities; no chest migration.
}
