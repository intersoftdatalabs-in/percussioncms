# Product widget XML inventory

**Generated:** 2026-08-09  
**Source:** `modules/perc-packages/src/main/resources/Packages/**/rxconfig/Widgets/*.xml`  
**Machine-readable:** [widget-xml-inventory.csv](./widget-xml-inventory.csv)

## Compiler status (#2751 / #2772 / #2789 / #2802 / parent #2630)

| Area | Status | Notes |
|------|--------|-------|
| **Widget XML → Component Package Manifest compiler** | Landed for **baseWidgets + high-traffic + residual #2789 + remaining #2802** | `com.percussion.packages.widgetxml.PSWidgetXmlCompiler` (+ package scanner) in `modules/perc-packages` |
| Golden parity (baseWidgets) | **percSimpleText** (+ package compile of all 3 baseWidgets) | Fixtures under `modules/perc-packages/src/test/resources/widgetxml/` |
| Golden parity (high-traffic #2772) | **percTitle**, **simplePageAutoList**, **percNavBreadcrumb** | Plus package compile of title, lists×2, nav×2, file, image (7 widgets) |
| Golden parity (residual #2789) | **percForm**, **percPoll**, **percIframe** | Plus package compile of blog/calendar×2/directory×4/social/form/poll/login/rss/iframe (**13** widgets) |
| Golden parity (remaining #2802) | **percImageAutoList**, **percComments**, **percEvent** | Plus package compile of auto-lists, blog companions, social/comments/cards, event/slider/cookie/jquery, login variants, Result/Redirect, defaultLanguage (**24** widgets / 23 packages) |
| Compiler extensions (#2772) | `<Resource href/type/placement>`, chrome slots without CT, layout UserPref → slot.layout | CSS/JS resources + nav chrome (no asset CT); residual batches needed no new shapes |
| **Product packages still ship Widget XML** | Yes (dual-run) | Compiler produces modern artifacts; does **not** yet remove product `rxconfig/Widgets/*.xml` from source trees |
| Residual product packages (after #2802) | **None** (product inventory complete except `perc.Test`) | Dual-run exit / product XML deletion remains Phase 5 (#2632 / parent #2630) |
| Runtime legacy XML shim | Landed (cluster #2766) | #2752 |

### High-traffic batch covered by #2772

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.widget.title` | percTitle | UserPref enum (wrapper) + content CT |
| `perc.widgets.lists` | simplePageAutoList, simpleTextAutoList | `<Resource>` CSS + layout/maxlength → slot.layout |
| `perc.widgets.nav` | percNavBar, percNavBreadcrumb | Chrome (no CT); chrome slot + CSS resource on breadcrumb |
| `perc.FileAssetWidget` | percFile | Content + rich media |
| `perc.widgets.image` | percImage | Content + rich media + CSS resource |

Measurable reduction (#2772): **7** product widget definition XMLs with validated modern compile path + goldens (beyond 3 baseWidgets).

### Residual long-tail batch covered by #2789

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.widgets.blog` | percBlogPost | Blog content CT + large UserPref set |
| `perc.widget.calendar` | percCalendar, percCalendarTwo | Shared CT `percCalendarAsset` |
| `perc.widget.directory` | percDepartment, percDirectory, percOrganization, percPerson | Multi-widget directory package |
| `perc.widget.socialButtons` | percSocialButtons | Social CT + placement UserPrefs |
| `perc.widget.form` | percForm | Integration form; golden |
| `perc.widget.poll` | percPoll | Social/blog poll; golden |
| `perc.widget.login` | percLogin | Membership login |
| `perc.widget.rss` | percRss | Blog/social RSS |
| `perc.widget.iframe` | percIframe | Chrome (no CT); golden |

Measurable reduction (#2789): **+13** product widget definition XMLs on the validated modern compile path (cumulative product widgets with package compile coverage: **3 base + 7 high-traffic + 13 residual = 23**, excluding `perc.Test`).

### Remaining product residual batch covered by #2802

| Package | Widgets | Notes |
|---------|---------|-------|
| `perc.ImageAutoListWidget` | percImageAutoList | Auto-list; golden (many UserPref + CssPref) |
| `perc.PageAutoListWidget` | percPageAutoList | Auto-list (page search) |
| `perc.widget.fileAutoList` | percFileAutoList | Auto-list (file) |
| `perc.widget.blogIndexPage` | percBlogIndexPage | Blog list companion |
| `perc.widget.archiveList` | percArchiveList | Blog archives |
| `perc.widget.categoryList` | percCategoryList | Blog categories |
| `perc.widget.taglist` | percTagList | Blog tags |
| `perc.widget.MostReadBlogPosts` | percMostReadBlogPosts | Chrome (no CT); search |
| `perc.widgets.comments` | percComments | Chrome (no CT); golden |
| `perc.widgets.liked` | percLiked | Chrome (no CT) |
| `perc.widget.commentForm` | percCommentsForm | Blog/social form CT |
| `perc.openGraphWidget` | percOpenGraph | Social meta cards |
| `perc.twitterSummaryCards` | percTwitterSummaryCards | Social meta cards |
| `perc.eventWidget` | percEvent | Content CT; golden |
| `perc.widget.imageSlider` | percImageSlider | Rich media slider |
| `perc.widget.cookieConsent` | percCookieConsent | Consent banner |
| `perc.widget.jquery` | percJQueryWidget | Chrome (no CT) |
| `perc.widget.jqueryUI` | percJQueryUIWidget | Chrome (no CT) |
| `perc.widget.registration` | percRegistration | Deprecated login variant |
| `perc.widget.secureLogin` | percSecureLogin | Deprecated login variant |
| `perc.widget.Result` | percResult | Chrome (no CT); search |
| `perc.widgets.Redirect` | percRedirect | Redirect CT |
| `perc.defaultLanguage` | percDefaultLang, percLocalLang | Multi-widget language package |

Measurable reduction (#2802): **+24** product widget definition XMLs on the validated modern compile path (cumulative product widgets with package compile coverage: **3 + 7 + 13 + 24 = 47**, excluding `perc.Test`; full product inventory of 48 − test = 47).

Ship format: [component-package-manifest.md](./component-package-manifest.md). ADR: [004-no-definition-xml-packaging.md](./adr/004-no-definition-xml-packaging.md).

## Summary

| Metric | Value |
|--------|-------|
| Widget definition files | **48** |
| Code language | **jexl** (100%) |
| Content markup | **velocity** (100%) |
| With `contenttype_name` | 39 |
| Without content type (logic/chrome widgets) | 9 |
| With at least one `CssPref` | 34 |
| With at least one `UserPref` | 41 |

**Without content type:** `percIframe`, `percJQueryWidget`, `percJQueryUIWidget`, `percMostReadBlogPosts`, `percResult`, `percComments`, `percLiked`, `percNavBar`, `percNavBreadcrumb`.

**Implication for migration:** every product widget is already “JEXL bindings + Velocity snippet (+ optional asset CT)”. Packaging change is structural, not a language rewrite.

## Full matrix
| Package | File | Title | Content type | Category | Code | Markup | CssPrefs | UserPref count |
|---------|------|-------|--------------|----------|------|--------|----------|----------------|
| perc.baseWidgets | percRawHtml.xml | HTML | percRawHtmlAsset | integration | jexl | velocity | — | 0 |
| perc.baseWidgets | percRichText.xml | Rich Text | percRichTextAsset | content | jexl | velocity | rootclass | 0 |
| perc.baseWidgets | percSimpleText.xml | Simple Text | percSimpleTextAsset | content | jexl | velocity | rootclass | 0 |
| perc.defaultLanguage | percDefaultLang.xml | Default Language | percDefaultLanguage | — | jexl | velocity | — | 0 |
| perc.defaultLanguage | percLocalLang.xml | Local Language | percLocalLanguage | — | jexl | velocity | — | 0 |
| perc.eventWidget | percEvent.xml | Event | percEventAsset | content | jexl | velocity | rootclass | 9 |
| perc.FileAssetWidget | percFile.xml | File | percFileAsset | content,rich media | jexl | velocity | rootclass | 2 |
| perc.ImageAutoListWidget | percImageAutoList.xml | Image Auto List | percImageAutoList | rich media,search | jexl | velocity | rootclass;listclass;thumblinkclass;captionclass | 16 |
| perc.openGraphWidget | percOpenGraph.xml | Facebook Open Graph | percOpenGraph | social | jexl | velocity | rootclass | 7 |
| perc.PageAutoListWidget | percPageAutoList.xml | Page Auto List | percPageAutoList | content,search | jexl | velocity | rootclass;summaryclass | 22 |
| perc.Test | PSWidget_TestProperties.xml | Test Properties | PSWidget_TestProperties | — | jexl | velocity | — | 4 |
| perc.twitterSummaryCards | percTwitterSummaryCards.xml | Twitter Summary Cards | percTwitterCards | social | jexl | velocity | rootclass | 4 |
| perc.widget.archiveList | percArchiveList.xml | Archives | percArchiveList | blog,search | jexl | velocity | rootclass;summaryclass | 3 |
| perc.widget.blogIndexPage | percBlogIndexPage.xml | Blog List | percBlogIndexAsset | blog | jexl | velocity | rootclass | 31 |
| perc.widget.calendar | percCalendar.xml | Calendar | percCalendarAsset | Other | jexl | velocity | rootclass | 5 |
| perc.widget.calendar | percCalendarTwo.xml | Calendar 2.0 | percCalendarAsset | Other | jexl | velocity | rootclass | 18 |
| perc.widget.categoryList | percCategoryList.xml | Categories | percCategoryList | blog,search | jexl | velocity | rootclass | 2 |
| perc.widget.commentForm | percCommentsForm.xml | Comments Form | percCommentsFormAsset | blog,social | jexl | velocity | rootclass | 1 |
| perc.widget.cookieConsent | percCookieConsent.xml | Cookie Consent | percCookieConsent | — | jexl | velocity | — | 6 |
| perc.widget.directory | percDepartment.xml | Department | percDepartment | — | jexl | velocity | — | 8 |
| perc.widget.directory | percDirectory.xml | Directory | percDirectory | — | jexl | velocity | — | 12 |
| perc.widget.directory | percOrganization.xml | Organization | percOrganization | — | jexl | velocity | — | 8 |
| perc.widget.directory | percPerson.xml | Person | percPerson | — | jexl | velocity | — | 9 |
| perc.widget.fileAutoList | percFileAutoList.xml | File Auto List | percFileAutoList | rich media,search | jexl | velocity | rootclass;listclass | 15 |
| perc.widget.form | percForm.xml | Form | percFormAsset | integration | jexl | velocity | rootclass | 1 |
| perc.widget.iframe | percIframe.xml | Iframe | — | integration | jexl | velocity | rootclass | 11 |
| perc.widget.imageSlider | percImageSlider.xml | Image Slider | percImageSlider | content,rich media | jexl | velocity | rootclass;captionclass | 26 |
| perc.widget.jquery | percJQueryWidget.xml | jQuery Widget | — | Design | jexl | velocity | — | 9 |
| perc.widget.jqueryUI | percJQueryUIWidget.xml | jQuery UI Widget | — | Design | jexl | velocity | — | 7 |
| perc.widget.login | percLogin.xml | Login | percLoginAsset | integration | jexl | velocity | rootclass | 2 |
| perc.widget.MostReadBlogPosts | percMostReadBlogPosts.xml | Most Read Blog Posts | — | search | jexl | velocity | rootclass | 14 |
| perc.widget.poll | percPoll.xml | Polls | percPollAsset | social,blog | jexl | velocity | rootclass | 7 |
| perc.widget.registration | percRegistration.xml | Registration (Deprecated) | percRegistrationAsset | integration | jexl | velocity | rootclass | 3 |
| perc.widget.Result | percResult.xml | Result | — | search | jexl | velocity | rootclass | 12 |
| perc.widget.rss | percRss.xml | RSS | percRssAsset | blog,social | jexl | velocity | rootclass | 16 |
| perc.widget.secureLogin | percSecureLogin.xml | Secure Login (Deprecated) | percSecureLogin | integration | jexl | velocity | rootclass;labelclass;inputclass;buttonclass | 2 |
| perc.widget.socialButtons | percSocialButtons.xml | Social Buttons | percSocialButtons | social | jexl | velocity | — | 9 |
| perc.widget.taglist | percTagList.xml | Tags | percTagList | blog,search | jexl | velocity | rootclass;summaryclass | 2 |
| perc.widget.title | percTitle.xml | Title | percTitleAsset | content | jexl | velocity | rootclass | 1 |
| perc.widgets.blog | percBlogPost.xml | Blog Post | percBlogPostAsset | blog | jexl | velocity | rootclass | 15 |
| perc.widgets.comments | percComments.xml | Comments | — | blog,social | jexl | velocity | rootclass | 1 |
| perc.widgets.image | percImage.xml | Image | percImageAsset | content,rich media | jexl | velocity | rootclass | 6 |
| perc.widgets.liked | percLiked.xml | Like | — | social | jexl | velocity | rootclass | 0 |
| perc.widgets.lists | simplePageAutoList.xml | Auto List | percSimpleAutoList | content,search | jexl | velocity | rootclass | 3 |
| perc.widgets.lists | simpleTextAutoList.xml | Text Auto List | percSimpleAutoList | content,search | jexl | velocity | — | 3 |
| perc.widgets.nav | percNavBar.xml | Navigation | — | navigation | jexl | velocity | rootclass | 12 |
| perc.widgets.nav | percNavBreadcrumb.xml | Breadcrumb | — | navigation | jexl | velocity | rootclass | 5 |
| perc.widgets.Redirect | percRedirect.xml | Redirect | percRedirect | — | jexl | velocity | — | 0 |
