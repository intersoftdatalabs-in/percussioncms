# Virtual Sites & Git-Backed Documentation (8.2)

| Field | Value |
|-------|--------|
| **Status** | Active — Phase 1 implementation |
| **Created** | 2026-08-09 |
| **Type** | Product architecture |
| **GitHub epic** | [#2678](https://github.com/intersoftdatalabs-in/percussioncms/issues/2678) |
| **Phase 1 issue** | [#2679](https://github.com/intersoftdatalabs-in/percussioncms/issues/2679) |
| **Related** | Assembler epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626), Markdown assemblers [#2628](https://github.com/intersoftdatalabs-in/percussioncms/issues/2628) |

## North star

**Virtual Sites** are Sites whose content originates outside the Percussion content repository. Phase 1 delivers a **Git/Filesystem** adapter for product documentation under repo-root **`product-docs/`**, using Percussion assemblers as the site generator while Git remains system of record.

## Documents

| Doc | Purpose |
|-----|---------|
| [spec.md](./spec.md) | Product specification |
| [adr/](./adr/) | Architecture decision records |
| [contracts/](./contracts/) | Frontmatter and site config contracts |

## Code anchors

| Area | Path |
|------|------|
| Virtual site package | `system/services/.../virtualsite/` |
| Markdown render helpers | `system/services/.../assembly/impl/plugin/PSTextAssemblerSupport.java` |
| Product docs tree | `product-docs/` |
| Build script | `scripts/build-cms-docs.bat` / `scripts/build-cms-docs.sh` |

## Working model (8.2)

When landing a product feature, add or update corresponding Markdown under `product-docs/` in the same (or immediate follow-up) change set. Run the docs build locally or in CI to produce static HTML.
