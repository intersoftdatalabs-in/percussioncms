## Summary

Machine analysis found **222** finding(s), **2** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 912 analyzed
- In-diff: 5 finding(s); preexisting: 217
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxfix/PSRxFixCmd.java:356 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 356)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSSFtpDeliveryHandler.java:199 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 199)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSSFtpDeliveryHandler.java:225 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 225)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/data/PSSite.java:258 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 258)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/DefaultAuthHandler.java:1247 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1247)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/DefaultAuthHandler.java:1249 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1249)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/DefaultAuthHandler.java:1251 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1251)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 8 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1512 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1512)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 9 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1518 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1518)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 10 -- Severity: bug

- File: system/Tools/InlineLinkConverter/src/com/percussion/inlinelinkconverter/PSInlineLinkConverter.java:532 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 532)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: system/Tools/InlineLinkConverter/src/com/percussion/inlinelinkconverter/PSInlineLinkConverter.java:539 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 539)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: system/Tools/InlineLinkConverter/src/com/percussion/inlinelinkconverter/PSInlineLinkConverter.java:1195 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1195)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 13 -- Severity: bug

- File: system/Tools/InlineLinkConverter/src/com/percussion/inlinelinkconverter/PSInlineLinkConverter.java:1209 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1209)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 14 -- Severity: bug

- File: system/business/src/com/percussion/delivery/metadata/PSMetadataExtractorService.java:142 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 142)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 15 -- Severity: bug

- File: system/business/src/com/percussion/delivery/metadata/PSMetadataExtractorService.java:150 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 150)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 16 -- Severity: bug

- File: system/business/src/com/percussion/delivery/metadata/PSMetadataExtractorService.java:154 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 154)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 17 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java:288 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 288)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 18 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java:452 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 452)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 19 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java:464 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 464)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 20 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java:637 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 637)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 21 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java:647 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 647)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 22 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSSFtpClientJSch.java:141 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 141)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 23 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSSFtpDeliveryHandler.java:364 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 364)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 24 -- Severity: bug

- File: system/business/src/com/percussion/rx/delivery/impl/PSSFtpDeliveryHandler.java:466 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 466)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 25 -- Severity: bug

- File: system/business/src/com/percussion/rx/publisher/impl/PSRxPublisherService.java:844 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 844)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 26 -- Severity: bug

- File: system/business/src/com/percussion/rx/publisher/impl/PSRxPublisherService.java:866 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 866)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 27 -- Severity: bug

- File: system/services/src/com/percussion/services/aaclient/PSActionExecutor.java:487 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 487)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 28 -- Severity: bug

- File: system/services/src/com/percussion/services/aaclient/PSActionExecutor.java:587 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 587)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 29 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/data/PSAssemblyWorkItem.java:1339 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1339)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 30 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/data/PSAssemblyWorkItem.java:1354 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1354)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 31 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/impl/PSAssemblyServlet.java:429 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 429)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 32 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/impl/PSInlineLinkContentHandler.java:840 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 840)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 33 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/impl/PSInlineLinkContentHandler.java:841 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 841)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 34 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/impl/plugin/PSVelocityAssembler.java:701 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 701)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 35 -- Severity: bug

- File: system/services/src/com/percussion/services/assembly/ui/PSVariantMigrationBean.java:635 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 635)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 36 -- Severity: bug

- File: system/services/src/com/percussion/services/contentmgr/impl/legacy/PSTypeConfiguration.java:1111 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1111)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 37 -- Severity: bug

- File: system/services/src/com/percussion/services/menus/PSContentTypeActionMenuHelper.java:312 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 312)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 38 -- Severity: bug

- File: system/services/src/com/percussion/services/menus/PSTemplateActionMenuHelper.java:232 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 232)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 39 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1004 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1004)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 40 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1007 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1007)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 41 -- Severity: bug

- File: system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:1059 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1059)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 42 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:231 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 231)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 43 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:233 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 233)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 44 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:234 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 234)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 45 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:236 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 236)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 46 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:237 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 237)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 47 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:239 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 239)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 48 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:240 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 240)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 49 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:242 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 242)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 50 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:243 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 243)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 51 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:245 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 245)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 52 -- Severity: bug

- File: system/services/src/com/percussion/services/virtualsite/PSInMemoryVirtualParticipantService.java:274 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 274)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 53 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSState.java:449 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 449)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 54 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSState.java:451 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 451)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 55 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSWorkflow.java:362 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 362)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 56 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSWorkflow.java:364 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 364)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 57 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSWorkflow.java:370 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 370)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 58 -- Severity: bug

