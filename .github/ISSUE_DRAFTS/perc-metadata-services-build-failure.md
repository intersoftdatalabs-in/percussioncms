# [DRAFT] Build failures in delivery-tier-suite/perc-metadata-services (use -DskipTests=true to bypass unrelated test failures)

## Summary

Building the `perc-metadata-services` module fails in CI/local builds and is blocking delivery-tier-suite work. Tests in the forms module are currently being fixed by @vijaya-boddipudi, so please run the build with `-DskipTests=true` to isolate compile/runtime issues in `perc-metadata-services`.

## Repository / Module

- Module: `perc-metadata-services`
- Path: `delivery-tier-suite/perc-metadata-services`

## How to reproduce

From the repository root, run a targeted build for the metadata module while skipping tests:

```bash
# build the module and required upstream modules but skip tests
mvn -pl delivery-tier-suite/perc-metadata-services -am clean install -DskipTests=true
```

Or:

```bash
mvn -pl :perc-metadata-services -am clean install -DskipTests=true
```

## Observed behavior

- The build fails with compilation/runtime errors originating in `perc-metadata-services` (or its transitive dependencies).
- Running without skipping tests shows unrelated failures due to ongoing work in the forms module; therefore use `-DskipTests=true` to focus on the metadata service failures.

## Expected behavior

- `perc-metadata-services` should compile and package successfully (tests can remain skipped until forms tests are fixed), enabling iteration on fixes without noise from unrelated test failures.

## Suggested investigation checklist / likely causes

- [ ] Reproduce with the command above and capture full Maven errors and stack traces.
- [ ] Check `perc-metadata-services/pom.xml` for:
  - inconsistent dependency versions (Jakarta vs javax namespace mismatches),
  - missing explicit dependencies that were previously provided transitively,
  - incorrect packaging/plugin configuration.
- [ ] Inspect recent changes in parent POM or `dependencyManagement` that may have shifted versions.
- [ ] Search source for unresolved imports or usage of removed/deprecated APIs (e.g., jakarta vs javax).
- [ ] Confirm module inter-dependencies — ensure required modules are available in the reactor or declared as dependencies.
- [ ] If compilation succeeds but resources or integration tests fail, confirm resource paths and fixtures (tests are intentionally skipped during triage).

## Proposed fixes (low-risk first)

- Explicitly declare any dependencies required by `perc-metadata-services` instead of relying on transitive dependencies from other modules.
- Align versions in the parent `dependencyManagement` if mismatches are found.
- If there are jakarta/javax namespace inconsistencies, either update imports to the correct namespace or pin dependency versions consistently.
- Validate fixes by running:

  ```bash
  mvn -pl delivery-tier-suite/perc-metadata-services -am clean install -DskipTests=true
  ```
- After fixes, open a PR with the change and mention why tests were skipped while forms fixes are in progress.

## Acceptance criteria

- `mvn -pl delivery-tier-suite/perc-metadata-services -am clean install -DskipTests=true` exits successfully (exit code 0).
- Root cause(s) documented in the issue with relevant stack traces and file references.
- A follow-up PR is opened containing the fix(s) and a short rationale about skipping tests during concurrent forms work.
- If cross-module coordination is required, the PR references and CCs @vijaya-boddipudi.

## Labels / Priority / Assignees

- Labels: `bug`, `build`, `maven`, `delivery-tier-suite`
- Priority: `high` (blocks delivery-tier-suite)
- Suggested assignee: module owner or delivery-tier-suite team; CC @vijaya-boddipudi for awareness

## Additional notes

- Do not re-enable unrelated tests or change unrelated code until the forms-module fixes are merged — use `-DskipTests=true` for CI reruns temporarily.
- Please paste full Maven error output (or attach `mvn -e` logs) to this issue once reproduced; that will speed up root-cause diagnosis.

---

You can post this draft by copy/pasting the file contents into the GitHub Issues UI, or run one of the commands below to create the issue from your machine.

### Create via GitHub CLI (recommended if you have `gh` configured)

```bash
# create an issue with title and body-file; adds labels and assigns (adjust assignees as needed)
gh issue create --repo intersoftdatalabs-in/percussioncms \
  --title "[DRAFT] Build failures in delivery-tier-suite/perc-metadata-services (use -DskipTests=true)" \
  --body-file .github/ISSUE_DRAFTS/perc-metadata-services-build-failure.md \
  --label bug --label build --label maven --label delivery-tier-suite \
  --assignee ""
```

> Note: the GitHub CLI does not support a true "draft issue" flag for all installs; the `[DRAFT]` prefix in the title signals the same intent.

### Create via GitHub REST API (curl)

```bash
# set GITHUB_TOKEN with appropriate repo scope first
GITHUB_TOKEN=YOUR_TOKEN_HERE
curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/intersoftdatalabs-in/percussioncms/issues \
  -d @.github/ISSUE_DRAFTS/perc-metadata-services-build-failure.json
```

Where `.github/ISSUE_DRAFTS/perc-metadata-services-build-failure.json` contains:

```json
{
  "title": "[DRAFT] Build failures in delivery-tier-suite/perc-metadata-services (use -DskipTests=true)",
  "body": "$(sed -e 's/"/\\"/g' .github/ISSUE_DRAFTS/perc-metadata-services-build-failure.md)",
  "labels": ["bug","build","maven","delivery-tier-suite"],
  "assignees": []
}
```

(If you prefer, I can also create the JSON file for you in the repo so you can run the curl command directly.)
