# QA fixture published site (slice 3, #6243)

Fixture edition output for the `--with-dts` QA cell
(spec `specs/2402-qa-dts-publish/`, epic #6237). `qa-publish-fixture`
(`docker/scripts/perc-devctl.py`) copies `published-site/` into the QA DTS
cell's Tomcat static dir and registers the two contexts from
`publishing-server.json`, so the stranded surface specs go green:

- `PUBLISHED_SITE_URL` → `/published-site/` (`index.html`)
- `PUBLISHED_CATEGORY_URL` → `/published-site/category/` (`category/index.html`)
- publish-time `web_resources` → `/web_resources` (origin root, the way real
  published pages address it)

## Layout

```text
published-site/                  # edition output root (docBase for /published-site)
  index.html                     # static home page (published-site-smoke.spec.js)
  category/index.html            # collapsible category list
                                 # (published-category-tree-fancytree.spec.js,
                                 # #6189 checkbox 1; .perc-category-list marker
                                 # also serves slice-4 matrix helper)
  web_resources/cm/jslib/jquery.js
  web_resources/cm/jslib/jquery-ui.js
  web_resources/cm/jslib/jquery.fancytree.js
  web_resources/cm/css/fancytree/skin/ui.fancytree.css
  web_resources/cm/css/fancytree/skin-win8/{loading,icons,icons-rtl,vline,vline-rtl}.gif
publishing-server.json           # fixture publishing-server manifest
                                 # (machine-readable target mapping)
```

Deliberately absent: `web_resources/cm/jslib/jquery.dynatree.js` and
`web_resources/cm/css/dynatree/` — the specs assert those retired copies
stay 404.

## Provenance (publish-time copies, not hand-written)

Published filenames mirror
`modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml`:

| Published path | Source (exact version) |
|---|---|
| `web_resources/cm/jslib/jquery.js` | npm `jquery@3.7.1` `dist/jquery.min.js` (published delivery runtime is the `profiles/3x` jQuery-3 tree — see `WebUI/src/main/webapp/cm/jslib/profiles/readme.txt` — and FancyTree 2.38.x is not jQuery-4 clean at load: `jquery.fancytree-all.min.js` throws `Cannot read properties of undefined (reading 'fancytree')` under jQuery 4.0.0, verified in the slice-3 surface run; newest 3.x keeps the 3x contract) |
| `web_resources/cm/jslib/jquery-ui.js` | npm `jquery-ui@1.14.1` `dist/jquery-ui.min.js` (matches `jquery-ui@^1.14.1` in `WebUI/src/main/frontend/package.json`; FancyTree asserts `$.ui` at load — `_assert($.ui, "Fancytree requires jQuery UI")` — so published pages load jquery-ui before fancytree, and the fixture does the same) |
| `web_resources/cm/jslib/jquery.fancytree.js` | npm `jquery.fancytree@2.38.3` `dist/jquery.fancytree-all.min.js` (matches the `#6052` provisioned copy) |
| `web_resources/cm/css/fancytree/skin/ui.fancytree.css` | npm `jquery.fancytree@2.38.3` `dist/skin-win8/ui.fancytree.min.css` |
| `web_resources/cm/css/fancytree/skin-win8/*.gif` | npm `jquery.fancytree@2.38.3` `dist/skin-win8/` (`loading.gif` asserted 200 by the category spec; siblings keep the skin self-consistent — the CSS references them via `../skin-win8/…`) |

Vendor license banners inside the copied files are kept intact (MIT).

Regenerate after a Dependabot bump of either package:

```bash
curl -sSL -o /tmp/ft.tgz https://registry.npmjs.org/jquery.fancytree/-/jquery.fancytree-<VERSION>.tgz
curl -sSL -o /tmp/jq.tgz https://registry.npmjs.org/jquery/-/jquery-<VERSION>.tgz
# extract, copy per the table above, update this README + commit
```

## Tomcat registration (why path-prefix contexts)

The contexts are registered as per-Host context descriptors
(`conf/Catalina/localhost/published-site.xml`,
`conf/Catalina/localhost/web_resources.xml`, `<Context docBase="…">`)
rather than a second `<Host>` alias: agent cells address the cell by
`127.0.0.1:<DTS_PORT>` with no DNS/hosts entry, so a Host-alias name
would not resolve from the Playwright runner. Path-prefix contexts keep
the DTS `ROOT` app and service webapps untouched, need no `server.xml`
edit, and deploy without a Tomcat restart (`autoDeploy="true"`).
