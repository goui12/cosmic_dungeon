# Mercenary retention, respawn and HUD

- Active parties see stacked mercenary name/hirer, health and respawn panels above currency.
- Mercenaries drop no items or experience on death. Their original equipment and supplies remain on the same persistent entity while it rests.
- After ten minutes of server game time, the same mercenary returns at a safe position near its living hirer in the active run. Offline/dead hirers and blocked positions defer return.
- Optional saved rest fields support old saves without migration. Protocol 12 requires matching client/server jars; rollback after new rest saves requires a matching world backup.
- Java 21 build, 195 native unit tests and offline checks passed. Licensed gameplay and visual acceptance remain pending.

[Behavior, compatibility and testing](../../ai/TESTING_BATCH_10_20261004.md) | [Rollout](../../ai/TESTING_ROLLOUT_20261003.md).
