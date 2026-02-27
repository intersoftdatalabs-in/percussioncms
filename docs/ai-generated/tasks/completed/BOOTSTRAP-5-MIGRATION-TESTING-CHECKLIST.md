# Bootstrap 5 Migration - Phase 4 Testing Checklist

**Issue:** #639
**Branch:** feature/639-bootstrap-5-migration
**Status:** Ready for Functional Testing
**Test Date:** 2024-02-26

---

## Part 1: Pre-Testing Setup

### Environment Verification
- [ ] Java 21 configured (`JAVA_HOME` points to JDK 21)
- [ ] Maven available and working (`mvn -v` succeeds)
- [ ] Git branch correct: `feature/639-bootstrap-5-migration`
- [ ] WebUI WAR file built: `WebUI/target/perc-web-ui-8.2.0-SNAPSHOT.war` exists (343MB)
- [ ] Bootstrap 5 vendored in `/cm/jslib/` (from Phase 2)

### Browser Preparation
- [ ] Chrome/Chromium latest version (primary browser)
- [ ] Firefox latest version (secondary browser)
- [ ] Developer Tools console visible for error checking
- [ ] Network tab ready to monitor requests

### Test Environment
- [ ] Fresh deployment with WAR file built from feature branch
- [ ] Database populated with test data (existing test data OK)
- [ ] Logs cleared before starting tests
- [ ] No other developers accessing test instance

---

## Part 2: CSS & Styling Verification

### CSS Utility Classes
**Objective:** Verify CSS class migrations display correctly

- [ ] **Margin/Padding Utilities**
  - [ ] Elements with `ms-` (start margin) display correctly
  - [ ] Elements with `me-` (end margin) display correctly
  - [ ] Elements with `ps-` (start padding) display correctly
  - [ ] Elements with `pe-` (end padding) display correctly
  - [ ] No visual misalignment in layouts
  - [ ] Responsive spacing works on mobile view

- [ ] **Text Alignment**
  - [ ] `text-start` elements align left (LTR) ✓
  - [ ] `text-end` elements align right (LTR) ✓
  - [ ] `text-center` elements centered ✓
  - [ ] Alignment consistent across pages

- [ ] **Button Utilities**
  - [ ] `d-block w-100` buttons display full width ✓
  - [ ] Button sizes all correct ✓
  - [ ] Button colors apply correctly ✓
  - [ ] Active/disabled states show properly ✓

- [ ] **Float Utilities**
  - [ ] `float-start` elements float left (LTR) ✓
  - [ ] `float-end` elements float right (LTR) ✓
  - [ ] Text wrapping around floated elements works ✓

- [ ] **Grid System**
  - [ ] Grid columns display (col-md-4, col-sm-6, etc.) ✓
  - [ ] Responsive breakpoints work ✓
  - [ ] Gutter spacing correct (converted no-gutters → g-0) ✓
  - [ ] Mobile view (< 768px) responsive ✓
  - [ ] Tablet view (768px - 1024px) responsive ✓
  - [ ] Desktop view (> 1024px) responsive ✓

### Visual Regression Check
- [ ] Page header displays with correct styling
- [ ] Navigation bar properly styled and positioned
- [ ] Sidebar layout preserved and styled
- [ ] Cards/boxes have proper spacing and borders
- [ ] Form fields properly spaced and aligned
- [ ] Tables display with correct styling
- [ ] Modal dialogs appear without layout issues
- [ ] Alerts/notifications styled correctly

---

## Part 3: Component API Testing

### Modal Components
**Objective:** Verify modal open/close lifecycle works

**Test Page:** Site Management or similar page with modals

- [ ] **Modal Initialization**
  - [ ] Page loads without JavaScript console errors
  - [ ] Bootstrap 5 compatibility layer loads (check console for "Bootstrap 5 Compatibility Layer loaded")
  - [ ] No "undefined" errors in console
  - [ ] jQuery modal API available: `$.fn.modal` exists

- [ ] **Modal Opening (Test 1: Click Button)**
  - [ ] Click "Add Item" or similar modal-triggering button
  - [ ] Modal dialog appears on screen ✓
  - [ ] Modal is centered on page ✓
  - [ ] Modal backdrop (overlay) appears ✓
  - [ ] Page behind modal is dimmed/disabled ✓
  - [ ] Console: No errors ✓

