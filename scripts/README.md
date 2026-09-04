# scripts/

Repository-wide operational and helper scripts. Per the project's AGENTS.md, generated scripts MUST live under this directory (or the owning module's script directory). Scratch work uses `./tmp`; do not use system temp dirs.

## Scope (per spec 994-python-build-scripts)

All build-time scripts in this directory are cross-platform Python 3.9+ (FR-001). The migration delivers per-directory PRs (FR-001a); the `scripts/` directory is **Scope 1** and landed in **US2**. See [`specs/994-python-build-scripts/spec.md`](../../specs/994-python-build-scripts/spec.md) for the full in-scope/out-of-scope split and [`specs/994-python-build-scripts/contracts/cli-schemas.md`](../../specs/994-python-build-scripts/contracts/cli-schemas.md) for the CLI contract of each script.

Out of scope for spec 994 (must NOT be touched):
- Repo-root Maven wrapper (`./mvnw` / `mvnw.cmd`) — already cross-platform; use it for builds.
- Anything under `system/release/`, `system/installResources/`, `system/Tools/`, etc. — runtime scripts deployed with customer installations (FR-013).

## Scripts

### `build-cms-docs.bat` / `build-cms-docs.sh`

Build the **product documentation Virtual Site** (`product-docs/`) to static HTML using the
`system` module Virtual Site build service (`PSVirtualSiteBuildMain`).

- **Purpose**: Offline/CI dogfood of Git-backed docs assembly (Markdown + frontmatter → HTML).
- **Defaults**: site root = repo `product-docs/`; output = `tmp/product-docs-site/`.
- **Usage**:

  ```bat
  scripts\build-cms-docs.bat
  scripts\build-cms-docs.bat C:\path\to\product-docs C:\path\to\out
  ```

  ```bash
  scripts/build-cms-docs.sh
  scripts/build-cms-docs.sh /path/to/product-docs /path/to/out
  ```

- **Prereqs**: JDK 21, repo-root `mvnw`/`mvnw.cmd`, network once for Maven deps.
- **Design docs**: `docs/ai-generated/tasks/virtual-sites-git-docs/`.

### `ci-smoke-product-docs.bat` / `ci-smoke-product-docs.sh`

