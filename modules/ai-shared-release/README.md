# ai-shared-release Module

The `ai-shared-release` module holds the AI tools (skills, prompts, instructions, and future agent-tool plugins) that are **shipped with Percussion CMS** to operators and end users. The contents of `src/main/resources/` are staged into the CMS install at build time by the `perc-distribution-tree` module.

## Packaging

`<packaging>pom</packaging>` — this module does **not** produce a JAR or a tarball. It exists as a reactor entry solely so the installer can find and stage the resources from a known path.

## Install layout

After `mvn -pl modules/perc-distribution-tree verify` (or any full reactor build that includes it), the assembled CMS distribution contains:

```
<InstallDir>/
  sys_resources/
    ai-tools/                      <-- shipped with the product, replaced on upgrade
      skills/
        percussioncms-config/
          SKILL.md
          README.md
          CHANGELOG.md
        <future-skill>/
          ...
      plugins/                     <-- future: per-tool shims (claude, kilo, vscode, ...)
      agents/                      <-- future: always-on agent personas
      instructions/                <-- future: style / review checklists
      prompts/                     <-- future: copy-paste one-shot prompts
  rx_resources/
    ai-tools/                      <-- EMPTY, created by the installer; survives upgrades
```

The staging step lives in `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` (search for `Staging AI tools`). It copies `src/main/resources/` into `${assembly-directory}/sys_resources/ai-tools/` with `overwrite="true" force="true"`, then `mkdir`s `${assembly-directory}/rx_resources/ai-tools/`.

## sys_resources / rx_resources overlay convention

This is the standard Percussion resource overlay pattern:

- `sys_resources/...` — shipped with the product; **replaced on every upgrade**.
- `rx_resources/...` — operator / customer customization; **preserved on upgrade**.

An agent reading skills from a deployed CMS should look in `rx_resources/ai-tools/` first and fall back to `sys_resources/ai-tools/`. If a file exists in both, the `rx_resources/` copy wins; if it exists only in `sys_resources/`, the shipped one is used; if neither has it, the agent does not see it.

To override a shipped skill (e.g. localize `percussioncms-config` for a specific customer), an operator copies the file to the matching path under `rx_resources/`:

```
<InstallDir>/rx_resources/ai-tools/skills/percussioncms-config/SKILL.md
```

That copy will take precedence from that point forward and will **not** be overwritten when the operator upgrades the CMS.

To add a brand-new skill without modifying the shipped set, the operator creates the file in `rx_resources/ai-tools/skills/<new-skill>/SKILL.md` and points their agent at `<InstallDir>/rx_resources/ai-tools/skills` as an additional skills directory.

## Layout under `src/main/resources/`

| Path                      | Purpose                                                  |
|---------------------------|----------------------------------------------------------|
| `src/main/resources/skills/`     | Discoverable skills (Claude / Kilo / etc.) with a `SKILL.md` per skill.    |
| `src/main/resources/agents/`     | Named agent personas (always-on style guides).                            |
| `src/main/resources/instructions/` | Always-on style review checklists.                                      |
| `src/main/resources/prompts/`    | Copy-paste one-shot prompts.                                              |
| `src/main/resources/plugins/`    | Per-agent-tool shims (e.g. `plugins/claude/`, `plugins/kilo/`). Reserved for future use. |

The three non-`skills` directories ship as placeholders with a `.gitkeeep` so the directory structure is preserved in git. The installer excludes `.gitkeeep` files when staging.

## Adding a new skill

1. Create `src/main/resources/skills/<skill-name>/SKILL.md` (required) plus any `README.md`, `scripts/`, or `reference/` files.
2. Add a one-line entry to `src/main/resources/skills/<skill-name>/CHANGELOG.md` so the release notes pick it up.
3. Run `mvn -pl modules/perc-distribution-tree verify` locally and confirm `target/.../sys_resources/ai-tools/skills/<skill-name>/` contains the new files.
4. Commit. The next CMS build will ship the skill.

## Guidelines

- **Ship only what end users need.** Anything developer-only (build-time agents, internal review prompts) belongs in `modules/ai-shared-develop/`, not here.
- **No Java code in this module.** This module is `packaging>pom` on purpose. If you need runtime Java (e.g. a backend for a skill), add it to `rest/`, `projects/sitemanage/`, or a new module that depends on it.
- **No production-only resources** (DB drivers, license files, signing keys) under `src/main/resources/`. Those go through their own installer paths.
- **Document end-user-facing skills** in `src/site/markdown/` and the project's end-user help site so operators can find them.
- **Cross-platform paths only** in any scripts or references. Use `Path` / `pathlib`, never hardcoded `/` or `\` separators.

## Related modules

- `modules/ai-shared-develop/` — developer-only AI resources (skills, prompts, agents) used by AI coding agents while working on the Percussion CMS source tree. Built into a JAR; not shipped to end users.
- `modules/perc-distribution-tree/` — the installer that copies this module's resources into `<InstallDir>/sys_resources/ai-tools/`.
- `modules/perc-tinymce/` — the only other module currently using the `sys_resources` / `rx_resources` overlay pattern; reference it for additional conventions.
