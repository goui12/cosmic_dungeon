# Theurgist resurrection

- Positive Potions level 10 unlocks same-group, same-instance player resurrection.
- The death screen offers Accept Resurrection from the living mercenary; respawn at the
  exact recorded death location with five seconds of invincibility.
- Three-minute cooldown appears in inventory group HUD hover after unlock.
- Server validates current membership, death token, living mercenary, cooldown and
  inventory recovery before native respawn. Death drops are never copied or refunded.
- Additive run values preserve cooldown/death identity through reload; new runs reset.
  Protocol 16 requires matching client/server builds. No spawner migration or datagen.
