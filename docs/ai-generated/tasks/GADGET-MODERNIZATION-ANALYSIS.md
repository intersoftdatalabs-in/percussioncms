# Gadget Modernization Analysis & Mapping

**Date:** February 25, 2026  
**Source:** GadgetRegistry.xml + Legacy Package Definitions  
**Status:** Phase 1 mostly complete; Phase 1d planning in progress

---

## Gadget Registry Overview

All 21 gadgets from `/WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml`:

---

## Phase Analysis: Legacy → React Migration

### Phase 1a - COMPLETE ✅ (Core Dashboard & Welcome)
Gadgets converted to React:
- ✅ **Welcome** → `WelcomeWidget.tsx`
- ✅ **Dashboard Container** → `Dashboard.tsx`
- ✅ **Layout** → `DashboardLayout.tsx`

### Phase 1b - COMPLETE ✅ (REST-Connected Widgets)
Gadgets converted:
- ✅ **Activity** → `ActivityWidget.tsx`
  - File: `perc_activity_gadget.xml`
  - REST Endpoint: `/services/activity/contentactivity`
  - Config Options: site selection, duration (days/weeks/months/years), row count
  - Status: **Converted** (basic timeline display, no config UI yet)

- ✅ **Pages By Status (Workflow)** → `WorkflowStatusWidget.tsx`
  - File: `perc_workflow_status_gadget.xml`
  - REST Endpoint: `/services/dashboardmanagement/gadget/workflow-status`
  - Config Options: row count display
  - Features: Search criteria panel, bulk approve button (not in React MVP)
  - Status: **Converted** (basic status display, no bulk operations yet)

### Phase 1c - COMPLETE ✅ (Configuration Management)
Infrastructure for managing widget configs:
- ✅ **useDashboardConfig Hook** → Configuration management
  - REST Endpoints: 
    - `GET /services/dashboardmanagement/dashboard/{userId}`
    - `PUT /services/dashboardmanagement/dashboard/{userId}`
  - Features: CRUD operations on widget configurations

---

## Phase 1d - PENDING (Advanced Widgets) 

### High Priority

#### 1. **Traffic Widget** 
- File: `perc_traffic_gadget.xml`
- baseuri: `/cm/gadgets/repository/perc_traffic_gadget`
- REST Endpoint: `/services/dashboard/traffic` (inferred)
- Complexity: **HIGH**
- Config Options:
  - Usage type: pageviews, unique pageviews
  - Site selection via `/services/sitemanage/site/choices`
  - Date range (from/to)
  - Granularity: DAY, WEEK, MONTH, YEAR
  - Activity options: new, updates, takedowns (boolean flags)
  - Live pages toggle
  - Display height: 5-25 rows
- Features in Legacy:
  - jqPlot graphs (bar/line charts)
  - Multiple data series visualization
  - Activity metrics display
  - Data tables with pagination
- React MVP Plan:
  - Start with fixed configuration
  - Simple bar/line chart using Recharts library
  - No user preference UI in Phase 1d (deferred to Phase 2)

#### 2. **Process Monitor Widget**
- File: `PercProcessorMonitorGadget.xml`
- baseuri: `/cm/gadgets/repository/PercProcessorMonitorGadget`
- REST Endpoint: `/services/processor/status` (inferred)
- Complexity: **MEDIUM**
- Config Options: Likely similar to activity (row count, duration)
- Legacy Features: Real-time processor status, job queue display
- React MVP Plan:
  - Display running/queued jobs
  - Job status indicators
  - Basic auto-refresh (30-60s)

#### 3. **Reports Widget**
- File: `perc_reports_gadget.xml`
- baseuri: `/cm/gadgets/repository/perc_reports_gadget`
- REST Endpoint: `/services/reports/list` (inferred)
- Complexity: **MEDIUM**
- Config Options: Likely report type selection
- Legacy Features: Available reports list, launch actions
- React MVP Plan:
  - List available reports
  - Quick launch buttons
  - No report execution in MVP

