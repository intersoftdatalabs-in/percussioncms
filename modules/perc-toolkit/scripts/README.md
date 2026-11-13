# perc-toolkit scripts

Node-based helpers for this module. Pure node (no shell-outs, `path.join`
everywhere) so they run unchanged on Windows, Linux, and macOS.

## sync-installer-vendored-js.cjs

Refreshes the npm-tracked third-party JS shipped in the CMS installer
payload (`src/main/resources/InstallDir/...`, assembled verbatim
into the dist by `perc-distribution-tree`) from the pins in
`../package.json`:

- `jcrop-0.9.12@0.9.12` → `rx_resources/js/jquery.Jcrop.min.js` (byte-identical copy)
- `jquery-color@3.0.0` → `rx_resources/js/jquery.color.js` (provenance banner + npm dist)
- `jquery-form@4.5.0` → `rx_resources/addins/psoimageeditor/js/form.js`
  (provenance banner + npm dist; historic file name kept so deployed editor
  pages keep working)

### Usage

```bash
cd modules/perc-toolkit
npm install
npm run sync
```

After any pin change, re-run `npm run sync` and commit the refreshed payload
bytes together with `package.json` / `package-lock.json` — the
`InstallerPayloadVendoredJsAuditTest` guard test pins the banners and
package.json pins, so payload drift fails the module build. See issues #6273
(image-crop cluster) and #6274 (editor-legacy cluster, audit #6069).
