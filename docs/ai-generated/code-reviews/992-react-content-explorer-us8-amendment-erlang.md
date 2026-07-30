# Erlang pre-commit review — Spec 992 amendment US8 (no-residuals-policy)

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-20 15:15 ET
**Subject**: Spec amendment that brings the missing `rest` work for the modern Content Explorer's DependencyViewer + RelationshipsView into spec 992 as a new user-story **US8 (T092–T104)**. Supersedes the morning "Approve with partial" SC-012 path.
**Trigger**: same-day policy revision (2026-07-20 15:15 ET) — "No residuals are allowed out of these spec phases. If rest API work is needed for the UI, the spec must be revised to include that work so the UI can be delivered."

---

## Findings

### Bugs (hard gate)

**None.** The amendment adds scope rather than modifying shipped code; the analysis only touches spec, plan, tasks, matrix, security-review, and parity-evidence files. No shipped product behavior is changed by this commit; no new test-helper code is introduced.

### Spec amendment content

|                                   Artifact                                   |                                                                                                                                             Change                                                                                                                                             |                               Compliance                               |
|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `specs/992-react-content-explorer/research/relationship-rest-gaps.md` (NEW)  | The morning spike artifact that referenced but never authored; now on disk. Records the morning outcome (5 dimensions unknown with clientSidePreview) AND the post-policy US8 amendment with 5 typed GET endpoints + consolidated summary endpoint + sitemanage + rest + WebUI work breakdown. | ✅ Constitution II (no invented APIs — types mirror existing Java DTOs) |
| `specs/992-react-content-explorer/tasks.md` Phase 11 (NEW)                   | Adds T092–T104 with the same task-shape convention used elsewhere in the spec (Vitest + Playwright + service-contract + Erlang pre-push gate + review-thread resolution).                                                                                                                      | ✅ Constitution V (Plan / Complexity)                                   |
| `specs/992-react-content-explorer/contracts/capability-matrix.md` P-Adv rows | Flips DependencyViewer + RelationshipsView from Partial → **Implemented** with US8 evidence; SC-012 release-decision indicator updated to "no partials permitted."                                                                                                                             | ✅ matrix preamble alignment                                            |
| `docs/ai-generated/release/security-review-992.md`                           | Adds US8 row to the threat-model scope with GET / CSRF / AuthZ / DoS controls; rest façade acknowledged per the same-day amendment.                                                                                                                                                            | ✅ Constitution VI                                                      |
| `docs/ai-generated/release/992-8.2-parity-evidence.md`                       | §8 packet rewritten: SC-012 clears when US8 lands; Post-US8 SC-012 packet section added.                                                                                                                                                                                                       | ✅                                                                      |
| `specs/992-react-content-explorer/checklists/cutover-inventory.md`           | New P-Adv (US8) row in the phase sign-off log.                                                                                                                                                                                                                                                 | ✅                                                                      |
| T074 task entry                                                              | Annotation that T074 is done with both the morning AND the afternoon readings (per the policy revision).                                                                                                                                                                                       | ✅                                                                      |
| T086 task entry                                                              | Updated to reflect US8 amendment and the **32 / 32 in-scope Done** post-US8 count.                                                                                                                                                                                                             | ✅                                                                      |
| T089 + T089a + T090 task entries                                             | Updated to reflect US8 surface (5 GET endpoints) and the closed-loop open question.                                                                                                                                                                                                            | ✅                                                                      |

### Constitutional compliance

