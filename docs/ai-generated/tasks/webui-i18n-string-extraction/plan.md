# WebUI non-localized string extraction & i18n wiring

Owner: WebUI + perc-i18n
Base branch: `development` (JDK 21)
Scope: every user-visible English string in `WebUI/src/main/ts/**` that is not yet
resolved through `message(...)` from `WebUI/src/main/ts/i18n/message.ts`, plus
the ~40 dashboard chrome keys (`perc.ui.dashboard.modern@`,
`perc.ui.dashboard.welcome@`, `perc.ui.dashboard.activity@`) already
referenced in the modern `MSG` catalog that **do not exist** in `CmsUi.tmx`.

Out of scope: jQuery / Knockout / Dojo / GWT legacy pages under
`WebUI/src/main/webapp/**`. WebUI/AGENTS.md treats those as residual debt that
must not be modernized by extracting new i18n keys for SPA work; new keys ship
only with React/TypeScript SPA rewrites.

---

## Background (what already exists)

|        Piece         |                                                                                                                Where                                                                                                                 |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| WebUI i18n seam      | `WebUI/src/main/ts/i18n/message.ts` — `message(key, args?)` resolves via `window.I18N.message`, falls back to text after `@` when TMX is missing or returns the key.                                                                 |
| TMX catalog          | `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` (UI, 1625 `perc.ui.*` TUs), `SystemResources.tmx`, `DeveloperUi.tmx`, `ResourceBundle.tmx` (seed; no body).                                                                    |
| Key convention       | `perc.ui.<area>.<component>@<Human Text>` (CmsUi); `psx.ce.<type>@<Message>` (SystemResources). Body stores text under base locales; regionals only contain dialect overrides.                                                       |
| Run-time loader      | `PSTmxResourceBundle` reads `rxconfig/i18n/` (lowercase). `tmx.jsp?mode=js&prefix=perc.ui.` loads the JS catalog the SPA consumes.                                                                                                   |
| Translation pipeline | `modules/perc-i18n/scripts/i18n_translate.py [--target xx] [--file CmsUi.tmx] [--dry-run|--force|--limit N]`. Single source of new translation text. Rate-limit + backoff + checked-in cache at `scripts/cache/i18n_translate.json`. |

### Prefix census (CmsUi.tmx, 2026-08-01)

Counted TUs per prefix currently referenced by `WebUI/src/main/ts/i18n/message.ts`:

|                     Prefix                     | TUs in TMX |                                                          Notes                                                           |
|------------------------------------------------|-----------:|--------------------------------------------------------------------------------------------------------------------------|
| `perc.ui.gadgets.*` (gadget **catalog names**) |        174 | Legacy Dojo-era keys. Reuse, do **not** re-add.                                                                          |
| `perc.ui.publish.title@`                       |         65 | Reuse.                                                                                                                   |
| `perc.ui.home.modern@`                         |         31 | Reuse.                                                                                                                   |
| `perc.ui.publish.modern@`                      |         26 | Reuse.                                                                                                                   |
| `perc.ui.common.label@`                        |         24 | Reuse (e.g. `Log Out`).                                                                                                  |
| `perc.ui.navMenu.*`                            |         12 | Reuse.                                                                                                                   |
| `perc.ui.home@`                                |          6 | Reuse.                                                                                                                   |
| `perc.ui.publish.incrementalPreview@`          |          4 | Reuse.                                                                                                                   |
| `perc.ui.page.mypages@`                        |          3 | Reuse.                                                                                                                   |
| `perc.ui.publish.view@`                        |          1 | Reuse.                                                                                                                   |
| `perc.ui.dashboard.title@`                     |          1 | Reuse.                                                                                                                   |
| `perc.ui.dashboard.modern@`                    |      **0** | **Missing** — dashboard shell chrome, Add-Gadget modal, Widget-Configuration dialog, gadget *descriptions*. ~25 strings. |
| `perc.ui.dashboard.welcome@`                   |      **0** | **Missing** — Welcome widget (greetings + link labels). 5 strings.                                                       |
| `perc.ui.dashboard.activity@`                  |      **0** | **Missing** — Activity widget labels (Loading/Empty/Path/Site/Published/Pending/New/Updated/Archived). 9 strings.        |

