# Research: Configurable Allowed and Blocked URL Lists

**Branch**: `986-url-allowlist-config` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)

## R1 — Where validation lives today

**Decision**: Extend `modules/perc-security-utils` `URLValidation` / `URLValidationConfig` as the single choke point; all call sites (`PSProxyQueryResource`, `PSDocumentUtils`, `PSDtdTree`, SOAP HTTP, feeds URL validation paths that already call it) inherit new behavior without per-call changes.

**Rationale**: Spec and constitution (shared security in `perc-security-utils`). Existing tests in `URLValidationTest` already encode baseline SSRF rules.

**Alternatives considered**: Per-extension allowlists (duplicative); gateway-only policy (does not cover assembly-time server calls).

## R2 — Avoid circular module dependency for install path

**Decision**: Do **not** add a Maven dependency from `perc-security-utils` → `utils` (utils already depends on perc-security-utils). Resolve install-root files by:

1. Prefer paths passed into a loader / `URLValidationConfig` factory (tests inject temp dirs).
2. Production default path: `{rxdeploydir}/rxconfig/Server/{allowedUrls,blockedUrls}.properties` using system property `rxdeploydir` (same property name as `PathUtils.DEPLOY_DIR_PROP`), with optional auto-detect fallback only when that property is set at process start (server always sets it).
3. Optional early wiring from CMS server init to call `URLValidationConfig.setDefault(...)` after Rx dir is known (belt-and-suspenders).

**Rationale**: Keeps security-utils leaf-level; avoids cycle.

**Alternatives considered**: Move validation into `utils` (large blast radius); depend on PathUtils (cycle).

## R3 — Decision order (align with clarified spec)

**Decision**: Evaluate in order:

1. Scheme must be `http` or `https` (hard fail otherwise; allow file cannot enable other schemes).
2. If normalized URL matches any **block** pattern → **deny**.
3. If baseline permit (loopback any port; public host with port absent or 80/443; not private/metadata) → **permit**.
4. If normalized URL matches any **allow** pattern → **permit** (including private hosts / nonstandard ports).
5. Else → **deny**.

**Rationale**: Spec FR-006 / FR-006a / FR-007; additive allow; block wins; private unlock only via allow.

**Alternatives considered**: Exclusive allowlist (rejected in clarify); block only after allow (weaker for metadata).

## R4 — Pattern format and matching

**Decision**:

- File content: line-oriented; `#` starts a comment; blank lines ignored; active patterns are one absolute-URL glob per line (optional `pattern=` key ignored if present for future — prefer bare lines for simplicity).
- Normalize candidate: `new URL(urlString)` then rebuild comparable string as `scheme://host[:port]/path[?query]` with scheme/host lowercased; default ports may be omitted in the string used for matching when port is -1.
- Glob: translate `*` → `.*`, escape other regex metacharacters; match full string; case-insensitive for scheme/host portion by normalizing before match (path/query case-sensitive as in URL).
- Lone `*` on **allow** lines: drop with log warn; on **block** lines: also ignore (must not block everything by accident).

**Rationale**: Spec FR-004/FR-005; admin-friendly full-URL patterns.

**Alternatives considered**: Host-only match (rejected in clarify); AntPathMatcher dependency (unnecessary; small custom glob is enough).

## R5 — Remove unreleased system properties

**Decision**: Delete `URLValidationConfig.loadFromProperties()` usage of:

- `percussion.url.validation.allowed.ports`
- `percussion.url.validation.allowed.hosts`
- `percussion.url.validation.allowed.ip.ranges`
- `percussion.url.validation.allow.private.networks` (and related fields driven only by those props)

Replace “explicit host/port/range allow” with allow-file globs. Retain in-code baseline: loopback, standard ports 80/443 for non-private hosts, hard block of dangerous hostnames that are also seeded into default `blockedUrls.properties` (defense in depth: hard-coded metadata still blocked even if block file emptied).

**Rationale**: Clarify session — properties never released; files sole surface.

**Alternatives considered**: Deprecate with dual-read (rejected).

## R6 — Default file contents

**Decision**:

|           File           |                                                                            Seed content                                                                             |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `blockedUrls.properties` | Active patterns for known-dangerous targets currently hard-coded (`http://169.254.169.254/*`, `https://169.254.169.254/*`, `http://0.0.0.0/*`, etc.) plus comments. |
| `allowedUrls.properties` | Header comments explaining additive policy + private unlock; **commented-out** examples for weather/HR/i18n style URLs; zero active allow lines.                    |

**Rationale**: Spec FR-003 / FR-003a.

## R7 — Install / upgrade create-if-absent

**Decision**:

1. Ship defaults under distribution config source used by installer (same tree as other `rxconfig/Server` files).
2. Installer: copy with **replaceType=never** (existing `PSCopy` pattern for customer-owned server config) so upgrades never overwrite.
3. Runtime seed (server or security-utils first load): if either file is missing under install-root `rxconfig/Server/`, write product default bytes once; if present, do not write.

**Rationale**: Spec FR-008/FR-009; mirrors `installDistributionFiles.xml` “never” replace for server.properties-class files.

**Alternatives considered**: Install-only seed without runtime (risk if file deleted or partial upgrade); always overwrite (destroys customizations).

## R8 — Hard-coded vs file-only dangerous hosts

**Decision**: Keep a minimal hard-coded deny for cloud metadata / reserved addresses as defense-in-depth **and** ship the same entries as defaults in `blockedUrls.properties`. Emptying the block file does not re-open metadata.

**Rationale**: Spec wants defaults in file but also SSRF safety if operators clear the file.

## R9 — Documentation

**Decision**: Update release notes under project release-notes process for 8.2 and admin/help content describing files, decision order, wildcards, additive allow, private unlock, no system properties. Point operators at `rxconfig/Server/`.

**Rationale**: FR-010/FR-011.

## R10 — Testing strategy

**Decision**:

- Unit tests in `perc-security-utils` with temp directories (no full server).
- Cover: baseline public allow; private deny without allow; private allow with pattern; block wins; lone `*` ignored; nonstandard port with allow; scheme reject; seed create-if-absent / no-overwrite.
- Keep consumer SSRF tests (proxy, document utils) green; add one allow-pattern integration case if feasible without full server.

**Rationale**: Constitution III; FR-013.
