package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Client presentation only; the server validates every action and owns the roster. */
final class D1PartyPanel {
    private PartyPayloads.View view;
    private String inviteName = "", groupName = "";
    private boolean recruiting;
    void setView(PartyPayloads.View view) {
        this.view = view;
        if (view.members().isEmpty()) recruiting = false;
    }
    private void send(int containerId, String action, String target) {
        ModNetwork.sendToServer(new PartyPayloads.Action(containerId, view == null ? 0 : view.state().revision(), action, target));
    }
    private static void button(Consumer<AbstractWidget> add, String title, int x, int y, int width, boolean enabled, Runnable action) {
        var button = Button.builder(Component.literal(title), ignored -> action.run()).bounds(x, y, width, 20).build();
        button.active = enabled; add.accept(button);
    }
    void build(Font font, Consumer<AbstractWidget> add, Runnable rebuild, int x, int y, int containerId) {
        if (view == null) return;
        var state = view.state();
        var social = view.recruitment();
        boolean grouped = !view.members().isEmpty();
        boolean preparing = state.phase().equals("PREPARING");
        if (recruiting && grouped) {
            button(add, "Back", x + 290, y + 32, 60, true, () -> { recruiting = false; rebuild.run(); });
            int row = 0;
            for (var candidate : social.candidates()) {
                button(add, "Invite", x + 280, y + 64 + row++ * 24, 70, view.invitation().canInvite(),
                        () -> send(containerId, "invite", candidate.name()));
            }
            button(add, "Previous", x + 10, y + 166, 100, social.page() > 0, () -> send(containerId, "lfg_prev", ""));
            button(add, "Next", x + 250, y + 166, 100, social.page() + 1 < social.pages(), () -> send(containerId, "lfg_next", ""));
            if (view.invitation().canInvite()) {
                var field = new EditBox(font, x + 10, y + 210, 220, 20, Component.literal("Friend's player name"));
                field.setMaxLength(16); field.setValue(inviteName); field.setResponder(value -> inviteName = value);
                add.accept(field);
                button(add, "Invite friend", x + 238, y + 210, 112, true,
                        () -> send(containerId, "invite", field.getValue().trim()));
            }
            return;
        }
        if (!grouped) {
            var field = new EditBox(font, x + 10, y + 67, 220, 20, Component.literal("Group name"));
            field.setMaxLength(32); field.setValue(groupName); field.setResponder(value -> groupName = value);
            add.accept(field);
            button(add, "Create group", x + 238, y + 67, 112, view.invitation().token().isEmpty(),
                    () -> send(containerId, "create", field.getValue().strip()));
            button(add, social.looking() ? "Stop looking for group" : "Looking for group", x + 10, y + 116, 340, true,
                    () -> send(containerId, "lfg", ""));
            button(add, "Change class", x + 10, y + 152, 340, true, () -> send(containerId, "class", ""));
        } else {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            var ready = D1PartyPresentation.readiness(view, player == null ? null : player.getGameProfile().name());
            boolean allReady = view.members().stream().allMatch(PartyPayloads.Member::ready);
            button(add, ready.label(), x + 10, y + 144, 162, ready.enabled(), () -> send(containerId, ready.action(), ""));
            button(add, "Start Adventure!", x + 178, y + 144, 172,
                    state.leader() && state.phase().equals("READY_CHECK") && allReady, () -> send(containerId, "queue", ""));
            button(add, "Begin ready check", x + 10, y + 170, 162,
                    state.leader() && state.phase().equals("ASSEMBLY"), () -> send(containerId, "begin", ""));
            button(add, "Class", x + 178, y + 170, 66, !preparing, () -> send(containerId, "class", ""));
            button(add, state.leader() ? "Disband" : "Leave group", x + 250, y + 170, 100, !preparing,
                    () -> send(containerId, "leave", ""));
            button(add, "Recruit / Invite friends", x + 10, y + 210, 340, !preparing,
                    () -> { recruiting = true; rebuild.run(); });
        }
        var invite = view.invitation();
        if (!invite.token().isEmpty()) {
            button(add, invite.accepted() ? "Accepted" : "Accept invite", x + 10, y + 210, 162, !invite.accepted(),
                    () -> send(containerId, "accept", invite.token()));
            button(add, "Decline", x + 178, y + 210, 172, true,
                    () -> send(containerId, "decline", invite.token()));
        }
    }
    void render(GuiGraphics graphics, Font font, int x, int y) {
        if (view == null) { graphics.drawString(font, "Loading group...", x + 10, y + 37, 0xFFFFFFFF, false); return; }
        var state = view.state();
        var social = view.recruitment();
        if (recruiting && !view.members().isEmpty()) {
            graphics.drawString(font, "Looking for group", x + 10, y + 36, 0xFFFFFFAA, false);
            int row = 0;
            for (var candidate : social.candidates()) {
                String label = candidate.name() + " / " + ClassSelectorScreen.className(candidate.classId()).getString();
                graphics.drawString(font, font.plainSubstrByWidth(label, 258), x + 10, y + 70 + row++ * 24, 0xFFFFFFFF, false);
            }
            if (social.candidates().isEmpty())
                graphics.drawString(font, "No players currently advertising here.", x + 10, y + 73, 0xFFBBBBBB, false);
            graphics.drawString(font, (social.page() + 1) + " / " + social.pages(), x + 163, y + 172, 0xFFBBBBBB, false);
            graphics.drawString(font, view.invitation().canInvite() ? "Invite any online friend by player name:"
                    : "Group full or queued. Cancel queueing to recruit.", x + 10, y + 198, 0xFFBBBBBB, false);
            return;
        }
        if (view.members().isEmpty()) {
            graphics.drawString(font, "Create a group or advertise your class", x + 10, y + 34, 0xFFFFFFAA, false);
            graphics.drawString(font, "Group name:", x + 10, y + 53, 0xFFBBBBBB, false);
            graphics.drawString(font, social.looking() ? "Your selected class is listed for recruiters."
                    : "Looking for group lists your selected class.", x + 10, y + 99, 0xFFBBBBBB, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(social.groupName(), 340), x + 10, y + 32, 0xFFFFFFAA, false);
            graphics.drawString(font, "Members: " + view.members().size() + "/" + state.capacity(), x + 10, y + 46, 0xFFBBBBBB, false);
            int rowY = y + 60;
            for (var member : view.members()) {
                String row = (member.leader() ? "* " : "  ") + member.name() + " / "
                        + ClassSelectorScreen.className(member.classId()).getString() + (member.ready() ? " / Ready" : "");
                graphics.drawString(font, font.plainSubstrByWidth(row, 340), x + 10, rowY,
                        member.ready() ? 0xFFAAFFAA : 0xFFFFFFFF, false);
                rowY += 11;
            }
            String status = switch (state.phase()) {
                case "QUEUED" -> state.countdownSeconds() < 0 ? "Queue " + state.queuePosition() + ": waiting for an instance."
                        : "Entry in " + state.countdownSeconds() + " seconds.";
                case "READY_CHECK" -> "Everyone confirms; the leader starts.";
                case "PREPARING" -> "Preparing your instance";
                default -> "Leader begins; everyone confirms personally.";
            };
            graphics.drawString(font, status, x + 10, y + 130, 0xFF90CAF9, false);
        }
        if (!view.invitation().token().isEmpty()) {
            graphics.drawString(font, "Invitation from " + view.invitation().inviter(), x + 10, y + 183, 0xFFFFFFAA, false);
            if (view.invitation().accepted())
                graphics.drawString(font, "Finish setup near Tamsin to join.", x + 10, y + 195, 0xFFBBBBBB, false);
        }
    }
}
