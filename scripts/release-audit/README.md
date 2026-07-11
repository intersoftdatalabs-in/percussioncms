# Release Audit Pipeline

The `release-audit/` pipeline inventories every non-dependabot PR merged into a tagged release of `development-8.1.x`, classifies each PR against the `development` branch (JDK 21 / Jakarta EE 10), and emits a prioritized migration backlog plus a reviewable Markdown summary.

This is the audit deliverable for spec **005-migrate-8.1.7-changes** — migrate v8.1.7 changes to the 8.2 development branch.

## Prerequisites

- `bash` 4+
- `git` with `origin` reachable
- `gh` CLI v2.x — authenticated to `github.com` (active account)
- `jq` 1.6+

Verify before first run:

```bash
gh --version && git --version && jq --version && bash --version | head -1
gh auth status --active
git ls-remote origin v8.1.6 v8.1.7
```

## Usage

```bash
bash scripts/release-audit/release-audit.sh \
  --from-tag v8.1.6 \
  --to-tag v8.1.7 \
  --target-branch development \
  --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
```

### Flags

| Flag | Default | Notes |
|------|---------|-------|
| `--from-tag <TAG>` | `v8.1.6` | Lower bound of tag range; required |
| `--to-tag <TAG>` | `v8.1.7` | Upper bound of tag range; required |
| `--target-branch <BRANCH>` | `development` | Branch to compare against |
| `--output-dir <DIR>` | `./tmp/release-audit/<from>..<to>` | Where the four output files are written |
| `--include-dependabot` | `false` | Include dependabot PRs in the inventory (flagged but not excluded) |
| `-h`, `--help` | — | Show usage and exit |

### Exit codes

| Code | Meaning |
|------|---------|
| `0` | Success; all expected output files written |
| `2` | Partial failure; some output files may be present |
| `3` | Invalid arguments (e.g. tag range unresolvable on `origin`) |
| `4` | `gh` CLI active account not authenticated, or `origin` unreachable |

## Outputs

Written under `--output-dir` (gitignored via `./tmp/release-audit/`):

| File | Format | Schema |
|------|--------|--------|
| `_audit_config.json` | JSON | run timestamp, tag range, target branch, commit SHAs |
| `inventory.json` | JSON array | one PRRecord per non-dependabot PR in range |
| `dependabot-excluded.json` | JSON array | audit log of dependabot PRs excluded from inventory |
| `verdicts.json` | JSON array | one PRVerdict per PRRecord (verdict + evidence) |
| `migration-backlog.md` | Markdown | prioritized backlog (only `needs-migration` PRs) |
| `v8.1.7-to-8.2-migration-report.md` | Markdown | 7-section summary for posting to a GitHub issue |

Full JSON / Markdown schemas: [`specs/005-migrate-8.1.7-changes/contracts/audit-output-schemas.md`](../../specs/005-migrate-8.1.7-changes/contracts/audit-output-schemas.md).

## Re-runnability

The pipeline accepts any tag range. To verify the script is re-runnable (Scenario 3 in quickstart.md):

```bash
bash scripts/release-audit/release-audit.sh \
  --from-tag v8.1.5 --to-tag v8.1.6 \
  --target-branch development \
  --output-dir ./tmp/release-audit/v8.1.5..v8.1.6
ls ./tmp/release-audit/v8.1.5..v8.1.6/
```

Same five output files appear under the new output directory; structure is identical.

## CI integration

The audit can run in CI on every new v*.x tag push. Sample GitHub Actions step:

```yaml
- name: Run release audit
  if: startsWith(github.ref, 'refs/tags/v')
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    PREV_TAG=$(git tag --sort=-version:refname | grep -E "^v[0-9]+\.[0-9]+\.[0-9]+$" | sed -n '2p')
    NEW_TAG=${GITHUB_REF#refs/tags/}
    bash scripts/release-audit/release-audit.sh \
      --from-tag "${PREV_TAG}" \
      --to-tag "${NEW_TAG}" \
      --output-dir "./tmp/release-audit/${PREV_TAG}..${NEW_TAG}"
    # Optional: attach the summary report as a workflow artifact
```

## Out of scope

- **Porting PRs** are not produced by this pipeline. The audit produces the backlog; downstream porters open PRs against `development` per Constitution Principle III (Test Discipline) and IX (PR Review Comment Resolution). See `PORTING.md` (planned, US4).
- **Source-code modification** is forbidden by the audit (`release-audit.sh` is read-only on `origin`/`development`).

## Related documents

- Spec: `specs/005-migrate-8.1.7-changes/spec.md`
- Plan: `specs/005-migrate-8.1.7-changes/plan.md`
- Research: `specs/005-migrate-8.1.7-changes/research.md` (141-PR inventory + 20-PR sample verdicts)
- Data model: `specs/005-migrate-8.1.7-changes/data-model.md`
- Contracts: `specs/005-migrate-8.1.7-changes/contracts/audit-output-schemas.md`
- Quickstart: `specs/005-migrate-8.1.7-changes/quickstart.md`
- Tasks: `specs/005-migrate-8.1.7-changes/tasks.md`

## Conventions

- All paths are repo-relative.
- Outputs are plain text / JSON / Markdown so they diff cleanly in a PR review.
- The script is intentionally **not** a Maven module — it's a bash tool under `scripts/release-audit/` per the Complexity Tracking decision in `plan.md` and the AGENTS.md rule "ALWAYS add generated scripts to repo script dir".