**Bottom line**: the truly missing keys referenced from `MSG` are the
`perc.ui.dashboard.modern@`, `perc.ui.dashboard.welcome@`, and
`perc.ui.dashboard.activity@` prefixes — roughly **40 chrome strings**, not
the ~150 implied by earlier draft phrasing. Every other prefix in `MSG`
already has its TMX entry; reuse them as-is and never re-add (the runtime is
last-wins, so duplicates are dangerous per perc-i18n/AGENTS.md §1a).

---

## Phases

### Phase 0 — Audit & candidate list (WebUI only)

Goal: a single Excel-/Markdown-friendly table per area listing every hardcoded
English string that needs an i18n key. No code changes yet.

1. Sweep with a script-friendly regex. Use the existing helper pattern under
   `WebUI/src/main/ts/i18n/` rather than inventing a new one — wire a tiny
   `tools/i18n-audit.mjs` (or just a one-off `Select-String` run) that flags:
   - JSX text nodes: `>[A-Z][a-zA-Z\s,'.\-:]{3,}<` (already exercised during
     planning; catches hundreds of candidate sites).
   - Attribute-localizable strings: `placeholder="..."`, `title="..."`,
     `aria-label="..."`, `alt="..."` whose value starts with an uppercase
     English word or contains a space.
   - `<option>...</option>` and `<label>...</label>` content.
   - String literals in TS that flow into the DOM via `alert(`, `confirm(`,
     `prompt(`, `throw new Error("…")`, `console.error("…")`, toast/snackbar
     messages, `data-testid` is **not** localized.
   - `perc.ui.*@*` literals used directly (without going through MSG) — fold
     them into the MSG catalog so future grep-and-replace stays easy.
2. **Manual review** the candidate list to remove false positives:
   - Comments, JSDoc, log strings, HTTP error codes, regexes, file paths,
     data-testid, machine identifiers (CSS classes, MIME types, role IDs).
   - Strings that are intentionally English-only by domain rule (e.g. enum
     values: `value="PRODUCTION"` is fine — the human **label**
     `"Production"` in `<option>Production</option>` is what we localize).
