# Erlang Review — Issue #1603 feeds Javadoc

## Summary

Documentation-only fix for the `feeds` module. The Maven build succeeded but
Javadoc generation reported **4 errors** (1 unique HTML error + plugin-emitted
detail lines) and **200 warnings** (100 unique Javadoc source warnings +
plugin-emitted detail lines). After this PR the module generates a clean
Javadoc JAR.

## Scope

- Base: `origin/development` (commit `ea85629c2f`)
- Head: feature branch `fix/1603-javadoc-issues-feeds`
- Files: 21 changed in `deliverytiersuite/delivery-tier-suite/feeds/src/main/java/**`
- Prior report: none
- Memory patterns hit: none (pure Javadoc cleanup; no I/O, no security, no
  behavioral change)

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

None.

## Specific changes

- `services/IPSFeedDao.java` — fixed the HTML error. The Javadoc on
  `find(String name, String site)` had a stray `</code>` end tag
  (`the retrieved descriptor or </code>empty Optional</code>`); replaced
  with the correct opening `<code>`. Also added descriptions for the four
  `@param` tags on `saveConnectionInfo` and a leading description on
  `getConnectionInfo`.
- `data/FeedType.java` (both copies under `com.percussion.delivery.data` and
  `com.percussion.delivery.feeds.data`) — added Javadoc to the enum class
  and to all three enum constants (`ATOM`, `RSS1`, `RSS2`).
- `data/IPSFeedDescriptor.java` (both copies) — added leading descriptions
  to all seven getter methods.
- `data/PSFeedDescriptor.java` (both copies under `data/`) —
  `rdbms/PSFeedDescriptor.java` — added explicit default constructors with
  Javadoc, added leading descriptions on every getter and setter, replaced
  `/* (non-Javadoc) */` blocks with real Javadoc summaries, and documented
  the additional 6-arg `PSFeedDescriptor` convenience constructor in the
  older `data/` copy.
- `data/PSFeedDescriptors.java` (both copies) — added class/constructor
  Javadoc and leading descriptions on all getters and setters.
- `data/PSFeedDTO.java` (both copies) — added class/constructor Javadoc,
  explicit default constructor on the new copy, and leading descriptions on
  every getter and setter.
- `data/PSFeedItem.java` (both copies) — added explicit default
  constructor with Javadoc on the older `data/` copy and leading
  descriptions on all getters and setters.
- `PSFeedGenerator.java` — added a class-level description, an explicit
  default constructor with Javadoc, and leading descriptions on
  `makeFeedContent`, `fixupHost`, and `getHost`.
- `PSFeedsApplication.java` — added class-level and constructor Javadoc.
- `services/IPSConnectionInfo.java` — added class-level description and
  Javadoc to all five interface methods.
- `services/IPSFeedsRestService.java` — added class-level description,
  `@param`/`@return` descriptions, replaced `/* (non-Javadoc) */` blocks
  with proper Javadoc on `addMetadataListener` and `removeMetadataListener`,
  and added Javadoc to the four `PROP_*` constants and `rotateKey`.
- `services/PSFeedService.java` — added explicit default constructor with
  Javadoc and leading descriptions on `getRssFeedsIP`, `setRssFeedsIP`,
  `csrf`, and the autowired constructor.
- `services/rdbms/PSConnectionInfo.java` — added class-level description,
  explicit default constructor, and Javadoc on the constructor and all
  getters/setters; replaced `/* (non-Javadoc) */` blocks with proper
  Javadoc summaries.
- `services/rdbms/PSFeedDescriptor.java` — added Javadoc to every
  `@Basic` field (`site`, `name`, `title`, `description`, `link`, `type`,
  `query`), explicit default constructor, and Javadoc on the copy
  constructor and every getter/setter.

## Cross-platform / path review

Not applicable — diff touches Javadoc text only. No new file I/O or path
construction.

## Build evidence

- `mvnw.cmd -pl deliverytiersuite/delivery-tier-suite/feeds -am javadoc:javadoc`:
  feeds section reports 0 javadoc errors and 0 javadoc warnings.
  (Upstream modules like `perc-legacy` still report pre-existing warnings
  that are unrelated to this issue and out of scope.)
- `mvnw.cmd clean install -pl deliverytiersuite/delivery-tier-suite/feeds -am`:
  BUILD SUCCESS; war + javadoc.jar produced; tests: 110 run, 0 failures,
  0 errors, 100 skipped (pre-existing skips for
  `PSFeedServicePerformanceTest`).
- `mvnw.cmd spotless:check -pl deliverytiersuite/delivery-tier-suite/feeds`:
  clean after `spotless:apply` reformatted 4 files.

## Notes

- The feeds module contains two parallel copies of the data package
  (`com.percussion.delivery.data.*` and `com.percussion.delivery.feeds.data.*`).
  Both copies are tracked, both contain the same public types, and the
  javadoc tool scans both. Every javadoc fix was applied to both copies.
- Per AGENTS.md "Pre-PR Spotless formatting (HARD GATE)", a root-level
  spotless run was rejected due to an unrelated encoding error in
  `.kilo/worktrees/.../raw-files.js`; spotless was run module-scoped on
  `feeds` only.
- Per AGENTS.md "Pre-PR Maven verification (HARD GATE)", the build was run
  with `-pl deliverytiersuite/delivery-tier-suite/feeds -am` to include
  upstream dependencies (not a full reactor).

