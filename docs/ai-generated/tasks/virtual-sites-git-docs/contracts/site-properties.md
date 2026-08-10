# Virtual Site property keys

Used on `IPSSite` / `PSSiteProperty` without requiring new `RXSITES` columns in Phase 1.

| Key | Required for Virtual | Values |
|-----|----------------------|--------|
| `virtual.sourceKind` | Yes | `git-filesystem` (Phase 1). Other kinds later. Blank or `repository` ⇒ traditional repository Site. |
| `virtual.rootPath` | Yes | Filesystem path to Virtual Site root (`product-docs` checkout). Prefer absolute; blank is treated as unset. |
| `virtual.configFile` | No | Default `_config.yaml`. |
| `virtual.siteKey` | No | Key for participant registry; default site name, else `default`. |

Helper: `com.percussion.services.virtualsite.PSVirtualSiteHelper`.

Operator workflow (build + link report): [../operator-build-and-link-report.md](../operator-build-and-link-report.md).
