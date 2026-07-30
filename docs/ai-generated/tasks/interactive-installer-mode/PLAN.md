# Plan: Interactive Installer Mode (CMS + DTS)

**GitHub issue:** [#1513](https://github.com/intersoftdatalabs-in/percussioncms/issues/1513)  
**Feature branch:** `feat/1513-interactive-installer`  
**Process:** Issue + feature branch implementation — **not** full Speckit  
**Branch base:** `development`  
**Status:** CMS Phases 1–3 + DTS Phase 4 implemented on branch (single PR)  
**Primary modules:** `modules/perc-distribution-tree` (CMS preinstall), `deliverytiersuite/.../delivery-tier-distribution` (DTS preinstall)  
**Related work already landed:**

|       Area        |                                                  What exists today                                                   |                Gap for interactive UX                |
|-------------------|----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| Install path      | Required first CLI positional arg                                                                                    | No prompt if omitted                                 |
| Java home         | Discovery + multi-candidate prompt (`JavaInstallSelection`, issue #1340 / specs/991)                                 | Only when path already known; not step 1 of a wizard |
| DB target         | Fully parameter-driven: `--dbprops`, `--db.*`, env file, env vars, default H2 (`DbInstallConfigResolver`, specs/006) | No guided multi-step capture                         |
| DB test           | ANT task `PSValidateRepositoryConnection` **after** files are written (new install)                                  | No optional **pre-install** test in the wizard       |
| Summary / confirm | None — install proceeds once args parse                                                                              | No review/confirm step                               |
| Silent mode       | `--silent` / `--no-tty`                                                                                              | Keep as hard non-interactive path                    |

---

## Problem

CLI installers currently require operators (or automation) to pass everything up front:

```text
java -jar PercussionCMS.jar <installDir> [--dbprops=...] [--db.type=...] ...
java -jar PercussionDTS.jar  <installDir> [--db.type=...] ...
```

If `installDir` is missing, the process exits with a short usage message. Interactive pieces exist only as **islands** (Java multi-home pick; upgrade “clean obsolete dirs” y/N). There is no end-to-end walkthrough for a human at a TTY.

---

## Goal

When a **TTY is available** and the operator did **not** pass `--silent` / `--no-tty`, run a linear console wizard that collects and confirms configuration, then hands the same resolved inputs into the existing install pipeline (no second configuration system).

When silent / no TTY / fully parameterized automation runs: **behavior unchanged** (parameters + defaults; no new prompts).

---

## Target interactive flow

```text
┌─────────────────────────────────────────────────────────────┐
│  0. Mode detection                                          │
│     TTY && !silent  → interactive wizard                    │
│     else            → current CLI/env/dbprops path only     │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  1. Installation directory                                  │
│     Prompt if positional path omitted                       │
│     Default: cwd or last known? (decide in spec; start      │
│     simple — no default, require explicit path)             │
│     Validate: writable parent, refuse unsafe paths          │
│     Detect upgrade vs new install (Version.properties)      │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  2. System Java JRE/JDK directory                           │
│     Reuse JavaInstallSelection (discover eligible 21+)      │
│     Unattended -Dperc.java.home still wins                  │
│     Multi-candidate menu; single → auto-select; zero → fail │
│     Optional: allow typed path override if not in list      │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Database connection (new install only; multi-step)      │
│     Skip / preserve-on-upgrade when existing repo config    │
│                                                             │
│     3a. Backend choice (wizard menu):                       │
│         • Embedded H2 (default demo / small site)           │
│         • SQL Server Express (small-site path — see note) │
│         • External MySQL/MariaDB | SQL Server | Oracle |    │
│           PostgreSQL                                        │
│     3b. H2: no further DB fields                            │
│     3c. Express / external: host, port, name/service,       │
│         schema, user, password (masked), SSL flags          │
│     3d. Alt: load from --dbprops / path prompt              │
│         (file path short-circuits field prompts)            │
│     Map answers → same Map/ResolvedDbConfig as today        │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Test database connection (optional)                     │
│     Offer [Y/n] for external backends only                  │
│     Reuse connection logic (extract pure helper from        │
│     PSValidateRepositoryConnection / InstallUtil)           │
│     Success → continue; failure → re-edit DB / abort        │
│     Never print passwords                                   │
└───────────────────────────┬─────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Summary + proceed                                       │
│     Show install path, upgrade/new, Java home, DB type,     │
│     host/name/user (no password), SSL summary               │
│     Confirm [Y/n]; N → back to edit step or exit            │
│     On Y → existing extract + ANT install path              │
└─────────────────────────────────────────────────────────────┘
```

**Upgrade path note:** Steps 3–4 should default to “use existing repository configuration” when `Version.properties` / repo props already exist. Optional later: interactive clean-install-dir already exists and can remain a side prompt after path detection.

---

## Design principles

1. **Single configuration contract** — Wizard fills the same structures `DbInstallConfigResolver` / `JavaInstallSelection` already produce. Prefer building a synthetic options map (or calling shared resolvers with prompted values) over forking ANT property names.
2. **CLI wins over prompts** — If the operator already passed install path, `--dbprops`, `--db.host`, `-Dperc.java.home`, etc., skip those wizard steps (or pre-fill and allow override only when interactively requested).
3. **Silent is sacred** — Docker/matrix/CI (`--silent` / no console) must not hang waiting for input. Same exit codes and validation messages as today.
4. **Console-only MVP** — No Swing/JavaFX GUI in this plan. `System.console()` / injectable `InteractivePrompt` (already used for Java selection).
5. **Cross-platform** — Paths via `Path` / `Path.of`; password read via `Console.readPassword()` when available; Windows + Linux + macOS.
6. **Secrets hygiene** — Never echo/log `PWD` / `db.password`; summary redacts credentials.
7. **Testability** — Wizard steps accept `InteractivePrompt` (or a small `LineReader` + `PasswordReader`) so unit tests drive answers without a real TTY.

---

## Proposed code shape (CMS first)

|                                  Piece                                   |                                                               Responsibility                                                               |
|--------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `InteractiveInstallWizard` (new, preinstall package)                     | Orchestrates steps 1–5; returns `WizardResult` (installPath, options map / ResolvedDbConfig, java selection already persisted or deferred) |
| `ConsolePrompts` (new, thin)                                             | Defaults, yes/no, menu, masked password, path validation                                                                                   |
| `DbInstallConfigResolver`                                                | Keep pure resolution/validation; add package-visible helpers if needed for “build options from answers”                                    |
| `JavaInstallSelection`                                                   | Already step 2; call from wizard after path known                                                                                          |
| `RepositoryConnectionTester` (new pure helper, or extract from perc-ant) | Pre-install connectivity test without ANT; share logic with `PSValidateRepositoryConnection`                                               |
| `Main.main`                                                              | If interactive mode and incomplete required inputs → run wizard; else current path                                                         |

DTS (`MainDTSPreInstall`) should get a **parallel** wizard (same UX language, DTS field set / production vs staging where relevant), reusing shared prompt helpers if we extract a tiny common module—or duplicate thin wizard code initially to avoid a new shared artifact (prefer extract only if duplication hurts).

---

## Precedence (interactive session)

Recommended order for each field:

1. Explicit CLI / system property already supplied
2. Env file / environment (existing `DbInstallConfigResolver` precedence)
3. Interactive answer
4. Product default (e.g. `h2`, SSL defaults)

---

## Non-goals (this plan)

- GUI / web-based installer
- Changing silent/automation contracts or sample `rxrepository.*.properties` format
- Replacing ANT install steps
- Shipping a bundled JRE
- Full Speckit constitution/spec unless product prioritizes a tracked feature branch

---

## Implementation phases

### Phase 0 — Spec spike (short)

- Capture UX copy defaults (menus, defaults, cancel keys).
- Confirm DTS parity scope (same 5 steps vs DTS-specific production/staging).
- Confirm whether typed custom Java path is in MVP.

### Phase 1 — CMS wizard skeleton ✅

- Detect interactive mode in `Main`.
- Step 1: install directory prompt when missing.
- Step 5: summary + confirm (even if DB/Java still from CLI defaults).
- Unit tests with fake prompt.
- **Landed:** `InteractiveInstallWizard`, `InstallPrompt`, `SystemConsoleInstallPrompt`, wired from `Main`; tests in `InteractiveInstallWizardTest`.

### Phase 2 — Java step integration ✅

- Run `JavaInstallSelection` as wizard step 2 (before summary).
- Optional custom path entry (deferred — discovery + `-Dperc.java.home` covers MVP).

### Phase 3 — Database multi-step + optional test ✅

- `InteractiveDbConfigCollector` menu + structured fields (incl. SQL Server Express copy).
- `RepositoryConnectionProbe` best-effort preinstall probe (SKIPPED when driver not on classpath).
- ANT `PSValidateRepositoryConnection` remains authoritative after files are written.

### Phase 4 — DTS parity + docs ✅

- `InteractiveDtsInstallWizard` + collectors/probe in `delivery-tier-distribution`.
- Production vs staging wizard step.
- CMS + DTS READMEs document interactive mode.
- Matrix/Docker remain on `--silent` + flags.

### Phase 5 — Polish

- “Back” navigation between steps (nice-to-have).
- Upgrade-specific messaging.
- Help text when launched with zero args (print “interactive mode available” vs usage-only exit).

---

## Acceptance criteria (MVP)

1. **TTY + no install path:** operator is prompted for install directory; install can complete without any prior CLI args for a default H2 install.
2. **TTY + new install + external DB:** multi-step prompts produce a valid `ResolvedDbConfig` equivalent to `--db.type=... --db.host=...` etc.
3. **Optional DB test:** operator can test connectivity before extract/ANT; failure does not leave a half-written install.
4. **Summary:** operator sees non-secret options and must confirm before install work begins.
5. **Silent / no TTY / full CLI:** no new prompts; existing automation green.
6. **Passwords:** never printed in prompts (echo off), summary, or logs.
7. **Unit tests** cover wizard branching (defaults, skip-when-CLI-supplied, test-fail re-edit, confirm-no abort).

---

## Decisions (resolved)

| # |         Question          |                                                             Decision                                                              |
|---|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| 1 | Zero-arg + TTY            | **Enter wizard** (do not only print usage and exit). Usage still printed for `--help` / silent-missing-path cases as appropriate. |
| 2 | Final confirm default     | **Default Y for embedded H2**; **require explicit Y** for external / SQL Server Express (or any non-default backend).             |
| 3 | DTS production vs staging | **Include as a wizard step** on DTS interactive path (parity with knowing server type up front).                                  |
| 4 | dbprops vs fields         | **Offer menu:** “Enter fields” / “Load properties file”.                                                                          |
| 5 | Tracking                  | **GitHub issue + feature branch**; no Speckit constitution/spec/tasks set for this feature.                                       |

---

## Design note (open product choice): SQL Server Express on the “small site” path

**Idea (under consideration, not MVP-blocking):** On the interactive database step, alongside embedded H2, offer **SQL Server Express** as a guided option aimed at small deployments (**&lt; ~10 users**, **&lt; ~10 GB** repository), with clear sizing guidance in the prompt copy.

**Why it may matter:** If a customer later scales and corporate standard is SQL Server, starting on Express can **avoid a future H2 → SQL Server migration**. Express is still the product’s existing `sqlserver` backend path (host/port/db/user/password), not a second engine — UX can default localhost/named instance patterns and SSL defaults appropriate for Express.

**What this is *not* (unless we decide later):**

- Not replacing H2 as the zero-config default for demos/CI.
- Not auto-installing SQL Server Express (operator provisions the instance; installer only configures CMS/DTS to use it).
- Not a free license entitlement claim — copy must say operator supplies a licensed Express (or full SQL Server) instance.

**MVP stance:**

1. Implement interactive flow with backends already supported (`h2`, `mysql`, `sqlserver`, `oracle`, `postgresql`).
2. Add an Express-oriented **menu label / defaults / help text** for `sqlserver` (e.g. default host `localhost`, default port `1433`, schema `dbo`) without a separate `db.type`.
3. Capture full Express “productized small-site path” (docs, sample props, sizing guardrails, optional local discovery) as a **follow-up** on the same issue or a child issue after MVP lands.

**Risks / follow-ups to decide before over-investing:**

- Express edition limits (10 GB user DB, CPU/RAM caps) vs product growth messaging.
- Auth modes (SQL auth vs Windows/integrated) — today structured install leans SQL auth + password; Windows auth may be out of MVP.
- Whether DTS should share the same Express instance/database vs separate DB (likely separate schema/db names for prod practice).

---

## Suggested next step

1. Implement on `feat/1513-interactive-installer` against [#1513](https://github.com/intersoftdatalabs-in/percussioncms/issues/1513) (CMS Phase 1–3 first).
2. DTS parity (Phase 4) in a follow-up PR on the same branch or stacked PR.
3. Express menu copy + defaults can land with Phase 3 DB step; deeper Express packaging stays optional.

