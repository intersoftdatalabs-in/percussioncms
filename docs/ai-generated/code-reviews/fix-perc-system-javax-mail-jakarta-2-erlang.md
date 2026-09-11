# Erlang Code Review — `fix/perc-system-javax-mail-jakarta-2`

**Date:** 2026-09-11  
**Reviewer:** Erlang Shen (independent; did not author this change)  
**Mode:** uncommitted working tree vs `HEAD` (branch even with `origin/main`; no unique commits)  
**Branch:** `fix/perc-system-javax-mail-jakarta-2`  
**Base:** `origin/main` @ `9be98aa9e83739c0e16ddb9e643842ad894adb11`  
**Ignored:** untracked `.grok/workflows/`  
**Change class:** Commons Email 1.6 (`javax.mail`) → `commons-email2-jakarta` 2.0.0-M1 (`jakarta.mail.*`) so perc-system compiles with `com.sun.mail:jakarta.mail` 2.0.2; unused `javax.mail` imports removed from `PSWebServices`.

**Memory patterns hit:** incomplete change-class closure; tests that only grep source tokens; library cutover leaving residual `javax.*` consumers; parent `dependencyManagement` exclusions hiding transitives.

---

## Summary

The artifact/import remap in perc-system, rest, extensions-workflow, and DTS common is directionally correct: `org.apache.commons.mail2.{core,jakarta}` matches Commons Email 2.0.0-M1, `setTLS`/`setSSL` in `PSEmailHelper` map 1:1 to `setStartTLSEnabled`/`setSSLOnConnect` (already used on `main` in `PSWorkFlowUtils` / `PSSecureMailProgram`), and parent BOM now pins `commons-email2-*` plus `com.sun.mail:jakarta.mail` 2.0.2 while excluding the 2.0.1 transitive from email2.

**Re-review (2026-09-11):** Issue 1 is fixed. `PSOEmailUtils` uses `jakarta.mail.*`; `perc-toolkit` declares `com.sun.mail:jakarta.mail` (`provided`); production `import javax.mail` is gone (only negative asserts in tests). Residual suggestions (helper property wiring not driven; M1 note belongs in the PR body; rest POM still pins a redundant version) are **not** hard gates.

## Recommendation

approve

## Gate

**May commit/push: yes**

No open **bug**. Before a GitHub PR, AGENTS still requires standalone `clean install` of every changed Maven module (system, rest, extensions-workflow, DTS `common`, perc-toolkit). Author reported perc-toolkit `BUILD SUCCESS`; Erlang did not re-run Maven.

## Cross-platform path checklist

**Outcome: clean (N/A for production I/O; tests portable).**

Diff does not add filesystem joins, Unix-only roots, or Windows-only paths. New tests use `Path.of(...)` / `Files.readString` / `Files.isRegularFile` and normalize `\r\n` before string asserts. Source-locator fallbacks are cwd-sensitive (module vs repo root) but not OS-separator-sensitive.

## Issues

### Issue 1 -- Severity: bug
- File: `modules/perc-toolkit/src/main/java/com/percussion/pso/utils/PSOEmailUtils.java:26`
- Description: Incomplete cutover. `PSOEmailUtils` still imports `javax.mail.*` (`Address`, `Message`, `Session`, `Transport`, `internet.*`). `modules/perc-toolkit` is a root-reactor module (`pom.xml` `<module>modules/perc-toolkit</module>`) and depends on `perc-system` (`provided`) with **no** mail artifact of its own. On `main`, perc-system’s `commons-email` 1.6 transitively supplies `com.sun.mail:javax.mail`. This change removes that artifact and leaves only `com.sun.mail:jakarta.mail` 2.x (`jakarta.mail.*`). `javax.mail` is gone from every `pom.xml`. The same class of defect already forced the `PSWebServices` unused-import deletion; toolkit was not grepped. Pre-PR “build only modules I touched” will miss this; CI reactor will not.
- Suggestion: Mirror `PSJavaxMailProgram` — switch `PSOEmailUtils` to `jakarta.mail.*`. Confirm standalone `cd modules/perc-toolkit && ../../mvnw.cmd clean install` (and perc-system / extensions-workflow / DTS `common` / `rest`). Do not add a parallel `javax.mail` 1.x dependency to keep two mail namespaces in the product.
- Status: **fixed** (re-review) — `jakarta.mail.*` imports; `perc-toolkit/pom.xml` `com.sun.mail:jakarta.mail` provided; `PSOEmailUtilsJakartaMailNamespaceTest`; tree grep has no production `import javax.mail`.

