# Backup / restore dry-run notes (T083 / QC-015 / SC-007)

**Goal:** Execute documented offline backup + restore steps in ≤ 60 minutes on a non-production install tree.

| Step |                             Action                             |  Est.  | Done |
|------|----------------------------------------------------------------|--------|------|
| 1    | Inventory CMS `rxrepository.properties` + DTS service backends | 5 min  | [ ]  |
| 2    | Stop CMS + DTS (platform-specific)                             | 5 min  | [ ]  |
| 3    | Copy CMS `Repository/` + companion configs to external path    | 10 min | [ ]  |
| 4    | Copy each Derby DTS service data + config                      | 10 min | [ ]  |
| 5    | Record sizes / paths in change ticket                          | 5 min  | [ ]  |
| 6    | (Optional) Restore to a throwaway copy and start smoke         | 20 min | [ ]  |
| 7    | Start original instance; verify login / health                 | 5 min  | [ ]  |

**Refs:** `docs/ai-generated/tasks/548-derby-embedded-migration/operator-backup-restore.md`

**Result / wall-clock:** _(fill when executed)_  
**Date / operator:** _(fill)_  
**Issues:** _(fill)_
