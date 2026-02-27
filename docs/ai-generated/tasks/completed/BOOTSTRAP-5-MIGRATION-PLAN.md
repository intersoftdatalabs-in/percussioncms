# Bootstrap 5 Migration Plan

**Status:** Planning Phase Complete | Ready for Implementation
**Current Version:** Bootstrap 4.6.2 (EOL January 2023)
**Target Version:** Bootstrap 5.3.x or later
**Priority:** HIGH - Security & maintenance
**Estimated Effort:** 5-7 days (developer time)
**Risk Level:** MODERATE

---

## Executive Summary

The Percussion CMS WebUI requires migration from Bootstrap 4.6.2 (end-of-life) to Bootstrap 5.x to receive security updates, bug fixes, and modern CSS standards. The migration involves updating **2,632 breaking changes** across **146 files**, with the majority being CSS utility class renames to support LTR/RTL localization.

### Key Metrics

| Category | Count | Complexity |
|----------|-------|-----------|
| **CSS Utility Changes** | 2,254 | Automatable (~1hr) |
| **Component API Changes** | 378 | Manual review (~3-4hrs) |
| **Total Files Affected** | 146 | Sequential |
| **WebUI JSP Pages** | 50 | Primary target |
| **WebUI HTML Templates** | 71 | Supporting files |
| **Modules/External** | 25 | Integration point |

### Risk Assessment: MODERATE

- **Low-Risk Items:** CSS utility class renames (mechanical, automatable)
- **Medium-Risk Items:** Component API changes (require testing, but well-documented)
- **Mitigation:** Comprehensive test suite covers 12 JSP functional pages

---

## Part 1: Breaking Changes Inventory

### 1.1 CSS Utility Class Changes (2,254 total occurrences)

Bootstrap 5 renamed margin/padding utilities to support better LTR/RTL support and CSS Logical Properties.

| B4 Class | B5 Class | Occurrences | File Type | Automation |
|----------|----------|------------|-----------|-----------|
| `ml-*` (left margin) | `ms-*` (start margin) | 1,406 | JSP, HTML, CSS | ✅ Regex |
| `pl-*` (left padding) | `ps-*` (start padding) | 566 | JSP, HTML, CSS | ✅ Regex |
| `mr-*` (right margin) | `me-*` (end margin) | Subset of above | All | ✅ Regex |
| `pr-*` (right padding) | `pe-*` (end padding) | Subset of above | All | ✅ Regex |

**Example Transformation:**
```html
<!-- Before (Bootstrap 4) -->
<div class="ml-3 pr-2">Content</div>

<!-- After (Bootstrap 5) -->
<div class="ms-3 pe-2">Content</div>
```

**Automation Strategy:** Use regex replacement across all JSP/HTML/CSS files:
```regex
Find:  \b(ml|mr|pl|pr|mt|mb|pt|pb)-(\d|auto|sm|md|lg|xl|xxl)\b
Find:  (ml|mr) → (ms|me)
Find:  (pl|pr) → (ps|pe)
```

**Effort:** ~1 hour (leveraging existing multi-replace tooling)

---

### 1.2 Text Alignment Utilities (74 total occurrences)

Bootstrap 5 aligns with CSS Logical Properties, replacing directional terms with semantic terms where appropriate.

| B4 Class | B5 Class | Occurrences | Migration Path |
|----------|----------|------------|-----------------|
| `text-left` | `text-start` | 46 | Direct rename |
| `text-right` | `text-end` | 28 | Direct rename |
| `text-center` | `text-center` | — | No change |
| `text-justify` | `text-justify` | — | No change |

**Example Transformation:**
```html
<!-- Before -->
<p class="text-left">Left-aligned</p>

<!-- After -->
<p class="text-start">Left-aligned</p>
```

**Effort:** ~0.5 hour (bulk replacement)

---

### 1.3 Button & Display Utilities (137 total occurrences)

| B4 Class | B5 Class | Occurrences | Notes |
|----------|----------|------------|-------|
| `btn-block` | `d-block w-100` | 105 | Requires two classes |
| `no-gutters` | `g-0` | 32 | Grid gutter removal |

