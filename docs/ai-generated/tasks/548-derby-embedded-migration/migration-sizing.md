# Migration duration and disk sizing

Guidance for capacity planning before Derby → H2 upgrade migration (plan WP6 / T087 / T050).

## Disk precheck (product)

Before pump, the CMS migrator requires free space on the target volume of roughly:

```text
required ≈ max(64 MiB, 3 × size(Repository) + 64 MiB)
```

If free space is insufficient, outcome is **`FAILED`** with Derby left intact (QC-021).

Operators should also reserve space for:

| Bucket | Purpose |
|--------|---------|
| Source | Existing Derby tree (retained after SUCCESS) |
| Target | New H2 files under the product H2 base path |
| Staging | `PreInstall/tablefactory-migration/<ts>/` export XML |
| Backup | `PreInstall/migration-backup/<ts>/` or external backup store |

**Rule of thumb:** free space ≥ **source size × 3** on the volume that holds the install tree, plus external backup capacity if backups leave the install disk.

## Duration (wall-clock)

Wall-clock depends on repository size, disk speed, and host load. Capture measurements in:

```text
specs/548-derby-embedded-migration/checklists/migration-timing.md
```

| Scale (illustrative) | Expectation |
|----------------------|-------------|
| Empty / smoke fixture | Seconds |
| Small content set | Minutes |
| Large (≥1000 content items / multi-GB) | Plan a maintenance window; log actuals in migration-timing.md (T050) |

Until T050 scale fixtures publish measured numbers, treat large repos as **maintenance-window** work and run a dry-run timing on a clone when possible.

## Failure injection confidence

Controlled failures (disk, lock, unreachable source, validation, gate) must leave Derby config live — see `PSMigrationFailureInjectionTest` (SC-004).
