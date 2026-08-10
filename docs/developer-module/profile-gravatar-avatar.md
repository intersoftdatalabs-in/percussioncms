# Profile Gravatar and header avatar

**Parent:** GitHub #2374 slice 5 / #2397

## What ships

- Profile **Avatar** section: optional Gravatar email (or primary account email), live preview, privacy and SSO notes.
- Header **UserMenu** chip: Gravatar image when allowed and available, otherwise accessible initials.
- Preference key `perc_profile_gravatar_email` via public PreferenceResource (empty = use primary email from `GET /user/user/current`).
- No custom uploaded avatars (out of scope).

## Operator kill-switch

|          Property          | Default |                                                                                                           Effect                                                                                                            |
|----------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `allowExternalAvatarFetch` | `true`  | When `false`, SPA bootstrap sets `allowExternalAvatarFetch: false`. UI never builds or loads Gravatar URLs; initials fallback is used. Saved Gravatar email preference is still stored for when the property is re-enabled. |

Set in `rxconfig/Server/server.properties` (or product equivalent), then **restart** the CMS.

```properties
# Enterprise privacy: do not load avatars from gravatar.com
allowExternalAvatarFetch=false
```

## SSO / directory caveat

Directory and SSO users may have primary email managed outside CMS. They can still set a separate Gravatar email in the Avatar section. Editing the **primary** account email is a different slice (account editor); this feature only reads primary email for Gravatar fallback.

## Privacy note (product)

The browser requests `https://www.gravatar.com/avatar/{sha256(email)}` with `d=404` so missing images fall back to initials without inventing a third-party face. The email itself is not rendered in the header chip.

## Surface tests

```bash
# Vitest (WebUI module clean install or frontend)
cd WebUI && ../mvnw clean install

# Playwright surface (QA mode)
npm run test:surface -- --path tests/profile-avatar.spec.js
```

