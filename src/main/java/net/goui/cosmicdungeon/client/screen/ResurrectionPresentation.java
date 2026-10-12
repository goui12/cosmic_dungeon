package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.TheurgistPayloads;

/** Presentation of server-confirmed seconds only; the client never advances readiness itself. */
public final class ResurrectionPresentation {
    private ResurrectionPresentation() {}
    public static String time(int seconds) {
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
    public static String status(TheurgistPayloads.Resurrection resurrection) {
        String state = switch (resurrection.state()) {
            case "READY" -> "Ready";
            case "COOLDOWN" -> "Cooldown";
            case "DEAD" -> "Dead";
            case "OFFLINE" -> "Offline";
            case "OUT_OF_RUN" -> "Outside this dungeon";
            case "DIFFERENT_DIMENSION" -> "Different dimension";
            case "LOCKED" -> "Locked";
            case "PENDING" -> "Resurrection pending";
            case "HIRER_UNAVAILABLE" -> "Hirer unavailable";
            default -> "Unavailable";
        };
        if (resurrection.seconds() > 0)
            return state + (resurrection.state().equals("COOLDOWN") ? ": " : " / cooldown ") + time(resurrection.seconds());
        if (resurrection.seconds() == 0 && !resurrection.state().equals("READY"))
            return state + " / cooldown ready";
        return state;
    }
    public static String line(TheurgistPayloads.Caster caster) {
        return caster.name() + ": " + status(caster.resurrection());
    }
    public record Page(int index, int pages, int start, int end) {}
    public static Page page(int requested, int size, int capacity) {
        if (size < 0 || capacity < 1) throw new IllegalArgumentException("Invalid resurrection page");
        int pages = Math.max(1, (size + capacity - 1) / capacity);
        int index = Math.clamp(requested, 0, pages - 1);
        return new Page(index, pages, index * capacity, Math.min(size, (index + 1) * capacity));
    }
}
