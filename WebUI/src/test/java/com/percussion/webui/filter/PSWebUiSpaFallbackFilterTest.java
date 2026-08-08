/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.webui.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit tests for PR-9 SPA path fallback path → spa.jsp forward mapping. */
public class PSWebUiSpaFallbackFilterTest {

  @Test
  public void forwardsHomeAndSection() {
    assertEquals(
        "/cm/app/spa.jsp?entry=home&section=library",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home/library", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=home",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=home&section=gadgets",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home/gadgets/", null));
  }

  @Test
  public void forwardsPublishWorkflowAdminWithQueryPreserved() {
    assertEquals(
        "/cm/app/spa.jsp?entry=publish&section=logs&siteId=42",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/publish/logs", "siteId=42"));
    assertEquals(
        "/cm/app/spa.jsp?entry=workflow&tab=users",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/workflow/users", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=admin&tab=tools",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/admin/tools", null));
  }

  @Test
  public void forwardsExplorerAndWidgetBuilder() {
    assertEquals(
        "/cm/app/spa.jsp?entry=explorer&path=%2FSites%2Ffoo",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/explorer", "path=%2FSites%2Ffoo"));
    assertEquals(
        "/cm/app/spa.jsp?entry=widget-builder",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/widget-builder", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=widget-builder",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/widgetbuilder", null));
  }

  @Test
  public void forwardsProfileEntry() {
    assertEquals(
        "/cm/app/spa.jsp?entry=profile",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/profile", null));
    assertEquals(
        "/cm/pages/app/spa.jsp?entry=profile",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/pages/app/profile", null));
  }

  @Test
  public void forwardsDeveloperWithSection() {
    assertEquals(
        "/cm/app/spa.jsp?entry=developer",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/developer", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=developer&section=content-types",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/developer/content-types", null));
  }

  @Test
  public void dualTreePagesAppSupported() {
    assertEquals(
        "/cm/pages/app/spa.jsp?entry=home&section=recent",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/pages/app/home/recent", null));
  }

  @Test
  public void passesThroughRealJspsAndStaticAndNonSpa() {
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/spa.jsp", "entry=home"));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/index.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/dashboard.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/webmgt.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/assetPickerModern.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/js/legacy/foo.js", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/css/styles.css", null));
    assertNull(
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/modern/assets/perc-modern-ui.js", null));
    // Unknown first segment (legacy page name without .jsp would be odd — still pass through)
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/dialogs/foo", null));
  }

  @Test
  public void rejectsTraversalSegments() {
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home/../admin", null));
    // URL-encoded dots (defense-in-depth when URI not fully decoded)
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home/%2e%2e/admin", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/home/%2E%2E/admin", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/%2e%2e", null));
    assertTrue(PSWebUiSpaFallbackFilter.isUnsafePathSegment(".."));
    assertTrue(PSWebUiSpaFallbackFilter.isUnsafePathSegment("%2e%2e"));
    assertTrue(PSWebUiSpaFallbackFilter.isUnsafePathSegment("%252e%252e"));
    assertTrue(PSWebUiSpaFallbackFilter.isUnsafePathSegment("a%2fb"));
    assertTrue(!PSWebUiSpaFallbackFilter.isUnsafePathSegment("library"));
    assertTrue(!PSWebUiSpaFallbackFilter.isUnsafePathSegment("widget-builder"));
  }

  @Test
  public void dropsDuplicateEntryFromQuery() {
    String forward =
        PSWebUiSpaFallbackFilter.buildSpaForwardPath(
            "/cm/app/home", "entry=publish&section=library");
    assertTrue(forward.startsWith("/cm/app/spa.jsp?entry=home"));
    assertTrue(forward.contains("section=library"));
    // Only one entry= and it is home (from path)
    assertEquals(1, forward.split("entry=", -1).length - 1);
  }
}