- [ ] **Modal Content**
  - [ ] Modal title displays correctly
  - [ ] Modal body content renders properly
  - [ ] Form fields in modal are editable
  - [ ] Buttons in modal footer visible and clickable

- [ ] **Modal Closing (Test 1: Cancel Button)**
  - [ ] Click "Cancel" button in modal footer
  - [ ] Modal closes/disappears ✓
  - [ ] Backdrop disappears ✓
  - [ ] Page is re-enabled ✓
  - [ ] Focus returns to page ✓
  - [ ] Console: No errors ✓

- [ ] **Modal Closing (Test 2: X Button)**
  - [ ] Click X button in modal header
  - [ ] Modal closes smoothly ✓
  - [ ] No console errors ✓

- [ ] **Modal Closing (Test 3: Backdrop Click)**
  - [ ] Click outside modal (on backdrop)
  - [ ] Modal closes (if backdrop-dismissible in B5) ✓
  - [ ] Console: No errors ✓

- [ ] **Modal with Form Submission**
  - [ ] Fill in modal form
  - [ ] Click Submit/Save button
  - [ ] Form submits successfully
  - [ ] Modal closes after submission
  - [ ] Data saved correctly
  - [ ] Success message appears (if applicable)

### Tooltip Components
**Objective:** Verify tooltip initialization and display

**Test Pages:** Any page with help icons or tooltips

- [ ] **Tooltip Initialization**
  - [ ] jQuery tooltip API available: `$.fn.tooltip` exists
  - [ ] No initialization errors in console
  - [ ] data-bs-toggle="tooltip" attributes recognized

- [ ] **Tooltip Display (Test 1: Hover)**
  - [ ] Hover over element with tooltip (help icon, etc.)
  - [ ] Tooltip appears after short delay (~500ms) ✓
  - [ ] Tooltip displays in correct position ✓
  - [ ] Tooltip text is readable ✓
  - [ ] Tooltip styling is Bootstrap 5 (dark background, white text typical) ✓

- [ ] **Tooltip Content Variations**
  - [ ] Tooltip with plain text works ✓
  - [ ] Tooltip with HTML content renders (if applicable) ✓
  - [ ] Multiple tooltips on page work independently ✓

- [ ] **Tooltip Positioning**
  - [ ] Tooltip appears above element (if data-bs-placement="top") ✓
  - [ ] Tooltip appears below element (if data-bs-placement="bottom") ✓
  - [ ] Tooltip appears left/right as specified ✓
  - [ ] Tooltip repositions if near edge of viewport ✓

- [ ] **Tooltip Dismissal**
  - [ ] Move mouse away from element
  - [ ] Tooltip disappears ✓
  - [ ] No console errors ✓

### Dropdown Components
**Objective:** Verify dropdown menus trigger and display

**Test Pages:** Pages with dropdown menus (user menu, action menus, etc.)

- [ ] **Dropdown Initialization**
  - [ ] jQuery dropdown API available: `$.fn.dropdown` exists
  - [ ] No initialization errors
  - [ ] data-bs-toggle="dropdown" attributes recognized

- [ ] **Dropdown Triggering**
  - [ ] Click dropdown trigger button
  - [ ] Dropdown menu appears ✓
  - [ ] Menu items visible ✓
  - [ ] Menu positioning correct (below button) ✓

- [ ] **Dropdown Menu Interaction**
  - [ ] Click menu item
  - [ ] Menu closes ✓
  - [ ] Action triggers correctly (navigation, form change, etc.) ✓
  - [ ] No console errors ✓

- [ ] **Dropdown Dismissal**
  - [ ] Click outside menu to close
  - [ ] Menu closes ✓
  - [ ] Page returns to normal state ✓

### Popover Components
**Objective:** Verify popovers work if used

**Test Pages:** Any pages with popover components

- [ ] **Popover Display**
  - [ ] Click/hover element with popover
  - [ ] Popover appears with content ✓
  - [ ] Popover positioned correctly ✓

- [ ] **Popover Dismissal**
  - [ ] Click outside popover to dismiss
  - [ ] Popover closes ✓

### Carousel/Slider Components
**Objective:** Verify carousel if used in WebUI

- [ ] **Auto-cycle** (if applicable)
  - [ ] Carousel auto-advances slides ✓

- [ ] **Manual Navigation**
  - [ ] Previous/Next buttons work ✓
  - [ ] Indicators/dots work ✓

---

## Part 4: Functional Testing of Key Pages

