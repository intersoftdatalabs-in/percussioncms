# Erlang self-review — issue #2934 services Xlint residual

**Date:** 2026-08-11  
**Issue:** #2934 (Parent #2877 / Epic #2022)  
**Module:** `system` (`perc-system`)  
**Scope:** `com.percussion.services.contentmgr` / `publisher` / `assembly` rawtypes residual batch

## Change class

Tech-debt generics / `-Xlint` rawtypes cleanup on services package clusters. Not product-facing.

## Companions

| Companion | Status |
|-----------|--------|
| Production typing in contentmgr/publisher/assembly | Done |
| Unit tests (`PSServicesPackageTypedTest`) | Done |
| product-docs | N/A — no operator/user/API behavior change |
| Playwright | N/A |
| Downstream reverse-dep rebuild | none — no `final`/`sealed`; public API tightened with source-compatible `Class<?>` / `Map<String, ?>` / `List<Class<?>>` |

## Review checklist

- [x] Prefer real generics over blanket `@SuppressWarnings` (helpers only where Hibernate `List`/`Query.list` forces unchecked)
- [x] No non-portable path construction
- [x] Generated ANTLR lexer/parser (`SqlLexer`/`SqlParser`/`Xpath*`) left out of scope
- [x] install package residual (#2933/#2942) not touched
- [x] Behavioral unit tests for typed APIs exercised without full CMS stack where possible

## Residual (if any)

Generated query parsers and any remaining raw Hibernate result handling outside this batch may still warn; file residual only if inventory shows a PR-sized services remainder after this merge.
