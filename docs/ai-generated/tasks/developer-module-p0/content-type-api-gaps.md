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
- **allowedWorkflows[]** + **defaultWorkflow** (P0.2b)  
- **allowedTemplates[]** (P0.2b)  
- `designGaps[]` strings for client honesty  

## Keywords

| Surface | Path |
|---------|------|
| List keywords (+ optional choices) | `GET /services/keywords?includeChoices=true` |

Write (create/edit/delete keyword) not exposed yet.

## Explicit gaps vs Workbench Content Type editor

| Gap | FR IDs | Notes |
|-----|--------|-------|
| Field validation / visibility / editability / transforms | CD-05–CD-07 | On `PSField` + rule collections; not serialized |
| Control property + choice configuration | CD-07 | Only control **name** today |
| Item-level pre/post exits & validations | CD-09 | Properties tab |
| Edit workflow/template associations | CD-08, CD-12 | **Read-only lists available** |
| Enable/disable as design action | CD-13 | Read `enabled` only |
| Shared field file editing | CD-15 | Separate object |
| System def | CD-16 | Separate object |
| Create / rename / delete / lock | CD-01, §5.2 | SOAP design only |
| Import/export CT | CD-14 | Workbench wizards |
| ACL | CD-19, §5.4 | Existing ACL REST may help later |
| Keyword write | CD-17 | List only |

## Recommended next API work

1. Optional JSON projection of field rules (read-only) from `PSItemDefinition`  
2. Design-session lock + `PUT` save via thin REST over `IPSContentDesignWs`  
3. Keyword create/update/delete  
4. Templates/slots design list UI (public REST partially exists)

## Client behavior

SPA shows gaps list under the field table so implementers do not confuse catalog view with full editor parity.
