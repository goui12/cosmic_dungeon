package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Move one existing main-world pet through saved source reservation/removal, never a live copy. */
final class BogatyrWithdrawal {
    private BogatyrWithdrawal(){}
    static boolean prepare(MinecraftServer server,BogatyrCompanionData.Companion entry)throws Exception{
        var data=BogatyrCompanionData.get(server);
        var archive=data.archive(entry.wolf()).orElse(null);
        var source=BogatyrRecovery.level(server,entry.dimension());
        if(source==null||!source.dimension().equals(Level.OVERWORLD)||!entry.located())
            throw new IllegalStateException("Companion source location needs review before recall");
        if(data.identityHeld(entry.dimension(),entry.entityUuid()))
            throw new IllegalStateException("Companion identity conflict needs developer review");
        if(!BogatyrRecovery.ready(server,entry.wolf(),source,entry.position()))return false;
        Wolf wolf=BogatyrRecovery.loaded(server,entry);
        if(archive==null||archive.phase().equals(WolfArchive.DONE)){
            if(wolf==null)throw new IllegalStateException("Original companion was not found; no replacement was created");
            verifyLive(wolf,entry,source);
            BogatyrCompanions.track(wolf,true);
            entry=data.find(entry.wolf()).orElseThrow();
            archive=new WolfArchive(WolfArchive.RESERVED,UUID.randomUUID(),entry.run(),entry.dimension(),
                    entry.position(),"",0,BogatyrRecovery.sourceImage(wolf));
            data.putArchive(entry.wolf(),archive);BogatyrRecovery.pin(wolf,archive);
            if(!data.flushVerified(server))throw new IllegalStateException("Source reservation save was not verified; original retained");
        }
        if(!archive.sourceMatches(entry)||(wolf!=null&&wolf.blockPosition().asLong()!=archive.sourcePosition()))
            throw new IllegalStateException("Reserved source identity or position changed; transfer held");
        if(archive.phase().equals(WolfArchive.RESERVED)){
            if(wolf==null)throw new IllegalStateException("Reserved original missing before removal was armed; review required");
            verifyLive(wolf,entry,source);BogatyrRecovery.pin(wolf,archive);
            if(!BogatyrRecovery.saved(wolf,BogatyrRecovery.image(wolf)))
                throw new IllegalStateException("Exact source entity save was not verified; original retained");
            var armed=archive.removing();data.putArchive(entry.wolf(),armed);
            if(!data.flushVerified(server)){
                data.putArchive(entry.wolf(),archive);
                throw new IllegalStateException("Source removal receipt was not verified; original retained");
            }
            archive=armed;
        }
        if(!archive.phase().equals(WolfArchive.REMOVING))
            throw new IllegalStateException("Companion is not awaiting source removal");
        if(wolf!=null){
            verifyLive(wolf,entry,source);
            if(!wolf.getPersistentData().getStringOr(WolfArchive.MARKER,"").equals(archive.transaction().toString()))
                throw new IllegalStateException("Original transfer marker does not match");
            wolf.discard();
            if(wolf.getRemovalReason()!=Entity.RemovalReason.DISCARDED)
                throw new IllegalStateException("Original companion removal was not accepted");
        }
        // RESERVED never reaches this absence test. REMOVING was saved only after an exact
        // source entity save, at the fixed recorded position, and before discarding the original.
        source.save(null,true,false);
        var position=BlockPos.of(archive.sourcePosition());
        if(source.getEntity(entry.entityUuid())!=null||ReadOnlyEntityRegion.find(
                ReadOnlyEntityRegion.read(BogatyrRecovery.entityFolder(source),position.getX()>>4,position.getZ()>>4),
                entry.entityUuid()).isPresent())
            throw new IllegalStateException("Source absence was not verified; destination remains blocked");
        if(data.find(entry.wolf()).filter(entry::equals).isEmpty())
            throw new IllegalStateException("Source directory changed during removal; review required");
        var stored=archive.sourceRemoved();
        data.putArchive(entry.wolf(),stored);
        data.remember(new BogatyrCompanionData.Companion(entry.wolf(),entry.owner(),entry.run(),
                "cosmicdungeon:stored_companions",0,false,entry.entityUuid()));
        if(!data.flushVerified(server)){
            data.remember(entry);data.putArchive(entry.wolf(),archive);
            throw new IllegalStateException("Source removal completion was not verified; retry companion recall");
        }
        return true;
    }
    private static void verifyLive(Wolf wolf,BogatyrCompanionData.Companion entry,ServerLevel source){
        if(wolf.level()!=source||!wolf.isAlive()||!wolf.isTame()||wolf.isRemoved()
                ||wolf.isPassenger()||wolf.isVehicle()||BogatyrIdentity.held(wolf)
                ||!entry.owner().equals(BogatyrCompanions.owner(wolf))
                ||!entry.wolf().equals(BogatyrIdentity.id(wolf))||!entry.entityUuid().equals(wolf.getUUID())
                ||!entry.dimension().equals(wolf.level().dimension().location().toString()))
            throw new IllegalStateException("Original companion ownership or live state changed");
    }
}
