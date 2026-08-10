# ADR-001: Site retains name; Virtual Site = source kind

## Status

Accepted

## Context

Product channels (different websites, audiences, delivery targets) are already modeled as Sites. Renaming Site to Channel would churn domain language, UI, schema, and APIs without improving the external-content problem.

## Decision

- Keep the primary product term **Site**.
- External-origin content is a **Virtual Site**: a Site with a non-repository **source kind / adapter**.
- Do not introduce “Channel” as a replacement for Site.

## Consequences

- Site Properties / site manager remain the configuration surface.
- Docs and UI say: “A Site may be backed by the content repository or by an external source (Virtual Site).”