#### 4. **What's Working / Effectiveness Widget**
- File: `perc_effectiveness_gadget.xml`
- baseuri: `/cm/gadgets/repository/perc_effectiveness_gadget`
- REST Endpoint: `/services/effectiveness/summary` (inferred)
- Complexity: **MEDIUM**
- Features: Performance metrics, trending indicators
- React MVP Plan:
  - Display key metrics
  - Trend indicators (up/down/stable)
  - No chart details initially

### Medium Priority

#### 5. **Assets By Status Widget**
- File: `PercAssetStatusGadget.xml`
- REST Endpoint: `/services/asset/status` (inferred)
- Similar to Pages By Status
- Status/state display logic

#### 6. **Blogs Widget**
- File: `PercBlogsGadget.xml`
- REST Endpoint: `/services/blogs/list` (inferred)
- Blog listings and status

#### 7. **Bulk Upload Widget**
- File: `perc_bulk_file_upload_gadget.xml`
- REST Endpoint: `/services/upload/status` (inferred)
- Upload progress tracking

#### 8. **Comments Widget**
- File: `perc_comments_gadget.xml`
- REST Endpoint: `/services/comments/latest` (inferred)
- Comment feed display

#### 9. **Cookie Consent Widget**
- File: `perc_cookie_consent_gadget.xml`
- REST Endpoint: `/services/compliance/cookie-consent` (inferred)
- Compliance status display

#### 10. **Forms Tracker Widget**
- File: `PercFormTrackerGadget.xml`
- REST Endpoint: `/services/forms/tracker` (inferred)
- Form submission tracking

### Low Priority / Admin-Specific

#### 11. **Global Variables Widget**
- File: `PercGlobalVariablesGadget.xml`
- Admin-specific, may not need React version initially

#### 12. **Google Setup Widget**
- File: `perc_google_setup_gadget.xml`
- Integration configuration

#### 13. **Iframe Widget**
- File: `perc_iframe_gadget.xml`
- Generic iframe loader

#### 14. **Membership Widget**
- File: `perc_membership_gadget.xml`
- User/membership management

#### 15. **Siteimprove Widget**
- File: `perc_site_improve_gadget.xml`
- Third-party integration

#### 16. **SEO Audit Widget**
- File: `perc_seo_status_gadget.xml`
- REST Endpoint: `/services/seo/audit` (inferred)
- SEO metrics display

#### 17. **Sitewide Framework Widget**
- File: `perc_sitewide_framework_gadget.xml`
- Framework-level configuration

#### 18. **Widget Configuration Widget**
- File: `PercWidgetConfigGadget.xml`
- Meta-widget for managing dashboard itself
- High complexity for React implementation

---

## REST Endpoint Mapping (Inferred)

### Confirmed (Currently Used)
| Widget | Endpoint | Method | Purpose |
|--------|----------|--------|---------|
| Activity | `/services/activity/contentactivity` | GET | Recent activity timeline |
| Workflow | `/services/dashboardmanagement/gadget/workflow-status` | GET | Workflow statuses |
| Dashboard Config | `/services/dashboardmanagement/dashboard/{userId}` | GET/PUT | Load/save user preferences |
| Site Choices | `/services/sitemanage/site/choices` | GET | List available sites |

### Inferred (Need Verification)
| Widget | Endpoint | Method | Purpose |
|--------|----------|--------|---------|
| Traffic | `/services/dashboard/traffic` | GET | Traffic metrics |
| Process Monitor | `/services/processor/status` | GET | Job queue status |
| Reports | `/services/reports/list` | GET | Available reports list |
| Effectiveness | `/services/effectiveness/summary` | GET | Performance metrics |
| Assets Status | `/services/asset/status` | GET | Asset workflow status |
| SEO Audit | `/services/seo/audit` | GET | SEO metrics |

