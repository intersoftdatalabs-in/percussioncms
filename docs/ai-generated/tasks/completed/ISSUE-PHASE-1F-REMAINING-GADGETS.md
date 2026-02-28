# Phase 1f - Remaining Gadget Implementation Plan

**Status:** Ready to start
**Total Gadgets to Implement:** 12
**Target Completion:** Systematic, build-verified rollout
**Dependencies:** All Phase 1a-1e complete ✅

---

## Implementation Priority & Breakdown

### Tier 1: Content Management (High Priority) - 4 gadgets

1. **Blogs Widget** - Blog listings and management
   - File: `PercBlogsGadget.xml`
   - REST Endpoint: `/services/blogs/list` (inferred)
   - Complexity: **LOW**
   - Similar to: ReportsWidget (list display pattern)
2. **Comments Widget** - Latest comments feed
   - File: `perc_comments_gadget.xml`
   - REST Endpoint: `/services/comments/latest` (inferred)
   - Complexity: **LOW**
   - Similar to: ActivityWidget (timeline pattern)
3. **Forms Tracker Widget** - Form submission tracking
   - File: `PercFormTrackerGadget.xml`
   - REST Endpoint: `/services/forms/tracker` (inferred)
   - Complexity: **MEDIUM**
   - Features: Submission count, status breakdown, trending
4. **Bulk Upload Widget** - ✅ ALREADY DONE (Phase 1d)

### Tier 2: Compliance & Analytics (Medium Priority) - 3 gadgets

5. **Cookie Consent Widget** - Compliance tracking
   - File: `perc_cookie_consent_gadget.xml`
   - REST Endpoint: `/services/compliance/cookie-consent` (inferred)
   - Complexity: **LOW**
   - Status display, consent metrics
6. **SEO Audit Widget** - SEO metrics and recommendations
   - File: `perc_seo_status_gadget.xml`
   - REST Endpoint: `/services/seo/audit` (inferred)
   - Complexity: **MEDIUM**
   - Score display, key metrics, issues list
7. **Google Setup Widget** - Google integration config
   - File: `perc_google_setup_gadget.xml`
   - REST Endpoint: Need to verify
   - Complexity: **MEDIUM**
   - Config status, integration controls

### Tier 3: System/Admin (Lower Priority) - 3 gadgets

8. **Global Variables Widget** - Admin configuration
   - File: `PercGlobalVariablesGadget.xml`
   - REST Endpoint: `/services/admin/variables` (inferred)
   - Complexity: **MEDIUM**
   - Key-value pair display, edit controls
9. **Membership Widget** - User/membership management
   - File: `perc_membership_gadget.xml`
   - REST Endpoint: `/services/membership/list` (inferred)
   - Complexity: **LOW-MEDIUM**
   - Status display, member count
10. **Sitewide Framework Widget** - Framework config
    - File: `perc_sitewide_framework_gadget.xml`
    - REST Endpoint: `/services/framework/config` (inferred)
    - Complexity: **MEDIUM**
    - Configuration display, status checks

### Tier 4: Integration/Utility (Lowest Priority) - 3 gadgets

11. **Siteimprove Widget** - Third-party integration
    - File: `perc_site_improve_gadget.xml`
    - REST Endpoint: Need to verify
    - Complexity: **LOW**
    - Status display, link to external service
12. **Iframe Widget** - Generic iframe loader
    - File: `perc_iframe_gadget.xml`
    - REST Endpoint: N/A (custom URL)
    - Complexity: **LOW**
    - Configuration: URL, dimensions, sandbox options
13. **Widget Configuration Widget** - Dashboard management UI
    - File: `PercWidgetConfigGadget.xml`
    - REST Endpoint: `/services/gadget/` (GET/POST/DELETE)
    - Complexity: **HIGH**
    - Note: Similar to AddGadgetModal (already implemented)

---

## Implementation Strategy

### Phase 1f-1a: Content Management (Blogs, Comments, Forms, Bulk) - 4 gadgets

**Goal:** 4 gadgets handling content-related data display
**Estimated Time:** 8-12 hours
**Build Cycles:** 2-3 incremental builds

1. BlogsWidget.tsx (150 lines)
2. CommentsWidget.tsx (180 lines)
3. FormsTrackerWidget.tsx (220 lines)
4. Bulk Upload Widget (✅ already complete)
5. Tests: 4x ~15 tests each = 60 tests
6. Build & commit after each gadget pair

### Phase 1f-1b: Compliance & Analytics (Cookie, SEO, Google) - 3 gadgets

**Goal:** 3 gadgets for monitoring and integration setup
**Estimated Time:** 6-8 hours
**Build Cycles:** 1-2 incremental builds

