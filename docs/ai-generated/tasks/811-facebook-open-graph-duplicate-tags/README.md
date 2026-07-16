# Bug Fix #811 – Facebook Open Graph Meta Tags Rendered Twice in Page Source

## Problem Summary

- When a page contains the **Facebook Open Graph** widget, all generated Open Graph meta tags (`og:site_name`, `og:title`, `og:description`, `og:url`, `og:type`, `og:image`, `og:image:width`, `og:image:height`, `og:locale`, `og:fb_app_id`) are rendered twice in the HTML source head section.

## Root Cause

- The JEXL code block in the Facebook Open Graph widget [percOpenGraph.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.openGraphWidget/sys__UserDependency--rxconfig/Widgets/percOpenGraph.xml) appends the meta tags directly to the page's `AdditionalHeadContent` variable.
- During page assembly, JEXL code of widgets on the template/page is evaluated twice (or inherited from template assembly context).
- Because the JEXL code did not check if the tags were already present in `AdditionalHeadContent`, it unconditionally appended them again, creating exact duplicates.

## Solution

- Modified the JEXL code inside [percOpenGraph.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.openGraphWidget/sys__UserDependency--rxconfig/Widgets/percOpenGraph.xml) to check if the specific Open Graph properties (e.g., `property="og:site_name"`) are already present in `AdditionalHeadContent` before appending them.

## Files Changed

- [percOpenGraph.xml](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.openGraphWidget/sys__UserDependency--rxconfig/Widgets/percOpenGraph.xml)

