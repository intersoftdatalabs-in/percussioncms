# Product widget XML inventory

**Generated:** 2026-08-09  
**Source:** `modules/perc-packages/src/main/resources/Packages/**/rxconfig/Widgets/*.xml`  
**Machine-readable:** [widget-xml-inventory.csv](./widget-xml-inventory.csv)

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
