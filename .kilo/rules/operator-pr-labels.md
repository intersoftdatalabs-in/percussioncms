# Operator + model labels on PRs and residual issues (HARD GATE)

Applies to **every Kilo Code session** in this repository (Nate, Vijay, and any
other operator using Kilo). Same scheme as the Grok overnight workflow
`night-issue-prs` (see `.kilo/rules/co-author-attribution.md` and host-local
Grok workflow docs under `.grok/` — that tree is **gitignored** and is not a
tracked source of truth in this repository).

## Why

Daily status reporting needs a reliable **Operator** column:

| Operator cell (report) | Labels on PR / residual issue |
|------------------------|-------------------------------|
| `Kilo` (`<model>`) | `operator:kilo` + `model:<model-id>` |
| `Grok` (`<model>`) | `operator:grok` + `model:<model-id>` |
| `Grok: night-issue-prs` (`<model>`) | `operator:grok` + `operator:night-issue-prs` + `model:<model-id>` |
| `Minimax` (`<model>`) | `operator:minimax` + `model:<model-id>` |
| `Nate` (human only) | optional `operator:nate` — or no agent labels |
| `Dependabot` | author is dependabot (no agent labels required) |

Do **not** invent a `daily-status` label. Status is derived from **last 24h PR
activity** + these labels (and author filters such as excluding
`vijaya-boddipudi` when requested for *human* status views — **still label your
agent PRs** so agent work is attributable).

**Canonical write-up for Kilo (in this repository):** this file. Peer rule:
`.kilo/rules/co-author-attribution.md` (Co-Authored footer + pointer here).

Do **not** chase:

- `.grok/workflows/README.md` — host-local / gitignored (not committed)
- `scripts/daily-status.py` — not present in this repository; operators may use
  private host tooling for reports, but agent gates live in `.kilo/rules/`

## HARD GATE — every PR you open or update as Kilo

When you create or update a **pull request** for work authored in this Kilo
session:

1. **Always** apply:
   - `operator:kilo`
   - `model:<exact model id for this session>`  
     Examples: `model:claude-sonnet-4`, `model:gpt-4.1`, whatever the tool/UI
     reports — use a **stable lowercase slug** with no spaces  
     (prefer `model:claude-sonnet-4.5` over marketing names).
2. Create missing labels if needed (idempotent):

   ```bash
   gh label create "operator:kilo" --force --color "d73a4a" --description "Work produced by Kilo Code agent"
   gh label create "model:<id>" --force --color "1d76db" --description "Model: <id>"
   ```

3. Prefer on create:

   ```bash
   gh pr create ... --label "operator:kilo" --label "model:<id>"
   ```

   Or after open:

   ```bash
   gh pr edit <n> --add-label "operator:kilo" --add-label "model:<id>"
   ```

4. Put the same attribution in the **PR body** (one line is enough):

   ```text
   Operator: Kilo (<model-id>)
   ```

5. Keep the existing Co-Authored footer on commits (see
   `.kilo/rules/co-author-attribution.md`) — labels do **not** replace it.

## Residual / child issues

When you file follow-up or residual GitHub issues for unfinished work:

```bash
gh issue create ... --label "operator:kilo" --label "model:<id>"
```

Leave residuals **unassigned** unless the human asks otherwise.

## Do **not** apply agent labels when

- The human explicitly asks for a **human-only** PR with no agent attribution.
- You are only commenting/reviewing with no code authorship.
- Dependabot or another bot owns the PR (do not overwrite bot authorship labels).

## Nate vs Vijay

Both use **the same labels** when Kilo authors the PR:

- Always `operator:kilo` + `model:<id>` for agent work.
- Do **not** put `operator:nate` on Vijay’s agent PRs.
- Do **not** put `operator:vijay` (not part of the scheme).
- Human-only Nate work may use optional `operator:nate` or no operator label;
  daily-status heuristics then attribute to Nate by GitHub author.

## Do not use these for Kilo product PRs

| Label | Who uses it |
|-------|-------------|
| `operator:grok` | Grok Build / Grok CLI only |
| `operator:night-issue-prs` | Grok overnight workflow only |
| `operator:minimax` | Minimax only |

## Checklist before “PR ready”

- [ ] `operator:kilo` on the PR  
- [ ] `model:<session model id>` on the PR  
- [ ] Residual issues (if any) carry the same two labels  
- [ ] Co-Authored footer on agent commits  
- [ ] No merge unless the human ordered it  

## Related

- Pre-commit Erlang: `.kilo/rules/pre-commit-review.md`
- Co-author footer: `.kilo/rules/co-author-attribution.md`
- Root PR review resolution gate: `AGENTS.md` / `REVIEW.md` (reply + resolve threads)
