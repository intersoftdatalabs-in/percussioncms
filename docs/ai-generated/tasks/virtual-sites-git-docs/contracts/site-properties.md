# Virtual Site property keys

Used on `IPSSite` / `PSSiteProperty` without requiring new `RXSITES` columns in Phase 1.

| Key | Required for Virtual | Values |
|-----|----------------------|--------|
| `virtual.sourceKind` | Yes | **Allow-list (Phase 1):** `git-filesystem` only. Blank or `repository` ⇒ traditional repository Site. Unknown values rejected by `validate`. |
| `virtual.rootPath` | Yes | Filesystem path to Virtual Site root (`product-docs` checkout). Prefer absolute; blank is treated as unset and fails validation when virtual. NIO `Path.normalize()`; reject empty / remaining `..` segments. |
| `virtual.configFile` | No | Default `_config.yaml`. Simple file name only (no path separators or `..`). |
| `virtual.siteKey` | No | Key for participant registry; default site name, else `default`. |

Helper: `com.percussion.services.virtualsite.PSVirtualSiteHelper` — call `validate(IPSSite)` before treating a Site as a safe Virtual source.

Product docs peers: `product-docs/8.2/admin/sites.md`, `product-docs/8.2/developer/virtual-sites.md`, `product-docs/8.2/reference/site-config.md`.

Operator workflow (build + link report): [../operator-build-and-link-report.md](../operator-build-and-link-report.md).
