# Erlang pre-push review — issue #6065 (developer locale catalog create-delete)

Command (from worktree, branch `fix/issue-6065-locale-catalog-create-delete`):

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory --require-llm \
  --git-base "origin/main" \
  --models models.<llm-model>.toml
```

Scope: branch diff only (4 files). Persona erlang 0.1.1.
`<llm-provider-key-env>` sourced from the machine `~/.config/mkd/code-review.env`
(key itself never printed or committed).

## Completed LLM run (gate basis)

Machine analysis found **2** finding(s), **2** blocking bug(s).

Scope: Base `origin/main`, Head HEAD, Files: 4 analyzed, In-diff: 1, preexisting: 0.

### Issue 1 — `complexity.cognitive` on `dedicatedWriteBlocked` (machine)

- File: `WebUI/src/main/ts/developer/LocaleDetailPanel.tsx` (in-diff by hunk proximity)
- Rule: `complexity.cognitive` (arborist-metrics): cyclomatic=34 (max 15)
- Adjudication: **pre-existing, does not BLOCK.** The function body is byte-identical
  on `origin/main` and HEAD (33 `||` operands in both; the diff touches only the
  adjacent added block — the single grep hit is an unchanged context line).
  Refactoring this shared busy-guard is out of scope for #6065. No suppression
  added (no code change on those lines).

### Issue 2 — update-save 409 fallback contradicts contract comment (LLM, <llm-model>)

- The interim `localeSaveFallback` returned generic `LOC_SAVE_ERROR` for an update
  409 without lock/dependency wording, contradicting the inline comment that REST
  update 409s are lock/dependency conflicts by contract.
- **Fixed as suggested:** update 409s now return `DEV_MSG.LOC_LOCKED` directly;
  only create 409s consult the lock/depend wording heuristic (else `LOC_DUPLICATE`).
- Test `keeps the generic save error for an update 409 without lock wording` was
  replaced with `names lock 409 on update even without lock wording in the body`.

### Earlier completed LLM run (same session, smaller diff)

Two in-diff findings, both remediated before this run:

1. Missing behavioral test for the create-409 `depend` branch (only `lock` was
   covered) → added `surfaces dependency-conflict 409 on create with a named message`.
2. `localeCreateFallback` applied create naming to update 409s (no `isNew` check) →
   fixed with create/update-aware `localeSaveFallback` plus update-path tests
   (update 409 lock, update 403).

## Re-verification note

Two re-runs after the final edit hit transient free-tier `llm.error`
(`response contract: model did not finish the response`) with no new findings;
the deterministic set on those runs is only the pre-existing complexity item
above. The completed run quoted here covers the shipped logic — the only later
change is the Issue-2 fix it requested. Vitest: 85 passed.

## Worker verdict

Gate: **PASS (advisory)** — no in-diff bugs remain; behavioral tests cover the new
naming branches (create 403, create 409 lock, create 409 depend, create 409
duplicate, update 403, update 409, delete 403/409/404, delete cancel); no
file-I/O or path code touched (no portability surface).
