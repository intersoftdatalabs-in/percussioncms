# Task: DTS Server Dropdown Default to NONE (Issue #847)

## Objective

Fix a bug in the publishing server configuration UI and backend where newly created sites fail their initial publish due to missing DTS server configuration (resulting in NullPointerExceptions, fallback warnings, or NONE values) until the publish configuration is manually saved. The DTS server association must be properly initialized and persisted during site creation.

## Changes Made

1. **Publishing Server DAO (`system/services/src/com/percussion/services/pubserver/impl/PSPubServerDao.java`)**:
   - Updated `createServer` method to query the `PSDeliveryInfoService` for the available DTS URLs of the corresponding server type (e.g. `PRODUCTION`).
   - Automatically initialize the `publishServer` property of the newly created publishing server to the first available DTS server URL if configured, or default it to `"NONE"`.
   - Wrap the lookup in a try-catch block to fall back safely to `"NONE"` in case the services are not fully initialized or lookup fails.
2. **Minuet Publisher View (`WebUI/war/views/PercPublishMinuetView.js`)**:
   - Added `"NONE"` explicitly as a valid option to the DTS Server select dropdown.
   - Refactored the selected server logic so that if the server configuration does not specify a DTS server (or specifies an empty/NONE value), it correctly defaults `selectedserver` to `"NONE"` instead of defaulting to the first available server in the list.
   - Verified that if the default selection is `"NONE"` or a valid DTS server, it selects the option correctly.

## Verification

- Ran `./mvn-env.sh spotless:apply` to ensure code formatting complies with project standards.
- Re-built `perc-web-ui` module via `./mvn-env.sh clean install -pl WebUI -DskipTests` successfully.
- Re-built `perc-system` module via `./mvn-env.sh clean install -pl system -DskipTests` successfully.

