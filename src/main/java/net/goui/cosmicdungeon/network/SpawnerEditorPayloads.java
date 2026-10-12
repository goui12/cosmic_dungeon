package net.goui.cosmicdungeon.network;

import java.util.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.*;
import net.minecraft.world.item.ItemStack;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorDraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Small typed edit requests; detailed hover data travels only after fresh server authorization. */
public final class SpawnerEditorPayloads {
    private SpawnerEditorPayloads() {}
    public enum Kind { APPLY, CANCEL }
    public enum Status { OPEN, APPLIED, INVALID, CLOSED }
    public enum LibraryKind { LIST, LOAD, SAVE, OVERWRITE, DELETE, BIND, INSPECT }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path));
    }
    private static void write(RegistryFriendlyByteBuf b, SpawnerEditorDraft d) {
        b.writeUtf(d.entityId(),256); b.writeUtf(d.name(),128);
        b.writeBoolean(d.captain()); b.writeBoolean(d.boss()); b.writeVarInt(d.cap());
        b.writeVarInt(d.minDelay()); b.writeVarInt(d.maxDelay());
        b.writeBoolean(d.silent()); b.writeBoolean(d.noAi()); b.writeBoolean(d.invulnerable());
        b.writeBoolean(d.persistent()); b.writeBoolean(d.nameVisible());
    }
    private static SpawnerEditorDraft read(RegistryFriendlyByteBuf b) {
        return new SpawnerEditorDraft(b.readUtf(256),b.readUtf(128),b.readBoolean(),b.readBoolean(),b.readVarInt(),
                b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean());
    }
    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf b){
        var result=new ArrayList<ItemStack>();for(int i=0;i<6;i++)result.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));return result;
    }
    private static void writeEquipment(RegistryFriendlyByteBuf b,SpawnerEquipmentDraft draft){
        for(var edit:draft.slots()){
            b.writeEnum(edit.origin());b.writeUtf(edit.itemId(),256);b.writeBoolean(edit.advanced());
            b.writeVarInt(edit.enchantments().size());for(var enchant:edit.enchantments()){b.writeUtf(enchant.id(),256);b.writeVarInt(enchant.level());}
        }
    }
    private static SpawnerEquipmentDraft readEquipment(RegistryFriendlyByteBuf b){
        var slots=new ArrayList<Edit>();
        for(int i=0;i<6;i++){
            var origin=b.readEnum(Origin.class);var item=b.readUtf(256);var advanced=b.readBoolean();int count=b.readVarInt();
            if(count<0||count>SpawnerEquipmentDraft.MAX_ENCHANTMENTS)throw new IllegalArgumentException("Too many enchantment edits.");
            var enchantments=new ArrayList<Enchant>();for(int j=0;j<count;j++)enchantments.add(new Enchant(b.readUtf(256),b.readVarInt()));
            slots.add(new Edit(origin,item,advanced,enchantments));
        }
        return new SpawnerEquipmentDraft(slots);
    }
    public record Request(UUID session, Kind kind, SpawnerEditorDraft draft, SpawnerEquipmentDraft equipment,SpawnerLootDraft loot) implements CustomPacketPayload {
        public Request(UUID session,Kind kind,SpawnerEditorDraft draft){this(session,kind,draft,SpawnerEquipmentDraft.unchanged(),SpawnerLootDraft.unchanged());}
        public Request(UUID session,Kind kind,SpawnerEditorDraft draft,SpawnerEquipmentDraft equipment){this(session,kind,draft,equipment,SpawnerLootDraft.unchanged());}
        public static final Type<Request> TYPE = id("spawner_editor_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> STREAM_CODEC = StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeEnum(p.kind);write(b,p.draft);writeEquipment(b,p.equipment);writeLoot(b,p.loot);},
                b->new Request(b.readUUID(),b.readEnum(Kind.class),read(b),readEquipment(b),readLoot(b)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record View(UUID session, BlockPos pos, SpawnerEditorDraft draft, Status status, String message,
            List<ItemStack> equipment,ItemStack held,SpawnerLootDraft.Snapshot loot,List<String> presetNames,List<String> shortcuts,boolean presetLoaded) implements CustomPacketPayload {
        public View {
            if(equipment.size()!=6)throw new IllegalArgumentException("Six equipment slots required.");
            equipment=equipment.stream().map(ItemStack::copy).toList();held=held.copy();presetNames=List.copyOf(presetNames);shortcuts=List.copyOf(shortcuts);
        }
        public View(UUID session,BlockPos pos,SpawnerEditorDraft draft,Status status,String message,List<ItemStack> equipment,ItemStack held){
            this(session,pos,draft,status,message,equipment,held,SpawnerLootDraft.Snapshot.empty(),List.of(),Collections.nCopies(5,""),false);
        }
        public View(UUID session,BlockPos pos,SpawnerEditorDraft draft,Status status,String message){
            this(session,pos,draft,status,message,Collections.nCopies(6,ItemStack.EMPTY),ItemStack.EMPTY);
        }
        @Override public List<ItemStack> equipment(){return equipment.stream().map(ItemStack::copy).toList();}
        @Override public ItemStack held(){return held.copy();}
        public static final Type<View> TYPE = id("spawner_editor_view");
        public static final StreamCodec<RegistryFriendlyByteBuf,View> STREAM_CODEC = StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeBlockPos(p.pos);write(b,p.draft);b.writeEnum(p.status);b.writeUtf(p.message,256);for(var stack:p.equipment)ItemStack.OPTIONAL_STREAM_CODEC.encode(b,stack);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.held);writeSnapshot(b,p.loot);writeNames(b,p.presetNames);writeNames(b,p.shortcuts);b.writeBoolean(p.presetLoaded);},
                b->new View(b.readUUID(),b.readBlockPos(),read(b),b.readEnum(Status.class),b.readUtf(256),readStacks(b),ItemStack.OPTIONAL_STREAM_CODEC.decode(b),readSnapshot(b),readNames(b),readNames(b),b.readBoolean()));
        @Override public Type<View> type() { return TYPE; }
    }
    private static void writeNames(RegistryFriendlyByteBuf b,List<String> names){
        b.writeVarInt(names.size());for(String name:names)b.writeUtf(name,64);
    }
    private static int size(RegistryFriendlyByteBuf b,int max){
        int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("Editor collection too large.");return n;
    }
    private static List<String> readNames(RegistryFriendlyByteBuf b){
        int n=size(b,256);var values=new ArrayList<String>();for(int i=0;i<n;i++)values.add(b.readUtf(64));return values;
    }
    private static void writeRules(RegistryFriendlyByteBuf b,List<SpawnerLootDraft.Rule> rules){
        b.writeVarInt(rules.size());for(var r:rules){b.writeUtf(r.id(),64);b.writeUtf(r.itemId(),256);b.writeFloat(r.chance());b.writeVarInt(r.count());b.writeEnum(r.kind());}
    }
    private static List<SpawnerLootDraft.Rule> readRules(RegistryFriendlyByteBuf b){
        int n=size(b,SpawnerLootDraft.MAX_RULES);var values=new ArrayList<SpawnerLootDraft.Rule>();
        for(int i=0;i<n;i++)values.add(new SpawnerLootDraft.Rule(b.readUtf(64),b.readUtf(256),b.readFloat(),b.readVarInt(),b.readEnum(IntrinsicDropRule.Kind.class)));return values;
    }
    private static void writeLoot(RegistryFriendlyByteBuf b,SpawnerLootDraft d){
        for(float f:d.gear())b.writeFloat(f);b.writeBoolean(d.replaceRules());writeRules(b,d.rules());
    }
    private static SpawnerLootDraft readLoot(RegistryFriendlyByteBuf b){
        var gear=new ArrayList<Float>();for(int i=0;i<6;i++)gear.add(b.readFloat());return new SpawnerLootDraft(gear,b.readBoolean(),readRules(b));
    }
    private static void writeSnapshot(RegistryFriendlyByteBuf b,SpawnerLootDraft.Snapshot d){
        for(float f:d.gear())b.writeFloat(f);writeRules(b,d.rules());b.writeVarInt(d.defaults().size());
        for(var row:d.defaults()){b.writeUtf(row.itemId(),256);b.writeUtf(row.chanceLabel(),256);}
        b.writeUtf(d.note(),1024);b.writeUtf(d.entityId(),256);
    }
    private static SpawnerLootDraft.Snapshot readSnapshot(RegistryFriendlyByteBuf b){
        var gear=new ArrayList<Float>();for(int i=0;i<6;i++)gear.add(b.readFloat());var rules=readRules(b);int n=size(b,256);
        var defaults=new ArrayList<SpawnerLootDraft.DefaultDrop>();
        for(int i=0;i<n;i++)defaults.add(new SpawnerLootDraft.DefaultDrop(b.readUtf(256),b.readUtf(256)));
        return new SpawnerLootDraft.Snapshot(gear,rules,defaults,b.readUtf(1024),b.readUtf(256));
    }
    public record LibraryRequest(UUID session,UUID operation,LibraryKind kind,String name,int shortcut,
            SpawnerEditorDraft draft,SpawnerEquipmentDraft equipment,SpawnerLootDraft loot) implements CustomPacketPayload {
        public static final Type<LibraryRequest> TYPE=id("spawner_library_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,LibraryRequest> STREAM_CODEC=StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeUUID(p.operation);b.writeEnum(p.kind);b.writeUtf(p.name,64);b.writeVarInt(p.shortcut);write(b,p.draft);writeEquipment(b,p.equipment);writeLoot(b,p.loot);},
                b->new LibraryRequest(b.readUUID(),b.readUUID(),b.readEnum(LibraryKind.class),b.readUtf(64),b.readVarInt(),read(b),readEquipment(b),readLoot(b)));
        @Override public Type<LibraryRequest> type(){return TYPE;}
    }
    public record LibraryView(UUID session,UUID operation,boolean success,String message,List<String> names,List<String> shortcuts,SpawnerLootDraft.Snapshot loot) implements CustomPacketPayload {
        public LibraryView {names=List.copyOf(names);shortcuts=List.copyOf(shortcuts);}
        public static final Type<LibraryView> TYPE=id("spawner_library_view");
        public static final StreamCodec<RegistryFriendlyByteBuf,LibraryView> STREAM_CODEC=StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeUUID(p.operation);b.writeBoolean(p.success);b.writeUtf(p.message,256);writeNames(b,p.names);writeNames(b,p.shortcuts);writeSnapshot(b,p.loot);},
                b->new LibraryView(b.readUUID(),b.readUUID(),b.readBoolean(),b.readUtf(256),readNames(b),readNames(b),readSnapshot(b)));
        @Override public Type<LibraryView> type(){return TYPE;}
    }
    public record HoverRequest(BlockPos pos) implements CustomPacketPayload {
        public static final Type<HoverRequest> TYPE = id("spawner_hover_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,HoverRequest> STREAM_CODEC =
                StreamCodec.of((b,p)->b.writeBlockPos(p.pos),b->new HoverRequest(b.readBlockPos()));
        @Override public Type<HoverRequest> type() { return TYPE; }
    }
    public record HoverView(BlockPos pos, CompoundTag details) implements CustomPacketPayload {
        public HoverView { details = details.copy(); }
        @Override public CompoundTag details() { return details.copy(); }
        public static final Type<HoverView> TYPE = id("spawner_hover_view");
        public static final StreamCodec<RegistryFriendlyByteBuf,HoverView> STREAM_CODEC =
                StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeNbt(p.details);},
                        b->new HoverView(b.readBlockPos(),java.util.Objects.requireNonNull(b.readNbt())));
        @Override public Type<HoverView> type() { return TYPE; }
    }
}
