package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;

@OnlyIn(Dist.CLIENT)
public class ToastManager {
    private static final int SLOT_COUNT = 5;
    private static final int ALL_SLOTS_OCCUPIED = -1;
    final Minecraft minecraft;
    private final List<ToastManager.ToastInstance<?>> visibleToasts = new ArrayList<>();
    private final BitSet occupiedSlots = new BitSet(5);
    private final Deque<Toast> queued = Queues.newArrayDeque();
    private final Set<SoundEvent> playedToastSounds = new HashSet<>();
    @Nullable
    private ToastManager.ToastInstance<NowPlayingToast> nowPlayingToast;

    public ToastManager(Minecraft minecraft, Options options) {
        this.minecraft = minecraft;
        if (options.showNowPlayingToast().get()) {
            this.createNowPlayingToast();
        }
    }

    public void update() {
        MutableBoolean mutableboolean = new MutableBoolean(false);
        this.visibleToasts.removeIf(p_392505_ -> {
            Toast.Visibility toast$visibility = p_392505_.visibility;
            p_392505_.update();
            if (p_392505_.visibility != toast$visibility && mutableboolean.isFalse()) {
                mutableboolean.setTrue();
                p_392505_.visibility.playSound(this.minecraft.getSoundManager());
            }

            if (p_392505_.hasFinishedRendering()) {
                this.occupiedSlots.clear(p_392505_.firstSlotIndex, p_392505_.firstSlotIndex + p_392505_.occupiedSlotCount);
                return true;
            } else {
                return false;
            }
        });
        if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
            this.queued.removeIf(p_392506_ -> {
                int i = p_392506_.occcupiedSlotCount();
                int j = this.findFreeSlotsIndex(i);
                if (j == -1) {
                    return false;
                } else {
                    this.visibleToasts.add(new ToastManager.ToastInstance<>(p_392506_, j, i));
                    this.occupiedSlots.set(j, j + i);
                    SoundEvent soundevent = p_392506_.getSoundEvent();
                    if (soundevent != null && this.playedToastSounds.add(soundevent)) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundevent, 1.0F, 1.0F));
                    }

                    return true;
                }
            });
        }

        this.playedToastSounds.clear();
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.update();
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!this.minecraft.options.hideGui) {
            int i = guiGraphics.guiWidth();
            if (!this.visibleToasts.isEmpty()) {
                guiGraphics.nextStratum();
            }

            for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
                toastinstance.render(guiGraphics, i);
            }

            if (this.minecraft.options.showNowPlayingToast().get()
                && this.nowPlayingToast != null
                && (this.minecraft.screen == null || !(this.minecraft.screen instanceof PauseScreen))) {
                this.nowPlayingToast.render(guiGraphics, i);
            }
        }
    }

    private int findFreeSlotsIndex(int slots) {
        if (this.freeSlotCount() >= slots) {
            int i = 0;

            for (int j = 0; j < 5; j++) {
                if (this.occupiedSlots.get(j)) {
                    i = 0;
                } else if (++i == slots) {
                    return j + 1 - i;
                }
            }
        }

        return -1;
    }

    private int freeSlotCount() {
        return 5 - this.occupiedSlots.cardinality();
    }

    @Nullable
    public <T extends Toast> T getToast(Class<? extends T> toastClass, Object token) {
        for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
            if (toastinstance != null
                && toastClass.isAssignableFrom(toastinstance.getToast().getClass())
                && toastinstance.getToast().getToken().equals(token)) {
                return (T)toastinstance.getToast();
            }
        }

        for (Toast toast : this.queued) {
            if (toastClass.isAssignableFrom(toast.getClass()) && toast.getToken().equals(token)) {
                return (T)toast;
            }
        }

        return null;
    }

    public void clear() {
        this.occupiedSlots.clear();
        this.visibleToasts.clear();
        this.queued.clear();
    }

    public void addToast(Toast toast) {
        if (net.neoforged.neoforge.client.ClientHooks.onToastAdd(toast)) return;
        this.queued.add(toast);
    }

    public void showNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.resetToast();
            this.nowPlayingToast.getToast().showToast(this.minecraft.options);
        }
    }

    public void hideNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.getToast().setWantedVisibility(Toast.Visibility.HIDE);
        }
    }

    public void createNowPlayingToast() {
        this.nowPlayingToast = new ToastManager.ToastInstance<>(new NowPlayingToast(), 0, 0);
    }

    public void removeNowPlayingToast() {
        this.nowPlayingToast = null;
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public double getNotificationDisplayTimeMultiplier() {
        return this.minecraft.options.notificationDisplayTime().get();
    }

    @OnlyIn(Dist.CLIENT)
    class ToastInstance<T extends Toast> {
        private static final long SLIDE_ANIMATION_DURATION_MS = 600L;
        private final T toast;
        final int firstSlotIndex;
        final int occupiedSlotCount;
        private long animationStartTime;
        private long becameFullyVisibleAt;
        Toast.Visibility visibility;
        private long fullyVisibleFor;
        private float visiblePortion;
        protected boolean hasFinishedRendering;

        ToastInstance(T toast, int firstSlotIndex, int occupiedSlotCount) {
            this.toast = toast;
            this.firstSlotIndex = firstSlotIndex;
            this.occupiedSlotCount = occupiedSlotCount;
            this.resetToast();
        }

        public T getToast() {
            return this.toast;
        }

        public void resetToast() {
            this.animationStartTime = -1L;
            this.becameFullyVisibleAt = -1L;
            this.visibility = Toast.Visibility.HIDE;
            this.fullyVisibleFor = 0L;
            this.visiblePortion = 0.0F;
            this.hasFinishedRendering = false;
        }

        public boolean hasFinishedRendering() {
            return this.hasFinishedRendering;
        }

        private void calculateVisiblePortion(long visibilityTime) {
            float f = Mth.clamp((float)(visibilityTime - this.animationStartTime) / 600.0F, 0.0F, 1.0F);
            f *= f;
            if (this.visibility == Toast.Visibility.HIDE) {
                this.visiblePortion = 1.0F - f;
            } else {
                this.visiblePortion = f;
            }
        }

        public void update() {
            long i = Util.getMillis();
            if (this.animationStartTime == -1L) {
                this.animationStartTime = i;
                this.visibility = Toast.Visibility.SHOW;
            }

            if (this.visibility == Toast.Visibility.SHOW && i - this.animationStartTime <= 600L) {
                this.becameFullyVisibleAt = i;
            }

            this.fullyVisibleFor = i - this.becameFullyVisibleAt;
            this.calculateVisiblePortion(i);
            this.toast.update(ToastManager.this, this.fullyVisibleFor);
            Toast.Visibility toast$visibility = this.toast.getWantedVisibility();
            if (toast$visibility != this.visibility) {
                this.animationStartTime = i - (int)((1.0F - this.visiblePortion) * 600.0F);
                this.visibility = toast$visibility;
            }

            boolean flag = this.hasFinishedRendering;
            this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && i - this.animationStartTime > 600L;
            if (this.hasFinishedRendering && !flag) {
                this.toast.onFinishedRendering();
            }
        }

        public void render(GuiGraphics guiGraphics, int guiWidth) {
            if (!this.hasFinishedRendering) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(this.toast.xPos(guiWidth, this.visiblePortion), this.toast.yPos(this.firstSlotIndex));
                this.toast.render(guiGraphics, ToastManager.this.minecraft.font, this.fullyVisibleFor);
                guiGraphics.pose().popMatrix();
            }
        }
    }
}