Path-filtered CI / local **smoke** around `build-cms-docs` (issue #2704).

- **Purpose**: Clean the output dir, run the docs build, and **fail** if no versioned
  `index.html` is emitted (default check: `tmp/product-docs-site/8.2/index.html`).
- **CI**: `.github/workflows/product-docs-build.yml` (ubuntu) installs `system` +
  upstream reactor SNAPSHOTs, then runs the `.sh` smoke script.
- **Windows operators**: use the `.bat` entrypoint for the same assertions; CI itself
  stays on ubuntu to control runner cost.
- **Usage**:

  ```bat
  scripts\ci-smoke-product-docs.bat
  scripts\ci-smoke-product-docs.bat C:\path\to\product-docs C:\path\to\out
  ```

  ```bash
  scripts/ci-smoke-product-docs.sh
  scripts/ci-smoke-product-docs.sh /path/to/product-docs /path/to/out
  ```

- **Failure path**: non-zero build exit (broken md/config/link ids) **or** missing
  index HTML → exit 1. See `product-docs/README.md` → **CI smoke**.
- **Failure artifacts**: after wipe, the smoke scripts **recreate** the output root and
  write `.ci-smoke-meta.txt` so the workflow `if: failure()` upload of
  `tmp/product-docs-site/` always has a path and at least one file (partial build
  output is preserved when the builder emits anything before failing).

### Definition XML inventory gates (G4 / #3026 Widget, #3581 Pages/Gadgets)

**Not a Python script.** Phase 5 criteria **G4** for product definition XML under Packages
ship paths is enforced in Maven Surefire for `modules/perc-packages`:

| Kind | Ship paths | API | Surefire |
|------|------------|-----|----------|
| Widget | `sys__UserDependency--rxconfig/Widgets/` | `com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory` | `PSWidgetDefinitionXmlInventoryTest` |
| Page | `sys__UserDependency--rxconfig/Pages/` and `rxconfig/Pages/` | `com.percussion.packages.pagexml.PSPageDefinitionXmlInventory` | `PSPageDefinitionXmlInventoryTest` |
| Gadget | `sys__UserDependency--rxconfig/Gadgets/` and `rxconfig/Gadgets/` | `com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory` | `PSGadgetDefinitionXmlInventoryTest` |

Shared Page/Gadget scanner: `com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory`
(combined `PAGE|GADGET|ALL` CLI). Tests fail if dummy non-waived fixture XML is introduced
under a non-waived package. **Widget** waiver is empty after perc.Test ship-exit (#3736);
**Pages/Gadgets** waiver is empty after perc.Test page dual-ship exit (#3737).
Cross-platform: `Path` / `Files` only (no hardcoded separators).

### Dual-ship page templateDef inventory gate (#3675)

**Not a Python script.** Fail-closed Surefire gate so product package-build cannot silently
re-introduce dual-ship page `*.templateDef` materialization (modern `pages/` + explicit
committed `page.installMode=dual-ship`; native is the default as of #3949). Waiver is **empty** after perc.Test page dual-ship exit
(#3737). Sibling #3674 leftover widget binaries are not dual-ship-retained. API:
`com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory` /
`PSDualShipPageTemplateDefInventoryTest`. Retirement checklist:
`docs/ai-generated/tasks/template-assembler-normalization/dual-ship-page-template-retirement.md`.

Optional CLI (after compiling the module):

```bat
cd modules\perc-packages
..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory ^
  -Dexec.args="src\main\resources\Packages"

..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.pagexml.PSPageDefinitionXmlInventory ^
  -Dexec.args="src\main\resources\Packages"

..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory ^
  -Dexec.args="src\main\resources\Packages"

..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory ^
  -Dexec.args="src\main\resources\Packages"
```

```bash
cd modules/perc-packages
../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory \
  -Dexec.args="src/main/resources/Packages"

../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.pagexml.PSPageDefinitionXmlInventory \
  -Dexec.args="src/main/resources/Packages"

../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory \
  -Dexec.args="src/main/resources/Packages"

../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.pagexml.PSDualShipPageTemplateDefInventory \
  -Dexec.args="src/main/resources/Packages"
```

Criteria doc: `docs/ai-generated/tasks/template-assembler-normalization/definition-xml-shim-removal-criteria.md`.

### M2 product/H2 zero-legacy-selection evidence (#3583 / #3738)

**Not a Python script.** Phase 5 **M2** product/H2 *selection* evidence is
enforced in Maven Surefire (`modules/perc-packages` + `projects/sitemanage`):

- Class: `com.percussion.packages.shim.PSProductPackageRootSelectionEvidence`
- Tests: `PSProductPackageRootSelectionEvidenceTest` (product tree + H2
  classpath materialize; fails if a dummy non-waived package selects
  `LEGACY_*`; waive list empty **or** `perc.Test` only)
- DAO harness: `PSWidgetDaoProductH2ZeroLegacySelectionTest` (blank
  `widgetDao.modernPackageRoots` + `rxdeploydir` materialize)
- Widget waiver: **empty after perc.Test ship-exit (#3736)** — until then, `perc.Test` /
  `PSWidget_TestProperties` only (#3738 dual-mode); after #3736, the residual is dropped
  and `PSWidget_TestProperties` must select modern-first.
- **Keep** `PSLegacyDefinitionXmlShim` (#2852). Do **not** treat a green scan
  as M2 PASS / removal-ready (M3 still FAIL).

Optional CLI (after compiling the module):

```bat
cd modules\perc-packages
..\..\mvnw.cmd -q exec:java -Dexec.classpathScope=compile ^
  -Dexec.mainClass=com.percussion.packages.shim.PSProductPackageRootSelectionEvidence ^
  -Dexec.args="src\main\resources\Packages"
```

```bash
cd modules/perc-packages
../../mvnw -q exec:java -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.percussion.packages.shim.PSProductPackageRootSelectionEvidence \
  -Dexec.args="src/main/resources/Packages"
```

### Third-party license inventory (Maven + npm merge)

**Not a Python script.** Merged inventory generation for issue #1689 lives in
`com.intsof.common:utilities` as
`com.intsof.common.utilities.license.ThirdPartyLicenseInventory` (generic Java API +
`main` for `exec-maven-plugin:java`). Product wiring:

- Root: `license-maven-plugin` → `THIRD-PARTY-MAVEN.txt`
- `perc-distribution-tree`: Java merge → `THIRD-PARTY.txt` + copy into assembly

See `src/license/README.md` and `modules/intsof-common-utilities/README.md`.

### `prune-stale-worktrees.py` / `prune-stale-worktrees.bat`

List or remove **stale git worktrees** left by agent sessions (Kilo / Grok / etc.).

- **Purpose**: Free disk after PRs merge. Full monorepo worktrees under `.kilo/worktrees/`, `~/.grok/worktrees/`, etc. fill disks quickly when not cleaned up. Complements root `AGENTS.md` → **Git worktree hygiene (HARD GATE)** and `.kilo/rules/worktree-hygiene.md`.
- **Usage**:

  ```bash
  # Dry-run (default): show keep vs remove using gh PR state
  python3 scripts/prune-stale-worktrees.py

  # Remove worktrees whose branches have MERGED or CLOSED PRs
  python3 scripts/prune-stale-worktrees.py --apply --force --delete-local-branches

  # Also drop worktrees with no linked PR
  python3 scripts/prune-stale-worktrees.py --apply --force --include-no-pr --delete-local-branches
  ```

  Windows:

  ```bat
  scripts\prune-stale-worktrees.bat
  scripts\prune-stale-worktrees.bat --apply --force --delete-local-branches
  ```
- **Keeps**: main worktree, current cwd worktree, locked worktrees, branches with **open** PRs (unless `--include-open`).
- **Prereqs**: `git`; `gh` authenticated (unless `--skip-gh` with `--include-no-pr` only).
- **Tests**: `python3 -m pytest scripts/test_prune_stale_worktrees.py -v` (or `python3 scripts/test_prune_stale_worktrees.py`).

### `nightly-i18n-refresh.sh` / `nightly_i18n_refresh.py`

Automated nightly i18n translation refresh for the 16 base locales.

- **Purpose**: Rotate through 16 base locales (`ar`, `bn`, `de`, `es`, `fr`, `he`, `hi`, `it`, `nl`, `pl`, `pt`, `ru`, `sv`, `te`, `tr`, `uk`) using `day_of_year % 16`, translate missing TUVs in TMX files, and create a PR with the changes.
- **Dedicated worktree**: Uses a persistent worktree at `~/.kilo/worktrees/nightly-i18n-refresh` (override with `--worktree <path>`). This isolates the cron job from developer checkouts and matches the worktree-hygiene rules in root `AGENTS.md`. The worktree is auto-created on first run from `origin/main` and reused thereafter.
- **Locale selection**: Default rotation via `day_of_year % 16`; override with `--locale <code>` for manual backfills.
- **Usage**:

  ```bash
  # Run with default locale rotation
  python3 scripts/nightly_i18n_refresh.py

  # Override to a specific locale (e.g., German backfill)
  python3 scripts/nightly_i18n_refresh.py --locale de

  # Dry-run (limits to 5 keys, no git operations)
  python3 scripts/nightly_i18n_refresh.py --locale de --dry-run

  # Use a custom worktree path
  python3 scripts/nightly_i18n_refresh.py --worktree /path/to/worktree

  # Verbose logging
  python3 scripts/nightly_i18n_refresh.py --verbose

  # Resume an interrupted run (keeps dirty TMX/cache on the locale branch)
  python3 scripts/nightly_i18n_refresh.py --locale tr --resume
  ```

  Or via the shell wrapper (recommended for cron):

  ```bash
  ./scripts/nightly-i18n-refresh.sh --locale de
  ./scripts/nightly-i18n-refresh.sh --locale tr --resume
  ```

- **Pre-flight checks**: Verifies `trans` (translate-shell) is on PATH, working tree is clean (in the worktree), on `main` branch (or detached HEAD at `origin/main` — git disallows two worktrees on the same branch), and the **active** `gh` account is authenticated (`gh auth status --active`). Inactive multi-account tokens that are expired do not fail this check. With **`--resume`**, the clean-tree and main-branch checks are skipped so partial work can continue; `trans` and `gh` are still required. `--resume` also requires `--locale`.
- **Resume**: After a hang or kill mid-translation, re-run with `--locale <code> --resume` from the same worktree. The wrapper leaves the dirty tree alone; `i18n_translate_direct.py` fills only missing `<tuv>`s and hits the on-disk cache for already-translated strings. Bare URL-only segments pass through without calling `trans` (avoids hangs on help-doc href keys).
- **Pipeline**: Ensures worktree exists → fetches origin/main → checks for existing PR → creates branch → runs `i18n_translate_direct.py --target <locale>` → runs `i18n_translate_direct.py --target <locale> --fix-matching-en` → commits → pushes → creates PR.
- **No spotless**: This wrapper does **not** invoke `mvnw spotless:apply`. The perc-i18n spotless config targets JSON (which we exclude due to the 4 MB translation cache) and the eclipseWtp XML formatter hangs on this environment's Eclipse OSGi classloader. TMX formatting is left to the translation script itself per `modules/perc-i18n/AGENTS.md`. The `scripts/cache/**` exclude in `modules/perc-i18n/pom.xml` is kept as defense in depth for manual spotless runs.
- **Locking**: Uses `flock` on `tmp/nightly-i18n.lock` to prevent concurrent runs.
- **PR labels**: Applies `operator:kilo` and `model:minimax-m3`.
- **Logging**: Writes to `~/logs/nightly-i18n-refresh.log` with rotation (10MB × 5 backups).
- **Platform**: Linux/macOS only. The wrapper imports `fcntl` for `flock`; Windows is unsupported (use WSL2).
- **Tests**: `python3 scripts/test_nightly_i18n_refresh.py`

### `derby-surface-inventory.py` / `derby-surface-inventory.bat`

Repo-wide inventory of Apache Derby surface area for feature **#548** (default embedded DB migration).

- **Purpose**: QC-001 / tasks T004–T005 — produce a dispositionable checklist of every `derby` / `sqlDerby` / NetworkServer / Liquibase `dbms=derby` / etc. hit for triage before GA.
- **Usage**:

  ```bash
  python3 scripts/derby-surface-inventory.py
  # Windows:
  scripts\derby-surface-inventory.bat
  ```
- **Output**: `specs/548-derby-embedded-migration/checklists/derby-surface-inventory.md`
- **Prereqs**: Python 3.9+
- **Notes**: Excludes `target/`, `node_modules/`, `.git`, `*.log`, and common binary suffixes. Assigns dispositions (`port`, `migration-only`, `docs-only`, `test-only`, `false-positive`, …). Re-run after large tree changes. Use `--fail-on-unknown` for QC-001 freeze checks (must exit 0 with zero `unknown` rows).

### `fetch-gh-code-scanning-alerts.py`

Fetch code scanning (CodeQL) alerts for a repository using the `gh` CLI and write a markdown report.

- **Purpose**: Reusable enumerator for the `004-zero-code-scanning-alerts` triage workflow and any future release-readiness check.
- **Usage**:

  ```bash
  python3 scripts/fetch-gh-code-scanning-alerts.py [--repo OWNER/REPO] [--state open|dismissed|fixed|all]
  # state: open | dismissed | fixed | all (default: open)
  python scripts/fetch-gh-code-scanning-alerts.py        # Windows
  ```
- **Output**: `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` — markdown list of alerts including alert number, rule ID, severity, file path + line, and message.
- **Prereqs**: `gh` CLI authenticated (`gh auth login`).
- **Notes**:
  - Pagination is handled automatically (`--paginate`, `per_page=100`).
  - After fetching, the script invokes `filter_stale_alerts.py` to write `alerts-stale-cache.md` (T007b).
- **Tests**: `python3 -m pytest scripts/test_fetch_gh_code_scanning_alerts.py -v`

### Matrix install smoke (docker/scripts — not this directory)

Ephemeral CMS/DTS install matrix for #1500 lives under **`docker/scripts/matrix-install-smoke.py`** (not here). It mounts `perc-distribution-tree*.jar` / `delivery-tier-distribution*.jar`, runs silent install, starts the product, probes login/health, records JSON under `docker/logs/`, and destroys the cell. See `docker/README.md`.

### `install-cms-dev.py`

Run the Percussion CMS installer ONCE on the host into a persistent `install_root/` directory. The docker **dev** runtime bind-mounts that directory into the `cms-dts` container at `/opt/Percussion/` so:

- the container's only job is to run `StartJetty.sh` (no in-container install);
- container restarts do **not** re-install (the install persists on the host);
- hot-deploys (jar swaps, config edits) are local file edits in `install_root/`, picked up by the container on the next `docker compose restart`.
- **Purpose**: One-time CMS install into `./docker/dev-data/cms-dts/install_root/` (default). Idempotent — skips install if the marker file is present.
- **Usage**:

  ```bash
  python3 scripts/install-cms-dev.py                  # one-time install
  python3 scripts/install-cms-dev.py --reset          # force reinstall
  python3 scripts/install-cms-dev.py --install-root /tmp/cms-install
  ```
- **Prereqs**:
  - JDK 21 on the host.
  - Built artifacts: `modules/perc-distribution-tree/target/perc-distribution-tree.jar` and `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar` (run `./mvnw clean install -DskipTests=true`).
  - For MySQL installs: the `mysql` compose service must be running and reachable on `localhost:3306`.
- **Output**: `RESULT:OK STEP:install LOG:<path>` or `RESULT:FAIL STEP:install LOG:<path>`.
- **Tests**: `python3 -m pytest scripts/test_install_cms_dev.py -v`

### `create-large-folder-fixture.py`

Create a single CMS folder with ≥500 children for the SC-005 perf UAT scenario of feature `992-react-content-explorer`.

- **Purpose**: Tasks.md T012b perf fixture scaffolding. Run on a test CMS instance to seed the fixture used for the SC-005 pass criterion (`p95 ≤ 10 s` on standard office network).
- **Usage**:

  ```bash
  python3 scripts/create-large-folder-fixture.py \
      --base-url https://cms.local:8443 \
      --user admin1 --password <redacted> \
      --fixture-path /Sites/PerfFixture --fixture-count 500
  ```
- **Output**: A folder `FIXTURE_PATH/PerfFixtureRoot` with `FIXTURE_COUNT` children (default `/Sites/PerfFixture/PerfFixtureRoot` × 500).
- **Prereqs**: `curl`, network reachability to a running CMS instance with admin credentials.
- **Tests**: `python3 -m pytest scripts/test_create_large_folder_fixture.py -v`

### `erlang-harvest-review-patterns.py`

Harvest GitHub PR **line review comments** (including closed/merged PRs) from `kilo-code-bot[bot]` (and optional other authors), cluster them into generalized themes, write a candidate report, and optionally auto-merge multi-PR themes into Erlang review pattern memory.

- **Purpose**: Keep Erlang's institutional review memory (`patterns.md`) fed from real Kilo/GitHub review history.
- **Usage**:

  ```bash
  python3 scripts/erlang-harvest-review-patterns.py [--apply] [--promote-critical]
  ```
- **Outputs**: `docs/ai-generated/code-reviews/harvest-candidates-YYYY-MM-DD.md` and, with `--apply`, appends selected bullets to `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`.
- **Prereqs**: Python 3.9+, `gh` CLI authenticated.
- **Tests**: `python3 -m pytest scripts/test_erlang_harvest_review_patterns.py -v`

### Other scripts in this directory

Each entry below has been ported to cross-platform Python 3.9+ with pytest coverage under `scripts/test_<name>.py`.

- `authenticate-sigstore.py` — Sigstore OIDC token retrieval + cache. Test: `test_authenticate_sigstore.py`.
- `gh-preflight.py` — pre-flight checks for `gh` CLI usage. Test: `test_gh_preflight.py`.
- `hot-deploy-local.py` — local hot-deploy helper for the CMS (jar modules + webui). Test: `test_hot_deploy_local.py`. H2 QA SPA copy (entry + hashed chunks, not host-install) is `docker/scripts/hot-deploy-webui-modern.py` / `perc-devctl.py qa-deploy-webui` (#3893 / #3948 / #4141 / #4123; refuses bundles missing quoted `object-storage`, `rss-atom`, `icalendar`, `sitemap-xml`, and `developer-am-new`).
- `resolve-conflicts.py` — git conflict resolution helper (ours / theirs / manual). Test: `test_resolve_conflicts.py`.
- `verify-no-finder-jsp-references.py` — CI-gate artifact-grep for spec 992 / FR-019a (modern Track B shell, hard-cut in PR #1390). Test: `test_verify_no_finder_jsp_references.py`.
- `verify-no-jqplot-vendor-refs.py` — CI-gate guard that the removed jqplot vendor library stays gone. Test: `test_verify_no_jqplot_vendor_refs.py`.
- `verify-no-cadf-legacy-auditlog.py` — Phase 2c (#2675) grep gate: `modules/jcadf-master` stays gone; no `com.ibm.cadf` / `com.percussion.auditlog` in production Java/POMs (docs/specs may still mention CADF historically). Test: `test_verify_no_cadf_legacy_auditlog.py`. Run: `python3 scripts/verify-no-cadf-legacy-auditlog.py` (Windows: `python scripts\verify-no-cadf-legacy-auditlog.py`).
- `verify-no-bare-ipsobjectstoreerrors.py` — Phase 2b (#3143 / parent #2616) freeze gate: no **new** bare production `IPSObjectStoreErrors` imports / `implements` / qualified constant uses outside an explicit allow-list (interface + utils `ObjectStoreErrorCode` bridge only; residual production list empty after #3175/#3176/#3177 — Desktop CX, Design ACL, deployer, LockManager, Handler retypes landed). Tests and comment-only mentions are ignored. Test: `test_verify_no_bare_ipsobjectstoreerrors.py`. Run: `python3 scripts/verify-no-bare-ipsobjectstoreerrors.py` (Windows: `python scripts\verify-no-bare-ipsobjectstoreerrors.py`); `--list-allowlist` prints residual paths.
- `verify-no-bare-ipserrors.py` — Phase 2b (#3586 / parent #2616) freeze gate for **remaining** `IPS*Errors` families (`IPSWebserviceErrors`, `IPSTransformationErrors`, plus other live catalogs under `system/` and `modules/`). Fails on **new** bare production call-sites outside an explicit allow-list: interface files named `IPS*Errors.java`, typed `*ErrorCodes.java` / `*ErrorCode.java` bridges, and exact residual paths in `ipserrors-residual-allowlist.txt` (sitemanage leftovers converted in #3584/#3846; system/services leftovers in #3847; servlet/WebDAV leftovers in #3848; webservices/transformation leftover call sites in #3585; leftover system/webservices SOAP/ws in #3861; leftover system/src/main cms builders in #3882; leftover cms handlers in #3883; leftover cms.objectstore + client in #3884; leftover cms.objectstore.server in #3900; leftover extensions-main in #3756/#3938; leftover system/src/main com.percussion.data (+ macro/vfs) in #3939; leftover system/src/main com.percussion.mail in #4017; leftover system/src/main com.percussion.cx in #4013; leftover IPSServerErrors DTD/workflow/relationship/date/CMS in #4142; leftover PSServer/handlers/parsers/console in #4150; leftover system/server command/cache/actions/clone/compare/config in #4153; leftover system search IPSSearchErrors in #4155; leftover system/src/main relationship.effect in #4156; leftover system install JDBC table-factory in #4157; leftover servletutils PSTomcatUtils in #4195; leftover PSDtdTree IPSXmlErrors in #4197; leftover system server webservices handlers in #4263; deployer catalog/client #3739, server/handlers #3740, PSDeployJexlUtils #4196). `IPSObjectStoreErrors` stays on the sibling #3143 gate. Tests and comment-only mentions are ignored. Do **not** add directory prefixes — a new file under the same tree must fail until it is listed with an issue link. Test: `test_verify_no_bare_ipserrors.py`. Run: `python3 scripts/verify-no-bare-ipserrors.py` (Windows: `python scripts\verify-no-bare-ipserrors.py`); `--list-allowlist` prints residual paths; `--dump-residuals` reprints current production leftovers for allow-list maintenance.
- `verify-codeql-analyzer-of-record.py` — asserts the advanced CodeQL workflow + config + playbook are in place and that the default-setup is `not-configured`. Test: `test_verify_codeql_analyzer_of_record.py`.

### `004-zero-code-scanning-alerts` workflow scripts

All converted to cross-platform Python 3.9+ (US2). All run from the repo root.

|           Python script           |                                                                                                       Purpose                                                                                                       | Spec ref |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `filter-stale-alerts.py`          | Filter out alerts whose file path is no longer in `git ls-files`; write the stale rows to `alerts-stale-cache.md` for audit. Invoked automatically by `fetch-gh-code-scanning-alerts.py` at the end of every fetch. | T007b    |
| `verify-triage-inventory.py`      | CI-lite check on `triage.md`: row count == open-alert count, every `false-positive`/`accepted-risk` row has non-empty `notes`, every `module_owner` is a path under `AGENTS.md`.                                    | T012     |
| `verify-distribution-archive.py`  | Rebuild `modules/perc-distribution-tree` (and `modules/perc-packages`) and assert none of the files listed in `tmp/gh-codeql-alerts/removed-files.txt` appear in the resulting JARs or `.ppkg` installer.           | T019     |
| `verify-valid-fixes.py`           | Assert every `triage.md` row with `disposition == valid` has a non-empty `linked_pr`.                                                                                                                               | T035     |
| `verify-suppressions.py`          | For every row in `suppressions.md`, grep the cited source line for the matching `// codeql[…] comment and `justification:` text.                                                                                    | T064     |
| `verify-pr-review-resolution.py`  | For every `linked_pr` in `triage.md`, query `gh pr view --json reviewThreads` and fail if any thread has `isResolved: false` (Constitution IX, `SC-007`).                                                           | T078b    |
| `test-verify-triage-inventory.py` | Self-test for `verify-triage-inventory.py` against `scripts/test-fixtures/triage-good.md` and `triage-bad.md`.                                                                                                      | T013     |

#### Usage

```sh
# Re-fetch and re-triage (weekly cadence per the alerts dir README).
python3 scripts/fetch-gh-code-scanning-alerts.py --repo intersoftdatalabs-in/percussioncms

# Pre-merge gates (run before merging a closing PR).
python3 scripts/verify-triage-inventory.py
python3 scripts/verify-valid-fixes.py
python3 scripts/verify-suppressions.py
python3 scripts/verify-distribution-archive.py
python3 scripts/verify-pr-review-resolution.py
```

#### Test fixtures

`scripts/test-fixtures/triage-{good,bad}.md` are minimal 4-row triage inventories used by `test-verify-triage-inventory.py`. Companion `scripts/test-fixtures/alerts-{good,bad}.md` files provide the alerts.md content for the row-count check. The "bad" fixture exercises the empty-notes and unknown-module_owner failure modes; the "good" fixture is the expected clean state.

### `release-audit/` package

Cross-platform Python port of the v8.1.x → 8.2 migration audit pipeline (spec 005-migrate-8.1.7-changes). Replaces the previous bash `release-audit.sh` + `lib/*.sh` + `tests/test_*.sh` layout with a Python package (`scripts/release-audit/*.py` + `scripts/release-audit/tests/*.py`).

- **Usage**:

  ```bash
  python3 scripts/release-audit/__main__.py --help
  python3 scripts/release-audit/__main__.py --from-tag v8.1.6 --to-tag v8.1.7 \
      --target-branch development --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
  ```
- **Subcommands**: `inventory`, `verdicts`, `backlog`, `report`, `port`, `all` (default).
- **Tests**: `python3 -m pytest scripts/release-audit/tests/ -v`
- **Note**: The directory name `release-audit/` contains a dash, which Python cannot import as a package name. Users invoke the entry point by file path (`python3 scripts/release-audit/__main__.py`) rather than via `python -m release_audit`.

## Conventions

- **Cross-platform Python only.** All scripts in this directory are Python 3.9+ per spec 994 FR-001. The legacy "Windows users run the `.cmd` counterpart" guidance has been retired (FR-011).
- **Stdlib only at runtime.** No third-party imports beyond pytest (which is declared in `scripts/requirements-dev.txt`, FR-006).
- **`pathlib.Path` everywhere.** No hardcoded `/` or `\\` separators in filesystem paths (FR-007; root AGENTS.md Cross-Platform File I/O & Paths).
- **`subprocess.run([...], shell=False, check=False, timeout=N)`** for every external invocation (FR-008). Never `shell=True`, `os.system`, `bash -c`, `cmd /c`.
- **Colocated pytest module** (`test_<name>.py`) per script (FR-009). Tests invoke scripts via `subprocess.run([sys.executable, str(script_path), ...])` per R4.
- **`## Behavioral Notes`** section in every script's module docstring enumerating deviations from the shell original (FR-009b).
- **`logging.getLogger(__name__)`** with format `%(asctime)s %(levelname)s %(message)s`.
- **Scripts MUST NOT write to `%TEMP%` or `$TMPDIR`**; use `./tmp` for scratch.
- **Scripts MUST NOT invent third-party APIs or extension points** — see root AGENTS.md "Evidence Over Invention".

