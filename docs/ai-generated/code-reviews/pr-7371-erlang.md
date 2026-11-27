# Erlang review — PR #7371 (fix #7369: drop installer JSP CDN jQuery pins for vanilla JS)

- Verdict: **LGTM / approve** — no in-diff bugs; static gate 0 findings; Codex 0x [P0]/[P1] (1x [P2] recorded, not blocking).
- Persona: erlang 0.1.1 (`~/.local/share/mkd/agents/erlang`) + patterns memory loaded.
- Scope: `origin/main...870e3875` — 4 files (2 JSP, 1 guard test, 1 prior review note).
- Checks at review time: signed-commits pass (ubuntu+windows), Detect language changes pass,
  Analyze (javascript-typescript) skipped, Analyze (java-kotlin) pending → merge only when green.

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang independent assessment (manual, in-diff)

1. **DispatchTemplateGenerator.jsp** — `$(document).ready(populateSiteList)` →
   `document.addEventListener("DOMContentLoaded", ...populateSiteList)`. The script is inline
   and executes during parsing, strictly before `DOMContentLoaded` fires, so the listener
   cannot miss. `populateSiteList` already null-guards `#sitelist`. No bug.
2. **importtest_post.jsp** — `$(...)`/`$.ajax`/`$('<div/>')` → `getElementById`/`fetch`/
   `createElement+textContent`. The submit handler's first statement dereferences the
   server-side JSP implicit object `request` in browser scope (`request.getParameter("")`),
   which throws `ReferenceError` in **both** the old and new code before any DOM/fetch work
   runs — the interception was already inert, behavior is identical, only the library is
   gone. `htmlEncode` rewrite is equivalent and XSS-safe (`textContent` sink). `#form`
   exists in the page, so the `DOMContentLoaded` binding cannot null-throw. No in-diff bug.
3. **Behavioral tests** — present and adequate for this change class: `InstallerPayloadCdnGuardTest`
   (4/4, inventory now empty — the acceptance criterion of #7369 is exactly "no CDN loads
   remain", verified against the real payload files) + `InstallerPayloadVendoredJsAuditTest`
   (8/8 untouched) + planted-ref negative control that tripped the guard as designed.
   Full `modules/perc-toolkit` suite BUILD SUCCESS, 261 tests, 0 failures.
4. **Portability** — no filesystem path logic touched; URL-form CDN removals only. Clean.
5. **License headers** — no new source files (2 JSP edits + 1 test edit + 1 review note). Gate N/A.
6. **Change-class closure** — JSP installer-payload resources + guard-test pin update; no Java API
   shape change (no reverse-dep install needed); no WebUI/SPA surface (no Playwright companion);
   no operator-facing behavior change (same forms/endpoints → product-docs N/A with reason).

### Codex review

The migration lacks the behavioral tests explicitly required by the repository's review gate. No additional introduced runtime defect was established; existing defects in the import helper were not attributed to this patch.

Review comment:

- [P2] Add behavioral tests for the JavaScript migration — importtest_post.jsp:66-70
  The AJAX-to-fetch migration has no behavioral regression coverage: the modified Java tests only scan for CDN references and cannot verify submission interception, POST headers/body, or safe response rendering. Add executable DOM/fetch tests for these behaviors and the Dispatch page's initialization. Missing behavioral tests for changed non-trivial logic explicitly block completion under AGENTS.md:124; passing the inventory guard does not satisfy that requirement.

**Erlang disposition of the [P2] (recorded, not blocking):** the translated handlers are a
mechanical 1:1 swap on pages whose JS is either trivial-ready (`populateSiteList`) or
provably unreachable (the import-test submit handler dies on the server-side `request`
reference in both versions — there is no live behavior to pin without first fixing that
preexisting dead code, which is out of scope for this slice). The payload-level guard tests
verify this change's actual acceptance criterion against the real files. No residual issue
filed: machine findings are 0 and an LLM [P2] suggestion for dead-code DOM tests is not a
genuine leftover under the residual rules (nit-class suggestions never file).

## Residuals

- Preexisting/out-of-diff machine bug or complexity rows in PR-touched files: **none** (0 findings).
- Residual issues filed: **none**.
