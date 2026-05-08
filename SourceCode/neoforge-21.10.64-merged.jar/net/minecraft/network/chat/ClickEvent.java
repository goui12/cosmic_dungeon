package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface ClickEvent {
    Codec<ClickEvent> CODEC = ClickEvent.Action.CODEC.dispatch("action", ClickEvent::action, p_392608_ -> p_392608_.codec);

    ClickEvent.Action action();

    public static enum Action implements StringRepresentable {
        OPEN_URL("open_url", true, ClickEvent.OpenUrl.CODEC),
        OPEN_FILE("open_file", false, ClickEvent.OpenFile.CODEC),
        RUN_COMMAND("run_command", true, ClickEvent.RunCommand.CODEC),
        SUGGEST_COMMAND("suggest_command", true, ClickEvent.SuggestCommand.CODEC),
        SHOW_DIALOG("show_dialog", true, ClickEvent.ShowDialog.CODEC),
        CHANGE_PAGE("change_page", true, ClickEvent.ChangePage.CODEC),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true, ClickEvent.CopyToClipboard.CODEC),
        CUSTOM("custom", true, ClickEvent.Custom.CODEC);

        public static final Codec<ClickEvent.Action> UNSAFE_CODEC = StringRepresentable.fromEnum(ClickEvent.Action::values);
        public static final Codec<ClickEvent.Action> CODEC = UNSAFE_CODEC.validate(ClickEvent.Action::filterForSerialization);
        private final boolean allowFromServer;
        /**
         * The canonical name used to refer to this action.
         */
        private final String name;
        final MapCodec<? extends ClickEvent> codec;

        private Action(String name, boolean allowFromServer, MapCodec<? extends ClickEvent> codec) {
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

        public MapCodec<? extends ClickEvent> valueCodec() {
            return this.codec;
        }

        public static DataResult<ClickEvent.Action> filterForSerialization(ClickEvent.Action action) {
            return !action.isAllowedFromServer() && (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() == null || net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) // Neo: Allow open file commands to work on integrated servers. PR #915
                ? DataResult.error(() -> "Click event type not allowed: " + action)
                : DataResult.success(action, Lifecycle.stable());
        }
    }

    public record ChangePage(int page) implements ClickEvent {
        public static final MapCodec<ClickEvent.ChangePage> CODEC = RecordCodecBuilder.mapCodec(
            p_394119_ -> p_394119_.group(ExtraCodecs.POSITIVE_INT.fieldOf("page").forGetter(ClickEvent.ChangePage::page))
                .apply(p_394119_, ClickEvent.ChangePage::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.CHANGE_PAGE;
        }
    }

    public record CopyToClipboard(String value) implements ClickEvent {
        public static final MapCodec<ClickEvent.CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec(
            p_394320_ -> p_394320_.group(Codec.STRING.fieldOf("value").forGetter(ClickEvent.CopyToClipboard::value))
                .apply(p_394320_, ClickEvent.CopyToClipboard::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.COPY_TO_CLIPBOARD;
        }
    }

    public record Custom(ResourceLocation id, Optional<Tag> payload) implements ClickEvent {
        public static final MapCodec<ClickEvent.Custom> CODEC = RecordCodecBuilder.mapCodec(
            p_428106_ -> p_428106_.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(ClickEvent.Custom::id),
                    ExtraCodecs.NBT.optionalFieldOf("payload").forGetter(ClickEvent.Custom::payload)
                )
                .apply(p_428106_, ClickEvent.Custom::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.CUSTOM;
        }
    }

    public record OpenFile(String path) implements ClickEvent {
        public static final MapCodec<ClickEvent.OpenFile> CODEC = RecordCodecBuilder.mapCodec(
            p_393988_ -> p_393988_.group(Codec.STRING.fieldOf("path").forGetter(ClickEvent.OpenFile::path)).apply(p_393988_, ClickEvent.OpenFile::new)
        );

        public OpenFile(File p_394020_) {
            this(p_394020_.toString());
        }

        public OpenFile(Path p_393758_) {
            this(p_393758_.toFile());
        }

        public File file() {
            return new File(this.path);
        }

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.OPEN_FILE;
        }
    }

    public record OpenUrl(URI uri) implements ClickEvent {
        public static final MapCodec<ClickEvent.OpenUrl> CODEC = RecordCodecBuilder.mapCodec(
            p_394254_ -> p_394254_.group(ExtraCodecs.UNTRUSTED_URI.fieldOf("url").forGetter(ClickEvent.OpenUrl::uri)).apply(p_394254_, ClickEvent.OpenUrl::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.OPEN_URL;
        }
    }

    public record RunCommand(String command) implements ClickEvent {
        public static final MapCodec<ClickEvent.RunCommand> CODEC = RecordCodecBuilder.mapCodec(
            p_394018_ -> p_394018_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.RunCommand::command))
                .apply(p_394018_, ClickEvent.RunCommand::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.RUN_COMMAND;
        }
    }

    public record ShowDialog(Holder<Dialog> dialog) implements ClickEvent {
        public static final MapCodec<ClickEvent.ShowDialog> CODEC = RecordCodecBuilder.mapCodec(
            p_425539_ -> p_425539_.group(Dialog.CODEC.fieldOf("dialog").forGetter(ClickEvent.ShowDialog::dialog)).apply(p_425539_, ClickEvent.ShowDialog::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.SHOW_DIALOG;
        }
    }

    public record SuggestCommand(String command) implements ClickEvent {
        public static final MapCodec<ClickEvent.SuggestCommand> CODEC = RecordCodecBuilder.mapCodec(
            p_394637_ -> p_394637_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.SuggestCommand::command))
                .apply(p_394637_, ClickEvent.SuggestCommand::new)
        );

        @Override
        public ClickEvent.Action action() {
            return ClickEvent.Action.SUGGEST_COMMAND;
        }
    }
}
