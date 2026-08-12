# Erlang review — #3204 Object ACL Design/Runtime prefs + product path

## Change class

Operator-facing Developer Preferences persist/reload + Object ACL product path
(docs + Playwright). Not a site/DF ACL panel rewrite.

## Findings

| Severity | Item | Disposition |
|----------|------|-------------|
| Bug | GET-by-name empty/unwrap-null dropped saved Runtime visibility | Fixed: list fallback + object-value parse |
| Behavioral tests | loadDefaultAclTemplate list fallback; parse object payload; JAXB list unwrap | Added |
| Playwright | Prefs save → reload Visible persist | Added on product-path spec |
| Product docs | Operator path missing | `product-docs/8.2/admin/object-acl.md` |
| Paths | No new filesystem path concat | N/A |
| API shape | No public Java signature / final/sealed change | downstream_checked=none |

## Hard gates

- No missing persist/reload companion: Vitest + Playwright + product-docs
- Cross-platform: NIO/path N/A
- Copyright: new test file uses Intersoft 2026 header
