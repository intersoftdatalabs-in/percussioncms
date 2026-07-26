# Release notes snippet — Configurable URL allow/block lists (issue #1205)

## Security: outbound URL validation allow/block lists

Server-initiated outbound HTTP(S) requests are validated against SSRF controls. Operators configure additional policy via install-root files:

|                   File                   |                                                          Purpose                                                           |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `rxconfig/Server/allowedUrls.properties` | **Additive** allow patterns (full-URL globs). Unlock private/internal hosts or nonstandard ports that baseline would deny. |
| `rxconfig/Server/blockedUrls.properties` | Deny patterns. **Block always wins** over baseline and allow.                                                              |

### Defaults

- **Allow file**: comments and inactive examples only (no open allows until you edit).
- **Block file**: active dangerous targets (e.g. cloud metadata URL shapes).
- Upgrade **creates missing files** and **never overwrites** existing files.

### Pattern rules

- One absolute URL glob per line; `#` comments; `*` multi-character wildcard.
- Lone `*` is ignored (does not allow or block everything).
- Only `http` / `https` schemes; allow file cannot enable other schemes.
- Restart the server after editing.

### Removed

Unreleased JVM system properties (`percussion.url.validation.allowed.hosts`, `.ports`, `.ip.ranges`, `.allow.private.networks`) are not used. Use the property files instead.

### Examples (allow file — uncomment to activate)

```
# https://api.openweathermap.org/*
# http://hr.internal.example.com:8080/api/*
# https://i18n.example.com/v1/translate*
```

