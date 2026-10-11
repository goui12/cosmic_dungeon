package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/** Immutable, bounded routes captured from this Minecraft version's vanilla brewing definitions. */
public final class PotionRecipeCatalog {
    private static final int MAX_STEPS=16, MAX_ROUTES=4096, MAX_STATES=512;
    public record Target(String id,Holder<Potion> potion,Item container,Set<String> classes) {
        public Target {
            Objects.requireNonNull(id);Objects.requireNonNull(potion);Objects.requireNonNull(container);
            classes=Set.copyOf(classes);
            if(id.isBlank()||classes.isEmpty())throw new IllegalArgumentException("Potion target");
        }
    }
    public record Route(Target target,Holder<Potion> inputPotion,Item inputContainer,List<Item> ingredients) {
        public Route {
            Objects.requireNonNull(target);Objects.requireNonNull(inputPotion);Objects.requireNonNull(inputContainer);
            ingredients=List.copyOf(ingredients);
            if(ingredients.isEmpty()||ingredients.size()>MAX_STEPS)throw new IllegalArgumentException("Potion route steps");
        }
        public int steps(){return ingredients.size();}
    }
    private record State(Holder<Potion> potion,Item container) {}
    private record PotionEdge(Holder<Potion> from,Item ingredient,Holder<Potion> to) {}
    private record ContainerEdge(Item from,Item ingredient,Item to) {}
    private record Edge(State from,Item ingredient,State to) {}
    private static final class Capture extends PotionBrewing.Builder {
        final FeatureFlagSet flags;
        final List<PotionEdge> potions=new ArrayList<>();
        final List<ContainerEdge> containers=new ArrayList<>();
        Capture(FeatureFlagSet flags){super(flags);this.flags=flags;}
        @Override public void addMix(Holder<Potion> from,Item ingredient,Holder<Potion> to) {
            if(from.value().isEnabled(flags)&&ingredient.isEnabled(flags)&&to.value().isEnabled(flags))
                potions.add(new PotionEdge(from,ingredient,to));
        }
        @Override public void addContainerRecipe(Item from,Item ingredient,Item to) {
            if(from.isEnabled(flags)&&ingredient.isEnabled(flags)&&to.isEnabled(flags))
                containers.add(new ContainerEdge(from,ingredient,to));
        }
    }
    private final List<Target> targets;
    private final Map<String,Target> byId;
    private final Map<String,List<Route>> routes;
    private PotionRecipeCatalog(List<Target> targets,Map<String,List<Route>> routes) {
        this.targets=List.copyOf(targets);this.routes=Map.copyOf(routes);
        var index=new LinkedHashMap<String,Target>();
        for(var target:targets)if(index.put(target.id(),target)!=null)throw new IllegalArgumentException("Duplicate potion target");
        byId=Map.copyOf(index);
    }
    public List<Target> targets(){return targets;}
    public Target target(String id){return id==null?null:byId.get(id);}
    public List<Route> routes(Target target) {
        return target!=null&&target.equals(byId.get(target.id()))?routes.get(target.id()):List.of();
    }

