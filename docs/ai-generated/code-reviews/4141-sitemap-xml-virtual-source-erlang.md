# Erlang review: #4141 sitemap-xml Developer Sites dual-ship + chrome

**Branch:** `fix/issue-4141-sitemap-xml-virtual-source`

Independent pre-commit review of Cycle Verify residual #4141 (parent #2678 / chrome slice #4115).

## Summary

Cherry-picks the #4115 Developer Sites `sitemap-xml` source-kind chrome onto `main` and tightens `qa-deploy-webui` so a stale `cm/modern` bundle (csv/sql/http-json/object-storage/rss-atom/icalendar present, `option[value=sitemap-xml]` absent) cannot deploy. Build/Preview/Publish chrome stays hidden for `sitemap-xml`.

## Scope

- Prior report: `docs/ai-generated/code-reviews/4115-sitemap-xml-source-chrome-erlang.md` (chrome slice).
- Memory: #3893 / #3948 stale `perc-modern-ui.js` → old `developer-<hash>.js` (kind option missing in live select).
- Diff vs `origin/main`: WebUI form/panel/Vitest, perc-qa-automation spec + kind-option contract, product-docs 8.2, `docker/scripts/hot-deploy-webui-modern.py` + tests, perc-devctl help, docker/scripts README notes.

## Recommendation

Approve after standalone `WebUI` and `modules/perc-qa-automation` `mvnw clean install` and C5 Playwright surface on H2 QA with `qa-deploy-webui` of the rebuilt generated tree.

## Gate

May commit/push: yes (no bug, behavioral tests present, portable paths).

Cross-platform path review: hot-deploy dest remains absolute POSIX inside the Linux cell (`/opt/Percussion/...`); host src uses `Path` (`WebUI/target/generated-webui/cm/modern`). Tests reject backslash dest joins. Playwright rootPath `C:/sitemap-xml-docs` is a form field value, not an OS file join.

## Issues

None at `bug` severity.

- suggestion: keep `REQUIRED_KIND_MARKERS` in lockstep with `SOURCE_KIND_SELECT_VALUES` when adding the next Virtual Site kind (this PR adds `icalendar` + `sitemap-xml` to the gate that previously stopped at `rss-atom`).
- nit: `--skip-object-storage-check` flag name is historical; `--skip-kind-marker-check` alias already exists.

## Tests

- `docker/scripts/test_hot_deploy_webui_modern.py`: missing `sitemap-xml` fails even when older kinds are present; full marker set present; backticks; 22 tests OK.
- WebUI Vitest: `SOURCE_KIND_SELECT_VALUES` includes `sitemap-xml`; save/GET-roundtrip; Build/Preview/Publish hidden.
- Playwright: `developer-site-virtual-source.spec.js` option inventory + save/GET-roundtrip + live persist; helper message cites full `cm/modern` deploy.
