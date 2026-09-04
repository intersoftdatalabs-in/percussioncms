# Erlang review — issue #4314 Pipelines Slice B wave 2 pipe IR read

| Field | Value |
|-------|-------|
| **Branch** | `feat/issue-4314-pipelines-pipe-ir-read` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: **yes** |
| **Reviewed** | 2026-09-04 |

## Summary

Adds Admin REST `GET /services/pipelines/{idOrName}/ir` returning pipeline-ir-v1
`PipelineIrDocument` (native load, else classic import without save). Shrinks
`designGaps` so IR read is no longer claimed unsupported. Mockito resource tests,
Spring stub method, sitemanage adaptor tests, and product-docs developer REST
section included. Does not implement start/stop (wave-1 siblings).

## Scope

- `rest` pipelines resource / adaptor interface / Spring stub / Mockito tests
- `projects/sitemanage` `PipelinesAdaptor` IR read + designGaps helper
- `product-docs/8.2/developer/rest.md` Pipelines section
- Prior patterns: path-injection / no raw name echo (pipelines catalog + execute peers);
  change-class companions (resource + interface + stub + adaptor tests + product-docs)

Cross-platform path review: no new filesystem path joins; IR loaders use existing
`IPSPipelineIrService` / catalog-resolved single-segment app names only.

## Issues

None (bug / missing behavioral tests / non-portable I/O).

### Suggestions (non-blocking)

- **suggestion** — Open start/stop PR #4308/#4313 will conflict on the same adaptor
  files (`defaultDesignGaps`, ctors, product-docs Pipelines section). Additive IR
  read should merge cleanly with care; no thrash of start/stop surfaces in this PR.

## Evidence

- `cd rest && ../mvnw.cmd clean install` → BUILD SUCCESS
- `cd projects/sitemanage && ../../mvnw.cmd clean install` → BUILD SUCCESS
- `PipelinesAdaptorTest` Tests run: 22, Failures: 0
- `PipelinesResourceTest` includes getPipelineIr* cases (module suite green)

> Co-Authored by Grok Build 1.0.5 using grok-4.5 with agent night-issue-prs.
