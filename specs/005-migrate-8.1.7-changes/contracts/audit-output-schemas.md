# Audit Output Schemas (Contracts)

The audit tool emits four files per run. These JSON/Markdown schemas are the public contract that downstream consumers (the planning PR, the porting PRs, the next release's audit) depend on. The schemas are intentionally plain and stable; the script may emit them in any order, but every field is mandatory unless explicitly marked optional.

## Contract 1: `inventory.json`

**Path**: `./tmp/release-audit/<tagRange>/inventory.json`

**Format**: JSON array of PRRecord objects.

```json
[
  {
    "number": 763,
    "title": "bugfix/757: Fix footer misalignment by using min-height for vspan reg…",
    "author": "natechadwick-intsof",
    "mergedAt": "2026-05-26T19:11:04Z",
    "baseRef": "development-8.1.x",
    "mergeCommitSha": "5e6b0f3a2c1b4d5e6f7a8b9c0d1e2f3a4b5c6d7e",
    "modulePaths": ["system"],
    "dependabotFlag": false,
    "jdk8OnlyFlag": false,
    "securityFlag": false
  }
]
```

Field-level contract:

|      Field       |         Type          | Required |                                            Notes                                             |
|------------------|-----------------------|----------|----------------------------------------------------------------------------------------------|
| `number`         | int                   | yes      | Unique within the file                                                                       |
| `title`          | string                | yes      | Full PR title                                                                                |
| `author`         | string                | yes      | GitHub login                                                                                 |
| `mergedAt`       | string (ISO-8601 UTC) | yes      | Used for the cutoff filter                                                                   |
| `baseRef`        | string                | yes      | Must be `development-8.1.x` for this audit                                                   |
| `mergeCommitSha` | string (40-char hex)  | yes      | Resolved via `gh pr view --json mergeCommit.oid`                                             |
| `modulePaths`    | array of string       | yes      | May be empty `[]` for meta PRs                                                               |
| `dependabotFlag` | bool                  | yes      | If `true`, this PR is mis-recorded (excluded PRs go in `dependabot-excluded.json`, not here) |
| `jdk8OnlyFlag`   | bool                  | yes      | Heuristic; see data-model.md                                                                 |
| `securityFlag`   | bool                  | yes      | Heuristic; see data-model.md                                                                 |

## Contract 2: `dependabot-excluded.json`

**Path**: `./tmp/release-audit/<tagRange>/dependabot-excluded.json`

**Format**: JSON array of `{number, title, author, mergedAt}` records.

```json
[
  {
    "number": 657,
    "title": "build(deps): Bump org.yaml:snakeyaml from 2.5 to 2.6",
    "author": "dependabot[bot]",
    "mergedAt": "2026-03-10T13:28:00Z"
  }
]
```

Field-level contract:

|   Field    |         Type          | Required |                       Notes                        |
|------------|-----------------------|----------|----------------------------------------------------|
| `number`   | int                   | yes      |                                                    |
| `title`    | string                | yes      |                                                    |
| `author`   | string                | yes      | Always matches `dependabot[bot]` for this contract |
| `mergedAt` | string (ISO-8601 UTC) | yes      |                                                    |

This file is the audit log proving that Dependabot PRs were considered and intentionally excluded (FR-002).

## Contract 3: `verdicts.json`

**Path**: `./tmp/release-audit/<tagRange>/verdicts.json`

**Format**: JSON array of PRVerdict objects.

```json
[
  {
    "prNumber": 763,
    "verdict": "needs-migration",
    "evidenceCommit": null,
    "evidenceFilePath": null,
    "evidenceNote": "not found at path system/cms/.../perc_decoration.css; vspan_ rules have no !important override on development",
    "jdk8Only": false,
    "securityFlag": false
  },
  {
    "prNumber": 851,
    "verdict": "superseded",
    "evidenceCommit": "f5a33ea8bd",
    "evidenceFilePath": "system/services/src/com/percussion/services/pubserver/data/PSPubServer.java",
    "evidenceNote": "development migrated getPublishServer() to return Optional; callers updated to .map(...).orElse(...) — same functional intent, different API shape",
    "jdk8Only": false,
    "securityFlag": false
  },
  {
    "prNumber": 794,
    "verdict": "conflicts-with-newer-design",
    "evidenceCommit": "a16d21e972",
    "evidenceFilePath": "WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml",
    "evidenceNote": "GadgetRegistry.xml deleted by Feature/jdk 21 stabilization (#605); no equivalent gadget catalog on development",
    "jdk8Only": false,
    "securityFlag": false
  }
]
```

Field-level contract:

|       Field        |      Type      |  Required   |                                                    Notes                                                    |
|--------------------|----------------|-------------|-------------------------------------------------------------------------------------------------------------|
| `prNumber`         | int            | yes         | FK → inventory.json[].number                                                                                |
| `verdict`          | string (enum)  | yes         | One of: `already-present`, `needs-migration`, `not-applicable`, `superseded`, `conflicts-with-newer-design` |
| `evidenceCommit`   | string or null | conditional | Required when verdict is `already-present` or `superseded`                                                  |
| `evidenceFilePath` | string or null | conditional | Required when verdict is `already-present` or `superseded`                                                  |
| `evidenceNote`     | string         | yes         | Required for every verdict; explains the classification                                                     |
| `jdk8Only`         | bool           | yes         |                                                                                                             |
| `securityFlag`     | bool           | yes         |                                                                                                             |

## Contract 4: `migration-backlog.md`

**Path**: `./tmp/release-audit/<tagRange>/migration-backlog.md`

**Format**: GitHub-flavored Markdown, sorted by priority then by merge date. Contains ONLY PRs with `verdict == "needs-migration"`.

Required sections (in order):

1. **Top matter**: tag range, target branch, run timestamp, total backlog items, total `needs-migration` count.
2. **P0 — Security** (`securityFlag == true`): table with columns `PR | Title | Module | Strategy | Test coverage in v8.1.7 | Blockers`.
3. **P1 — REST contract / Publishing**: same table schema.
4. **P2 — UI fix**: same table schema.
5. **P3 — Cosmetic / Gadget**: same table schema.
6. **Per-item detail** (one H3 per backlog item): v8.1.7 PR URL, merge commit, target module path, evidence note (from `verdicts.json`), proposed migration strategy with rationale.

Table column schema:

|          Column           |                 Source                 |                                  Format                                  |
|---------------------------|----------------------------------------|--------------------------------------------------------------------------|
| `PR`                      | inventory.json[].number                | `[#NNN](https://github.com/intersoftdatalabs-in/percussioncms/pull/NNN)` |
| `Title`                   | inventory.json[].title                 | First 90 chars; ellipsis if truncated                                    |
| `Module`                  | inventory.json[].modulePaths[0]        | First non-empty entry; `(none)` if empty                                 |
| `Strategy`                | MigrationBacklogItem.strategy          | enum value                                                               |
| `Test coverage in v8.1.7` | MigrationBacklogItem.testCoverageIn817 | one-line summary (e.g. "added PagesTest.testPageWithLeadingSitesPath")   |
| `Blockers`                | MigrationBacklogItem.blockerNotes      | one-line; empty if none                                                  |

## Contract 5: `v8.1.7-to-8.2-migration-report.md`

**Path**: `./tmp/release-audit/<tagRange>/v8.1.7-to-8.2-migration-report.md`

**Format**: Markdown summary intended for posting to a GitHub issue or PR description. Reviewable in under 10 minutes (SC-007).

Required sections (in order):

1. **Header**: feature branch, tag range, target branch, run timestamp.
2. **TL;DR**: 3–5 bullet summary; e.g. "Inventory: 141 PRs (after excluding 229 dependabot). Verdicts: 14 needs-migration (in sample of 20), 2 already-present, 2 superseded, 1 not-applicable, 2 conflicts-with-newer-design."
3. **Verdict distribution table**: counts per verdict.
4. **Top 10 backlog items**: by priority, linked to `migration-backlog.md`.
5. **Exclusions**: count and one-line summary of dependabot PRs excluded (no PR numbers, just categories).
6. **Open questions / data gaps**: copy of section 6 from `research.md`.
7. **Next steps**: link to `migration-backlog.md` and the planned porting PRs.

The report MUST NOT include raw diffs or commit lists beyond what fits in a Top-10 table; deeper evidence lives in `verdicts.json` and `inventory.json`.

## Contract 6: CLI surface

The audit script exposes a minimal CLI:

```
release-audit.sh --from-tag <TAG> --to-tag <TAG> [--target-branch <BRANCH>] [--output-dir <DIR>] [--include-dependabot]
```

|          Flag          |                  Default                   |                                 Notes                                  |
|------------------------|--------------------------------------------|------------------------------------------------------------------------|
| `--from-tag`           | `v8.1.6`                                   | Lower bound of tag range; required for the first run, optional after   |
| `--to-tag`             | `v8.1.7`                                   | Upper bound of tag range; required                                     |
| `--target-branch`      | `development`                              | Branch to compare against; per FR-008 must be overridable              |
| `--output-dir`         | `./tmp/release-audit/<from-tag>..<to-tag>` | Where the four output files are written                                |
| `--include-dependabot` | `false`                                    | When set, dependabot PRs are inventoried but flagged (FR-002 override) |

Exit codes:

- `0`: success; all four files written.
- `2`: partial failure; some output files may be present but with errors logged.
- `3`: invalid arguments (e.g. tag range does not resolve on `origin`).
- `4`: `gh` CLI not authenticated or `origin` unreachable.