1. CookieConsentWidget.tsx (140 lines)
2. SEOAuditWidget.tsx (240 lines)
3. GoogleSetupWidget.tsx (160 lines)
4. Tests: 3x ~15 tests each = 45 tests
5. Build & commit after each gadget

### Phase 1f-1c: System/Admin (Global, Membership, Framework) - 3 gadgets

**Goal:** 3 gadgets for system administration
**Estimated Time:** 6-8 hours
**Build Cycles:** 1-2 incremental builds

1. GlobalVariablesWidget.tsx (180 lines)
2. MembershipWidget.tsx (160 lines)
3. SitewideFrameworkWidget.tsx (190 lines)
4. Tests: 3x ~15 tests each = 45 tests
5. Build & commit after gadget trio

### Phase 1f-1d: Integration & Utility (Siteimprove, Iframe) - 2 gadgets

**Goal:** 2 gadgets for third-party integration
**Estimated Time:** 3-4 hours
**Build Cycles:** 1 incremental build

1. SiteimproveWidget.tsx (140 lines)
2. IframeWidget.tsx (160 lines)
3. Tests: 2x ~12 tests each = 24 tests
4. Single build & commit

### Phase 1f-1e: Meta Widget (Widget Configuration) - 1 gadget

**Goal:** 1 meta-widget for dashboard management
**Estimated Time:** 4-6 hours
**Note:** Similar to AddGadgetModal, may reuse components

1. WidgetConfigurationWidget.tsx (250 lines)
2. Tests: ~20 tests
3. Single build & commit

---

## Implementation Template

Each widget will follow this structure:

```typescript
// Widget.tsx
import React, { useEffect, useState } from "react";
import { get, post } from "../../api/client";

export interface WidgetProps {
  title?: string;
  refreshInterval?: number;
  maxItems?: number;
}

export const WidgetName: React.FC<WidgetProps> = ({
  title = "Default Title",
  refreshInterval = 30000,
  maxItems = 10
}) => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await get<any>("/services/endpoint/path");
        setData(response);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, refreshInterval);
    return () => clearInterval(interval);
  }, [refreshInterval]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{color: "#c33"}}>Error: {error}</div>;
  if (!data.length) return <div style={{color: "#999"}}>No data</div>;

  return (
    <div style={{padding: "16px"}}>
      <h3>{title}</h3>
      {/* Content here */}
    </div>
  );
};
```

---

## Testing Template

```typescript
// Widget.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { WidgetName } from './WidgetName';

describe('WidgetName', () => {
  const mockData = [
    { id: '1', name: 'Item 1' },
    { id: '2', name: 'Item 2' },
  ];

  it('should render loading state', () => {
    render(<WidgetName />);
    expect(screen.getByText('Loading')).toBeDefined();
  });

  it('should display data when loaded', async () => {
    // Mock API response
    render(<WidgetName />);
    await waitFor(() => {
      expect(screen.getByText('Item 1')).toBeDefined();
    });
  });

  // ... more tests
});
```

---

## Module Updates

Each implementation cycle will update:
1. `WebUI/src/main/ts/dashboard/index.ts` - Add exports
2. `registry.ts` - Register component if JSP-mounted
3. `Dashboard.tsx` - Add to AVAILABLE_GADGETS registry

---

## Build & Test Checklist

Per implementation cycle:
- [ ] All widgets compile without errors
- [ ] All new tests pass (Vitest)
- [ ] No regressions in Phase 1a-1e code
- [ ] Build time < 25 seconds
- [ ] Git commit with description

---

## Estimated Totals

**Code Metrics:**
- Total new TypeScript: ~1,900 lines (components)
- Total new Tests: ~174 test cases
- Total new CSS: ~0 (inline styles, consistent pattern)

**Build Time:** Currently ~19-20s, should remain stable

**Git Commits:** ~6-8 commits (one per implementation tier)

**Timeline:** 24-32 engineering hours (4-5 days at 6 hours/day)

---

## Success Criteria

✅ All 21 gadgets implemented and working
✅ Complete test coverage (15+ tests per major gadget)
✅ Zero build regressions
✅ Consistent code style across all widgets
✅ REST endpoints verified and working
✅ AddGadgetModal can select any of 21 gadgets
✅ useDashboardConfig persists preferences for all gadgets
✅ Dashboard gracefully handles all gadget types

---

## Notes

- REST endpoints for some gadgets are inferred; verification needed
- Some gadgets may have complex configuration; initial MVP will have defaults
- SEO and Google Setup widgets may need third-party API keys
- Widget Configuration widget may need special handling (meta-widget)
- Consider feature flags for admin-only widgets (Global Variables, Widget Config)

---

**Starting:** Phase 1f-1a (Content Management Gadgets)
**Order:** Systematic, build-validated, tier-by-tier
