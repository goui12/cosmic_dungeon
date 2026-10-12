package net.goui.cosmicdungeon.network;

import java.util.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft;
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
    public record Request(UUID session, Kind kind, SpawnerEditorDraft draft, SpawnerEquipmentDraft equipment) implements CustomPacketPayload {
        public Request(UUID session,Kind kind,SpawnerEditorDraft draft){this(session,kind,draft,SpawnerEquipmentDraft.unchanged());}
        public static final Type<Request> TYPE = id("spawner_editor_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> STREAM_CODEC = StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeEnum(p.kind);write(b,p.draft);writeEquipment(b,p.equipment);},
                b->new Request(b.readUUID(),b.readEnum(Kind.class),read(b),readEquipment(b)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record View(UUID session, BlockPos pos, SpawnerEditorDraft draft, Status status, String message,
            List<ItemStack> equipment,ItemStack held) implements CustomPacketPayload {
        public View {
            if(equipment.size()!=6)throw new IllegalArgumentException("Six equipment slots required.");
            equipment=equipment.stream().map(ItemStack::copy).toList();held=held.copy();
        }
        public View(UUID session,BlockPos pos,SpawnerEditorDraft draft,Status status,String message){
            this(session,pos,draft,status,message,Collections.nCopies(6,ItemStack.EMPTY),ItemStack.EMPTY);
        }
        @Override public List<ItemStack> equipment(){return equipment.stream().map(ItemStack::copy).toList();}
        @Override public ItemStack held(){return held.copy();}
        public static final Type<View> TYPE = id("spawner_editor_view");
        public static final StreamCodec<RegistryFriendlyByteBuf,View> STREAM_CODEC = StreamCodec.of(
                (b,p)->{b.writeUUID(p.session);b.writeBlockPos(p.pos);write(b,p.draft);b.writeEnum(p.status);b.writeUtf(p.message,256);for(var stack:p.equipment)ItemStack.OPTIONAL_STREAM_CODEC.encode(b,stack);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.held);},
                b->new View(b.readUUID(),b.readBlockPos(),read(b),b.readEnum(Status.class),b.readUtf(256),readStacks(b),ItemStack.OPTIONAL_STREAM_CODEC.decode(b)));
        @Override public Type<View> type() { return TYPE; }
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
