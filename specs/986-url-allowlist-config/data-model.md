# Data Model: Configurable Allowed and Blocked URL Lists

**Branch**: `986-url-allowlist-config` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)

## Entities

### URLPattern

| Field | Type | Rules |
|-------|------|--------|
| rawLine | string | Original line from file (for logging) |
| pattern | string | Glob pattern after trim; may contain `*` |
| active | boolean | False if comment, blank, or discarded lone `*` on allow |
| source | enum | `ALLOW` \| `BLOCK` |

**Validation**:
- Empty / `#` comment → not active
- Pattern equal to `*` → not active (warn); never “match all”
- Pattern should look like absolute URL when active (warn + skip if no scheme)

### URLListFile

| Field | Type | Rules |
|-------|------|--------|
| path | path | Install-root relative `rxconfig/Server/allowedUrls.properties` or `blockedUrls.properties` |
| kind | enum | `ALLOWED` \| `BLOCKED` |
| patterns | list of URLPattern | Loaded active patterns only |
| loadedAt | instant | For diagnostics |

**Lifecycle**:
1. **Missing** → seed with product defaults (create once)
2. **Present** → load; never overwrite on upgrade
3. **Reload** → process restart (no live reload required for v1)

### ValidationBaseline (code-defined, not a file)

| Rule | Behavior |
|------|----------|
| Scheme | Only `http`, `https` |
| Loopback hosts | `localhost`, `127.0.0.1`, `::1` (any port) → permit after block check |
| Public host + port -1 or 80/443 | Permit if not private/metadata and not blocked |
| Private IPv4 ranges / link-local metadata | Deny unless allow-pattern match |
| Hard metadata hosts | Deny even if block file emptied (defense in depth) |

### ValidationDecision

| Field | Type | Description |
|-------|------|-------------|
| outcome | enum | `PERMIT` \| `DENY` |
| reason | enum | `SCHEME`, `BLOCK_LIST`, `BASELINE`, `ALLOW_LIST`, `DEFAULT_DENY`, `HARD_BLOCK` |
| matchedPattern | string? | Pattern that fired, if any |
| candidateNormalized | string | String used for glob matching |

## Decision flow (state)

```text
candidate URL
    → normalize
    → if scheme not http(s) → DENY(SCHEME)
    → if HARD_BLOCK host → DENY(HARD_BLOCK)
    → if any block glob matches → DENY(BLOCK_LIST)
    → if baseline permit → PERMIT(BASELINE)
    → if any allow glob matches → PERMIT(ALLOW_LIST)
    → DENY(DEFAULT_DENY)
```

## Relationships

- One install has **one** allowed file and **one** blocked file.
- `URLValidationConfig` holds two lists of active `URLPattern`s loaded from those files.
- Call sites do not hold lists; they call `URLValidation.validateURL*` only.

## File content model (logical)

```text
# comment
# https://example.com/inactive-example/*

https://hr.internal.corp/api/*
http://169.254.169.254/*
```

- Active line = non-empty, not starting with `#` after trim.
- Example patterns in default **allow** file are written as comments.
- Default **block** file has active lines for dangerous targets.
