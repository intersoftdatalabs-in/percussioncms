# Contract: URL validation decision (runtime)

**Feature**: 986-url-allowlist-config  
**Audience**: Implementers of server-initiated outbound HTTP(S) calls; security tests

## Entry points (existing public API — keep signatures)

|                  Method                   |                     Role                      |
|-------------------------------------------|-----------------------------------------------|
| `URLValidation.validateURL(URL)`          | Validate; throw `SecurityException` if denied |
| `URLValidation.validateURLString(String)` | Parse + validate; return `URL` or throw       |

Optional internal/test-visible: config built from explicit file paths.

## Inputs

- Candidate absolute URL (`http` or `https`)
- Loaded allow patterns + block patterns from install-root files (or empty lists if files empty/missing after failed seed)

## Outputs

| Result |                              Behavior                               |
|--------|---------------------------------------------------------------------|
| Permit | Method returns normally (`validateURLString` returns `URL`)         |
| Deny   | Throws `SecurityException` with non-secret message (no credentials) |

## Ordering (normative)

1. Reject non-`http`/`https`
2. Reject hard-coded reserved/metadata hosts (defense in depth)
3. Reject if **block** glob matches normalized URL
4. Permit if **baseline** (loopback any port; non-private host with unspecified port or 80/443)
5. Permit if **allow** glob matches (including private hosts / nonstandard ports)
6. Reject otherwise

## Logging

- Deny: log at warn/debug with host and reason code; never log passwords from URL userinfo if present (strip or redact)
- Ignore lone `*`: log warn once per load
- Seed create: log info with path
- Seed skip (exists): no error

## Compatibility

Call sites that already call `validateURL*` require **no** signature change. Behavioral change is policy data + removal of system-property overrides.
