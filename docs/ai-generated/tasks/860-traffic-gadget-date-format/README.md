# Task: Traffic Gadget Fails with HTTP 500 Due to Invalid Google Analytics Date Format When Selecting and Saving a Site (Issue #860)

## Objective

Fix an HTTP 500 error in the Traffic Gadget when loading traffic data from the Google Analytics (GA4) API. The GA4 Data API requires the `startDate` and `endDate` parameters to be formatted as `YYYY-MM-DD`, but the application was sending them in the `yyyyMMdd` format, causing an `InvalidArgumentException` / `StatusRuntimeException`.

## Changes Made

1. **PSGoogleAnalyticsProviderHelper (`projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHelper.java`)**:
   - Added a new `QUERY_DATE_FORMAT` of type `FastDateFormat` mapped to pattern `"yyyy-MM-dd"`.
   - Exposed a new synchronized getter method `getQueryDateFormat()` to retrieve the new formatter.
   - Kept the original `DATE_FORMAT` (`"yyyyMMdd"`) unchanged, as it is still used by `parseDate()` to parse returned date values from the Google Analytics API responses.
2. **PSGoogleAnalyticsProviderQueryHandler (`projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderQueryHandler.java`)**:
   - Modified `createQueryForPageViewsByPathPrefix` to format query dates (`startDate` and `endDate`) using the new helper method `getQueryDateFormat()`.
   - Modified `createQueryForVisitsViews` to format query dates (`startDate` and `endDate`) using the new helper method `getQueryDateFormat()`.

## Verification

- Ran `./mvn-env.sh spotless:apply` to ensure Google Java Format is correctly applied to the codebase.
- Built the `sitemanage` module along with its dependencies via `./mvn-env.sh clean install -pl projects/sitemanage -am -DskipTests` successfully.
- Ran tests in `sitemanage` via `./mvn-env.sh test -pl projects/sitemanage` successfully.
- Verified that all PMD, Checkstyle, and other validations pass using `./mvn-env.sh verify -pl projects/sitemanage -DskipTests` successfully.

