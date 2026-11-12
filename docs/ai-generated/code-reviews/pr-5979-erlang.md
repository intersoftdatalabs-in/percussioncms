# Erlang review — PR #5979 (advisory-only leftover pass)

> **Status: advisory-only.** Own-model-only PR (`model:muse-spark-1.3-contributor`).
> Hard ban applies: **no APPROVE, no MERGE** from this review. Nothing was
> pasted to the PR (no comments, no reviews). Verdict recorded here only.
> Persona: **erlang 0.1.1** (`~/.agents/skills/erlang/`).

## Target

- PR: intersoftdatalabs-in/percussioncms#5979
- Title: `fix(deployer): clear test generics + drop-in deprecation warnings (#2028)`
- Head: `ba015a575b` (`fix/issue-2028-deployer-javac-warnings`)
- Base: `main`
- Files: 24 (9 main + 14 test + 1 report artifact)
- CI snapshot: CodeQL `Analyze (java-kotlin)` **pending**, `Detect language
  changes` pass. Nothing red at review time.

## Summary

Warning-cleanup slice of #2028: parametrize raw types in deployer test
sources, drop-in replacements for deprecated APIs in main sources, plus a
missing REST endpoint javadoc. **Zero in-diff blocking bugs. Verdict: LGTM
(advisory — do not merge from this pass).**

