# Erlang review — #2466 design.objectstore security/UI this-escape

## Scope
Real this-escape fixes for security/directory/UI-adjacent design.objectstore types after #2404.

## Change class
Leaf final setters/fromXml + private apply*/fromXmlBase for bases with subclasses (PSSubject, PSSecurityProviderInstance). Shared: final getComponentState on PSDatabaseComponent.

## Findings
- No bugs found in final/private-helper pattern (matches #2404).
- Behavioral tests extended: 19 tests green.
- Residual Element-ctor warnings remain where fromXml publishes `this` via updateParentList or delegates to overridable super.fromXml — same residual class as #2404 peers; not suppress-only.
- No path I/O / cross-platform issues.
- Did not thrash #2465 editors/app-config types.

## Verdict
Pass for PR.

> Co-Authored by Grok Build using grok-4.5 with agent main.
