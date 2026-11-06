<!--
 - Copyright (c) 2026 Intersoft Data Labs, Inc.
 -
 - Licensed under the Apache License, Version 2.0 (the "License");
 - you may not use this file except in compliance with the License.
 - You may obtain a copy of the License at
 -
 -     http://www.apache.org/licenses/LICENSE-2.0
 -
 - Unless required by applicable law or agreed to in writing, software
 - distributed under the License is distributed on an "AS IS" BASIS,
 - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 -
 - See the License for the specific language governing permissions and
 - limitations under the License.
 -->

# Audit: vendored TS/JS libraries (issue #5627)

Scope: every third-party `.js`/`.ts` asset in the tree that is **not** resolved
from `package.json`/`node_modules` at build time. Excluded: `target/` build
output, `WebUI/war/**` (dual-ship mirror of `WebUI/src/**`), `node_modules`,
first-party CM1 code (`cm/app`, `cm/plugins`, `cm/widgets`, `cm/services`,
`cm/views`, `cm/controllers`, `cm/models`), and `*-apidocs/script-dir`
javadoc assets. All versions below were read from file headers on `origin/main`.

Key context: the new React SPA (`WebUI/src/main/frontend/package.json`) already
consumes npm versions of many of these libraries (jquery ^4.0.0, bootstrap
^5.3.8, datatables.net, handlebars ^4.7.10, moment ^2.31.0, …). The vendored
copies below serve the **classic CM1 UI** (`cm/jslib`, loaded via
`cm/app/includes/common_js.jsp` / `finder_js.jsp`), the **DTS p13n service**
(`p13n-ds/lib-js`, served to site visitors), and the **TinyMCE bundle**
(`modules/perc-tinymce`).

## Cluster A — jQuery core / migrate / UI (classic UI)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery-3.6.0.js` | 3.6.0 | MIT | classic CM1 (common_js.jsp) | `jquery` (SPA: ^4.0.0) |
| `.../3x/jquery/jquery-migrate-3.3.2.js` | 3.3.2 (2020) | MIT | classic CM1 | `jquery-migrate` (SPA: ^4.0.2) |
| `.../3x/jquery/libraries/jquery-ui/jquery-ui.js` (+css) | 1.13.2 (2022) | MIT | classic CM1 | `jquery-ui` (SPA: ^1.14.1) |
| `WebUI/src/main/webapp/cm/api/lib/jquery-3.6.0.min.js` | 3.6.0 | MIT | CM1 content API pages | `jquery` |
| `WebUI/src/main/webapp/cm/jslib/jquery-ui.js` (root copy) | unknown (header has unbuilt `@VERSION` placeholder) | MIT (assumed) | unknown — resolve or delete | — |

Risk: medium. jQuery 3.6.0 is behind 3.7.x but has no direct CVE; jQuery-UI
1.13.2 is behind 1.14.x. Effort: drop-in npm swap. The root `cm/jslib/jquery-ui.js`
with the `@VERSION` placeholder needs a provenance decision (identify or delete).
Follow-on: **#5660** (filed, linked from #5627).

## Cluster B — Bootstrap 4.6.2 (EOL January 2023)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `.../3x/libraries/bootstrap/js/bootstrap.min.js` + `bootstrap.bundle.min.js` + css | 4.6.2 | MIT | classic CM1 | `bootstrap` (SPA: ^5.3.8) |

Risk: medium-high. Bootstrap 4 is end-of-life (no security fixes since Jan 2023).
Effort: API migration (v4 → v5 has breaking changes). Follow-on: **#5661**.

## Cluster C — Tree widgets: FancyTree 2.38.3 + DynaTree 1.1.0 (EOL)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `.../3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all*.js` | 2.38.3 | MIT | classic CM1 finder/tree | `jquery.fancytree` (SPA: ^2.38.3) |
| `.../3x/jquery/plugins/jquery-dynatree/jquery.dynatree.js` | 1.1.0 (~2014, EOL predecessor of FancyTree) | MIT/GPL dual (MIT option OK) | legacy classic UI | n/a (dead upstream) |

Risk: medium. DynaTree upstream is dead; FancyTree copy matches the npm version
the SPA already uses. Effort: retire DynaTree in favor of FancyTree; npm-swap
FancyTree. Follow-on: **#5662**.

## Cluster D — jQuery plugin cluster (classic UI)

