# CurseForge companion publishing recovery

Scope: publisher, publisher tests, release workflow, release workflow documentation. No runtime JAR changes,
version changes, registries, networking, saved data, or exclusive gameplay hotspots.

The live API rejects string relation project IDs. Use an integer and regression-test it.
When the companion project is configured, publish the helper there without a redundant
archive attachment to a potentially processing parent. Preserve fallback archives when
no companion is configured, plus all receipt/hash/retry guards.

Validation: Python publisher suite and required Java 21 Integration Gate. Manual evidence:
main9063474 approved; companion9063592 and archive9063594 accepted using exact tag CI hashes.
Licensed CurseForge app installation/update and companion moderation are still pending.

GitHub release assets now select only the two exact version filenames. The publish
checkout contains a historical tracked 1.5.0 JAR, which the old wildcard accidentally
attached. Its incorrect release attachment was removed; repository binary policy and
the historical source-tracked file remain unchanged.