---

## Architecture Recommendations

### For Phase 1d Implementation

1. **Widget Component Pattern**
   ```typescript
   // Standardized widget structure:
   - Loading state display
   - Error state with retry
   - Empty state message
   - Auto-refresh with configurable intervals
   - Inline styling (consistent with dashboard.styles.ts)
   - Typed API client for all REST calls
   - Optional: Widget-specific preferences (stored via useDashboardConfig)
   ```

2. **Configuration Strategy**
   - Phase 1d: Hard-coded default configs (5-10 item defaults)
   - Phase 2: User preference UI in widget menu
   - Phase 2+: Drag-and-drop layout with persistence via useDashboardConfig

3. **REST Endpoint Strategy**
   - Verify all inferred endpoints exist
   - Check query parameter support (site, duration, granularity, etc.)
   - Ensure CSRF token injection (automatic via apiClient)

4. **Chart/Visualization Library**
   - Add Recharts (already used in modern React projects)
   - For Traffic: Bar/Line charts
   - For Effectiveness: KPI cards with trend indicators
   - For Process Monitor: Progress bars and status badges

---

## Implementation Priority Order

### Tier 1 (Phase 1d) - Most Used Gadgets
1. **Traffic** (HIGH complexity) - Heavily used, complex charts
2. **Process Monitor** (MEDIUM) - Essential for operations
3. **Reports** (MEDIUM) - Common workflow

### Tier 2 (Phase 2) - Specialized Gadgets
4. **Effectiveness** (MEDIUM)
5. **Assets By Status** (MEDIUM)
6. **Blogs** (LOW)

### Tier 3 (Phase 2+) - Admin/Integration
7. All others (integrate as needed)

---

## Code Structure for Phase 1d

```
WebUI/src/main/ts/dashboard/
├── widgets/
│   ├── TrafficWidget.tsx                    # Phase 1d
│   ├── ProcessMonitorWidget.tsx             # Phase 1d
│   ├── ReportsWidget.tsx                    # Phase 1d
│   ├── EffectivenessWidget.tsx              # Phase 2
│   └── ...
├── hooks/
│   ├── useDashboardConfig.ts                # ✅ Phase 1c
│   ├── useWidgetPreferences.ts              # Phase 2
│   └── useTrafficData.ts                    # Shared data hooks
│
└── Dashboard.tsx (updated to support all widgets)
```

---

## Testing Strategy

### Unit Tests for Each Widget
- Rendering with mock API data
- Loading/error state handling
- Empty data display
- Auto-refresh interval behavior
- Configuration prop validation

### Integration Tests
- Dashboard loads multiple widgets
- useDashboardConfig provides config to widgets
- CSRF tokens injected correctly
- Performance under load (multiple widgets refreshing)

### E2E Tests (Phase 2)
- User preferences saved and persisted
- Widgets update when config changes
- Drag-and-drop reordering works
- Feature flag toggle switches between old/new dashboard

---

## Questions for Implementation

1. **REST Endpoints**: Should I verify all the inferred endpoints against existing Java services?
2. **Chart Library**: Should Phase 1d Traffic widget use Recharts for visualization?
3. **Default Configs**: What are good default values for duration, row counts, etc.?
4. **Priority**: Should I start with Traffic or Process Monitor first?
5. **Legacy CSS/JS**: Any existing CSS/JS from legacy gadgets that should be preserved/ported?

---

## Summary

- **Total Gadgets:** 21
- **Converted (Phase 1a-c):** 3 core + 2 data widgets = 5
- **Ready for Phase 1d:** 3 high-priority (Traffic, Process Monitor, Reports)
- **Remaining:** 13 specialized/admin widgets (Phase 2+)
- **REST Endpoints:** 4 confirmed, 6+ inferred (need verification)
- **Build Status:** All Phase 1 code compiling cleanly ✅