- File: system/services/src/com/percussion/services/workflow/data/PSWorkflow.java:372 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 372)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 59 -- Severity: bug

- File: system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:125 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 125)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 60 -- Severity: bug

- File: system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:689 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 689)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 61 -- Severity: bug

- File: system/servlet/src/com/percussion/hooks/servlet/RhythmyxServlet.java:786 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 786)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 62 -- Severity: bug

- File: system/servlet/src/com/percussion/util/servlet/PSServletRequester.java:455 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 455)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 63 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/method/PSPropFindMethod.java:185 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 185)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 64 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/method/PSPropFindMethod.java:225 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 225)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 65 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/method/PSPropFindMethod.java:337 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 337)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 66 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/method/PSPropFindMethod.java:493 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 493)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 67 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/method/PSWebdavMethod.java:1016 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1016)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 68 -- Severity: bug

- File: system/servlet/src/com/percussion/webdav/objectstore/PSWebdavConfig.java:85 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 85)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 69 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/CookieModule.java:287 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 287)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 70 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/HTTPResponse.java:444 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 444)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 71 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/HTTPResponse.java:564 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 564)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 72 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/Response.java:964 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 964)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 73 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/Response.java:968 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 968)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 74 -- Severity: bug

- File: system/src/main/java/com/percussion/HTTPClient/Response.java:1077 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1077)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 75 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/PSApplicationBuilder.java:984 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 984)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 76 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/PSApplicationBuilder.java:1002 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1002)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 77 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/PSApplicationBuilder.java:1236 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1236)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 78 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/PSChoiceBuilder.java:650 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 650)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 79 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSCloneCommandHandler.java:355 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 355)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 80 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSCommandHandler.java:221 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 221)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 81 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSConditionalCloneHandler.java:256 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 256)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 82 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSContentEditorHandler.java:1241 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1241)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 83 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSCopyHandler.java:1474 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1474)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 84 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSCopyHandler.java:1481 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1481)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 85 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSEditCommandHandler.java:126 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 126)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 86 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSEditCommandHandler.java:133 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 133)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 87 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSQueryCommandHandler.java:759 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 759)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 88 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSRelationshipCommandHandler.java:386 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 386)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 89 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSRelationshipCommandHandler.java:388 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 388)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 90 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSRelationshipCommandHandler.java:390 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 390)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 91 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSRelationshipCommandHandler.java:392 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 392)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 92 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSSearchCommandHandler.java:572 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 572)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 93 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSSearchCommandHandler.java:582 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 582)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 94 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSSearchCommandHandler.java:588 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 588)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 95 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSSearchCommandHandler.java:635 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 635)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 96 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSFolder.java:951 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 951)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 97 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSItemDefinition.java:158 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 158)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 98 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSItemField.java:225 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 225)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 99 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSItemField.java:232 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 232)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 100 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/server/PSActiveAssemblerProcessor.java:152 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 152)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 101 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/server/PSServerItem.java:1208 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1208)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 102 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/server/PSServerItem.java:1216 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1216)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 103 -- Severity: bug

- File: system/src/main/java/com/percussion/content/PSHtmlParser.java:1125 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1125)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 104 -- Severity: bug

- File: system/src/main/java/com/percussion/content/PSHtmlParser.java:1151 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1151)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 105 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSContentItemStatusExtractor.java:173 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 173)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 106 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSDtdRelationalMapper.java:253 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 253)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 107 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSDtdRelationalMapper.java:273 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 273)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 108 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSDtdRelationalMapper.java:282 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 282)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 109 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSDtdRelationalMapper.java:288 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 288)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 110 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSRequestLinkGenerator.java:114 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 114)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 111 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSResultSetXmlConverter.java:839 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 839)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 112 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSResultSetXmlConverter.java:888 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 888)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 113 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSResultSetXmlConverter.java:1563 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1563)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 114 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSResultSetXmlConverter.java:1789 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1789)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 115 -- Severity: bug

- File: system/src/main/java/com/percussion/data/PSXmlFieldExtractor.java:138 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 138)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 116 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/PSXmlDatabaseMetaData.java:2719 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2719)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 117 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/PSXmlDocumentQuery.java:1087 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1087)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 118 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/PSXmlDocumentQuery.java:1202 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1202)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 119 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/sqlparser/SQLParser.java:442 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 442)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 120 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/sqlparser/TokenMgrError.java:110 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 110)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 121 -- Severity: bug

