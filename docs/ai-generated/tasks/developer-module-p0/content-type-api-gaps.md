# Content type design API — gap map (P0.2)

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 |
| **FR** | CD-01–CD-19 in `docs/developer-module/workbench-functional-inventory.md` |

## Surfaces available today

| Surface | Capability | Path / type |
|---------|------------|-------------|
| Public REST list | Catalog name/label/description/guid | `GET /services/contenttypes` |
| Public REST detail (**new P0.2**) | Read-only field catalog + meta | `GET /services/contenttypes/{idOrName}` |
| Design SOAP (Workbench) | Full load/save/lock/create/delete | `IPSContentDesignWs` / `ContentDesignSOAPImpl` |
| Item def manager | Runtime CE definition cache | `PSItemDefManager.getItemDef` |

## Detail payload includes

- name, label, description, enabled, hideFromMenu, appName, editorUrl, guid  
- fields: name, label (display), origin (local/system/shared), dataType, control, searchable, fieldSet  
- child field set names  
- `designGaps[]` strings for client honesty  

## Explicit gaps vs Workbench Content Type editor

| Gap | FR IDs | Notes |
|-----|--------|-------|
| Field validation / visibility / editability / transforms | CD-05–CD-07 | On `PSField` + rule collections; not serialized |
| Control property + choice configuration | CD-07 | Only control **name** today |
| Item-level pre/post exits & validations | CD-09 | Properties tab |
| Allowed workflows / default workflow | CD-08 | `PSContentTypeWorkflow` associations |
| Allowed templates | CD-12 | Template–CT associations |
| Enable/disable as design action | CD-13 | Read `enabled` only |
| Shared field file editing | CD-15 | Separate object |
| System def | CD-16 | Separate object |
| Create / rename / delete / lock | CD-01, §5.2 | SOAP design only |
| Import/export CT | CD-14 | Workbench wizards |
| ACL | CD-19, §5.4 | Existing ACL REST may help later |

## Recommended next API work (P0.2b / P0.3)

1. `GET .../contenttypes/{id}/workflows` and template associations  
2. Optional JSON projection of field rules (read-only) from `PSItemDefinition`  
3. Design-session lock + `PUT` save via thin REST over `IPSContentDesignWs` (harder; needs session/user)  
4. Keywords CRUD via `IPSContentService` (already used by design WS)

## Client behavior

SPA shows gaps list under the field table so implementers do not confuse catalog view with full editor parity.
