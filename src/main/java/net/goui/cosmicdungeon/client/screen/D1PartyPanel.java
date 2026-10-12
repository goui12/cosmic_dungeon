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
    private boolean recruiting, mercenaries, sharing;
    void setView(PartyPayloads.View view) {
        if (this.view != null && !this.view.sharing().choice().equals(view.sharing().choice())) sharing = false;
        this.view = view;
        if (view.members().isEmpty()) { recruiting = false; mercenaries = false; sharing = false; }
    }
    private boolean consent() { return view != null && (sharing || D1PartyPresentation.needsConsent(view)); }
    private void send(int containerId, String action, String target) {
        ModNetwork.sendToServer(new PartyPayloads.Action(containerId, view == null ? 0 : view.state().revision(), action, target));
    }
    private static void button(Consumer<AbstractWidget> add, String title, int x, int y, int width, boolean enabled, Runnable action) {
        var button = Button.builder(Component.literal(title), ignored -> action.run()).bounds(x, y, width, 20).build();
        button.active = enabled; add.accept(button);
    }
    void build(Font font, Consumer<AbstractWidget> add, Runnable rebuild, int x, int y, int containerId,
               D1PartyPanelLayout box, String viewerName) {
        if (view == null) return;
        var state = view.state();
        var social = view.recruitment();
        boolean grouped = !view.members().isEmpty();
        boolean preparing = state.phase().equals("PREPARING");
        boolean mutable = !preparing && !state.phase().equals("QUEUED");
        if (grouped) {
            // Progression belongs to the entire group flow, including recruitment and mercenary pages.
            var ready = D1PartyPresentation.readiness(view, viewerName);
            boolean answered = !D1PartyPresentation.needsConsent(view);
            button(add, ready.label(), x + 10, y + box.progressionY(), box.halfWidth(), answered && ready.enabled(),
                    () -> send(containerId, ready.action(), ""));
            var forward = D1PartyPresentation.forward(view);
            button(add, forward.label(), x + box.rightColumn(), y + box.progressionY(), box.halfWidth(),
                    answered && forward.enabled(), () -> send(containerId, forward.action(), ""));
        }
        if (grouped && consent()) {
            button(add, "YES - contribute", x + 10, y + box.managementY(), box.halfWidth(), !preparing,
                    () -> send(containerId, "resources_yes", view.sharing().context()));
            button(add, "NO - keep materials", x + box.rightColumn(), y + box.managementY(), box.halfWidth(), !preparing,
                    () -> send(containerId, "resources_no", view.sharing().context()));
            if (!D1PartyPresentation.needsConsent(view))
                button(add, "Back", x + box.width() - 70, y + 32, 60, true, () -> { sharing = false; rebuild.run(); });
            return;
        }
        if (mercenaries && grouped) {
            button(add, "Group", x + box.width() - 70, y + 32, 60, true, () -> { mercenaries = false; rebuild.run(); });
            if (!view.hire().selectedClass().isEmpty())
                button(add, "Release " + view.hire().selectedClass(), x + 10, y + box.managementY(), box.innerWidth(),
                        mutable, () -> send(containerId, "release_hire", ""));
            else {
                int n = 0;
                for (String cls : net.goui.cosmicdungeon.mercenary.MercenaryContract.CLASSES) {
                    int col = n % 2, row = n++ / 2;
                    button(add, ClassSelectorScreen.className(cls).getString(), x + (col == 0 ? 10 : box.rightColumn()),
                            y + 76 + row * 24, box.halfWidth(), mutable && view.members().size() < state.capacity(),
                            () -> send(containerId, "hire", cls));
                }
            }
            return;
        }
        if (recruiting && grouped) {
            button(add, "Group", x + box.width() - 70, y + 32, 60, true, () -> { recruiting = false; rebuild.run(); });
            int row = 0;
            for (var candidate : social.candidates()) {
                button(add, "Invite", x + box.width() - 80, y + 60 + row++ * 22, 70, view.invitation().canInvite(),
                        () -> send(containerId, "invite", candidate.name()));
            }
            button(add, "Previous", x + 10, y + box.navigationY(), 85, social.page() > 0,
                    () -> send(containerId, "lfg_prev", ""));
            button(add, "Next", x + box.width() - 95, y + box.navigationY(), 85, social.page() + 1 < social.pages(),
                    () -> send(containerId, "lfg_next", ""));
            if (view.invitation().canInvite()) {
                var field = new EditBox(font, x + 10, y + box.managementY(), box.innerWidth() - 108, 20,
                        Component.literal("Friend's player name"));
                field.setMaxLength(16); field.setValue(inviteName); field.setResponder(value -> inviteName = value);
                field.setTooltip(Tooltip.create(Component.literal("Online friend's player name")));
                add.accept(field);
                button(add, "Invite friend", x + box.width() - 112, y + box.managementY(), 102, true,
                        () -> send(containerId, "invite", field.getValue().trim()));
            }
            return;
        }
        if (!grouped) {
            var field = new EditBox(font, x + 10, y + 67, box.innerWidth() - 118, 20, Component.literal("Group name"));
            field.setMaxLength(32); field.setValue(groupName); field.setResponder(value -> groupName = value);
            add.accept(field);
            button(add, "Create group", x + box.width() - 122, y + 67, 112, view.invitation().token().isEmpty(),
                    () -> send(containerId, "create", field.getValue().strip()));
            button(add, social.looking() ? "Stop looking for group" : "Looking for group", x + 10, y + 116,
                    box.innerWidth(), true, () -> send(containerId, "lfg", ""));
            button(add, "Change class", x + 10, y + 152, box.innerWidth(), true, () -> send(containerId, "class", ""));
        } else {
            if (state.leader()) {
                button(add, "-", x + box.width() - 68, y + 42, 26, mutable && state.capacity() > view.members().size(),
                        () -> send(containerId, "capacity", Integer.toString(state.capacity() - 1)));
                button(add, "+", x + box.width() - 36, y + 42, 26, mutable && state.capacity() < 6,
                        () -> send(containerId, "capacity", Integer.toString(state.capacity() + 1)));
            }
            button(add, "Class", x + box.thirdX(0), y + box.managementY(), box.thirdWidth(), !preparing,
                    () -> send(containerId, "class", ""));
            button(add, state.leader() ? "Disband" : "Leave group", x + box.thirdX(1), y + box.managementY(),
                    box.thirdWidth(), !preparing, () -> send(containerId, "leave", ""));
            var difficulty = net.goui.cosmicdungeon.dungeon.DungeonDifficulty.parse(view.difficulty())
                    .orElse(net.goui.cosmicdungeon.dungeon.DungeonDifficulty.HARD);
            button(add, "Mode: " + difficulty.title(), x + box.thirdX(2), y + box.managementY(), box.thirdWidth(),
                    state.leader() && mutable,
                    () -> send(containerId, "difficulty", net.goui.cosmicdungeon.dungeon.DungeonDifficulty.values()[(difficulty.ordinal() + 1) % 4].name()));
            button(add, "Recruit", x + box.thirdX(0), y + box.navigationY(), box.thirdWidth(), !preparing,
                    () -> { recruiting = true; rebuild.run(); });
            button(add, "Mercenary", x + box.thirdX(1), y + box.navigationY(), box.thirdWidth(), !preparing,
                    () -> { mercenaries = true; rebuild.run(); });
            button(add, "Resources", x + box.thirdX(2), y + box.navigationY(), box.thirdWidth(), !preparing,
                    () -> { sharing = true; rebuild.run(); });
        }
        var invite = view.invitation();
        if (!invite.token().isEmpty()) {
            button(add, invite.accepted() ? "Accepted" : "Accept invite", x + 10, y + box.progressionY(), box.halfWidth(),
                    !invite.accepted(), () -> send(containerId, "accept", invite.token()));
            button(add, "Decline", x + box.rightColumn(), y + box.progressionY(), box.halfWidth(), true,
                    () -> send(containerId, "decline", invite.token()));
        }
    }
    void render(GuiGraphics graphics, Font font, int x, int y, D1PartyPanelLayout box) {
        if (view == null) { graphics.drawString(font, "Loading group...", x + 10, y + 37, 0xFFFFFFFF, false); return; }
        var state = view.state();
        var social = view.recruitment();
        if (consent() && !view.members().isEmpty()) {
            graphics.drawString(font, "Contribute group resources?", x + 10, y + 34, 0xFFFFFFAA, false);
            int textY = y + 58;
            for (String paragraph : D1PartyPresentation.consentExplanation(view.sharing().choice())) {
                for (var line : font.split(Component.literal(paragraph), box.innerWidth())) {
                    graphics.drawString(font, line, x + 10, textY, 0xFFEEEEEE, false); textY += 11;
                }
                textY += 6;
            }
            return;
        }
        if (mercenaries && !view.members().isEmpty()) {
            graphics.drawString(font, "Hire one mercenary", x + 10, y + 36, 0xFFFFFFAA, false);
            graphics.drawString(font, font.plainSubstrByWidth(view.hire().price() + " Trace on start; uses one slot.", box.innerWidth()),
                    x + 10, y + 57, 0xFFBBBBBB, false);
            if (!view.hire().selectedClass().isEmpty())
                graphics.drawString(font, "Reserved: " + view.hire().selectedClass(), x + 10, y + 100, 0xFFFFFFFF, false);
            return;
        }
        if (recruiting && !view.members().isEmpty()) {
            graphics.drawString(font, "Looking for group", x + 10, y + 36, 0xFFFFFFAA, false);
            int row = 0;
            for (var candidate : social.candidates()) {
                String label = candidate.name() + " / " + ClassSelectorScreen.className(candidate.classId()).getString();
                graphics.drawString(font, font.plainSubstrByWidth(label, box.innerWidth() - 80), x + 10,
                        y + 66 + row++ * 22, 0xFFFFFFFF, false);
            }
            if (social.candidates().isEmpty())
                graphics.drawString(font, "No players currently advertising here.", x + 10, y + 73, 0xFFBBBBBB, false);
            graphics.drawCenteredString(font, (social.page() + 1) + " / " + social.pages(), x + box.width() / 2,
                    y + box.navigationY() + 6, 0xFFBBBBBB);
            return;
        }
        if (view.members().isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth("Create a group or advertise your class", box.innerWidth()),
                    x + 10, y + 34, 0xFFFFFFAA, false);
            graphics.drawString(font, "Group name:", x + 10, y + 53, 0xFFBBBBBB, false);
            graphics.drawString(font, font.plainSubstrByWidth(social.looking() ? "Your class is listed for recruiters."
                    : "Looking for group lists your class.", box.innerWidth()), x + 10, y + 99, 0xFFBBBBBB, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(social.groupName(), box.innerWidth()), x + 10, y + 32, 0xFFFFFFAA, false);
            graphics.drawString(font, "Members: " + view.members().size() + " / Maximum: " + state.capacity(),
                    x + 10, y + 46, 0xFFBBBBBB, false);
            int rowY = y + 64;
            for (var member : view.members()) {
                String row = (member.leader() ? "* " : "  ") + member.name() + " / "
                        + ClassSelectorScreen.className(member.classId()).getString() + D1PartyPresentation.readySuffix(view, member);
                graphics.drawString(font, font.plainSubstrByWidth(row, box.innerWidth()), x + 10, rowY,
                        D1PartyPresentation.showsReadiness(view, member) && member.ready() ? 0xFFAAFFAA : 0xFFFFFFFF, false);
                rowY += 11;
            }
            String status = switch (state.phase()) {
                case "QUEUED" -> state.countdownSeconds() < 0 ? "Queue " + state.queuePosition() + ": waiting for an instance."
                        : "Entry in " + state.countdownSeconds() + " seconds.";
                case "READY_CHECK" -> "Everyone confirms; the leader starts.";
                case "PREPARING" -> "Preparing your instance";
                default -> "Leader begins; everyone confirms personally.";
            };
            graphics.drawString(font, font.plainSubstrByWidth(status, box.innerWidth()), x + 10, y + 132, 0xFF90CAF9, false);
        }
        if (!view.invitation().token().isEmpty()) {
            graphics.drawString(font, "Invitation from " + view.invitation().inviter(), x + 10,
                    y + box.managementY() + 3, 0xFFFFFFAA, false);
        }
    }
    void tooltip(GuiGraphics graphics, Font font, int x, int y, D1PartyPanelLayout box, int mouseX, int mouseY) {
        if (view == null || consent() || recruiting || mercenaries
                || mouseX < x + 10 || mouseX >= x + box.width() - 10 || mouseY < y + 64) return;
        int row = (mouseY - y - 64) / 11;
        if (row < view.members().size())
            graphics.setComponentTooltipForNextFrame(font,
                    D1PartyPresentation.memberDetails(view, view.members().get(row)).stream()
                            .map(Component::literal).map(line -> (Component) line).toList(), mouseX, mouseY);
    }
}
