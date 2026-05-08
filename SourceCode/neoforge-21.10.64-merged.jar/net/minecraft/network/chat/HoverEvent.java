package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public interface HoverEvent {
    Codec<HoverEvent> CODEC = HoverEvent.Action.CODEC.dispatch("action", HoverEvent::action, p_392615_ -> p_392615_.codec);

    HoverEvent.Action action();

    public static enum Action implements StringRepresentable {
        SHOW_TEXT("show_text", true, HoverEvent.ShowText.CODEC),
        SHOW_ITEM("show_item", true, HoverEvent.ShowItem.CODEC),
        SHOW_ENTITY("show_entity", true, HoverEvent.ShowEntity.CODEC);

        public static final Codec<HoverEvent.Action> UNSAFE_CODEC = StringRepresentable.fromValues(HoverEvent.Action::values);
        public static final Codec<HoverEvent.Action> CODEC = UNSAFE_CODEC.validate(HoverEvent.Action::filterForSerialization);
        private final String name;
        private final boolean allowFromServer;
        final MapCodec<? extends HoverEvent> codec;

        private Action(String name, boolean allowFromServer, MapCodec<? extends HoverEvent> codec) {
            this.name = name;
            this.allowFromServer = allowFromServer;
            this.codec = codec;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Override
        public String toString() {
            return "<action " + this.name + ">";
        }

        private static DataResult<HoverEvent.Action> filterForSerialization(HoverEvent.Action action) {
            return !action.isAllowedFromServer()
                ? DataResult.error(() -> "Action not allowed: " + action)
                : DataResult.success(action, Lifecycle.stable());
        }
    }

    public static class EntityTooltipInfo {
        public static final MapCodec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.mapCodec(
            p_392616_ -> p_392616_.group(
                    BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id").forGetter(p_304417_ -> p_304417_.type),
                    UUIDUtil.LENIENT_CODEC.fieldOf("uuid").forGetter(p_392617_ -> p_392617_.uuid),
                    ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(p_304585_ -> p_304585_.name)
                )
                .apply(p_392616_, HoverEvent.EntityTooltipInfo::new)
        );
        public final EntityType<?> type;
        public final UUID uuid;
        public final Optional<Component> name;
        @Nullable
        private List<Component> linesCache;

        public EntityTooltipInfo(EntityType<?> type, UUID id, @Nullable Component name) {
            this(type, id, Optional.ofNullable(name));
        }

        public EntityTooltipInfo(EntityType<?> type, UUID id, Optional<Component> name) {
            this.type = type;
            this.uuid = id;
            this.name = name;
        }

        public List<Component> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = new ArrayList<>();
                this.name.ifPresent(this.linesCache::add);
                this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(Component.literal(this.uuid.toString()));
            }

            return this.linesCache;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other != null && this.getClass() == other.getClass()) {
                HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = (HoverEvent.EntityTooltipInfo)other;
                return this.type.equals(hoverevent$entitytooltipinfo.type)
                    && this.uuid.equals(hoverevent$entitytooltipinfo.uuid)
                    && this.name.equals(hoverevent$entitytooltipinfo.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.type.hashCode();
            i = 31 * i + this.uuid.hashCode();
            return 31 * i + this.name.hashCode();
        }
    }

    public record ShowEntity(HoverEvent.EntityTooltipInfo entity) implements HoverEvent {
        public static final MapCodec<HoverEvent.ShowEntity> CODEC = RecordCodecBuilder.mapCodec(
            p_394451_ -> p_394451_.group(HoverEvent.EntityTooltipInfo.CODEC.forGetter(HoverEvent.ShowEntity::entity))
                .apply(p_394451_, HoverEvent.ShowEntity::new)
        );

        @Override
        public HoverEvent.Action action() {
            return HoverEvent.Action.SHOW_ENTITY;
        }
    }

    public record ShowItem(ItemStack item) implements HoverEvent {
        public static final MapCodec<HoverEvent.ShowItem> CODEC = ItemStack.MAP_CODEC.xmap(HoverEvent.ShowItem::new, HoverEvent.ShowItem::item);

        public ShowItem(ItemStack item) {
            item = item.copy();
            this.item = item;
        }

        @Override
        public HoverEvent.Action action() {
            return HoverEvent.Action.SHOW_ITEM;
        }

        @Override
        public boolean equals(Object p_394568_) {
            return p_394568_ instanceof HoverEvent.ShowItem hoverevent$showitem && ItemStack.matches(this.item, hoverevent$showitem.item);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.item);
        }
    }

    public record ShowText(Component value) implements HoverEvent {
        public static final MapCodec<HoverEvent.ShowText> CODEC = RecordCodecBuilder.mapCodec(
            p_393562_ -> p_393562_.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(HoverEvent.ShowText::value))
                .apply(p_393562_, HoverEvent.ShowText::new)
        );

        @Override
        public HoverEvent.Action action() {
            return HoverEvent.Action.SHOW_TEXT;
        }
    }
}