| Path | Version | License | npm |
|------|---------|---------|-----|
| `.../plugins/jquery-datatables/js/jquery.dataTables.js` (+bootstrap/bulma/foundation/semanticui/jqueryui integrations) | DataTables 1.12.1 | MIT | `datatables.net` (SPA: ^3.1.5) |
| `.../plugins/jquery-form/*` | (header check in follow-on) | MIT/GPL dual | `jquery-form` (SPA: ^4.3.0) |
| `.../plugins/jquery-validation/*` | (header check in follow-on) | MIT | `jquery-validation` (SPA: ^1.21.0) |
| `.../plugins/jquery-jeditable`, `-superfish`, `-timeago`, `-layout`, `-print-this`, `-dropdown`, `-hoverintent`, `-collapser`, `jquery-ui-multiselect-widget`, `-testrunner` | various legacy | MIT/GPL-dual (verify each) | mixed availability — verify per plugin |
| `cm/api/lib/jquery.ba-bbq.min.js` | 1.2.1 (2010) | MIT/GPL dual | dead upstream — remove or replace |
| `cm/api/lib/jquery.slideto.min.js`, `jquery.wiggle.min.js` | ancient | verify | dead upstream — remove or replace |

Risk: medium. Several upstreams are dead (ba-bbq, slideto, wiggle). Effort: mixed
— drop-in swap where npm exists, removal/replacement where it does not.
Follow-on: **#5663**.

## Cluster E — Backbone 1.4.0 + Underscore 1.13.1 + Backgrid (EOL)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `.../3x/libraries/backbone/backbone.js` | 1.4.0 (2019) | MIT | classic CM1 | `backbone` (SPA: ^1.6.0) |
| `.../3x/libraries/underscore/underscore.js` | 1.13.1 | MIT | classic CM1 | `underscore` (SPA: ^1.13.7) |
| `cm/jslib/backgridjs/backgrid.js` (+extensions) | ~0.3.x era (header ©2013; upstream EOL ~2017) | MIT | classic CM1 grids | dead upstream — replace |

Risk: medium. Backgrid upstream is dead. Effort: npm swap for Backbone/Underscore;
Backgrid needs a replacement decision. Follow-on: **#5664**.

## Cluster F — Handlebars 4.0.12 (known CVEs) + 4.7.8

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `.../3x/libraries/handlebars/handlebars-v4.0.12.js` | 4.0.12 | MIT | classic CM1 | `handlebars` (SPA: ^4.7.10) |
| `.../3x/libraries/handlebars/handlebars-v4.7.8.js` | 4.7.8 | MIT | classic CM1 | `handlebars` |

Risk: **high (security)**. Handlebars < 4.7.7 is affected by known template-escape
CVEs (e.g. CVE-2021-20922 and related), fixed in ≥ 4.7.7. The 4.0.12 copy must go;
consolidate on one npm-tracked ≥ 4.7.7. Effort: drop-in swap. Follow-on:
**#5666** (highest classic-UI priority).
**issue #F** (highest classic-UI priority).

## Cluster G — Moment 2.29.4 (upstream maintenance mode / EOL)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `.../3x/libraries/momentjs/moment-with-locales.js` (+plugins) | 2.29.4 | MIT | classic CM1; DTS widgets via `perc-common-ui-bundle` | `moment` (SPA: ^2.31.0, last release) |

Risk: low-medium. Moment is in upstream maintenance mode (no new features; Moment
team recommends Luxon/date-fns/dayjs for new code). 2.29.4 includes the ReDoS fix
(CVE-2022-31129). Effort: npm swap now; `date-fns` (already an SPA dep) for new
code. Follow-on: **#5665**.

## Cluster H — DTS p13n-ds `lib-js` (served to site visitors)

| Path | Version | License | Consumers | npm |
|------|---------|---------|-----------|-----|
| `deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery.min.js` | **1.3.2 (2009)** | MIT/GPL dual | p13n tracking/profile delivery pages | `jquery` |
| `.../jquery-treeview/*` | Treeview 1.4 | MIT/GPL dual | p13n admin/demo | dead upstream |
| `.../jquery-dialog/jquery.simplemodal.js` | SimpleModal 1.1.1 | MIT/GPL dual | p13n dialogs | dead upstream |
| `.../jquery-form/jquery.form.js` | legacy | MIT/GPL dual | p13n forms | `jquery-form` |
| `.../jquery-splitter/splitter.js` | legacy | verify | p13n UI | verify |
| `.../json2.js` | 2008-11-19 | Public Domain | legacy JSON shim | remove (native JSON) |
| `.../log4javascript/log4javascript.js` | 1.3.1 | Apache-2.0 | p13n client logging | dead upstream (EOL ~2015) — replace or remove |

Risk: **high (security + external exposure)**. jQuery 1.3.2 predates every modern
XSS hardening pass (affected by the pre-3.5.0 selector/HTML CVEs, e.g.
CVE-2020-11022/11023 class) and is served to public site visitors by the p13n
service. Effort: mixed — jQuery/form/json2 are drop-in; treeview/simplemodal/
log4javascript need replacement decisions. Follow-on: **#5668** (highest
overall priority — externally facing).

