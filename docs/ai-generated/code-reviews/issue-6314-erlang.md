## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1273
- Rule: `llm.openrouter-free`
- Tool: `llm`
- Description: The diff adds a new FTPS branch to the folder-only update dispatch (and a new FtpsDeliveryFolderPanel wired into SiteWorkspace), but no test changes accompany it. Existing folder-only coverage for Local/FTP/SFTP does not exercise the new FTPS path, so the driver-selection behavior (FTPS routed to PSFtpsDeliveryFolderUpdate rather than falling through to PSFtpDeliveryFolderUpdate) and the documented 255-character/blank-folder semantics are unverified.
- Suggestion: Add a service-level test that issues a folder-only PUT against an FTPS server and asserts PSFtpsDeliveryFolderUpdate is applied (host/port/user/passphrase untouched, blank folder clears, overlong folder rejected), plus a UI test for the new FtpsDeliveryFolderPanel save/cancel/error behavior.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.


## Worker adjudication (2026-11-14)

Issue 1 (suggestion, non-blocking) is a **false positive from diff scope**: the
analyzer saw only 4 tracked modified files because the 7 new files were still
untracked when the CLI ran. The requested tests exist in this same change set:

- `PSFtpsDeliveryFolderUpdateTest` (8 tests: shape, replace, blank-clear,
  null-body, overlong, Local/FTP/SFTP/S3 rejection)
- `deliveryServerFtpsFolder.test.ts` + `FtpsDeliveryFolderPanel.test.tsx`
  (11 vitest tests, green)
- `sitesDeliveryServerFtpsFolder.spec.js` (set + blank-clear + cancel +
  overlong + non-FTPS + 400/403/409, H2 QA cell)

Gate remains **PASS / approve**; no code change required.
