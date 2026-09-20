package net.goui.cosmicdungeon.achievement.d1;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import com.mojang.serialization.*;
import net.minecraft.nbt.*;
import java.util.*;
public final class D1PiglinChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        var people=new ArrayList<PiglinDisguiseRules.Character>();
        for(int i=1;i<=6;i++)people.add(new PiglinDisguiseRules.Character(new UUID(0,i),true,true,true));
        check(PiglinDisguiseRules.qualifies(people,6),"Six distinct head-wearing characters qualify simultaneously");
        check(!PiglinDisguiseRules.qualifies(people.subList(0,5),6),"Five cannot use a past sixth player");
        check(!PiglinDisguiseRules.qualifies(Collections.nCopies(6,people.getFirst()),6),"Duplicate events cannot impersonate six people");
        for(int i=0;i<6;i++) {
            for(int field=0;field<3;field++){
                var sample=new ArrayList<>(people);
                sample.set(i,new PiglinDisguiseRules.Character(people.get(i).owner(),field!=0,field!=1,field!=2));
                check(!PiglinDisguiseRules.qualifies(sample,6),"Each character needs membership, Camp4 and head");
            }
        }
        check(PiglinDisguiseRules.qualifies(people.subList(0,3),3),"Configured participant count retained");
        check(!PiglinDisguiseRules.qualifies(people,0)&&!PiglinDisguiseRules.qualifies(people,7),"Invalid required counts fail closed");
        check(!PiglinDisguiseRules.qualifies(null,6)&&!PiglinDisguiseRules.qualifies(List.of(),6),"Missing sample is not achievement");
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);
        var codec=(Codec<D1RunData>)field.get(null);
        var data=codec.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        String id="cosmicdungeon:achievements/wolves_in_piglin_clothing";
        var ids=people.stream().map(PiglinDisguiseRules.Character::owner).toList();
        check(data.creditShared(41,id,ids),"Shared run award records exact eligible roster");
        check(!data.creditShared(41,id,List.of(new UUID(0,99))),"Repeat trigger cannot add late recipients");
        check(data.values(42,"awarded").isEmpty(),"Other active instance has no progress");
        var image=codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        var bytes=new java.io.ByteArrayOutputStream();NbtIo.writeCompressed((CompoundTag)image,bytes);
        data=codec.parse(NbtOps.INSTANCE,NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),NbtAccounter.unlimitedHeap())).getOrThrow();
        data.clearRun(41);
        check(data.values(41,"awarded").isEmpty(),"Cleanup resets instance marker");
        for(UUID owner:ids)check(data.achievementCredits(owner).equals(List.of(id)),"Earned UUID entitlement survives native save and cleanup");
        check(data.achievementCredits(new UUID(0,99)).isEmpty(),"Nonrecipient cannot gain retained credit");
        check(data.creditShared(42,id,ids),"Fresh run can independently complete requirement");
        for(UUID owner:ids)check(data.achievementCredits(owner).size()==1,"Repeated completions do not duplicate lifetime entitlements");
        System.out.println(checks+" Piglin achievement checks passed");
    }
}
