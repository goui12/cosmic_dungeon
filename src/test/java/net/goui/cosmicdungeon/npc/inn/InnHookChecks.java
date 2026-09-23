package net.goui.cosmicdungeon.npc.inn;

import org.objectweb.asm.*;
import java.io.InputStream;
import java.util.*;

/** Exact native bytecode targets only; actual Mixin application still needs the licensed server. */
public final class InnHookChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private record Method(String name,String descriptor){}
    private static Set<Method> methods(String owner)throws Exception{
        var result=new HashSet<Method>();
        try(InputStream stream=InnHookChecks.class.getClassLoader().getResourceAsStream(owner+".class")){
            if(stream==null)throw new IllegalStateException("Missing native class "+owner);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                    result.add(new Method(name,desc));return null;
                }
            },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        return result;
    }
    public static void main(String[] args)throws Exception{
        String world="net/minecraft/world/level/",core="Lnet/minecraft/core/",state="L"+world+"block/state/BlockState;";
        String spread="(L"+world+"LevelAccessor;"+core+"BlockPos;"+state+core+"Direction;L"+world+"material/FluidState;)V";
        check(methods(world+"material/FlowingFluid").contains(new Method("spreadTo",spread)),"FlowingFluid mixin descriptor matches installed native method");
        check(methods(world+"material/LavaFluid").contains(new Method("spreadTo",spread)),"Lava override protected before replacement");
        check(methods(world+"block/FireBlock").contains(new Method("checkBurnOut","(L"+world+"Level;"+core+"BlockPos;ILnet/minecraft/util/RandomSource;I"+core+"Direction;)V")),"Burn destruction target matches NeoForge patch");
        check(methods(world+"block/entity/CreakingHeartBlockEntity").contains(new Method("spawnProtector","(Lnet/minecraft/server/level/ServerLevel;L"+world+"block/entity/CreakingHeartBlockEntity;)Lnet/minecraft/world/entity/monster/creaking/Creaking;")),"Protected Heart spawn interception target exists");
        final int[] redirects={0};
        try(var stream=InnHookChecks.class.getClassLoader().getResourceAsStream(world+"block/FireBlock.class")){
            new ClassReader(Objects.requireNonNull(stream)).accept(new ClassVisitor(Opcodes.ASM9){
                @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                    if(!name.equals("tick"))return null;
                    return new MethodVisitor(Opcodes.ASM9){
                        @Override public void visitMethodInsn(int opcode,String owner,String called,String descriptor,boolean itf){
                            if(owner.equals("net/minecraft/server/level/ServerLevel")&&called.equals("setBlock")
                                    &&descriptor.equals("("+core+"BlockPos;"+state+"I)Z"))redirects[0]++;
                        }
                    };
                }
            },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }
        check(redirects[0]>=1,"Fire ignition redirect matches actual invocation owner and descriptor");
        System.out.println("Inn native hook checks passed: "+checks);
    }
}
