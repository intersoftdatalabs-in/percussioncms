# ai-shared-develop Module

The `ai-shared-develop` module is the correct location to place shared agentic files such as `AGENTS.md`, shared prompts, and shared instructions that are used when developing Percussion CMS.

## Purpose

This module is not intended to be shipped with the product and should not contain any production code or resources. It is purely for development purposes to provide a common location for AI agent instructions and resources that are used across multiple modules and multiple agentic providers (e.g., Copilot, Claude, Gemini, Kilo Code, etc.) during development.

## Guidelines

- Place shared agentic instructions, skills, prompts, and resources here that are intended to be used by multiple modules during development.
- Do not place any production code, resources, or documentation here. This module is not intended to be included in the final product and should not contain anything that is needed at runtime or for end users.
- Do not place module specific instructions or prompts here. Module specific agentic instructions and prompts should be placed in the respective module's directory (e.g., `modules/perc-jetty/AGENTS.md` for Jetty module specific instructions).
- Use clear and descriptive naming for any files placed here to indicate their purpose and intended usage.

## Layout

|                Path                |               Purpose                |
|------------------------------------|--------------------------------------|
| `src/main/resources/agents/`       | Named agent personas (tool-agnostic) |
| `src/main/resources/skills/`       | Skills discoverable by coding agents |
| `src/main/resources/prompts/`      | Copy-paste one-shot prompts          |
| `src/main/resources/instructions/` | Always-on style review checklists    |
| `src/main/resources/chatmodes/`    | Optional chat-mode definitions       |

## CodeQL / code-scanning PRs

**Goal:** Stop residual thrashing (same SSRF/LDAP/path sinks re-opened as new alert IDs on every PR).

|       Asset       |                                 Path                                  |
|-------------------|-----------------------------------------------------------------------|
| Playbook          | repo `docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md` |
| Skill             | `src/main/resources/skills/codeql-pr/SKILL.md`                        |
| Advanced workflow | repo `.github/workflows/codeql.yml`                                   |
| Config + models   | repo `.github/codeql/`                                                |

Default CodeQL setup must stay **disabled** (`not-configured`). Analyzer of record is advanced setup only.

## Scripts

The `scripts/` directory holds cross-platform Python utilities for AI resource signing, signature verification, and integrity checking. Per spec 994 (`specs/994-python-build-scripts/spec.md`), the original `.sh` wrappers have been removed (FR-004).

|           Script            |                                                                                                  Purpose                                                                                                  |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sign-ai-resources.py`      | Sign every tracked AI resource using Sigstore (delegates to `com.percussion.ai.signing.ResourceSigner` via `mvn-env.sh exec:java`). Excludes `*.sha256`, `*.sha256.sig`, `*.sigstore.json` sidecars.      |
| `verify-signatures-hook.py` | Verify every tracked AI resource's Sigstore signature (delegates to `com.percussion.ai.signing.ResourceVerifier` via `mvn-env.sh exec:java`). Designed to be called by a git pre-commit hook.             |
| `build-integrity-check.py`  | Build-time integrity check: verify SHA-256 hash + Sigstore signature sidecars for one or more AI resources. Uses `hashlib.sha256` (no shell dependency on `sha256sum`/`shasum`) and `cosign verify-blob`. |

Both scripts accept `--dry-run` to print the planned Maven invocations without building or signing — used by pytest to exercise the wiring without paying the build-time cost.

### Tests

```sh
python3 -m pytest modules/ai-shared-develop/scripts/ -v
```

Or via the cross-platform runner:

```sh
bash scripts/run-python-tests.sh --skip-install --pytest-args "-q"
```

### Erlang — strict pre-commit review

**Goal:** Catch correctness bugs and weak tests **before** commit/PR so GitHub review cycles stay short.

|        Asset         |                              Path                              |
|----------------------|----------------------------------------------------------------|
| Canonical agent      | `src/main/resources/agents/erlang-code-review.md`              |
| Skill                | `src/main/resources/skills/erlang-review/SKILL.md`             |
| Pattern memory       | `src/main/resources/skills/erlang-review/patterns.md`          |
| One-shot prompt      | `src/main/resources/prompts/erlang-review-uncommitted.md`      |
| Durable reports      | repo `docs/ai-generated/code-reviews/` (not `tmp/`)            |
| Kilo workflow        | repo `.kilocode/workflows/erlang-review.md` (`/erlang-review`) |
| Kilo project rule    | repo `.kilocode/rules/pre-commit-review.md`                    |
| Copilot agent mirror | repo `.github/agents/erlang.agent.md`                          |

### How to run (Kilo first)

1. In Kilo: run workflow **`/erlang-review`** (optional args: PR number or "uncommitted only").
2. Or open a chat, load the Erlang agent, and say: *Review my uncommitted changes and branch vs development.*
3. Fix any **bug** findings (strict: missing behavioral tests count as bugs).
4. Re-run Erlang, then commit/push only when Gate says **May commit/push: yes**.

### How to run (any other tool)

Paste `prompts/erlang-review-uncommitted.md` or attach `agents/erlang-code-review.md` and ask for a pre-commit review.

### Strictness

- **Block** on any bug and on missing behavioral tests for new/changed non-trivial logic.
- **Block** on non-portable path/file I/O (hardcoded separators, Unix-only or Windows-only
  absolute paths in shared code/tests, etc.). See root `AGENTS.md` → **Cross-Platform File I/O & Paths**
  and the checklist in `agents/erlang-code-review.md`.
- Recommendation `request-changes` ⇒ do not commit or open/update a PR yet.
- Durable reports go under repo `docs/ai-generated/code-reviews/` (not wipeable `tmp/`).
- Institutional pattern memory: `src/main/resources/skills/erlang-review/patterns.md`.
- Refresh patterns from Kilo/GitHub PR review history:
  `python3 scripts/erlang-harvest-review-patterns.py --apply`
  (see `scripts/README.md`; Windows: `scripts/erlang-harvest-review-patterns.py`).

### Not for production

Do not copy Erlang into `ai-shared-release` or ship it with the CMS product. It is for **developers and AI coding agents** only.