3. Bucket survivors by screen/area and emit a working doc
   `docs/ai-generated/tasks/webui-i18n-string-extraction/audit-<area>.md`
   per area. The full inventory of every `WebUI/src/main/ts/**` directory
   (counted from a regex sweep against the workspace on 2026-08-01):

   |                Area (top-level dir)                 | .tsx/.ts |          regex hits |                                                                                                                                                                                                 Notes / action                                                                                                                                                                                                  |
   |-----------------------------------------------------|---------:|--------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
   | `app/` (shell + layout + bootstrap)                 |       24 |                   3 | Small; **audit**, then merge into Phase 3 PR-A.                                                                                                                                                                                                                                                                                                                                                                 |
   | `ui-themes/`                                        |        6 |                   0 | Theme tokens; no user-visible chrome. Skip.                                                                                                                                                                                                                                                                                                                                                                     |
   | `components/`                                       |        1 |                   0 | `HelloWorld.ts` placeholder. Skip.                                                                                                                                                                                                                                                                                                                                                                              |
   | `i18n/`                                             |        1 |                   0 | The catalog itself; touched only in Phase 1.                                                                                                                                                                                                                                                                                                                                                                    |
   | `api/`                                              |       44 |                   0 | REST client + types; no UI chrome. Skip.                                                                                                                                                                                                                                                                                                                                                                        |
   | `util/`                                             |        1 |                   0 | Shared utils. Skip.                                                                                                                                                                                                                                                                                                                                                                                             |
   | `login/`                                            |       10 |                   0 | Already localized via `LOGIN_KEYS` / `t()` (`WebUI/src/main/ts/login/i18n.ts`). Verify all keys resolve to TMX (Phase 5 Vitest gate).                                                                                                                                                                                                                                                                           |
   | `logout/`                                           |        5 |                   0 | Already localized via `LOGOUT_KEYS` / `t()` (`WebUI/src/main/ts/logout/i18n.ts`). Verify.                                                                                                                                                                                                                                                                                                                       |
   | `home/`                                             |       18 |                   3 | Mostly clean; `GadgetsSection.tsx` / `UnavailableView.tsx` stragglers. Fold into Phase 3 PR-A.                                                                                                                                                                                                                                                                                                                  |
   | `dashboard/` (gadgets + shell)                      |       29 |                  36 | The big one. Most gadget widgets have already-localized chrome via `MSG`; Phase 3 must replace any direct JSX text with `message(MSG.X)`. **Phase 2 backfills ~40 missing `perc.ui.dashboard.modern@`/`@welcome@`/`@activity@` keys first.**                                                                                                                                                                    |
   | `contentBrowser/`                                   |        3 |                   1 | Likely one attribute on `ContentBrowser.tsx`. Fold into a small PR.                                                                                                                                                                                                                                                                                                                                             |
   | `contentExplorer/`                                  |       22 |                  11 | `SiteCopyWizard.tsx` (4), `SubfolderCopyWizard.tsx` (2), `FolderSecurityPanel.tsx` (2), `ReducedActions.tsx` (1), `SearchPanel.tsx` (1), `RelationshipsView.tsx` (1). New prefix `perc.ui.contentexplorer.*`.                                                                                                                                                                                                   |
   | `publishing/` (shell + sections + design + drivers) |       39 |                  68 | Largest area. `LogDetailsPanel.tsx` (3), `ServerEditor.tsx` (5), drivers `FileDriverFields.tsx` (1), design (`ContentListEditor.tsx`, `ContextsPanel.tsx`, `DeliveryTypesPanel.tsx`, `EditionEditor.tsx`, `SiteDesignPanel.tsx`, `SiteRootBrowser.tsx`), sections (`DesignSection.tsx`, `LogsSection.tsx`, `RuntimeSection.tsx`, `SiteWorkspace.tsx`). Prefix `perc.ui.publish.<screen>@`.                      |
   | `widgetbuilder/`                                    |        4 |                  13 | `DefinitionEditor.tsx` (8) — labels for Label/Prefix/Version/Author/URL/Description/HTML; `DefinitionList.tsx` (4). Prefix `perc.ui.widgetbuilder.*`.                                                                                                                                                                                                                                                           |
   | `workflowAdmin/`                                    |       12 |                   9 | `RoleEditor.tsx` (3), `CategoriesSection.tsx` (2), `WorkflowSiteAssign.tsx` (1), `WorkflowStepList.tsx` (2 attrs). Prefix `perc.ui.workflowadmin.*`.                                                                                                                                                                                                                                                            |
   | `workflowActions/`                                  |        3 |                   5 | `WorkflowActionsPanel.tsx` (4), `AdhocSearch.tsx` (1 attr). Prefix `perc.ui.workflowactions.*`.                                                                                                                                                                                                                                                                                                                 |
   | `admin/` (shell + tools)                            |        7 |                  15 | `TasksSection.tsx` (9+1), `ConsistencyChecker.tsx` (4), `TaskNotifications.tsx` (1). Prefix `perc.ui.admin.tools.*` / `perc.ui.admin.shell@`.                                                                                                                                                                                                                                                                   |
   | `developer/` (design tools)                         |       47 |                  13 | Hits are **attributes only** (placeholder/aria-label/title/alt on 7 files: `CommunityDetailPanel.tsx`, `ContentTypeDetailPanel.tsx`, `DeveloperShell.tsx`, `KeywordEditorPanel.tsx`, `ObjectAclSection.tsx`, `SlotDetailPanel.tsx`, `TemplateDetailPanel.tsx`). Already uses `DEV_MSG` from `developer/messages.ts` — audit is "verify these attribute strings flow through `t()` already", not "add new keys". |
   | **Total**                                           |  **282** | **~178 candidates** | After manual review, expect ~120 real strings to wire.                                                                                                                                                                                                                                                                                                                                                          |

   The "regex hits" column is the output of

   ```
   >[A-Za-z][a-zA-Z\s,'.\-:!?]{3,}<   # JSX text node
   placeholder="[A-Z] | aria-label="[A-Z] | title="[A-Z] | alt="[A-Z]
   ```

   on every `.tsx` in each directory. False positives (comments, test ids,
   enum values, regex, data-attrs) are stripped during manual review.

   Per-area docs:
   `docs/ai-generated/tasks/webui-i18n-string-extraction/audit-<area>.md`
   with columns `file:line | english | proposed_key | reuse-existing? | notes`.
   For areas already partially localized (`login/`, `logout/`, `developer/`),
   the audit doc's "reuse-existing?" column notes the local catalog key and
   the audit task becomes "verify it resolves to a real TMX tuid" rather
   than "add a new TMX entry".

