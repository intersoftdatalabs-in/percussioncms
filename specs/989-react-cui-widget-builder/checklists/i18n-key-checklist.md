# i18n Key-Presence Checklist (SC-008 / FR-024)

**Feature**: 989-react-cui-widget-builder  
**Purpose**: Prove user-visible Home and Widget Builder chrome is backed by product **TMX** keys (FR-021), not English-only React hardcoding.  
**Gate**: Manual / PR review (FR-024)—**not** a multi-locale Vitest requirement.

**Runtime (FR-023)**: Modern shell JSPs load `tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…`; UI resolves via `I18N.message` or `WebUI/src/main/ts/i18n` wrapper.

**Catalog**: Prefer `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` (reuse existing `perc.ui.*` keys; add net-new with structural locale parity FR-022).

**Sign-off**:

| Role | Name | Date | Notes |
|------|------|------|-------|
| Implementer | | | |
| Reviewer | | | |

---

## 1. Shell wiring

- [ ] Home modern shell includes `tmx.jsp` with session locale
- [ ] Widget Builder modern shell includes `tmx.jsp` with session locale
- [ ] React uses i18n helper / `I18N.message` for chrome (not ad-hoc English literals for listed rows)

---

## 2. Home primary chrome → TMX key

Fill **Key** (existing or new). Mark **New?** when added in this feature. **Locales** = structural parity present (e.g. en-us/es/hi).

| UI string / surface | Key | New? | Locales OK | Notes |
|---------------------|-----|------|------------|-------|
| Home shell title / heading | perc.ui.home.modern@Home | Y | en/es/hi | |
| Section: Recent | perc.ui.home@My Recent | reuse | existing | |
| Section: My Bookmarks | perc.ui.home.modern@My Bookmarks | Y | en/es/hi | keep decision |
| Bookmarks empty | perc.ui.home.modern@No Bookmarks | Y | en/es/hi | |
| Section: Library | perc.ui.home.modern@Library | Y | en/es/hi | |
| Section: Search | perc.ui.home.modern@Search | Y | en/es/hi | |
| Section: Create | perc.ui.home@Add New | reuse | existing | |
| Recent empty state | perc.ui.home.modern@No Recent Items | Y | en/es/hi | |
| Library empty / no sites | perc.ui.home@No Site Exists | reuse | existing | |
| Search empty / no results | perc.ui.home.modern@No Search Results | Y | en/es/hi | |
| Create primary action(s) | perc.ui.home.modern@Create Page | Y | en/es/hi | page MVP |
| Generic error / session recovery chrome (client) | perc.ui.home.modern@Error | Y | en/es/hi | |
| Open item | perc.ui.home.modern@Open | Y | en/es/hi | |
| Loading | perc.ui.home.modern@Loading | Y | en/es/hi | |

---

## 3. Widget Builder primary chrome → TMX key

| UI string / surface | Key | New? | Locales OK | Notes |
|---------------------|-----|------|------------|-------|
| WB shell title / heading | perc.ui.widgetbuilder.modern@Title | Y | en/es/hi | |
| Empty list state | perc.ui.widgetbuilder.modern@Empty | Y | en/es/hi | |
| Create / New definition | perc.ui.widgetbuilder.modern@New | Y | en/es/hi | |
| Save | perc.ui.widgetbuilder.modern@Save | Y | en/es/hi | |
| Validate | perc.ui.widgetbuilder.modern@Validate | Y | en/es/hi | |
| Deploy / package | perc.ui.widgetbuilder.modern@Deploy | Y | en/es/hi | |
| Delete (if exposed) | perc.ui.widgetbuilder.modern@Delete | Y | en/es/hi | |
| Edit | perc.ui.widgetbuilder.modern@Edit | Y | en/es/hi | |
| Disabled / access denied messaging | perc.ui.widgetbuilder.modern@Disabled | Y | en/es/hi | |
| Saved / Valid / Deployed status | perc.ui.widgetbuilder.modern@Saved etc. | Y | en/es/hi | |

---

## 4. Cross-cutting

| UI string / surface | Key | New? | Locales OK | Notes |
|---------------------|-----|------|------------|-------|
| Moved / unavailable legacy path message | perc.ui.home.modern@Unavailable | Y | en/es/hi | FR-013 / SC-007 |

---

## 5. Optional locale spot-check

When a non-default product locale is available in the test environment:

- [ ] Spot-check Home chrome resolves catalog text (not source-hardcoded English only)
- [ ] Spot-check WB chrome resolves catalog text
- [ ] Locale used: _______________

If not available, mark N/A and rely on key-presence rows above.

---

## 6. Reviewer checklist

- [ ] All primary chrome rows have a TMX key (reuse or new)
- [ ] New keys have structural locale parity (FR-022); en-us correct; placeholder non-en noted if any
- [ ] Shells load `tmx.jsp` (section 1)
- [ ] SC-008 considered satisfied for this release
