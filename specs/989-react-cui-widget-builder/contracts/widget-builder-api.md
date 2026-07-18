# Contract: Widget Builder REST (reuse)

**Owner service**: `projects/sitemanage/.../PSWidgetBuilderService.java`  
**Base**: `{SERVICES_ROOT}/widgetmanagement/widgetbuilder`  
(`SERVICES_ROOT` as used by WebUI `perc_path_constants.js`, typically under `/Rhythmyx/services`.)

Modern React client MUST call these endpoints (typed wrappers OK). **No breaking changes** required for this feature.

## Endpoints

### GET `/active`

- **Response**: boolean (or JSON boolean) — Widget Builder enabled.
- **UI**: Hide nav entry or show denied state when false (parity with today).

### GET `/summaries`

- **Response**: list of summary DTOs (`PSWidgetBuilderSummaryData` / list wrapper as today).
- **UI**: Definition list / empty state.

### GET `/definition/{definitionId}`

- **Response**: full `PSWidgetBuilderDefinitionData`.
- **UI**: Edit form load.

### POST `/definition/`

- **Body**: full definition JSON/XML as currently accepted.
- **Response**: `PSWidgetBuilderValidationResults` (errors or success id).
- **Semantics**: **Last write wins** for concurrent editors (FR-015). UI confirms save; reload shows server state.

### POST `/validate/`

- **Body**: full definition.
- **Response**: validation results without requiring persist (if current server behavior).

### POST `/deploy/{definitionId}`

- **Effect**: build/deploy package server-side.
- **UI**: package/export action; surface server errors.

### DELETE `/definition/{definitionId}`

- **Effect**: remove definition.
- **UI**: delete with confirmation.

### GET `/deployed/{definitionId}`

- **Response**: boolean deployed flag (optional UI badge).

## AuthZ / session

- Same authenticated CMS session as other Web Management views.
- View gates: `widgetbuilder` in admin/designer view lists in `index.jsp`.
- CSRF: OWASP CSRFGuard header on mutating methods via modern `api/client.ts`.

## Error contract

- Non-2xx: modern UI shows recoverable message (no blank screen).
- Validation failures: field/message list from results object—do not invent client-only package rules that contradict server.
