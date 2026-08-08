# Erlang review — issue #2348 default logrotate samples

**Branch:** `fix/issue-2348-default-logrotate`  
**Change class:** installer/packaging samples + operator docs (no runtime Java behavior change beyond packaging tests)

## Verdict

**PASS** for commit/PR. Packaging samples + structural unit tests + docs; opt-in only; portable paths.

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new logic | N/A runtime — samples are static config; installDts copy is failonerror=false + erroronmissingdir=false |
| Behavioral unit tests | Structural packaging tests (CMS 6, DTS 4, perc-doctor guide assertion) |
| Portable path / file I/O | Pass — generic `/opt/Percussion`, `C:\Percussion`; no developer homes; PS1 resolves install root via script location + `Join-Path` |
| Change-class companions | Samples + README + installDts wiring + packaging tests + perc-doctor docs |
| Auto-enable without consent | Documented opt-in; not written to `/etc/logrotate.d` by installer |
| copytruncate / catalina.out | Present on both CMS and DTS samples (`*.out`) |

## Module clean install

| Module | Result |
|--------|--------|
| `modules/perc-doctor` | BUILD SUCCESS |
| `modules/perc-distribution-tree` | BUILD SUCCESS (LogrotateSamplePackagingTest 6/6) |
| `delivery-tier-distribution` | BUILD SUCCESS (DtsLogrotateSamplePackagingTest 4/4) |

## Residual

None for acceptance criteria. Optional future: installer checkbox to copy into logrotate.d (explicitly out of scope).
