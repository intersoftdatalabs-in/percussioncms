# Erlang review — issue #4428 Pipelines Slice D XSL result-page HTML apply

Independent of implementer. Change class: native IR persist + execute merge + Developer chrome + Playwright + product-docs.

## Findings

No hard-gate bugs found in this increment.

- Path/URI guard matches binary resource: no cloud schemes, no userinfo, no `..`, no absolute/drive/backslash paths.
- Bundled XSL is classpath-only; missing sandbox files fail closed (no invented HTML).
- TransformerFactory is `PSSecureXMLUtils.getSecuredTransformerFactory()` (external DTD/stylesheet access disabled).
- Execute applies XSL only when a result page is bound **and** the request asks for HTML (`.html` / `text/html`). JSON invoke still returns rows.
- Classic XML Applications are not rewritten (native IR overlay only).
- Tests: path guard, bundled merge marker, missing stylesheet, JSON skip, REST 400/403, adaptor persist, Vitest chrome, Playwright C5 spec.

## Cross-platform

Stylesheet URI join uses `Path` / `Files`; no hardcoded OS separators for filesystem paths. Tests do not assert Unix-only absolute shapes.

## Companions

REST DTO + adaptor interface + sitemanage impl + Spring test stub + resource Mockito tests + runtime unit tests + WebUI API/panel/Vitest + Playwright surface spec + product-docs 8.2 REST + Developer Pipelines.
