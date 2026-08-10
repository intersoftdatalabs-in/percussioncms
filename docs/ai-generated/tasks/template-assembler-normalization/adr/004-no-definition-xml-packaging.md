# ADR-004: No Page / Widget / Gadget XML definition files for product packaging

| Field | Value |
|-------|--------|
| **Status** | Accepted |
| **Date** | 2026-08-09 |

## Decision

When 8.2 product packages **ship**, they are **not** authored or maintained as:

- `rxconfig/Widgets/*.xml` widget definitions
- Page meta/definition XML dialects (region/widget blob as the authoring format)
- Per-gadget OpenSocial-style definition XML files

### Allowed transitional uses

1. **Upgrade input** — compile/convert tools read old XML.
2. **Time-boxed runtime shim** — load customer XML if modern package not present.

Product **source of truth** becomes:

```text
Content types + Templates (assembler + JEXL bindings + source)
  + Slots (incl. layout/styles) + Catalog metadata + Resources
```

## Evidence

- **Widgets:** 48 product XML defs under `perc-packages` (see widget inventory).
- **Gadgets:** registry XML remains; per-gadget definition files already largely absent from tree (see gadget inventory) — finish the job with a proper catalog.

## Consequences

- Compiler/upgrade tooling is mandatory before deleting package XML.
- Widget Builder / Design tools write modern format only.
- Package install/export paths must understand the new manifest shape (prefer extending existing package system).

## Component Package Manifest (schema v1.0)

Ship-format model and docs landed as Phase 3 slice 1 (#2750):

| Artifact | Location |
|----------|----------|
| Schema / field docs | [../component-package-manifest.md](../component-package-manifest.md) |
| Java model + IO + validation | `modules/perc-packages/.../manifest/PSComponentPackageManifest*.java` |
| Minimal fixture | `modules/perc-packages/src/test/resources/manifests/minimal-component-package.json` |

**Upgrade-input XML** remains compiler/shim only; **product ship format** is `component-package.json` plus content types, templates, slots, catalog metadata, and resources.