Deliverable: audit `<area>.md` for every area above with
`file:line | english | proposed_key | notes` rows. Save them under
`docs/ai-generated/tasks/webui-i18n-string-extraction/audit-*.md`. Treat the
audit as the source of truth that the rest of the plan implements against.

### Phase 1 — Naming & MSG catalog extension

Goal: a stable, greppable key per string, registered in `MSG` so we never have
to chase loose string literals again.

1. Use the existing `perc.ui.<area>.<component>@…` convention for CmsUi keys.
   Suggested prefixes per area (extend if needed):
   - `perc.ui.dashboard.modern@` — dashboard shell chrome only (Add Gadget,
     Widget Configuration modal, gadget *descriptions*). The gadget *titles*
     already live under `perc.ui.gadgets.*` — do not re-add.
   - `perc.ui.dashboard.welcome@` and `perc.ui.dashboard.activity@` — same
     shell, separate sub-prefix per widget.
   - `perc.ui.publish.<screen>@` for publishing design/driver/sections.
   - `perc.ui.workflowadmin.<screen>@` for workflow admin screens.
   - `perc.ui.widgetbuilder.<screen>@` for widget builder.
   - `perc.ui.contentexplorer.<screen>@` for content explorer.
   - `perc.ui.admin.tools.<tool>@` and `perc.ui.admin.shell@` for admin.
   - Reuse existing keys whenever possible (don't create `@Save` if a key
     like `perc.ui.common.action@Save` already exists).
2. Extend `WebUI/src/main/ts/i18n/message.ts`:
   - Group new constants in named nested objects per area
     (`MSG.DASHBOARD.*`, `MSG.PUBLISH.DESIGN.*`, etc.) so future grep stays
     small. Keep the flat `MSG` re-export for ergonomic usage:
     `message(MSG.DASHBOARD.ADD_GADGET)` or `message(MSG.DASHBOARD["ADD_GADGET"])`.
   - Add `as const` and an explicit `// audited in audit-<area>.md` comment
     next to each block so we can trace every key back to its audit row.
3. Run `npm run typecheck` / Vitest after edits to keep MSG typed.

Deliverable: one PR touching only `WebUI/src/main/ts/i18n/message.ts` (and
possibly `WebUI/src/test/ts/i18n/message.test.ts` if the catalog grows
meaningfully). No TMX changes here.

### Phase 2 — TMX seed (en-us only)

Goal: every new key is registered with an English `<seg>` so the catalog is
self-consistent; non-en backfill is owned by `i18n_translate.py` (Phase 4).

1. Decide the file boundary:
   - CmsUi for product UI labels & messages (the default).
   - SystemResources.tmx for editor / content-explorer style "resource"
     strings (rule of thumb: if a server-side `PSI18nUtils.getString`-style
     lookup would also consume it, prefer SystemResources). The audit must
     mark each row with its target file.
2. For each row in the audit, add a `<tu tuid="…">…<tuv xml:lang="en-us"><seg>…</seg></tuv></tu>`
   block to the chosen TMX file. Place it in the same alphabetical/area order
   the file already uses — follow the conventions in the file's existing
   `<tu>` ordering so diffs stay readable.
3. Preserve `<tuv xml:lang="…">` attribute casing exactly: lowercase hyphen
   BCP-47 (`en-us`), per perc-i18n/AGENTS.md section 1a.
4. Validate after each batch:
   - `xmllint --noout modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`
   - `python3 modules/perc-i18n/scripts/test_i18n_translate.py` for
     regressions on the seed/format.
5. **Hard ban**: hand-write non-en `<seg>` values. Do not paste translated text
   from any external source. Phase 4 owns that.

Deliverable: one or more TMX-only commits (split per area is fine — keep diffs
reviewable). Each commit message references the audit file, e.g.
`"i18n(seed): add dashboard.modern keys per audit-dashboard.md"`.

**Pre-flight check (mandatory before adding any `<tu>`)**: for each candidate
key, run

```bash
Select-String -LiteralPath modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx \
  -Pattern 'tuid="perc\.ui\.<prefix>@<Human Text>"'
```

If a match exists, **reuse** the existing tuid; do not create a second `<tu>`
with the same tuid (runtime is last-wins per
`PSTmxResourceBundle.addResourcesToCache` — duplicates silently shadow each
other across `tmx.jsp` reloads).

