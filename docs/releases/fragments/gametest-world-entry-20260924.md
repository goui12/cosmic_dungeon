# Development world entry

Fix a GameTest registry serialization mismatch that could leave development clients
stuck on Loading terrain when opening existing or new worlds. Preserve the existing
trade and dungeon regression tests using Minecraft's native function test instances.
No saved-world migration is required.
