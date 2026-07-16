# Implementation Plan: Content Repository API Standard Upgrade (JCR 1.0 → 2.0)

**Branch**: `1286-jcr-2-0-api-migration` (spec dir `987-jcr-2-0-api-migration`) | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/987-jcr-2-0-api-migration/spec.md`  
**Related**: Issue #506; dependency bump #531 (`javax.jcr:jcr` 1.0 → 2.0 on `development`)

## Summary

The development line already pins **`javax.jcr:jcr:2.0`**. This plan restores **application compile compatibility** (Phase 1, own PR), then performs **deprecation cleanup** and verification (Phase 2+) without adopting optional new JCR 2.0 product features. Percussion’s content repository is a **JSR-170/283-typed projection** over the existing CMS store; implementors (`PSContentNode`, value/property factories, query types, `PSContentMgr`) must implement new 2.0 interface methods—typically with identity mapping or `UnsupportedRepositoryOperationException` for unsupported optional capabilities. Custom extensions require **source rebuild**. Feature-complete requires automated tests plus a short **scripted smoke** (create/save, open, preview, one publish).

## Technical Context
- **Language/Version**: Java 21 on `development` (out of scope: `development-8.1.x` / Java 8)
- **Owning Module(s)**: `system/` (contentmgr, assembly, publisher); `modules/utils` (`com.percussion.utils.jsr170`)
- **Secondary modules**: `modules/perc-toolkit`, `projects/sitemanage`, `modules/segmentation-rx`, `modules/p13n-api`, `deployer`, selected `modules/extensions-*`, `modules/ContentUI`, limited DTS touchpoints
- **AGENTS Hierarchy**: Root `./AGENTS.md`; apply module AGENTS if present under touched paths
- **Dependencies & Storage**: Parent BOM `javax.jcr:jcr:2.0`; `jackrabbit-jcr-commons` 2.22.x; existing CMS RDBMS content store (no schema migration)
- **Testing**: JUnit 5, Mockito; module tests via `./mvn-env.sh`; feature-complete scripted smoke (manual/recorded)
- **Scale/Impact**: ~200 files import `javax.jcr`; **compile risk concentrated in ~15 implementor types**; editors/publishers see no intentional UX change; ops security posture; integrators rebuild extensions
- **Delivery**: Phase 1 compile PR first (FR-014); then deprecation PRs; smoke at feature-complete only

## Constitution Check
- [x] **I. Module-First Boundaries** — Primary owners `system/`, `modules/utils`; shared helpers in utils jsr170 package
- [x] **II. Evidence Over Invention** — `research.md` cites jar diffs and concrete classes (`PSContentNode`, `PSQuery`, `PSValueFactory`, …)
- [x] **III. Test Discipline** — Unit tests for new implementor methods; module tests; smoke at feature-complete
- [x] **IV. Contract & Integration Integrity** — HTTP/package contracts unchanged; internal JCR implementor + integrator rebuild contracts under `contracts/`
- [x] **V. Safe Modernization** — Compatibility + deprecation cleanup only; no Spring Boot; no Jackrabbit storage rewrite
- [x] **VI. Security by Default** — Dependency already security-driven (#531); post-change tree check for 1.0 residual / high-severity CVEs
- [x] **VII. Build & Dependency Hygiene** — JDK 21, centralized BOM, Spotless as per module norms
- [x] **VIII. Documentation & Operability** — Release notes + integrator rebuild notes (FR-008, FR-011)
- [x] **IX. PR Review Comment Resolution** — Standard PR process; resolve threads with mitigation replies
- [x] **Complexity Budget** — No new modules; stubs for unsupported JCR optional features justified in Complexity Tracking

### Post-design Constitution Check
Re-validated after `research.md`, `data-model.md`, `contracts/`, `quickstart.md`: **pass**. No new violations.

## Project Structure
### Documentation (this feature)
```text
specs/987-jcr-2-0-api-migration/
├── spec.md
├── plan.md                 # This file
├── research.md             # Phase 0 findings
├── data-model.md           # Logical repository projection
├── quickstart.md           # Validation commands & smoke
├── contracts/
│   ├── jcr-2.0-implementor-surface.md
│   └── integrator-rebuild.md
├── checklists/requirements.md
└── tasks.md                # (via /speckit-tasks)
```

### Source Code (affected paths)
```text
# Phase 1 — implementors (compile-critical)
modules/utils/src/main/java/com/percussion/utils/jsr170/
  PSBaseValue.java, PS*Value.java, PSValueFactory.java,
  PSPropertyDefinition.java, PSMultiProperty.java, PSNodeIterator.java, …
system/src/main/java/com/percussion/system/utils/jsr170/PSProperty.java
system/services/src/com/percussion/services/contentmgr/
  data/PSContentNode.java, PSQuery.java, PSQueryResult.java
  impl/PSContentMgr.java
  impl/legacy/PSTypeConfiguration.java
  IPSContentMgr.java, IPSNode.java