### Issue 2 -- Severity: suggestion
- File: `deliverytiersuite/delivery-tier-suite/common/src/test/java/com/percussion/delivery/utils/PSEmailHelperCommonsEmail2JakartaTest.java:53`
- Description: The only coverage of the real logic change in `PSEmailHelper` (`setTLS` → `setStartTLSEnabled`, `setSSL` → `setSSLOnConnect` at `PSEmailHelper.java:180-200`) is a source-token grep (`contains(".setTLS(")` / `setStartTLSEnabled`). `missingHostDoesNotInitializedClient` does not set TLS/SSL props. Pattern: tests that only grep source strings do not prove runtime flags. `PSWorkFlowUtilsCommonsEmail2JakartaTest.multiPartEmailAcceptsJakartaAuthenticatorWithoutSend` exercises the **library**, not `PSWorkFlowUtils.sendMailWithAttachment`. `PSJavaxMailProgramJakartaNamespaceTest` / `PSWebServicesJakartaMailNamespaceTest` are `Class.forName` + import greps (import-only production edits; compile is the real proof).
- Suggestion: For `PSEmailHelper`, drive `createMultiPartEmail` (package-visible, test subclass, or reflection) with `EMAIL_PROPS_TLS=true` and a numeric SSL port and assert `isStartTLSEnabled()` / `isSSLOnConnect()` / `getSslSmtpPort()` on the `MultiPartEmail` **without** `send()`. Keep classpath `instanceof Authenticator` tests; drop or demote source greps so they cannot be the sole proof of the remap.
- Status: **partially addressed** (re-review) — `tlsAndSslUseJakartaEmail2Setters` asserts library setters without `send()`. Still does not drive `PSEmailHelper` TLS/SSL properties. Residual suggestion; not a block.

### Issue 3 -- Severity: suggestion
- File: `pom.xml:102`
- Description: `${commons-email.version}` is `2.0.0-M1`, the only Central coordinate for `commons-email2-jakarta` as of this review (Jun 2024 milestone, not GA). Runtime dep of that artifact is `com.sun.mail:jakarta.mail` **2.0.1**; parent correctly excludes it and pins **2.0.2**. Milestone APIs can still move before 2.0.0; acceptable for the Jakarta split but should be explicit in the PR body (GH-4411 companion, not a silent BOM bump).
- Suggestion: Call out M1 + the 2.0.1→2.0.2 pin in the PR. Revisit when Commons Email 2 GA exists. DTS parent DM (`deliverytiersuite/delivery-tier-suite/pom.xml:591`) does not repeat the jakarta.mail exclusion; harmless while only `common` consumes email2 and already excludes, but copy the root exclusion if more DTS modules depend on it.
- Status: **deferred** (re-review) — author will note in PR body; not a code defect.

### Issue 4 -- Severity: nit
- File: `modules/extensions-workflow/src/main/java/com/percussion/workflow/mail/PSJavaxMailProgram.java:33`
- Description: Class Javadoc still says “JAVAX mail provided by Sun” after the `jakarta.mail` import swap. `rest/pom.xml:35` still duplicates `${commons-email.version}` though parent `dependencyManagement` already manages `commons-email2-jakarta`.
- Suggestion: Update the one-line plugin description to Jakarta Mail 2. Drop the redundant rest version (keep the comment). Optional.
- Status: **partially addressed** (re-review) — javadoc now “uses Jakarta Mail.” `rest/pom.xml` still duplicates `${commons-email.version}`. Leftover nit; not a block.

