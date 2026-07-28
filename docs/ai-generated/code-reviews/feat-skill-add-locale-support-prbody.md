## Summary

Adds a new developer skill, `add-locale-support`, that wraps the existing
**Adding a New Language** workflow in
`modules/perc-i18n/AGENTS.md` §2b into a single checklist for AI agents
(or humans) to follow end-to-end.

## What the skill covers

- BCP-47 code naming, generic-vs-regional decision, Docker pre-flight
- Six-step checklist:
  1. Wire the new code into the `<header>` of `CmsUi.tmx` and
     `SystemResources.tmx`
  2. Add the `RXLOCALE` seed row in
     `modules/perc-distribution-tree/.../Installer/data/cmsTableData.xml`
  3. Update locale-aware consumers (calendar widget picker, Lucene
     analyzer branch) — only for regional variants
  4. Back-fill translations with `i18n_translate.py`
     (`soimort/translate-shell` via Docker, with cache + 429 backoff)
  5. Validate XML, run `mvn clean install` standalone on `perc-i18n` and
     `perc-distribution-tree`, run i18n unit tests
  6. Update `perc-i18n/AGENTS.md` Quick Reference and
     `perc-i18n/README.md` Supported Languages
- Variants for refresh-existing-language, new-translation-unit, and
  remove-locale flows
- Cross-references to `perc-i18n/AGENTS.md`, the script README, and root
  cross-platform path rules
- Pre-PR / pre-push gates including Erlang review

## Build / tests

This change is **docs-only** (one new SKILL.md). No Java, XML, or path
code is touched, so no Maven build is required. The Erlang review
sanity-checked the dry-run path against the existing canonical TMX files:

- `python3 modules/perc-i18n/scripts/i18n_translate.py --target de-de --dry-run` → 2262 keys would be inserted, 0 actually inserted (correct).
- `python3 modules/perc-i18n/scripts/test_i18n_translate.py` → 11 OK.
- `python3 -c "import xml.etree.ElementTree as ET; ET.parse(...)"` on both
  canonical TMX files → exit 0.

## Erlang review

Independent Erlang (strict) review:
`docs/ai-generated/code-reviews/feat-skill-add-locale-support-erlang.md` —
recommendation **approve**, gate **May commit/push: yes**. No bugs, two
**suggestions** captured as non-blocking nits (canonical `.sh` wrapper
form matches the AGENTS.md convention; one minor wording tightening on
the calendar picker pre-condition).
