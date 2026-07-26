# Quickstart Validation: Clean Obsolete Install Directories

**Feature**: `specs/007-clean-install-dir`  
**Contracts**: [contracts/](contracts/) · **Model**: [data-model.md](data-model.md)

## Prerequisites

- Branch with this feature implemented
- JDK 21 via `./mvn-env.sh`
- Unit tests under `modules/perc-distribution-tree` (cleaner class)

## Scenario 1 — Unit: candidate listing

**Expect**: Given a temp install root with `PreInstall/` and `jetty/`, only `PreInstall` (and other MVP paths if present) appears as candidate; `jetty` never does.

```bash
./mvn-env.sh -pl modules/perc-distribution-tree test \
  -Dtest=ObsoleteInstallDirCleanerTest -Dai.integrity.skip=true
```

## Scenario 2 — Unit: flag and prompt decision

**Expect**:

- Flag false + non-interactive → no delete
- Flag true → delete without prompt
- Interactive + flag false + candidates → decision method returns “prompt required”
- Flag true + interactive → proceed without prompt

## Scenario 3 — Unit: size estimate

**Expect**: Directory with known file sizes sums within tolerance (exact sum of file lengths).

## Scenario 4 — Unit: warn-and-continue on delete failure

**Expect**: Simulated undeletable path recorded in failed list; overall result still `continueUpgrade=true`.

## Scenario 5 — Unit: JBoss bak eligibility

**Expect**: With major=5, minor=3, no AppServer → `JBossServerXML_BAK` not eligible even if present. With major=8 → eligible if present.

## Scenario 6 — Manual / integration (optional)

1. Copy a disposable install root or fixture with a multi-MB `PreInstall`.
2. Run upgrade jar/command without flag (TTY): decline → folder remains.
3. Re-run with `--clean-install-dir=true`: folder gone; upgrade continues.

## CI gate

Scenarios 1–5 on every PR. Scenario 6 optional/nightly.