### Phase 3 — Wire the SPA

Goal: replace hardcoded strings with `message(MSG.X)`. One PR per screen
boundary (matches the WebUI/AGENTS.md rule "screen-by-screen").

1. Per file in the audit:
   - Replace JSX text nodes with `{message(MSG.AREA.PROPERTY)}`.
   - Replace attribute-localizable strings with `aria-label={message(...)}`,
     `placeholder={message(...)}`, `title={message(...)}`. Do **not** pass a
     function-call result for `<input value>` — derive it from state and wrap
     a getter that returns the localized label, mirroring `HomeShell.tsx:136`.
   - Replace `<option>Production</option>` with
     `<option value="PRODUCTION">{message(MSG.PUBLISH.PRODUCTION)}</option>`
     (keep the `value=` machine-readable; only the human text is localized).
   - For dynamic messages (`error.toast`, `Failed to save`), pass the key as
     the **base string** and stitch runtime data with `I18N.message`'s
     positional `{0}` placeholders via `args` (the WebUI wrapper already
     supports this — see `message.ts:55`).
2. Update `home/index.ts`, `publishing/index.ts`, etc. to re-export the
   local sub-`MSG` if a screen-level export helps tests.
3. Add Vitest cases next to the touched component
   (`WebUI/src/test/ts/<area>/<component>.test.tsx`) asserting that the
   rendered text equals the `MSG` constant when no `I18N` global is present
   (this verifies the fallback path works without a live CMS — important for
   `npm run test` in CI).
4. **Hard gate** — every product UI screen change in this phase requires an
   updated Playwright spec in `modules/perc-qa-automation/` (per WebUI/AGENTS.md
   Playwright (HARD GATE)). Add or extend:
   - `tests/<area>.spec.js` (new behavior) OR
   - `tests/bugs/bug-<issue-id>.spec.js` (regression).
   - Cover: rendered text equals the localized string after a locale switch
     (`/Rhythmyx/...login...→` then refresh into the target locale, or call
     the existing `tests/helpers/locale.js` helper). Use `data-testid` over
     brittle text selectors where possible.
5. Keep PRs small enough to review. Suggested split (each PR's body must
   list every audit file/line it covers and which Playwright spec(s) cover
   the new behavior):
   - PR-A: Dashboard / Gadgets — `dashboard/**`, `app/**` shell, `home/**`
     stragglers; touches `MSG.DASHBOARD.*` and the new dashboard chrome keys
     in TMX; Playwright spec
     `modules/perc-qa-automation/frontend/tests/dashboard.spec.js` (or
     extend existing dashboard coverage).
   - PR-B: Publishing — `publishing/**`; sub-PRs if a single PR exceeds
     ~500 lines of diff. Playwright spec
     `tests/publishing.spec.js` and per-section workflows.
   - PR-C: Content Explorer + Content Browser — `contentExplorer/**`,
     `contentBrowser/**`; Playwright `tests/content-explorer.spec.js`.
   - PR-D: Widget Builder — `widgetbuilder/**`; Playwright
     `tests/widgetbuilder.spec.js`.
   - PR-E: Workflow Admin + Workflow Actions — `workflowAdmin/**`,
     `workflowActions/**`; Playwright
     `tests/workflow.spec.js` / `tests/workflows/*.spec.js`.
   - PR-F: Admin shell + tools — `admin/**`; Playwright
     `tests/admin.spec.js`.
   - PR-G (touch-up): `developer/**` — replace any direct attribute
     strings with `t(DEV_MSG.X)`; this area is already mostly i18n-aware.
     Likely a no-op audit.
   - PR-H (touch-up): `login/**` and `logout/**` — verify every key in
     `login/i18n.ts` and `logout/i18n.ts` resolves to a real TMX tuid via
     the Phase 5 Vitest gate; fix any `LOGIN_KEYS.X` that has no TMX
     entry. No new keys expected.

Deliverable: per-screen PRs as above. Each PR's body must list every audit
file/line it covers and which Playwright spec(s) cover the new behavior.

### Phase 4 — Translation backfill

Goal: each new key has working `<tuv xml:lang="…">` for every active locale in
the matrix.

