# Erlang review — feat/000-react-spa-pr3-home-publish

**Date:** 2026-07-27  
**Scope:** SPA PR-3 — embed HomeShell + PublishingShell; Home default landing. Stacked on PR-2.  
**Memory patterns hit:** dual chrome; missing tests; open redirects.

## Summary

Home and Publish are real SPA routes (lazy `loadComponent`). `HomeShell` supports `embedded` to avoid double BrandBar under AppLayout. Dashboard remains a legacy exit with product note that it may fold into Home (not a peer SPA route).

## Recommendation

**approve**

## Gate

|      Check       |                     Result                      |
|------------------|-------------------------------------------------|
| Bugs             | None                                            |
| Behavioral tests | App + HomeShell embedded + existing shell tests |
| May commit/push  | **yes**                                         |

## Issues

None.
