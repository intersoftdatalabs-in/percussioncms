# Consolidated audit summary

Generated 2026-08-01 from the per-area audits in this folder.

## Headline numbers

|                               Metric                               |                    Value |
|--------------------------------------------------------------------|-------------------------:|
| Total raw regex hits across `WebUI/src/main/ts/**/*.tsx`           |                      177 |
| False positives (JSDoc, enum values, data-testid, comments, regex) |                      ~16 |
| Real user-visible chrome candidates                                |                     ~161 |
| New `MSG.*` constants to add (per-area + global)                   |                     ~150 |
| New TMX `<tu>` entries required for Phase 2 (en-us)                |                     ~553 |
| Existing keys reused (no TMX work)                                 |                      ~10 |
| Areas with a local `messages.ts` / `i18n.ts` already               | login, logout, developer |

## Per-area breakdown

|                       Area                       |       Raw |      Real | Reuse MSG | Reuse TMX |             New |   False | New TMX tuids |          Audit doc          |
|--------------------------------------------------|----------:|----------:|----------:|----------:|----------------:|--------:|--------------:|-----------------------------|
| app + home                                       |         6 |         5 |         3 |         1 |               1 |       1 |             1 | `audit-app-home.md`         |
| dashboard                                        |        36 |        33 |         0 |         1 |              31 |       3 |            31 | `audit-dashboard.md`        |
| publishing (shell + sections + design + drivers) |        68 |        62 |         3 |        11 |              48 |       6 |            48 | `audit-publishing.md`       |
| contentExplorer                                  |        11 |         7 |         0 |         0 |               7 |       4 |             6 | `audit-contentexplorer.md`  |
| contentBrowser                                   |         1 |         1 |         0 |         0 |               1 |       0 |             1 | `audit-contentexplorer.md`  |
| widgetbuilder                                    |        13 |        13 |         0 |         0 | 13 → 9 (shared) |       0 |             9 | `audit-widgetbuilder.md`    |
| workflowAdmin                                    |         9 |         9 |         0 |         0 |               9 |       0 |             9 | `audit-workflow.md`         |
| workflowActions                                  |         5 |         5 |         0 |         0 |               5 |       0 |             5 | `audit-workflow.md`         |
| admin (shell + tools)                            |        15 |        15 |         0 |         0 |              15 |       0 |            15 | `audit-admin.md`            |
| developer (attribute fixes)                      |        13 |        13 |         3 |         0 |              10 |       0 |            10 | `audit-developer-logout.md` |
| developer (full MSG catalog → TMX gap)           | 556 const | 408 tuids |       n/a |         0 |             408 |     n/a |       **408** | `audit-developer-logout.md` |
| logout (verify)                                  |         0 |         0 |         0 |         0 |               0 |       0 |         **3** | `audit-developer-logout.md` |
| login (verify)                                   |         0 |         0 |         0 |         6 |               0 |       0 |             0 | `audit-login.md`            |
| **Total**                                        |   **177** |  **~161** |     **6** |   **~19** |        **~136** | **~16** |      **~553** |                             |

Notes:

- "Real" excludes false positives (JSDoc, enum `value=`, data-testid, etc.).
- "Reuse MSG" = an existing `MSG.*` constant in `WebUI/src/main/ts/i18n/message.ts` matches a candidate's English text exactly.
- "Reuse TMX" = an existing tuid in `CmsUi.tmx` matches the candidate's English text; needs only a new `MSG.*` constant.
- "New" = requires a new `MSG.*` constant **and** a new TMX entry.
- "New TMX tuids" deduplicates shared English across files (e.g. `Source:` shared between `SiteCopyWizard` and `SubfolderCopyWizard`).
- The **developer row** is split: the 13 attribute hits are real wire-up work (Phase 3 PR-G), but the 408 catalog-vs-TMX gap is a Phase 2 bulk insert that fills in keys the `DEV_MSG` catalog already references.

## Top-3 prefixes by new-tuid count

|           Prefix            | New tuids |                    Source                     |
|-----------------------------|----------:|-----------------------------------------------|
| `perc.ui.developer@`        |       408 | developer/messages.ts catalog → CmsUi.tmx gap |
| `perc.ui.dashboard.modern@` |        31 | dashboard widget chrome                       |
| `perc.ui.publish.design.*@` |       ~30 | publishing design panels                      |

The developer gap (408 keys) is the single largest chunk of Phase 2 work — one TMX bulk insert covering all keys already exposed by `developer/messages.ts` so they actually translate at runtime instead of falling back to `@`-text.

## Phase 2 pre-flight (mandatory before any `<tu>` insert)

For every proposed tuid in this folder's audits, the pre-flight grep has been performed against `CmsUi.tmx` and recorded in the per-area audit doc. Any tuid marked "New" was verified to have 0 matches in the existing TMX at audit time. Cross-check before opening the PR:

```powershell
Select-String -LiteralPath modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx `
  -Pattern 'tuid="<proposed-prefix>@<exact text>"'