|              Constraint              |                                                                                                    Compliance                                                                                                     |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| I (no invariants violated)           | ✅ — no shipped-code regression; this commit only re-authorizes spec & evidence files.                                                                                                                             |
| II (no invented APIs)                | ✅ — US8 types mirror existing Java DTOs (`IPSRelationshipCataloger`, `IPSNodeService`, `IPSWidgetAssetRelationshipService`); see `research/relationship-rest-gaps.md` §US8 §"Delivery surface (constitution II)". |
| III (behavioral tests)               | ✅ — T092/3/4/5 each ship Vitest/Playwright.                                                                                                                                                                       |
| IV (service-contract tests)          | ✅ — T099 adds `rest/src/test/java/com/percussion/share/relationship/RelationshipSummaryResourceTest.java` with happy path + AuthZ negative + JSON wire envelope.                                                  |
| V (Plan / Complexity)                | ✅ — Plan recorded in T104 description (~18 new files: sitemanage 2 + rest 11 + WebUI 5 + 3 modified). No new DB; no CSRF surface; no path traversal.                                                              |
| VI (threat-model note)               | ✅ — updated `security-review-992.md` table covers US8 GETs.                                                                                                                                                       |
| VII (format checks)                  | ✅ — `./mvnw -pl projects/sitemanage,rest -am verify` + `npx vitest run` + `npm test -- tests/us8-dependency.spec.js` (no Java / npm file changes in this commit; documented for the upcoming US8 PR train).       |
| IX (review-thread resolution per PR) | ✅ — each US8 sub-PR carries inline reply + `resolveReviewThread` per thread; T104 mandates this.                                                                                                                  |
| E (no residuals out of spec phases)  | ✅ — the amendment itself is the resolution to the residual. US8 is in scope; no out-of-spec work remains.                                                                                                         |

### Cross-platform / portability

No file I/O, no path construction, no OS-specific concerns added or removed by this commit. The 5 US8 GETs accept only `itemId` (an integer or GUID), validated server-side; client-side URLs are assembled from `window.location.origin + "/Rhythmyx/rest/content-explorer/relationships/" + encodeURIComponent(itemId)` — already covered by the existing fetch wrapper and the `BASE_URL` helper.

### Style / cleanliness

- All updated `.md` files use lowercase sections where the spec convention applies; no emoji added.
- The Erlang report at `docs/ai-generated/code-reviews/992-react-content-explorer-us8-amendment-erlang.md` (this file) follows the previous reviews' shape.
- Prettier N/A (markdown).

### ER-typed summary

|              Category               |                                                                                                                                               Count |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------:|
| Blocking bugs                       |                                                                                                                                                   0 |
| Non-blocking observations           | 1 (informational: US8 is a multi-week train; the implementer should kick off the sitemanage sub-PR first so the rest façade has a backing service.) |
| Style cleanups                      |                                                                                                                                                   0 |
| Cross-platform portability findings |                                                                                                                                                   0 |
| Constitution rule violations        |                                                                                                                                                   0 |

### Non-blocking observation

1. **Suggested implementation order for US8** (informational, not a release-gate):
   1. Sitemanage sub-PR (T096 + T097) ships first — provides the backing service interface and impl.
   2. rest sub-PR (T098 + T099) consumes the sitemanage service — adds the JAX-RS resource + service-contract test.
   3. WebUI sub-PR (T100–T104) lands last — types + api client + view updates + Vitest + Playwright; matrix P-Adv flips from Partial → Implemented; SC-012 packet closes.
      The Erlang pre-push gate fires per sub-PR.

---

## Recommendation

**APPROVE** commit + push the spec amendment.

This is the explicit user-enforced pre-push Erlang gate per `.kilocode/rules/pre-commit-review.md` and root `AGENTS.md` "Pre-commit code review (Erlang)". The amendment is the resolution to a same-day policy revision; deferring it would leave the DependencyViewer / RelationshipsView in a partial state at 8.2 GA, which the new policy forbids.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this amendment:  0 blocking, 0 critical, 0 minor + 1 informational
PORTABILITY CHECK:            0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:      0
FAILS (any):                  no
```

After push, the spec/contracts/matrix/evidence artifacts are aligned. The next concrete deliverable is the US8 implementation PR train (T092–T104); without that, SC-012 still blocks 8.2 GA.