1. Translate one locale group at a time. Recommended order (matches locale
   popularity & lowest rate-limit risk per `i18n_translate.py` defaults):
   - `de`, `fr`, `es`, `it`, `pt-br`, `nl`, `pl`, `ru`, `tr`, `sv`.
   - Then regionals (`de-de`, `fr-fr`, `es-es`, `es-mx`, …) which by policy
     store **only dialect overrides**; let the script leave them empty unless
     it's a genuine regional difference.
   - Then `ar`, `hi`, `bn`, `te`, `zh-cn`, `zh-tw`, `ja-jp`.
2. For each batch:

   ```bash
   # Dry-run first so we know the diff size
   python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de --dry-run --file CmsUi.tmx

   # Then do it for real
   python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de --file CmsUi.tmx
   ```

   - Commit the cache update in `scripts/cache/i18n_translate.json` alongside
     the TMX diff. The cache is checked in on purpose so re-runs resume.
   - On merge conflicts in the cache (multi-machine runs), run
     `scripts/resolve_i18n_cache_conflicts.py` to union both sides.
   - On TMX merge conflicts, follow the policy in memory
     (`i18n.merge_conflict_tmx.development_headers_canonical`,
     `i18n.merge_conflict_tmx.safeguard_local_tuvs`): keep the development
     `<header>` and never blanket-take `--theirs` if the working tree has
     uncommitted `bn` (or any new locale) TUVs.
3. Validate per batch:
   - `xmllint --noout modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`
   - `./mvnw -pl modules/perc-i18n test`
   - `./mvnw -pl modules/perc-i18n clean install` — produces the new
     `rxconfig/i18n/CmsUi.tmx` snapshot in
     `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/i18n/`.
   - Then `./mvnw -pl modules/perc-distribution-tree clean install` so the
     `updateConfiguration.xml` force-copy picks it up on the next upgrade.
4. Watch for the rake of `perc.ui.dashboard.modern@*` keys specifically — the
   script's first run on those will be large; cap with `--limit N` if a single
   translation call risks hitting rate limits, then re-run.
5. **Hard ban**: do not commit `rxl` files, `tmp/`, or any cache other than
   `scripts/cache/i18n_translate.json` for this work.

Deliverable: one commit per locale (or per batch of 3–4 locales) on the
`development` branch; no separate PR is required for translation backfill —
fold it into the original feature PR or, if it lands in the same review
window, into a follow-up PR titled `i18n(translate): backfill <locales>`.

### Phase 5 — Verification

Goal: prove that the new keys render in the SPA at runtime, not just in
Vitest.

1. Vitest — extend `WebUI/src/test/ts/i18n/message.test.ts` to cover:
   - Catalog symbol table — every `MSG.AREA.KEY` defined in TS has a matching
     `tuid` in `CmsUi.tmx`. Implemented with a simple XML parse at test-time
     (read the TMX once, build a `Set`, assert each `MSG.AREA.KEY`'s key
     string is present). This catches drift between code and TMX forever.
   - Fallback — `message(key)` returns `fallbackLabelFromKey(key)` when
     `window.I18N` is undefined and when the global is defined but echoes
     the key back.
   - Args — `message(key, ["a", "b"])` correctly forwards `args` to
     `I18N.message`.
2. Playwright — for each area PR, add:
   - A smoke test that logs in, switches the locale (using the helpers in
     `tests/helpers/`), navigates to the screen, and asserts the rendered
     label equals the expected translation (read once per locale and locked
     in as the spec fixture; review-friendly snapshot-style assertions).
   - One Vitest-level data-testid presence assertion so the SPA shell can be
     re-mounted in isolation.
3. Local CMS smoke (per WebUI/AGENTS.md dev mode):
   - Build the modern bundle: `cd WebUI && ../mvnw clean install`.
   - Hot-copy to the dev install (no restart for JS/CSS; restart if JSP
     filter changes): copy `perc-modern-ui.js` / `.css` to
     `$DEV_PERCUSSION_INSTALL/jetty/base/webapps/Rhythmyx/cm/modern/assets/`.
   - Manually switch to two non-English locales and screenshot the touched
     screens. Save under
     `docs/ai-generated/tasks/webui-i18n-string-extraction/screenshots/`.
