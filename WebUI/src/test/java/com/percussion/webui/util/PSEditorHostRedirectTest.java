/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
package com.percussion.webui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Behavioral tests for leftover CE bookmark → React editor host (#3473). */
public class PSEditorHostRedirectTest {

  @Test
  public void parseContentIdAcceptsNumericAndGuidTail() {
    assertEquals("551", PSEditorHostRedirect.parseContentId("551"));
    assertEquals("551", PSEditorHostRedirect.parseContentId("16777215-101-551"));
    assertEquals("708", PSEditorHostRedirect.parseContentId("1-101-708"));
    assertNull(PSEditorHostRedirect.parseContentId(null));
    assertNull(PSEditorHostRedirect.parseContentId(""));
    assertNull(PSEditorHostRedirect.parseContentId("0"));
    assertNull(PSEditorHostRedirect.parseContentId("not-an-id"));
  }

  @Test
  public void normalizeModeMapsReadonlyToView() {
    assertEquals("view", PSEditorHostRedirect.normalizeMode("readonly"));
    assertEquals("view", PSEditorHostRedirect.normalizeMode("VIEW"));
    assertEquals("promote", PSEditorHostRedirect.normalizeMode("promote"));
    assertEquals("edit", PSEditorHostRedirect.normalizeMode(null));
    assertEquals("edit", PSEditorHostRedirect.normalizeMode("edit"));
  }

  @Test
  public void spaRedirectDoesNotUseLeftoverViewEditor() {
    String loc = PSEditorHostRedirect.buildSpaRedirect("", "16777215-101-551", "readonly");
    assertEquals("/cm/app/spa.jsp?entry=editor&contentId=551&mode=view", loc);
    assertFalse(loc.contains("view=editor"));
    assertFalse(loc.contains("editAsset.jsp"));
    String empty = PSEditorHostRedirect.buildSpaRedirect(null, null, null);
    assertEquals("/cm/app/spa.jsp?entry=editor&mode=edit", empty);
    assertTrue(
        PSEditorHostRedirect.buildSpaRedirect("/Rhythmyx", "42", "edit")
            .startsWith("/Rhythmyx/cm/app/spa.jsp?entry=editor"));
  }

  @Test
  public void retiredEditAssetJspMatchesAppAndPagesTrees() {
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/app/editasset.jsp"));
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/pages/app/editasset.jsp"));
    assertTrue(PSEditorHostRedirect.isRetiredEditAssetJsp("/rhythmyx/cm/app/editasset.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/app/webmgt.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/app-editasset.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp("/cm/foo/editasset.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredEditAssetJsp(null));
    assertTrue(PSEditorHostRedirect.isRetiredArchitectureJsp("/cm/app/sitearchitecture.jsp"));
    assertTrue(
        PSEditorHostRedirect.isRetiredArchitectureJsp("/rhythmyx/cm/pages/app/sitearchitecture.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredArchitectureJsp("/cm/app-sitearchitecture.jsp"));
  }

  @Test
  public void retiredWorkflowAdminJspsMatchAppAndPagesTrees() {
    // #6095 FR-002: five legacy hosts retired from both product trees.
    String[] hosts = {
      "adminworkflow.jsp", "percworkflow.jsp", "percroles.jsp", "percusers.jsp", "perccategories.jsp"
    };
    for (String host : hosts) {
      assertTrue(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/app/" + host));
      assertTrue(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/pages/app/" + host));
      assertTrue(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/rhythmyx/cm/app/" + host));
    }
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/app/admin.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/app/webmgt.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/app-adminworkflow.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowAdminJsp("/cm/foo/percusers.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowAdminJsp(null));
  }

  @Test
  public void retiredSchedulerAdminJspsMatchUiAdminTrees() {
    // #6140 FR-002: eleven scheduler-console hosts retired from ui/admin (+tools),
    // in both the .jsp and the .faces spelling (JSF views bookmark as .faces).
    String[] pages = {
      "scheduledtask",
      "scheduledtasklist",
      "removeconfirmation",
      "tasklogs",
      "deletealltasklogwarning",
      "notasklogselectionwarning",
      "tasknotificationeditor",
      "tasknotificationlist"
    };
    for (String page : pages) {
      assertTrue(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/" + page + ".jsp"));
      assertTrue(
          PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/" + page + ".faces"));
      assertTrue(
          PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/rhythmyx/ui/admin/" + page + ".jsp"));
    }
    String[] tools = {"consistencychecker", "consistencycheckerreport", "consistencycheckerreview"};
    for (String tool : tools) {
      assertTrue(
          PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/tools/" + tool + ".jsp"));
      assertTrue(
          PSEditorHostRedirect.isRetiredSchedulerAdminJsp(
              "/ui/admin/tools/" + tool + ".faces"));
    }
    // Kept pages still pass through: detail log, command console, index redirect,
    // auth include, ConvertVariant + RxFix tools (no React equivalent yet).
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/taskdetaillog.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/taskdetaillog.faces"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/console.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/console.faces"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/index.jsp"));
    assertFalse(
        PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/tools/convertvariant.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/tools/rxfix.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin/tools/rxfixpreview.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/ui/admin-scheduledtask.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp("/cm/app/tasklogs.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredSchedulerAdminJsp(null));
  }

  @Test
  public void retiredWorkflowDialogJspsMatchActiveAssemblyWorkflowTree() {
    // #6139 FR-002: three AA workflow dialog-pane fragments retired from
    // ui/activeassembly/workflow. Sibling UserStatus.jsp / help pages keep passing through.
    String[] panes = {"workflowactions.jsp", "adhocsearch.jsp", "adhocresults.jsp"};
    for (String pane : panes) {
      assertTrue(
          PSEditorHostRedirect.isRetiredWorkflowDialogJsp(
              "/ui/activeassembly/workflow/" + pane));
      assertTrue(
          PSEditorHostRedirect.isRetiredWorkflowDialogJsp(
              "/Rhythmyx/ui/activeassembly/workflow/" + pane));
    }
    assertFalse(
        PSEditorHostRedirect.isRetiredWorkflowDialogJsp("/ui/activeassembly/UserStatus.jsp"));
    assertFalse(
        PSEditorHostRedirect.isRetiredWorkflowDialogJsp(
            "/ui/activeassembly/help/aboutaahelp.jsp"));
    assertFalse(
        PSEditorHostRedirect.isRetiredWorkflowDialogJsp(
            "/ui/activeassembly/workflow-workflowactions.jsp"));
    assertFalse(
        PSEditorHostRedirect.isRetiredWorkflowDialogJsp("/cm/app/workflowactions.jsp"));
    assertFalse(PSEditorHostRedirect.isRetiredWorkflowDialogJsp(null));
  }

  @Test
  public void spaRedirectForwardsLinkbackWarningWithoutLegacySiteHints() {
    String loc =
        PSEditorHostRedirect.buildSpaRedirect(
            "", null, "readonly", "The page you are attempting to reach, does not exist in the CMS.");
    assertTrue(loc.contains("entry=editor"));
    assertTrue(loc.contains("warningMessage="));
    assertTrue(loc.contains("does+not+exist") || loc.contains("does%20not%20exist"));
    assertFalse(loc.contains("pathType="));
    assertFalse(loc.contains("editAsset.jsp"));
  }
}
