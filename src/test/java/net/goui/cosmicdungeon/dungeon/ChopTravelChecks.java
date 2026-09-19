package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/** Save interruption fixtures exercise production codecs/decisions without claiming in-world teleport QA. */
public final class ChopTravelChecks {
    private static int checks;
    private static final UUID OWNER=new UUID(0,2701),OTHER=new UUID(0,2702),TOKEN=new UUID(0,2703);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private interface Work {void run()throws Exception;}
    private static void reject(Work work,String why){boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<DungeonInventoryEscrowData> codec()throws Exception{
        var f=DungeonInventoryEscrowData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<DungeonInventoryEscrowData>)f.get(null);
    }
    private static CompoundTag encode(DungeonInventoryEscrowData data)throws Exception{return (CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();}
    private static CompoundTag inventory(String name,int count){
        var item=new CompoundTag();item.putByte("Slot",(byte)0);item.putString("id","minecraft:paper");item.putInt("count",count);
        var components=new CompoundTag();var custom=new CompoundTag();custom.putString("authored",name);components.put("minecraft:custom_data",custom);item.put("components",components);
        var items=new ListTag();items.add(item);var result=new CompoundTag();result.put("Items",items);return result;
    }
    private static ChopTravelPlan plan(String kind){
        long run=kind.equals("leave")||kind.equals("return")?27:0;
        var source=ChopTravelPlan.pose("minecraft:overworld",2.25,64,-3.75,17,2);
        var destination=run==0?source:ChopTravelPlan.pose("minecraft:the_nether",12.25,70,8.75,23,0);
        var before=inventory("source components",64);var after=inventory("destination components",32);
        var entry=new DungeonInventoryEscrowData.Entry(27,OWNER,inventory("dungeon armor/offhand",1),inventory("village",3),kind.equals("leave"));
        var fire=new CompoundTag();fire.put("pos",new IntArrayTag(new int[]{12,70,8}));
        return ChopTravelPlan.create(OWNER,run,kind,before,after,source,destination,new CompoundTag(),
                run==0?new CompoundTag():DungeonInventoryEscrowData.image(entry),
                ChopOwnershipData.issuedImage(TOKEN),kind.equals("return")?new CompoundTag():ChopOwnershipData.entryImage(new ChopOwnershipData.Entry(TOKEN.toString(),run,false)),fire);
    }
    private record Cut(String label,CompoundTag world,CompoundTag player,CompoundTag ownership){}
    private static CompoundTag player(ChopTravelPlan plan,boolean prepared,boolean applied){
        var p=new CompoundTag();p.put("inventory",plan.tag(applied?"after":"before"));p.put("pose",plan.tag(applied?"destination":"source"));
        if(prepared&&!applied)p.put("custody",plan.reservation());
        if(applied)p.put("receipt",plan.receipt());return p;
    }
    public static int reviewedInterruptions(ChopTravelPlan plan)throws Exception{
        int before=checks;interruptions(plan);return checks-before;
    }
    private static void interruptions(ChopTravelPlan plan)throws Exception{
        String kind=plan.kind();UUID ownerId=plan.owner();var data=codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();data.reserve(plan);
        var cuts=new ArrayList<Cut>();
        cuts.add(new Cut("reservation only",encode(data),player(plan,false,false),plan.tag("ownership_before")));
        cuts.add(new Cut("owner preparation unsaved",encode(data),player(plan,false,false),plan.tag("ownership_before")));
        cuts.add(new Cut("owner preparation saved",encode(data),player(plan,true,false),plan.tag("ownership_before")));
        cuts.add(new Cut("commit not saved",encode(data),player(plan,true,false),plan.tag("ownership_before")));
        data.commit(ownerId,plan.id());plan=data.transition(ownerId);
        var committedImage=encode(data);
        cuts.add(new Cut("commit saved",committedImage,player(plan,true,false),plan.tag("ownership_before")));
        cuts.add(new Cut("ownership saved",encode(data),player(plan,true,false),plan.tag("ownership_after")));
        data.applyEscrow(plan);
        cuts.add(new Cut("escrow saved",encode(data),player(plan,true,false),plan.tag("ownership_after")));
        cuts.add(new Cut("teleport not saved",encode(data),player(plan,true,false),plan.tag("ownership_after")));
        cuts.add(new Cut("owner receipt saved",encode(data),player(plan,false,true),plan.tag("ownership_after")));
        cuts.add(new Cut("receipt with earlier committed world image",committedImage,player(plan,false,true),plan.tag("ownership_after")));
        cuts.add(new Cut("ack not saved",encode(data),player(plan,false,true),plan.tag("ownership_after")));
        data.acknowledge(ownerId,plan.id());
        cuts.add(new Cut("ack saved",encode(data),player(plan,false,true),plan.tag("ownership_after")));
        Path dir=Files.createTempDirectory("chop-save-cuts-");
        try{
            for(var cut:cuts){
                var worldFile=dir.resolve("escrow.dat");var playerFile=dir.resolve("player.dat");var ownerFile=dir.resolve("ownership.dat");
                NbtIo.writeCompressed(cut.world(),worldFile);NbtIo.writeCompressed(cut.player(),playerFile);NbtIo.writeCompressed(cut.ownership(),ownerFile);
                var loaded=codec().parse(NbtOps.INSTANCE,NbtIo.readCompressed(worldFile,NbtAccounter.unlimitedHeap())).getOrThrow();
                var p=NbtIo.readCompressed(playerFile,NbtAccounter.unlimitedHeap());var owner=NbtIo.readCompressed(ownerFile,NbtAccounter.unlimitedHeap());
                var pending=loaded.transition(ownerId);boolean committed=pending==null||pending.committed();
                String label=kind+" / "+cut.label();
                if(pending!=null){
                    check(pending.recoverable(p.getCompoundOrEmpty("custody"),p.getCompoundOrEmpty("receipt")),label+" recovery evidence accepted");
                    if(pending.committed()){
                        check(owner.equals(pending.tag("ownership_before"))||owner.equals(pending.tag("ownership_after")),label+" ownership side effect remains recoverable after receipt");
                        owner=pending.tag("ownership_after");loaded.applyEscrow(pending);
                    }
                    if(!pending.receipted(p.getCompoundOrEmpty("receipt"))){
                        check(p.getCompoundOrEmpty("inventory").equals(pending.tag("before")),label+" source remains exact before owner receipt");
                        if(pending.committed()){
                            check(owner.equals(pending.tag("ownership_before"))||owner.equals(pending.tag("ownership_after")),label+" ownership compare-and-set accepts only frozen images");
                            owner=pending.tag("ownership_after");loaded.applyEscrow(pending);
                            p.put("inventory",pending.tag("after"));p.put("pose",pending.tag("destination"));
                        }
                        p.remove("custody");p.put("receipt",pending.receipt());
                    }
                    check(pending.receipted(p.getCompoundOrEmpty("receipt")),label+" repeat delivery is receipted");
                    loaded.acknowledge(ownerId,pending.id());
                }
                check(p.getCompoundOrEmpty("inventory").equals(plan.tag(committed?"after":"before")),label+" inventory counts and custom components preserved exactly once");
                check(p.getCompoundOrEmpty("pose").equals(plan.tag(committed?"destination":"source")),label+" inventory and location share outcome");
                check(owner.equals(plan.tag(committed?"ownership_after":"ownership_before")),label+" token entitlement shares outcome");
                check(!p.contains("custody")&&loaded.transition(ownerId)==null,label+" pending owner index cleared only after receipt");
                check(codec().parse(NbtOps.INSTANCE,encode(loaded)).getOrThrow().transition(ownerId)==null,label+" settled native codec round trip");
                if(plan.run()>0)check(loaded.get(plan.run(),ownerId).isPresent()==committed,label+" escrow decision retained");
            }
        }finally{
            Files.deleteIfExists(dir.resolve("escrow.dat"));Files.deleteIfExists(dir.resolve("player.dat"));Files.deleteIfExists(dir.resolve("ownership.dat"));Files.deleteIfExists(dir);
        }
    }
    public static void main(String[] args)throws Exception{
        for(String kind:List.of("leave","return","adopt","refresh"))interruptions(plan(kind));
        var plan=plan("leave");var copy=plan.image();copy.getCompoundOrEmpty("before").putString("changed","outside mutation");
        check(!plan.tag("before").contains("changed"),"Journal input is defensively copied");
        copy=plan.tag("after");copy.putString("changed","outside mutation");
        check(!plan.tag("after").contains("changed"),"Journal accessors cannot alter saved output");
        var committed=plan.commit();
        check(!committed.recoverable(new CompoundTag(),new CompoundTag()),"Committed travel cannot invent missing owner custody");
        check(plan.recoverable(new CompoundTag(),new CompoundTag()),"Unprepared reservation cancels without replacing inventory");
        var wrong=plan.reservation();wrong.putString("owner",OTHER.toString());
        check(!committed.recoverable(wrong,new CompoundTag()),"Foreign owner custody held");
        var wrongReceipt=committed.receipt();wrongReceipt.putBoolean("committed",false);
        check(!committed.receipted(wrongReceipt),"Wrong outcome cannot authorize acknowledgement");
        check(!committed.recoverable(plan.reservation(),committed.receipt()),"Receipt plus lingering custody is inconsistent");
        var unknown=plan.image();unknown.putInt("version",99);
        reject(()->new ChopTravelPlan(unknown),"Future journal versions preserved for review");
        var missing=plan.image();missing.remove("before");
        reject(()->new ChopTravelPlan(missing),"Missing source inventory fails closed");
        var invalid=plan.image();invalid.getCompoundOrEmpty("destination").putDouble("x",Double.NaN);
        reject(()->new ChopTravelPlan(invalid),"Invalid destination cannot become a teleport");
        reject(()->new ChopOwnershipData.Entry("not-uuid",0,false),"Invalid ownership token held");
        reject(()->new ChopOwnershipData.Entry(TOKEN.toString(),-1,false),"Negative run rejected");
        reject(()->new ChopOwnershipData.Entry(TOKEN.toString(),27,true),"Delivery cannot still own a live run");
        var data=codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();data.reserve(plan);
        check(data.pendingDimension("minecraft:overworld")&&data.pendingDimension("minecraft:the_nether"),"Both travel dimensions block destructive restore");
        check(!data.pendingDimension("minecraft:the_end"),"Unrelated dimensions remain available");
        reject(()->data.reserve(plan("return")),"Only one unresolved transfer per owner");
        reject(()->data.commit(OWNER,UUID.randomUUID()),"Wrong transaction cannot commit");
        reject(()->data.acknowledge(OWNER,UUID.randomUUID()),"Wrong transaction cannot clear evidence");
        data.commit(OWNER,plan.id());data.applyEscrow(data.transition(OWNER));data.applyEscrow(data.transition(OWNER));
        check(data.get(27,OWNER).isPresent(),"Repeated escrow application is idempotent");
        data.put(new DungeonInventoryEscrowData.Entry(27,OWNER,inventory("foreign",1),new CompoundTag(),false));
        reject(()->data.applyEscrow(data.transition(OWNER)),"Changed escrow cannot be overwritten by recovery");
        var duplicate=new CompoundTag();var entries=new ListTag();var entry=DungeonInventoryEscrowData.image(new DungeonInventoryEscrowData.Entry(27,OWNER,inventory("old",64),new CompoundTag(),true));
        entries.add(entry.copy());entries.add(entry.copy());duplicate.put("entries",entries);
        reject(()->codec().parse(NbtOps.INSTANCE,duplicate).getOrThrow(),"Duplicate legacy escrow is held instead of silently overwritten");
        var legacy=new CompoundTag();var single=new ListTag();single.add(entry);legacy.put("entries",single);
        var migrated=codec().parse(NbtOps.INSTANCE,legacy).getOrThrow();
        check(migrated.transition(OWNER)==null&&DungeonInventoryEscrowData.image(migrated.get(27,OWNER).orElseThrow()).equals(entry),"Legacy escrow adopts empty journal without losing components/counts");
        var saved=new CompoundTag();saved.putString("Dimension","minecraft:overworld");saved.put("Pos",new ListTag());saved.put("Rotation",new ListTag());
        var moved=saved.copy();moved.putString("Dimension","minecraft:the_nether");
        check(PlayerSaveProof.matches(saved,moved)&&!PlayerSaveProof.matchesLocation(saved,moved),"Travel proof verifies position beyond ordinary inventory proof");
        var equipment=saved.copy();equipment.put("equipment",inventory("armor",1));
        check(!PlayerSaveProof.matchesLocation(saved,equipment),"Travel proof covers equipped items");
        final int[] contextual={0},contextless={0};
        try(var stream=ChopTravelChecks.class.getClassLoader().getResourceAsStream("net/goui/cosmicdungeon/dungeon/ChopTravelRecovery.class")){
            new org.objectweb.asm.ClassReader(java.util.Objects.requireNonNull(stream)).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                    if(!name.equals("encode"))return null;
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9){
                        @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){
                            if(owner.equals("net/minecraft/world/level/storage/TagValueOutput")){
                                if(name.equals("createWithContext"))contextual[0]++;
                                if(name.equals("createWithoutContext"))contextless[0]++;
                            }
                        }
                    };
                }
            },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
        }
        check(contextual[0]==1&&contextless[0]==0,"Travel inventory writer supplies native registry context for enchantment/holder components");
        System.out.println("Chop travel checks passed: "+checks);
    }
}
