# Erlang review — #3989 icalendar Developer Sites chrome

Independent pre-commit review of feat/issue-3989-icalendar-sites-chrome (stacked on REST #3988 / cluster #3994).

## Change class

Developer Sites product chrome: enable Build Virtual Site / Preview assembled site / Publish Virtual Site for `sourceKind=icalendar` after save (same last-build contract as rss-atom). No REST persist/build/preview/publish reimplementation.

## Verdict

**Pass.** No bug, missing behavioral tests, or non-portable path/file I/O found.

## Findings

None.

## Checks

- Build/Preview/Publish chrome is gated on `shouldShowVirtual*Chrome("icalendar")`; repository / blank / unknown remain hidden.
- PUT envelope still sends empty `remoteUrl`/`branch` and never CalDAV credentials.
- Host fixture uses `path.join`; in-container roots and publish dests are POSIX `/opt/...` with `..` rejection before `docker exec`.
- Vitest covers helpers + panel; Playwright C5 live Build/Preview/Publish on H2 QA; fixture unit tests on `npm run test:unit`.
- product-docs 8.2 admin Sites / publishing / developer Virtual Sites / REST / site-config drop later-phase chrome wording.

## Tests / evidence

- `cd WebUI && ../mvnw.cmd clean install` BUILD SUCCESS; Vitest 3221 passed
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` BUILD SUCCESS
- Playwright `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js --grep icalendar` 6 passed; console-clean; server.log-clean
