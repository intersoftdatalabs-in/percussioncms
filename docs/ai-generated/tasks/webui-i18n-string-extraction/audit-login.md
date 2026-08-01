# login audit (verify-only)

> Area: `WebUI/src/main/ts/login/`
> PR slot: **PR-H (touch-up)** — no code change expected.
> Status: **PASS (verify-only)** — every `LOGIN_KEYS` constant resolves to a real
> `tuid="perc.ui.login.modern@…"` entry in `CmsUi.tmx`.

---

## Scope

Area: `WebUI/src/main/ts/login/` (10 .ts/.tsx files: `LoginPage.tsx`,
`LocaleSelect.tsx`, `LocaleFlag.tsx`, `tmxLoader.ts`, `types.ts`, `i18n.ts`,
plus test files).

Phase 0 candidate file:

```
C:\workspaces\intersoft-workspace\percussioncms\tmp\webui-i18n-by-area\candidates-login.tsv
```

**Confirmed absent.** The `tmp\webui-i18n-by-area\` directory contains
candidate TSVs for every other area (admin, app, contentBrowser,
contentExplorer, dashboard, developer, home, publishing, widgetbuilder,
workflowActions, workflowAdmin) — `candidates-login.tsv` is intentionally
**not** emitted, matching the Phase 0 sweep result of **0** regex hits for
this directory (see plan §"Screen inventory", `login/` row: 10 files / 0 hits).

The 0-hit outcome is expected: `login/` already routes every user-visible
string through `LOGIN_KEYS` / `t()` (`WebUI/src/main/ts/login/i18n.ts`),
which `message()` resolves from the `perc.ui.login.modern@*` prefix. There
are no raw JSX text nodes, no localizable `placeholder=` / `aria-label=`
attributes, and no direct `perc.ui.*@*` literals to flag.

---

## Local catalog (login/i18n.ts)

Source: `WebUI/src/main/ts/login/i18n.ts:33-40`.

|   Constant   | English text  |          tuid (catalog key)          |
|--------------|---------------|--------------------------------------|
| `TITLE`      | Sign in       | `perc.ui.login.modern@Sign in`       |
| `USERNAME`   | User name     | `perc.ui.login.modern@User name`     |
| `PASSWORD`   | Password      | `perc.ui.login.modern@Password`      |
| `LOCALE`     | Locale        | `perc.ui.login.modern@Locale`        |
| `USE_LEGACY` | Use legacy UI | `perc.ui.login.modern@Use legacy UI` |
| `SUBMIT`     | Login         | `perc.ui.login.modern@Login`         |
| **Total**    |               | **6**                                |

`LOGIN_KEYS` is typed `as const`; `LoginKey` is a union of those six string
literals. `t(key, args?)` in the same file forwards to
`message(key, args)` from `../i18n/message`, which reads `window.I18N.message`
fresh on every call and falls back to the text after `@` via
`fallbackLabelFromKey` (e.g. `perc.ui.login.modern@Sign in` → `"Sign in"`)
when the TMX bundle is missing or echoes the key.

---

## TMX verification

TMX file: `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`.

Reproducible enumeration command (PowerShell 5.1, run from repo root):

```powershell
Select-String -LiteralPath modules\perc-i18n\src\main\resources\i18n\CmsUi.tmx `
  -Pattern 'tuid="perc\.ui\.login\.modern@' |
  ForEach-Object { if ($_.Line -match 'tuid="(perc\.ui\.login\.modern@[^"]+)"') { $Matches[1] } }
```

Output (one tuid per line, 6 entries):

```
perc.ui.login.modern@Sign in
perc.ui.login.modern@User name
perc.ui.login.modern@Password
perc.ui.login.modern@Locale
perc.ui.login.modern@Use legacy UI
perc.ui.login.modern@Login
```

Count check:

```powershell
(Select-String -LiteralPath modules\perc-i18n\src\main\resources\i18n\CmsUi.tmx `
  -Pattern 'tuid="perc\.ui\.login\.modern@').Count
```

Result: **6**.

Cross-check against the plan's prefix census (2026-08-01):

> `perc.ui.login.modern@` has **6** entries in CmsUi.tmx today — matches the
> 6 keys in `login/i18n.ts` (`LOGIN_KEYS`).

Matches exactly. **6 LOGIN_KEYS ↔ 6 TMX entries**, all six tuids are
distinct (no duplicates, no shadowing risk per the runtime last-wins rule
in `PSTmxResourceBundle.addResourcesToCache`).

---

## Result

**verify-only: PASS.** Every `LOGIN_KEYS` constant resolves to a real
`tuid="perc.ui.login.modern@…"` entry in `CmsUi.tmx`. **No Phase 2 TMX
additions required for login.** PR-H (verify-only) is a **no-op** for
login — the audit is the deliverable; no source change, no MSG catalog
edit, no TMX edit, no Phase 4 translation work for these keys (translations
already ship under `perc.ui.login.modern@*` in the existing locale
snapshots).

Phase 5 follow-up (per plan): extend
`WebUI/src/test/ts/i18n/message.test.ts` so the catalog-symbol-table
assertion includes `LOGIN_KEYS.*`, locking the 6↔6 match in CI.

---

## False positives

None. `tmp\webui-i18n-by-area\candidates-login.tsv` was not emitted
because the Phase 0 regex sweep against `WebUI/src/main/ts/login/`
returned **0** hits. Login's chrome is already fully funnelled through
`LOGIN_KEYS` → `t()` → `message()` → `window.I18N.message`, so there is
no audit row to escalate and no Phase 3 wiring to schedule.
