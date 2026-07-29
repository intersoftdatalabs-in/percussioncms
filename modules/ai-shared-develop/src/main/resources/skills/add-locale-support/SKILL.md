---
name: add-locale-support
description: >-
  Add a new locale (language) to Percussion CMS, with translated strings in
  CmsUi.tmx and SystemResources.tmx, an RXLOCALE seed row, optional calendar
  picker / Lucene analyzer touch-points, and a back-fill via the Docker
  translate-shell script. Use when the user says "add a new locale", "add a
  new language", "support <locale> translations", "translate to <lang>",
  "add <code> to the language matrix", "RXLOCALE row for <code>", or any
  variant thereof. Canonical source of truth is modules/perc-i18n/AGENTS.md;
  this skill is a develop checklist around it.
---

# Add Locale Support

This skill is the **workflow layer** for adding a new locale to Percussion
CMS. The source of truth for every rule below lives in
`modules/perc-i18n/AGENTS.md` (treat that file as mandatory). When the two
disagree, **AGENTS.md wins**; open a PR to fix this skill afterwards.

## When to activate

- User asks to "add a new language / locale" to the product.
- User asks to translate an existing key set for a new BCP-47 code.
- User wants a new RXLOCALE row, a calendar-picker entry, or a Lucene
  analyzer branch.
- User asks "how do we ship Portuguese-Brazil (or similar)?" — point them
  to this checklist before they touch code.

Do **not** activate for: per-string fixes to a language already in the
matrix (use the *translation pipeline* commands in §5 only), or for
removing an existing locale (different playbook — see §7).

## Pre-flight (must answer first)

1. **What is the BCP-47 code?** Lowercase hyphen, e.g. `en-gb`, `es-mx`,
   `hi-in`, `pt-br`. Tags like `de_DE`, `EN_us`, `ja-JP` all normalize at
   runtime via `PSTmxResourceBundle.normalizeLang(...)`, but every new
   piece of content must use the canonical form.
2. **Is it a generic code (`es`, `hi`) or a regional variant?** Generic
   codes catch-fall any untagged region; regional variants (`es-mx`)
   override only that region. Decide which matrix slots it occupies; do
   not introduce a code outside the canonical 17-locale matrix without
   first updating the `RXLOCALE` table.
3. **Is Docker on PATH?** The translation pipeline (`i18n_translate.py`)
   shells out to `docker run --rm soimort/translate-shell`. Confirm
   `docker info` exits 0 before starting; failure is a tooling issue, not
   a content one.
4. **Which branch / working tree?** Module-level changes span three
   modules — start from `origin/development` (or your assigned base).
   Per repo rules, **never** force-push to `development`; create a
   `feat/<issue>-<code>-locale` branch.

## The 6-step checklist

> Repeat the checklist verbatim in your PR description and tick each item
> before requesting review. Erlang will block the PR if any step is
> skipped without a justification comment.

### 1. Wire the new code into the matrix

1. `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` — add a
   `<prop type="supportedlanguage"><code></prop>` line in the header,
   **alphabetical position**. Do not re-order existing lines.
2. `modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx` — same
   edit. The header is the source of truth for "what ships out of the
   box".
3. `modules/perc-i18n/src/main/resources/i18n/ResourceBundle.tmx` —
   intentionally **untouched**; it is the seed file (`en-us` only,
   empty body).

### 2. Add the RXLOCALE seed row

File: `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml`.

Insert a new `<row>` inside the `<table name="RXLOCALE">` block (around
line 12054). Schema:

```xml
<row onTableCreateOnly="no" action="i">
    <column name="LOCALEID"><next-id></column>
    <column name="LANGUAGESTRING"><code></column>
    <column name="DISPLAYNAME"><Native endonym (English fallback)></column>
    <column name="SORTORDER"><gaps-of-10></column>
    <column name="DESCRIPTION"><English sentence describing region/scope></column>
    <column name="STATUS">1</column>
    <column name="VERSION">0</column>
</row>
```

Conventions observed in the existing 17-row block:

- `SORTORDER` increments by 10 per family (`10`, `20`, `30`…) and by 1
  inside regional variants (`40`, `41`, `42`, `43` for `es*`).
- `LOCALEID` is the next integer after the highest existing id, never
  reused.
- `STATUS=1` makes the locale visible by default. Use `0` only when the
  locale must ship seeded but hidden.
- Avoid backslashes / special XML characters in `DISPLAYNAME` /
  `DESCRIPTION`.

The runtime loader references `RXLOCALE` via `sys_i18nSupport.xml`,
`sys_psxCms.xml`, `sys_AutoTranslation.xml`, and the Designer
cataloger; no other seeding point is required.

### 3. Update locale-aware consumers (only if a regional variant)

Skip this whole step for a generic code (`es`, `hi`).

