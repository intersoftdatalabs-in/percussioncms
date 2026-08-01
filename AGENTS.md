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
- **Platforms**: Cross-platform product — builds, tests, installs, and runs on **Windows**, **Linux**, and **macOS**. All file I/O and path handling MUST be portable (see **Cross-Platform File I/O & Paths** below).

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
5. Any **bug** finding, missing **behavioral** unit tests for new/changed non-trivial logic, or **non-portable path/file I/O** (Windows/Unix — see **Cross-Platform File I/O & Paths**), is a **hard gate** — do not commit or open the PR until fixed and re-reviewed. Erlang must apply the cross-platform path checklist when the diff touches file I/O, paths, installers, packaging, or path assertions.
6. Durable reports: `docs/ai-generated/code-reviews/` (not `tmp/` — temp is wipeable). Pattern memory: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`.
7. Refresh pattern memory from Kilo/GitHub PR review history (including closed PRs): `python3 scripts/erlang-harvest-review-patterns.py --apply` (Windows: `scripts\erlang-harvest-review-patterns.bat --apply`). See `scripts/README.md`.

Tool-agnostic one-shot prompt: `modules/ai-shared-develop/src/main/resources/prompts/erlang-review-uncommitted.md`.

## Pre-PR Spotless formatting (HARD GATE)

**Before every final commit** that will ship on a GitHub PR (and before `git push` / open / update that PR), agents **MUST** run Spotless when the change set can include Spotless-covered files.

Spotless in this monorepo is **not Java-only**. It formats / checks **Java, Markdown/docs, JavaScript, TypeScript**, and other configured globs. Skipping it “because this is not Java” is a common agent failure mode.

### Required sequence

**Order is mandatory: `spotless:apply` first, then `spotless:check` second.** Never run `check` alone as the pre-PR gate and “fix later if it fails” — apply rewrites the tree; check only verifies. Agents that reverse the order leave dirty formatting unapplied or waste cycles on a failing check that apply would have fixed.

1. Ensure `JAVA_HOME` is JDK 21; use the repo Maven wrapper (`./mvnw` / `mvnw.cmd`).
2. From **repo root** (preferred for a final PR commit so docs/JS/TS outside a single module are included):

   ```bash
   # 1) FIRST — rewrite in-place to Spotless style
   ./mvnw spotless:apply
   # 2) SECOND — verify nothing still fails (must exit 0)
   ./mvnw spotless:check
   ```

   Windows:

   ```bat
   rem 1) FIRST — rewrite in-place to Spotless style
   mvnw.cmd spotless:apply
   rem 2) SECOND — verify nothing still fails (must exit 0)
   mvnw.cmd spotless:check
   ```

   Module-scoped apply is OK for mid-work iteration; the **final** PR commit must still end with a clean `spotless:check` for everything that will land on the PR (root apply then check when unsure).

3. Immediately inspect what Spotless rewrote:

   ```bash
   git status
   git diff --name-only
   ```
4. **Partition the working tree** into:
   - **In-scope** — files that are part of the agent’s intentional task (feature/fix/docs you meant to ship on *this* PR).
   - **Out-of-scope** — files Spotless rewrote that the agent did **not** intentionally change for this task (baseline formatting debt, unrelated modules, “100 files I never touched”).

### Out-of-scope Spotless hits — mandatory split (do not freak out)

If Spotless touches files **outside** the agent’s task scope:

|                                                              Do                                                               |                                              Do **not**                                              |
|-------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Keep **only in-scope** files on the feature branch / feature PR                                                               | Stuff dozens or hundreds of unrelated Spotless files into the feature PR                             |
| Leave out-of-scope Spotless changes **uncommitted** (or stash them) while finishing the feature commit                        | Panic, discard the whole worktree, or “fix everything Spotless touched” as part of the feature story |
| Open a **second PR** whose sole purpose is baseline formatting                                                                | Expand the feature PR description to claim ownership of unrelated modules                            |
| Title that second PR clearly, e.g. **`chore: Spotless cleanup`** (branch e.g. `chore/spotless-cleanup-<short-date-or-topic>`) | Skip Spotless entirely because “too many files”                                                      |
| Commit **only** the Spotless-only diffs on that cleanup branch                                                                | Mix product logic and repo-wide reformat in one review                                               |

Concrete workflow when `git status` shows a sea of unrelated Spotless files:

```bash
# 1) Stage and commit ONLY the intentional task files on the feature branch
git add <in-scope paths...>
git commit   # feature commit — no drive-by formatting of the monorepo

