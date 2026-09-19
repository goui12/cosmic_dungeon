# D1 Tamsin entry and itemized vendor quotes

Players previously bypassed Tamsin's agreement, and vendor quotes showed only a total.
This change adds personal persistent Yes/No acceptance, an interface-only map and
server-validated routing into the six-class selector. Returning accepted players
resume selection or Ready. Developers bind an existing NPC to an existing selector;
there is no automatic NPC or item creation.

Sale confirmation now shows base, enchantment, curse and cap/floor adjustment amounts.
Exact arithmetic and component revalidation reject overflow and changed breakdowns.

New optional-field Tamsin SavedData; existing IDs/data preserved. Protocol version 2
requires matching client/server jars. Bind Tamsin before opening entry to players.
Full party invitations/queue, tax and cross-file crash recovery remain TODOs.
Offline tests/build are recorded in the batch receipt; in-world acceptance is pending.
No required new item PNG. Optional 512 x 256 Tamsin map artwork has a built-in fallback.
