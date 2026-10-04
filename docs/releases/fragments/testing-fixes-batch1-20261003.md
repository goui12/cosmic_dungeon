# Testing fixes: ownership, kill attribution and Bogatyr wolves

- Attuned equipment can be dropped and falls on death. First eligible acquisition binds
  its original owner permanently; other players cannot collect it. Death drops, including
  the logical currency drop, begin together in the player's death block.
- Cosmic mob group splits require actual player/companion kill attribution. A later
  environmental death credits the last damaging player; untouched environmental deaths
  award nothing. Existing per-mob receipts, split radius and remainder rotation remain.
- Bogatyr wolf eggs tame on spawn, including egg-on-wolf offspring, while respecting the
  existing in-run pack cap. Friendly players, NPCs and owned pets cannot be targeted or hurt.
- Additive ownership/attribution metadata preserves existing saves. No placed-spawner,
  preset, chest-content, loot, registry or networking migration.
- Further approved testing improvements and the offline-source skill balance draft are
  tracked separately; the skill, party, difficulty, mercenary and leaderboard runtimes
  are not part of this batch.
