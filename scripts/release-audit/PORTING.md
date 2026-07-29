# Per-Item Porting Workflow

This workflow describes how to take a single item from `migration-backlog.md` and produce a development-branch PR.

## Pre-flight

Before opening a porting PR, verify:

1. **Run the audit** (so `migration-backlog.md` is current):

   ```bash
   bash scripts/release-audit/release-audit.sh \
     --from-tag v8.1.6 --to-tag v8.1.7 \
     --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
   ```
2. **Identify the backlog item** — pick a PR from `migration-backlog.md`. P0 (security) items first.
3. **Read module AGENTS** — apply Rule Discovery Protocol (root `AGENTS.md` → module `AGENTS.md`/`AGENTS.local.md`) for the target module.
4. **Resolve dev paths** — if the v8.1.7 PR touched files under `system/Packages/`, the dev branch moved them to `modules/perc-packages/src/main/resources/Packages/`. Use `resolve_dev_path` from `lib/verdicts.sh` to map.

## Branch creation and cherry-pick

```bash
# Source the lib so the helpers are in scope
source scripts/release-audit/lib/port.sh

# Cherry-pick creates the feature branch and cherry-picks the merge commit
BRANCH=$(cherry_pick_pr 894 development)
echo "Feature branch: $BRANCH"
```

If the cherry-pick conflicts:

1. Resolve each conflict in the editor.
2. `git add <resolved-files>`
3. `git cherry-pick --continue`
4. Re-run `flag_jdk8_idioms` to scan for new JDK 8 idioms introduced by the conflict resolution.

## JDK 8 idiom translation

```bash
# Save the cherry-picked commit's diff
git show HEAD > /tmp/cherry-picked.diff

# Scan for JDK 8 idioms; warnings file lists each match
flag_jdk8_idioms /tmp/cherry-picked.diff /tmp/jdk8-warnings.txt

# If warnings exist, translate each one:
#   javax.ws.rs.*     → jakarta.ws.rs.*
#   javax.persistence.* → jakarta.persistence.*
#   javax.xml.bind.*  → jakarta.xml.bind.* (or remove; JAXB removed from JDK 11+)
#   sun.misc.*        → find a public replacement
#   com.sun.*         → find a public replacement
```

## Test execution

```bash
# Run the v8.1.7 test class on JDK 21
./mvnw -pl rest -am test -Dtest=PagesTest

# Run Spotless
./mvnw -pl rest -am spotless:check
```

If `PagesTest` does not exist in the dev branch, port the test class from the v8.1.7 PR's diff:

```bash
git log v8.1.7 --oneline -- "rest/src/test/java/.../PagesTest.java"
git show <v817-commit-sha> -- "rest/src/test/java/.../PagesTest.java" \
  | git apply -p1 --directory=rest/src/test
```

## PR creation

The PR body MUST cite:

1. The v8.1.7 PR URL: `https://github.com/intersoftdatalabs-in/percussioncms/pull/<NNN>`
2. The backlog item reference (this spec: `specs/005-migrate-8.1.7-changes/migration-backlog.md`)
3. A summary of any JDK 8 → JDK 21 translations applied
4. A list of new/modified tests

```bash
git push -u origin "$BRANCH"
gh pr create --base development --head "$BRANCH" \
  --title "Migrate PR #<NNN>: <one-line summary>" \
  --body "$(cat <<'EOF'
Cherry-pick of intersoftdatalabs-in/percussioncms#<NNN> from v8.1.7.

Resolves backlog item in specs/005-migrate-8.1.7-changes/migration-backlog.md.

JDK 8 → JDK 21 translations:
- <file>: <jakarta migration or removal note>

Tests:
- <test-class>: <new or modified>
EOF
)"
```

## Per Constitution Principle IX (PR Review Comment Resolution)

When review comments arrive on the porting PR:

1. **Locate** review threads via the GraphQL query in root `AGENTS.md` "PR Review Comment Resolution".
2. **Reply inline** with a citation of the commit hash that fixes the issue and a short description.
3. **Resolve** each thread via the `resolveReviewThread` GraphQL mutation.
4. **Re-verify** via the GraphQL query that every thread is `isResolved: true` before claiming the work complete.

A code-only fix that does not also resolve the corresponding review thread is incomplete from the merge-readiness perspective.

## When to skip

Mark a backlog item `skip` in the backlog's per-PR row only when:

- The dev branch's replacement is functionally equivalent AND confirmed by review.
- The dev branch has deliberately removed the affected surface (e.g. gadget registry refactor).

Do NOT mark `skip` solely because the diff is large. Large cherry-picks that compile and pass tests on JDK 21 are still in scope.
