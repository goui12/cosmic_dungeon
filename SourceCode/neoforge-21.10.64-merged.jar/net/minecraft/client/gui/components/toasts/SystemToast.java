package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int MARGIN = 10;
    private final SystemToast.SystemToastId id;
    private Component title;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private final int width;
    private boolean forceHide;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public SystemToast(SystemToast.SystemToastId id, Component title, @Nullable Component message) {
        this(
            id,
            title,
            nullToEmpty(message),
            Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(title), message == null ? 0 : Minecraft.getInstance().font.width(message)))
        );
    }

    public static SystemToast multiline(Minecraft minecraft, SystemToast.SystemToastId id, Component title, Component message) {
        Font font = minecraft.font;
        List<FormattedCharSequence> list = font.split(message, 200);
        int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
        return new SystemToast(id, title, list, i + 30);
    }

    private SystemToast(SystemToast.SystemToastId id, Component title, List<FormattedCharSequence> messageLines, int width) {
        this.id = id;
        this.title = title;
        this.messageLines = messageLines;
        this.width = width;
    }

    private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component message) {
        return message == null ? ImmutableList.of() : ImmutableList.of(message.getVisualOrderText());
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return 20 + Math.max(this.messageLines.size(), 1) * 12;
    }

    public void forceHide() {
        this.forceHide = true;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long visibilityTime) {
        if (this.changed) {
            this.lastChanged = visibilityTime;
            this.changed = false;
        }

        double d0 = this.id.displayTime * toastManager.getNotificationDisplayTimeMultiplier();
        long i = visibilityTime - this.lastChanged;
        this.wantedVisibility = !this.forceHide && i < d0 ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Font font, long visibilityTime) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        if (this.messageLines.isEmpty()) {
            guiGraphics.drawString(font, this.title, 18, 12, -256, false);
        } else {
            guiGraphics.drawString(font, this.title, 18, 7, -256, false);

            for (int i = 0; i < this.messageLines.size(); i++) {
                guiGraphics.drawString(font, this.messageLines.get(i), 18, 18 + i * 12, -1, false);
            }
        }
    }

    public void reset(Component title, @Nullable Component message) {
        this.title = title;
        this.messageLines = nullToEmpty(message);
        this.changed = true;
    }

    public SystemToast.SystemToastId getToken() {
        return this.id;
    }

    public static void add(ToastManager toastManager, SystemToast.SystemToastId id, Component title, @Nullable Component message) {
        toastManager.addToast(new SystemToast(id, title, message));
    }

    public static void addOrUpdate(ToastManager toastManager, SystemToast.SystemToastId id, Component title, @Nullable Component message) {
        SystemToast systemtoast = toastManager.getToast(SystemToast.class, id);
        if (systemtoast == null) {
            add(toastManager, id, title, message);
        } else {
            systemtoast.reset(title, message);
        }
    }

    public static void forceHide(ToastManager toastManager, SystemToast.SystemToastId id) {
        SystemToast systemtoast = toastManager.getToast(SystemToast.class, id);
        if (systemtoast != null) {
            systemtoast.forceHide();
        }
    }

    public static void onWorldAccessFailure(Minecraft minecraft, String message) {
        add(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
            Component.translatable("selectWorld.access_failure"),
            Component.literal(message)
        );
    }

    public static void onWorldDeleteFailure(Minecraft minecraft, String message) {
        add(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
            Component.translatable("selectWorld.delete_failure"),
            Component.literal(message)
        );
    }

    public static void onPackCopyFailure(Minecraft minecraft, String message) {
        add(minecraft.getToastManager(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(message));
    }

    public static void onFileDropFailure(Minecraft minecraft, int failedFileCount) {
        add(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.FILE_DROP_FAILURE,
            Component.translatable("gui.fileDropFailure.title"),
            Component.translatable("gui.fileDropFailure.detail", failedFileCount)
        );
    }

    public static void onLowDiskSpace(Minecraft minecraft) {
        addOrUpdate(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.LOW_DISK_SPACE,
            Component.translatable("chunk.toast.lowDiskSpace"),
            Component.translatable("chunk.toast.lowDiskSpace.description")
        );
    }

    public static void onChunkLoadFailure(Minecraft minecraft, ChunkPos chunkPos) {
        addOrUpdate(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.CHUNK_LOAD_FAILURE,
            Component.translatable("chunk.toast.loadFailure", Component.translationArg(chunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    public static void onChunkSaveFailure(Minecraft minecraft, ChunkPos chunkPos) {
        addOrUpdate(
            minecraft.getToastManager(),
            SystemToast.SystemToastId.CHUNK_SAVE_FAILURE,
            Component.translatable("chunk.toast.saveFailure", Component.translationArg(chunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    @OnlyIn(Dist.CLIENT)
    public static class SystemToastId {
        public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId FILE_DROP_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId LOW_DISK_SPACE = new SystemToast.SystemToastId(10000L);
        public static final SystemToast.SystemToastId CHUNK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId CHUNK_SAVE_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
        final long displayTime;

        public SystemToastId(long displayTime) {
            this.displayTime = displayTime;
        }

        public SystemToastId() {
            this(5000L);
        }
    }
}
