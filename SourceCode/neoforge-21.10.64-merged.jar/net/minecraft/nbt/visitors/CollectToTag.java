package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;

public class CollectToTag implements StreamTagVisitor {
    private final Deque<CollectToTag.ContainerBuilder> containerStack = new ArrayDeque<>();

    public CollectToTag() {
        this.containerStack.addLast(new CollectToTag.RootBuilder());
    }

    @Nullable
    public Tag getResult() {
        return this.containerStack.getFirst().build();
    }

    protected int depth() {
        return this.containerStack.size() - 1;
    }

    private void appendEntry(Tag tag) {
        this.containerStack.getLast().acceptValue(tag);
    }

    @Override
    public StreamTagVisitor.ValueResult visitEnd() {
        this.appendEntry(EndTag.INSTANCE);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(String entry) {
        this.appendEntry(StringTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte entry) {
        this.appendEntry(ByteTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(short entry) {
        this.appendEntry(ShortTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int entry) {
        this.appendEntry(IntTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long entry) {
        this.appendEntry(LongTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(float entry) {
        this.appendEntry(FloatTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(double entry) {
        this.appendEntry(DoubleTag.valueOf(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte[] entry) {
        this.appendEntry(new ByteArrayTag(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int[] entry) {
        this.appendEntry(new IntArrayTag(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long[] entry) {
        this.appendEntry(new LongArrayTag(entry));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitList(TagType<?> type, int size) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.EntryResult visitElement(TagType<?> type, int size) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String id) {
        this.containerStack.getLast().acceptKey(id);
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    private void enterContainerIfNeeded(TagType<?> type) {
        if (type == ListTag.TYPE) {
            this.containerStack.addLast(new CollectToTag.ListBuilder());
        } else if (type == CompoundTag.TYPE) {
            this.containerStack.addLast(new CollectToTag.CompoundBuilder());
        }
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        CollectToTag.ContainerBuilder collecttotag$containerbuilder = this.containerStack.removeLast();
        Tag tag = collecttotag$containerbuilder.build();
        if (tag != null) {
            this.containerStack.getLast().acceptValue(tag);
        }

        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> type) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    static class CompoundBuilder implements CollectToTag.ContainerBuilder {
        private final CompoundTag compound = new CompoundTag();
        private String lastId = "";

        @Override
        public void acceptKey(String p_410576_) {
            this.lastId = p_410576_;
        }

        @Override
        public void acceptValue(Tag p_410777_) {
            this.compound.put(this.lastId, p_410777_);
        }

        @Override
        public Tag build() {
            return this.compound;
        }
    }

    interface ContainerBuilder {
        default void acceptKey(String lastId) {
        }

        void acceptValue(Tag tag);

        @Nullable
        Tag build();
    }

    static class ListBuilder implements CollectToTag.ContainerBuilder {
        private final ListTag list = new ListTag();

        @Override
        public void acceptValue(Tag p_410836_) {
            this.list.addAndUnwrap(p_410836_);
        }

        @Override
        public Tag build() {
            return this.list;
        }
    }

    static class RootBuilder implements CollectToTag.ContainerBuilder {
        @Nullable
        private Tag result;

        @Override
        public void acceptValue(Tag p_410356_) {
            this.result = p_410356_;
        }

        @Nullable
        @Override
        public Tag build() {
            return this.result;
        }
    }
}
