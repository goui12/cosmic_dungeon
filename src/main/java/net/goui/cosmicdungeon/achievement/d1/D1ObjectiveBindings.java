package net.goui.cosmicdungeon.achievement.d1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.saveddata.*;
import java.util.*;
public final class D1ObjectiveBindings extends SavedData {
    public record Place(String dimension,BlockPos pos){
        static final Codec<Place> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Place::dimension),BlockPos.CODEC.fieldOf("pos").forGetter(Place::pos)).apply(i,Place::new));
    }
    private static final Codec<D1ObjectiveBindings> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING,Place.CODEC).optionalFieldOf("locations",Map.of()).forGetter(d->d.locations)
    ).apply(i,D1ObjectiveBindings::new));
    private static final SavedDataType<D1ObjectiveBindings> TYPE=new SavedDataType<>("cosmicdungeon_d1_objective_locations_v1",D1ObjectiveBindings::new,CODEC);
    private final Map<String,Place> locations=new HashMap<>();
    private D1ObjectiveBindings(){}
    private D1ObjectiveBindings(Map<String,Place> map){locations.putAll(map);}
    public static D1ObjectiveBindings get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public Place location(String key){
        Place configured=locations.get(key);if(configured!=null)return configured;
        // Source coordinates, not newly authored world content.
        return switch(key){
            case "journal_1"->new Place("cosmicdungeon:dungeon_1",new BlockPos(640,-60,60));
            case "journal_2"->new Place("cosmicdungeon:dungeon_1",new BlockPos(619,-1,119));
            case "journal_3"->new Place("cosmicdungeon:dungeon_1",new BlockPos(1637,98,4253));
            case "fire_start"->new Place("cosmicdungeon:dungeon_1_nether",new BlockPos(193,25,119));
            case "fire_end"->new Place("cosmicdungeon:dungeon_1_nether",new BlockPos(206,28,99));
            default->null;
        };
    }
    public boolean matches(ServerLevel level,BlockPos position,String key){
        var place=location(key);return place!=null&&place.pos().equals(position)
                &&place.dimension().equals(DungeonInstanceSlots.templateDimensionForPhysical(level.getServer(),level.dimension()).location().toString());
    }
    private static final Set<String> KEYS = Set.of("base_camp","stairway","journal_1","journal_2","journal_3","fire_start","fire_end");
    private static boolean validBlock(ServerLevel level, BlockPos pos, String key) {
        var entity = level.getBlockEntity(pos);
        if (key.equals("stairway")) return entity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity;
        if (key.startsWith("journal_")) return entity instanceof net.minecraft.world.level.block.entity.LecternBlockEntity;
        return true;
    }
    private static int status(CommandSourceStack source) {
        var data = get(source.getServer());
        for (String key : new TreeSet<>(KEYS)) {
            var place = data.location(key);
            if (place == null) {
                source.sendSuccess(() -> Component.literal(key + ": not bound"), false); continue;
            }
            var id = net.minecraft.resources.ResourceLocation.tryParse(place.dimension());
            var level = id == null ? null : source.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, id));
            String state = key.equals("stairway") && !place.dimension().equals("minecraft:overworld")
                    ? "legacy D1 binding retained; rebind the World Spawn chest"
                    : level == null || !level.hasChunkAt(place.pos()) ? "unloaded; block not inspected"
                    : validBlock(level, place.pos(), key) ? "loaded, expected block type" : "loaded, wrong block type";
            source.sendSuccess(() -> Component.literal(key + ": " + place.dimension() + " " + place.pos().toShortString()
                    + (data.locations.containsKey(key) ? " (configured; " : " (source default; ") + state + ")"), false);
        }
        return 1;
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("d1").then(Commands.literal("objective")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("bind").then(Commands.argument("key",com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((context,builder) -> SharedSuggestionProvider.suggest(new TreeSet<>(KEYS),builder))
                        .then(Commands.argument("position",BlockPosArgument.blockPos()).executes(c -> {
                            String key = com.mojang.brigadier.arguments.StringArgumentType.getString(c,"key");
                            if (!KEYS.contains(key)) return 0;
                            var source = c.getSource();
                            var dimension = DungeonInstanceSlots.templateDimensionForPhysical(source.getServer(),source.getLevel().dimension());
                            boolean allowed = key.equals("stairway")
                                    ? dimension.equals(net.minecraft.world.level.Level.OVERWORLD)
                                    : DungeonDefinitions.DUNGEON_1.containsDimension(dimension);
                            if (!allowed) {
                                source.sendFailure(Component.literal("Bind Stairway in World Spawn (Overworld); other objectives in D1.")); return 0;
                            }
                            var pos = BlockPosArgument.getLoadedBlockPos(c,"position");
                            if (!validBlock(source.getLevel(), pos, key)) {
                                source.sendFailure(Component.literal("Stairway needs a chest; a journal reading position needs a lectern.")); return 0;
                            }
                            var data = get(source.getServer());
                            data.locations.put(key,new Place(dimension.location().toString(),pos)); data.setDirty();
                            source.sendSuccess(() -> Component.literal("D1 " + key + " location saved."),true);
                            return 1;
                        }))))));
    }
}
