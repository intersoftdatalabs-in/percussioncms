# Erlang review — #4430 Pipelines Slice D raw JSON/XML (no presentation)

Reviewer persona: independent of implementer. Date: 2026-09-09.

## Change class

Native pipeline IR result-page persist + execute presentation: `presentation=none` omits/disables XSL; JSON (`.json` / `Accept: application/json`) and XML skip XSL even when a stylesheet remains; HTML still applies XSL when bound. Developer chrome clear + Test JSON/XML. Product-docs + Playwright.

## Companions checked

| Layer | Artifact |
|-------|----------|
| system | `PipelineResultPageIr.presentation`, `PSPipelineXslMerge.wantsJson/wantsXml/shouldApplyXsl`, runtime skip + `PipelineExecuteResult.xml` |
| rest | `PipelineResultPage.presentation`, `requireWriteFields` allows blank URI when none, `TestPipelinesAdaptor` |
| sitemanage | `PipelinesAdaptor.putResultPage` omit vs disable; path-injection 400 |
| WebUI | PipelineDetailPanel clear / Test JSON / Test XML + Vitest |
| QA | `developer-pipelines-result-page-raw.spec.js` |
| product-docs | `product-docs/8.2/developer/rest.md`, `product-docs/8.2/admin/developer-pipelines.md` |

`IPipelinesAdaptor` implementors: `PipelinesAdaptor`, `TestPipelinesAdaptor` only (no extra reverse-dep types).

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| — | Bugs | None. JSON extension wins over HTML Accept; presentation tokens reject `..` / separators. Stylesheet URI still fail-closed on cloud/traversal when non-blank. |
| — | Behavioral tests | Runtime JSON/XML/none; REST blank-none; adaptor clear + injection; Vitest clear/JSON/XML. |
| — | Paths | `PSPipelineResultPagePath` uses `Path.of`; no OS-separator joins. |

## Hard gates

- Cross-platform I/O: pass
- Unit tests for new logic: pass
- Product-docs: pass (operator + REST)

No blocking findings.