system/services/src/com/percussion/services/publisher/impl/PSQueryResultUtils.java
modules/utils/src/test/java/com/percussion/utils/testing/PSMockProperty.java
modules/perc-toolkit/.../pso/restservice/model/*Value.java

# Phase 2 — call-site deprecation (selected)
system/services/.../assembly, publisher, contentmgr consumers
projects/sitemanage/... (PSJcrNodeFinder, activity, path, feeds, …)
modules/perc-toolkit, segmentation-rx, extensions-*, deployer

# Dependency
pom.xml  # already 2.0 on development; ensure feature branch synced
```

## Implementation Approach

### Phase 0 — Sync and inventory
1. Merge/rebase `origin/development` onto feature branch so BOM is jcr 2.0.
2. Compile core modules; capture error list into `tmp/jcr-compile-phase1.log` (repo temp).
3. Confirm implementor list vs `research.md` R2 (update inventory if new implementors appear).

### Phase 1 — Compile-clean (single PR)
1. **Values / factory / property** (`modules/utils` + `system` PSProperty):
   - Add `getBinary()`, `getDecimal()`, Binary factory methods.
   - Adjust exception signatures for `Value` overrides.
   - Property: `isMultiple`, `setValue(Binary|BigDecimal)`, etc.
2. **Node** (`PSContentNode`):
   - `getIdentifier()` → same identity as current `getUUID()` source.
   - New overloads (`getNodes(String[])`, references/weak refs, Binary/BigDecimal setProperty, share/lifecycle) as UROE or empty per R3.
   - Fix `Item.remove` / versioning signature deltas as required by compiler.
3. **Query stack** (`PSQuery`, `PSQueryResult`, `RowQueryResult`, `PSContentMgr`):
   - bind/limit/offset/bind names; `getSelectorNames`; `getQOMFactory` (UROE or minimal stub).
4. **NodeType / PropertyDefinition** (`PSTypeConfiguration`, `PSPropertyDefinition`).
5. **Toolkit Value models** and **PSMockProperty**.
6. Unit tests for new methods; full compile; open **Phase 1 PR only**.

### Phase 2 — Deprecation cleanup (follow-on PRs)
1. Type-aware inventory of `Node.getUUID` (exclude `IPSGuid.getUUID`).
2. Migrate to `getIdentifier()` on JCR nodes; optional shared helper if repeated.
3. Binary path improvements only where touch is natural (no drive-by rewrites).
4. **Do not** rewrite product SQL queries to JCR-SQL2.
5. Non-critical hard cases → exception register (FR-013); zero exceptions on critical editor/publish paths.
6. Keep module automated tests green per story PR.

### Phase 3 — Docs & security posture
1. Release notes: 2.0 API, no data migration, extension rebuild.
2. Link or summarize integrator contract.
3. Dependency tree check for residual jcr 1.0; note CVE scan result.

### Phase 4 — Feature-complete gate
1. Designated automated suites green.
2. Scripted smoke per `quickstart.md`; record on final PR.
3. Close #506 when criteria met (or track remaining exceptions).

## Test Plan (summary)

| Phase | Tests |
|-------|--------|
| 1 | New unit tests for Binary/identifier/Query 2.0 methods; `modules/utils` value tests; compile all shipping modules |
| 2 | Existing contentmgr/finder/publisher tests; update mocks (`Query.SQL` still valid) |
| 4 | Scripted smoke; optional dependency:tree assertion in CI docs |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Large `getUUID` false-positive rename breaks GUID APIs | Type-aware review; never bulk-replace `getUUID` |
| Stub methods hide real missing behavior | Prefer UROE; critical paths covered by smoke |
| Feature branch lacks #531 pin | Phase 0 rebase/merge development first |
| Toolkit `Value` types diverge from utils | Same Binary/decimal methods; share helper if possible |
| QOM factory expectation from third-party code | Document UROE; rebuild note for integrators |

## Complexity Tracking
- **Violation**: Implementing full JSR-283 optional features (shareable nodes, lifecycle, JQOM) would expand scope far beyond product behavior.
- **Justification & Alternatives**: Use interface-complete stubs (UROE/empty) consistent with existing unsupported versioning/lock methods on `PSContentNode`. Alternative (full Jackrabbit) rejected as storage rewrite.

## Generated Artifacts (Phase 0–1)
- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/jcr-2.0-implementor-surface.md](./contracts/jcr-2.0-implementor-surface.md)
- [contracts/integrator-rebuild.md](./contracts/integrator-rebuild.md)
- [quickstart.md](./quickstart.md)

## Next Command
`/speckit-tasks` — break Phase 0–4 into dependency-ordered implementation tasks aligned with user stories and PR phases.
