# Erlang review — issue #1919 Jackson-migrate publisher/pubserver

**Reviewer persona:** Erlang (independent of implementer)  
**Scope:** `PSContentList`, `PSEdition`, `PSDeliveryType`, `PSPubServer` (+ nested param/property beans)  
**Date:** 2026-08-05

## Change class

Jackson design-object XML migration for publisher/pubserver domain (annotations + nested `addType` + golden/round-trip unit tests). Peer class: filter #1915 / security #1889 / keyword #1888.

## Checklist

|         Gate          |                                                                  Result                                                                  |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs                  | No open bug findings after fix of Optional/BeanUtils restore on `PSPubServer` and null-safe edition site/pubserver setters               |
| Behavioral unit tests | `PSPublisherXmlSerializationTest` (10), `PSPubServerXmlSerializationTest` (4) — write shape, golden parity, round-trip, legacy null root |
| Portable paths        | No new filesystem path construction; XML only                                                                                            |
| Companions            | Nested param/property types annotated; `addType` registrations; deviations doc; no production `.betwixt` to drop                         |
| Overlap with open PRs | Avoided filter/sitemgr/assembly/ACL/serialization-suite files from #1922/#1958/#1902/#1913/#1904                                         |

## Findings addressed during review

1. **HashSet iteration order** — XML list accessors sort by name for golden stability.
2. **Optional description/serverType** — class-based `fromXML` + dedicated String Jackson setters so restore does not depend on BeanUtils Optional→String copy.
3. **Password property double-encrypt** — `setValueXml` stores raw wire value.
4. **GuidManager locator in unit tests** — `PSGuid` assemble for delivery type / filter / site / pubserver ids.

## Verdict

**PASS** — ready for Spotless + system `clean install` + PR against `main`.