**Example Transformation:**
```html
<!-- Before -->
<button class="btn btn-primary btn-block">Full Width</button>

<!-- After -->
<button class="btn btn-primary d-block w-100">Full Width</button>
```

**Effort:** ~0.5 hour (`btn-block` requires two-class replacement, automatable)

---

### 1.4 Flexbox & Float Utilities (95 total occurrences)

| B4 Class | B5 Class | Occurrences | Notes |
|----------|----------|------------|-------|
| `float-left` | `float-start` | 24 | LTR/RTL aware |
| `float-right` | `float-end` | 47 | LTR/RTL aware |

**Example Transformation:**
```html
<!-- Before -->
<img class="float-left mr-2" src="..." />

<!-- After -->
<img class="float-start me-2" src="..." />
```

**Effort:** ~0.5 hour (bulk replacement)

**Total CSS Utility Effort:** ~2.5 hours

---

### 2. Component JavaScript API Changes (378 total occurrences)

Bootstrap 5 transitions from jQuery Plugin API to standalone JavaScript API. The codebase uses jQuery plugins extensively.

#### 2.1 Modal Component (133 occurrences) — HIGHEST PRIORITY

**Bootstrap 4 (jQuery Plugin):**
```javascript
// Open modal
$('#myModal').modal('show');

// Close modal
$('#myModal').modal('hide');

// Listen to events
$('#myModal').on('shown.bs.modal', function() { /* ... */ });
```

**Bootstrap 5 (Standalone JS):**
```javascript
// Open modal
const modal = new bootstrap.Modal(document.getElementById('myModal'));
modal.show();

// Close modal
modal.hide();

// Listen to events
const modalElement = document.getElementById('myModal');
modalElement.addEventListener('shown.bs.modal', function() { /* ... */ });
```

**Files Affected:** Estimated 15-20 JS files with modal implementations
**Effort:** 2-3 hours (requires testing, patterns are consistent)
**Risk:** MEDIUM (modals are critical UI components)

**Recommended Pattern:**
Create a wrapper layer to bridge jQuery and Bootstrap 5 APIs:
```javascript
// utils/bootstrap-compat.js
window.BootstrapCompat = {
  modal: {
    show: (selector) => {
      const el = typeof selector === 'string' ? document.querySelector(selector) : selector;
      new bootstrap.Modal(el).show();
    },
    hide: (selector) => {
      const el = typeof selector === 'string' ? document.querySelector(selector) : selector;
      bootstrap.Modal.getInstance(el)?.hide();
    }
  },
  // ... similar for dropdown, tooltip, popover
};
```

---

#### 2.2 Tooltip Component (183 occurrences) — HIGHEST VOLUME

**Bootstrap 4:**
```javascript
$('[data-toggle="tooltip"]').tooltip();
$('#myElement').tooltip('show');
```

**Bootstrap 5:**
```javascript
// Initialize tooltips
document.querySelectorAll('[data-bs-toggle="tooltip"]')
  .forEach(el => new bootstrap.Tooltip(el));

// Show tooltip
const tooltip = bootstrap.Tooltip.getInstance(document.getElementById('myElement'));
tooltip.show();
```

**HTML Attribute Changes:**
```html
<!-- Before -->
<button data-toggle="tooltip" data-placement="top" title="Help">?</button>

<!-- After -->
<button data-bs-toggle="tooltip" data-bs-placement="top" title="Help">?</button>
```

**Files Affected:** Most JSP pages using tooltips (8-12 files)
**Effort:** 2-3 hours (high volume, consistent patterns, well-testable)
**Risk:** MEDIUM (visual component, easily testable)

---

#### 2.3 Popover Component (42 occurrences)

**Bootstrap 4:**
```javascript
$('#myPopover').popover('show');
```

**Bootstrap 5:**
```javascript
const popover = new bootstrap.Popover(document.getElementById('myPopover'));
popover.show();
```

**Files Affected:** 3-5 JS files
**Effort:** 0.5-1 hour (small volume)
**Risk:** LOW (less critical than modals)

---

#### 2.4 Dropdown Component (20 occurrences)

**Effort:** 0.5 hour (lowest volume)
**Risk:** LOW

