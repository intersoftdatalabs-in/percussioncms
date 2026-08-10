# ADR-002: No repository ingest for Virtual Site items

## Status

Accepted

## Context

Ingesting Git documentation into the CMS as editable content items would create dual systems of record, workflow/permission conflicts, and drift from developer PR review.

## Decision

- Virtual Site content remains owned by the external source (Git/filesystem in Phase 1).
- Percussion discovers, projects, assembles, and optionally registers lightweight identities.
- Items are **not** created as normal editable CMS content items in the repository.

## Consequences

- Finder folder trees are not the authoring surface for Virtual pages.
- Identity uses source-defined stable ids (frontmatter `id`), not content IDs.
- Optional future visual editing layers must respect Git as source of truth if added.