- File: system/src/main/java/com/percussion/data/jdbc/sqlparser/TokenMgrError.java:113 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 113)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 122 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSApplication.java:1824 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1824)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 123 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSContentItemStatus.java:126 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 126)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 124 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSContentTypeHelper.java:915 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 915)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 125 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSField.java:941 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 941)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 126 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSField.java:943 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 943)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 127 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSNamedReplacementValue.java:186 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 186)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 128 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSBackendTableConverter.java:82 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 82)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 129 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSBackendTableConverter.java:104 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 104)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 130 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSBackendTableConverter.java:112 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 112)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 131 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSBackendTableConverter.java:116 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 116)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 132 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSBackendTableConverter.java:139 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 139)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 133 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSContentTypeWorkflowsUpdater.java:245 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 245)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 134 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSSecurityProviderConverter.java:100 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 100)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 135 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSSecurityProviderConverter.java:106 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 106)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 136 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSSecurityProviderConverter.java:108 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 108)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 137 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSSecurityProviderConverter.java:210 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 210)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 138 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSTableLocatorConverter.java:84 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 84)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 139 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSTableLocatorConverter.java:104 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 104)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 140 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSTableLocatorConverter.java:112 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 112)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 141 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSTableLocatorConverter.java:116 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 116)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 142 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/legacy/PSTableLocatorConverter.java:185 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 185)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 143 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/server/PSDatabaseComponentLoader.java:278 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 278)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 144 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/server/PSXmlObjectStoreHandler.java:555 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 555)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 145 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionDef.java:342 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 342)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 146 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionHandler.java:584 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 584)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 147 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java:157 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 157)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 148 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java:492 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 492)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 149 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java:570 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 570)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 150 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java:572 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 572)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 151 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSJavaExtensionHandler.java:153 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 153)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 152 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSJavaExtensionHandler.java:193 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 193)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 153 -- Severity: bug

- File: system/src/main/java/com/percussion/extension/PSJavaExtensionHandler.java:273 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 273)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 154 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/globaltemplate/PSRxGlobals.java:80 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 80)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 155 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/globaltemplate/PSRxGlobals.java:329 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 329)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 156 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/globaltemplate/PSRxGlobals.java:373 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 373)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 157 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1225 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1225)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 158 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1239 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1239)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 159 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1245 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1245)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 160 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSApplicationHandler.java:1984 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1984)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 161 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSRequest.java:464 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 464)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 162 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSResponse.java:635 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 635)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 163 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSUserSession.java:497 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 497)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 164 -- Severity: bug

- File: system/src/main/java/com/percussion/server/PSUserSession.java:515 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 515)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 165 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:2844 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2844)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 166 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:2922 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2922)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 167 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:3477 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3477)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 168 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:3479 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3479)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 169 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:3717 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3717)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 170 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:4054 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 4054)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 171 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java:5599 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5599)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 172 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/crosssite/PSCrossSiteFolderActionProcessor.java:236 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 236)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 173 -- Severity: bug

- File: system/src/main/java/com/percussion/server/webservices/crosssite/PSCrossSiteFolderActionProcessor.java:243 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 243)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 174 -- Severity: bug

- File: system/src/main/java/com/percussion/system/utils/PSWorkflowInfo.java:360 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 360)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 175 -- Severity: bug

- File: system/src/main/java/com/percussion/xml/PSDtdTree.java:852 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 852)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 176 -- Severity: bug

- File: system/src/main/java/com/percussion/xml/PSDtdTree.java:1660 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1660)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 177 -- Severity: bug

- File: system/src/test/java/com/percussion/design/objectstore/PSControlDependencyMapTest.java:303 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 303)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 178 -- Severity: bug

- File: system/src/test/java/com/percussion/design/objectstore/PSObjectStoreSerialFieldNonEditorsTest.java:128 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 128)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 179 -- Severity: bug

- File: system/src/test/java/com/percussion/design/objectstore/PSObjectStoreSerialFieldNonEditorsTest.java:134 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 134)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 180 -- Severity: bug

- File: system/src/test/java/com/percussion/design/objectstore/PSRelationshipConfigTest.java:163 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 163)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 181 -- Severity: bug

- File: system/src/test/java/com/percussion/process/PSProcessDaemonPathInjectionTest.java:186 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 186)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 182 -- Severity: bug

- File: system/src/test/java/com/percussion/process/PSProcessDaemonPathInjectionTest.java:210 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 210)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 183 -- Severity: bug

- File: system/src/test/java/com/percussion/server/PSServerInitLockSearchTypedTest.java:103 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 103)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 184 -- Severity: bug

- File: system/src/test/java/com/percussion/server/PSServerInitLockSearchTypedTest.java:110 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 110)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 185 -- Severity: bug