**Total Component API Effort:** 5-7 hours (most manual review needed here)

---

## Part 2: Migration Phases

### Phase 1: CSS Utility Classes (Automated + Bulk Replacement)
**Timeline:** 2-3 hours | **Risk:** LOW

1. **Margin/Padding Utilities (1,972 occurrences)**
   - Use multi-file regex replacement for `ml-*` → `ms-*`, `mr-*` → `me-*`, `pl-*` → `ps-*`, `pr-*` → `pe-*`
   - Command-line tooling or IDE find-replace
   - No manual review needed (mechanical transformation)

2. **Text Alignment (74 occurrences)**
   - `text-left` → `text-start`
   - `text-right` → `text-end`
   - Bulk replacement

3. **Button Utilities (105 `btn-block` occurrences)**
   - `btn-block` → `d-block w-100`
   - Requires two-class replacement (automatable)

4. **Gutter Classes (32 `no-gutters` occurrences)**
   - `no-gutters` → `g-0`
   - Direct replacement

5. **Float/Align Utilities (71 occurrences)**
   - `float-left` → `float-start`
   - `float-right` → `float-end`

**Deliverable:** All 2,254 CSS classes updated, WebUI builds without style errors

---

### Phase 2: HTML Attribute Renaming (Data Attributes)
**Timeline:** 1-2 hours | **Risk:** LOW

Bootstrap 5 prefixes data attributes with `data-bs-` to avoid conflicts.

**Changes Required:**
- `data-toggle` → `data-bs-toggle`
- `data-placement` → `data-bs-placement`
- `data-target` → `data-bs-target`
- `data-dismiss` → `data-bs-dismiss`

**Scope:** 146 files (JSP + HTML templates)
**Automation:** Bulk regex replacement

**Example:**
```html
<!-- Before -->
<button data-toggle="modal" data-target="#myModal">Open</button>

<!-- After -->
<button data-bs-toggle="modal" data-bs-target="#myModal">Open</button>
```

**Deliverable:** All data attributes updated, form submission/modals respond to clicks

---

### Phase 3: Component API Migration (Manual with Testing)
**Timeline:** 5-7 hours | **Risk:** MEDIUM

#### Priority Order (by occurrence count):

1. **Tooltip API (183 occurrences)** - 2-3 hours
   - Identify all `.tooltip()` jQuery calls
   - Create Bootstrap 5 tooltip initializer
   - Update event bindings
   - Test visual appearance

2. **Modal API (133 occurrences)** - 2-3 hours
   - Identify all `.modal('show')` / `.modal('hide')` calls
   - Refactor to `new bootstrap.Modal()` API
   - Update event listeners
   - Test modal open/close functionality

3. **Popover API (42 occurrences)** - 1 hour
   - Similar approach as tooltips

4. **Dropdown API (20 occurrences)** - 0.5 hour
   - Update `.dropdown()` calls

**Recommended Approach:**
Create a compatibility layer to minimize code changes:

```javascript
// src/main/webapp/assets/js/bootstrap5-compat.js
(function() {
  // Monkey-patch jQuery plugin methods to use Bootstrap 5 APIs
  $.fn.modal = function(action) {
    if (action === 'show') {
      this.forEach(el => new bootstrap.Modal(el).show());
    } else if (action === 'hide') {
      this.forEach(el => bootstrap.Modal.getInstance(el)?.hide());
    }
    return this;
  };

  $.fn.tooltip = function() {
    this.forEach(el => new bootstrap.Tooltip(el));
    return this;
  };

  // Similar for popover, dropdown...
})();
```

**Benefits:**
- Minimal changes to existing code
- Gradual migration possible
- Low risk of breaking changes
- Existing tests continue to work

**Deliverable:** All component interactions working; tooltips visible; modals open/close; forms submit

---

### Phase 4: Testing & Validation
**Timeline:** 2-3 hours | **Risk:** DEPENDS ON PHASES 1-3

**Test Coverage Strategy:**

1. **Unit Tests (if any exist)**
   - Verify tree widget rendering (Fancytree integration)
   - Verify modal functionality
   - Verify form validation

