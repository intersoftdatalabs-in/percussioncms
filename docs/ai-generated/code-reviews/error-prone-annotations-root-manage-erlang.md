# Erlang review: root-manage error_prone_annotations

## Summary

Manage `com.google.errorprone:error_prone_annotations` at root (`2.48.0`) after
Dependabot gson 2.14.0 (#1881) raised RequireUpperBoundDeps on
`delivery-tier-distribution`. Remove the child pin at `2.41.0`. Add one-line
AGENTS.md rule to prefer root dependency version management.

## Scope

- Uncommitted vs `HEAD` on `main` (pre-branch)
- Files: `pom.xml`, `deliverytiersuite/.../delivery-tier-distribution/pom.xml`, `AGENTS.md`
- Memory patterns hit: agent rule changes need human review (user explicitly
  requested AGENTS update + PR); incomplete change-class not applicable (dep pin only)
- Cross-platform path review: N/A (no file I/O code)

## Recommendation

**approve**

## Gate

- May commit/push: **yes**
- Bugs: none
- Missing behavioral tests: N/A (Maven property / dependencyManagement only)
- Human AGENTS rule approval: **yes** (user requested the rule and PR)

## Issues

None.

## Verification noted

```text
cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution
..\..\..\mvnw.cmd validate
# RequireUpperBoundDeps + DependencyConvergence passed; BUILD SUCCESS
```
