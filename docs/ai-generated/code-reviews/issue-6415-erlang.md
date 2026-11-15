# Erlang review — issue #6415 (work-agent pre-push report)

- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --require-llm --git-base origin/main --models <models-dir>/models.openrouter-free.toml` (then `models.ollama-dev-coder.toml` failover)
- Commit: `8daec29` — `fix(WebUI): translation create target-locale search + validation (#6415)`
- Verdict: **LLM stage unavailable — `blocked=erlang_llm_unavailable`. No PR opened.** Deterministic machine stage: **0 code findings** across 7 files. Manual review below.

## Sanitized CLI output

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: required model configuration could not be loaded
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or add an entry in `.mkd-code-review.toml`.

## Work-agent notes (failover evidence)

Profile failover was genuinely exhausted; the `--models` loader rejects
everything in this environment with the identical message:

1. `openrouter-free` — no key in the process environment (verified empty);
   key exists only in the CLI env file, so sourced it and retried with a
   matching pool file — same `llm.error`.
2. `ollama-dev-coder` — Ollama up (`/api/tags` HTTP 200, `dev-coder:latest`
   present); tried four pool-file schemas (`[[models]]` / `[models]` +
   `[[models.pool]]`, with/without `tier`/`base_url`/`api_key_env`), the
   canonical `code_review.toml` itself, a CWD-relative
   `config/models.ollama-dev-coder.toml`, proxy-stripped env, and JSON output.
   Every variant — including a **nonexistent** `--models` path — returns the
   identical `required model configuration could not be loaded`, so the loader
   path is broken in CLI 0.5.0 here, not the file content.

Per step 6.3 the turn stops with `status=failed`,
`blocked=erlang_llm_unavailable`. The single finding is infra-only (File:
`review`), not an in-diff bug, missing test, or non-portable path.

## Manual review (legacy path — author self-check, host Erlang re-reviews)

- `filterTargetLocales` mirrors the #6414 `filterTranslationVariants` shape:
  code + label haystack, case-insensitive substring, NUL-strip, empty query
  returns all. No file I/O, no path handling, no `RegExp` construction from
  user input (uses `String.includes` — no ReDoS surface).
- Selection (`selectedLocales`) is independent of the filter; zero-match hides
  only the fieldset, submit stays disabled (`selectedLocales.size === 0`);
  resets on item change and after successful create, matching the #6414
  rationale. Create error mapping (400/403/404/409), success notice, reload,
  and open-variant flow untouched.
- Message key uses `{filter}` placeholder replaced at the call site; fallback
  `perc.ui.explorer@…` text resolves under Vitest and TMX alike.
- Companions present: Vitest unit + component block (#6415 describes),
  Playwright surface block in the merged #6414 spec file (no thrash with open
  PR #6442, which touches a different spec), product-docs Translations text,
  live H2 proof (4/4 green, console + `server.log` clean, screenshot
  `docs/ai-generated/proofs/issue-6415-target-locale-search.png`).
- Portability: Playwright route patterns are CMS URL paths (`**/…`), per the
  helper-file convention; no OS path construction.

## Build / test evidence (kept for the retry)

- `tsc --noEmit` (WebUI): clean.
- Vitest `TranslationsPanel.test.tsx`: 34/34 (root config).
- Batch `EditorHostTranslations` + `SiteDesignPanel.contextVariableClear` +
  `TranslationsPanel`: 41/41.
- `mvnw clean install` in `modules/perc-qa-automation`: BUILD SUCCESS.
- `mvnw clean install` in `WebUI`: Java Surefire 82 tests, 0 failures;
  `build:modern` + `build:legacy` succeed; `npm-test` phase red ONLY on
  pre-existing failures proven identical on stashed baseline —
  `widgetPayloadNpmParity` (Dependabot lodash 4.18.1 vs expected 4.17.21,
  #6319 line) and full-suite contention flakes (`EditorHost.test.tsx`,
  `SiteDesignPanel`, `EditorHostTranslations` pass in smaller batches).
- QA cell: `qa-up --skip-image-build --then-qa-deploy-webui` → OK,
  `qa-health` → healthy, surface spec 4/4 pass, `server.log` zero
  ERROR/FATAL, then `qa-down`.
