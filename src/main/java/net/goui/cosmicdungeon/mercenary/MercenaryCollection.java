package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.block.custom.ClassLocked;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;

/** Small loaded-space search, at most four whole-stack transfers per two-second poll. */
public final class MercenaryCollection {
    private MercenaryCollection(){}
    public static boolean mayOpen(BaseContainerBlockEntity chest,MercenaryContract contract){
        if(contract==null||chest.isLocked())return false;
        if(chest instanceof ClassLockedChestBlockEntity personal)
            return personal.permitsMercenary(contract.id(),contract.classId());
        return chest instanceof ChestBlockEntity||chest instanceof BarrelBlockEntity||chest instanceof BrewingStandBlockEntity;
    }
    public static void collect(MercenaryEntity entity,ServerLevel level){
        int[] transfers={0};
        MercenaryBrain.nearby(level,ItemEntity.class,entity.getBoundingBox().inflate(2),24,item->{
            if(transfers[0]>=4||!item.isAlive()||item.hasPickUpDelay()
                    ||item.getTarget()!=null&&!item.getTarget().equals(entity.getUUID())
                    ||!entity.getSensing().hasLineOfSight(item)
                    ||!MercenaryInventory.permitted(item.getItem(),entity.contract()))return;
            if(MercenaryInventory.insert(entity.supplies(),item.getItem())){
                item.discard();transfers[0]++;
            }
        });
        if(transfers[0]>=4)return;
        var center=entity.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-2,-1,-2),center.offset(2,1,2))){
            if(!level.hasChunkAt(pos)||entity.distanceToSqr(Vec3.atCenterOf(pos))>9)continue;
            if(!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity chest)||!mayOpen(chest,entity.contract()))continue;
            var hit=level.clip(new ClipContext(entity.getEyePosition(),Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
            if(hit.getType()!=HitResult.Type.MISS&&!hit.getBlockPos().equals(pos))continue;
            for(int slot=0;slot<chest.getContainerSize()&&transfers[0]<4;slot++){
                var stack=chest.getItem(slot);
                if(stack.isEmpty()||!MercenaryInventory.permitted(stack,entity.contract()))continue;
                var plan=MercenaryInventory.copy(entity.supplies());
                if(!MercenaryInventory.insert(plan,stack))continue;
                // Owned slot chests explicitly authorize their mercenary; other containers keep extraction vetoes.
                if(!(chest instanceof ClassLockedChestBlockEntity)&&!chest.canTakeItem(chest,slot,stack))continue;
                chest.setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
                MercenaryInventory.commit(entity.supplies(),plan);chest.setChanged();transfers[0]++;
            }
            if(transfers[0]>=4)return;
        }
    }
}
