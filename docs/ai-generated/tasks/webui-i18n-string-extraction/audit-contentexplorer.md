# contentExplorer + contentBrowser audit

Owner: WebUI PR-C (per `docs/ai-generated/tasks/webui-i18n-string-extraction/plan.md`).
Date: 2026-08-01.
Base branch: `development` (JDK 21).
Inputs: `tmp/webui-i18n-by-area/candidates-contentExplorer.tsv` (11 hits),
`tmp/webui-i18n-by-area/candidates-contentBrowser.tsv` (1 hit).

## Scope

Raw regex sweep (`>[A-Za-z][…]<` text nodes + `placeholder=`, `aria-label=`,
`title=`, `alt=` with uppercase first letter) over every `.tsx` under
`WebUI/src/main/ts/contentExplorer/**` and `WebUI/src/main/ts/contentBrowser/**`.
Per-file hit counts:

|                       File                        | Raw hits |  Real | False positive (JSDoc) |
|---------------------------------------------------|---------:|------:|-----------------------:|
| `contentExplorer/FolderSecurityPanel.tsx`         |        2 |     0 |                      2 |
| `contentExplorer/ReducedActions.tsx`              |        1 |     0 |                      1 |
| `contentExplorer/SearchPanel.tsx`                 |        1 |     0 |                      1 |
| `contentExplorer/views/RelationshipsView.tsx`     |        1 |     1 |                      0 |
| `contentExplorer/wizards/SiteCopyWizard.tsx`      |        4 |     4 |                      0 |
| `contentExplorer/wizards/SubfolderCopyWizard.tsx` |        2 |     2 |                      0 |
| **contentExplorer total**                         |   **11** | **7** |                  **4** |
| `contentBrowser/ContentBrowser.tsx`               |        1 |     1 |                      0 |
| **contentBrowser total**                          |    **1** | **1** |                  **0** |
| **Both areas combined**                           |   **12** | **8** |                  **4** |

(The plan's "Note for hosts" JSDoc paragraph in `ReducedActions.tsx:263`
counts as 1 JSDoc hit, not 2. The earlier sub-count was 4 JSDoc, not 5;
that matches the 5 JSDoc items listed under § False positives below
where each file contributes exactly one JSDoc block, with `FolderSecurityPanel.tsx`
contributing two lines from a single block.)

## Reusable keys (none expected — these areas are not in MSG)

The plan's prompt text asserts "no MSG constants exist for this area
yet." That is true of the **global** `MSG` catalog in
`WebUI/src/main/ts/i18n/message.ts` — there are zero
`CONTENT_EXPLORER_*` / `CONTENT_BROWSER_*` constants there (verified by
`Select-String` over the file). However, the audit must correct the
prompt's premise before any PR ships:

- **A per-area `EXPLORER_MSG` catalog already exists** at
  `WebUI/src/main/ts/contentExplorer/messages.ts:26` (`export const
  EXPLORER_MSG = { … } as const;`). It defines ~70 keys consumed by
  `FolderSecurityPanel.tsx`, `ReducedActions.tsx`, `SearchPanel.tsx`,
  `ExplorerTree.tsx`, `DetailList.tsx`, `ContentExplorerShell.tsx`,
  `clipboard/ClipboardPanel.tsx`, `views/RelationshipsView.tsx`,
  `wizards/SiteCopyWizard.tsx`, and `wizards/SubfolderCopyWizard.tsx`.
- **Prefix inconsistency with the plan.** Existing `EXPLORER_MSG` keys
  use a flat `perc.ui.explorer@…` prefix (e.g. `EXPLORER_MSG.TITLE =
  "perc.ui.explorer@Content Explorer"`,
  `EXPLORER_MSG.SITE_COPY_STEP_CONFIRM = "perc.ui.explorer@Confirm"`).
  The plan's Phase 1 § "Naming & MSG catalog extension" proposes
  `perc.ui.contentexplorer.<screen>@…` (with a `<screen>` sub-key).
  This audit uses the plan's proposed prefix for all **new** keys; the
  existing flat `perc.ui.explorer@…` keys are out of scope for this
  audit (they predate the plan and are slated for a separate
  re-prefixing pass — see § "Open issues" below).
- **No per-area catalog exists for `contentBrowser/`** yet
  (`contentBrowser/` has no `messages.ts`). The single candidate in
  that area will use an inline `message("perc.ui.contentbrowser@…")`
  call per the prompt's instruction, since there is no MSG constant
  bucket to extend. A `contentBrowser/messages.ts` can be introduced
  later if the file's chrome grows.

## Reusable keys (TMX has the tuid)

Pre-flight per the plan's Phase 2 rule — `Select-String` against
`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` for
`tuid="perc\.ui\.content(explorer|browser)\."` returns **zero hits**.
Therefore there are no reusable CmsUi tuids for any of the candidates
below; every row in § "New keys" is net-new and Phase 2 must add the
`<tu>` block to CmsUi.tmx in en-us only.