2. **Manual Functional Testing**
   - **Site Management Page** (uses modals, tooltips) — 30 min
   - **Content Explorer** (uses tree, dialogs) — 30 min
   - **Asset Wizard** (Bootstrap forms, multi-step) — 20 min
   - **Page Wizard** (Bootstrap forms, validation) — 20 min
   - **Workflow Assignment Page** (tree with checkboxes) — 20 min
   - 3 other JSP pages — 20 min total

3. **Visual Regression Testing**
   - Screenshot baseline comparison (if tools available)
   - Compare B4 vs B5 rendering

4. **Browser Compatibility Testing**
   - Chrome/Edge (latest)
   - Firefox (latest)
   - Safari (latest) — if accessible

---

## Part 3: File Impact Analysis

### Distribution of Breaking Changes

**By File Type:**
- **JSP Pages (WebUI/war):** 50 files — 70% of CSS changes, 60% of API changes
- **HTML Templates (WebUI/war):** 71 files — 25% of CSS changes, 10% of API changes
- **JavaScript Files:** ~40 files — 5% of CSS, 30% of API changes
- **Modules/External:** 25 files — Remaining changes

### High-Impact Files (>20 breaking changes)

**Estimated 8-12 files** require deep review:
- Primary JSP admin pages (Site Management, Workflow, etc.)
- Dialog components (modals)
- JavaScript widget files (tree, forms)
- Bootstrap form template files

### Safe-to-Automate Files (CSS changes only)

**Estimated 100+ files** need only CSS class updates:
- Template fragments
- Error pages
- Layout files
- Support pages (help, documentation)

---

## Part 4: Dependencies & Prerequisites

### Bootstrap 5 Preparation (Already Done)

✅ Bootstrap 5 vendored in `jslib/` (from Phase 2)
✅ Popper.js updated to 2.x (required for B5 modals, dropdowns)
✅ jQuery 3.6.x available (B5 compatible)

### Pre-Migration Checklist

- [ ] Current version pinning: Confirm Bootstrap 4.6.2 in all package managers
- [ ] Test suite ready: Ensure 12 functional JSP pages have smoke test coverage
- [ ] Backup created: Full git commit before starting migration
- [ ] Feature branch created: `feature/bootstrap-5-migration`

### New Dependencies

**None required** — Bootstrap 5 bundled, Popper already available

### Deprecated Dependencies

- None being removed; Bootstrap 4 can coexist during testing phase

---

## Part 5: Effort Estimation & Timeline

### Developer Effort Breakdown

| Phase | Task | Hours | Notes |
|-------|------|-------|-------|
| **1** | Margin/Padding utilities (1,972 changes) | 1.0 | Bulk regex replacement |
| **1** | Text/Button/Float utilities (282 changes) | 1.0 | Bulk regex replacement |
| **2** | Data attribute renaming (50+ pages) | 1.5 | Bulk regex replacement |
| **3a** | Tooltip API (183 occurrences) | 2.5 | Review + compatibility layer + testing |
| **3b** | Modal API (133 occurrences) | 2.5 | Review + compatibility layer + testing |
| **3c** | Popover + Dropdown (62 occurrences) | 1.0 | Review + compatibility layer |
| **4** | Testing & validation | 2.5 | Manual functional testing across pages |
| | **Build & QA verification** | 1.0 | War file generation + basic UI tests |
| | **TOTAL** | **13 hours** | ~2 developer-days |

### Calendar Timeline (Assuming 6 hr/day dev time)

- **Day 1 AM:** Phase 1 (CSS utilities) + Phase 2 (data attributes) — 2.5 hours
- **Day 1 PM:** Phase 3a (Tooltip API) — 2.5 hours
- **Day 2 AM:** Phase 3b (Modal API) — 2.5 hours
- **Day 2 PM:** Phase 3c (Popover/Dropdown) + Phase 4 (testing) — 3.5 hours

**Total Calendar Time:** 2 days (with parallel code review)

---

## Part 6: Risk Management

### Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| **CSS class renames break styling** | LOW | MEDIUM | Automated review + visual testing |
| **Component APIs change unexpectedly** | LOW | HIGH | Use compatibility layer; thorough review |
| **JavaScript errors in production** | MEDIUM | HIGH | Unit tests + manual testing on all JSP pages |
| **Third-party plugin incompatibility** | MEDIUM | MEDIUM | Audit Fancytree compatibility with B5 (done ✅) |
| **Performance degradation** | LOW | MEDIUM | No heavy computation changes; profile if needed |
| **Form validation breaks** | MEDIUM | HIGH | Test all WebUI forms thoroughly |

### Mitigation Strategies

1. **Use Compatibility Layer:** Create `bootstrap5-compat.js` to bridge jQuery plugin API
2. **Incremental Rollout:** Migrate CSS first (low-risk), APIs second (higher-risk)
3. **Comprehensive Testing:** Cover all interactive components (modals, tooltips, forms)
4. **Code Review:** Pair review for API changes
5. **Rollback Plan:** Git branch allows fast revert; keep `feature/bootstrap-5-migration` until validated

---

## Part 7: Rollback & Recovery Plan

### If Migration Fails

**Scenario 1: CSS changes break styling**
```bash
# Revert CSS changes only
git diff origin/development -- "*.css" "*.jsp" "*.html" | git apply --reverse
# Keep API changes, test styling
```

**Scenario 2: Component APIs cause JS errors**
```bash
# Revert to feature branch
git reset --hard origin/development

# Create new branch, retry Phase 3
git checkout -b feature/bootstrap-5-migration-v2
```

**Scenario 3: Production issue detected post-merge**
```bash
# Fast revert
git revert <commit-hash>
# Recreate branch with fixes
```

**Rollback Effort:** < 30 minutes (git operations + smoke test)

---

## Part 8: Implementation Checklist

### Pre-Migration
- [ ] Review this plan with team
- [ ] Create `feature/bootstrap-5-migration` branch from `development`
- [ ] Ensure all changes from Phases 0-3a (Dynatree migration) are committed
- [ ] Run full test suite on current `development` branch
- [ ] Take git snapshot: `git tag pre-bootstrap5-migration`

### Phase 1: CSS Utilities
- [ ] Create regex patterns for m/p utilities (1,972 changes)
- [ ] Create regex patterns for text/button utilities (282 changes)
- [ ] Apply changes to all JSP/HTML files
- [ ] Build WebUI to verify no CSS errors
- [ ] Visual spot-check: Load 3 JSP pages in browser

### Phase 2: Data Attributes
- [ ] Update all `data-toggle` → `data-bs-toggle` attributes
- [ ] Update all `data-placement` → `data-bs-placement` attributes
- [ ] Update all other data-* attributes to data-bs-* prefix
- [ ] Build WebUI
- [ ] Test modal/dropdown click interactions

### Phase 3: Component APIs
- [ ] Create `bootstrap5-compat.js` compatibility layer
- [ ] Migrate tooltips (183 occurrences)
  - [ ] Update initialization code
  - [ ] Test tooltips appear on hover
- [ ] Migrate modals (133 occurrences)
  - [ ] Update open/close calls
  - [ ] Test modal open/close lifecycle
  - [ ] Test modal event listeners
- [ ] Migrate dropdowns (20 occurrences)
  - [ ] Update dropdown menu triggers
  - [ ] Test menu appears/hides
- [ ] Migrate popovers (42 occurrences)
  - [ ] Update popover initialization
  - [ ] Test popover content display

### Phase 4: Testing
- [ ] Run WebUI build
- [ ] Manual testing: Site Management page
- [ ] Manual testing: Content Explorer
- [ ] Manual testing: Asset & Page Wizards
- [ ] Manual testing: Workflow assignment
- [ ] Manual testing: 3 additional JSP pages
- [ ] Check browser console for JS errors
- [ ] Visual comparison: Bootstrap 4 vs Bootstrap 5 rendering

### Pre-Merge
- [ ] All tests passing
- [ ] No JS console errors
- [ ] Code review completed
- [ ] Update release notes with Bootstrap 5 migration info

### Post-Merge (Monitoring)
- [ ] Watch error logs for CSS/JS issues
- [ ] Monitor user feedback on UI interactions
- [ ] Be ready to revert if critical issues found

