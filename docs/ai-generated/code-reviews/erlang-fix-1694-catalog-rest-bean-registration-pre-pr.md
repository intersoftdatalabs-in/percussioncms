# Erlang review — fix/1694-catalog-rest-bean-registration (pre-PR)

## Summary

Intent: register five missing CXF JAX-RS service beans
(`restControlsResource`, `restSearchResource`, `restViewResource`,
`restServerConfigsResource`, `restRelationshipTypeResource`) inside the
`rest-jax-rs` server definition of
`projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml`
so that Developer Catalog REST endpoints stop returning 500 on a clean QA
install (#1694).

This is a pure **wiring** fix. Each bean id maps to an existing
`@PSSiteManageBean`-annotated resource, an existing sitemanage apibridge
adaptor, and an existing Spring test stub + Mockito resource test in
`rest`. The change is the **direct continuation** of precedent commit
`62004b86d5` ("fix: Register missing REST resource beans for
keywords/locales/slots/sharedfields/systemdef", #1714) using the same
mechanical "add `<ref bean=.../>` lines into the existing `<jaxrs:server>`"
pattern that maintainers have already accepted.

The diff applies cleanly on top of `origin/development`, touches no
production code, no tests, no path/file I/O, and no agent rules.

## Scope

- **Base:** `origin/development`
- **Head:** `fix/1694-catalog-rest-bean-registration`
  (no commits yet; single working-tree modification, file uncommitted).
- **Files:** 1 changed (`projects/sitemanage-beans.xml`), `+5` lines.
- **Precedent:** commit `62004b86d5` (#1714) — same file, same pattern,
  accepted by maintainers.
- **Memory patterns hit:** none of the hard gates apply (no new logic, no
  new tests, no path/I/O, no agent rule edits, no security surface). The
  change-class companion checklist from `projects/sitemanage/AGENTS.md`
  *did* apply (verified below).
- **Prior report:** none for this branch/ticket. Cross-referenced with the
  general precedent above.

## Recommendation

**approve** — May commit / push / open PR: **yes**.

## Gate

- Blocking bugs: **0**
- Maintainability suggestions: 1 (informational; non-blocking)
- Nits: 0
- May commit/push: **yes**

## Verification matrix

Five beans × four required checks. All rows pass.

| Bean id (XML `ref bean=`)            | Resource exists w/ matching `@PSSiteManageBean` | Apibridge `IXxxAdaptor` impl in `projects/sitemanage` | `TestXxxAdaptor` Spring stub in `rest` test classpath | Mockito `*ResourceTest` in `rest` |
|--------------------------------------|------------------------------------------------|-------------------------------------------------------|-------------------------------------------------------|------------------------------------|
| `restControlsResource`               | ✓ `rest/src/main/java/com/percussion/rest/cecontrols/ControlsResource.java:29` `@PSSiteManageBean(value="restControlsResource")` | ✓ `projects/sitemanage/src/main/java/com/percussion/apibridge/ControlAdaptor.java` | ✓ `rest/src/test/java/com/percussion/rest/test/apibridge/TestControlAdaptor.java:14 implements IControlAdaptor` | ✓ `rest/src/test/java/com/percussion/rest/cecontrols/ControlsResourceTest.java` |
| `restSearchResource`                 | ✓ `rest/src/main/java/com/percussion/rest/searches/SearchResource.java:29` `@PSSiteManageBean(value="restSearchResource")`     | ✓ `projects/sitemanage/src/main/java/com/percussion/apibridge/SearchAdaptor.java`  | ✓ `rest/src/test/java/com/percussion/rest/test/apibridge/TestSearchAdaptor.java:27 implements ISearchAdaptor`     | ✓ `rest/src/test/java/com/percussion/rest/searches/SearchResourceTest.java` |
| `restViewResource`                   | ✓ `rest/src/main/java/com/percussion/rest/views/ViewResource.java:29` `@PSSiteManageBean(value="restViewResource")`           | ✓ `projects/sitemanage/src/main/java/com/percussion/apibridge/ViewAdaptor.java`    | ✓ `rest/src/test/java/com/percussion/rest/test/apibridge/TestViewAdaptor.java:27 implements IViewAdaptor`        | ✓ `rest/src/test/java/com/percussion/rest/views/ViewResourceTest.java` |
| `restServerConfigsResource`          | ✓ `rest/src/main/java/com/percussion/rest/serverconfigs/ServerConfigsResource.java:29` `@PSSiteManageBean(value="restServerConfigsResource")` | ✓ `projects/sitemanage/src/main/java/com/percussion/apibridge/ServerConfigAdaptor.java` | ✓ `rest/src/test/java/com/percussion/rest/test/apibridge/TestServerConfigAdaptor.java:14 implements IServerConfigAdaptor` | ✓ `rest/src/test/java/com/percussion/rest/serverconfigs/ServerConfigsResourceTest.java` |
| `restRelationshipTypeResource`       | ✓ `rest/src/main/java/com/percussion/rest/relationshiptypes/RelationshipTypeResource.java:42` `@PSSiteManageBean(value="restRelationshipTypeResource")` | ✓ `projects/sitemanage/src/main/java/com/percussion/apibridge/RelationshipTypeAdaptor.java` | ✓ `rest/src/test/java/com/percussion/rest/test/apibridge/TestRelationshipTypeAdaptor.java:27 implements IRelationshipTypeAdaptor` | ✓ `rest/src/test/java/com/percussion/rest/relationshiptypes/RelationshipTypeResourceTest.java` |

**Companion closure check** (per `projects/sitemanage/AGENTS.md` →
**apibridge architecture → Adaptor implementation checklist** +
root `AGENTS.md` → **Change-class completeness**): every bean id wired
in this diff already has the full `rest` ↔ `sitemanage` artifact set in
place — adaptor interface in `rest`, adaptor impl under `com.percussion.apibridge`
in `sitemanage`, `TestXxxAdaptor` Spring stub in
`rest/src/test/java/com/percussion/rest/test/apibridge/`, and a
`XxxResourceTest` covering the resource. **No new companions are required
by this change** — the change only adds wiring entries; it does not add a
new resource, interface, or adaptor. The `MainTest` Spring context risk
(`No qualifying bean of type '…Adaptor'`) therefore does **not** apply.

## Cross-platform / Spotless / hygiene

- **Cross-platform path / file I/O:** **N/A**. XML-only diff in a packaged
  Spring config file; no filesystem paths, no path assertions, no
  installers, no packaging scripts touched. Checklist explicitly applied,
  outcome: clean.
- **Spotless:** root `pom.xml` (lines 2434-2810) configures Spotless for
  **`java`** (`googleJavaFormat`) and **`markdown`** only. There is no
  `<xml>` / `<wtp>` formatter block in the root config and no spotless
  override in `projects/sitemanage/pom.xml` (verified by grep). The XML
  file will not be rewritten by `./mvnw spotless:apply`. Indentation in
  the diff matches the surrounding `2-space` / `12-space` style. No
  formatting issue.
- **Merge from `origin/development`:** `git log origin/development..HEAD`
  is empty (no commits yet on the branch) and `git status -sb` shows only
  the single intended XML modification. Merge introduced no conflicts;
  the five new `<ref bean=…/>` lines apply cleanly into the existing
  `<jaxrs:serviceBeans>` block.
- **Out-of-scope Spotless rewrite:** not applicable — XML is not a
  Spotless-covered format in this monorepo, and the diff is the only
  working-tree change besides the review report.
- **Bean count after change:** 33 (28 prior + 5 new) — matches the matrix.
- **Co-author footer rule:** not applicable for the review artifact. The
  eventual commit/PR must include the
  `> Co-Authored by Kilo using MiniMax-M3 with agent kilo.` footer per
  `AGENTS.md` and `.kilo/rules/co-author-attribution.md`.

## Findings

### Issue 1 — Severity: informational
- File: `projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml:90`
- Description: `/services/extensions/catalog` is also listed in #1694 as
  returning 500 on QA install, but `restExtensionsResource` is **already**
  wired at line 90 (and has been since the original server definition).
  This is therefore **not** a bean-registration gap; it is a separate
  runtime / adaptor behavior issue (likely in
  `ExtensionsAdaptor`/`ExtensionsResource` or a downstream dependency)
  that this PR deliberately does not fix. The PR description / commit
  message should make this scope boundary explicit so reviewers do not
  flag the unfixed `extensions` 500 as regression coverage of this PR.
- Suggestion: In the commit message and PR body, call out that this
  change is the **registration half** of the Developer Catalog fix and
  that the `extensions` 500 is a **separate** adaptor-side defect,
  tracked / to be tracked separately. No code change required for this
  review.
- Status: open (informational)
- Pattern-id: (no pattern match; out-of-scope note)

### Issue 2 — Severity: nit
- File: same XML
- Description: The `<jaxrs:serviceBeans>` list now mixes three
  formatting styles:
  - `restCommunityResource"/>` (no space before self-closing slash — pre-existing)
  - `restAclResource" />`, `restControlsResource"/>`, etc. (single space before slash — pre-existing + new)
  The new lines are consistent with their immediate neighbors in the
  block. No change needed; the inconsistency is pre-existing and not
  introduced by this diff.
- Suggestion: None — leave for a future spotless-XML / general cleanup.
  Do not block.
- Status: open (informational, not blocking)

## Out-of-scope concerns (not blocking)

- The precedent commit `62004b86d5` (#1714) also fixed an unrelated
  WebUI Vitest test issue (`App.test.tsx` React Router URL setup). The
  current PR for #1694 does **not** include a WebUI test fix. That is
  acceptable scope discipline for this branch; if there is a matching
  WebUI test flake on #1694, it should be addressed in a separate PR
  (per root `AGENTS.md` → **Change-class completeness**: companions must
  match the change class, not be invented).
- Rest module Spring `MainTest` and the new `Test*Adaptor` stubs have
  already shipped on `origin/development` (the stubs exist on disk today).
  A standalone `./mvnw -pl rest clean install` is still required per
  root `AGENTS.md` → **Pre-PR Maven verification**, but no new test
  wiring is introduced by this diff.

## Evidence

- `git status -sb` →
  `## fix/1694-catalog-rest-bean-registration...origin/development`
  (one modified file, plus the untracked review report draft).
- `git diff` against the working tree (file-level) → exact +5 lines shown
  above in the task brief, all on the existing `<jaxrs:server
  id="rest-jax-rs">` block.
- `git log origin/development..HEAD --oneline` → empty (no commits yet).
- Bean id ↔ resource mapping verified by ripgrep on
  `@PSSiteManageBean` across
  `rest/src/main/java/com/percussion/rest/**.java`; 37 matches, all 5
  required ids present.
- Apibridge adaptor impls verified by `glob` for each of the 5 paths
  under `projects/sitemanage/src/main/java/com/percussion/apibridge/`.
- Test stubs verified by `glob` for each of the 5 paths under
  `rest/src/test/java/com/percussion/rest/test/apibridge/` plus grep
  showing each stub `implements` the matching `IXxxAdaptor` interface.
- Mockito resource tests verified by `glob` for each of the 5 paths under
  `rest/src/test/java/com/percussion/rest/<area>/`.
- Precedent confirmed: `git show 62004b86d5 --stat` shows identical
  `sitemanage-beans.xml` fix with 5 lines added and accepted on
  `origin/development`.
- Spotless XML coverage confirmed absent by inspecting the root
  `pom.xml` `<plugin>spotless-maven-plugin</plugin>` block (lines
  2434-2810): only `<java>` and `<markdown>` formatters are configured.

## Stop / handoff

- Scope reviewed end-to-end; findings are severity-tagged and reasonable.
- Recommendation is `approve`.
- No bug-level findings — May commit / push / open PR: **yes**.
- Durable artifact: this file at
  `docs/ai-generated/code-reviews/erlang-fix-1694-catalog-rest-bean-registration-pre-pr.md`.
- Author (Kilo / Hephaestus) may proceed to:
  1. `git add projects/sitemanage-beans.xml`
  2. Commit with message including `Fixes #1694` and the
     `> Co-Authored by Kilo using MiniMax-M3 with agent kilo.` footer.
  3. Run `./mvnw -pl projects/sitemanage clean install` (and
     `./mvnw -pl rest clean install` if the rest module is rebuilt).
  4. `./mvnw spotless:apply && ./mvnw spotless:check` (apply is a no-op
     for XML in this monorepo but is the documented pre-PR gate).
  5. Push branch and open PR against `origin/development` with
     `gh pr create --base development --head fix/1694-catalog-rest-bean-registration`.
- Pattern memory: no new generalized pattern to promote — this is a
  mechanical wiring fix that exactly mirrors the already-documented
  precedent (`62004b86d5` / #1714) and is captured as the
  "new `IXxxAdaptor` → register `<ref bean>` in `sitemanage-beans.xml`"
  step of the `projects/sitemanage/AGENTS.md` **apibridge**
  implementation checklist.
