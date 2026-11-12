# Erlang review — PR #6034

Status: machine-only (LLM layer unavailable — Ollama `localhost:11434` down at review time; machine findings kept per protocol), manual strict-gate review applied.
Persona: erlang 0.1.1 (`/home/nate/.agents/skills/erlang`)
Date (UTC): 2026-11-11
PR: intersoftdatalabs-in/percussioncms#6034 — fix(deployer): clear IOTools/single-site/pkginfo warning clusters (#5978)
Base: origin/main → Head: fix/issue-5978-deployer-httpclient-iotools (ea28e49f8d)

## Machine report (mkd-code-review, no --models: Ollama down)

## Summary

Machine analysis found **18** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 31 analyzed
- In-diff: 0 finding(s); preexisting: 18
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:389 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 389)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:1708 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1708)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3288 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3288)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3292 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3292)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3296 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3296)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3301 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3301)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3308 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3308)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3314 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3314)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3318 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3318)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:3323 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3323)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSImportJob.java:583 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 583)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSPackageLockManager.java:178 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 178)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 13 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSPackageLockManager.java:180 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 180)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 14 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/PSDeploymentHandler.java:2735 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `processRequest` cognitive=808 (max 15), cyclomatic=56 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: bug

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentRelationDependencyHandler.java:620 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `processFieldSet` cognitive=75 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSAclDefDependencyHandler.java:83 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getChildDependencies` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/services/pkginfo/utils/PSPkgHelper.java:207 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getVersion` cognitive=26 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 18 -- Severity: suggestion

- File: deployer/src/test/java/com/percussion/deployer/server/PSPackageLockManagerTest.java:47 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `testAll` cognitive=19 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.


## Erlang manual strict-gate review (he/him)

Diff read in full (31 files; production delta is import/call-site mechanicals + 2 constant/deprecation migrations). Semantic-equivalence verification of every behavior-adjacent substitution:

- `IOTools.getChecksum(String)` is CRC32 of `str.getBytes()` → manual `CRC32` over UTF-8 bytes: identical on UTF-8 platforms; version is computed-and-stored in the same upgrade run (`setVersion`+save), never compared across runtimes — self-consistent regardless. `FileUtils.checksumCRC32(file)` same reasoning in `PSPkgHelper.getVersion`. Not a behavior change.
- `RS_INLINERELATIONSHIP` is a `@Deprecated` alias literally equal to `PDU_INLINERELATIONSHIP` ("rs_inlinerelationship", PSRelationshipConfig.java:1235/1240). Safe de-deprecation.
- `IPSAclEntry.getType()` is `@Deprecated` with `@see IPSTypedPrincipal#getPrincipalType()`; impl delegates to the same underlying value. Documented migration, safe.
- `PSXMLDomUtil.toString(Node)` already delegates to `PSXmlUtil.toString(node)` (PSXMLDomUtil.java:498-500). Identical.
- `Session.get` → `find`, untyped `createQuery` → typed, redundant-cast removals: JPA-equivalent, compile-verified.
- `IOTools.copyStream` → `IOUtils.copyLarge`, `copyFileStreams` → `FileUtils.copyFile`, `copyToDir` → `copyFileToDirectory` (test loop skips directories, verified), `deleteFile` → `deleteQuietly` (test cleanup only), `Strings.CS.equals` vs `StringUtils.equals` (both null-safe), `Collections.emptyIterator()` (target-type inference, compiles).
- Findings adjudication: 18 machine rows, **all preexisting / out-of-diff** → gate PASS. Two flagged rows verified as false positives and NOT filed: `PSImportJob.java:583` (log-message string concat, not a path join) and `PSPackageLockManager.java:178,180` (Windows `.bat` usage-example text — documented one-OS example per patterns). Genuine clusters filed as residuals: #6038 (paths cluster), #6039 (processRequest), #6040 (processFieldSet), #6041/#6042/#6043 (p8 complexity suggestions).
- Tests: PR body records `cd deployer && ../mvnw clean install` BUILD SUCCESS, 316 tests, 0 failures. `PSPackageLockManagerTest` call sites updated consistently. No new behavioral logic → no new unit-test closure owed. No product-docs surface (warning cleanup, no operator-facing change — checklist N/A with reason).
- Copyright: no new source files. Cross-platform: no new path joins introduced.

Checks snapshot: CodeQL pass, Analyze (java-kotlin) pass. Checks green.

## Verdict

**LGTM (advisory).** Zero in-diff bugs. Residuals filed: #6038, #6039, #6040, #6041, #6042, #6043. Own-model PR (`muse-spark-1.3-contributor`) — per hard ban this review is COMMENT-only; no APPROVE, no merge; left for human morning review.

## Second pass — leftover phase (2026-11-12 UTC)

Independent re-review (erlang 0.1.1) with the LLM stage enabled — Ollama back up
(`dev-coder:latest` on `localhost:11434`):

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --diff pr-6034.diff \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Result: **identical** — 18 findings, **0 in-diff**, 0 blocking bugs, recommendation
`approve` (advisory). LLM stage surfaced no additional in-diff bugs. First-pass
verdict and FP adjudications (PSImportJob:583 log-string, PSPackageLockManager
.bat usage text) stand.

Sibling status: #6029, #6031, #6033, #6035, #6036, #6037 are all MERGED into
`origin/main` (now at `208c7ca674`). **#6034 is the only remaining open PR** in
this slice.

**Conflict triage (merge BLOCKED by conflict, not by findings):** `mergeable:
CONFLICTING`, `mergeStateStatus: DIRTY` vs `origin/main@208c7ca674`. Local
`merge-tree` shows 5 hunks, all from the same warning-cleanup landing twice —
this branch vs merged #6033:

- `.../rx/services/deployer/PSPkgUiResponse.java` — identical
  `@SuppressWarnings("this-escape")` added on both sides (accept either).
- `.../rx/services/deployer/PSUninstallMessage.java` — same (accept either).
- `.../services/pkginfo/data/PSIdName.java` — comment-only (`/** Serialization
  id. */`, accept either).
- `.../services/pkginfo/data/PSPkgElementDependency.java` — comment-only
  (accept either).
- `.../services/pkginfo/impl/PSIdNameService.java` — `@SuppressWarnings(
  "unchecked")` comment wording differs (accept either, keep one).

Morning-human resolution (no erlang-fix on this pass): rebase
`fix/issue-5978-deployer-httpclient-iotools` onto `origin/main`, accept either
side per hunk above, re-run `cd deployer && ../mvnw clean install`, push.
Mechanical conflicts only — no semantic risk identified.

Residuals: re-deduped this pass — #6038, #6039, #6040, #6041, #6042, #6043
already cover every qualifying preexisting row; **0 new issues filed** (cap 12,
used 0).

Verdict: **reviewer-LGTM, comment-only** — own-model PR
(`model:muse-spark-1.3-contributor`), so no APPROVE and no merge from this
review. Unblocked on findings; blocked on the rebase above.
