# Vendored `@mkd/language`

Third-party browser client for crowd-sourced translation correction triggers.
Built artifacts only — **do not edit** `dist/`.

| Field | Value |
|-------|--------|
| Package | `@mkd/language` |
| Vendored version | `0.2.0` |
| Upstream | Separate mkd-language workspace — not part of this monorepo |
| Percussion usage | `WebUI/src/main/ts/i18n/mkdLanguage.ts` + tracked `message()` |

## Preferred host pattern (0.2+)

`message()` in `src/main/ts/i18n/message.ts` is wrapped with `createTrackedMessage`.
`ensureMkdLanguage` passes `getTrackedMessageId` — **no per-element attrs required**
for catalog keys on chrome that already calls `message(key)`.

Optional explicit attrs: `i18nKeyAttr` / library `messageIdProps`.

## Refresh

From a checkout of the mkd-language **client** package:

```bash
cd client
npm test
npm run build
```

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
