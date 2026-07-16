---
name: "Percussion CMS"
version: "8.2.x"
root: "./"
priority: "high"
capabilities: ["code-generation", "refactoring", "documentation", "testing", "debugging", "code-review", "project-management", "internationalization", "legacy-code-maintenance", "modernization", "code-completion", "code-analysis", "dependency-management", "build-management", "git-management", "maven-management", "npm-management"]
---
# Agent Guidelines

This repository is a large mono-repo with many modules.  This code base has a lot of history and is currently in the process of being modernized and refactored; Do not assume that all code is up to date with current best practices.  When making code changes, follow these guidelines:

## Project Context
- **Name** `Percussion CMS`
- **Aliases** `Rhythmyx`, `CM1`, `CM System`, `E2 Server`, `PercussionCMS`
- **Root:** `./`
- **Primary Configuration:** `./AGENTS.md`
- **Repo Temp Dir:** `./tmp`
- **Repo Script Dir:** `./scripts`
- **Repo Skills Dir:** `./modules/ai-shared-develop/src/main/resources/skills`
- **Stack**: Java 21, Spring, Hibernate, Artemis, React, JSP, jQuery, XML, XSL, JUnit 5, Mockito

## Key Terms
- **DTS**" `Delivery Tier Service` means `./deliverytiersuite/delivery-tier-suite`
- **CMS**: `Content Management System` means `./`
- **XML Application**: An XML application defined by the CMS and executed by the CMS XML application server
- **Package**: A deployable unit of CMS components, `.ppkg` file extension, a zip.

