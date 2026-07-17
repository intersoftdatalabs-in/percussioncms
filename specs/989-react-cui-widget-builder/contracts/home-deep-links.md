# Contract: Home deep links and nav entry

## Primary navigation

| View key | Legacy shell (delete US3) | Modern shell (new file) | Nav constant |
|----------|---------------------------|-------------------------|--------------|
| `home` | `home.jsp` | New thin JSP (name TBD at implement) | `VIEW_HOME` |
| `widgetbuilder` | `widgetBuilder.jsp` | New thin JSP (name TBD) | `VIEW_WIDGET_BUILDER` |

Dispatcher: `WebUI/.../cm/app/index.jsp` (and `cm/pages/app/index.jsp` mirror) `views` map.

**Hard cut**: Do not keep classic JSPs as redirect stubs. Known URL mapping is implemented via dispatcher query handling, server redirects, or modern app routing—not via retained classic files (FR-017 + FR-013).

## Home query parameters (known map)

Entry shape today: `?view=home&initialScreen=<value>` (and related product URLs).

| Legacy `initialScreen` | Modern section |
|------------------------|----------------|
| `library` | Library |
| `list` | Recent (list) |
| `search` | Search |
| `newitem` | Create |
| absent / unknown known-alias | Default section (Recent) or mapped alias documented at implement |
| unmapped obsolete path | Clear **on-page** “moved/unavailable” surface (dedicated shell/component or dispatcher page—not log-only; no classic UI, no silent blank) |

## Retired CUI paths

| Pattern | After cutover |
|---------|----------------|
| `/cm/pages/cui/index.html` (iframe src) | Not a production entry; map or message if bookmarked |
| `/cm/cui/**` SPA assets | Removed from distribution |
| Classic `home.jsp` direct URL | Map if still a documented entry; else unavailable message |

## Success criteria tie-in

SC-007 checklist minimum: main `home` / `widgetbuilder` views + `initialScreen` values above; plus one unmapped sample path showing clear message.

## Props into React mount (suggested)

```text
PercModernUI.mount('home-root', 'HomeShell', {
  initialSection: '<mapped-from-query>',
  locale: '<from page>',
});
```

Exact prop names are implementation detail; behavior must match the table.
