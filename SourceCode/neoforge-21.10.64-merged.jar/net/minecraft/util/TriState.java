package net.minecraft.util;

public enum TriState {
    TRUE,
    FALSE,
    DEFAULT;

    public boolean toBoolean(boolean defaultValue) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            default -> defaultValue;
        };
    }

    // Neo: Helper methods for use in patches

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isFalse() {
        return this == FALSE;
    }
}