4. Spotless (per AGENTS root rule):

   ```bash
   ./mvnw spotless:apply    # FIRST
   ./mvnw spotless:check    # SECOND
   ```

   Order is mandatory. If apply rewrote files outside this plan's audit
   scope (baseline debt in another module), do not fold them into the
   feature PR — open a separate `chore: Spotless cleanup` PR per the root
   rule.

5. Erlang review (root AGENTS.md hard gate) on the WebUI diff and any
   TMX/perc-i18n touch.

Deliverable: artifacts listed under
`docs/ai-generated/tasks/webui-i18n-string-extraction/verification/`.

### Phase 6 — Pre-PR gates (root AGENTS.md)

1. **Standalone clean install** per module changed:

   ```bash
   cd WebUI                 && ../mvnw clean install
   cd modules/perc-i18n     && ../../mvnw clean install
   cd modules/perc-distribution-tree && ../../mvnw clean install
   ```

   No `-pl … -am` reactor required — these build standalone against the
   local `~/.m2` (after any dependent standalone installs).

2. Spotless `apply` → `check` from the repo root (covers Java, TS, Markdown,
   JS) — see `Pre-PR Spotless formatting (HARD GATE)` in the root AGENTS.md.

3. PR body template (matches the existing `review.pr.delivery_format`
   memory):

   - Summary sentence.
   - Audit cross-reference (rows in `audit-<area>.md` this PR implements).
   - Modules touched & clean-install commands run.
   - Spotless commands run, in order.
   - Vitest command + counts.
   - Playwright spec(s) added/updated, command(s) run, pass/fail.
   - Translation backfill status (deferred to a follow-up PR? locale list
     shipped in this PR?).
   - Screenshots path if user-visible strings changed.
   - Erlang review reference / commit hash.

---

## Screen inventory (exhaustive, 2026-08-01)

Every top-level directory under `WebUI/src/main/ts/**` is covered by either
the audit (Phase 0) or an explicit skip. Counts come from a regex sweep of
every `.tsx` in each directory for hardcoded text nodes
(`>[A-Za-z][a-zA-Z\s,'.\-:!?]{3,}<`) plus localizable attributes
(`placeholder="…"`, `aria-label="…"`, `title="…"`, `alt="…"` with an
uppercase-letter value). False positives (comments, testids, enum values,
data-attrs) are stripped during manual review.

|                      Directory                      | .tsx/.ts |                 Hits |   In plan?   | PR slot |                   Audit doc                    |
|-----------------------------------------------------|---------:|---------------------:|--------------|---------|------------------------------------------------|
| `app/` (shell + layout + bootstrap + routes)        |       24 |                    3 | yes          | PR-A    | `audit-app.md`                                 |
| `ui-themes/`                                        |        6 |                    0 | skip         | —       | —                                              |
| `components/` (HelloWorld placeholder)              |        1 |                    0 | skip         | —       | —                                              |
| `i18n/`                                             |        1 |                    0 | Phase 1 only | —       | —                                              |
| `api/` (REST client + types)                        |       44 |                    0 | skip         | —       | —                                              |
| `util/`                                             |        1 |                    0 | skip         | —       | —                                              |
| `login/`                                            |       10 |                    0 | yes (verify) | PR-H    | `audit-login.md`                               |
| `logout/`                                           |        5 |                    0 | yes (verify) | PR-H    | `audit-logout.md`                              |
| `home/`                                             |       18 |                    3 | yes          | PR-A    | `audit-home.md`                                |
| `dashboard/` (gadgets + shell)                      |       29 |                   36 | yes          | PR-A    | `audit-dashboard.md`                           |
| `contentBrowser/`                                   |        3 |                    1 | yes          | PR-C    | `audit-contentbrowser.md`                      |
| `contentExplorer/`                                  |       22 |                   11 | yes          | PR-C    | `audit-contentexplorer.md`                     |
| `publishing/` (shell + sections + design + drivers) |       39 |                   68 | yes          | PR-B    | `audit-publishing.md` (+ sub-docs per section) |
| `widgetbuilder/`                                    |        4 |                   13 | yes          | PR-D    | `audit-widgetbuilder.md`                       |
| `workflowAdmin/`                                    |       12 |                    9 | yes          | PR-E    | `audit-workflowadmin.md`                       |
| `workflowActions/`                                  |        3 |                    5 | yes          | PR-E    | `audit-workflowactions.md`                     |
| `admin/` (shell + tools)                            |        7 |                   15 | yes          | PR-F    | `audit-admin.md`                               |
| `developer/` (design tools)                         |       47 | 13 (attributes only) | yes (verify) | PR-G    | `audit-developer.md`                           |
| **Total**                                           |  **282** |  **~178 candidates** |              |         |                                                |

