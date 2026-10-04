package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;

/** Small loaded-space search; at most four useful acquisitions per two-second poll. */
public final class MercenaryCollection {
    private MercenaryCollection(){}
    public static boolean mayOpen(BaseContainerBlockEntity chest,MercenaryContract contract){
        if(contract==null||chest.isLocked())return false;
        // Let the player's first open roll native loot/luck; merely inspecting must not rewrite loot.
        if(chest instanceof RandomizableContainerBlockEntity random&&random.getLootTable()!=null)return false;
        if(chest instanceof ClassLockedChestBlockEntity personal)
            return personal.permitsMercenary(contract.id(),contract.classId());
        return chest instanceof ChestBlockEntity||chest instanceof BarrelBlockEntity||chest instanceof BrewingStandBlockEntity;
    }
    static boolean receive(MercenaryEntity entity,ItemStack stack,PotionBrewing recipes){
        if(!MercenaryInventory.useful(stack,entity.contract(),recipes))return false;
        var slot=MercenaryEquipment.preferred(stack);
        if(slot==null)return MercenaryInventory.insert(entity.supplies(),stack);
        if(!entity.equipmentUpgrade(stack,slot))return false;
        var plan=MercenaryInventory.copy(entity.supplies());
        // Preserve previous gear, including equipment obtained by older builds; never drop it.
        if(!MercenaryInventory.insert(plan,entity.getItemBySlot(slot)))return false;
        entity.setItemSlot(slot,stack.copy());
        MercenaryInventory.commit(entity.supplies(),plan);
        return true;
    }
    static boolean copySlot(MercenaryEntity entity,BaseContainerBlockEntity chest,int slot,
            ResourceLocation dimension,PotionBrewing recipes){
        if(!mayOpen(chest,entity.contract())||slot<0||slot>=chest.getContainerSize())return false;
        var stack=chest.getItem(slot);
        if(stack.isEmpty()||!entity.lootMemory().available(dimension,chest.getBlockPos().asLong(),slot))return false;
        if(!(chest instanceof ClassLockedChestBlockEntity)&&!chest.canTakeItem(chest,slot,stack))return false;
        if(!receive(entity,stack,recipes))return false;
        entity.lootMemory().remember(dimension,chest.getBlockPos().asLong(),slot);
        return true; // Source stack, components and container are never mutated.
    }
    public static void collect(MercenaryEntity entity,ServerLevel level){
        int[] transfers={0};
        var recipes=level.potionBrewing();
        MercenaryBrain.nearby(level,ItemEntity.class,entity.getBoundingBox().inflate(2),24,item->{
            if(transfers[0]>=4||!item.isAlive()||item.hasPickUpDelay()
                    ||item.getTarget()!=null&&!item.getTarget().equals(entity.getUUID())
                    ||!entity.getSensing().hasLineOfSight(item))return;
            if(receive(entity,item.getItem(),recipes)){
                item.discard();transfers[0]++;
            }
        });
        boolean brewer=MercenaryBrewing.enabled(entity.contract()),attemptedBrew=false;
        if(transfers[0]>=4&&!brewer)return;
        var center=entity.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-2,-1,-2),center.offset(2,1,2))){
            if(!level.hasChunkAt(pos)||entity.distanceToSqr(Vec3.atCenterOf(pos))>9)continue;
            if(!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity chest)||!mayOpen(chest,entity.contract()))continue;
            var hit=level.clip(new ClipContext(entity.getEyePosition(),Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
            if(hit.getType()!=HitResult.Type.MISS&&!hit.getBlockPos().equals(pos))continue;
            for(int slot=0;slot<chest.getContainerSize()&&transfers[0]<4;slot++){
                if(copySlot(entity,chest,slot,level.dimension().location(),recipes))transfers[0]++;
            }
            if(brewer&&!attemptedBrew&&chest instanceof BrewingStandBlockEntity){
                MercenaryBrewing.brew(entity,chest,recipes);attemptedBrew=true;
            }
            if(transfers[0]>=4&&(!brewer||attemptedBrew))return;
        }
    }
}