# 2) Move remaining Spotless-only changes onto a cleanup branch
git switch -c chore/spotless-cleanup-<topic> origin/development   # or current base
# re-apply / keep the out-of-scope Spotless diffs (stash pop, cherry-pick, etc.)
git add <spotless-only paths...>
git commit -m "chore: Spotless cleanup (unrelated baseline formatting)"
# push + open PR titled "Spotless Cleanup" (or "chore: Spotless cleanup") against the same base
```

If the out-of-scope set is huge, still open the cleanup PR rather than folding it into the feature PR. Reviewers must be able to approve product work without auditing a monorepo reformat.

### Hard bans

* **Do not** open or update a product/feature PR that silently includes large unrelated Spotless diffs.
* **Do not** skip `spotless:apply` / `spotless:check` on the final PR commit for Spotless-covered work because apply rewrote files outside your scope — **split** instead.
* **Do not** treat Spotless noise as a reason to abandon the feature branch or rewrite history of unrelated modules.
* **Do not** claim “formatting only” inside a feature PR when the diff is mostly baseline debt; that is a **Spotless Cleanup** PR.

### Evidence in the PR

In the feature PR body (or a short comment before “ready for review”), record:

* That `./mvnw spotless:apply` ran **first**, then `./mvnw spotless:check` **second** (exact commands, that order).
* That the feature PR contains **only in-scope** files (or “Spotless rewrote no out-of-scope files”).
* If a cleanup PR was opened: its URL/number, e.g. “Unrelated Spotless hits → #NNNN”.

Failing this section is a **hard gate** equal to a failing Erlang review or failed clean install: fix partitioning / formatting, then open/update the PR.

## Pre-PR Maven verification (HARD GATE)

**Before** opening or updating a GitHub PR (and before treating a change set as “done”), agents **MUST** run a real Maven **clean install** against every module whose sources, tests, resources, or `pom.xml` they changed. Partial compiles, IDE “make project”, or “tests only on one class” are **not** a substitute.

### Requirements (all must pass)

1. **Compile** — every changed module builds successfully **standalone** (see below).
2. **Tests** — unit/integration tests that Maven runs for those modules **pass**. No exceptions for “known flaky” without fixing or an explicit, documented skip approved in the PR body.
3. **No new warnings** — the clean install must not introduce **new** compiler, surefire, enforcer, Spotless, or plugin warnings attributable to the change. Prefer zero warnings on the modules you own; if the baseline already warns, do not add more (diff against a clean build of the base branch when unsure).
4. **Use the Maven wrapper + JDK 21** — always invoke repo-root `mvnw` / `mvnw.cmd` from the **module directory** (path is relative to depth). Ensure `JAVA_HOME` points at **JDK 21** (this monorepo’s supported toolchain on `development`).

### How to run (default: per-module standalone — NOT full reactor)

This monorepo is large. **Do not** default to root `./mvnw -pl … -am clean install` — that often rebuilds dozens of upstream modules and wastes time. Prefer **standalone** builds: change into each changed module’s directory and clean-install **only that module**, resolving dependencies from the local repo / already-installed SNAPSHOTs.

Identify changed Maven modules from the diff (e.g. `rest`, `projects/sitemanage`, `system`). Then, for **each** changed module:

```bash
# Example: only rest changed
cd rest
../mvnw clean install

# Example: only sitemanage changed (two levels down)
cd projects/sitemanage
../../mvnw clean install

