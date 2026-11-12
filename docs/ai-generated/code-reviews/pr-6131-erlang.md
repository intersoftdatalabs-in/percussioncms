# Erlang review — PR #6131 (post-hoc; MERGED)

- Date: 2026-11-11
- PR: #6131 `fix(992): T087 candidate-build quickstart evidence + gate fixes (#6094)`
- State at review: MERGED (human APPROVED by natechadwick, checks SUCCESS/SKIPPED/NEUTRAL as applicable)
- Tool: `mkd-code-review analyze --pack percussion` (branch report: 0 findings, 0 bugs) + independent diff read
- Persona: erlang 0.1.1

## Scope (11 files)

- `WebUI/.../us7AdvancedModern.jsp` ×2 (`cm/app`, `cm/pages/app` — dual-ship lockstep ✓): parseable pilot id `1-101-708` + `?itemId=` override; stable `perc-modern-ui.js` URL (kills double-React `useState`-null mount)
- `WebUI/.../relationshipsApi.ts`: `fetchNodeSummary` unwraps live `@JsonRootName` envelope, flat-shape tolerant
- `ClipboardPanel.tsx`, `SiteCopyWizard.tsx`, `SubfolderCopyWizard.tsx`: a11y `#888` → `#767676`
- `tests/us7-advanced.spec.js` (Scenario H → US8 server-authoritative contract), `tests/us8-dependency.spec.js` (NEW: 2 stubbed + 1 live round-trip)
- `docs/ai-generated/release/992-8.2-parity-evidence.md` §8 T087 run record
- `docs/ai-generated/code-reviews/issue-6094-erlang.md` — machine report (0/0)

## Gate checks

- In-diff bugs: **0**.
  - Envelope unwrap: guarded (`raw` object check → key check → inner object check) with fallthrough to raw — flat stubs and live wire both parse; unknown shapes degrade to old behavior, never throw.
  - `fetchNodeSummary` sync→async conversion: return type was already `Promise<…>`; awaited callers unaffected.
  - JSP pilot-id parse + stable bundle URL: addresses the proven double-React mount failure; both dual-ship copies updated.
  - Inline-style color deltas only; no logic.
- Tests: rest 1831 + sitemanage 4175 (clean installs, 0 failures) + WebUI vitest 2831 + dist-tree install + live H2 Playwright 11/11, 0 console errors, 0 server ERROR/FATAL.
- Change-class closure: complete (resource contract + tolerant client + stub/live tests + evidence packet). T090 release DECISION correctly left human-gated; PR claims no GA.
- C2 downstream_checked: none — justified (no public/final/API signature changes).
- Product docs: N/A — justified (no operator-facing behavior/procedure change).

## Process note (non-blocking, post-hoc)

- C1 records `npx vite build` but no `tsc --noEmit` line and no standalone WebUI `mvnw clean install` for a PR that edits shipped TS. Risk realized: none (7-file TS delta is small; live bundle proven in cell). Same standing note as #6128 for future WebUI-source PRs.

## Residuals

- #6130 (HTTP 500 vs documented 403 for unresolvable relationship ids) already filed and OPEN. Nothing new to file. (Note: #6130 currently carries no `tech-debt`/`p8` labels — cosmetic; left for the owner; not re-filed to avoid a duplicate.)

## Verdict

**lgtm_merged** — 0 in-diff bugs, evidence complete, checks green, human-approved and merged. No action for erlang-fix.
