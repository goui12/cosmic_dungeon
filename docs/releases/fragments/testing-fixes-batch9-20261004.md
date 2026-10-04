# Mercenary field behavior

Hired mercenaries now follow their hirer, recover to a safe nearby loaded position when
stuck or separated, and fight nearby threats with their equipped weapon. Companion
ownership preserves existing kill credit and friendly exclusions without awarding human
weapon-skill XP for mercenary attacks.

Mercenaries collect nearby permitted chest/brewing supplies and dropped items. Personal
slot ownership, class restrictions, bound items and locked containers remain protected.
Whole collected stacks retain their components. Native recipe combinations consume real
ingredients and preserve crafting remainders; a full bag prevents the entire brew.

They use potions to support allies within eight blocks or affect enemies, filtering each
splash/cloud recipient and effect, including undead healing/harming inversion. Brewing,
fallback healing production and individual potion cooldowns are configurable. Default
fallback production is one splash healing potion per three minutes of active simulation;
unloaded/offline time produces no catch-up stock.

Entity timers are optional and backward compatible. No protocol, registry, spawner or
preset change. Death/respawn retention and stacked health/countdown HUD remain Batch 10.
See [Batch 9 verification and pending gameplay checks](../../ai/TESTING_BATCH_9_20261004.md).