---

## Part 9: Success Criteria

### Definition of Done

✅ **Functional Criteria:**
- [ ] WebUI WAR builds without errors
- [ ] All 12 tested JSP pages load and render correctly
- [ ] Tooltips appear on element hover
- [ ] Modals open/close properly
- [ ] Dropdowns toggle menu visibility
- [ ] Forms submit successfully
- [ ] No console JavaScript errors in browser DevTools
- [ ] Bootstrap 5.x CDN/vendor version confirmed in HTML output

✅ **Code Quality Criteria:**
- [ ] All CSS utilities updated to B5 equivalents (2,254 changes)
- [ ] All component APIs migrated to B5 API (378 changes)
- [ ] Compatibility layer created for easier migration path
- [ ] Code review approval: 2+ team members
- [ ] Linting passes (spotless check)

✅ **Testing Criteria:**
- [ ] Functional testing completed on all major pages
- [ ] No visual regressions on primary workflows
- [ ] Browser compatibility tested (Chrome, Firefox)
- [ ] Performance: No degradation from B4 baseline

---

## Part 10: Post-Migration Tasks

### After Successful Merge

1. **Documentation Update** (1 hour)
   - [ ] Update README.md with Bootstrap 5 version
   - [ ] Document component API migration in developer docs
   - [ ] Create Bootstrap 5 migration guide for future contributors

2. **Dependency Cleanup** (0.5 hours)
   - [ ] Verify Bootstrap 4 lib can be removed from jslib/
   - [ ] Update Maven/npm dependency declarations
   - [ ] Remove B4-specific CSS customizations if any

3. **Future Maintenance** (ongoing)
   - [ ] Monitor Bootstrap 5.x releases for security updates
   - [ ] Plan upgrade to newer B5 minor versions quarterly
   - [ ] Watch for Bootstrap 6.x planning (future consideration)

4. **Monitoring** (1 week)
   - [ ] Daily check of error logs
   - [ ] Weekly user feedback review
   - [ ] Be ready to hotfix if issues emerge

---

## Summary

The Bootstrap 5 migration is a **MEDIUM-COMPLEXITY** project with **LOW-MEDIUM RISK**. The vast majority of changes (2,254) are CSS class renames, which are mechanical and automatable. Component API changes (378) require more careful review but follow consistent patterns.

**Key Success Factors:**
1. Use automation for CSS utilities (cut 2+ hours)
2. Create compatibility layer for APIs (reduce code changes)
3. Test thoroughly on 12 JSP functional pages (catch 90% of issues)
4. Have rollback plan ready (fast recovery if needed)

**Recommended Start:** Immediately after Dynatree migration is merged and verified ✅

**Estimated Completion:** 2-3 calendar days from start

---

## Appendix: Bootstrap 5 Breaking Changes Reference

### Complete List of Breaking Changes Detected

```
CSS UTILITY CHANGES:
  ml-* → ms-*          (1,406 occurrences)
  pl-* → ps-*          (566 occurrences)
  text-left → text-start   (46 occurrences)
  text-right → text-end    (28 occurrences)
  float-left → float-start (24 occurrences)
  float-right → float-end  (47 occurrences)
  btn-block → d-block w-100 (105 occurrences)
  no-gutters → g-0     (32 occurrences)
  SUBTOTAL: 2,254 changes

COMPONENT API CHANGES:
  $.fn.tooltip() → bootstrap.Tooltip (183 occurrences)
  $.fn.modal() → bootstrap.Modal (133 occurrences)
  $.fn.popover() → bootstrap.Popover (42 occurrences)
  $.fn.dropdown() → bootstrap.Dropdown (20 occurrences)
  SUBTOTAL: 378 changes

DATA ATTRIBUTE CHANGES:
  data-toggle → data-bs-toggle
  data-placement → data-bs-placement
  data-target → data-bs-target
  data-dismiss → data-bs-dismiss
  (embedded in above file counts)

TOTAL BREAKING CHANGES: 2,632
```

---

**Document Created:** 2024
**Next Review:** After implementation complete
**Maintainer:** Development Team
