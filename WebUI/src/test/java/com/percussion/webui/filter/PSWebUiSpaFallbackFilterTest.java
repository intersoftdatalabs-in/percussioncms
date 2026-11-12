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
import static org.junit.Assert.assertFalse;
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
  public void forwardsEditorEntry() {
    assertEquals(
        "/cm/app/spa.jsp?entry=editor&contentId=42&mode=edit",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath(
            "/cm/app/editor", "contentId=42&mode=edit"));
  }

  @Test
  public void forwardsAssemblyEntry() {
    assertEquals(
        "/cm/app/spa.jsp?entry=assembly&contentId=42&templateId=7",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath(
            "/cm/app/assembly", "contentId=42&templateId=7"));
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
  public void forwardsDesignWithSection() {
    assertEquals(
        "/cm/app/spa.jsp?entry=design",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/design", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=design&section=templates",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/design/templates", null));
  }

  @Test
  public void forwardsArchitectureWithOptionalSite() {
    assertEquals(
        "/cm/app/spa.jsp?entry=architecture",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/architecture", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=architecture&site=Demo",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/architecture/Demo", null));
    // Legacy first segment alias
    assertEquals(
        "/cm/app/spa.jsp?entry=architecture",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/arch", null));
    assertEquals(
        "/cm/app/spa.jsp?entry=architecture",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/navigation", null));
  }

  @Test
  public void dualTreePagesAppSupported() {
    assertEquals(
        "/cm/pages/app/spa.jsp?entry=home&section=recent",
        PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/pages/app/home/recent", null));
  }

  @Test
  public void retiredSiteArchitectureJspRedirectsToArchView() {
    assertEquals(
        "/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/siteArchitecture.jsp", null));
    assertEquals(
        "/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/pages/app/siteArchitecture.jsp", ""));
    assertEquals(
        "/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/Rhythmyx/cm/app/siteArchitecture.jsp", null));
    assertEquals(
        "/cm/app/?view=arch&site=Site1",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/siteArchitecture.jsp", "site=Site1"));
    assertEquals(
        "/cm/app/?view=arch&site=Site1",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/SiteArchitecture.jsp", "view=arch&site=Site1"));
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation("/cm/app/admin.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation("/cm/app/home", null));
    assertNull(PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(null, "site=1"));
  }

  @Test
  public void retiredEditAssetJspRedirectsToEditorView() {
    assertEquals(
        "/cm/app/?view=editor",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/editAsset.jsp", null));
    assertEquals(
        "/cm/app/?view=editor",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/pages/app/editAsset.jsp", ""));
    assertEquals(
        "/cm/app/?view=editor&id=42",
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/editAsset.jsp", "id=42"));
    assertEquals(
        "/Rhythmyx/cm/app/?view=editor",
        PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx", "/cm/app/?view=editor"));
  }

  @Test
  public void retiredWorkflowAdminJspsRedirectToAdminView() {
    // #6095 FR-002: all five legacy hosts (app + pages trees) land on SPA Admin.
    String[] hosts = {
      "adminWorkflow.jsp", "percWorkflow.jsp", "percRoles.jsp", "percUsers.jsp", "percCategories.jsp"
    };
    for (String host : hosts) {
      assertEquals(
          "/cm/app/?view=admin",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation("/cm/app/" + host, null));
      assertEquals(
          "/cm/app/?view=admin",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/cm/pages/app/" + host, ""));
      assertEquals(
          "/cm/app/?view=admin",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/Rhythmyx/cm/app/" + host, null));
      assertTrue(
          PSWebUiSpaFallbackFilter.isRetiredWorkflowAdminJsp(("/cm/app/" + host).toLowerCase()));
      assertTrue(
          PSWebUiSpaFallbackFilter.isRetiredWorkflowAdminJsp(
              ("/cm/pages/app/" + host).toLowerCase()));
    }
    // Untouched hosts still pass through; near-miss names are not false positives.
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation("/cm/app/admin.jsp", null));
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/cm/app/adminWorkflow.jspx", null));
    assertFalse(PSWebUiSpaFallbackFilter.isRetiredWorkflowAdminJsp("/cm/app/admin.jsp"));
    assertFalse(PSWebUiSpaFallbackFilter.isRetiredWorkflowAdminJsp(null));
  }

  @Test
  public void retiredSchedulerAdminJspsRedirectToAdminView() {
    // #6140 FR-002: deleted ui/admin scheduler pages (+tools ConsistencyChecker trio)
    // land on SPA Admin in both .jsp and .faces spellings; kept pages pass through.
    // #6160: TaskDetailLog joins the retired set (TaskLogs per-row detail affordance).
    String[] pages = {
      "ScheduledTask.jsp",
      "ScheduledTaskList.jsp",
      "RemoveConfirmation.jsp",
      "TaskLogs.jsp",
      "TaskDetailLog.jsp",
      "DeleteAllTaskLogWarning.jsp",
      "NoTaskLogSelectionWarning.jsp",
      "TaskNotificationEditor.jsp",
      "TaskNotificationList.jsp"
    };
    for (String page : pages) {
      assertEquals(
          "/cm/app/?view=admin",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/ui/admin/" + page, null));
      assertEquals(
          "/cm/app/?view=admin&x=1",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/Rhythmyx" + "/ui/admin/" + page.replace(".jsp", ".faces"), "x=1"));
      assertTrue(
          PSWebUiSpaFallbackFilter.isRetiredSchedulerAdminJsp(
              ("/ui/admin/" + page).toLowerCase()));
    }
    String[] tools = {
      "ConsistencyChecker.jsp", "ConsistencyCheckerReport.jsp", "ConsistencyCheckerReview.jsp"
    };
    for (String tool : tools) {
      assertEquals(
          "/cm/app/?view=admin",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/ui/admin/tools/" + tool, null));
      assertTrue(
          PSWebUiSpaFallbackFilter.isRetiredSchedulerAdminJsp(
              ("/ui/admin/tools/" + tool).toLowerCase()));
    }
    // Kept scheduler-console pages still pass through (null = no redirect).
    // #6160: TaskDetailLog retired (see pages[] above); console/index/auth include
    // and ConvertVariant/RxFix tools stay (no React equivalent yet).
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/ui/admin/console.faces", null));
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/ui/admin/tools/RxFix.faces", null));
    assertFalse(
        PSWebUiSpaFallbackFilter.isRetiredSchedulerAdminJsp("/ui/admin/console.faces"));
    assertFalse(PSWebUiSpaFallbackFilter.isRetiredSchedulerAdminJsp(null));
  }

  @Test
  public void retiredWorkflowDialogJspsRedirectToEditorView() {
    // #6139 FR-002: deleted ui/activeassembly/workflow dialog-pane fragments
    // land on the SPA editor (EditorWorkflowPanel owns in-context transitions);
    // sibling AA pages pass through.
    String[] panes = {"workflowactions.jsp", "adhocsearch.jsp", "adhocresults.jsp"};
    for (String pane : panes) {
      assertEquals(
          "/cm/app/?view=editor",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/ui/activeassembly/workflow/" + pane, null));
      assertEquals(
          "/cm/app/?view=editor&sys_contentid=42",
          PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
              "/Rhythmyx/ui/activeassembly/workflow/" + pane, "sys_contentid=42"));
      assertTrue(
          PSWebUiSpaFallbackFilter.isRetiredWorkflowDialogJsp(
              ("/ui/activeassembly/workflow/" + pane).toLowerCase()));
    }
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetiredJspRedirectLocation(
            "/ui/activeassembly/UserStatus.jsp", null));
    assertFalse(
        PSWebUiSpaFallbackFilter.isRetiredWorkflowDialogJsp(
            "/ui/activeassembly/userstatus.jsp"));
    assertFalse(PSWebUiSpaFallbackFilter.isRetiredWorkflowDialogJsp(null));
  }

  @Test
  public void retiredArchitectureLocationGetsContextPrefix() {
    assertEquals(
        "/Rhythmyx/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx", "/cm/app/?view=arch"));
    assertEquals(
        "/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.withContextPath("", "/cm/app/?view=arch"));
    assertEquals(
        "/Rhythmyx/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx", "/Rhythmyx/cm/app/?view=arch"));
    assertEquals("/cm/app/?view=arch", PSWebUiSpaFallbackFilter.withContextPath("/", "/cm/app/?view=arch"));
    // #6145: trailing-slash context still prefixes exactly once; null/empty pass through.
    assertEquals(
        "/Rhythmyx/cm/app/?view=arch",
        PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx/", "/cm/app/?view=arch"));
    assertNull(PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx", null));
    assertEquals("", PSWebUiSpaFallbackFilter.withContextPath("/Rhythmyx", ""));
    assertTrue(PSWebUiSpaFallbackFilter.isRetiredArchitectureJsp("/rhythmyx/cm/app/sitearchitecture.jsp"));
    assertTrue(
        !PSWebUiSpaFallbackFilter.isRetiredArchitectureJsp("/cm/app/admin.jsp"));
    assertTrue(
        !PSWebUiSpaFallbackFilter.isRetiredArchitectureJsp("/cm/app-sitearchitecture.jsp"));
  }

  @Test
  public void passesThroughRealJspsAndStaticAndNonSpa() {
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/spa.jsp", "entry=home"));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/index.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/dashboard.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/webmgt.jsp", null));
    assertNull(PSWebUiSpaFallbackFilter.buildSpaForwardPath("/cm/app/siteArchitecture.jsp", null));
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
  public void remapsMisplacedSpaMountEditorIconsToCanonicalImages() {
    assertEquals(
        "/cm/images/icons/editor/delete.png",
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/pages/app/images/icons/editor/delete.png"));
    assertEquals(
        "/cm/images/icons/editor/edit.png",
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/pages/app/images/icons/editor/edit.png"));
    assertEquals(
        "/cm/images/icons/editor/delete.png",
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/app/images/icons/editor/delete.png"));
  }

  @Test
  public void remapsSpaMountImagesRejectsTraversalAndNonImages() {
    assertNull(
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/pages/app/images/../WEB-INF/web.xml"));
    assertNull(
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/pages/app/images/icons/%2e%2e/secret.png"));
    assertNull(
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath("/cm/pages/app/images/icons/editor/"));
    assertNull(
        PSWebUiSpaFallbackFilter.buildStaticImageRemapPath(
            "/cm/pages/app/images/icons/editor/delete.js"));
    assertNull(PSWebUiSpaFallbackFilter.buildStaticImageRemapPath("/cm/images/icons/editor/delete.png"));
    assertNull(PSWebUiSpaFallbackFilter.buildStaticImageRemapPath("/cm/app/home"));
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

  @Test
  public void forwardsRetiredApiDirectoryToStaticRedirect() {
    // #5672: directory requests serve the retirement redirect (index.html -> /openapi/)
    // because the webapp welcome-file list only covers index.jsp.
    assertEquals(
        "/cm/api/index.html",
        PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/api/"));
    assertEquals(
        "/cm/api/index.html", PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/api"));
    // Tolerate an unstripped context prefix (root-context cells) and casing.
    assertEquals(
        "/cm/api/index.html",
        PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/Rhythmyx/cm/api/"));
    assertEquals(
        "/cm/api/index.html",
        PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/Rhythmyx/cm/api"));
    assertEquals(
        "/cm/api/index.html", PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/CM/API/"));
  }

  @Test
  public void retirementForwardPassesThroughIndexAndOtherPaths() {
    // The index page itself is served statically — never forward (no self-loop).
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/api/index.html"));
    assertNull(
        PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/Rhythmyx/cm/api/index.html"));
    // Unrelated /cm/* trees and near-miss segments pass through.
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/app/home"));
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/apix"));
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/api-v2/"));
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath("/cm/api/other.html"));
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath(null));
    assertNull(PSWebUiSpaFallbackFilter.buildRetirementForwardPath(""));
  }
}
