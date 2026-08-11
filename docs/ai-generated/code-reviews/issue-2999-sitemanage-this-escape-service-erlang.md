# Erlang-style review — issue #2999 sitemanage this-escape service/listener

## Scope
PR-sized residual after DTO this-escape batch (#2980 / PR #3000). Cluster: service / listener /
register-on-construct constructors in `projects/sitemanage` main sources (not remaining DTO /
parser / serial-field residuals).

## Mitigations
| Pattern | Approach |
|---------|----------|
| Path item services `setRootName` in ctor | Seed protected `rootName` field; keep public `setRootName` final for post-construct use |
| Monitors / IndexHelper / servlets / wrapper / proxy / package resolver / merged region tree | Mark leaf types `final` (no monorepo subclasses) |
| Spring beans that publish `this` to registries (`@Transactional` or CGLIB-sensitive) | Justified `@SuppressWarnings("this-escape")` on ctor with comment |
| `PSResourceAssemblyResultExpander` registry put | Justified suppress (intentional publish-to-registry) |
| Proxy template ctor | Direct field seeds + final setters + private `copyBindings` |

## Findings
- **Bugs:** none found. Registration order and Spring wiring types preserved.
- **Behavior:** no REST / adaptor contract changes; no product surface change.
- **Tests:** `PSThisEscapeServiceListenerTest` — path root seed, proxy seeds, servlet wrapper, finality.
- **Cross-platform:** N/A (no path/file I/O changes).
- **API shape (C2):** several types made `final`; grepped monorepo for `extends <Type>` — only
  known test subclasses of non-final `PSSitePathItemService` remain. No anonymous subclasses of
  newly final types.

## Residual (out of this PR)
- this-escape DTO/parser leftovers: `PSUnusedAssetSummary`, widget-builder data, `PSRegionParserAdapter`
- serial-field residual + misc Xlint (static qualification, etc.)

## Verdict
PASS for commit/PR.

> Co-Authored by Grok Build using grok-4.5 with agent main.
