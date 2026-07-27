# Contract: Install-root URL list property files

**Feature**: 986-url-allowlist-config  
**Audience**: CMS administrators, upgrade tooling, URL validation runtime

## Locations (install root)

|    File    |                   Path                   |
|------------|------------------------------------------|
| Allow list | `rxconfig/Server/allowedUrls.properties` |
| Block list | `rxconfig/Server/blockedUrls.properties` |

## Format

- UTF-8 text
- One pattern per line
- Lines whose first non-whitespace character is `#` are comments
- Blank lines ignored
- Active pattern: trimmed line that is not a comment
- Pattern language: glob over **normalized absolute URL** (`*` = multi-character wildcard)
- Lone `*` as the entire active pattern is **ignored** (allow and block)

## Semantics

| List  |                                                            Meaning                                                            |
|-------|-------------------------------------------------------------------------------------------------------------------------------|
| Allow | **Additive** permits beyond baseline (loopback; public host on default/80/443). Matching private/internal URLs are permitted. |
| Block | Deny if match; **wins** over baseline and allow.                                                                              |

## Upgrade / install guarantees

|    Condition     |               Behavior               |
|------------------|--------------------------------------|
| File missing     | Create from product default template |
| File exists      | Do not overwrite                     |
| Only one missing | Create only the missing file         |

## Default templates (product)

### allowedUrls.properties

- Header comments describing additive policy and private unlock
- **Inactive** (commented) examples only — no active allow lines

### blockedUrls.properties

- Active patterns covering known-dangerous destinations previously hard-coded (e.g. cloud metadata URL shapes)
- Comments documenting that operators may add more block patterns

## Non-goals of this contract

- JVM system properties for allow hosts/ports/ranges (removed)
- Enabling non-http(s) schemes via allow patterns
- Live reload without restart (v1: restart required)

