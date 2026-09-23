package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Durable transfer stages. Only STORED may start delivery; DONE can never recreate an entity. */
public record WolfArchive(String phase, UUID transaction, long run, String sourceDimension, long sourcePosition,
                          String targetDimension, long targetPosition, CompoundTag entity, long targetRun) {
    public WolfArchive(String phase,UUID transaction,long run,String sourceDimension,long sourcePosition,
                       String targetDimension,long targetPosition,CompoundTag entity){
        this(phase,transaction,run,sourceDimension,sourcePosition,targetDimension,targetPosition,entity,0);
    }
    public static final String PREPARED="prepared", STORED="stored", RELEASING="releasing", DONE="done";
    public static final String RESERVED="reserved", REMOVING="removing";
    public static final String MARKER="cosmicdungeon.bogatyr_transfer";
    static final Codec<WolfArchive> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("phase").forGetter(WolfArchive::phase),
            UUIDUtil.STRING_CODEC.fieldOf("transaction").forGetter(WolfArchive::transaction),
            Codec.LONG.fieldOf("run").forGetter(WolfArchive::run),
            Codec.STRING.fieldOf("source_dimension").forGetter(WolfArchive::sourceDimension),
            Codec.LONG.fieldOf("source_position").forGetter(WolfArchive::sourcePosition),
            Codec.STRING.optionalFieldOf("target_dimension","").forGetter(WolfArchive::targetDimension),
            Codec.LONG.optionalFieldOf("target_position",0L).forGetter(WolfArchive::targetPosition),
            CompoundTag.CODEC.fieldOf("entity").forGetter(WolfArchive::entity),
            Codec.LONG.optionalFieldOf("target_run",0L).forGetter(WolfArchive::targetRun)
    ).apply(i,WolfArchive::new));
    public WolfArchive {
        if(!java.util.Set.of(PREPARED,STORED,RELEASING,DONE,RESERVED,REMOVING).contains(phase))
            throw new IllegalArgumentException("Unknown wolf archive phase: "+phase);
        if(targetRun<0||targetRun>0&&!phase.equals(RELEASING)&&!phase.equals(DONE))
            throw new IllegalArgumentException("Run destination requires a delivery stage");
        entity=entity.copy();
    }
    @Override public CompoundTag entity(){return entity.copy();}
    public boolean originalNoAi(){return entity.getBooleanOr("NoAI",false);}
    public boolean originalInvulnerable(){return entity.getBooleanOr("Invulnerable",false);}
    public boolean valid(UUID wolf,UUID owner){
        return wolf!=null&&owner!=null&&run>0&&!sourceDimension.isBlank()&&entity.getStringOr("id","").equals("minecraft:wolf")
                &&entity.read("UUID",UUIDUtil.CODEC).isPresent()&&wolf.equals(WolfIdentity.imageId(entity))
                &&entity.read("Owner",UUIDUtil.CODEC).filter(owner::equals).isPresent()
                &&!entity.contains("Passengers")
                &&(phase.equals(DONE)||(Float.isFinite(entity.getFloatOr("Health",0))&&entity.getFloatOr("Health",0)>0))
                &&(!phase.equals(RELEASING)&&!phase.equals(DONE)||!targetDimension.isBlank());
    }
    public WolfArchive removing(){
        if(!phase.equals(RESERVED))throw new IllegalStateException("Source reservation not verified");
        return new WolfArchive(REMOVING,transaction,run,sourceDimension,sourcePosition,"",0,entity);
    }
    public WolfArchive sourceRemoved(){
        if(!phase.equals(REMOVING))throw new IllegalStateException("Durable source removal was not armed");
        return new WolfArchive(STORED,transaction,run,sourceDimension,sourcePosition,"",0,entity);
    }
    public boolean sourceMatches(BogatyrCompanionData.Companion entry){
        return entry!=null&&entry.located()&&entry.dimension().equals(sourceDimension)
                &&entry.position()==sourcePosition&&valid(entry.wolf(),entry.owner())
                &&entity.read("UUID",UUIDUtil.CODEC).filter(entry.entityUuid()::equals).isPresent();
    }
    public WolfArchive stored(){
        if(!phase.equals(PREPARED))throw new IllegalStateException("Source was not prepared");
        return new WolfArchive(STORED,transaction,run,sourceDimension,sourcePosition,"",0,entity);
    }
    public WolfArchive releasing(String dimension,long position){return releasing(dimension,position,0);}
    public WolfArchive releasing(String dimension,long position,long destinationRun){
        if(!phase.equals(STORED))throw new IllegalStateException("Source world has not been cleared");
        return new WolfArchive(RELEASING,transaction,run,sourceDimension,sourcePosition,dimension,position,entity,destinationRun);
    }
    public boolean targets(String dimension,long currentRun){
        return (phase.equals(RELEASING)||phase.equals(DONE))&&targetDimension.equals(dimension)&&targetRun==currentRun;
    }
    /** Caller must prove this exact attempt never entered the destination level. */
    public WolfArchive rejectedDelivery(){
        if(!phase.equals(RELEASING))throw new IllegalStateException("No prepared delivery to reject");
        return new WolfArchive(STORED,transaction,run,sourceDimension,sourcePosition,"",0,entity);
    }
    public WolfArchive done(){
        if(!phase.equals(RELEASING))throw new IllegalStateException("Delivery not prepared");
        // Keep only identity and original control flags for crash-safe unfreezing, never an entity image.
        var receipt=new CompoundTag();
        receipt.putString("id","minecraft:wolf");
        receipt.store("UUID",UUIDUtil.CODEC,WolfIdentity.imageId(entity));
        var persistent=new CompoundTag();WolfIdentity.set(persistent,WolfIdentity.imageId(entity));
        receipt.put("NeoForgeData",persistent);
        receipt.store("Owner",UUIDUtil.CODEC,entity.read("Owner",UUIDUtil.CODEC).orElseThrow());
        receipt.putBoolean("NoAI",entity.getBooleanOr("NoAI",false));
        receipt.putBoolean("Invulnerable",entity.getBooleanOr("Invulnerable",false));
        return new WolfArchive(DONE,transaction,run,sourceDimension,sourcePosition,targetDimension,targetPosition,receipt,targetRun);
    }
}
