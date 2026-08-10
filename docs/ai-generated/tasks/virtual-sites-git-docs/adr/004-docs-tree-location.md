# ADR-004: Product documentation tree at `product-docs/`

## Status

Accepted

## Context

Monorepo `docs/` already holds internal material (AI task notes, developer-module guides, policies). Putting product help under `docs/` would mix audiences and tooling.

Isolating product documentation early keeps Virtual Site content discoverable and separable from agent/dev trees.

## Decision

- Product documentation Virtual Site root is **`product-docs/`** at the repository root.
- Theme lives at `product-docs/_theme/`.
- Internal `docs/` remains for engineering/AI/policy material.

## Consequences

- Agents and developers update `product-docs/**` in lockstep with features.
- Build scripts and CI path-filter on `product-docs/**`.
- Earlier draft idea of `docs/cms/` is superseded.