### Test Page 1: Site Management Page
**URL:** `/Rhythmyx/cm/sitearchitecture` (or similar)
**Expected:** Site tree, modals for creating/editing sites

Checklist:
- [ ] Page loads without CSS errors (layout correct)
- [ ] Tree displays with proper styling
- [ ] "Add Site" button visible and styled correctly
- [ ] Click "Add Site" - modal appears with form
- [ ] Modal form has proper styling and spacing
- [ ] Fill form and submit
- [ ] Modal closes after submit
- [ ] New site appears in tree
- [ ] Console: No JavaScript errors
- [ ] Console: "Bootstrap 5 Compatibility Layer loaded" message present

**CSS Checks:**
- [ ] No broken button styling (`btn-block` converted to `d-block w-100`)
- [ ] Modal backdrop displays correctly
- [ ] Form labels properly aligned (`text-start` used if applicable)
- [ ] Spacing consistent with Bootstrap 5 utility classes

**Component Checks:**
- [ ] Modal API working (open/close cycles)
- [ ] Form submission functional
- [ ] Tree interactions functional

---

### Test Page 2: Content Explorer
**URL:** `/Rhythmyx/cm/contentuiexplorer` (or similar)
**Expected:** Content tree, folder/item operations

Checklist:
- [ ] Page loads with correct styling
- [ ] Tree displays (should use Fancytree from Phase 3b of Dynatree migration)
- [ ] Folder expand/collapse works
- [ ] Content items display
- [ ] Context menu or action buttons work
- [ ] Modal dialogs for operations open/close correctly
- [ ] No layout issues or broken CSS

**CSS Checks:**
- [ ] Tree styling consistent
- [ ] Menu buttons properly spaced
- [ ] Modal styling correct

---

### Test Page 3: Asset Wizard
**URL:** `/Rhythmyx/cm/assetwizard` (or similar)
**Expected:** Multi-step form wizard with Bootstrap styling

Checklist:
- [ ] Wizard loads with correct styling
- [ ] Step indicators display (Bootstrap progress bar or similar)
- [ ] Form fields properly spaced and aligned
- [ ] Next/Previous buttons visible and functional
- [ ] Tooltips on form fields work (hover shows help text)
- [ ] Wizard completes successfully
- [ ] Final modal/confirmation displays correctly

**CSS Checks:**
- [ ] Form layout using Bootstrap grid (col-md-6, etc.)
- [ ] Button styling consistent
- [ ] Alert messages properly styled
- [ ] No strange spacing due to utility class conversion

---

### Test Page 4: Page Wizard
**URL:** `/Rhythmyx/cm/pagewizard` (or similar)
**Expected:** Page creation wizard with templates

Checklist:
- [ ] Wizard loads without CSS/JS errors
- [ ] Template selection displays properly
- [ ] Form fields work
- [ ] Buttons styled and functional
- [ ] Multi-step navigation works
- [ ] Console: No errors
- [ ] Modal background/styling correct

---

### Test Page 5: Workflow Assignment
**URL:** Workflow assignment page (varies)
**Expected:** Tree with checkboxes, multi-select

Checklist:
- [ ] Tree with checkboxes displays
- [ ] Checkboxes functional (click to select)
- [ ] Parent/child selection works correctly
- [ ] Modal buttons styled correctly
- [ ] Form submits successfully
- [ ] No JavaScript errors in console

---

### Test Page 6-12: Additional JSP Pages (Choose 7 more key pages)
For each page, verify:
- [ ] Loads without CSS/styling errors
- [ ] All buttons styled consistently
- [ ] All modal dialogs work (if applicable)
- [ ] Form fields properly spaced
- [ ] No broken layout
- [ ] No console errors
- [ ] Responsive design working

---

## Part 5: Browser Compatibility Testing

### Chrome/Chromium (Primary)
- [ ] All tests from Part 2-4 pass
- [ ] No console errors or warnings
- [ ] Performance acceptable (page load < 3 seconds)
- [ ] Responsive design works (test at 320px, 768px, 1024px widths)

### Firefox (Secondary)
- [ ] All core functionality works
- [ ] Modals open/close properly
- [ ] Tooltips display (may have slight rendering differences)
- [ ] No console errors
- [ ] Performance acceptable

### Safari (Optional - if available)
- [ ] Core functionality works
- [ ] No major visual differences from Chrome

---

## Part 6: JavaScript Error Detection

### Console Check (All Browsers)

