# GA4 Analytics Migration Plan (#756)

## Overview

Migrate Google Analytics integration from Universal Analytics (UA) to Google Analytics 4 (GA4).

**Google sunset UA on July 1, 2024** - migration is critical.

Based on a detailed review, **a complete refactor of the Google provider implementation is required**. The architectural differences between UA and GA4 (Accounts/Properties/Views vs Accounts/Properties) and the complete shift in client libraries (REST API vs gRPC-based Google Cloud libraries) mean that attempting a 1-to-1 code translation will lead to brittle and complex code. We should rewrite the provider classes entirely to provide a clean, modern integration.

## Current State

### APIs Used (to be removed completely)

- `com.google.api.services.analyticsreporting.v4` - Analytics Reporting API v4
- `com.google.api.services.analytics` - Analytics Management API v3

### Dependencies (Already Added)

- `com.google.cloud:libraries-bom:26.80.0` (root pom.xml)
- `com.google.analytics:google-analytics-data` (sitemanage pom.xml)
- `com.google.analytics:google-analytics-admin` (sitemanage pom.xml)

## User Review Required

> [!WARNING]
> Because GA4 does not have "Profiles" or "Views" (only Properties), the existing UI/configuration that asks users to select a "Profile" will now display a list of GA4 **Properties**. The internal ID stored in the CMS configuration will change from the UA Profile ID (e.g. `12345678|12345`) to the GA4 Property ID (e.g. `properties/123456789`). This is a breaking change for existing site configurations, which will need to be re-authenticated/re-configured to select the new GA4 property. Does this approach align with your expectations?

## Proposed Changes

### Component: Analytics Provider Services

We will perform a complete rewrite of the Google Analytics provider classes to drop all UA dependencies and use the new GA4 Java clients (`BetaAnalyticsDataClient` and `AnalyticsAdminServiceClient`). We will retain the existing interface implementations (`IPSAnalyticsProviderHandler`, `IPSAnalyticsProviderQueryHandler`) to avoid breaking the CMS service layer interfaces.

#### [MODIFY] [PSGoogleAnalyticsProviderHelper.java](file:///home/nate/projects/java8/percussioncms/.worktrees/feature/756-ga4-analytics-migration/projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHelper.java)

- **Remove** all UA imports (`Analytics`, `AnalyticsReporting`, `GoogleCredential`, `JacksonFactory`, `PemReader`).
- **Implement** native `GoogleCredentials.fromStream()` parsing to simplify authentication.
- **Add** methods to initialize and return GA4 `AnalyticsAdminServiceClient` (for listing accounts/properties) and `BetaAnalyticsDataClient` (for running reports) using `FixedCredentialsProvider`.
- Remove the custom `GoogleCreds` class and PKCS8 private key parsers as `GoogleCredentials` handles this natively.

#### [MODIFY] [PSGoogleAnalyticsProviderHandler.java](file:///home/nate/projects/java8/percussioncms/.worktrees/feature/756-ga4-analytics-migration/projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHandler.java)

- **Complete Rewrite** of `getProfiles()`.
- Use `AnalyticsAdminServiceClient.searchAccountSummaries()` to list accounts and their child properties.
- GA4 only has Properties, so the returned Map will use the GA4 Property Name (e.g., `properties/123456789`) as the key and the Property Display Name as the value.

#### [MODIFY] [PSGoogleAnalyticsProviderQueryHandler.java](file:///home/nate/projects/java8/percussioncms/.worktrees/feature/756-ga4-analytics-migration/projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderQueryHandler.java)

- **Complete Rewrite** of `getVisitsViewsBySite()` and `getPageViewsByPathPrefix()`.
- Replace `ReportRequest` (UA) with `RunReportRequest` (GA4).
- Update metric and dimension names:
  - `ga:pageviews` -> `screenPageViews`
  - `ga:uniquePageviews` -> `sessions` (approximate as GA4 doesn't have an exact unique pageviews metric)
  - `ga:visits` -> `sessions`
  - `ga:newVisits` -> `newUsers`
  - `ga:date` -> `date`
  - `ga:pagePath` -> `pagePath`
- Parse the GA4 `RunReportResponse` and `Row` data structures into the existing `IPSAnalyticsQueryResult` lists.
- Update the filtering logic to use GA4's `FilterExpression` for `pagePath` prefix filtering.

## Verification Plan

### Automated Tests

- Build the module to ensure no compilation errors: `./mvn-env.sh clean compile -pl projects/sitemanage -am`
- Run Spotless formatting: `./mvn-env.sh spotless:apply -pl projects/sitemanage`
- Run Checkstyle/PMD: `./mvn-env.sh verify -pl projects/sitemanage`
- Run existing Analytics tests (note: these might need to be updated or mocked differently since the underlying client changed): `./mvn-env.sh test -pl projects/sitemanage -Dtest=*Analytics*`

### Manual Verification

- Deploy to a local CMS instance.
- Configure Google Analytics using a GA4 Service Account JSON key.
- Verify the "Profile" dropdown populates correctly with GA4 Properties instead of UA profiles.
- Verify the Analytics dashboard renders metrics correctly (Pageviews, Visits, etc.) using the GA4 property.

