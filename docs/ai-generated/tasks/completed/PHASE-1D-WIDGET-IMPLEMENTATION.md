# Phase 1d: Implement Remaining Dashboard Widgets

**Status**: In Progress ✅ (ProcessMonitorWidget complete)
**Last Updated**: 2026-02-25
**Branch**: development (JDK 21)

## Completed

### ✅ ProcessMonitorWidget (MERGED)

- **Location**: [WebUI/src/main/ts/dashboard/ProcessMonitorWidget.tsx](WebUI/src/main/ts/dashboard/ProcessMonitorWidget.tsx)
- **Tests**: [WebUI/src/test/ts/dashboard/ProcessMonitorWidget.test.tsx](WebUI/src/test/ts/dashboard/ProcessMonitorWidget.test.tsx)
- **REST Endpoint**: `GET /services/monitor/all`
- **Features**:
  - Display system monitor status (running, paused, error)
  - Auto-refresh (default 30s, configurable)
  - Status-based icons (✅ 🟡 ❌)
  - Handles multiple response formats ('monitors' or 'monitor' property)
- **Test Coverage**: 13 test cases covering all scenarios
- **Commit**: [283f1a5c86](https://github.com/...)

## In Development

### Phase 1d Widget Priority Queue

**Priority 1: User Preferences/Configuration Widget** (HIGHEST IMPACT)
- **Complexity**: LOW
- **REST Endpoint**: `GET /services/dashboardmanagement/dashboard/{userId}`
- **Features**:
- Display current user dashboard configuration
- Add/remove widgets UI
- Drag-drop reordering (using react-beautiful-dnd)
- Save preferences
- **Estimated Effort**: 4-5 hours
- **Blockers**: None
- **Notes**: Core infrastructure that enables full feature parity

**Priority 2: Reports Widget** (HIGH - Simple REST)
- **Complexity**: LOW
- **REST Endpoint**: `GET /services/reports/list`
- **Features**:
- List available reports
- Launch buttons
- Report icons/descriptions
- **Estimated Effort**: 2-3 hours
- **Blockers**: Need to verify actual Reports REST service

**Priority 3: Effectiveness Widget** (HIGH - Metrics Display)
- **Complexity**: LOW-MEDIUM
- **REST Endpoint**: `POST /services/activity/effectiveness`
- **Features**:
- Performance metrics timeline
- Trend indicators (↑ ↓ →)
- Response time graphs (Recharts)
- Sparklines for quick insights
- **Estimated Effort**: 3-4 hours
- **Blockers**: Recharts library setup

**Priority 4: Traffic Widget** (MEDIUM - Complex Charts)
- **Complexity**: MEDIUM-HIGH
- **REST Endpoint**: `POST /services/activity/contenttraffic` + `POST /services/activity/trafficdetails`
- **Features**:
- Traffic timeline (Recharts LineChart)
- Date range selector (last 7 days, 30 days, custom)
- Traffic breakdown by content type
- Peak traffic indicators
- Hourly/daily/weekly granularity
- **Estimated Effort**: 5-6 hours
- **Blockers**: Date picker UI, Recharts configuration

## Pending Features (Phase 1e+)

### Add/Remove Widget Functionality

- Add button to Dashboard component
- Modal/dropdown to select widgets to add
- Delete button on each widget
- Persist configuration via useDashboardConfig hook
- Undo/restore functionality

### Drag-Drop Layout

- Integrate react-beautiful-dnd library
- Enable drag-drop reordering on Dashboard
- Save layout preferences
- Responsive grid adjustment

### Additional Specialized Widgets (13 more)

1. Unpublished Items Widget
2. Recent Cache Keywords Widget
3. System Performance Widget
4. Content Approval Queue Widget
5. Failed Job Reports Widget
6. User Activity by Content Type Widget
7. Page Response Times Widget
8. Search Efficiency Widget
9. Scheduled Tasks Widget
10. Server Health Widget
11. Database Optimization Widget
12. Workflow Status Details Widget
13. Site Analytics Widget

## Technical Setup

### Dependencies Already Available

✅ React 19.1.0
✅ TypeScript 5.8.3
✅ Vitest 3.2.1
✅ React Testing Library 16.3.0
✅ Vite 6.3.5

### Dependencies Needed for Phase 1d

- **react-beautiful-dnd** (drag-drop): v13.1.1
- **recharts** (charts): v2.12.7
- **date-fns** (date handling): v3.18.0
- **react-calendar** or **react-datepicker** (optional): for date range picker

### Installation Command

```bash
npm install react-beautiful-dnd recharts date-fns
npm install -D @types/react-beautiful-dnd
```

## REST Endpoints Reference

All endpoints verified from Java source code:

| Endpoint                                           | Method          | Description             | Location                   |
|:---------------------------------------------------|:----------------|:------------------------|:---------------------------|
| `/services/monitor/all`                            | GET             | All system monitors     | PSMonitorService ✅         |
| `/services/monitor/list`                           | GET             | Monitor designators     | PSMonitorService ✅         |
| `/services/activity/contentactivity`               | POST            | Recent content activity | PSContentActivityService ✅ |
| `/services/activity/effectiveness`                 | POST            | Performance metrics     | PSContentActivityService ✅ |
| `/services/activity/contenttraffic`                | POST            | Traffic metrics         | PSContentActivityService ✅ |
| `/services/activity/trafficdetails`                | POST            | Detailed traffic data   | PSContentActivityService ✅ |
| `/services/dashboardmanagement/dashboard/{userId}` | GET/PUT         | User dashboard config   | PSUserProfileRestService   |
| `/services/gadget/`                                | GET/POST/DELETE | Gadget CRUD             | PSGadgetRestService ✅      |
| `/services/reports/list`                           | GET             | Available reports       | TBD - Needs verification   |

## Code Quality Standards

### Widget Template

Each Phase 1d widget should follow this pattern:

```typescript
// 1. Imports and types
import React, { useEffect, useState } from 'react';
import { get, post } from '../api/client';
import { styles } from './dashboard.styles';

interface DataType { /* ... */ }
export interface WidgetProps { /* JSDoc */ }

// 2. Component with JSDoc
export const MyWidget: React.FC<WidgetProps> = ({ /* props */ }) => {
  const [data, setData] = useState<DataType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 3. useEffect for data fetching
  useEffect(() => { /* fetch logic */ }, [/* deps */]);

  // 4. Helper functions
  const handleAction = (item: DataType) => { /* ... */ };

  // 5. Render helpers
  const renderContent = () => { /* TSX */ };

  // 6. Return widget with title and content
  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      {renderContent()}
    </div>
  );
};

export default MyWidget;
```

### Test Template

- Use vitest + React Testing Library
- Mock API client with vi.mock('@/api/client')
- Test: loading state, error state, empty state, data display
- Test: prop variations (custom title, custom refresh interval, etc.)
- Test: cleanup/unmount (clear intervals)
- Minimum 10-13 test cases per widget
- All tests should pass with 100% coverage

## Build & Test Commands

```bash
# Build WebUI module
./mvnw clean package -DskipTests -pl WebUI

# Run all tests
./mvnw test -pl WebUI

# Run specific test file
npm test WebUI/src/test/ts/dashboard/ProcessMonitorWidget.test.tsx

# Watch mode
npm run test:watch

# Check TypeScript
npm run build

# Lint
npm run lint
```

## Next Immediate Steps

1. **[OPTIONAL] Install chart/drag-drop dependencies** - Add to package.json if starting Traffic/DragDrop widgets
2. **Create Reports Widget** - Simplest, no complex logic needed (2-3 hours)
3. **Create Effectiveness Widget** - Basic metrics display with simple charts (3-4 hours)
4. **Create Traffic Widget** - More complex charts with date range (5-6 hours)
5. **Implement Dashboard Feature Parity** - Add/remove buttons, drag-drop (4-5 hours)

## Estimated Timeline

- **Reports Widget**: ✏️ TODO (2-3 hrs) - EASY
- **Effectiveness Widget**: ✏️ TODO (3-4 hrs) - EASY
- **Traffic Widget**: ✏️ TODO (5-6 hrs) - MEDIUM
- **Feature Parity**: ✏️ TODO (4-5 hrs) - MEDIUM
- **Total Phase 1d**: ~18-22 hours (fits in 2-3 development days)

## Known Issues & Workarounds

| Issue                          | Status  | Workaround                             |
|:-------------------------------|:--------|:---------------------------------------|
| Inline styles linting warnings | Known   | Existing codebase pattern - can ignore |
| Recharts peer dependencies     | Pending | Install dev dependencies if needed     |
| Date picker library choice     | TBD     | react-datepicker easier to integrate   |
| Drag-drop accessibility        | TBD     | react-beautiful-dnd widely tested      |

## References

- Phase 1a-1c: Dashboard foundation + ActivityWidget + WorkflowStatusWidget
- Gadget Modernization Analysis: [docs/ai-generated/tasks/GADGET-MODERNIZATION-ANALYSIS.md](docs/ai-generated/tasks/GADGET-MODERNIZATION-ANALYSIS.md)
- REST Endpoints: Verified in Java services (PSActivityService, PSMonitorService, PSGadgetRestService)
- Updated registry: [WebUI/src/main/ts/registry.ts](WebUI/src/main/ts/registry.ts)

## Completion Criteria

Each Phase 1d widget is complete when:
- ✅ Component created with full TSX/JSX implementation
- ✅ REST integration with correct endpoint and HTTP method
- ✅ Props interface documented with JSDoc
- ✅ Error handling and loading states
- ✅ 10+ test cases with full coverage
- ✅ Registered in dashboard module exports
- ✅ Registered in component registry (registry.ts)
- ✅ Builds successfully (tsc --noEmit + vite build)
- ✅ All tests pass
- ✅ Commit message with clear description

---

**Status Summary**: ProcessMonitorWidget ✅ | Reports Widget ⏳ | Effectiveness Widget ⏳ | Traffic Widget ⏳ | Feature Parity ⏳

Next: Implement Reports Widget (simplest Phase 1d widget)
