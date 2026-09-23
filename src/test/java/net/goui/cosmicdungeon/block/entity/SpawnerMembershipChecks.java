package net.goui.cosmicdungeon.block.entity;

import java.util.*;

/** Lifecycle fixtures exercise the same derived index/queue used by native hooks, without a world. */
public final class SpawnerMembershipChecks {
    private static int checks;
    private static void check(boolean ok, String name) { checks++; if (!ok) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        var manager = net.minecraft.world.level.entity.PersistentEntitySectionManager.class;
        var access = net.minecraft.world.level.entity.EntityAccess.class;
        check(manager.getDeclaredMethod("startTracking", access).getReturnType()==void.class,"Native visible-admission descriptor");
        check(manager.getDeclaredMethod("stopTracking", access).getReturnType()==void.class,"Native hidden-removal descriptor");
        check(net.minecraft.world.entity.Entity.class.getDeclaredMethod("load",net.minecraft.world.level.storage.ValueInput.class).getReturnType()==void.class,"Native loaded-data hook descriptor");
        check(net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("setHealth",float.class).getReturnType()==void.class,"Native health descriptor");
        check(net.minecraft.world.entity.Entity.class.getDeclaredMethod("setUUID",UUID.class).getReturnType()==void.class,"Native /data final identity descriptor");
        var index = new SpawnerMembership<Object>();
        UUID id = new UUID(1, 1); Object old = new Object(), replacement = new Object();
        index.put(id, old, Set.of("a", "b"), true);
        check(index.alive("a") == 1 && index.alive("b") == 1, "Multiple owning tags");
        index.alive(id, old, false);
        check(index.alive("a") == 0 && index.size("a") == 1, "Dead body retained for maintenance but not mob cap");
        index.alive(id, old, true);
        check(index.alive("b") == 1, "Cancelled death/resurrection restores count");
        index.put(id, replacement, Set.of("b"), true);
        index.remove(id, old);
        check(index.alive("b") == 1 && index.size("a") == 0, "Old object removal cannot erase reload replacement");
        index.alive(id, old, false);
        check(index.alive("b") == 1, "Old object health cannot change replacement");
        index.put(id, replacement, Set.of(), true);
        check(index.size() == 0 && index.alive("b") == 0, "Removing last tag releases index entries");

        index.put(id, old, Set.of("a"), true); index.remove(id, old);
        check(index.alive("a")==0,"Hidden chunk leaves visible cap set");
        index.put(id, old, Set.of("a"), true);
        check(index.alive("a")==1,"Visible chunk returns without requiring a fresh spawn");
        index.remove(id, old);

        var random = new Random(3801);
        var objects = new Object[40];
        var tags = new ArrayList<Set<String>>();
        boolean[] alive = new boolean[40];
        for (int i=0; i<40; i++) { objects[i]=new Object(); tags.add(Set.of()); }
        for (int step=0; step<500; step++) {
            int n=random.nextInt(40), op=random.nextInt(3);
            UUID key=new UUID(0,n);
            if (op==0) {
                tags.set(n, random.nextBoolean() ? Set.of("m"+random.nextInt(5)) : Set.of());
                alive[n]=random.nextBoolean();
                index.put(key, objects[n], tags.get(n), alive[n]);
            } else if (op==1) {
                alive[n]=random.nextBoolean(); index.alive(key, objects[n], alive[n]);
            } else {
                index.remove(key, objects[n]); tags.set(n,Set.of());
            }
            for (int marker=0; marker<5; marker++) {
                String tag="m"+marker;int expected=0, size=0;
                for (int j=0;j<40;j++) if(tags.get(j).contains(tag)){size++;if(alive[j])expected++;}
                check(index.alive(tag)==expected && index.size(tag)==size, "Load/death/retag/unload sequence "+step);
            }
        }
        var roundRobin = new SpawnerMembership<Integer>();
        for (int i=0;i<100;i++) roundRobin.put(new UUID(0,i),i,Set.of("wandered"),true);
        var visited = new HashSet<Integer>();
        for(int i=0;i<100;i++) visited.add(roundRobin.next("wandered"));
        check(visited.size()==100 && roundRobin.alive("wandered")==100, "Wandering members retained and visited fairly");

        var queue = new SpawnerMaintenanceQueue<Integer>();
        var totals = new HashMap<Integer,Integer>();
        for(int tick=0;tick<50;tick++) {
            for(int spawner=0;spawner<20;spawner++)queue.request(spawner,tick,3);
            int inspected=queue.run(tick,7,k->totals.merge(k,1,Integer::sum));
            check(inspected<=7,"Global budget");
        }
        check(totals.size()==20,"No spawner starvation under overload");
        check(Collections.max(totals.values())-Collections.min(totals.values())<=1,"Round-robin fair service");
        queue.clear();queue.request(1,60,2);queue.request(2,60,1);
        var units=new HashMap<Integer,Integer>();queue.run(60,500,k->units.merge(k,1,Integer::sum));
        check(units.get(1)==2&&units.get(2)==1&&queue.size()==0,"No repeated same-tick maintenance beyond membership");
        queue.request(1,61,100);queue.request(2,61,100);
        int[] stale={0};queue.run(62,1,k->stale[0]++);
        check(stale[0]==0&&queue.size()==1,"Unticking/unloaded spawner discarded within shared budget");
        queue.removeIf(k->k==2);check(queue.size()==0,"Level cleanup releases queued spawners");
        check(CosmicSpawnerEntities.position("cosmic_spawner_-15_22_68").equals(new net.minecraft.core.BlockPos(-15,22,68)),"Legacy marker coordinates");
        for(String tag:List.of("other","cosmic_spawner_1_2","cosmic_spawner_01_2_3","cosmic_spawner_1_2_3_4","cosmic_spawner_2147483648_2_3"))
            check(CosmicSpawnerEntities.position(tag)==null,"Malformed/unrelated tag isolated");
        System.out.println(checks+" spawner membership/budget checks passed");
    }
}
