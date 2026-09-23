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
    private String inviteName = "";
    void setView(PartyPayloads.View view) { this.view = view; }
    private void send(int containerId, String action, String target) {
        ModNetwork.sendToServer(new PartyPayloads.Action(containerId, view == null ? 0 : view.state().revision(), action, target));
    }
    private static void button(Consumer<AbstractWidget> add, String title, int x, int y, int width, boolean enabled, Runnable action) {
        var button = Button.builder(Component.literal(title), ignored -> action.run()).bounds(x, y, width, 20).build();
        button.active = enabled; add.accept(button);
    }
    void build(Font font, Consumer<AbstractWidget> add, int x, int y, int containerId) {
        if (view == null) return;
        var state = view.state();
        String phase = state.phase();
        boolean assembly = phase.equals("ASSEMBLY"), checking = phase.equals("READY_CHECK");
        boolean queued = phase.equals("QUEUED"), preparing = phase.equals("PREPARING");
        boolean grouped = !phase.equals("UNGROUPED");
        boolean allReady = !view.members().isEmpty() && view.members().stream().allMatch(PartyPayloads.Member::ready);
        button(add, assembly ? "Begin ready check" : checking ? "I'm ready" : "Waiting",
                x + 10, y + 144, 130, (assembly && state.leader()) || checking,
                () -> send(containerId, assembly ? "begin" : "ready", ""));
        button(add, "Join queue", x + 145, y + 144, 98, state.leader() && checking && allReady,
                () -> send(containerId, "queue", ""));
        button(add, "Unready", x + 248, y + 144, 102, grouped && !assembly && !preparing,
                () -> send(containerId, "unready", ""));
        button(add, state.leader() ? "Disband" : "Leave group", x + 248, y + 170, 102, grouped && !preparing,
                () -> send(containerId, "leave", ""));
        var invite = view.invitation();
        if (!invite.token().isEmpty()) {
            button(add, invite.accepted() ? "Accepted" : "Accept invite", x + 10, y + 210, 162, !invite.accepted(),
                    () -> send(containerId, "accept", invite.token()));
            button(add, "Decline", x + 178, y + 210, 172, true,
                    () -> send(containerId, "decline", invite.token()));
        } else if (invite.canInvite()) {
            var field = new EditBox(font, x + 10, y + 210, 220, 20, Component.literal("Player name"));
            field.setMaxLength(16); field.setValue(inviteName); field.setResponder(value -> inviteName = value);
            add.accept(field);
            button(add, "Invite", x + 238, y + 210, 112, true, () -> send(containerId, "invite", field.getValue().trim()));
        }
    }
    void render(GuiGraphics graphics, Font font, int x, int y) {
        if (view == null) { graphics.drawString(font, "Loading group...", x + 10, y + 37, 0xFFFFFFFF, false); return; }
        var state = view.state();
        String phase = switch(state.phase()) {
            case "ASSEMBLY" -> "Group assembly";
            case "READY_CHECK" -> "Personal ready check";
            case "QUEUED" -> "Queue position: " + state.queuePosition();
            case "PREPARING" -> "Preparing your instance";
            default -> "Invite players to form a group";
        };
        graphics.drawString(font, phase, x + 10, y + 32, 0xFFFFFFAA, false);
        graphics.drawString(font, "Members: " + view.members().size() + "/" + state.capacity(), x + 10, y + 46, 0xFFBBBBBB, false);
        int rowY = y + 60;
        for (var member : view.members()) {
            String row = (member.leader() ? "* " : "  ") + member.name() + " / " + member.classId()
                    + (member.ready() ? " / Ready" : "");
            graphics.drawString(font, font.plainSubstrByWidth(row, 340), x + 10, rowY,
                    member.ready() ? 0xFFAAFFAA : 0xFFFFFFFF, false);
            rowY += 11;
        }
        if (state.phase().equals("QUEUED"))
            graphics.drawString(font, state.countdownSeconds() < 0 ? "Waiting for an available instance."
                    : "Entry in " + state.countdownSeconds() + " seconds.", x + 10, y + 130, 0xFF90CAF9, false);
        else if (state.phase().equals("ASSEMBLY"))
            graphics.drawString(font, "Leader begins; everyone confirms personally.", x + 10, y + 130, 0xFFBBBBBB, false);
        if (!view.invitation().token().isEmpty()) {
            graphics.drawString(font, "Invitation from " + view.invitation().inviter(), x + 10, y + 183, 0xFFFFFFAA, false);
            if (view.invitation().accepted())
                graphics.drawString(font, "Finish setup near Tamsin to join.", x + 10, y + 195, 0xFFBBBBBB, false);
        } else if (view.invitation().canInvite()) {
            graphics.drawString(font, "Invite an online player by name:", x + 10, y + 198, 0xFFBBBBBB, false);
        } else if (state.phase().equals("UNGROUPED")) {
            graphics.drawString(font, "Speak with Tamsin to form a group.", x + 10, y + 198, 0xFFBBBBBB, false);
        }
    }
}
