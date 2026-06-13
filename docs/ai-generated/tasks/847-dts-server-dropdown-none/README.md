# Task: DTS Server Dropdown Default to NONE (Issue #847)

## Objective

Fix a bug in the publishing server configuration UI where the Select DTS Server dropdown incorrectly displays a configured server even when NONE is configured (e.g. for newly created sites). It should default to NONE when no DTS server is configured.

## Changes Made

1. **Minuet Publisher View (`WebUI/war/views/PercPublishMinuetView.js`)**:
   - Added `"NONE"` explicitly as a valid option to the DTS Server select dropdown.
   - Refactored the selected server logic so that if the server configuration does not specify a DTS server (or specifies an empty/NONE value), it correctly defaults `selectedserver` to `"NONE"` instead of defaulting to the first available server in the list.
   - Verified that if the default selection is `"NONE"` or a valid DTS server, it selects the option correctly.

## Verification

- Ran `./mvn-env.sh spotless:apply` to ensure code formatting complies with project standards.
- Re-built `perc-web-ui` module via `./mvn-env.sh clean install -pl WebUI -DskipTests` successfully.
