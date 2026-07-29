# Phase 1: Dashboard React Component Implementation Plan

## Objective

Replace the retired Apache Shindig 3.0.0-beta4 gadget container with modern React/TypeScript dashboard widgets that call existing REST endpoints.

**Security Impact:** Removes high-risk dependencies (Guice 2.0, Google Collections 1.0-rc2, Caja, ancient OAuth).

## Architecture Overview

```
WebUI/src/main/ts/
├── dashboard/
│   ├── Dashboard.tsx          (main component)
│   ├── DashboardLayout.tsx    (two-column grid with drag-drop)
│   ├── useDashboardConfig.ts  (hook: load/save layout)
│   ├── widgets/
│   │   ├── WelcomeWidget.tsx
│   │   ├── WorkflowStatusWidget.tsx
│   │   ├── ActivityWidget.tsx
│   │   ├── ContentTrafficWidget.tsx
│   │   ├── ProcessMonitorWidget.tsx
│   │   └── ReportsWidget.tsx
│   └── index.ts               (exports)
```

## Component Details

### 1. Dashboard.tsx (Main Container)

- Renders two-column layout with drag-and-drop grid
- Loads user's saved dashboard configuration via `useDashboardConfig`
- Renders widget components based on config
- Fallback: feature flag (`?legacyDashboard=true`) to load old Shindig dashboard

### 2. DashboardLayout.tsx

- Two-column responsive grid layout
- Optional: `react-grid-layout` for drag-and-drop support
- Otherwise: CSS Grid with fixed widget positions
- Initially: match existing visual design (minimal redesign)

### 3. Widget Components

|        Widget        |           Purpose           |                     REST Endpoint                      | Priority |
|----------------------|-----------------------------|--------------------------------------------------------|----------|
| WelcomeWidget        | Static welcome + links      | None                                                   | 1        |
| WorkflowStatusWidget | Show pending workflow tasks | `/services/dashboardmanagement/gadget/workflow-status` | 2        |
| ActivityWidget       | Recent content activities   | `/services/activity/contentactivity`                   | 2        |
| ContentTrafficWidget | Traffic metrics             | `/services/activity/contenttraffic`                    | 3        |
| ProcessMonitorWidget | Active processes            | `/services/dashboardmanagement/gadget/process-monitor` | 3        |
| ReportsWidget        | Quick reports               | `/services/dashboardmanagement/gadget/reports`         | 3        |

### 4. useDashboardConfig Hook

```typescript
interface DashboardConfig {
  widgets: {
    id: string;
    name: string;
    visible: boolean;
    position?: { x: number; y: number; w: number; h: number };
  }[];
}

// Loads from `/services/dashboardmanagement/dashboard/{userId}`
// Saves to PUT `/services/dashboardmanagement/dashboard/{userId}`
```

## Implementation Phases

### Phase 1a: Foundation (Weeks 1-2)

- [ ] Create Dashboard.tsx + DashboardLayout.tsx
- [ ] Create WelcomeWidget (static, no API calls)
- [ ] Register Dashboard in component registry
- [ ] Create mount point (JSP or embed script)
- [ ] Test locally with `npm run dev`

### Phase 1b: Widgets (Weeks 2-3)

- [ ] Create WorkflowStatusWidget + ActivityWidget
- [ ] Test REST API calls with CSRF token injection
- [ ] Add mock data for offline development
- [ ] Error handling + loading states

### Phase 1c: Config & Features (Weeks 3-4)

- [ ] Implement useDashboardConfig hook
- [ ] Add feature flag for legacy fallback
- [ ] Drag-and-drop (optional: skip if CSS Grid sufficient)
- [ ] Save/load user layout preferences

## REST Endpoints to Call

### Existing Endpoints (from sitemanage)

```
GET  /services/dashboardmanagement/dashboard/{userId}
PUT  /services/dashboardmanagement/dashboard/{userId}
GET  /services/dashboardmanagement/gadget/workflow-status
GET  /services/dashboardmanagement/gadget/process-monitor
GET  /services/dashboardmanagement/gadget/reports
GET  /services/activity/contentactivity
GET  /services/activity/contenttraffic
```

### CSRF Token Injection

All requests automatically include OWASP CSRFGuard token via the typed API client (Phase 0: `src/api/client.ts`).

## Testing Strategy

### Unit Tests (Vitest)

- Widget rendering with mock data
- Layout responsiveness
- Hook behavior (useDashboardConfig)

### Integration Tests

- Real API calls (mock/stub services)
- Feature flag behavior
- Dashboard.jsp mounting

### Manual Testing

- Local dev: `npm run dev`
- Build: `./mvnw clean install -pl WebUI`
- Browser: Load `/cm/app/dashboard.jsp` (feature flag to toggle old/new)

## Success Criteria

✅ Dashboard renders without errors
✅ All widgets render with placeholder data
✅ REST API calls work with CSRF tokens
✅ Drag-and-drop layout (if implemented)
✅ Feature flag toggle works
✅ Zero console errors
✅ Build succeeds with no new warnings

## Timeline

Target: **Complete by end of sprint**
- Phase 1a: ~3 days
- Phase 1b: ~3 days
- Phase 1c: ~2 days
- Buffer: 1 day for fixes/review

## Notes

- Do NOT remove Shindig dependency yet (keep as fallback for beta)
- Keep visual design consistent with legacy dashboard
- Use existing REST endpoints; do NOT create new ones
- Reuse typed API client from Phase 0
- No external UI libraries yet (use CSS Grid instead of react-grid-layout for now)

