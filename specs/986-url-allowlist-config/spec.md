# Feature Specification: Configurable Allowed and Blocked URL Lists

**Feature Branch**: `986-url-allowlist-config`  
**Created**: 2026-07-16  
**Status**: Draft  
**Input**: [Issue #1205](https://github.com/intersoftdatalabs-in/percussioncms/issues/1205) — UrlValidator needs to support allowedUrl and blockedUrl configuration  
**Related**: PR #1198 (SSRF hardening via URL validation)

## Module Scope
- **Primary module(s)**: `modules/perc-security-utils` (shared URL validation used system-wide)
- **Secondary / integration modules**: CMS install/upgrade distribution (`rxconfig/Server/` property file packaging and upgrade create-if-absent), server consumers of URL validation (e.g. assembly / proxy query paths such as external resource lookups), release notes and end-user documentation
- **AGENTS files to apply**: root `AGENTS.md`; `modules/perc-security-utils` local AGENTS if present
- **User roles affected**: CMS administrators and operators (configure allow/block lists); content integrators and publishers (rely on configured external URL lookups at assembly/publish time); security/ops reviewers
- **Install / upgrade impact**: config — new files under install-root `rxconfig/Server/`; upgrade must create missing files without overwriting existing customer files

## Clarifications

### Session 2026-07-16

- Q: Default allow policy when allow file is empty or has no match → A: **Additive allowlist** — existing baseline permit rules remain (e.g. public hosts on standard ports, loopback); allow file adds additional permitted patterns; block list still denies matches and takes precedence over both baseline and allow.
- Q: Can an allow pattern alone unlock private/internal network targets? → A: **Yes** — a matching allow pattern overrides baseline private-range denial for that match only; block list and http/https-only rules still apply.
- Q: How do new files relate to URL-validation system properties? → A: **Remove system properties completely** (never released; no deprecation path). Move host/port/range/private-network style configuration into the new `allowedUrls.properties` / `blockedUrls.properties` files as the sole customer configuration surface for this policy.
- Q: What does a URL pattern match against? → A: **Full absolute URL (glob)** — patterns match a normalized absolute URL string (scheme + host + optional port + path + query as present) with `*` as a multi-character wildcard; lone `*` on allow is ignored.
- Q: What ships in default allowedUrls.properties? → A: **Comments + inactive examples** — documented sample patterns that do not actively allow anything until the administrator uncomments or edits them; blocked file still ships with active default dangerous targets.

## User Scenarios & Testing
Each story must be independently testable.

### User Story 1 - Preserve legitimate external lookups after upgrade (Priority: P1)

As a customer who upgrades to a release that enforces URL validation for server-initiated outbound requests, I need to allow specific external and internal service URLs I already use (weather API, HR status API, translation memory API, and similar) so assembly and publishing continue to work without silent or hard failures.

**Why this priority**: Without a customer-controlled allow path, post-upgrade breakage of production integrations is the core pain described in the issue.

**Acceptance Scenarios**:

1. **Given** a fresh or upgraded install with default configuration files present, **When** an administrator adds an allow pattern for a known external HTTPS weather API host (needed when baseline alone is insufficient), **Then** server-initiated requests to matching URLs are permitted by validation (unless blocked).
2. **Given** an allow pattern for an internal REST API host (including a private-network host that baseline would otherwise block), **When** content assembly performs that lookup, **Then** validation permits the request without requiring a separate global “allow all private networks” switch (subject to block list rules).
3. **Given** an allow pattern for an external translation-memory API, **When** localization/assembly uses that URL, **Then** validation permits matching requests.
4. **Given** a candidate URL that is neither covered by baseline permit rules nor by any allow pattern, **When** validation runs, **Then** the request is rejected.
5. **Given** a candidate URL already permitted by baseline rules (e.g. public host on standard ports) and not blocked, **When** the allow file is empty, **Then** validation still permits the request (allow file is additive, not exclusive).

### User Story 2 - Keep dangerous targets blocked by default (Priority: P1)

As a security-conscious operator, I need known-dangerous destinations (e.g. cloud metadata endpoints and other previously hard-coded blocks) to remain blocked by default after upgrade, without relying on administrators to reinvent the block list from scratch.

**Why this priority**: Equal priority to allowlists — the feature must not weaken the SSRF mitigation that motivated URL validation.

**Acceptance Scenarios**:

1. **Given** default blocked-list contents shipped with the product, **When** validation is asked to allow a known dangerous target (e.g. cloud instance metadata address), **Then** the request is rejected.
2. **Given** a customer has customized the blocked list on disk, **When** an upgrade is applied, **Then** the customer’s blocked-list file is not overwritten.
3. **Given** a URL matches both an allow pattern and a block pattern, **When** validation runs, **Then** the block takes precedence and the request is rejected.

### User Story 3 - Administrators manage URL policy via install-root files (Priority: P1)

As a CMS administrator, I need documented install-root configuration files for allowed and blocked URL patterns so I can control policy without code changes, using simple pattern wildcards where needed.

**Acceptance Scenarios**:

1. **Given** the product is installed, **When** an administrator inspects install-root server configuration, **Then** `allowedUrls.properties` and `blockedUrls.properties` exist under `rxconfig/Server/` (or are created on first upgrade/startup path that is responsible for seeding them).
2. **Given** an allow pattern uses `*` as a multi-character wildcard in a full-URL pattern (e.g. `https://api.example.com/v1/*`), **When** a candidate absolute URL matches that glob after normalization, **Then** validation treats it as allowed (unless blocked).
3. **Given** an allow entry that is only `*` (wildcard alone), **When** the configuration is loaded, **Then** that entry is ignored and does not allow all URLs.
3a. **Given** a pattern that includes scheme, host, path, and optional query wildcards, **When** only the host matches but the path does not, **Then** the pattern does not match (full-URL matching, not host-only).
4. **Given** documentation and release notes for the release that introduces this feature, **When** an administrator reads them, **Then** they can find how to configure allowed and blocked URL patterns, that file-based lists are the configuration surface (not JVM system properties), and the upgrade behavior for existing files.

### User Story 4 - Safe upgrade seeding of configuration (Priority: P1)

As an operator upgrading an existing deployment, I need default allow/block files created only when missing so my prior customizations survive upgrades.

**Acceptance Scenarios**:

1. **Given** an install that does not yet have the two property files, **When** upgrade (or the documented seed step) runs, **Then** both files are created: `blockedUrls.properties` with active product-default dangerous targets; `allowedUrls.properties` with comments and inactive example patterns only (no live allows until edited).
2. **Given** either file already exists with customer edits, **When** upgrade runs, **Then** that file is left unchanged (not replaced by product defaults).
3. **Given** only one of the two files is missing, **When** upgrade/seed runs, **Then** only the missing file is created and the existing file is left intact.

### Edge Cases
- Empty allow file: baseline permit rules still apply; only targets that would already fail baseline need allow patterns.
- Empty block file: product-default blocked entries must still be present after seed; if an operator empties the file intentionally, only explicit empty content remains (documented risk).
- Malformed lines in either file (invalid patterns are skipped or rejected with server-side log detail; they do not crash the server).
- Wildcard patterns that would be overly broad beyond a lone `*` (e.g. `http://*`); product rules must remain fail-closed for unintended open allowlists where specified.
- Overlapping allow and block patterns for the same host or path (block wins over baseline and over allow).
- Private-range host matched by allow but also by block (block wins).
- Private-range host with no allow pattern (baseline private-network denial still applies).
- Case sensitivity of hosts and schemes (document and apply one consistent rule; default: scheme and host matching case-insensitive, path as documented).
- Changes to files while the server is running (default: changes take effect after restart unless product already supports live reload for similar config — assume restart required).
- Relative URLs, non-http(s) schemes, and loopback targets (scheme restrictions and loopback policy remain defined and documented; non-http(s) remain disallowed unless product explicitly documents otherwise).

## Requirements
### Functional Requirements
- **FR-001**: The system MUST load allowed-URL patterns from install-root `rxconfig/Server/allowedUrls.properties` as the source of truth for **additional** customer-allowed outbound URL patterns used by shared URL validation (additive to baseline permit rules).
- **FR-002**: The system MUST load blocked-URL patterns from install-root `rxconfig/Server/blockedUrls.properties` as the source of truth for blocked outbound URL patterns used by shared URL validation.
- **FR-003**: Product defaults that were previously hard-coded as blocked destinations MUST be shipped as **active** default entries in `blockedUrls.properties` so a new install retains equivalent protection without requiring administrators to recreate the list.
- **FR-003a**: The default `allowedUrls.properties` shipped/seeded by the product MUST contain instructional comments and **inactive** example patterns only (commented-out or otherwise non-matching until an administrator activates/edits them). It MUST NOT enable open production allows by default.
- **FR-004**: URL patterns in either file MUST be matched against a **normalized absolute URL string** (scheme, host, optional port, path, and query as present on the candidate) using `*` as a multi-character wildcard (glob-style). Patterns are not host-only unless written that way by the administrator.
- **FR-005**: A pattern consisting solely of `*` in `allowedUrls.properties` MUST be ignored and MUST NOT permit all URLs.
- **FR-006**: Shared URL validation for server-initiated outbound requests MUST decide permit vs deny using, in order: (1) mandatory protocol rules (http/https only unless product documents otherwise), (2) block list (deny if match), (3) baseline permit rules (e.g. loopback; public hosts on standard ports as today), (4) allow list (permit if match). A URL that fails all permit paths MUST be denied.
- **FR-006a**: A candidate URL that matches an allow pattern MUST be permitted even when baseline private-network denial would reject it, provided it is not blocked and uses an allowed scheme. Allow patterns are targeted unlocks, not a global private-network open switch.
- **FR-007**: When a candidate URL matches a block pattern, the system MUST deny the request even if baseline or allow would otherwise permit it (block takes full precedence).
- **FR-007a**: Shared URL validation MUST NOT read JVM/system properties formerly used for allow-hosts, allow-ports, allow-IP-ranges, or allow-private-networks. That configuration surface is removed (unreleased code; no deprecation period). Equivalent operator control MUST be expressed only via the allow/block files (and residual hard baseline rules such as loopback and http/https-only as documented).
- **FR-008**: On upgrade (and for new installs), the product MUST create each of the two files if it does not exist, using product defaults for that file.
- **FR-009**: On upgrade, the product MUST NOT overwrite either file if it already exists.
- **FR-010**: Release notes for the introducing release MUST describe the security enhancement, default behavior, file locations, wildcard rules, block-over-allow precedence, additive allow policy, private-target unlock via allow patterns, removal of the unreleased system-property knobs, and upgrade non-overwrite behavior.
- **FR-011**: End-user / administrator documentation MUST describe how to configure allowed and blocked URL patterns for common integration scenarios (external APIs, internal APIs, localization services).
- **FR-012**: Validation failures MUST be diagnosable from server logs without exposing secrets (no passwords/tokens in log lines related to this feature).
- **FR-013**: Behavioral automated tests MUST cover allow match, block match, block-over-allow, ignore of lone `*`, default blocked targets, and create-if-absent / no-overwrite seed behavior (or an equivalent unit-level substitute for seed where install harness is unavailable).

### Key Entities
- **Allowed URL configuration file**: Install-root file listing permitted URL patterns for outbound validation.
- **Blocked URL configuration file**: Install-root file listing denied URL patterns; includes product-default dangerous targets.
- **URL pattern**: A single allow or block entry that may include `*` wildcards; evaluated as a glob against the candidate’s normalized absolute URL string (scheme/host/port/path/query).
- **URL validation decision**: Permit or deny outcome for a candidate URL, driven by protocol rules, baseline permit rules, allow list, and block list with defined precedence.

## Success Criteria
### Measurable Outcomes
- **SC-001**: On a representative upgrade path, 100% of runs create missing `allowedUrls.properties` and `blockedUrls.properties` when absent, and 0% of runs overwrite either file when already present with non-default content.
- **SC-002**: With only product default blocked entries, attempts to use known-dangerous destinations that were blocked before this feature (e.g. cloud metadata) are still denied in 100% of automated security regression cases.
- **SC-003**: After an administrator adds three distinct allow patterns for targets that would fail baseline alone (e.g. internal HR host, custom-port external API), validation permits matching sample URLs for all three; a control URL that is neither baseline-permitted nor allow-matched remains denied; a separate public baseline-permitted control URL remains permitted with an empty allow file.
- **SC-004**: A lone `*` entry in the allow file never results in “allow all” behavior in automated tests (0 false permits for arbitrary control URLs).
- **SC-005**: Release notes and admin documentation for the release include a dedicated section on allowed/blocked URL configuration that a reviewer can locate without prior knowledge of the implementation.
- **SC-006**: Automated regression coverage for allow, block, precedence, wildcard, and upgrade seed behaviors passes in the project’s continuous verification for the affected product areas before release.

## Assumptions
- Shared URL validation in `perc-security-utils` remains the single choke point for the server-side outbound URL checks described in the issue (e.g. assembly-time external lookups such as proxy query resource); other call sites already using that validation inherit the new lists without each reimplementing policy.
- Property files live under CMS install-root `rxconfig/Server/` as stated in the issue; DTS-specific packaging is out of scope unless the same shared library is later configured with an equivalent path for a DTS process.
- File format is a simple line-oriented properties-style list of URL patterns (comments and blank lines allowed); exact key/value vs bare-line format is an implementation detail so long as admin documentation matches the shipped defaults.
- **Pattern matching is full-URL glob** against a normalized absolute URL string; administrators craft patterns to the URL shape they actually call (not host-only unless the pattern is written that way).
- **Allow list is additive** to baseline permit rules (not an exclusive allowlist). Blocking takes precedence over both baseline and allow when a block pattern matches.
- **Allow patterns unlock private/internal targets** for matching URLs only; administrators need not enable a global “allow all private networks” flag solely to permit one internal HR/API host.
- **Unreleased JVM/system-property configuration for URL validation is removed entirely** (not deprecated). Capabilities previously sketched via `percussion.url.validation.*` system properties are expressed only through the allow/block files (plus documented baseline rules).
- **Default allow file is documentation-oriented** (comments + inactive examples); **default block file is enforcement-oriented** (active dangerous-target entries).
- Configuration changes require a server restart to take effect unless an existing live-reload mechanism is reused for these files.
- Non-http and non-https schemes remain disallowed by product policy (consistent with current SSRF guidance); the allow file cannot re-enable them.
- Loopback and other baseline “always allow” behaviors remain part of baseline permit rules and are documented alongside the allow/block files so customers understand full policy.
- “UrlValidator” in the issue refers to the product’s shared URL validation used for SSRF protection (not a third-party library name).
- Documentation updates include both release notes and the help/site administrator materials maintained for this product version.
- Related PR #1198 remains the historical context for SSRF validation; this feature softens operational breakage without reopening unrestricted outbound access.

## Out of Scope
- Redesigning the full dashboard gadget model or unrelated security validators (path injection, XSS, LDAP).
- A graphical UI for editing allow/block lists (file-based configuration is sufficient for this issue).
- Automatically discovering or migrating customer URLs from existing content into the allow list.
- Changing authentication or authorization for who may edit files on the server filesystem.
- Retaining or documenting a deprecation period for unreleased `percussion.url.validation.*` system properties (they are removed, not deprecated).
