# Configurable solo dungeon entry

Servers can set TamsinVane.minimumPartySize to 1 (supported range 1-6; default remains 3).
Eligible dungeoneers near the bound Tamsin can choose Start solo, then use the normal class,
personal ready-check, queue and countdown flow. Inviting a second player remains available
when the selector has capacity. Existing invitations, group ownership and entry checks remain enforced.

Installing this code requires one server restart and matching client JAR. After installation,
NeoForge automatically reloads valid changes to config/CosmicDungeon.config.
The command /config showfile cosmicdungeon server identifies the config file; it does not reload it.

No registry, saved-data or packet-layout migration and no generated assets are required.
Manual TEST server entry verification follows deployment.