## Cluster I — Loader / misc infra + icon/font/CSS libs

| Path | Version | License | npm |
|------|---------|---------|-----|
| `.../3x/libraries/requirejs/require.js` (+`init.js` loader config, first-party) | RequireJS 2.3.6 | MIT | `requirejs` (or retire with classic UI) |
| `.../3x/libraries/qunit/qunit.js` (+css) | QUnit 2.6.2 (2018) | MIT | `qunit` devDep — test-only, low risk |
| `cm/jslib/modernizer/modernizr-custom.js` | custom build (version TBD in follow-on) | MIT | drop (baseline browsers no longer need it) or `modernizr` |
| `.../3x/libraries/bowser/es5.js` | (header TBD in follow-on) | MIT | `bowser` (SPA: ^2.15.0) |
| `.../3x/libraries/mousetrap/mousetrap.js` (+plugins) | 1.6.2 | Apache-2.0 | `mousetrap` (SPA: ^1.6.5) |
| `.../3x/libraries/popper/popper.js` | @popperjs/core 2.11.8 | MIT | dupe of SPA `@popperjs/core ^2.11.8` — delete vendored copy |
| `.../3x/libraries/animate.css/animate.css` | 3.7.1 | MIT | `animate.css` (SPA: ^4.1.1) |
| `.../3x/libraries/fontawesome/{css/all.css, js/fontawesome-all.js, js/v4-shims.js}` | CSS 5.6.1 / JS 5.3.1 (**mismatched**) | CC-BY-4.0 (Free) — **flag to legal**; CC-BY is attribution, not code copyleft, but confirm distribution terms | `@fortawesome/*` v5+ or v7 |
| `WEB-INF/Owasp.CsrfGuard.js` | ©2011 (CSRFGuard JS, version TBD) | BSD | verify against current OWASP CSRFGuard |
| `.../3x/libraries/perc-retiredjs/*` (date.js, jquery.xmldom-1.0.js, json2.js, rAF.js, simpledateformat.js) | first-party shims | n/a (first-party) | document-and-keep; drop json2.js (native JSON) |

Risk: low-medium, except the **FontAwesome version mismatch (5.6.1 CSS vs 5.3.1
JS)** and the **CC-BY-4.0 font license**, which needs an explicit legal nod.
Effort: mostly drop-in or delete. Follow-on: **#5667**.

## Record-only (no new issue)

- **TinyMCE 6.8.6** bundle + emoticons/theme assets + first-party plugins
  (`percadvimage`, `percadvlink`, `percglobalvariables`, `percmorelink`,
  `rxinline`, `rxinserthtml`) in `modules/perc-tinymce` — tracked by replacement
  epic **#5622**. pom.xml already documents the MIT-only ≤ 6.8.x constraint
  (7+ is GPL-2.0 — incompatible).
- **CodeMirror via Maven WebJar + Ant unpack** — migration to npm (`codemirror`
  5.65.21 in `modules/perc-tinymce/package.json`, esbuild bundle) is **#5626**
  (in progress).
- `modules/perc-common-ui-bundle` (jquery ^4.0.0, moment ^2.31.0 via esbuild) —
  already npm-based. No action.
- `WebUI/war/**` — dual-ship mirror of `WebUI/src/**`; follows automatically.
- `cm/app`, `cm/plugins`, `cm/widgets`, `cm/services`, `cm/views`,
  `cm/controllers`, `cm/models` — first-party CM1 product code, not vendored
  third-party. Out of scope.

## Conclusion

Nine coherent migration clusters (#5660–#5668, modulo numbering) cover every
vendored third-party JS/TS asset found; two areas are record-only (#5622
TinyMCE replacement, #5626 CodeMirror npm migration). Prioritize #5668
(externally facing jQuery 1.3.2) and #5666 (Handlebars 4.0.12 CVEs) first,
then the EOL upstreams (Bootstrap 4, DynaTree, Backgrid, Moment), then the
drop-in npm swaps. All clusters are MIT/BSD/Apache-2.0/PD (MIT option on
duals) except FontAwesome Free (CC-BY-4.0 — legal confirmation in #5667).

## License compatibility notes (Apache-2.0 distribution)

All third-party clusters are MIT, BSD, Apache-2.0, Public Domain, or dual
MIT/GPL (MIT option selectable) — compatible. Two flags: (1) FontAwesome Free
(CC-BY-4.0 fonts) — needs legal confirmation for redistribution; (2) never
upgrade TinyMCE past 6.8.x without legal review (GPL-2.0). No GPL-only code found.