**Notes per area**

- `login/`, `logout/` — already fully localized via `LOGIN_KEYS` /
  `LOGOUT_KEYS` (`WebUI/src/main/ts/login/i18n.ts`,
  `WebUI/src/main/ts/logout/i18n.ts`). Audit task: verify every key
  resolves to a real TMX tuid (Phase 5 Vitest gate). PR-H is a no-op
  unless audit finds missing entries.
- `developer/` — already largely localized via `DEV_MSG` (each panel
  imports `messages.ts`). The 13 hits are attribute strings
  (`placeholder=`, `aria-label=`, `title=`, `alt=`) on 7 files. Audit
  task: verify these flow through `t()` already. PR-G is likely a
  no-op unless audit finds direct attribute hardcoding.
- `api/`, `util/`, `ui-themes/`, `components/`, `i18n/` — no user-visible
  chrome or already covered.
- `app/` — small surface (3 hits across `App.tsx`, `LandingShell.tsx`,
  `routes.tsx`). Fold into PR-A (Dashboard + shell) since these are SPA
  chrome adjacent to the dashboard shell.
- `home/` — `GadgetsSection.tsx` (1 attr), `UnavailableView.tsx` (2 text
  nodes). Fold into PR-A.

---

## Risks & decisions

|                                                               Risk                                                               |                                                                                 Decision                                                                                  |
|----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Editing `CmsUi.tmx` by hand for non-en text is forbidden.                                                                        | Use `i18n_translate.py` exclusively for non-en. Hand edits only for en-us source and review fixes.                                                                        |
| Most `MSG` keys already exist in TMX under legacy prefixes (gadget catalog, publish title, home modern, nav menu, common label). | Reuse them as-is. Only the `perc.ui.dashboard.modern@`, `perc.ui.dashboard.welcome@`, `perc.ui.dashboard.activity@` prefixes are net-new (~40 keys).                      |
| Runtime is last-wins on duplicate `tuid`.                                                                                        | Mandatory pre-flight grep before adding any `<tu>` — see Phase 2 pre-flight check.                                                                                        |
| Locale fallback rules (base vs regional).                                                                                        | Store shared translations under base locale tags (`de`, `es`, `fr`, etc.); regionals store only dialect overrides. Script honors this by design — don't edit around it.   |
| Cross-platform.                                                                                                                  | All paths and scripts in this plan must work on Windows, Linux, macOS. Use the repo's wrapper (`mvnw.cmd` / `mvnw`) — never bare `mvn`.                                   |
| Per-screen PR scope creep.                                                                                                       | Follow the WebUI AGENTS.md "screen-by-screen" rule: one area per PR. Translation backfill may ride along if it ships in the same review window.                           |
| Out-of-scope Spotless hits.                                                                                                      | Follow root AGENTS.md: feature PR keeps only in-scope files; baseline debt goes to a `chore: Spotless cleanup` PR. Never abort the feature work.                          |
| Mixing SPA+i18n edits with dashboard-gadget backfills creates a giant diff.                                                      | Prefer one area at a time (Dashboard PR-A, Publishing PR-B, …).                                                                                                           |
| Playwright doesn't run in CI for some areas yet.                                                                                 | Land the test code alongside the change even if not runnable in this environment, and call it out explicitly in the PR body (per WebUI/AGENTS.md Playwright (HARD GATE)). |

---

## Related docs

- `WebUI/AGENTS.md` — Playwright HARD GATE, screen-by-screen rule, build pipeline.
- `modules/perc-i18n/AGENTS.md` — TMX layout rules, key naming, build sequence.
- `modules/perc-i18n/scripts/README.md` — `i18n_translate.py` CLI contract.
- `docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md` — Home
  acceptance checklist that this plan feeds into (sections 4–5).
- Memory records:
  - `project.md::review.pr.delivery_format` (PR review delivery shape).
  - `corrections.md::i18n.merge_conflict_tmx.development_headers_canonical`
    and `i18n.merge_conflict_tmx.safeguard_local_tuvs` (merge policy).

