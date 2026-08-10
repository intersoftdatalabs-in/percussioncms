# Erlang review — issue #2062 (Child B Betwixt purge)

**Scope:** Drop dual-engine Betwixt rollback; purge `commons-betwixt` POMs / `.betwixt` leftovers.  
**Change class:** Shared XML serialization engine removal + packaging dependency closure.

## Checklist

|               Gate                |                                                    Result                                                     |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------|
| Bugs / dual-engine leftover paths | Pass — facade Jackson-only; `PSBetwixtObjectConverter` deleted                                                |
| Portable paths                    | N/A (no new file I/O path construction)                                                                       |
| Behavioral unit tests             | Pass — utils facade/Jackson tests updated; Betwixt-only cases removed; system engine-property asserts removed |
| Module clean install              | Pass — `modules/utils` then `system` standalone `clean install`                                               |
| Spotless apply → check            | Pass (in-scope only committed)                                                                                |
| `commons-betwixt` on utils tree   | Pass — `dependency:tree -Dincludes=commons-betwixt*` empty                                                    |
| Residual production `.betwixt`    | Pass — 0 under source (non-target)                                                                            |

## Companions

- Root `dependencyManagement` entry removed
- `modules/utils` direct dependency removed
- Packaging exclusions removed (no longer needed): perc-distribution-tree, DTS suite, DTS distribution
- Installer classpath: `install.sh` + Eclipse launch config
- Fake ant manifest fixture jar name updated (not a runtime dep)
- `PSBetwixtIdrefExpander` **retained** (Jackson pre-read for historical package idrefs; no Apache Betwixt dependency)

## Intentional remaining string mentions

- Jetty logging regression test asserts log config does not reintroduce `org.apache.commons.betwixt.*` logger names
- Historical docs under `docs/ai-generated/**` and a #1824 comment in `PSOImportJexl`
- Class name `PSBetwixtIdrefExpander` (product idref expander, not the Betwixt library)

## Verdict

**Ready for PR** — no hard-gate findings.
