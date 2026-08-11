# Erlang review — issue #2933 install package Xlint residual (slice 1)

**Branch:** `fix/issue-2933-install-xlint-residual`  
**Scope:** `com.percussion.install` core RxUpgrade/pre-upgrade/small plugins rawtypes  
**Date:** 2026-08-11  
**Reviewer persona:** Erlang (implementer self-review)

## Summary

PR-sized batch types install/upgrade framework and smaller helpers with real generics
(`ArrayList<PSPluginResponse>`, `Set<String>`, `List<PSKeyword>`, `Map<String,String>`,
`List<String>` query helpers). Behavioral unit tests cover plugin response storage,
name-clash utility, deprecated-app set, and `constructNewValue`. No product behavior
change intended.

## Recommendation

**approve**

## Gate

- Bugs: none found  
- Behavioral tests: present (`PSInstallPackageTypedTest`, 6 tests green)  
- Cross-platform path checklist: N/A for new path I/O  
- **May commit/push: yes**

## Issues

None.

## Notes

- Left large residual clusters: `PSUpgradePluginRelationship`,
  `PSUpgradePluginConvertCommunityVisibility`, slot-name upgrade, Ora LONG tool,
  CleanLocationSchemes, UpdateExtensions, SpringBeans, Publishing, etc. (~183 raw
  hits remaining) for a follow-up residual issue.
- Public/package API tightenings are source-compatible for typical callers
  (`Set<?>` / `List<String>` / `ArrayList<PSPluginResponse>`).
- Stayed out of services (#2934) and xml/extension (#2935) siblings.

## Build evidence

- `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**  
- Tests run: **1703**, Failures: **0**, Errors: **0**, Skipped: **240**