    public static PotionRecipeCatalog vanilla(FeatureFlagSet flags) {
        Objects.requireNonNull(flags);
        var capture=new Capture(flags);
        PotionBrewing.addVanillaMixes(capture); // Deliberately excludes third-party recipe registration.
        var potionTypes=new LinkedHashSet<Holder<Potion>>();potionTypes.add(Potions.WATER);
        for(var edge:capture.potions){potionTypes.add(edge.from());potionTypes.add(edge.to());}
        var edges=new LinkedHashSet<Edge>();
        for(var container:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION))
            if(container.isEnabled(flags))for(var edge:capture.potions)
                edges.add(new Edge(new State(edge.from(),container),edge.ingredient(),new State(edge.to(),container)));
        for(var edge:capture.containers)for(var potion:potionTypes)
            edges.add(new Edge(new State(potion,edge.from()),edge.ingredient(),new State(potion,edge.to())));
        var outgoing=new HashMap<State,List<Edge>>();
        for(var edge:edges)outgoing.computeIfAbsent(edge.from(),ignored->new ArrayList<>()).add(edge);
        var water=new State(Potions.WATER,Items.POTION);
        var reachable=new HashSet<State>();var queue=new ArrayDeque<State>();
        if(Items.POTION.isEnabled(flags)&&Potions.WATER.value().isEnabled(flags)){reachable.add(water);queue.add(water);}
        while(!queue.isEmpty())for(var edge:outgoing.getOrDefault(queue.remove(),List.of()))
            if(reachable.add(edge.to())) {
                if(reachable.size()>MAX_STATES)throw new IllegalStateException("Vanilla potion graph exceeds state bound");
                queue.add(edge.to());
            }
        var incoming=new HashMap<State,List<Edge>>();
        for(var edge:edges)if(reachable.contains(edge.from())&&reachable.contains(edge.to()))
            incoming.computeIfAbsent(edge.to(),ignored->new ArrayList<>()).add(edge);
        incoming.values().forEach(list->list.sort(Comparator.comparing(e->stateKey(e.from())+"/"+itemKey(e.ingredient()))));
        var targets=new ArrayList<Target>();
        for(var potion:potionTypes) {
            boolean positive=false,negative=false,unsupported=false;
            for(var effect:potion.value().getEffects()) {
                var category=effect.getEffect().value().getCategory();
                if(category==MobEffectCategory.BENEFICIAL)positive=true;
                else if(category==MobEffectCategory.HARMFUL)negative=true;
                else unsupported=true;
            }
            if(unsupported||(!positive&&!negative))continue;
            var container=positive&&negative?Items.POTION:Items.SPLASH_POTION;
            if(!reachable.contains(new State(potion,container)))continue;
            var classes=positive&&negative?Set.of("theurgist","venefex"):Set.of(positive?"theurgist":"venefex");
            String id=potionKey(potion)+(container==Items.POTION?"_drinkable":"_splash");
            targets.add(new Target(id,potion,container,classes));
        }
        targets.sort(Comparator.comparing(Target::id));
        var routes=new LinkedHashMap<String,List<Route>>();
        for(var target:targets) {
            var found=new LinkedHashMap<String,Route>();
            var end=new State(target.potion(),target.container());
            collect(target,end,incoming,new HashSet<>(Set.of(end)),List.of(),found);
            var ordered=new ArrayList<>(found.values());
            ordered.sort(Comparator.comparingInt(Route::steps).thenComparing(PotionRecipeCatalog::routeKey));
            if(ordered.isEmpty())throw new IllegalStateException("Unreachable potion target");
            routes.put(target.id(),List.copyOf(ordered));
        }
        return new PotionRecipeCatalog(targets,routes);
    }

    private static void collect(Target target,State current,Map<State,List<Edge>> incoming,Set<State> visited,
                                List<Item> suffix,Map<String,Route> found) {
        for(var edge:incoming.getOrDefault(current,List.of())) {
            if(visited.contains(edge.from()))continue;
            if(suffix.size()>=MAX_STEPS)throw new IllegalStateException("Vanilla potion route exceeds step bound");
            var ingredients=new ArrayList<Item>();ingredients.add(edge.ingredient());ingredients.addAll(suffix);
            var route=new Route(target,edge.from().potion(),edge.from().container(),ingredients);
            found.putIfAbsent(routeKey(route),route);
            if(found.size()>MAX_ROUTES)throw new IllegalStateException("Vanilla potion routes exceed bound");
            visited.add(edge.from());collect(target,edge.from(),incoming,visited,ingredients,found);visited.remove(edge.from());
        }
    }
    private static String routeKey(Route route) {
        return stateKey(new State(route.inputPotion(),route.inputContainer()))+"/"+String.join(",",
                route.ingredients().stream().map(PotionRecipeCatalog::itemKey).toList());
    }
    private static String stateKey(State state){return potionKey(state.potion())+"@"+itemKey(state.container());}
    private static String potionKey(Holder<Potion> potion){return BuiltInRegistries.POTION.getKey(potion.value()).toString();}
    private static String itemKey(Item item){return BuiltInRegistries.ITEM.getKey(item).toString();}
}