a. **Calendar widget picker** — only if the new locale is itself a
   calendar region, **or** your UI team has confirmed that a picker
   entry is required for this locale:

   `modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--rxconfig/Widgets/percCalendarTwo.xml`

   Add an `<EnumValue value="<code>" display_value="<endonym (Locale)>" />`
   inside the `<Enum>` block. The existing `hi-in` row (line 130) is a
   good template.

   The file under
   `…/Resources/percCalendarTwo.xml` is the **user-resources copy**
   (legacy / hand-edited classic UI); the canonical source of truth is
   the `Widgets/` file. Decide which copy to update based on
   `Widgets/` ⇄ `Resources/` lockstep:

   - **(a) Lockstep confirmed** — the two copies are still kept in
     sync by the package consumer team. Update **both** so the modern
     widget and the legacy UI stay visually aligned.
   - **(b) Not in lockstep** — the legacy `Resources/` copy is
     considered canonical for the classic UI and is no longer
     mirrored into the modern package. Update **only `Widgets/`**
     (the source of truth) and leave `Resources/` untouched. Do
     **not** edit `Resources/` from this skill; that copy is
     maintained by the package / classic-UI team on its own cadence.

b. **Lucene analyzer** — only if the new language is not already covered
   by the analyzer's language branch table:

   `system/src/main/java/com/percussion/search/lucene/analyzer/PSLocaleSpecificLuceneAnalyzer.java`

   The switch (around lines 115-181) maps the **primary sub-tag** to a
   Lucene `Analyzer`. If your code's primary sub-tag is not yet
   present, add a new `case "<primary>":` with the appropriate analyzer
   constant. Note: the consumer reads the **primary sub-tag**, not the
   full BCP-47 code, so `es-mx` falls through the `case "es":` branch.

c. **Anything new?** Grep for the new code before opening the PR — if a
   `case "<primary>":` or `<EnumValue>` is already missing for a brand
   new primary tag, you have likely identified another touch-point.
   Document in the PR description.

### 4. Back-fill translations via the canonical script

```bash
# From repo root. Confirm what is missing first (no Docker required).
python3 modules/perc-i18n/scripts/i18n_translate.py \
    --target <code> --dry-run

# Then translate. Re-runs are resumable.
python3 modules/perc-i18n/scripts/i18n_translate.py \
    --target <code>

# Scope or limit if the matrix is too big to translate in one pass.
python3 modules/perc-i18n/scripts/i18n_translate.py \
    --target <code> --file CmsUi.tmx --limit 200
```

Honored contracts (from `modules/perc-i18n/scripts/README.md`):

- Source is always the `en-us` `<seg>` inside each `<tu>`; do **not**
  translate English source strings by hand.
- Placeholder-only segments (`{0}`, `{1,2,3}`) are passed through
  unchanged; no manual fix needed.
- Cache lives at `modules/perc-i18n/scripts/.cache/i18n_translate.json`.
  Use `--force` to bypass on audits.
- 429 from `soimort/translate-shell` triggers exponential backoff
  (2s → 60s cap, ±20% jitter, max 5 attempts). Do **not** retry outside
  the script; rate limits are shared across the team.
- XML escaping is done on write (`xml.sax.saxutils.escape`). Do not
  inline-escape `<seg>` text in the TMX; let the script.
- `--target` accepts the canonical BCP-47 hyphen form only; `de_DE`
  fails loudly.

### 5. Validate, build, test

```bash
# XML well-formedness for the two files you actually edited.
# (libxml2 xmllint if installed; otherwise the portable Python check.)
xmllint --noout modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx
xmllint --noout modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx

# Cross-platform portable alternative if xmllint is not on PATH:
python3 -c "import xml.etree.ElementTree as ET; ET.parse('modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx')"
python3 -c "import xml.etree.ElementTree as ET; ET.parse('modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx')"

# Standalone module builds (per root AGENTS.md "Pre-PR Maven verification"
# hard gate; do **not** use root reactor for this change).
cd modules/perc-i18n
../../mvnw clean install

cd ../perc-distribution-tree
../../mvnw clean install

# i18n unit tests
cd ../perc-i18n
../../mvnw test

# Pure-Python tests for the translation tool (no Docker needed).
python3 modules/perc-i18n/scripts/test_i18n_translate.py
```

Pipeline invariants:

- The runtime loader scans BOTH `rxconfig/I18n/` (legacy uppercase) and
  `rxconfig/i18n/` (lowercase, where the Maven build extracts the
  canonical files). Do **not** rename one to drop the other.
- `mvn clean install` on `perc-i18n` does **not** invoke the
  translation script; that is a developer step, not a build gate.
- No new compiler / surefire / enforcer / Spotless / javadoc warnings
  on the touched files. Run diff vs. an unchanged baseline if unsure.

### 6. Documentation touch

Per `perc-i18n/AGENTS.md` rule 8:

1. `modules/perc-i18n/AGENTS.md` — append to the **Quick Reference**
   17-locale matrix (keep alphabetical order; preserve the `es` / `hi`
   generic-and-regional commentary).
