# Erlang pre-push review — issue #5808 (`fix/issue-5808-m2-evidence-snapshot`)

| Field | Value |
|-------|-------|
| **Date (UTC)** | 2026-11-15 |
| **Base** | `origin/main` |
| **Branch** | `fix/issue-5808-m2-evidence-snapshot` |
| **Gate** | **BLOCKED — `llm.error`, not clean** |
| **Models profile** | `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.openrouter-free.toml` |

## Command

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory --require-llm \
  --git-base "origin/main" \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.openrouter-free.toml
```

Exit: 0 (report emitted with `request-changes`).

## Full report (`--format markdown` output)

```markdown
request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `openrouter-free` failed: config: missing env OPENROUTER_API_KEY (for OpenRouter set OPENROUTER_API_KEY from https://openrouter.ai/keys)
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
```

## Adjudication

- The single finding is environmental (`llm.error`: `OPENROUTER_API_KEY` is
  absent from the process environment; no key material is stored in the repo
  or the worktree). There are **zero machine findings** in the diff.
- Per the night-issue-prs work contract (step 6): an incomplete review is not
  clean — deterministic hard-gate failures aside, the LLM gate did not pass,
  so **no PR may be opened** on this branch until the Erlang CLI runs clean
  with the `openrouter/free` route.
- Diff scope is docs-only (`docs/ai-generated/tasks/template-assembler-
  normalization/m2-evidence-snapshot-2026-11-15-5808.md` new +
  `definition-xml-shim-removal-criteria.md` dated note); no production
  sources, no paths, no tests touched — nothing for the cross-platform or
  behavioral-test hard gates to bite on.
- Next run: re-run the same CLI command with `OPENROUTER_API_KEY` present;
  on `Gate: PASS`, open the PR. Probe logs are preserved at
  `scratch/m2-5808-{perc-packages,sitemanage,webui}.log`.

> Co-Authored by OpenCode 2.3.0 using muse-spark-1.3-contributor with agent night-issue-prs.
