# Specification Quality Checklist: Migrate Home/Contributor UI and Widget Builder to Modern UI

**Purpose**: Validate specification completeness and quality before proceeding to planning  
**Created**: 2026-07-17  
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Validation pass 2026-07-17; re-validated after clarify session 1 (5 answers) and clarify session 2 (US3 removal, 5 answers).
- Clarifications locked (cutover/UX): big-bang; map known deep links; single Home shell (Recent/Library/Search/Create); last-write-wins WB; Library browse/open parity.
- Clarifications locked (US3 removal): exclusive + proven orphans; hard-cut delete classic entry JSPs + rewire nav; same-release replace legacy client tests; manual removal inventory (no CI absence-scan gate); durable inventory under feature `checklists/removal-inventory.md`.
- Post-analyze remediation 2026-07-17: FR-008/009/010 release-gate wording; FR-020 main-nav smoke; FR-013 on-page unavailable surface; Out of Scope “manual inventory”; tasks T050a/b, T056a/b; plan release gate.
- Clarify i18n 2026-07-17: TMX-backed UI (FR-021); structural locale parity for new keys (FR-022); tmx.jsp + I18N.message (FR-023); i18n proof = SC-008 key checklist, not Vitest multi-locale (FR-024).
- Post-analyze remediation (i18n coverage): plan/research R10; tasks T003a, T010a, T020a, T034a, T050c, T056c + shell tmx.jsp; `checklists/i18n-key-checklist.md`.
- Home capability matrix added (`contracts/home-capability-matrix.md`); FR-001a/001b lock Create + open-path parity; classic Home was limited but complete—do not thin further.
- Ready for Create parity implementation (matrix §6 MUST) before Home acceptance.
