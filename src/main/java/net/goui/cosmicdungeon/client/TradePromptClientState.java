package net.goui.cosmicdungeon.client;

public final class TradePromptClientState {
    private static volatile boolean hidePrompt;

    private TradePromptClientState() {}

    public static boolean shouldHidePrompt() {
        return hidePrompt;
    }

    public static void setHidePrompt(boolean hide) {
        hidePrompt = hide;
    }
}
