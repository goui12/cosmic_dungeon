package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.*;
/** One bounded inventory projection per owner refresh; no world/chunk search, new currency or pet creation. */
public final class BogatyrFeed {
    record Meal(List<Wolf> wolves,List<Integer> slots,boolean healing){
        Meal{wolves=List.copyOf(wolves);slots=List.copyOf(slots);}
    }
    record Plan(List<Meal> meals){
        Plan{meals=List.copyOf(meals);}
        static Plan empty(){return new Plan(List.of());}
        int inputs(){return meals.stream().mapToInt(m->m.slots().size()).sum();}
    }
    private static final class Stock {
        final ServerPlayer player;final List<Integer> slots=new ArrayList<>();final int[] left;
        Stock(ServerPlayer player){
            this.player=player;var inv=player.getInventory();left=new int[inv.getContainerSize()];
            for(int slot=0;slot<left.length;slot++){
                var stack=inv.getItem(slot);
                if(food(stack)){slots.add(slot);left[slot]=stack.getCount();}
            }
            slots.sort(Comparator.comparingInt((Integer i)->WolfFeedRules.tier(id(inv.getItem(i))))
                    .thenComparingInt(i->nutrition(inv.getItem(i))).thenComparing(i->id(inv.getItem(i))).thenComparingInt(i->i));
        }
        List<Integer> take(int count,boolean heal){
            var result=new ArrayList<Integer>();
            for(int slot:slots){
                if(heal&&healing(player.getInventory().getItem(slot))<=0)continue;
                for(int j=0;j<left[slot]&&result.size()<count;j++)result.add(slot);
                if(result.size()==count)break;
            }
            if(result.size()!=count)return List.of();
            for(int slot:result)left[slot]--;return result;
        }
    }
    private BogatyrFeed(){}
    static String id(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    static int nutrition(ItemStack stack){var food=stack.get(DataComponents.FOOD);return food==null?0:food.nutrition();}
    static boolean food(ItemStack stack){return !stack.isEmpty()&&(stack.is(Items.BONE)||stack.is(ItemTags.WOLF_FOOD));}
    static float healing(ItemStack stack){
        return (float)(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("cooked_")
                ?Config.WOLF_COOKED_HEAL.get():Config.WOLF_RAW_HEAL.get()).doubleValue();
    }
    static boolean authorized(ServerPlayer p,long run){
        var current=ClassResourceService.activeRun(p).orElse(null);
        return current!=null&&current.runId()==run&&p.isAlive()&&!p.isDeadOrDying()
                &&"bogatyr".equals(ClassData.getClassId(p))
                &&p.level().getServer().getPlayerList().getPlayer(p.getUUID())==p
                &&BogatyrCompanionData.get(p.level().getServer()).mode(p.getUUID(),run).writable()
                &&InventoryTransactionGuard.beforeCurrentInventoryAction(p);
    }
    static boolean breedable(Wolf wolf,float health){
        return (BogatyrModes.standing(wolf)||!wolf.isOrderedToSit()&&!wolf.isInSittingPose())
                &&wolf.getAge()==0&&WolfCareRules.healthy(health,wolf.getMaxHealth())
                &&!wolf.isPassenger()&&!wolf.isVehicle()&&!wolf.isLeashed()&&!wolf.isPanicking()&&BogatyrModes.careAllowed(wolf);
    }
    // Native partners are within eight blocks. A cell map avoids quadratic scans of an uncapped pack.
    private record Cell(int x,int y,int z){
        static Cell at(Wolf w){return new Cell((int)Math.floor(w.getX()/8),(int)Math.floor(w.getY()/8),(int)Math.floor(w.getZ()/8));}
    }
    static Plan plan(ServerPlayer p,long run,List<Wolf> pack){
        if(!authorized(p,run))return Plan.empty();
        var stock=new Stock(p);var meals=new ArrayList<Meal>();var health=new IdentityHashMap<Wolf,Float>();
        var eligible=pack.stream().filter(BogatyrModes::careAllowed).toList();int count=0;
        var wounded=eligible.stream().filter(w->WolfCommandRules.injured(w.getHealth(),w.getMaxHealth()))
                .sorted(Comparator.comparingDouble(Wolf::getHealth).thenComparing(Wolf::getUUID)).toList();
        for(var wolf:wounded){
            float value=wolf.getHealth();
            while(value<wolf.getMaxHealth()&&count<WolfFeedRules.LIMIT){
                var slots=stock.take(1,true);if(slots.isEmpty())break;
                meals.add(new Meal(List.of(wolf),slots,true));count++;
                value=Math.min(wolf.getMaxHealth(),value+healing(p.getInventory().getItem(slots.getFirst())));
            }
            health.put(wolf,value);
        }
        var adults=eligible.stream().filter(w->breedable(w,health.getOrDefault(w,w.getHealth()))).toList();
        var cells=new HashMap<Cell,List<Wolf>>();
        for(var wolf:adults)cells.computeIfAbsent(Cell.at(wolf),key->new ArrayList<>()).add(wolf);
        var used=Collections.newSetFromMap(new IdentityHashMap<Wolf,Boolean>());
        for(var wolf:adults){
            if(count>=WolfFeedRules.LIMIT)break;
            if(used.contains(wolf))continue;
            var cell=Cell.at(wolf);Wolf mate=null;int visited=0;
            search:for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)
                for(var candidate:cells.getOrDefault(new Cell(cell.x+dx,cell.y+dy,cell.z+dz),List.of())){
                    if(candidate==wolf||used.contains(candidate))continue;
                    if(++visited>64)break search;
                    if(wolf.distanceToSqr(candidate)<=64&&(!wolf.isInLove()||!candidate.isInLove())){mate=candidate;break search;}
                }
            if(mate==null)continue;
            var targets=new ArrayList<Wolf>();if(!wolf.isInLove())targets.add(wolf);if(!mate.isInLove())targets.add(mate);
            if(count+targets.size()>WolfFeedRules.LIMIT)continue;
            var slots=stock.take(targets.size(),false);if(slots.isEmpty())continue;
            // Both partners are recorded, including an already-fed mate that costs nothing.
            meals.add(new Meal(List.of(wolf,mate),slots,false));used.add(wolf);used.add(mate);count+=slots.size();
        }
        return new Plan(meals);
    }
    static int execute(ServerPlayer p,long run){
        if(!authorized(p,run))return 0;
        var pack=BogatyrCommands.loaded(p,run);var plan=plan(p,run,pack);int spent=0;
        for(var meal:plan.meals()){
            if(!authorized(p,run))break;
            var needed=new HashMap<Integer,Integer>();for(int slot:meal.slots())needed.merge(slot,1,Integer::sum);
            if(needed.entrySet().stream().anyMatch(e->!food(p.getInventory().getItem(e.getKey()))
                    ||p.getInventory().getItem(e.getKey()).getCount()<e.getValue()))continue;
            if(meal.wolves().stream().anyMatch(w->!w.isAlive()||w.isRemoved()||!w.isAddedToLevel()||!BogatyrModes.careAllowed(w)))continue;
            if(meal.healing()){
                var wolf=meal.wolves().getFirst();float before=wolf.getHealth();if(!WolfCommandRules.injured(before,wolf.getMaxHealth()))continue;
                wolf.heal(healing(p.getInventory().getItem(meal.slots().getFirst())));
                if(wolf.getHealth()<=before)continue; // A canceled heal never consumes food.
            }else{
                var a=meal.wolves().get(0);var b=meal.wolves().get(1);
                if(!breedable(a,a.getHealth())||!breedable(b,b.getHealth())||a.distanceToSqr(b)>64
                        ||!BogatyrWolfEvents.breedingAllowed(a,b))continue;
                var hungry=meal.wolves().stream().filter(w->!w.isInLove()).toList();
                if(hungry.size()!=meal.slots().size())continue;
                for(var wolf:hungry)wolf.setInLove(p);
                if(hungry.stream().anyMatch(w->!w.isInLove()))continue;
            }
            for(int slot:meal.slots()){p.getInventory().getItem(slot).shrink(1);spent++;}
            for(var wolf:meal.wolves())wolf.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
        }
        if(spent>0){p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();}
        return spent;
    }
}
