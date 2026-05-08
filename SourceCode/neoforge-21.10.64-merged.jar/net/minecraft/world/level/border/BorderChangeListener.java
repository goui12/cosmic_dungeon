package net.minecraft.world.level.border;

public interface BorderChangeListener {
    void onSetSize(WorldBorder worldBorder, double size);

    void onLerpSize(WorldBorder worldBorder, double oldSize, double newSize, long time);

    void onSetCenter(WorldBorder worldBorder, double x, double z);

    void onSetWarningTime(WorldBorder worldBorder, int warningTime);

    void onSetWarningBlocks(WorldBorder worldBorder, int warningBlocks);

    void onSetDamagePerBlock(WorldBorder worldBorder, double damagePerBlock);

    void onSetSafeZone(WorldBorder worldBorder, double safeSize);
}
