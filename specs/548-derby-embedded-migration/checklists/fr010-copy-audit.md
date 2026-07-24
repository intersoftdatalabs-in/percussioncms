# FR-010 copy audit (T039) — new-install messaging

**Date**: 2026-07-24  
**Scope**: Installer/distribution docs and default templates must not claim **Derby** is the new-install default after #548.

## Fixed in this pass

| Path | Change |
|------|--------|
| `modules/perc-distribution-tree/README.md` | Default embedded = H2; Derby = migration/legacy only |
| `docs/docker/compose-dev.md` | Defaults `db.type=h2`; supported types include h2 |
| `scripts/install-cms-dev.py` | Default `PERC_DB_TYPE` / `DB_TYPE` → `h2` |
| `scripts/test_install_cms_dev.py` | Asserts `--db.type=h2` default |
| `delivery-tier-distribution/.../MainDTSPreInstall.java` | `DB_TYPE_DEFAULT=h2`; h2 treated as embedded (no host required) |

## Intentional remaining Derby references (not new-install default)

| Path | Disposition |
|------|-------------|
| Install `sqlDerby` / Derby NetworkServer branches | Migration + explicit `perc.db.type=derby` legacy path |
| `installDts.xml` `derbydata` mkdir / PSUpgradeDerby | Upgrade/migration residue |
| `specs/548-*`, bake-off, inventory | Design/docs for this feature |
| Historical `docs/ai-generated/code-reviews/*` | Historical PR notes; not operator install docs |
| `specs/006-installer-db-targets/*` | Prior feature; superseded by #548 defaults where conflict |
| `specs/992-react-content-explorer/*` | Dev-runtime notes; update when docker install path revalidated |

## Grep follow-ups (non-blocking)

- `deliverytiersuite/.../p13n-ds/src-sql/readme.txt` — sample/legacy p13n Derby text
- DTS test-beans comments saying “Apache Derby” where fixtures still use Derby for unit isolation