# Example: two modules changed — build each standalone, producer first if one depends on the other
cd rest && ../mvnw clean install && cd ..
cd projects/sitemanage && ../../mvnw clean install && cd ../..
```

Windows (from the module directory):

```bat
cd rest
..\mvnw.cmd clean install
```

Adjust `../` vs `../../` (etc.) so the path points at the **repo-root** `mvnw` / `mvnw.cmd`. Maven uses the **current working directory**’s `pom.xml`, so only that module is built.

|                         Situation                         |                                                                                             Command guidance                                                                                              |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| One or more leaf modules’ sources/tests/resources changed | **`cd` into each module** → `…/mvnw clean install` (standalone). If module B depends on module A and you changed both, build **A first**, then B (each still standalone after A is installed to `~/.m2`). |
| Only docs / AGENTS.md / non-Maven files                   | No Maven clean install required; say so in the PR body.                                                                                                                                                   |
| Change **requires** a multi-module reactor (see below)    | Only then use a root reactor command — and scope it as tightly as possible.                                                                                                                               |

### When a full (or partial) reactor build *is* required

Use root reactor / `-pl` / `-am` **only** when standalone module builds are insufficient, for example:

* Parent `pom.xml` / `dependencyManagement` / pluginManagement changes that affect multiple children
* Moving types or APIs across modules so local SNAPSHOTs must be rebuilt in order for compile correctness beyond a simple producer→consumer install
* You changed an upstream module and the downstream standalone build still resolves a **stale** installed artifact and you cannot fix that by installing only the upstream module first
* Packaging / distribution / installer modules that assemble many reactor outputs in one reactor build

When reactor is justified:

```bash
# From repo root — prefer the smallest set of modules; avoid -am unless you truly need upstreams rebuilt
./mvnw -pl rest,projects/sitemanage clean install