2. `modules/perc-i18n/README.md` — update the **Canonical 17-Locale
   Matrix** table; add the new code to the right family row.
3. If you added a Lucene branch or a calendar widget entry, link to the
   relevant file from the PR description (no separate docs file needed
   unless an existing page lists consumers).

## Common task variants

### I just want to refresh an existing key in an existing language

Do **not** re-add a locale; that's not this playbook. Use:

```bash
# Dry-run to see what will change.
python3 modules/perc-i18n/scripts/i18n_translate.py \
    --target de-de --file CmsUi.tmx --dry-run

# Force retranslates the chosen file, ignoring the cache.
python3 modules/perc-i18n/scripts/i18n_translate.py \
    --target de-de --file CmsUi.tmx --force --limit 50
```

Hand-editing a single `<tuv>` is allowed for review fixes but should be
followed by a re-run of `i18n_translate.py --force` for the affected
key so the script's cache stays in sync with the committed file.

### I want to add a translation unit (new key) without adding a locale

That is the *Adding a New Translation Unit* flow in
`perc-i18n/AGENTS.md`. You:

1. Edit `CmsUi.tmx` or `SystemResources.tmx` (never `ResourceBundle.tmx`).
2. Follow the key naming convention (`perc.ui.*` for CmsUi,
   `psx.ce.*` for SystemResources).
3. Always include `<tuv xml:lang="en-us">`.
4. Run `i18n_translate.py --target <each existing language>` to
   back-fill the rest of the matrix.
5. Validate, rebuild, test, then Erlang review.

### Removing an existing locale

Reverse the checklist: delete the header line in both canonical TMX
files, drop the `RXLOCALE` row (do **not** hard-delete; set
`STATUS=0` if existing user content may reference it), remove the
calendar `EnumValue` and any per-locale `case` in the Lucene analyzer,
then re-translate remaining keys (the script writes only **missing**
TUVs by default, so removed TUVs are not regenerated). Erlang review
will block if string-by-key fall-through to English is not documented.

## Style, conventions, and cross-cutting rules

- **Inline `<tuv>` is the only supported layout.** Per-locale sibling
  files (`de_DE.tmx`, `hi_IN.tmx`) are ignored by the cache and forbidden.
- **Duplicate `<tu tuid="...">` entries**: runtime last-wins via
  `PSTmxResourceBundle.addResourcesToCache`. Always resolve duplicates
  before commit.
- **Do not edit `ResourceBundle.tmx` body**; it is intentionally empty.
- **No invented APIs**. The translation script's CLI surface is the
  only contract; do not introduce new scripts that talk to a different
  translation provider without architecture review.
- **Secrets**: never commit `scripts/.cache/i18n_translate.json` to a
  PR branch — it is local state. The `.gitignore` covers it; if you
  accidentally `git add` it, `git rm --cached` it.
- **Cross-platform**: the script is `pathlib`-only and
  `subprocess.run([...])`-only; on Windows, `tmx` files use CRLF — the
  script preserves existing line endings. Do **not** rewrite to LF.

## Cross-references

- **Source of truth** — `modules/perc-i18n/AGENTS.md` (sections 1a, 2b,
  3, 4, 6, 8 in particular).
- **Translation CLI** — `modules/perc-i18n/scripts/i18n_translate.py`
  + `scripts/README.md`.
- **Distribution seam** — `modules/perc-distribution-tree/AGENTS.md`
  (especially the JDBC / assembly sections; not directly locale
  related but the build needs to succeed in that module).
- **System i18n runtime** — `modules/perc-i18n/src/main/java/com/percussion/i18n/PSTmxResourceBundle.java`
  (loader, `normalizeLang(...)`, cache invalidation).
- **Cross-platform / path rules** — root `AGENTS.md` →
  **Cross-Platform File I/O & Paths**. This skill touches
  filesystem paths only via `scripts/i18n_translate.py`, which is
  already portable; nothing new should be added without following the
  checklist.

## Pre-PR / pre-push gates

1. Re-read this skill and `modules/perc-i18n/AGENTS.md` once. Anything
   in §1-§6 you cannot tick has to be in the PR description as a
   "Justification for skipping X: …" entry.
2. Standalone clean installs for `modules/perc-i18n` and
   `modules/perc-distribution-tree` both **BUILD SUCCESS**.
3. `python3 modules/perc-i18n/scripts/test_i18n_translate.py` exits 0.
4. **Erlang review** report under
   `docs/ai-generated/code-reviews/<branch-slug>-erlang.md` with
   recommendation `approve`. Use the canonical review skill; this
   checklist is not a substitute.
5. Branch off `origin/development`; PR base is `development`; **never**
   force-push.
6. If you added a Lucene `case`, include the test that exercises a
   sample analyzer for the primary sub-tag (Erlang will flag a missing
   behavioral test as a bug).
