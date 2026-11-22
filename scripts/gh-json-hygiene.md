# gh JSON hygiene (HARD — #6986)

Single source of truth for the night-worker `gh --json` rule (#7015, #7020).
Every night agent/command doc that runs `gh --json` references this file;
`scripts/test_night_gh_json_hygiene.py` pins the reach.

## Rule

gh ≥ 2.105 prints a multi-line `gh event listen ...` hint to **stderr**
after `gh pr view` and related commands. NEVER merge stderr into stdout
(`2>&1`) when the output is piped to a JSON parser — the hint lands in the
JSON stream and `json.load` dies with `JSONDecodeError`.

## Blessed forms

- `gh ... --json ... 2>/dev/null | python3 -c '...json.load...'`
- Shape output with `-q` / `--jq` so no Python JSON parse is needed.
- `python3 scripts/gh-json.py <gh args...>` — forwards `gh` stdout
  byte-identical, inherits stderr untouched (visible, never merged into the
  JSON stream), and fails loudly with the same exit code on a non-zero `gh`
  exit. Never wrap this wrapper in `2>&1` — that reintroduces the bug.

## Scope

Applies to every phase the host executes inline (Claim-check,
Preflight/Reconcile/Triage walks, PRE/POST follow-up) — not only to `work`
sub-agents.
