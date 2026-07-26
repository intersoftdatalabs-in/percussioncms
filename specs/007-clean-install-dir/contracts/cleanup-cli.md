# Contract: Install Directory Cleanup CLI & Prompt

## Flag

```text
--clean-install-dir
--clean-install-dir=true
--clean-install-dir=false
```

|        Form        |      Meaning      |
|--------------------|-------------------|
| Absent             | `false` (default) |
| Present alone      | `true`            |
| `=true` / `=false` | explicit          |

Applies on **upgrade** only. On new install: no-op (log nothing-to-clean or silence).

## Interactive prompt (upgrade + TTY + candidates + flag not true)

Example shape (wording may vary):

```text
The following obsolete directories were found under <install-root>:
  PreInstall                    ~ 12.4 GB
  JBossServerXML_BAK            ~ 4 KB
Total approximate space: ~ 12.4 GB

These are not required by Percussion CMS 8.x. Back up anything you still need
before continuing.

Remove these directories now? [y/N]:
```

- Default answer if empty: **N** (safe).
- `y` / `yes` (case-insensitive) → delete.
- Anything else → retain.

## Flag + interactive

If `--clean-install-dir=true` and TTY: **no prompt**; delete eligible candidates and log the list and sizes as if accepted.

## Non-interactive without flag

No delete. Optionally log that obsolete dirs were found and how to enable cleanup.

## Output after decision

Must include:

- Which paths were deleted (or retained)
- Approximate sizes where known
- Any path-level failures (warn; upgrade continues)

## Exit code

Cleanup failure alone does **not** force non-zero exit (FR-013). Upgrade outcome still drives process exit as today.