## Key Links
- **Git Repository**: [GitHub](https://github.com/intersoftdatalabs-in/percussioncms)
- **Documentation Site**: [Help Site](https://percussioncmshelp.intsof.com/)

## Rule Discovery Protocol

**For any task, question, or code modification related to a specific module, you MUST first apply this protocol to the module's path:**

1. **Identify the module path:** Determine the specific directory context (e.g., `modules/perc-tinymce/` or `system/services/`).
2. **Check for local override files:** Scan the identified directory for the following files in this specific order of priority:
   * `AGENTS.local.md` (Personal or task-specific overrides)
   * `AGENTS.md` (Module-specific permanent rules)
3. **Apply Hierarchy:**
   * If local files exist, their instructions **supersede** global rules for that module's logic.
   * `AGENTS.local.md` takes precedence over `AGENTS.md`.
   * If no local files are found, default strictly to the root-level instructions.

## Pre-commit code review (Erlang)

Before `git commit`, `git push`, or opening/updating a GitHub PR for changes you authored:

1. Run a **strict Erlang** review (independent of the implementer persona).
2. Canonical agent: `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
3. Skill: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/SKILL.md`
4. **Kilo (preferred):** workflow `/erlang-review` (`.kilocode/workflows/erlang-review.md`); project rule `.kilocode/rules/pre-commit-review.md` also applies.
5. Any **bug** finding, or missing **behavioral** unit tests for new/changed non-trivial logic, is a **hard gate** — do not commit or open the PR until fixed and re-reviewed.
6. Optional report path: `tmp/reviews/` (repo temp dir).

Tool-agnostic one-shot prompt: `modules/ai-shared-develop/src/main/resources/prompts/erlang-review-uncommitted.md`.

## **Project Rules**

* Be creative, but DO NOT *invent* third-party APIs, libraries, functions, or syntax that does not actually exist. If it doesn't exist in real docs (MDN, JDK 21, official Percussion docs, etc.): Ask user to clarify.
* If instructions are unclear or you can't find needed info: ask the user for clarification and guidance — don't guess.
* Base EVERY output on:
  * The currently checked-out Git branch (e.g., development, feature/auth-fix, development-8.1.x, etc)
  * Files in the current workspace
* NEVER read and write to `%TEMP%` or `$TMPDIR` directories. ALWAYS use the repo temp dir.
* ALWAYS add generated scripts to repo script dir or module script dir if script is specific to a module.
* ALWAYS update relevant script dir `README.md` files with doc on script purpose and usage scanrios when creating/editing scripts.
* ALWAYS document your work in comments, README, or maven site documentation.
* **IMPORTANT** you must ALWAYS update or create unit tests for any code change that you make, new or edited. And the tests must pass. No exceptions.
* Always use the #codebase or root `./` context when resolving missing interfaces or classes.
* You MUST respect rate limits when calling 3rd party API's. All 3rd party API integrations must be implemented with rate limit detection and exponential backoff logic.
* You MUST NOT share or leak secrets, tokens, or keys over the wire, in logs, or in LLM sessions.  If you see MKD-REDACTED in a session, that means you leaked a secret.

## PR Review Comment Resolution

When a PR review comment is addressed, the fix is **not** complete until the comment is also explicitly resolved in the PR's review threads. The CI/merge gate will block a PR that has unresolved review threads, so a code-only fix that does not also resolve the corresponding thread is incomplete from the merge-readiness perspective.

For each review comment on a PR you are working on (whether the comment is from a human reviewer, a `kilo-code-bot[bot]`, `github-actions[bot]`, or any other source):

1. **Locate the review threads** for the PR:
   ```bash
   gh api graphql -H "X-GitHub-Api-Version: 2022-11-28" -f query='
     query($owner: String!, $repo: String!, $pr: Int!) {
       repository(owner: $owner, name: $repo) {
         pullRequest(number: $pr) {
           reviewThreads(first: 50) {
             nodes { id isResolved isOutdated
                     comments(first: 1) { nodes { databaseId path line body } } }
           }
         }
       }
     }' -f owner='<owner>' -f repo='<repo>' -F pr=<pr-number>
   ```
2. **Reply inline to each comment** with a concrete mitigation statement that cites:
   - The commit hash that contains the fix (e.g. `f1908b961e`).
   - A short description of what changed, in enough detail that a reviewer can confirm correctness without re-reading the full diff.
   - A pointer to any new tests, scripts, or documentation that back the fix.
   Use the REST endpoint, replying to the specific `databaseId` of the comment:
   ```bash
   gh api -X POST repos/<owner>/<repo>/pulls/<pr>/comments/<comment-id>/replies \
     -f body='**Mitigation (commit `<hash>`):** ...'
   ```
3. **Resolve the review thread** via the GraphQL `resolveReviewThread` mutation, using the `id` from step 1 (NOT the `databaseId`):
   ```bash
   gh api graphql -H "X-GitHub-Api-Version: 2022-11-28" -f query='
     mutation($threadId: ID!) {
       resolveReviewThread(input: { threadId: $threadId }) {
         thread { id isResolved }
       }
     }' -f threadId="<thread-id-from-step-1>"
   ```
4. **Re-verify** by re-running the GraphQL query from step 1 and confirming `isResolved: true` for every thread whose underlying finding you have addressed. Do not rely on the inline reply alone — a reply leaves the thread in `isResolved: false` until the mutation is run.

**Outdated threads** (where the diff no longer contains the offending line) still need an inline reply explaining the mitigation AND a `resolveReviewThread` call. The `isOutdated: true` flag is informational; it does not auto-resolve.

**Do not** mark a thread as resolved without first replying inline with the mitigation statement. A bare resolve is not a substitute for a documented fix.

This rule applies to ALL review comments on a PR you own, including comments that arrive after the initial submission (late feedback, as in the 002-jdbc-drivers-cleanup / PR #1185 → #1185 review cycle).

## CodeQL / code scanning (analyzer of record)

**Do not re-enable GitHub CodeQL default setup** without attaching the same config and model pack — default setup caused repeated residual re-opens on PRs (new alert IDs for the same fixed sinks).

| Piece | Path / command |
|-------|----------------|
| Playbook (required reading for security/CodeQL PRs) | `docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md` |
| Advanced workflow (PRs + `development` + schedule) | `.github/workflows/codeql.yml` |
| Config (`paths-ignore`, `packs`, `query-filters`) | `.github/codeql/codeql-config.yml` |
| Custom sanitizer models | `.github/codeql/models/` |
| Agent skill | `modules/ai-shared-develop/src/main/resources/skills/codeql-pr/SKILL.md` |
| Verify default setup off | `gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state` → `not-configured` |

Disposition ladder: **runtime fix + test → model pack barrier → sink-line `// codeql[rule-id]` → path query-filters → dismiss last**. Put suppressions on the **exact sink line** (not above multi-line builders).

## Git Branch & Maven Wrapper Information

* Base Branch Name: development
  * All code changes in this branch must be compatible with JDK 21
  * Use `./mvn-env.sh` or `./mvn-env.bat` maven wrapper to ensure JDK compliance.
* Base Branch Name: development-8.1.x
  * All code changes on this branch must be compatible with JDK 8.
  * Use `./mvn-env.sh` or `./mvn-env.bat` maven wrapper to ensure JDK compliance.

## Project & Dependency Management

* This is not a Spring Boot application; avoid Spring Boot dependencies.
* Dependabot is enabled for this repository and is configured on the development branch @.github/dependabot.yml
  * All branches requiring exclusions are managed in this dependabot.yml file, and any new exclusions must be added here.
* Use Maven for Java dependency management; ensure all dependencies are defined in the `pom.xml`.
* Use npm for typescript and javascript dependency management via the Maven frontend-plugin.
* Use the parent POM to manage shared dependencies and plugin versions.
* The parent POM (`pom.xml`) has a pluginManagement section to manage versions of plugins used in child modules. Use these plugins.
* Ignore module folders that are not referenced directly or indirectly in the `./pom.xml` as child modules.

## Skills
- Locate and read the `./modules/ai-shared-develop/src/main/resources/skills/SKILLS.md` file for available project skills.

## Module List
A list of child modules in this repository. Each bullet contains: Module name — module path — one-line description.

- **perc-security-utils** — `./modules/perc-security-utils` - System wide security related utilities. Common re-usable security code shareable by all modules belongs here.
- **Percussion Security ACL Shim** — `./modules/perc-security-acl-shim` — A temporary module that provides shim classes for Java 8 ACL related classes dropped from the JDK.
- **perc-xml-security** — `./modules/perc-xml-security` — Shared java library that contains all XML security related common code for use by all modules.
- **perc-exceptions-spring** — `./modules/perc-exceptions-spring` — Shared library for Spring related exceptions.
- **perc-legacy** — `./modules/perc-legacy` — Legacy module containing legacy code needed to upgrade older versions of the CMS.
- **utils** — `./modules/utils` — Shared general purpose utilities intended for use by all modules of the application.
- **perc-shared-test-resources** — `./modules/perc-shared-test-resources` — Legacy module containing common resources used by legacy tests.
- **auditlogger** — `./modules/jcadf-master` — Module intended to provide audit logging API for all modules of the CMS. Needs refactoring.
- **audit-log** — `./modules/perc-auditlog` — Legacy module intended to provide audit logging services to all modules of the CMS.  Needs rafactoring.
- **perc-simple** — `./modules/Simple` — Legacy module containing a mix of tools. Needs rafactoring / evaluation for consolidation or removal.
- **tablefactory** — `./modules/TableFactory` — Core cms module responsible for schema and data generation. Provides tools for schema and data migration using XML as the transfer.
- **perc-i18n** — `./modules/perc-i18n` — Core cms module responsible for all internationalization and localization in the CMS. Uses TMX based translations.
- **servlet-utils** — `./modules/servletutils` — Shared library containing common java servlet utilities used in the CMS.
- **perc-ant** — `./modules/perc-ant` — Used as the engine for the installer and CMS installation / upgrade.
- **perc-common-ui** — `./delivery` — Legacy module that used to contain the DTS common ui code - see `Percussion CMS Common UI Bundle`
- **perc-help** — `./modules/Help` — Legacy module providing Java Help integration for the `Desktop Content Explorer`
- **perc-ssl-tool** — `./modules/SSLTools` — non-core utility that checks for expiring SSL certificates.
- **tlsutils** — `./modules/tlsutils` — non-core utility with functions for ssl certificates.
- **perc-rxapps** — `./modules/perc-rxapps` — Packaging module used by the installer to package files required in the cms deployment.
- **webservices** — `./modules/webservices` — Legacy Rhythmyx SOAP web services API migrated from Apache Axis to CXF.
- **perc-system** — `./system` — The core CMS module representing Rhythmyx functionality.  Contains the core XML application server and content managenent implementation.
- **perc-service-wrapper** — `./modules/perc-service-wrapper` — Legacy module that was intended to be used for Windows service management. Currently not used in deployments.
- **rest** — `./rest` — Contains the public REST API for the CMS.
- **perc-tinymce** — `./modules/perc-tinymce` — Packaging module for the TinyMCE rich text editor used in the CMS ui to edit content.
- **perc-toolkit** — `./modules/perc-toolkit` — Legacy module containing
- **perc-taxonomy** — `./modules/perc-taxonomy` — Legacy Rhythmyx module that provides taxonomy services for CMS content.
- **perc-deployer** — `./deployer` — Core module that contains the component packaging and package management implementation used by the CMS.
- **perc-server-ui-cmp** — `./modules/ServerUIComponents` — Legacy module that provides backend code that supports the legacy Rhythmyx  ui's.
- **perc-server-ui-content** — `./modules/ContentUI` — Legacy module that provides backend code that supports the legacy Rhythmyx ui's.
- **extensions-default-template** — `./modules/extensions-default-template` — CMS Java extensions for looking up the default template based on the siteid, contenttype, and publish
- **extensions-main** — `./modules/extensions-main` — The core CMS 'built-in' Java and JavaScript extensions that ship with Percussion CMS
- **extensions-nav** — `./modules/extensions-nav` — CMS Java extensions module containing the core extensions required by the content navigation features.
- **extensions-sfp** — `./modules/extensions-sfp` — Contains all extensions for Site, Relationships and legacy calendar.
- **extensions-workflow** — `./modules/extensions-workflow` — CMS Java extensions module containing the core Workflow extensions
- **extensions-linkback** — `./modules/extensions-linkback` — CMS java extension module that installs the CMS extensions needed for the linkback to editor feature from previewed or published CMS content.
- **p13n-api** — `./modules/p13n-api` — Personalisation API shared by the DTS p13n-ds service and the legacy client tracking integration.
- soln-serverutils — modules/extensions-serverutils — No description in pom.xml
- **perc-package-manager** — `./PCM-PkgMgtUI` — Provides the legacy gwt Package Managent UI implementation for managing components packaged and installed by the `deployer` module.
- **Percussion CMS Common UI Bundle** — `./modules/perc-common-ui-bundle` — Minified JavaScript bundle for the Percussion CMS delivery-tier widgets (perc_common_ui.js and perc_common_ui_slim.js); built with esbuild and served as bundled web resources from this JAR.
- **Percussion OpenAPI Generator Maven Plugin** — `./modules/perc-openapi-generator-plugin` — Maven plugin to generate OpenAPI specification from JAX-RS annotations in the `rest` module.
- **perc-web-ui** — `./WebUI` — The main user interface for the product.
- **Percussion OpenAPI Web App** — `./modules/perc-openapi-webapp` — Provides the OpenAPI Swagger UI forinteracting with the products REST API's.
- **perc-thumbnail** — `./modules/perc-thumbnail` — Responsible for generating all web page thumbnails in the application.
- **sitemanage** — `./projects/sitemanage` — Provides the main middleware 'internal' REST API used by `./WebUI` - CM1 functionality.
- **perc-checkboxtree** — `./modules/perc-checkboxtree` — No description in pom.xml
- **perc-content-explorer** — `./modules/DesktopContentExplorer` — Legacy end user desktop content manager interface for the Rhythmyx cms.
- **perc-jetty** — `./modules/perc-jetty` — Packaging module used by the installer  for the jetty server.
- **perc-jetty-jars** — `./modules/perc-jetty-jars` — Packaging modules used by the installer for deploying the cms on jetty.
- **perc-jetty-logging** — `./modules/perc-jetty-logging` — Percussion CMS logging module for jetty.
- **Percussion AI Shared Development** — `./modules/ai-shared-develop` — Shared AI development skills and utilities for maintaining Percussion CMS. Not distributed.
- **Percussion AI Shared Release** — `./modules/ai-shared-release` — Shared AI skills to be used by end-user AI agents when working with a deployed Percussion CMS instance. Distributed.
- **perc-packages** — `./modules/perc-packages` — Responsible for using the CMS 'packaging' system to create deployable packages that cn be distributed by the installer and installed by the CMS on start-up.
- **delivery-tier-suite** — `./deliverytiersuite/delivery-tier-suite` — The top level pom for constructing the DTS; builds the individual delivery projects
- **perc-shared-app** — `./deliverytiersuite/delivery-tier-suite/perc-shared-app` — A shared dependency module for the DTS services. Legacy needs re-factored.
- **tomcat-common** — `./deliverytiersuite/delivery-tier-suite/tomcat-common` — Tomcat server configuration and extensions.
- **common** — `./deliverytiersuite/delivery-tier-suite/common` — Common DTS utilities shared by all DTS modules.
- **comments** — `./deliverytiersuite/delivery-tier-suite/comments` — DTS micro-service responsible for user generated comments  on published websites.
- **feeds** — `./deliverytiersuite/delivery-tier-suite/feeds` — DTS micro-service responsible for handling dynamic RSS feeds on public websites.
- **forms** — `./deliverytiersuite/delivery-tier-suite/forms` — DTS micro-service responsible for rendering and collecting end usergenerated forms on the published website.
- **membership** — `./deliverytiersuite/delivery-tier-suite/membership` — DTS micro-service responsible for providing basic membership functionality on published websites.
- **metadata** — `./deliverytiersuite/delivery-tier-suite/metadata` — DTS micro-service that provides dynamic indexing and search of CMS content on the published website.  Provides auto-list functionality for statically published pages.
- **polls** — `./deliverytiersuite/delivery-tier-suite/polls` — DTS micro-service responsible for rendering and collecting dynamic polls on the published website.
- **secure-membership** — `./deliverytiersuite/delivery-tier-suite/secure-membership` — DTS micro-service that supports spring security based logins on the statically published website.
- **delivery-tier-distribution** — `./deliverytiersuite/delivery-tier-suite/delivery-tier-distribution` — This is the installer module for the DTS.
- **perc-distribution-tree** — `./modules/perc-distribution-tree` — This is the installer module for the CMS.