Machine analysis (independent re-run, CLI 0.1.21, pack `percussion`,
advisory gate, `--models .../models.ollama-dev-coder.toml`, Ollama
`dev-coder-64k`) found **21 finding(s), 0 in-diff; all 21 preexisting** —
identical to the report embedded in the PR body. Recommendation: approve
(machine). Preexisting rows do not block; residuals belong to future debt
work (the PR already tracks the remainder in #5978).

## Scope

- Base: origin/main
- Head: ba015a575b (reviewed in a clean checkout of the PR head)
- Files: 24 analyzed
- In-diff: 0 finding(s); preexisting: 21
- Persona: erlang 0.1.1

## Manual verification (per replacement class)

All semantic-equivalence claims below were checked against the PR head tree
and, where noted, executed against commons-lang3 3.20.0.

1. `Validate.notNull(x)` → `Objects.requireNonNull(x)`
   (`PSDeploymentHandler`, `PSImportJob`, `PSValidationJob`). Both throw
   `NullPointerException`; only the message differs (`"The validated object
   is null"` vs `null`). Repo grep shows **no code depends on the message
   text**. Imports updated correctly: `PSImportJob` drops the `Validate`
   import (no remaining `Validate.` uses); `PSDeploymentHandler` keeps it
   (3 remaining uses); `PSDescriptor`/`PSAppTransformer` keep `StringUtils`
   (6/2 remaining uses); `PSValidationJob` is covered by `java.util.*`.
   **No bug.**

2. `StringUtils.defaultString(s, d)` → `Objects.toString(s, d)`
   (`PSDescriptor` x3). Identical for `String` inputs (default iff null).
   **No bug.**

3. `StringUtils.replaceOnce` → `Strings.CS.replaceOnce`
   (`PSAppTransformer`). Executed a null/empty matrix against the real jar:
   all 5 cases byte-identical (`null→null`, `null-search→text`,
   `empty→empty`, normal replace). Documented migration target. **No bug.**

4. `new URL(s)` → `URI.create(s).toURL()` (`PSDeployComponentUtils`,
   `PSAppObjectDependencyHandler` x2, `PSExitDefDependencyHandlerTypedTest`).
   Every call site extends the existing `MalformedURLException` catch with
   `IllegalArgumentException`, preserving the "not a URL, leave it alone"
   path for relative/plain paths (relative URI parses but `toURL()` throws
   `IllegalArgumentException`, which is now caught). Null-safety: both
   production methods (`getAppName`, `getDepFromPath`) guard null/empty
   **before** the try block, and all three `getDepFromPath` callers guard
   (`href.isBlank()`, `litVal.trim().length() > 0`; line ~225/269/325), so
   the `URI.create(null)`-NPE vs `new URL(null)` delta is unreachable.
   Test method declares `throws Exception` (covers checked
   `MalformedURLException` from `toURL()`). **No bug.**
   - *Observation (non-blocking suggestion):* for inputs `java.net.URL`
     tolerates but `java.net.URI` rejects (raw spaces, `{}`, `|` in an
     otherwise absolute URL), old code extracted `getFile()` while new code
     takes the ignore-path. Both converge to "no dependency found" in every
     realistic case (a space-containing href never resolves to a real dep);
     previously-undefined malformed-input handling. Not a gate item.

5. `PSIteratorUtils.emptyIterator()` → `Collections.emptyIterator()`
   (2 test sites + 1 main: `PSAuthTypeDependencyHandler`). Target types are
   `Iterator<? extends PSDependency>` / generic test contexts — inference
   holds; module suite green per build evidence. **No bug.**

6. Test generics + cast removal (11 test files). Every parametrized
   declaration was checked against the **production generic signature**:
   `getFiles()`/`getFiles(dep)` → `Iterator<PSDependencyFile>` ✓,
   `getIdTypeMappings` → `Iterator<PSApplicationIDTypeMapping>` ✓,
   `getResourceList`/`getElementList` → `Iterator<String>` ✓,
   `getPackages()` → `Iterator<PSDeployableElement>` ✓,
   `getDependencies(..)` → `Iterator<PSDependency>` ✓,
   `getImportPackageList()` → `List<PSImportPackage>` ✓,
   `setPackages(Iterator<PSDeployableElement>)` /
   `setModifiedPackages(Iterator<String>)` /
   `setRequiredClasses(Iterator<String>)` ✓ — all call-site types match, so
   no unchecked assignments are introduced. `doesFileListTheSame` (now
   `Iterator<PSDependencyFile>` x2) is test-local with a single call site.
   `PSConfigFilesFactoryTest.init` → `final` is this-escape hygiene in test
   scaffolding. `PSConfigMapperTest` wildcard casts (`Map<?,?>`, `List<?>`)
   match the untyped `props` map. **No bug.**

7. `PSPackageService.getAllPackages` javadoc — doc-only, clears the lone
   module javadoc diagnostic. **No bug.**

## Cross-platform path checklist

- No new filesystem path joins introduced by the diff (no `+ "/"`, `+ "\\"`,
  no new `Paths`/`File` logic).
- Touched-line-adjacent separators (`"rx_resources\\ewebeditpro\\config.xml"`,
  `folder.getAbsolutePath() + "config.xml"`) are **preexisting context
  lines**, unchanged by this PR — flagged in the preexisting machine rows,
  out of scope here.
- No new scripts, no temp-dir usage, no line-ending-sensitive assertions.
- **Pass.**

## Behavioral tests

Change class is warning-cleanup with proven zero behavior delta; no new or
changed logic exists to cover. Existing module suite (author-reported:
`cd deployer && ../mvnw clean install` → BUILD SUCCESS, Tests run: 316,
Failures: 0, Errors: 0, Skipped: 19) is the correct closure — no new tests
required, none missing. **Pass (not a gate item for this class).**

## Change-class closure

No new public surface, no bean wiring, no packaging/installer/config, no
UI, no locale, no product-facing behavior → no companions owed.
Product-docs N/A is correct. The `docs/ai-generated/code-reviews/*` churn
in the diff is the review artifact itself, not product surface. **Complete.**

## Gate

- In-diff blocking bugs: **0**
- Missing behavioral tests: none owed
- Non-portable path handling introduced: none
- Rule/instruction files in diff: none
- **Verdict: LGTM — advisory only. NOT approved, NOT merged (own-model hard
  ban). No PR comments posted, PR untouched.**

## Machine report (independent re-run, abridged)

Full CLI markdown retained in this review session; headline:

- `## Summary` — 21 finding(s), 0 blocking bug(s)
- `## Scope` — Base origin/main, Head HEAD, 24 files, in-diff 0 / preexisting 21
- `## Recommendation` — approve (machine; superseded by advisory-only ban)
- Issues 1–16 (bug, all preexisting): `paths.hardcoded_sep` in
  `PSDeploymentHandler` (10), `PSImportJob` (1),
  `PSAuthTypeDependencyHandler` (1), `PSArchiveTest` (1),
  `PSConfigFilesFactoryTest` (3)
- Issues 17–21 (suggestion, all preexisting): `complexity.cognitive` in
  `PSDeploymentHandler.validateArchive`, `PSAppObjectDependencyHandler`
  (x2), `PSApplicationIdContextTest.testXml`,
  `PSPackageLockManagerTest.testAll`

Preexisting rows in PR-touched files are noted for the host's residual
pass; none were filed from this advisory review (host owns residual
dedupe/filing; remainder already tracked in #5978 per the PR body).
