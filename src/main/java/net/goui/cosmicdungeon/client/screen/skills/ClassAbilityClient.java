package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.client.SkillInputPolicy;
import net.goui.cosmicdungeon.network.ClassSkillPayloads;
import net.goui.cosmicdungeon.network.ClassSkillPayloads.SlotView;
import net.goui.cosmicdungeon.network.ClassSkillPayloads.View;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Displays server capabilities. A slot never supplies gameplay authority or invents an ability. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class ClassAbilityClient {
    private static View view;
    private static Consumer<ClassSkillPayloads.Action> sender;
    private static boolean pending;
    private ClassAbilityClient() {}

    public static void actions(Consumer<ClassSkillPayloads.Action> callback) {
        sender = Objects.requireNonNull(callback);
    }
    public static void receive(View incoming) {
        Objects.requireNonNull(incoming);
        if (view != null && incoming.run() == view.run() && incoming.revision() < view.revision()) return;
        view = incoming;
        pending = false;
    }
    public static int level(String classId){
        return view==null||!view.classId().equals(classId)||view.tracks().isEmpty()?0:view.tracks().getFirst().level();
    }
    public static void clear() { view = null; pending = false; }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void clone(ClientPlayerNetworkEvent.Clone event) { clear(); }
    public static String actionId(ClassSkillSlot slot) { return SkillInputPolicy.Action.forSlot(slot).id; }
    private static boolean matches(View snapshot, String classId) {
        return snapshot != null && snapshot.classId().equals(classId);
    }
    private static SlotView unassigned(ClassSkillSlot slot) {
        return new SlotView(slot, "", slot.label(), "No ability is assigned to this slot.", false, 0, 0, 0);
    }
    /** Fill missing slots without carrying abilities across a class change or an absent snapshot. */
    public static List<SlotView> slots(View snapshot, String classId) {
        return Arrays.stream(ClassSkillSlot.values()).map(slot -> matches(snapshot, classId)
                ? snapshot.slots().stream().filter(value -> value.slot() == slot).findFirst().orElseGet(() -> unassigned(slot))
                : unassigned(slot)).toList();
    }
    public static Optional<ClassSkillPayloads.Action> request(View snapshot, String classId, ClassSkillSlot slot) {
        if (!matches(snapshot, classId) || snapshot.run() <= 0 || slot == null) return Optional.empty();
        var selected = slots(snapshot, classId).stream().filter(value -> value.slot() == slot).findFirst().orElseThrow();
        if (!selected.enabled() || selected.abilityId().isBlank() || selected.remainingTicks() > 0
                || selected.maxCharge() > 0 && selected.charge() < selected.maxCharge()) return Optional.empty();
        return Optional.of(new ClassSkillPayloads.Action(snapshot.run(), snapshot.revision(), classId, slot));
    }
    public static String unavailable(View snapshot, String classId, ClassSkillSlot slot) {
        var selected = slots(snapshot, classId).stream().filter(value -> value.slot() == slot).findFirst().orElseThrow();
        if (selected.abilityId().isBlank()) return slot.label() + " is not assigned.";
        if (!matches(snapshot, classId) || snapshot.run() <= 0) return "Class skills require an active dungeon.";
        if (selected.remainingTicks() > 0) return selected.title() + " is ready in "
                + ((selected.remainingTicks() + 19L) / 20) + " seconds.";
        if (selected.maxCharge() > 0 && selected.charge() < selected.maxCharge())
            return selected.title() + ": " + selected.charge() + "/" + selected.maxCharge() + " charge.";
        return selected.description().isBlank() ? selected.title() + " is unavailable." : selected.description();
    }
    public static SkillsPanelModel augment(SkillsPanelModel base) {
        if (!SkillInputPolicy.supportsClass("", base.classId())) return base;
        var actions = new ArrayList<>(base.actions());
        for (var slot : slots(view, base.classId())) {
            boolean enabled = sender != null && !pending && request(view, base.classId(), slot.slot()).isPresent();
            var description = Component.literal(slot.description()).withStyle(ChatFormatting.YELLOW);
            if (!enabled) description.append(Component.literal("\n" + (pending ? "Waiting for the server."
                    : unavailable(view, base.classId(), slot.slot()))).withStyle(ChatFormatting.GRAY));
            String title = slot.abilityId().isBlank() ? slot.slot().label()
                    : (slot.slot()==ClassSkillSlot.PRIMARY ? "Primary" : slot.slot().label())
                    + (slot.maxCharge()>0 ? " ["+slot.charge()+"/"+slot.maxCharge()+"]" : "") + ": " + slot.title();
            actions.add(new SkillsPanelModel.Action(actionId(slot.slot()), title, description, enabled));
        }
        String resource = base.resource();
        var tooltip = base.resourceTooltip().copy();
        if (matches(view, base.classId()) && !view.tracks().isEmpty()) {
            var track = view.tracks().getFirst();
            String summary = track.abbreviation() + " - " + track.level();
            boolean usefulResource = !resource.startsWith("Resource:");
            resource = summary + (usefulResource ? " | " + resource : "");
            String progress = track.next() == 0 ? "Maximum level." : track.progress() + "/" + track.next() + " to next level.";
            tooltip = Component.literal(track.title() + " - Level " + track.level() + "\n" + progress)
                    .withStyle(ChatFormatting.AQUA);
            if (usefulResource) tooltip.append(Component.literal("\n")).append(base.resourceTooltip().copy());
        }
        return new SkillsPanelModel(base.classId(), base.title(), resource, tooltip, actions, base.resourceSnapshot());
    }
    public static boolean activate(String id) {
        var action = SkillInputPolicy.Action.forId(id);
        return action != null && action.slot != null && activate(action.slot);
    }
    public static boolean activate(ClassSkillSlot slot) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || slot == null) return false;
        String classId = ClassData.getClassId(mc.player);
        var action = request(view, classId, slot);
        if (pending || sender == null || action.isEmpty()) {
            String reason = pending ? "Waiting for the server." : sender == null
                    ? "Class skills are not ready yet." : unavailable(view, classId, slot);
            mc.player.displayClientMessage(Component.literal(reason).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        pending = true;
        try { sender.accept(action.orElseThrow()); return true; }
        catch (RuntimeException failure) { pending = false; throw failure; }
    }
}
