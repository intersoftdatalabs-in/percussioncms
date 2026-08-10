# ADR-003: Virtual publish path (not fake content lists) in Phase 1

## Status

Accepted

## Context

Classic publishing uses `IPSContentListGenerator` → content IDs → JCR nodes → assembly → delivery. Virtual Markdown pages are not repository items. Faking content IDs and content types would be fragile and couple docs to repository schema.

## Decision

Phase 1 implements a **Virtual Site build/assemble path** that:

1. Discovers items via `IPSVirtualSiteSource`.
2. Loads Markdown + frontmatter.
3. Renders body with Markdown assembler helpers (`PSTextAssemblerSupport` / `markdownAssembler`).
4. Wraps layout theme HTML.
5. Writes static output and registers virtual participants.

Classic content-list / Edition UI parity for Virtual Sites is deferred.

## Consequences

- Assembler machinery is dogfooded without abusing content lists.
- Later phases may bridge Editions if product requires it.
- Offline/CI builds can run without a full CMS repository.
