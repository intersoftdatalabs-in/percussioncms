# Erlang review patterns (Percussion CMS)

> **Institutional review memory.** Loaded at the start of every Erlang review.
> Keep entries **generalized** (no one-off file:line noise). Promote only
> hard gates and high-signal recurring findings.
>
> Full reports live under `docs/ai-generated/code-reviews/` (not `tmp/`).
>
> Auto-harvest: `scripts/erlang-harvest-review-patterns.py` (see `--apply`).

## Hard gates (always scan)

- Missing **behavioral** unit tests for new/changed non-trivial logic
- Tests that only grep source strings / assert tokens without exercising behavior
- Non-portable filesystem path joins (`"/"` or `"\\"` concatenation) — use `Path` / `Files`
- Unix-only absolute roots (`/tmp`, `/var`, `/home`, …) or Windows-only `C:\…` in shared code/tests
- Multi-path lists split/joined with `:` or `;` only — use `File.pathSeparator`
- Path string equality / regex that assumes Unix shapes only (`^/.*`, raw `/` from OS `toString()`)
- Relying on case-insensitive path/import lookup (works on Windows, fails on Linux CI)
- Line-ending assertions that require `\n` only without normalizing `\r\n`
- Product-required automation that is Unix-shell-only with no `.bat`/`.cmd` or Maven/Java entry
- Secrets, passwords, tokens in code, tests, logs, or committed fixtures
- Empty catch / swallowed exceptions without log or justified ignore (user-facing → bug)
- False green: process or API reports success when child work failed (ignored exit codes / return values)
- Path containment / “safe path” checks must use a trusted root, not a parent derived from untrusted input
- Filename-only sanitizers are not path-traversal protection when callers pass full paths
- Duplicate method declarations (or other changes that prevent compilation) are hard bugs

## Recurring findings

### Installer / Ant / distribution

- Premature `Class.forName` (or similar) that bypasses InstallUtil custom JDBC/driver loader paths
- Outer preinstall / wrapper `Main` ignores Ant or child `processCode` → install fails in logs but process exits 0
- Upgrade vs clean-install gates (`do.install` and similar) missing or only structural string tests
- Maven `exec-maven-plugin` `<systemProperty>` must use `<name>`, not `<key>` (wrong element is a silent misconfig)

### Tests

- Structural / registry / XML string presence treated as sole proof of runtime behavior
- Happy-path-only coverage for validation, connect-failure, or security rejection paths
- Vacuous assertions (`message != null || cause != null`) that any exception would pass
- Tests that only assert “something was thrown” without SSRF/validation keywords are too weak for security helpers
- Tests must not depend on uncommitted fixtures or wrong absolute/base directories for path-safety cases

### Security / config

- Passwords or secrets on process command lines (`-D…`) instead of temp file (mode-restricted) or env
- SSRF / path / LDAP sanitizers without behavioral tests on rejection paths
- Broad whole-file CodeQL path excludes without documenting residual risk and runtime tests

### Cross-platform / I/O

- Hardcoded `/` path joins and Unix-only absolute path assertions that fail on Windows CI or installs
- Wrong-cased paths or imports that only pass on case-insensitive volumes (Windows / some macOS)
- Reflection or environment assumptions that break under module-only or Windows checkouts without `assumeTrue`
- Public helper Javadoc that contradicts implementation (e.g. nullability / parse contracts) misleads callers

### Deployer / packaging

- XML `fromXml` modernizations that drop attributes used for runtime type dispatch (e.g. `fileType` → `TYPE_ENUM`) silently default ints to `0` and break package install with well-formed / wrong-type / “missing” dependency-file errors even when archives are correct
- Modernization of null-check removals can invert keep/drop semantics (`if (x != null) remove` vs `if (x == null) remove`) — treat flipped conditions in package/association cleanup as hard bugs
- Never cast Spring-injected service interfaces to concrete `*Service` impls (JDK proxies); call interface methods only
- DOM attribute names are case-sensitive: shipped package XML may use legacy casing (e.g. `returntype` vs `returnType`) — deserializers must accept both when packages cannot be mass-rewritten
- Do not force-bump Hibernate `@Version` before `merge`/`save` (e.g. `setVersion(loaded+1)`): under Hibernate 7 this causes optimistic-lock failure and `UnexpectedRollbackException` that masks the root cause
- Do not null `@Version` on a managed entity then discard the reference: the entity stays in the persistence context, flush fails, and commit surfaces as UnexpectedRollbackException without an app-level throw

### Maintainability

- `StringBuilder.append(null)` → literal `"null"` in user-visible strings
- `Math.random` in security-sensitive or id-generation contexts (prefer secure random)
- Multi-copy shared WebUI / package assets edited in only one of several lockstep paths
- Orphaned javadoc left above renamed/extracted methods
- Logging that interpolates nullable returns without guards (NPE / `"null"` in logs)
- Swallowed exceptions that can mask broken-tree or partial-write state

## False-positive guards (do not flag)

- URL, URI, classpath resource, and ZIP entry paths that correctly use `/`
- Documentation examples that clearly describe one OS (still prefer portable samples)
- Intentional OS-specific branches behind `os.name` / `File.separator` with both sides covered

## How to update this file

### Automated (preferred when short-handed)

From the repo root (needs `gh` auth + network):

```text
# Full candidate report (always useful; safe)
python3 scripts/erlang-harvest-review-patterns.py

# Merge multi-PR themes into this file (default --apply path)
python3 scripts/erlang-harvest-review-patterns.py --apply

# Also merge single-PR CRITICAL hard-gate themes (noisier)
python3 scripts/erlang-harvest-review-patterns.py --apply --promote-critical

# Windows
scripts\erlang-harvest-review-patterns.bat --apply
```

best ones into permanent plain principles and drop the marker; re-run harvest
dedups against existing text.

Full workflow notes: `docs/ai-generated/code-reviews/README.md`.

### Manual

After a review with **bugs** or high-signal suggestions that generalize beyond
the current task:

1. Strip file names, ticket IDs, and one-off API names.
2. Add or reinforce a one-line principle under the right category.
3. Do **not** dump nits or every issue from the report.
4. Keep the briefing short (hard gates + ~15–40 recurring lines total is plenty).

