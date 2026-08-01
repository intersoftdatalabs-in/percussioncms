# Vendored `@mkd/language`

Third-party browser client for crowd-sourced translation correction triggers.
Built artifacts only — **do not edit** `dist/`.

| Field | Value |
|-------|--------|
| Package | `@mkd/language` |
| Vendored version | `0.1.0` |
| Upstream (human note) | Separate mkd-language workspace / repo — not part of this monorepo |
| Percussion usage | `WebUI/src/main/ts/i18n/mkdLanguage.ts` |

## Refresh

From a checkout of the mkd-language **client** package:

```bash
# in mkd-language/client
npm test
npm run build
```

Then copy into this tree (portable PowerShell example):

```powershell
Copy-Item <mkd-language>/client/dist/index.js   WebUI/vendor/mkd-language/dist/index.js
Copy-Item <mkd-language>/client/dist/index.d.ts WebUI/vendor/mkd-language/dist/index.d.ts
```

Align `version` in this `package.json` with upstream, then:

```bash
cd WebUI/src/main/frontend
# package.json uses file:../../../vendor/mkd-language
npm install
```

Do **not** point `package.json` at absolute paths outside this monorepo.
