package net.goui.cosmicdungeon.crafting;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
/** Verifies installed 1.21.10 bytecode targets and callback argument contracts without launching Minecraft. */
public final class CraftingHookChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ClassNode node(String path)throws Exception{
        try(var in=CraftingHookChecks.class.getClassLoader().getResourceAsStream(path+".class")){
            if(in==null)throw new IllegalStateException(path);
            var node=new ClassNode();new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return node;
        }
    }
    private static Object value(AnnotationNode annotation,String key){
        if(annotation.values==null)return null;
        for(int i=0;i<annotation.values.size();i+=2)if(annotation.values.get(i).equals(key))return annotation.values.get(i+1);
        return null;
    }
    public static void main(String[] args)throws Exception{
        for(String mixin:List.of("CraftingLookupMixin","CraftingPreviewMixin","CraftingTakeMixin","CraftingClickMixin","CraftingBookMixin","CraftingAutomationMixin","CraftingStonecutterMixin","CraftingSmithingMixin")){
            var source=node("net/goui/cosmicdungeon/mixin/"+mixin);
            var mix=source.invisibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") var owners=(List<Type>)value(mix,"value");
            for(var callback:source.methods){
                var annotations=new ArrayList<AnnotationNode>();
                if(callback.visibleAnnotations!=null)annotations.addAll(callback.visibleAnnotations);
                if(callback.invisibleAnnotations!=null)annotations.addAll(callback.invisibleAnnotations);
                for(var annotation:annotations){
                    boolean redirect=annotation.desc.endsWith("/Redirect;");
                    if(!redirect&&!annotation.desc.endsWith("/Inject;"))continue;
                    @SuppressWarnings("unchecked") var targets=(List<String>)value(annotation,"method");
                    Object rawAt=value(annotation,"at");
                    @SuppressWarnings("unchecked") List<AnnotationNode> ats=rawAt instanceof List<?>?(List<AnnotationNode>)rawAt:List.of((AnnotationNode)rawAt);
                    for(Type owner:owners)for(String target:targets){
                        var matches=node(owner.getInternalName()).methods.stream().filter(m->target.equals(m.name)||target.equals(m.name+m.desc)).toList();
                        check(matches.size()==1,mixin+" unambiguous installed target "+target);
                        var nativeMethod=matches.getFirst();
                        check(((nativeMethod.access&Opcodes.ACC_STATIC)!=0)==((callback.access&Opcodes.ACC_STATIC)!=0),mixin+" static contract");
                        if(!redirect){
                            Type[] expected=Type.getArgumentTypes(nativeMethod.desc),actual=Type.getArgumentTypes(callback.desc);
                            check(actual.length==expected.length+1,mixin+" callback arity");
                            for(int i=0;i<expected.length;i++)check(actual[i].equals(expected[i]),mixin+" callback parameter "+i);
                        }
                        for(var at:ats)if("INVOKE".equals(value(at,"value"))){
                            String invoked=(String)value(at,"target");int count=0;
                            for(var insn:nativeMethod.instructions)if(insn instanceof MethodInsnNode call)
                                if(invoked.equals("L"+call.owner+";"+call.name+call.desc))count++;
                            check(count>=1,mixin+" invocation target exists");
                        }
                    }
                }
            }
        }
        // Production retains native remainder handling; deny happens before assembly/dispensing.
        var crafter=node("net/minecraft/world/level/block/CrafterBlock");
        boolean remainders=false;
        for(var method:crafter.methods)for(var insn:method.instructions)
            if(insn instanceof MethodInsnNode call&&call.name.equals("getRemainingItems"))remainders=true;
        check(remainders,"Installed Crafter handles native remainders after authorized result");
        System.out.println("Crafting native hook checks passed: "+checks);
    }
}