## Issues (none invented)

No production path/I/O bugs in the hunks reviewed. `setTLS`/`setSSL` remaps match Commons Email 1.5+ aliases / 2.x replacements. Parent exclusion of `com.sun.mail:jakarta.mail` from `commons-email2-jakarta` plus explicit 2.0.2 is the right RequireUpperBoundDeps pattern. New test files use Intersoft 2026 Apache headers. Product-docs / Playwright N/A (library namespace; operator SMTP keys unchanged).

## Inspected (beyond hunks)

- `PSWorkFlowUtils.sendMailWithAttachment` (`system/.../PSWorkFlowUtils.java` ~954–1028) — already `setStartTLSEnabled` / `setSSLOnConnect` on `HEAD`; this diff is imports only.
- `PSSecureMailProgram.createMultiPartEmail` — same; already 2.x method names on `HEAD`.
- `PSJavaxMailProgram.sendMessage` — `javax.mail` → `jakarta.mail` types only.
- `PSWebServices.java` — unused `javax.mail` imports removed; no remaining mail API use.
- Root / DTS / system / rest / extensions-workflow POMs; grep of `commons-email`, `org.apache.commons.mail`, `import javax.mail` across `*.java` / `pom.xml`.
- `rest/.../AssetsResource.java:554` — catches `Exception`; needs `EmailException` on compile classpath via email2 (transitive `commons-email2-core`).
- Module `AGENTS.md`: `system/AGENTS.md`, `modules/extensions-workflow/AGENTS.md`. No module override for mail.

## Handoff (initial)

- **Reviewed:** uncommitted POM + mail Java + four new test classes vs `origin/main`; ignored `.grok/workflows/`.
- **Blocker:** `perc-toolkit` `PSOEmailUtils` still on `javax.mail` after removing the last `javax.mail` provider.
- **Secondary:** TLS/SSL remap covered only by source greps; M1 BOM should be disclosed on the PR.
- **Recommendation:** request-changes; **do not commit or open a PR** until Issue 1 is fixed and perc-toolkit + touched modules `clean install` green.
- **Author:** migrate `PSOEmailUtils` (and any other `javax.mail` hit after a full-tree grep), re-run Erlang.

---

## Re-review

**Date:** 2026-09-11  
**Scope:** same branch, uncommitted vs `HEAD` / `origin/main` (still no unique commits). New paths: `modules/perc-toolkit/pom.xml`, `PSOEmailUtils.java`, `PSOEmailUtilsJakartaMailNamespaceTest.java`; DTS helper test gained `tlsAndSslUseJakartaEmail2Setters`; `PSJavaxMailProgram` javadoc. Ignored: `.grok/workflows/`.

**Grep:** no production `import javax.mail`. Hits are comments/negative asserts in tests plus a parent POM comment about 1.6.

### Prior issues

| Issue | Prior | Now |
|-------|--------|-----|
| 1 bug `PSOEmailUtils` javax.mail | open | **fixed** |
| 2 suggestion DTS TLS/SSL behavioral | open | **partial** — library `MultiPartEmail` flags asserted; `PSEmailHelper` property wiring still grep-only |
| 3 suggestion M1 in PR body | open | **deferred** to PR text |
| 4 nit javadoc + rest version | open | **partial** — javadoc fixed; rest version pin remains |

### New findings

None. Toolkit `provided` `jakarta.mail` uses parent DM (2.0.2). `PSOEmailUtils.sendEmail` uses `Session` / `MimeMessage` / `Transport` under `jakarta.mail`. New test copyright Intersoft 2026; `Path.of` portable. Path checklist still **clean**.

### Recommendation (re-review)

**approve**

**May commit/push: yes**

Do not treat leftover Issue 2/3/4 as merge blockers. When opening the PR, include the M1 + jakarta.mail 2.0.2 pin note (Issue 3) and record standalone `clean install` for each changed module.

> Co-Authored by Grok Build 1.0.5 using grok-4-1-fast-reasoning with agent Erlang Shen.