For completeness, `SystemResources.tmx` contains ~14 legacy
`psx.ce.*` keys (`psx.ce.action@Check-in`, `…@Force Check-in`,
`…@Insert`, `…@Update`, `…@New Version`, `…@Check-out`,
`…@Preview`, `…@Edit`, `…@Add new item`, `…@Edit table`,
`psx.ce.error@requiredOccurrence`, `…@countedOccurrence`,
`…@genericFieldError`). Those are **server-side Content Editor
resource strings** consumed by `PSI18nUtils.getString`-style lookups
and have nothing to do with the React SPA chrome audited here. They
must not be reused, edited, or extended from this audit.

## New keys

All six new tuids below are net-new. `MSG`-constant recommendations
land in the per-area `EXPLORER_MSG` catalog
(`WebUI/src/main/ts/contentExplorer/messages.ts`) where one exists;
for `contentBrowser` the constant is **inline** in
`ContentBrowser.tsx` since no per-area catalog exists. The `Source:`
and `Target:` labels are shared across both copy wizards and must use
a single cross-screen tuid (see note column) — runtime is last-wins on
duplicate tuid per `PSTmxResourceBundle.addResourcesToCache`, so two
tuid blocks with identical text would silently shadow each other.

|                       file:line                       |        English        |                        Proposed tuid                        |                                                                    Proposed MSG constant (or `inline message(...)`)                                                                    |                                                                                                                                                                        Notes                                                                                                                                                                        |
|-------------------------------------------------------|-----------------------|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `contentExplorer/views/RelationshipsView.tsx:230`     | `Supplementary links` | `perc.ui.contentexplorer.relationships@Supplementary links` | `EXPLORER_MSG.RELATIONSHIPS_SUPPLEMENTARY_LINKS = "perc.ui.contentexplorer.relationships@Supplementary links"`                                                                         | Rendered as the `<summary>` of a `<details>` element (line 229) that reveals the AA / slot / taxonomy row list. Single literal, no interpolation. Add to `EXPLORER_MSG` next to the existing `RELATIONSHIPS_*` block at `messages.ts:106-110`.                                                                                                      |
| `contentExplorer/wizards/SiteCopyWizard.tsx:202`      | `Source:`             | `perc.ui.contentexplorer.copyconfirm@Source`                | `EXPLORER_MSG.COPY_CONFIRM_SOURCE = "perc.ui.contentexplorer.copyconfirm@Source"`                                                                                                      | Shared tuid — same English appears in `SubfolderCopyWizard.tsx:143`. A single `<tu>` in CmsUi.tmx serves both wizards; Phase 3 wires both files to the same `EXPLORER_MSG.COPY_CONFIRM_SOURCE`.                                                                                                                                                     |
| `contentExplorer/wizards/SiteCopyWizard.tsx:205`      | `Target:`             | `perc.ui.contentexplorer.copyconfirm@Target`                | `EXPLORER_MSG.COPY_CONFIRM_TARGET = "perc.ui.contentexplorer.copyconfirm@Target"`                                                                                                      | Same shared-tuids rule as `Source:`. Also used at `SubfolderCopyWizard.tsx:146`. The trailing `<code>{targetSite}{targetFolder}</code>` / `<code>{targetPath}</code>` is dynamic — keep those as-is.                                                                                                                                                |
| `contentExplorer/wizards/SiteCopyWizard.tsx:208`      | `Workflows:`          | `perc.ui.contentexplorer.sitecopy@Workflows`                | `EXPLORER_MSG.SITE_COPY_WORKFLOWS_LABEL = "perc.ui.contentexplorer.sitecopy@Workflows"`                                                                                                | Site-Copy-wizard-only label (not in the Subfolder wizard). The `<code>{workflows}</code>` interpolation is dynamic and is not localized.                                                                                                                                                                                                            |
| `contentExplorer/wizards/SiteCopyWizard.tsx:211`      | `Templates:`          | `perc.ui.contentexplorer.sitecopy@Templates`                | `EXPLORER_MSG.SITE_COPY_TEMPLATES_LABEL = "perc.ui.contentexplorer.sitecopy@Templates"`                                                                                                | Site-Copy-wizard-only label. `<code>{templates}</code>` interpolation stays dynamic.                                                                                                                                                                                                                                                                |
| `contentExplorer/wizards/SubfolderCopyWizard.tsx:143` | `Source:`             | *(reuse)* `perc.ui.contentexplorer.copyconfirm@Source`      | *(reuse)* `EXPLORER_MSG.COPY_CONFIRM_SOURCE`                                                                                                                                           | Reuse the SiteCopyWizard row — single tuid for both. See `SiteCopyWizard.tsx:202` above.                                                                                                                                                                                                                                                            |
| `contentExplorer/wizards/SubfolderCopyWizard.tsx:146` | `Target:`             | *(reuse)* `perc.ui.contentexplorer.copyconfirm@Target`      | *(reuse)* `EXPLORER_MSG.COPY_CONFIRM_TARGET`                                                                                                                                           | Reuse the SiteCopyWizard row — single tuid for both. See `SiteCopyWizard.tsx:205` above.                                                                                                                                                                                                                                                            |
| `contentBrowser/ContentBrowser.tsx:344`               | `Search…`             | `perc.ui.contentbrowser@Search…`                            | `inline message("perc.ui.contentbrowser@Search…")` (no per-area catalog exists; `SEARCH_PLACEHOLDER` constant in `EXPLORER_MSG` is for the explorer's `SearchPanel`, not the browser). | Rendered as the `placeholder=` attribute on a `<input type="search">`. The trailing `…` (U+2026 HORIZONTAL ELLIPSIS) is preserved verbatim from the current literal — keep the character; do not substitute `...`. When introducing a `contentBrowser/messages.ts`, move this into `BROWSER_MSG.SEARCH_PLACEHOLDER` for symmetry with the explorer. |

Net-new tuids after sharing `Source:` and `Target:` across both
wizards: **6** — five in the `perc.ui.contentexplorer.*` tree, one in
the `perc.ui.contentbrowser@` tree.

## False positives

The regex swept up five JSDoc paragraphs / `<p>`/`<li>`/`<strong>`
markers that live inside `/** … */` file-level or function-level
documentation blocks. JSDoc is not user-visible chrome; do not extract
keys for it. All five are listed for traceability.

|                  file:line                   |                                      Snippet                                       |                                                              Reason                                                              |
|----------------------------------------------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `contentExplorer/FolderSecurityPanel.tsx:22` | `* <p>Components:</p>`                                                             | Inside the file-level `/** … */` JSDoc block (lines 17–50). Not rendered.                                                        |
| `contentExplorer/FolderSecurityPanel.tsx:36` | `* <li>Read-only banner: shown when the user lacks ADMIN rights.</li>`             | Same file-level JSDoc block. Not rendered.                                                                                       |
| `contentExplorer/ReducedActions.tsx:263`     | `* <p><strong>Note for hosts</strong>: {@link ReducedActionHandlers.onOpen} …</p>` | Inside the `defaultReducedActionHandlers` JSDoc block (lines 258–273). The `<strong>` here is JSDoc HTML, not JSX. Not rendered. |
| `contentExplorer/SearchPanel.tsx:29`         | `* <p>State machine:</p>`                                                          | Inside the file-level JSDoc block (lines 17–41). Not rendered.                                                                   |

(`RelationshipsView.tsx:230`, the only "TEXT" hit on the relationships
file, is **not** JSDoc — see § "New keys".)

## Open issues

These are not blocking for Phase 3 PR-C but should be tracked for a
follow-up Phase 1 catalog extension:

1. **Re-prefix `EXPLORER_MSG` to match the plan's convention.**
   `WebUI/src/main/ts/contentExplorer/messages.ts` uses
   `perc.ui.explorer@…` for ~70 keys. The plan proposes
   `perc.ui.contentexplorer.<screen>@…` with a `<screen>` sub-key.
   Two reasonable resolutions:
   - **(a)** Re-prefix all existing `perc.ui.explorer@…` tuids to
     `perc.ui.contentexplorer.<screen>@…` (one `<screen>` per logical
     group: `tree`, `list`, `actions`, `clipboard`, `wizard`,
     `sitecopy`, `subfoldercopy`, `dependency`, `relationships`,
     `search`, `security`). Backwards-compatible only if the
     `<tu tuid=…>` blocks in CmsUi.tmx are renamed in lockstep —
     `tmx.jsp?mode=js&prefix=perc.ui.contentexplorer.` would then
     load them, but the `prefix=perc.ui.explorer.` consumer (none
     today, but check `tmx.jsp` references) would stop seeing them.
     Pre-flight: `Select-String` `tmx.jsp` for `prefix=perc\.ui\.explorer\.`
     — should return zero.
   - **(b)** Keep the new keys under `perc.ui.contentexplorer.<screen>@…`
     (this audit) and migrate the legacy flat keys later. Simpler
     Phase 3 PR-C; leaves a known inconsistency for a follow-up.
     The prompt directive for this audit is to use
     `perc.ui.contentexplorer.<screen>@…`, so this audit assumes (b).
     Phase 1 should decide before the WebUI `i18n/message.ts` extension
     lands.
2. **No `contentBrowser/messages.ts`.** The one new key in the
   contentBrowser area (`Search…`) is inlined into
   `ContentBrowser.tsx`. If the contentBrowser surface grows, mirror
   the explorer's per-area catalog. Not blocking for PR-C.

## Audit evidence

- `Select-String` against `WebUI/src/main/ts/i18n/message.ts` for
  `CONTENT_EXPLORER|CONTENT_BROWSER|contentexplorer|contentbrowser`
  → 0 matches.
- `Select-String` against
  `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` for
  `tuid="perc\.ui\.content(explorer|browser)\."` → 0 matches.
- `Select-String` against
  `modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx`
  for `tuid="(perc\.ui\.content(explorer|browser)\.|psx\.ce\.)"`
  → 14 matches, all legacy `psx.ce.action@*` / `psx.ce.error@*` keys
  consumed server-side; none reusable for the SPA chrome.
- Manual line-by-line review of every hit in
  `candidates-contentExplorer.tsv` and
  `candidates-contentBrowser.tsv` against the source files.

