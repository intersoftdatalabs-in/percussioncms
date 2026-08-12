# #2665 Live smoke: classic app GET `.json` + POST JSON body

| Field | Value |
|-------|-------|
| **Parent** | [#2662](https://github.com/intersoftdatalabs-in/percussioncms/issues/2662) |
| **Issue** | [#2665](https://github.com/intersoftdatalabs-in/percussioncms/issues/2665) |
| **Env** | H2 QA cell via `perc-devctl qa-up` (container `perc-matrix-cms-h2`, host port **9993**) |
| **Date (UTC)** | 2026-08-12 |
| **Operator** | night-issue-prs / Grok Build (grok-4.5) |

## Product bug found and fixed

Before this fix, live `GET …/resource.json` returned **HTTP 404** even though:

- `PSXmlDocumentJsonCodec` / `PSJsonContentParser` / `PAGE_TYPE_JSON` were present in `perc-system`
- Unit tests for codec/parser/page-type were green (#2660)

**Root cause:** `PSRequestPageMap.isMatch` only treated **`xml`** and **`txt`** as built-in document extensions when the requestor `MimeProperties` listed only `html`/`htm` (typical for stock system apps). Extension **`json`** failed the match → no dataset handler → 404.

**Fix:** allow **`json`** alongside `xml`/`txt` in `PSRequestPageMap.isMatch` (+ unit test `PSRequestPageMapTest`).

## Query GET smoke (PASS)

Admin session against `http://127.0.0.1:9993` (in-cell base `http://127.0.0.1:9992`).

| App | Resource | URL | Status | Content-Type | Notes |
|-----|----------|-----|--------|--------------|-------|
| `sys_commSupport` | `usercommunities` | `/Rhythmyx/sys_commSupport/usercommunities.xml` | 200 | `text/xml;charset=utf-8` | root `UserCommunities` |
| `sys_commSupport` | `usercommunities` | `/Rhythmyx/sys_commSupport/usercommunities.json` | 200 | **`application/json;charset=utf-8`** | root object key `UserCommunities` |
| `sys_commSupport` | `usercommunities` | `/Rhythmyx/sys_commSupport/usercommunities.txt` | 200 | `text/plain;charset=utf-8` | same XML body |
| `sys_psxCataloger` | `getSites` | `/Rhythmyx/sys_psxCataloger/getSites.xml` | 200 | `text/xml;charset=utf-8` | root `Sites` |
| `sys_psxCataloger` | `getSites` | `/Rhythmyx/sys_psxCataloger/getSites.json` | 200 | **`application/json;charset=utf-8`** | `{"Sites":{"Site":{"@name":""}}}` |
| `sys_DisplayFormats` | `getDisplayProperties` | `/Rhythmyx/sys_DisplayFormats/getDisplayProperties.xml?id=1` | 200 | `text/xml;charset=utf-8` | root `PSX_PROPERTIES` |
| `sys_DisplayFormats` | `getDisplayProperties` | `/Rhythmyx/sys_DisplayFormats/getDisplayProperties.json?id=1` | 200 | **`application/json;charset=utf-8`** | single root key |
| `sys_commSupport` | (extensionless) | `/Rhythmyx/sys_commSupport/usercommunities` + `Accept: application/json` | 200 | **`application/json;charset=utf-8`** | Accept negotiation (#2663 path) |

### Structure vs codec rules (usercommunities)

XML attributes map to `@…` properties; element text under structured nodes uses `#text`:

```json
{"UserCommunities":{"@communities_enabled":"yes","@community":"10","@username":"Admin","Community":{"@commid":"10","#text":"Default"}}}
```

Parity assertions (live script): `@communities_enabled`, `@username`, `Community/@commid`, `Community/#text` match the XML document.

## POST JSON body smoke

| Case | URL | Content-Type | Result |
|------|-----|--------------|--------|
| POST JSON body to query resource | `/Rhythmyx/sys_commSupport/usercommunities.xml` | `application/json; charset=UTF-8` | **200** `text/xml` (body accepted; not 415) |
| POST JSON body requesting JSON page | `/Rhythmyx/sys_commSupport/usercommunities.json` | `application/json; charset=UTF-8` | **200** `application/json` |
| POST XML body (control) | `/Rhythmyx/sys_commSupport/usercommunities.xml` | `text/xml; charset=UTF-8` | **200** `text/xml` |

### Full update-pipe round-trip

**Skipped on this H2 seed (explicit):**

- Query resources used above do not execute update pipes.
- Stock apps with `PSXUpdatePipe` on this install (e.g. `sys_commSupport` relation updates, `sys_Keywords` insert/update/delete) mutate shared catalog/ACL data and are **not agent-safe** for unattended overnight smoke.
- JSON **request** parsing remains covered by module unit tests: `PSJsonContentParserTest` (+ codec round-trip `PSXmlDocumentJsonCodecTest`).
- Live POST trials still prove the server **accepts** `Content-Type: application/json` without media-type rejection.

## Optional Playwright

Not required: classic app URLs are not a product UI surface for this residual.

## Reproduction commands

```bash
python docker/scripts/perc-devctl.py qa-up
# after Admin password from container passwords file:
# GET .xml / .json as above with session cookie
python docker/scripts/perc-devctl.py qa-down
```

In-cell evidence runner (this PR tree): `tmp/classic-json-live-smoke.py` (not a product module).

## Product bugs filed

None separate: the live 404 is fixed in this PR (routing allowlist for `.json`).

## Acceptance checklist

- [x] Query `.json` smoke documented with evidence
- [x] Update JSON body smoke documented (POST accept path + explicit skip for unsafe update resources)
- [x] Product bug (`.json` 404) fixed here and linked via parent #2662
- [x] Playwright optional — skipped (not required)
