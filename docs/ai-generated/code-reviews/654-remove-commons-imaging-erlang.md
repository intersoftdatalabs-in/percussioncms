# Erlang review: #654 remove commons-imaging

**Branch:** `chore/654-remove-commons-imaging`  
**Scope:** Uncommitted changes vs `origin/development`  
**Date:** 2026-07-23  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no new filesystem path construction; image bytes only)

## Summary

Ports the development-8.1.x removal of `org.apache.commons:commons-imaging` onto `development`.
`ImageReader` now uses only `javax.imageio.ImageIO` with TwelveMonkeys plugins already managed in
the parent POM. Local `org.apache.commons.imaging` package shims are deleted. `system/pom.xml`
declares TwelveMonkeys imageio artifacts **without versions** (parent `dependencyManagement`).

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Notes

- Dead public helpers (`hasAdobeMarker`, `isYcck`, CMYK convert, commons `getImageInfo`) removed;
  zero external callers verified.
- Tests exercise `ImageReader.read` for PNG/JPEG/TIFF/CMYK/WebP/GIF; WebP resize skipped (no writer).
- Parent depMgmt entry for commons-imaging removed; only `system` consumed it.

## Memory patterns hit

- Prefer standard JDK / already-managed libs over alpha third-party deps when equivalent capability exists.

