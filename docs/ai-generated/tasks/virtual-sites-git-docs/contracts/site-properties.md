# Virtual Site property keys

Used on `IPSSite` / `PSSiteProperty` without requiring new `RXSITES` columns in Phase 1.

| Key | Required for Virtual | Values |
|-----|----------------------|--------|
| `virtual.sourceKind` | Yes | `git-filesystem` (Phase 1). Other kinds later. |
| `virtual.rootPath` | Yes | Filesystem path to Virtual Site root (`product-docs` checkout). |
| `virtual.configFile` | No | Default `_config.yaml`. |
| `virtual.siteKey` | No | Key for participant registry; default site name. |

Helper: `com.percussion.services.virtualsite.PSVirtualSiteHelper`.