```

If a match appears, reuse it instead — the runtime is last-wins on duplicate `tuid`.

## Phase 1 work derived from the audits

|             MSG group              |          Files / areas covered          |          New constants |
|------------------------------------|-----------------------------------------|-----------------------:|
| `MSG.DASHBOARD.*`                  | dashboard widgets, modals, descriptions |                    ~31 |
| `MSG.PUBLISH.DESIGN.*`             | publishing design panels                |                    ~30 |
| `MSG.PUBLISH.SECTIONS.*`           | Logs/Runtime/Design/Site sections       |                    ~12 |
| `MSG.PUBLISH.SERVER.*`             | Server editor + driver fields           |                     ~5 |
| `MSG.WIDGETBUILDER.*`              | widget builder editor + list            |                     ~9 |
| `MSG.WORKFLOWADMIN.*`              | workflow admin screens                  |                     ~9 |
| `MSG.WORKFLOWACTIONS.*`            | workflow actions                        |                     ~5 |
| `MSG.ADMIN.*`                      | admin shell + tools                     |                    ~15 |
| `MSG.CONTENTEXPLORER.*`            | content explorer                        |                     ~6 |
| `MSG.CONTENTBROWSER.*`             | content browser                         |                      1 |
| `MSG.APP.SHELL.*`                  | app-level chrome                        |                    1-2 |
| `MSG.DEVELOPER.*` (new attributes) | developer panel ARIA strings            |                     10 |
| **Total**                          |                                         | **~135 new constants** |

`MSG.PUBLISH_SERVER_TYPE` re-keying: the publishing audit flagged that
`MSG.PUBLISH_SERVER_TYPE = "perc.ui.publish.view@Production"` is wired to a
`<label>` but resolves to the *Production* option text. Phase 1 should
re-point to a new `perc.ui.publish.server.editor@Server Type` tuid.

`MSG.DASHBOARD.WIDGET_CONFIG.*`, `MSG.DASHBOARD.MODAL.*`, and the existing
`MSG.GADGET_DESC_*`/`MSG.GADGET_*` constants in the global MSG catalog
cover most of the dashboard chrome — Phase 1 mostly adds per-widget
constants under `MSG.DASHBOARD.<WIDGET>.*`.

## Phase 3 PR split (updated from audits)

|  PR   |                            Scope                            |                   Files                   | New JSX rewires |       New Playwright spec        |
|-------|-------------------------------------------------------------|-------------------------------------------|----------------:|----------------------------------|
| PR-A1 | Dashboard chrome + DashboardLayout + UnavailableGadgetShell | `dashboard/**`                            |             ~10 | `tests/dashboard.spec.js`        |
| PR-A2 | App chrome + home stragglers                                | `app/**`, `home/**`                       |              ~6 | extend `tests/home.spec.js`      |
| PR-B1 | Publishing shell + sections (Logs/Runtime/Design/Site)      | `publishing/sections/**`, root            |             ~15 | `tests/publishing.spec.js`       |
| PR-B2 | Publishing design panels                                    | `publishing/design/**`                    |             ~30 | extend publishing spec           |
| PR-B3 | Publishing server editor + drivers + logs details           | `publishing/components/**`                |             ~10 | extend publishing spec           |
| PR-C  | Content Explorer + Content Browser                          | `contentExplorer/**`, `contentBrowser/**` |              ~7 | `tests/content-explorer.spec.js` |
| PR-D  | Widget Builder                                              | `widgetbuilder/**`                        |             ~13 | `tests/widgetbuilder.spec.js`    |
| PR-E  | Workflow Admin + Actions                                    | `workflowAdmin/**`, `workflowActions/**`  |             ~14 | `tests/workflow.spec.js`         |
| PR-F  | Admin shell + tools                                         | `admin/**`                                |             ~15 | `tests/admin.spec.js`            |
| PR-G  | Developer attribute fixes (aria/title/alt on 7 panels)      | `developer/**` (7 files)                  |             ~10 | extend `tests/developer.spec.js` |
| PR-H  | Login + Logout verify (no code change unless gap found)     | `login/**`, `logout/**`                   |               0 | (Vitest gate)                    |

## Phase 2 TMX seed PR split (separate from code PRs)

|                       Seed PR                       | Tuids added |   File    |
|-----------------------------------------------------|------------:|-----------|
| Seed-1: dashboard chrome                            |          31 | CmsUi.tmx |
| Seed-2: publishing design + sections + server       |         ~48 | CmsUi.tmx |
| Seed-3: content explorer + browser + widget builder |         ~16 | CmsUi.tmx |
| Seed-4: workflow admin + actions + admin            |         ~29 | CmsUi.tmx |
| Seed-5: developer catalog gap                       |     **408** | CmsUi.tmx |
| Seed-6: app shell + logout                          |           4 | CmsUi.tmx |
| **Total**                                           |    **~536** |           |

(Counts will refine as the PR splits are executed and shared keys are
deduped. The developer seed is by far the largest single insertion.)

## Recommendation on order of execution

1. **Phase 1** — extend `MSG` catalog (single PR touching only
   `WebUI/src/main/ts/i18n/message.ts` plus a few new per-area
   `messages.ts` shims if any). This is the prerequisite for every
   downstream phase.
2. **Phase 2 seed-5 (developer 408 keys)** — a single bulk TMX insert that
   unblocks developer chrome. No code change in this PR.
3. **Phase 2 seed-1 / seed-2 / etc.** — per-area TMX inserts.
4. **Phase 3 PR-A1 … PR-H** — per-screen JSX rewires, each riding the
   matching seed PR.
5. **Phase 4** — `i18n_translate.py` per locale.
6. **Phase 5** — Vitest `MSG → TMX` parity gate (this is what guarantees
   we never re-introduce the "0 keys in TMX" state).
7. **Phase 6** — pre-PR gates.

The 408-key developer seed is the biggest single risk because it inserts a
huge block into a 2.6 MB TMX file. Break it into 4 chunks of ~100 keys
each (DeveloperShell + ACL + ObjectAcl + Actions/Communities/ContentTypes
+ DisplayFormats/Extensions/ItemFilters/Keywords/Locales +
Pipelines/Relationships/Searches/ServerConfigs/SharedFields/Sites/Slots/
SystemDef/Templates/Views/Workflows) to keep each commit reviewable.