# Only if upstream reactor modules must be rebuilt for this change (expensive — justify in PR body)
./mvnw -pl projects/sitemanage -am clean install
```

**Do not** use `-am` “just in case.” Prefer standalone `cd module && …/mvnw clean install` first.

### Hard bans

* **Do not** open or update a PR if clean install failed, tests failed, or new warnings appeared in the modules you changed.
* **Do not** use `-DskipTests`, `-Dmaven.test.skip=true`, or equivalent to green-wash a PR unless the user explicitly ordered a docs-only or non-code exception **and** the PR body states it.
* **Do not** claim “builds” from a non-clean incremental compile alone when the PR includes structural moves, package renames, or POM edits — **clean** is required.
* **Do not** rely on CI alone as the first full build of your modules; local (or agent-session) clean install is the pre-PR gate.
* **Do not** default to root `-pl … -am` / full-reactor builds for ordinary single- or multi-module source edits — that rebuilds large chunks of the monorepo unnecessarily.

### Evidence in the PR

In the PR description (or a short comment before “ready for review”), record:

* The exact command(s) run (**including `cd` into each module**, or a justified reactor command)
* **BUILD SUCCESS** for each changed module
* Test counts for modules with test changes (e.g. `Tests run: N, Failures: 0`)
* Confirmation of no new warnings on the changed modules
* If a reactor / `-am` build was used, **one sentence why** standalone was not enough

Failing this section is a **hard gate** equal to a failing Erlang review: fix, re-run clean install, then open/update the PR.

## Git worktree hygiene (HARD GATE)

Agent sessions (Kilo, Grok, etc.) often create **full monorepo git worktrees** under `.kilo/worktrees/`, `.worktrees/`, or `~/.grok/worktrees/`. Each copy is multi‑GB. Leaving them after a PR merges is a common disk-fill failure mode and can slow tooling that walks the tree.

### When this applies

Any time an agent **creates or works inside** a disposable git worktree for a feature/fix branch (not the primary developer checkout).

### Required sequence (end of task)

1. **While the PR is open** — keep the worktree if you still need it for review fixes; do not create additional worktrees for the same branch without removing the old one.
2. **When the PR is submitted and you are done for the session** — if no further commits are expected from that worktree, remove it (preferred) or document why it must stay.
3. **When the PR is merged or closed** — **must** remove the worktree before ending the session:

   ```bash
   # Run from the primary (main) checkout — not from inside the disposable worktree
   git worktree remove --force <worktree-path>
   git worktree prune
   # optional if the branch is fully merged / no longer needed locally
   git branch -D <branch-name>
   ```

4. **Periodic cleanup** (any machine with leftover agent worktrees):

   ```bash
   # Dry-run (default): list keep vs remove using GitHub PR state via gh
   python3 scripts/prune-stale-worktrees.py
   # Apply: remove worktrees whose branches have MERGED/CLOSED PRs
   python3 scripts/prune-stale-worktrees.py --apply --force --delete-local-branches
   ```

   Windows:

   ```bat
   scripts\prune-stale-worktrees.bat
   scripts\prune-stale-worktrees.bat --apply --force --delete-local-branches
   ```

### Hard bans

* **Do not** leave full-tree agent worktrees behind after PR merge/close “for later.”
* **Do not** nest dozens of `.kilo/worktrees/*` copies of this monorepo.
* **Do not** remove the primary worktree, a worktree with an **open** PR (unless the human ordered it), or the worktree you are currently running in (switch to main first).
* **Do not** use OS temp for worktrees; prefer repo-local `.kilo/worktrees/` (gitignored) or the agent host’s designated worktree root, then **delete when done**.

### Evidence

When finishing a PR-oriented agent task that used a worktree, the session summary should state either:

* “Worktree removed: `<path>`”, or
* “Worktree kept: `<path>` — reason: open PR #N / human requested retain”

Kilo rule: `.kilo/rules/worktree-hygiene.md`. Script docs: `scripts/README.md`.

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
* **IMPORTANT — WebUI + Playwright:** When changing a **product UI screen** under `WebUI/` (React SPA, login, shell chrome, user-visible flows), agents **MUST** also create or update Playwright specs in `modules/perc-qa-automation/` for the changed behavior. Vitest alone is not sufficient for screen work. See `WebUI/AGENTS.md` → **Playwright (HARD GATE)** and `modules/perc-qa-automation/AGENTS.md`.
* **IMPORTANT — Pre-PR Spotless:** Before every final PR commit, run `./mvnw spotless:apply` **first**, then `./mvnw spotless:check` **second** (JDK 21 + Maven wrapper). Do not check-only first. Spotless covers **Java, docs/Markdown, JS, and TS** (and other configured globs)—not Java only. If Spotless rewrites files **outside** your task scope, **do not** fold them into the feature PR: commit only in-scope files there, and open a second **`chore: Spotless cleanup`** PR for the unrelated formatting. Do not panic or abandon the feature work. See **Pre-PR Spotless formatting (HARD GATE)** above.
* **IMPORTANT — Pre-PR build:** Before opening or updating a GitHub PR, **`cd` into each module you changed** and run repo-root `mvnw` / `mvnw.cmd` **`clean install` standalone** (not default root `-pl -am` reactor builds). Code must compile, tests must pass, and there must be **no new warnings**. Use a full/partial reactor only when the change requires it. See **Pre-PR Maven verification (HARD GATE)** above. CI is not a substitute for this local gate.
* **IMPORTANT — Worktree hygiene:** If you used a git worktree for the task, remove it when the PR is merged/closed (or when the session ends and no further worktree commits are expected). Use `python3 scripts/prune-stale-worktrees.py --apply --force --delete-local-branches` for bulk cleanup of MERGED/CLOSED PR worktrees. See **Git worktree hygiene (HARD GATE)** above.
* Always use the #codebase or root `./` context when resolving missing interfaces or classes.
* You MUST respect rate limits when calling 3rd party API's. All 3rd party API integrations must be implemented with rate limit detection and exponential backoff logic.
* You MUST NOT share or leak secrets, tokens, or keys over the wire, in logs, or in LLM sessions.  If you see MKD-REDACTED in a session, that means you leaked a secret.
* **Cross-platform is mandatory.** Percussion CMS is a cross-platform build, test, and deploy product (Windows, Linux, macOS). Any production code, unit/integration test, script, or path assertion that works only on Unix-style paths is a defect. Follow **Cross-Platform File I/O & Paths** below.

## Cross-Platform File I/O & Paths

Percussion CMS is built, tested, installed, and deployed on **Windows, Linux, and macOS**. Agents MUST write portable file and path code. Failures that appear only on Windows (or only on Unix) from non-portable path handling are preventable defects — treat them as hard bugs, not environment quirks.

### Non-negotiable rules

1. **Never hardcode OS path separators in filesystem paths.** Do not concatenate paths with `"/"`, `"\\"`, or mixed literals for local files. Hardcoded `/` is correct only for **URL, URI, classpath, and ZIP entry** paths (those always use `/`).
2. **Prefer portable Java NIO path APIs** for all filesystem work:
   * `java.nio.file.Path`, `Paths.get(...)`, `Path.of(...)` (JDK 21 baseline; prefer NIO over legacy `File` string ops)
   * `path.resolve("child")`, `path.resolveSibling(...)`, `path.getParent()`, `path.normalize()`, `path.toAbsolutePath()`
   * `Files.*` (`Files.readString`, `Files.write`, `Files.createDirectories`, `Files.exists`, `Files.walk`, etc.) instead of ad-hoc `File` + string ops when practical
3. **When a separator character is required**, use the platform constants — do not invent them:
   * `File.separator` / `File.separatorChar` — path element separator (`\` on Windows, `/` on Unix)
   * `File.pathSeparator` / `File.pathSeparatorChar` — multi-path list separator (`;` on Windows, `:` on Unix)
   * Prefer `Path` resolve/join over manually inserting separators
4. **Do not assume a case-sensitive filesystem.** Windows (default) and some macOS volumes are case-insensitive. Avoid tests or logic that require `Foo.txt` and `foo.txt` as distinct files in the same directory. Prefer exact canonical names and case-insensitive comparisons only when the product domain requires them.
5. **Do not assume Unix-only roots or temp locations.** Avoid hardcoding `/tmp`, `/var`, `/home`, or drive-letter-free absolute paths. Use `System.getProperty("java.io.tmpdir")`, `Files.createTempFile` / `Files.createTempDirectory`, or the repo temp dir (`./tmp`) as appropriate. On Windows, absolute paths include a drive letter or UNC prefix (`C:\...`, `\\server\share\...`).
6. **Normalize before comparing paths as strings.** Prefer `Path` equality (`path1.normalize().toAbsolutePath().equals(...)`) or `Files.isSameFile` over string equality of raw path text. If string form is unavoidable, normalize separators first (e.g. via `Path` then `toString()`, or consistent use of `File.separator`).
7. **Line endings differ by platform.** Do not assert exact multi-line file contents with only `\n` when the runtime or Git may produce `\r\n` on Windows. Normalize line endings in tests (`replace("\r\n", "\n")`) or compare logical lines / use platform-agnostic matchers.
8. **Shell scripts are not portable by themselves.** Repo automation that must run on Windows needs a `.bat`/`.cmd` counterpart (or a documented Java/Maven entry point). Existing pattern: `./mvnw` and `./mvnw.cmd`. Do not land Unix-only scripts as the sole way to run a required workflow.
9. **Unit and integration tests must be cross-platform.** Tests that construct paths, write files, parse absolute paths, or assert path strings MUST pass on Windows. Common failure modes to avoid:
   * Expected path strings built with `/` when the OS returns `\`
   * Splitting `PATH` / classpath with `:` only
   * Regexes that only match Unix paths (`^/.*`) and reject `C:\...`
   * Commands invoked via `/bin/sh` without a Windows alternative in product code paths
   * Assuming executable bits / POSIX permissions semantics

### Preferred patterns (Java)

```java
// GOOD: portable join and resolve
Path base = Path.of(System.getProperty("java.io.tmpdir"), "percussion-test");
Path out = base.resolve("reports").resolve("result.xml");
Files.createDirectories(out.getParent());
Files.writeString(out, content);

// GOOD: when a File is required by a legacy API
File f = out.toFile();

// GOOD: multi-entry path lists (classpath, PATH)
String joined = String.join(File.pathSeparator, entry1, entry2);

// BAD: hardcoded separators for filesystem paths
String bad1 = base + "/reports/result.xml";
String bad2 = "C:\\temp\\reports\\result.xml";
String bad3 = dir + "\\" + name;

// GOOD: URLs / classpath / zip always use '/'
String resource = "com/percussion/config/defaults.xml";
URI uri = URI.create("file:///some/logical/url"); // URL path form, not OS file join
```

### Review checklist (agents)

Before finishing any change that touches file I/O, paths, installers, packaging, or tests:

* [ ] No new `".../" +` or `"...\\" +` filesystem path construction
* [ ] New path logic uses `Path` / `Paths.get` / `Path.of` / `Files`
* [ ] Separators from `File.separator` / `File.pathSeparator` only when a char/string is truly needed
* [ ] Tests do not assert Unix-only absolute path shapes or raw `/` path strings from the OS
* [ ] Temp files use portable APIs or the repo `./tmp` convention — not OS-specific temp hardcodes
* [ ] Line-ending sensitive assertions are normalized
* [ ] Required scripts have Windows counterparts or a cross-platform runner where operators need them

**Linux/macOS-only developer machines are not an excuse.** Code and tests must still be written as if the next CI agent or customer install is Windows.

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

**Also keep GitHub Code Quality disabled** (Settings → Code quality). Its dynamic workflow (`dynamic/github-code-scanning/codeql`, run name `Code Quality: CodeQL Setup`) ignores advanced config, scans extra languages, and empty analyses on `development` mass-close open alerts.

**Languages in scope:** Java + JavaScript/TypeScript only (see `.github/workflows/codeql.yml`).

**Path-filtered on PR/push (repo-wide):** `Analyze (java-kotlin)` runs only when Java-relevant paths change; `Analyze (javascript-typescript)` only when JS/TS-relevant paths change. Docs-only changes skip both. Weekly schedule + `workflow_dispatch` still run full dual-language scans. Prefer required check name **`CodeQL`** (the always-on gate job) so skipped language jobs do not block merge.

|                            Piece                            |                                                Path / command                                                |
|-------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Playbook (required reading for security/CodeQL PRs)         | `docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md`                                             |
| Advanced workflow (PRs + `development` + schedule + manual) | `.github/workflows/codeql.yml`                                                                               |
| Config (`paths-ignore`, Java `packs`, `query-filters`)      | `.github/codeql/codeql-config.yml`                                                                           |
| Custom sanitizer models                                     | `.github/codeql/models/`                                                                                     |
| Agent skill                                                 | `modules/ai-shared-develop/src/main/resources/skills/codeql-pr/SKILL.md`                                     |
| Verify default setup off                                    | `gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state` → `not-configured` |

Disposition ladder: **runtime fix + test → model pack barrier → sink-line `// codeql[rule-id]` → path query-filters → dismiss last**. Put suppressions on the **exact sink line** (not above multi-line builders).

## Git Branch & Maven Wrapper Information

* **Toolchain today (do not target older JDKs on `development`):** parent `pom.xml` uses **`java.version` / compiler `release` = 21**. Agent instructions, Spotless (`google-java-format`), and local builds assume **JDK 21** via `JAVA_HOME` + `./mvnw` / `mvnw.cmd`. Do **not** follow stale “Java 11” or “Java 17” modernization checklists as the current baseline—those were intermediate migrations; **current product line is Java 21**.
* Base Branch Name: **`development`**
  * All code changes on this branch must be compatible with **JDK 21**.
  * Prefer modern language features that compile on 21 (records, sealed types, pattern matching, virtual threads where appropriate, `var`, `Optional`, Streams, NIO `Path`).
  * Use `./mvnw` or `./mvnw.cmd` with `JAVA_HOME` pointing at JDK 21.
* Base Branch Name: **`development-8.1.x`**
  * Maintenance line: all code changes must remain compatible with **JDK 8**.
  * Do not introduce JDK 9+ APIs or language features on this branch.
  * Use `./mvnw` or `./mvnw.cmd` with a JDK 8-compatible toolchain as required by that line.

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

- **intsof-common-utilities** — `./modules/intsof-common-utilities` — Product-agnostic Intersoft utilities (`com.intsof.common:utilities`, semver). Portable per-user config under `~/.intsof/<app>/`. No CMS-specific code.
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
- **rest** — `./rest` — Public REST API (JAX-RS resources, wire DTOs, `IXxxAdaptor` interfaces). **Must not** depend on `sitemanage` (reactor cycle). See `rest/AGENTS.md`. Workbench-replacement REST + **dev/QA test modes**: `docs/developer-module/workbench-rest-and-qa-modes.md`.
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
- **perc-web-ui** — `./WebUI` — The main user interface for the product. UI screen changes require Playwright create/update in `modules/perc-qa-automation` (see `WebUI/AGENTS.md`).
- **perc-qa-automation** — `./modules/perc-qa-automation` — Playwright (+ TestNG) E2E against a live CMS; required companion tests for WebUI screen work. **Dev mode** = local install + docker bind + hot copy (no restart); **QA mode** = all-in-docker pass/fail. See `docs/developer-module/workbench-rest-and-qa-modes.md` and `modules/perc-qa-automation/AGENTS.md`.
- **Percussion OpenAPI Web App** — `./modules/perc-openapi-webapp` — Provides the OpenAPI Swagger UI forinteracting with the products REST API's.
- **perc-thumbnail** — `./modules/perc-thumbnail` — Responsible for generating all web page thumbnails in the application.
- **sitemanage** — `./projects/sitemanage` — CM1 middleware + `com.percussion.apibridge` implementations of `rest` `IXxxAdaptor` interfaces. Depends on `rest`; never reverse. See `projects/sitemanage/AGENTS.md`.
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