- File: system/src/test/java/com/percussion/test/http/HttpRequest.java:457 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 457)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 186 -- Severity: bug

- File: system/src/test/java/com/percussion/test/http/HttpRequest.java:462 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 462)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 187 -- Severity: bug

- File: system/src/test/java/com/percussion/test/http/HttpRequest.java:493 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 493)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 188 -- Severity: bug

- File: system/src/test/java/com/percussion/test/install/action/RxFileManagerTest.java:173 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 173)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 189 -- Severity: bug

- File: system/src/test/java/com/percussion/test/install/action/RxFileManagerTest.java:185 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 185)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 190 -- Severity: bug

- File: system/src/test/java/com/percussion/testing/PSTestRunner.java:70 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 70)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 191 -- Severity: bug

- File: system/src/test/java/com/percussion/testing/PSTestRunnerJunit.java:70 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 70)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 192 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:71 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 71)
- Status: open

### Issue 193 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:185 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 185)
- Status: open

### Issue 194 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:471 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 471)
- Status: open

### Issue 195 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:599 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 599)
- Status: open

### Issue 196 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:604 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 604)
- Status: open

### Issue 197 -- Severity: bug

- File: system/Tools/RxFix/src/com/percussion/rxverify/modules/PSJdbcTableCheck.java:606 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 606)
- Status: open

### Issue 198 -- Severity: bug

- File: system/services/src/com/percussion/services/relationship/impl/PSJDBCQueryHelper.java:610 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 610)
- Status: open

### Issue 199 -- Severity: bug

- File: system/services/src/com/percussion/services/relationship/impl/PSJDBCQueryHelper.java:618 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 618)
- Status: open

### Issue 200 -- Severity: bug

- File: system/services/src/com/percussion/services/relationship/impl/PSRelationshipService.java:455 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 455)
- Status: open

### Issue 201 -- Severity: bug

- File: system/services/src/com/percussion/services/relationship/impl/PSRelationshipService.java:904 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 904)
- Status: open

### Issue 202 -- Severity: bug

- File: system/services/src/com/percussion/services/relationship/impl/PSRelationshipService.java:934 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 934)
- Status: open

### Issue 203 -- Severity: bug

- File: system/src/test/java/com/percussion/data/jdbc/PSXmlDatabaseMetaDataTest.java:201 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 201)
- Status: open

### Issue 204 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:95 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 95)
- Status: open

### Issue 205 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:105 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 105)
- Status: open

### Issue 206 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:178 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 178)
- Status: open

### Issue 207 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:225 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 225)
- Status: open

### Issue 208 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:235 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 235)
- Status: open

### Issue 209 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:276 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 276)
- Status: open

### Issue 210 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:286 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 286)
- Status: open

### Issue 211 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:328 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 328)
- Status: open

### Issue 212 -- Severity: bug

- File: system/src/test/java/com/percussion/extension/PSExtensionDefFactoryTest.java:338 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 338)
- Status: open

### Issue 213 -- Severity: bug

- File: system/src/test/java/com/percussion/log/PSLogManagerThreadTest.java:281 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 281)
- Status: open

### Issue 214 -- Severity: bug

- File: system/src/test/java/com/percussion/log/PSLogManagerThreadTest.java:297 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 297)
- Status: open

### Issue 215 -- Severity: bug

- File: system/src/test/java/com/percussion/log/PSLogManagerThreadTest.java:348 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 348)
- Status: open

### Issue 216 -- Severity: bug

- File: system/src/test/java/com/percussion/test/ant/PSMakeFilterFileTask.java:135 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 135)
- Status: open

### Issue 217 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/objectstore/PSDbComponentList.java:364 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `fromXmlLoad` cognitive=31 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 218 -- Severity: bug

- File: system/src/main/java/com/percussion/xml/PSDtdTree.java:959 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getOccurrenceSetting` cognitive=31 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 219 -- Severity: suggestion

- File: system/src/test/java/com/percussion/security/PSBackEndTableDirectoryCatalogerTest.java:34 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 34)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 220 -- Severity: suggestion

- File: system/src/main/java/com/percussion/cms/objectstore/PSDbComponentSet.java:353 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `fromXmlLoad` cognitive=24 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 221 -- Severity: suggestion

- File: system/src/main/java/com/percussion/cms/objectstore/PSProcessorProxy.java:438 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getProcessor` cognitive=18 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 222 -- Severity: suggestion

- File: system/src/test/java/com/percussion/debug/PSTraceMessageFactoryTest.java:31 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `testMessages` cognitive=3 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

