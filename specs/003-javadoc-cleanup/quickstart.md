# Quickstart: Verifying the Javadoc Cleanup

**Spec**: `spec.md` | **Date**: 2026-07-11 | **Branch**: `003-javadoc-cleanup`

This file is the validation guide. Run these steps on JDK 21 with a clean checkout of
the branch. Implementation details live in `plan.md` / `tasks.md`.

## Prerequisites

- JDK 21 reachable via `./mvn-env.sh` (the script sets `JAVA_HOME=JAVA_HOME_21`).
- Clean working tree on branch `003-javadoc-cleanup` (or `development` if running a
  pre-cleanup baseline).
- Ability to run `./mvn-env.sh` (network for first-time dep download is the same as any
  other module build; offline builds require the local repo to already have the
  `perc-system`, `utils`, `webservices`, `perc-security-utils`, `perc-i18n`,
  `perc-xml-security`, and `javafx-*` jars).

## Step 1 — Capture the baseline (one time only)

```bash
./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests 2> \
  specs/003-javadoc-cleanup/baseline-raw.txt
```

Expected pre-cleanup outcome:

- Tool prints `44 errors` and `100 warnings` at the end.
- `baseline-raw.txt` contains ~242 issue lines.

If the baseline materially differs from the numbers in `research.md`, capture the
actual numbers; SC-001 measures the **delta**, not an absolute target.

## Step 2 — Apply the fixes

Implemented by the `tasks.md` workflow. Not part of this guide.

## Step 3 — Re-run and capture the post-cleanup report

```bash
./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests 2> \
  specs/003-javadoc-cleanup/post-cleanup.txt
```

Expected outcome:

- Tool prints `0 errors` and `0 warnings`.
- Exit code from `mvn-env.sh` is `0` (note: even at baseline, exit is `0` because the
  parent POM sets `failOnError=false`; the relevant signal is the error/warning
  summary).
- `git diff --stat modules/DesktopContentExplorer/` shows only comment/whitespace
  changes (SC-004).

## Step 4 — Validate against success criteria

| Criterion | How to verify |
|-----------|---------------|
| **SC-001** (≥ 80% warning reduction) | Compare the "N warnings" summary line between `baseline-raw.txt` and `post-cleanup.txt`. Compute `(baseline − post) / baseline`. Must be ≥ 0.80. |
| **SC-002** (0 errors) | Last `N errors` summary line in `post-cleanup.txt` must read `0 errors`. |
| **SC-003** (full module build succeeds) | `./mvn-env.sh -pl modules/DesktopContentExplorer -am verify -DskipTests` exits `0`. |
| **SC-004** (no signature changes) | `git diff --stat -- ':!*.md'` on `modules/DesktopContentExplorer/` shows only comment/whitespace edits; non-comment lines unchanged. |

## Step 5 — Spot-check that no run-time behavior changed

Because this feature only touches comments, formatting, and a small number of
intentional HTML/Javadoc tag repairs, a quick smoke test on the module's own test
suite is sufficient:

```bash
./mvn-env.sh -pl modules/DesktopContentExplorer test
```

> Per Constitution III (Test Discipline), the spec's FR-009 deliberately excludes
> adding new tests for this docs-only feature. The existing tests must still pass
> unchanged.
