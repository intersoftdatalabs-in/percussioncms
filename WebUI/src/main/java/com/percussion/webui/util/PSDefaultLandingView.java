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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps effective CMS homepage types (and view-key aliases) to {@code index.jsp} {@code view=} keys
 * and fails closed when the mapped target is not allowed for the current user.
 *
 * <p>Used from app-entry JSPs so mapping + authorization fallback is unit-testable outside the JSP
 * container. Callers should pass the effective landing from {@code PSRoleService.getUserHomepage()}
 * (user override when set — see issue #2209 — else role resolve else Home).
 *
 * <p>Product #959 slice 3 (issue #2210).
 */
public final class PSDefaultLandingView {

  /** Safe default when unset, unknown, or unauthorized. */
  public static final String VIEW_HOME = "home";

  public static final String VIEW_DASH = "dash";
  public static final String VIEW_EDITOR = "editor";
  public static final String VIEW_DESIGN = "design";
  public static final String VIEW_ARCH = "arch";
  public static final String VIEW_PUBLISH = "publish";
  public static final String VIEW_WORKFLOW = "workflow";
  public static final String VIEW_WIDGET_BUILDER = "widgetbuilder";

  /** Content Explorer SPA ({@code spa.jsp?entry=explorer}). */
  public static final String VIEW_EXPLORER = "explorer";

  /** Developer / System Definition SPA ({@code spa.jsp?entry=developer}). */
  public static final String VIEW_DEVELOPER = "developer";

  /** Canonical product homepage types (PascalCase), peer to role/user service constants. */
  public static final String TYPE_HOME = "Home";

  public static final String TYPE_DASHBOARD = "Dashboard";
  public static final String TYPE_EDITOR = "Editor";
  public static final String TYPE_DESIGNER = "Designer";
  public static final String TYPE_ARCHITECTURE = "Architecture";
  public static final String TYPE_PUBLISH = "Publish";
  public static final String TYPE_WORKFLOW = "Workflow";
  public static final String TYPE_WIDGET_BUILDER = "WidgetBuilder";

  /** Product homepage type: Content Explorer. */
  public static final String TYPE_EXPLORER = "Explorer";

  /** Product homepage type: Developer / System Definition. */
  public static final String TYPE_DEVELOPER = "Developer";

  /**
   * Views that require Admin. Designer may open a subset ({@link #DESIGNER_ALLOWED_VIEWS});
   * workflow and admin SPA entry remain Admin-only.
   */
  private static final Set<String> ADMIN_REQUIRED_VIEWS =
      Set.of(
          VIEW_DESIGN,
          VIEW_ARCH,
          VIEW_PUBLISH,
          VIEW_WORKFLOW,
          VIEW_WIDGET_BUILDER,
          VIEW_DEVELOPER,
          "admin");

  /** Subset of admin-gated views that Designers may open (matches index.jsp designerViews). */
  private static final Set<String> DESIGNER_ALLOWED_VIEWS =
      Set.of(VIEW_DESIGN, VIEW_ARCH, VIEW_PUBLISH, VIEW_WIDGET_BUILDER, VIEW_DEVELOPER);

  /** Canonical type → view key. */
  private static final Map<String, String> TYPE_TO_VIEW =
      Map.ofEntries(
          Map.entry(TYPE_HOME, VIEW_HOME),
          Map.entry(TYPE_DASHBOARD, VIEW_DASH),
          Map.entry(TYPE_EDITOR, VIEW_EDITOR),
          Map.entry(TYPE_DESIGNER, VIEW_DESIGN),
          Map.entry(TYPE_ARCHITECTURE, VIEW_ARCH),
          Map.entry(TYPE_PUBLISH, VIEW_PUBLISH),
          Map.entry(TYPE_WORKFLOW, VIEW_WORKFLOW),
          Map.entry(TYPE_WIDGET_BUILDER, VIEW_WIDGET_BUILDER),
          Map.entry(TYPE_EXPLORER, VIEW_EXPLORER),
          Map.entry(TYPE_DEVELOPER, VIEW_DEVELOPER));

  private PSDefaultLandingView() {}

  /**
   * Map a homepage type or view-key alias to an {@code index.jsp} {@code view} key.
   *
   * <p>Unknown / blank → {@link #VIEW_HOME}. Does not apply role authorization (use {@link
   * #resolveAuthorizedView(String, boolean, boolean)}).
   *
   * @param homepageType product type (Home, Dashboard, …) or view key (home, dash, …); may be null
   * @return never null view key
   */
  public static String homepageTypeToViewKey(String homepageType) {
    if (homepageType == null) {
      return VIEW_HOME;
    }
    String trimmed = homepageType.trim();
    if (trimmed.isEmpty()) {
      return VIEW_HOME;
    }
    // Exact canonical product types first
    String fromType = TYPE_TO_VIEW.get(trimmed);
    if (fromType != null) {
      return fromType;
    }
    // Already a known view key or alias (case-insensitive)
    String lower = trimmed.toLowerCase(Locale.ROOT);
    switch (lower) {
      case "home":
        return VIEW_HOME;
      case "dash":
      case "dashboard":
        return VIEW_DASH;
      case "editor":
      case "pageeditor":
      case "webmgt":
        return VIEW_EDITOR;
      case "design":
      case "designer":
      case "siteadmin":
      case "admin":
        return VIEW_DESIGN;
      case "arch":
      case "architecture":
      case "navigation":
        return VIEW_ARCH;
      case "publish":
        return VIEW_PUBLISH;
      case "workflow":
        return VIEW_WORKFLOW;
      case "widgetbuilder":
      case "widget-builder":
        return VIEW_WIDGET_BUILDER;
      case "explorer":
        return VIEW_EXPLORER;
      case "developer":
        return VIEW_DEVELOPER;
      default:
        return VIEW_HOME;
    }
  }

  /**
   * Whether the given view key requires Admin (and is not open to Designer alone).
   *
   * @param viewKey index.jsp view key; may be null
   * @return true when only Admin may open it
   */
  public static boolean isAdminOnlyView(String viewKey) {
    if (viewKey == null || viewKey.isBlank()) {
      return false;
    }
    String key = viewKey.trim().toLowerCase(Locale.ROOT);
    return ADMIN_REQUIRED_VIEWS.contains(key) && !DESIGNER_ALLOWED_VIEWS.contains(key);
  }

  /**
   * Whether the given view key is gated (Admin or Designer).
   *
   * @param viewKey index.jsp view key; may be null
   * @return true when Admin is required (Designer may still open if in designer-allowed set)
   */
  public static boolean isRoleGatedView(String viewKey) {
    if (viewKey == null || viewKey.isBlank()) {
      return false;
    }
    return ADMIN_REQUIRED_VIEWS.contains(viewKey.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * True when the user may open {@code viewKey} given Admin/Designer flags (same rules as
   * index.jsp).
   *
   * @param viewKey index.jsp view key; may be null/blank (treated as allowed / home)
   * @param isAdmin current user is Admin
   * @param isDesigner current user is Designer
   * @return true if allowed
   */
  public static boolean isViewAuthorized(String viewKey, boolean isAdmin, boolean isDesigner) {
    if (viewKey == null || viewKey.isBlank()) {
      return true;
    }
    String key = viewKey.trim().toLowerCase(Locale.ROOT);
    if (!ADMIN_REQUIRED_VIEWS.contains(key)) {
      return true;
    }
    if (isAdmin) {
      return true;
    }
    return isDesigner && DESIGNER_ALLOWED_VIEWS.contains(key);
  }

  /**
   * Map effective homepage type to a view key the current user is allowed to open.
   *
   * <p>Unauthorized targets (e.g. Designer override for a non-designer) fail closed to {@link
   * #VIEW_HOME} so login / {@code /cm/app/} without {@code view} never redirect-loops.
   *
   * @param homepageType from {@code getUserHomepage()} (or equivalent); may be null
   * @param isAdmin current user is Admin
   * @param isDesigner current user is Designer
   * @return never null authorized view key
   */
  public static String resolveAuthorizedView(
      String homepageType, boolean isAdmin, boolean isDesigner) {
    String view = homepageTypeToViewKey(homepageType);
    if (isViewAuthorized(view, isAdmin, isDesigner)) {
      return view;
    }
    return VIEW_HOME;
  }
}
