# Bug Fix #801 – Image Slider Widget Edit Template Warning/Error Dialog

## Problem Summary
- When editing a template containing the **Image Slider** widget, a warning/error icon is displayed in the template editor. Clicking on it opens an error dialog stating:
  `File not found -- http://localhost:9992/Rhythmyx/web_resources/widgets/imageSlider/css/percImageSlider.css`
- Additionally, the server log emits:
  `ERROR [velocity] Error in $rx.pageutils.isLinkGood: no protocol: /web_resources/cm/jslib/jquery.js`

## Root Cause
1. **Invalid Context Loading in Resource Definition:**
   [percImageSlider.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.imageSlider/sys__UserDependency--rxconfig/Resources/percImageSlider.xml) defines two entries for the widget stylesheet:
   - One for `PREVIEW` context pointing to `/rx_resources/widgets/percImageSlider/css/percImageSlider.css`.
   - One without a context mapping to `/web_resources/widgets/imageSlider/css/percImageSlider.css`.
   Because the latter has no context constraint, it matches all contexts (including `PREVIEW` in the template designer). Consequently, both files are loaded, causing the designer browser to attempt fetching `/Rhythmyx/web_resources/widgets/imageSlider/css/percImageSlider.css`, which is absent or misrouted in the designer.
2. **Missing Protocol Support in `isLinkGood` JEXL function:**
   [PSPageUtils.isLinkGood](file:///home/nate/projects/java8/percussioncms/projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java) receives relative URLs (like `/web_resources/cm/jslib/jquery.js`) but attempts to parse them directly via `new URL(link)`. This fails with a `MalformedURLException` due to a missing protocol, logging an error and falsely marking the link as invalid/bad.

## Solution
1. **Apply Context Constraints on Resources:**
   Updated [percImageSlider.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.imageSlider/sys__UserDependency--rxconfig/Resources/percImageSlider.xml) to mark the default stylesheet as `context="PUBLISH"`. This ensures it is only evaluated/loaded in publish context, preventing the designer from requesting it.
2. **Enhance `isLinkGood` to handle relative links:**
   Modified [PSPageUtils.isLinkGood](file:///home/nate/projects/java8/percussioncms/projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java) to detect relative URLs (missing `://` and `//`). When relative, it resolves them by constructing a full local loopback URL (using `localhost`, the active listener port from `PSServer.getListenerPort()`, and prepending the `/Rhythmyx` servlet context if not present).

## Files Changed
- [percImageSlider.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.widget.imageSlider/sys__UserDependency--rxconfig/Resources/percImageSlider.xml)
- [PSPageUtils.java](file:///home/nate/projects/java8/percussioncms/projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java)
