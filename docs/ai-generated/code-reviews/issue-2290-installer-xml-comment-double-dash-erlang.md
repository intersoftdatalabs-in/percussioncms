# Erlang review: fix/issue-2290-installer-xml-comment-double-dash

**Date:** 2026-08-07  
**Branch:** fix/issue-2290-installer-xml-comment-double-dash  
**Base:** origin/main  
**Reviewer persona:** Erlang (strict gate)  
**Author agent:** Grok Build / main / grok-4.5

## Summary

Comment-only fix for invalid XML `--` sequences inside Installer Ant comments that abort CMS install (issue #2290). Adds a cheap well-formedness + comment-body regression test for top-level Installer Ant XML.

## Scope

- `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` — reword `--demo-sites` in comment
- `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/remove_PercussionInstallation.xml` — reword `--clean-install-dir` in comment
- `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/InstallerXmlWellFormedTest.java` — new unit tests

Cross-platform path review: test uses `Path.of` / `Files.list` / `Files.readString`; no hardcoded `/` or `\` joins beyond Path segments; module-relative paths match peer packaging tests. Clean.

Memory patterns: none specific hit; installer packaging tests peer pattern followed.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

(none)

## Evidence

- Full-tree comment scan of Installer `*.xml`: only the two reworded sites had illegal `--`; post-fix count 0
- `cd modules/perc-distribution-tree && ../../mvnw clean install` green
- `InstallerXmlWellFormedTest`: Tests run: 2, Failures: 0, Errors: 0

## Residual (ops / optional)

Live install smoke remains optional human/ops follow-up; not required for this PR.
