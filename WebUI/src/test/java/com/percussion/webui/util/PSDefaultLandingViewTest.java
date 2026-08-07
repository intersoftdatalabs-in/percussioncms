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
package com.percussion.webui.util;

import static com.percussion.webui.util.PSDefaultLandingView.TYPE_ARCHITECTURE;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_DASHBOARD;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_DESIGNER;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_EDITOR;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_HOME;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_PUBLISH;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_WIDGET_BUILDER;
import static com.percussion.webui.util.PSDefaultLandingView.TYPE_WORKFLOW;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_ARCH;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_DASH;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_DESIGN;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_EDITOR;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_HOME;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_PUBLISH;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_WIDGET_BUILDER;
import static com.percussion.webui.util.PSDefaultLandingView.VIEW_WORKFLOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Behavioral tests for default CMS landing → {@code view=} mapping and unauthorized fail-closed
 * (issue #2210 / parent #959 slice 3).
 */
public class PSDefaultLandingViewTest {

  @Test
  public void mapsCanonicalRoleTypesUnchangedForRegression() {
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey(TYPE_HOME));
    assertEquals(VIEW_DASH, PSDefaultLandingView.homepageTypeToViewKey(TYPE_DASHBOARD));
    assertEquals(VIEW_EDITOR, PSDefaultLandingView.homepageTypeToViewKey(TYPE_EDITOR));
  }

  @Test
  public void mapsExpandedUserLandingTypes() {
    assertEquals(VIEW_DESIGN, PSDefaultLandingView.homepageTypeToViewKey(TYPE_DESIGNER));
    assertEquals(VIEW_ARCH, PSDefaultLandingView.homepageTypeToViewKey(TYPE_ARCHITECTURE));
    assertEquals(VIEW_PUBLISH, PSDefaultLandingView.homepageTypeToViewKey(TYPE_PUBLISH));
    assertEquals(VIEW_WORKFLOW, PSDefaultLandingView.homepageTypeToViewKey(TYPE_WORKFLOW));
    assertEquals(
        VIEW_WIDGET_BUILDER, PSDefaultLandingView.homepageTypeToViewKey(TYPE_WIDGET_BUILDER));
  }

  @Test
  public void mapsViewKeyAliases() {
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey("home"));
    assertEquals(VIEW_DASH, PSDefaultLandingView.homepageTypeToViewKey("dash"));
    assertEquals(VIEW_DASH, PSDefaultLandingView.homepageTypeToViewKey("dashboard"));
    assertEquals(VIEW_EDITOR, PSDefaultLandingView.homepageTypeToViewKey("editor"));
    assertEquals(VIEW_DESIGN, PSDefaultLandingView.homepageTypeToViewKey("design"));
    assertEquals(VIEW_DESIGN, PSDefaultLandingView.homepageTypeToViewKey("admin"));
    assertEquals(VIEW_ARCH, PSDefaultLandingView.homepageTypeToViewKey("arch"));
    assertEquals(VIEW_ARCH, PSDefaultLandingView.homepageTypeToViewKey("navigation"));
    assertEquals(VIEW_PUBLISH, PSDefaultLandingView.homepageTypeToViewKey("publish"));
    assertEquals(VIEW_WORKFLOW, PSDefaultLandingView.homepageTypeToViewKey("workflow"));
    assertEquals(VIEW_WIDGET_BUILDER, PSDefaultLandingView.homepageTypeToViewKey("widgetbuilder"));
  }

  @Test
  public void blankUnknownFallsBackToHome() {
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey(null));
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey(""));
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey("   "));
    assertEquals(VIEW_HOME, PSDefaultLandingView.homepageTypeToViewKey("NotARealModule"));
  }

  @Test
  public void roleOnlyUsersResolveHomeDashEditorWithoutGating() {
    // Contributor / Editor users: Home, Dashboard, Editor always allowed
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_HOME, false, false));
    assertEquals(
        VIEW_DASH, PSDefaultLandingView.resolveAuthorizedView(TYPE_DASHBOARD, false, false));
    assertEquals(
        VIEW_EDITOR, PSDefaultLandingView.resolveAuthorizedView(TYPE_EDITOR, false, false));
  }

  @Test
  public void unauthorizedDesignArchPublishFailClosedToHome() {
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_DESIGNER, false, false));
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_ARCHITECTURE, false, false));
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_PUBLISH, false, false));
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_WIDGET_BUILDER, false, false));
  }

  @Test
  public void workflowIsAdminOnlyDesignerCannotOpen() {
    assertEquals(
        VIEW_HOME, PSDefaultLandingView.resolveAuthorizedView(TYPE_WORKFLOW, false, true));
    assertEquals(
        VIEW_WORKFLOW, PSDefaultLandingView.resolveAuthorizedView(TYPE_WORKFLOW, true, false));
    assertEquals(
        VIEW_WORKFLOW, PSDefaultLandingView.resolveAuthorizedView(TYPE_WORKFLOW, true, true));
  }

  @Test
  public void designerMayOpenDesignArchPublishWidgetBuilder() {
    assertEquals(
        VIEW_DESIGN, PSDefaultLandingView.resolveAuthorizedView(TYPE_DESIGNER, false, true));
    assertEquals(
        VIEW_ARCH, PSDefaultLandingView.resolveAuthorizedView(TYPE_ARCHITECTURE, false, true));
    assertEquals(
        VIEW_PUBLISH, PSDefaultLandingView.resolveAuthorizedView(TYPE_PUBLISH, false, true));
    assertEquals(
        VIEW_WIDGET_BUILDER,
        PSDefaultLandingView.resolveAuthorizedView(TYPE_WIDGET_BUILDER, false, true));
  }

  @Test
  public void adminMayOpenAllGatedLandings() {
    assertEquals(
        VIEW_DESIGN, PSDefaultLandingView.resolveAuthorizedView(TYPE_DESIGNER, true, false));
    assertEquals(
        VIEW_ARCH, PSDefaultLandingView.resolveAuthorizedView(TYPE_ARCHITECTURE, true, false));
    assertEquals(
        VIEW_PUBLISH, PSDefaultLandingView.resolveAuthorizedView(TYPE_PUBLISH, true, false));
    assertEquals(
        VIEW_WORKFLOW, PSDefaultLandingView.resolveAuthorizedView(TYPE_WORKFLOW, true, false));
    assertEquals(
        VIEW_WIDGET_BUILDER,
        PSDefaultLandingView.resolveAuthorizedView(TYPE_WIDGET_BUILDER, true, false));
  }

  @Test
  public void isViewAuthorizedMatchesIndexJspRules() {
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_HOME, false, false));
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_DASH, false, false));
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_EDITOR, false, false));
    assertFalse(PSDefaultLandingView.isViewAuthorized(VIEW_DESIGN, false, false));
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_DESIGN, false, true));
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_DESIGN, true, false));
    assertFalse(PSDefaultLandingView.isViewAuthorized(VIEW_WORKFLOW, false, true));
    assertTrue(PSDefaultLandingView.isViewAuthorized(VIEW_WORKFLOW, true, false));
  }

  @Test
  public void roleGatedClassification() {
    assertFalse(PSDefaultLandingView.isRoleGatedView(VIEW_HOME));
    assertFalse(PSDefaultLandingView.isRoleGatedView(VIEW_DASH));
    assertTrue(PSDefaultLandingView.isRoleGatedView(VIEW_DESIGN));
    assertTrue(PSDefaultLandingView.isAdminOnlyView(VIEW_WORKFLOW));
    assertFalse(PSDefaultLandingView.isAdminOnlyView(VIEW_DESIGN));
  }
}
