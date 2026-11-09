# Erlang review — issue #5823 (residual: `paths.hardcoded_sep` in `PSWidgetXmlInstallEmitter`)

Branch: `fix/issue-5823-widgetxml-install-emitter-paths` vs `origin/main`.
Command:

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base "origin/main" \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Full CLI report (unmodified)

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-packages/src/main/java/com/percussion/packages/widgetxml/PSWidgetXmlInstallEmitter.java:434 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 434)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

## Implementer disposition (machine false positive — documented, tested, not "fixed" per suggestion)

The flagged line is `return "/" + p;` inside `toInstallHref()`, whose output is
written into Widget definition XML as `href` / `thumbnail` attributes in URL
form (`/rx_resources/...`, `/web_resources/...`), consumed as **web paths** by
the deployer / `PSWidgetDao` — not as OS filesystem paths. Applying the
tool suggestion (`File.separator` / NIO `Path` join) would emit
`\rx_resources\...` on Windows and **break** install XML on that platform: the
suggestion itself is the portable-code violation here.

Evidence the slash is intentional URL form, not filesystem I/O:

- Root `AGENTS.md` → Cross-Platform File I/O & Paths: "Hardcoded `/` is
  correct only for **URL, URI, classpath, and ZIP entry** paths (those always
  use `/`)."
- Same package precedent: `PSWidgetArchiveManifestInventory` line ~557 already
  documents `"ArchiveFile is a ZIP entry path — always '/'."` for the identical
  shape.
- The method already normalizes input `\` → `/` (`trim().replace('\\', '/')`),
  proving URL intent; no `java.io.File` / NIO filesystem join is involved.
- No filesystem API ever consumes this string: callers embed it in emitted XML
  (`emitWidgetXml` → `href`/`thumbnail` attributes) and the manifest inventory.

What this PR does instead of following the harmful suggestion:

1. Production: javadoc on `toInstallHref()` now states the leading slash is a
   URL-path separator required on every OS, with an explicit do-not-replace
   warning; inline `// URL form — always '/'` comment at the flagged line
   (mirrors the sibling ZIP-entry precedent) so future runs do not re-flag it.
2. Tests: new `toInstallHref_alwaysUsesForwardSlashUrlForm` behavioral test
   locks the contract on all platforms — backslash input normalizes to `/`,
   output always starts with `/` and never contains `\`, null/blank → null.
   This is the correct cross-platform test for URL-form output (asserting
   `File.separator` in output would enshrine the Windows bug).

Build evidence: `cd modules/perc-packages && ../../mvnw clean install` →
BUILD SUCCESS, Tests run: 239, Failures: 0, Errors: 0, Skipped: 0
(`PSWidgetXmlInstallEmitterTest`: 5/5 pass, incl. the new test).

Request for the host Erlang phase: independently verify the URL-vs-filesystem
classification above; if it agrees, this machine row should be recorded as a
pattern-level false positive for `paths.hardcoded_sep` on URL/href emitters
rather than a merge blocker.
