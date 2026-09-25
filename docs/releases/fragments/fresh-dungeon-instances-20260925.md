# Fresh dungeon copies

New dungeon runs are created from the latest explicit `/world save` snapshot, including saved entities and village POI data. Each run gets unique physical world IDs; completed/forfeited copies are closed and removed after inventory and companion recovery. Missing snapshots and interrupted preparation fail safely instead of reusing a dirty world. Existing fixed-slot saves remain readable.

Developer note: unsaved authoring changes no longer enter new runs. Save the template after editing it. Back up the full world before upgrading, and drain runs before downgrading to a build without dynamic instance support.
