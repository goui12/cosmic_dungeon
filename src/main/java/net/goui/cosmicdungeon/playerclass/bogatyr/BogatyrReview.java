package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.item.identity.ItemAuthoringPlan;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import java.util.*;

/** Developer diagnostics and exact live-source re-reservation. Never creates or deletes a wolf. */
final class BogatyrReview {
    record State(BogatyrCompanionData.Companion entry,WolfArchive archive){}
    private record Pending(UUID bond,String dimension,long position,ItemAuthoringPlan<State> edit){}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private BogatyrReview(){}
    static void clear(){PENDING.clear();}
    static void clearPlayer(UUID player){PENDING.remove(player);}
    static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("d1").then(Commands.literal("wolves")
                .then(Commands.literal("holds").requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(c->holds(c.getSource())))
                .then(Commands.literal("review").requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("bond",UuidArgument.uuid())
                                .executes(c->inspect(c.getSource(),UuidArgument.getUuid(c,"bond")))
                                .then(Commands.literal("preview").executes(c->preview(c.getSource(),UuidArgument.getUuid(c,"bond"))))))
                .then(Commands.literal("review-apply").requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("token",StringArgumentType.word())
                                .executes(c->apply(c.getSource(),StringArgumentType.getString(c,"token")))))));
    }
    private static void say(CommandSourceStack source,String message){source.sendSuccess(()->Component.literal(message),false);}
    private static int fail(CommandSourceStack source,String message){source.sendFailure(Component.literal(message));return 0;}
    private static int holds(CommandSourceStack source){
        var holds=BogatyrCompanionData.get(source.getServer()).identityHolds();
        say(source,holds.size()+" persistent companion identity holds. Format: dimension | native UUID.");
        holds.stream().sorted().limit(32).forEach(h->say(source,h));
        return holds.size();
    }
    private static State state(CommandSourceStack source,UUID bond){
        var data=BogatyrCompanionData.get(source.getServer());
        return new State(data.find(bond).orElseThrow(()->new IllegalStateException("Unknown companion bond")),
                data.archive(bond).orElse(null));
    }
    private static int inspect(CommandSourceStack source,UUID bond){
        try{
            var state=state(source,bond);var entry=state.entry();var archive=state.archive();
            say(source,"Bond "+bond+" | owner "+entry.owner()+" | native "+entry.entityUuid());
            say(source,"Directory "+entry.dimension()+" @ "+net.minecraft.core.BlockPos.of(entry.position()).toShortString()
                    +" | run "+entry.run()+" | located "+entry.located());
            if(archive==null)say(source,"No archive. A stale directory alone cannot recreate a companion.");
            else{
                say(source,"Archive "+archive.phase()+" | transaction "+archive.transaction()+" | source run "+archive.run());
                say(source,"Source "+archive.sourceDimension()+" @ "+net.minecraft.core.BlockPos.of(archive.sourcePosition()).toShortString());
                if(!archive.targetDimension().isEmpty())say(source,"Destination "+archive.targetDimension()+" @ "
                        +net.minecraft.core.BlockPos.of(archive.targetPosition()).toShortString()+" | run "+archive.targetRun());
            }
            Wolf actual=BogatyrRecovery.loaded(source.getServer(),entry);
            say(source,actual==null?"No verified live wolf at the recorded addresses; no chunks were loaded.":
                    "Observed live wolf: "+actual.level().dimension().location()+" @ "+actual.blockPosition().toShortString());
            say(source,"Preview can re-reserve only a verified, marked, living main-world original. "
                    +"Missing deliveries, unknown owners and cloned identity holds remain protected.");
            return 1;
        }catch(Exception error){return fail(source,Objects.toString(error.getMessage(),"Companion review failed"));}
    }
    private static ServerPlayer developer(CommandSourceStack source){
        if(source.getEntity() instanceof ServerPlayer player&&AccessPolicy.isDeveloper(player))return player;
        fail(source,"Preview/apply requires an in-game developer. Console review remains read-only.");return null;
    }
    private static Wolf original(CommandSourceStack source,State state){
        var entry=state.entry();var archive=state.archive();
        if(archive==null||!Set.of(WolfArchive.RESERVED,WolfArchive.REMOVING).contains(archive.phase()))
            throw new IllegalStateException("Only a reserved or armed live source can be re-reserved");
        Wolf wolf=BogatyrRecovery.loaded(source.getServer(),entry);
        if(wolf==null||!wolf.level().dimension().equals(Level.OVERWORLD)||!wolf.isAlive()||!wolf.isTame()
                ||wolf.isRemoved()||wolf.isPassenger()||wolf.isVehicle()||BogatyrIdentity.held(wolf)
                ||!entry.dimension().equals(archive.sourceDimension())
                ||!archive.sourceDimension().equals(wolf.level().dimension().location().toString())
                ||!entry.owner().equals(BogatyrCompanions.owner(wolf))
                ||!entry.wolf().equals(BogatyrIdentity.id(wolf))||!entry.entityUuid().equals(wolf.getUUID())
                ||!wolf.getPersistentData().getStringOr(WolfArchive.MARKER,"").equals(archive.transaction().toString()))
            throw new IllegalStateException("Live original identity, owner, source or transaction is not verified");
        return wolf;
    }
    private static int preview(CommandSourceStack source,UUID bond){
        var actor=developer(source);if(actor==null)return 0;
        try{
            var before=state(source,bond);Wolf wolf=original(source,before);
            String token=UUID.randomUUID().toString();
            var plan=new ItemAuthoringPlan<State>(token,source.getServer().overworld().getGameTime()
                    +Config.WOLF_REVIEW_SECONDS.get()*20L,before,before);
            PENDING.put(actor.getUUID(),new Pending(bond,wolf.level().dimension().location().toString(),wolf.blockPosition().asLong(),plan));
            say(source,"PREVIEW: re-reserve this same living wolf at "+wolf.blockPosition().toShortString()
                    +". Preserve its current contents; archive the previous record as review evidence. No entity will be created or deleted. "
                    +"Apply: /d1 wolves review-apply "+token);
            return 1;
        }catch(Exception error){return fail(source,Objects.toString(error.getMessage(),"Preview failed"));}
    }
    private static int apply(CommandSourceStack source,String token){
        var actor=developer(source);if(actor==null)return 0;
        var pending=PENDING.get(actor.getUUID());if(pending==null)return fail(source,"No companion review preview");
        try{
            var before=state(source,pending.bond());Wolf wolf=original(source,before);
            boolean same=pending.dimension().equals(wolf.level().dimension().location().toString())
                    &&pending.position()==wolf.blockPosition().asLong();
            if(!pending.edit().accepts(token,source.getServer().overworld().getGameTime(),same,before,false,Objects::equals))
                return fail(source,"Review expired or source changed; preview again");
            var nextEntry=new BogatyrCompanionData.Companion(before.entry().wolf(),before.entry().owner(),before.entry().run(),
                    pending.dimension(),pending.position(),true,wolf.getUUID());
            var nextArchive=new WolfArchive(WolfArchive.RESERVED,UUID.randomUUID(),before.archive().run(),pending.dimension(),
                    pending.position(),"",0,BogatyrRecovery.sourceImage(wolf));
            var evidence=new CompoundTag();evidence.putString("action","re-reserve verified live original");
            evidence.store("actor",UUIDUtil.CODEC,actor.getUUID());evidence.putLong("time_epoch_ms",System.currentTimeMillis());
            evidence.put("before_entry",BogatyrCompanionData.Companion.CODEC.encodeStart(NbtOps.INSTANCE,before.entry()).getOrThrow());
            evidence.put("before_archive",WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,before.archive()).getOrThrow());
            evidence.put("after_entry",BogatyrCompanionData.Companion.CODEC.encodeStart(NbtOps.INSTANCE,nextEntry).getOrThrow());
            evidence.put("after_archive",WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,nextArchive).getOrThrow());
            UUID review=UUID.randomUUID();
            var folder=source.getServer().getWorldPath(LevelResource.ROOT).resolve("data/cosmicdungeon_companion_reviews");
            CompanionReviewFiles.write(folder,review,false,evidence);
            var data=BogatyrCompanionData.get(source.getServer());data.remember(nextEntry);data.putArchive(pending.bond(),nextArchive);
            if(!data.flushVerified(source.getServer())){
                data.remember(before.entry());data.putArchive(pending.bond(),before.archive());BogatyrRecovery.pin(wolf,before.archive());
                return fail(source,"Review save not verified; previous reservation retained. Evidence "+review);
            }
            BogatyrRecovery.pin(wolf,nextArchive);pending.edit().committed(false);PENDING.remove(actor.getUUID());
            try{CompanionReviewFiles.write(folder,review,true,evidence);}
            catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("Review applied; completion evidence write failed: "+review,error);}
            say(source,"Same original re-reserved. Owner may retry recall/recovery. Review evidence "+review);return 1;
        }catch(Exception error){return fail(source,Objects.toString(error.getMessage(),"Review apply failed; inspect preserved source"));}
    }
}
// TODO(M44, damaged-save recovery): a missing prepared destination or a cloned bond cannot be
// cleared from one absent chunk lookup. Review full source/destination entity saves and independent
// backups before any explicit restoration. Keep original archives/holds. Never blind respawn,
// bulk rekey, infer another owner, or drop a cap slot merely because the player is offline.
