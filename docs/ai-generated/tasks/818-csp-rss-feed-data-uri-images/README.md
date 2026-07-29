# Bug Fix #818 – RSS Feed XML Fails to Load Correctly Due to Content Security Policy (CSP) Blocking Data URI Images

## Problem Summary

- When accessing RSS Feed URLs (served by the DTS `feeds` module), the browser blocks SVG/image resources starting with the `data:` scheme (such as `data:image/svg+xml,...`).
- The browser console displays a Content Security Policy (CSP) violation indicating that loading the image was refused because the current CSP configuration does not list `data:` in the `img-src` directive.

## Root Cause

- In the default Content Security Policy (CSP) directives defined for the Delivery Tier (DTS) services, the `img-src` directive is set to:
  `img-src * 'self' 'unsafe-inline' 'unsafe-eval';`
- The `*` wildcard matches only network schemes (e.g. `http`, `https`, `ws`, `wss`). It does not match the `data:` or `blob:` URI schemes.
- Because `data:` is not explicitly allowed in `img-src` in the delivery tier configuration (unlike on the CMS server side in `server.properties` which does permit `data:`), any inline SVG/data-URI images are blocked by the browser.

## Solution

1. **Updated Distribution Config**: Updated [perc-security.properties](file:///home/nate/projects/java8/percussioncms/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/conf/perc/perc-security.properties) and the installer file [installDts.xml](file:///home/nate/projects/java8/percussioncms/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/rxconfig/Installer/installDts.xml) to add `data: blob:` to the default `img-src` directive.
2. **Updated Individual Module Properties & Beans**:
   - Found all other default fallback configuration files (properties and Spring XML beans files) for comments, feeds, forms, integrations, membership, metadata, and polls delivery services.
   - For configs containing `default-src 'self' *;` as a default, appended `img-src * 'self' data: blob:;` to explicitly allow data/blob images without exposing other resources (like scripts) to the data URI scheme.
   - For configs containing `default-src 'self';` as a default, appended `img-src 'self' data: blob:;`.

## Validation

- Ran spotless check (`./mvnw spotless:check`) and spotless formatting (`./mvnw spotless:apply`) successfully.
- Re-built the `feeds` module and its dependencies via `./mvnw clean install -pl deliverytiersuite/delivery-tier-suite/feeds -am -DskipTests` successfully.