**CRITICAL - MUST RESOLVE:**
- [ ] No `Uncaught TypeError` errors
- [ ] No `Uncaught ReferenceError` errors
- [ ] No `bootstrap is undefined` errors
- [ ] No `jQuery is undefined` errors

**WARNINGS - Review (May be OK):**
- [ ] Any deprecation warnings (log what they are)
- [ ] Any failed resource loads (log which resources)

### Network Tab Check
- [ ] All JavaScript files load successfully
- [ ] `bootstrap5-compat.js` loads (check for file in Network tab)
- [ ] No 404 errors for CSS/JS resources
- [ ] Bootstrap CSS loads correctly

---

## Part 7: Data Integrity Testing

### Database Consistency
- [ ] Create new site through wizard - data saves correctly
- [ ] Edit existing item - changes persist after reload
- [ ] Delete item - removal confirmed and persisted
- [ ] No data corruption in database

### State Management
- [ ] Modal state doesn't persist incorrectly on navigation
- [ ] Tree expansion state preserved correctly
- [ ] Form field values clear between operations
- [ ] No ghost data appearing

---

## Part 8: Regression Testing

### Dynatree → Fancytree Migration (Verify Not Broken)
- [ ] Tree displays all items correctly
- [ ] Tree expand/collapse functions
- [ ] Icons display (folder, file, etc.)
- [ ] Drag-and-drop works (if applicable)
- [ ] No console errors related to tree operations

### Previous Bootstrap 4 Features (Verify Still Work)
- [ ] Responsive grid layouts work at all breakpoints
- [ ] Button states (active, disabled, hover) display correctly
- [ ] Form validation styling displays (Bootstrap form feedback)
- [ ] Tables format correctly
- [ ] Pagination controls work (if applicable)

---

## Part 9: Performance Testing

### Page Load Times
- [ ] Home/dashboard: < 3 seconds
- [ ] Content Explorer: < 4 seconds
- [ ] Wizards: < 3 seconds
- [ ] Modal open: < 300ms
- [ ] Tree interaction (expand): < 100ms

### Memory & Resource Usage
- [ ] Page memory footprint reasonable (no memory leaks)
- [ ] JavaScript file sizes acceptable
- [ ] CSS load times reasonable
- [ ] No excessive re-renders or flickering

---

## Part 10: Issue Tracking & Resolution

### Issues Found During Testing

**Issue #1:**
- **Description:**
- **Page:**
- **Severity:** [ ] Critical [ ] High [ ] Medium [ ] Low
- **Steps to Reproduce:**
- **Expected Behavior:**
- **Actual Behavior:**
- **Resolution:**

**Issue #2:**
- **Description:**
- **Page:**
- **Severity:** [ ] Critical [ ] High [ ] Medium [ ] Low
- **Steps to Reproduce:**
- **Expected Behavior:**
- **Actual Behavior:**
- **Resolution:**

---

## Part 11: Sign-Off

### Testing Complete Verification
- [ ] All "CRITICAL" tests passed
- [ ] All "High" severity issues resolved or logged
- [ ] All 12 key pages tested and functional
- [ ] CSS styling consistent across pages
- [ ] No unplanned API behavior
- [ ] Documentation updated (if needed)

### Ready for Code Review
- [ ] All testing complete
- [ ] Issues documented and tracked
- [ ] Branch pushed to origin: `feature/639-bootstrap-5-migration`
- [ ] Code review requested from team lead

### Ready for Merge
- [ ] Code review approved
- [ ] All PR comments addressed
- [ ] Testing signed off by QA/team
- [ ] Final build successful
- [ ] Changelog/release notes updated

---

## Test Execution Notes

### Tester Name:
### Test Date:
### Start Time:
### End Time:
### Total Duration:

### Notes & Observations:


### Screenshots (Recommended):
- [ ] Home page (before/after comparison if possible)
- [ ] Modal dialog open
- [ ] Tooltip displayed
- [ ] Mobile responsive view
- [ ] Any unusual styling (document for review)

---

## Approval Sign-Off

**QA Tester:** _________________ **Date:** ________
**Code Reviewer:** _________________ **Date:** ________
**Technical Lead:** _________________ **Date:** ________

---

**Next Steps After Testing:**
1. Address any critical issues found
2. Commit fixes to feature branch
3. Re-test critical paths
4. Request final code review
5. Merge to development branch
6. Create release notes documenting Bootstrap 5 upgrade
7. Plan post-merge monitoring (watch logs for issues)
