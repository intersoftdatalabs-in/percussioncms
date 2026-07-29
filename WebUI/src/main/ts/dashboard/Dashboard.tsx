/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

/**
 * Main Dashboard component.
 *
 * <p>Replaces the retired Apache Shindig 3.0.0-beta4 gadget container with
 * a modern React/TypeScript implementation. Loads user's saved dashboard
 * layout and renders widgets accordingly.</p>
 *
 * <p>Features fallback to legacy dashboard via feature flag (URL param
 * {@code ?legacyDashboard=true}).</p>
 */

import React, { useEffect, useState, useCallback, useMemo } from "react";
import { DashboardLayout, type DashboardWidget } from "./DashboardLayout";
import { AddGadgetModal, type AddGadgetModalProps } from "./AddGadgetModal";
import { useDashboardConfig, type WidgetConfig } from "./hooks/useDashboardConfig";
import {
  loadPreferredGadgetIds,
  PREFERRED_GADGETS_EVENT,
} from "./gadgetsCatalog";
import { WelcomeWidget } from "./WelcomeWidget";
import { ActivityWidget } from "./ActivityWidget";
import { WorkflowStatusWidget } from "./WorkflowStatusWidget";
import { ProcessMonitorWidget } from "./ProcessMonitorWidget";
import { EffectivenessWidget } from "./EffectivenessWidget";
import { AssetsStatusWidget } from "./AssetsStatusWidget";
import { BulkUploadWidget } from "./BulkUploadWidget";
import { ReportsWidget } from "./ReportsWidget";
import { TrafficWidget } from "./TrafficWidget";
import { BlogsWidget } from "./BlogsWidget";
import { CommentsWidget } from "./CommentsWidget";
import { FormsTrackerWidget } from "./FormsTrackerWidget";
import { CookieConsentWidget } from "./CookieConsentWidget";
import { SEOAuditWidget } from "./SEOAuditWidget";
import { GoogleSetupWidget } from "./GoogleSetupWidget";
import { MembershipWidget } from "./MembershipWidget";
import { SitewideFrameworkWidget } from "./SitewideFrameworkWidget";
import { SiteimproveWidget } from "./SiteimproveWidget";
import { IframeWidget } from "./IframeWidget";
import { GlobalVariablesWidget } from "./GlobalVariablesWidget";
import { WidgetConfigurationWidget } from "./WidgetConfigurationWidget";

/**
 * Available gadgets registry for the dashboard.
 *
 * <p>Maps gadget IDs to their component types, names, and descriptions.</p>
 */
interface GadgetInfo {
  id: string;
  name: string;
  component: React.ComponentType<any>;
  description?: string;
  category?: string;
}

const AVAILABLE_GADGETS: GadgetInfo[] = [
  {
    id: "welcome",
    name: "Welcome",
    component: WelcomeWidget,
    description: "Welcome message and dashboard introduction",
    category: "System",
  },
  {
    id: "workflow",
    name: "Pages By Status",
    component: WorkflowStatusWidget,
    description: "Pages grouped by workflow state (classic Pages By Status)",
    category: "Content Management",
  },
  {
    id: "activity",
    name: "Activity",
    component: ActivityWidget,
    description: "Content activity metrics by path and duration",
    // GadgetRegistry.xml group "Deprecated" (v8.1.7 #722 / #885)
    category: "Deprecated",
  },
  {
    id: "process-monitor",
    name: "Process Monitor",
    component: ProcessMonitorWidget,
    description: "System process and monitoring status",
    category: "System",
  },
  {
    id: "effectiveness",
    name: "What's Working",
    component: EffectivenessWidget,
    description:
      "Effectiveness scores (requires Google Analytics — see Google Setup)",
    category: "Analytics",
  },
  {
    id: "assets-status",
    name: "Assets By Status",
    component: AssetsStatusWidget,
    description: "Asset workflow status distribution",
    category: "Content Management",
  },
  {
    id: "bulk-upload",
    name: "Bulk Upload",
    component: BulkUploadWidget,
    description: "Upload files into Assets/uploads",
    category: "Content Management",
  },
  {
    id: "reports",
    name: "Reports",
    component: ReportsWidget,
    description: "Quick CMS reports hub (SEO, forms, comments, activity)",
    category: "Analytics",
  },
  {
    id: "traffic",
    name: "Traffic",
    component: TrafficWidget,
    description:
      "Content traffic series (visits need Google Analytics profile)",
    category: "Analytics",
  },
  {
    id: "blogs",
    name: "Blogs",
    component: BlogsWidget,
    description: "Blog listings and management",
    category: "Content Management",
  },
  {
    id: "comments",
    name: "Comments",
    component: CommentsWidget,
    description: "Latest visitor comments and feedback",
    category: "Content Management",
  },
  {
    id: "forms-tracker",
    name: "Form Tracker",
    component: FormsTrackerWidget,
    description: "Form submission tracking and analytics",
    category: "Content Management",
  },
  {
    id: "cookie-consent",
    name: "Cookie Consent",
    component: CookieConsentWidget,
    description: "GDPR compliance and cookie consent status",
    category: "Compliance",
  },
  {
    id: "seo-audit",
    name: "SEO Audit",
    component: SEOAuditWidget,
    description: "SEO health metrics and recommendations",
    category: "Analytics",
  },
  {
    id: "google-setup",
    name: "Google Setup",
    component: GoogleSetupWidget,
    description:
      "Google Analytics provider status (required for Traffic / What's Working)",
    category: "Analytics",
  },
  {
    id: "membership",
    name: "Membership",
    component: MembershipWidget,
    description: "User membership information and statistics",
    // GadgetRegistry.xml group "Deprecated" (v8.1.7 #722 / #885)
    category: "Deprecated",
  },
  {
    id: "sitewide-framework",
    name: "Sitewide Framework",
    component: SitewideFrameworkWidget,
    description: "Framework configuration and module status",
    category: "System",
  },
  {
    id: "siteimprove",
    name: "Siteimprove",
    component: SiteimproveWidget,
    description: "Accessibility and quality metrics from Siteimprove",
    // GadgetRegistry.xml group "Deprecated" (v8.1.7 #722 / #885)
    category: "Deprecated",
  },
  {
    id: "iframe",
    name: "External Content",
    component: IframeWidget,
    description: "Embedded external content and dashboards",
    category: "External",
  },
  {
    id: "global-variables",
    name: "Global Variables",
    component: GlobalVariablesWidget,
    description: "System-wide global variables and configuration settings",
    category: "System",
  },
  {
    id: "widget-configuration",
    name: "Dashboard Configuration",
    component: WidgetConfigurationWidget,
    description: "Manage dashboard widgets and configuration",
    // GadgetRegistry.xml group "Deprecated" (v8.1.7 #722 / #885)
    category: "Deprecated",
  },
];

/**
 * Default Home Gadgets layout — verified CMS/DTS APIs only.
 * Delivery-backed gadgets (Comments, Cookie Consent, Membership) and SEO /
 * Siteimprove are available via Add Gadget; shells without REST peers stay
 * honest “not available” placeholders.
 */
const DEFAULT_GADGETS: DashboardWidget[] = [
  {
    id: "welcome",
    name: "Welcome",
    component: WelcomeWidget,
    props: { userName: "User" },
    position: { column: "left", order: 0 },
  },
  {
    id: "blogs",
    name: "Blogs",
    component: BlogsWidget,
    props: {},
    position: { column: "left", order: 1 },
  },
  {
    id: "workflow",
    name: "Pages By Status",
    component: WorkflowStatusWidget,
    props: {},
    position: { column: "left", order: 2 },
  },
  {
    id: "activity",
    name: "Activity",
    component: ActivityWidget,
    props: {},
    position: { column: "right", order: 0 },
  },
  {
    id: "assets-status",
    name: "Assets By Status",
    component: AssetsStatusWidget,
    props: {},
    position: { column: "right", order: 1 },
  },
  {
    id: "process-monitor",
    name: "Process Monitor",
    component: ProcessMonitorWidget,
    props: {},
    position: { column: "right", order: 2 },
  },
  {
    id: "google-setup",
    name: "Google Setup",
    component: GoogleSetupWidget,
    props: {},
    position: { column: "left", order: 3 },
  },
  {
    id: "traffic",
    name: "Traffic",
    component: TrafficWidget,
    props: {},
    position: { column: "right", order: 3 },
  },
  {
    id: "effectiveness",
    name: "What's Working",
    component: EffectivenessWidget,
    props: {},
    position: { column: "right", order: 4 },
  },
  {
    id: "forms-tracker",
    name: "Form Tracker",
    component: FormsTrackerWidget,
    props: {},
    position: { column: "left", order: 4 },
  },
  {
    id: "comments",
    name: "Comments",
    component: CommentsWidget,
    props: {},
    position: { column: "right", order: 5 },
  },
  {
    id: "global-variables",
    name: "Global Variables",
    component: GlobalVariablesWidget,
    props: {},
    position: { column: "left", order: 5 },
  },
  {
    id: "cookie-consent",
    name: "Cookie Consent",
    component: CookieConsentWidget,
    props: {},
    position: { column: "right", order: 6 },
  },
];

/** Build layout tiles from preferred catalog ids (session layout prefs). */
function buildGadgetsFromIds(ids: string[]): DashboardWidget[] {
  const out: DashboardWidget[] = [];
  ids.forEach((id, index) => {
    const info = AVAILABLE_GADGETS.find((g) => g.id === id);
    if (!info) return;
    out.push({
      id: info.id,
      name: info.name,
      component: info.component,
      props: {},
      position: {
        column: index % 2 === 0 ? "left" : "right",
        order: Math.floor(index / 2),
      },
    });
  });
  return out.length > 0 ? out : DEFAULT_GADGETS;
}

function initialGadgets(): DashboardWidget[] {
  const preferred = loadPreferredGadgetIds();
  if (preferred && preferred.length > 0) {
    return buildGadgetsFromIds(preferred);
  }
  return DEFAULT_GADGETS;
}

export interface DashboardProps {
  legacyDashboardUrl?: string;
  userId?: string;
  /**
   * When true (Home Gadgets section), avoid full-viewport loading chrome and
   * treat this as an embedded panel rather than a standalone page.
   */
  embedded?: boolean;
}

/**
 * Dashboard gadget host — React widgets formerly shown on the peer dash page.
 * Product placement is Home → Gadgets (PR-7); this component remains reusable.
 *
 * @param legacyDashboardUrl - Optional URL to legacy dashboard for fallback
 * @param userId - Optional user ID for loading/saving dashboard configuration
 * @param embedded - Home section embed (compact loading, no peer-page framing)
 */
export const Dashboard: React.FC<DashboardProps> = ({
  legacyDashboardUrl = "/cm/app/dashboard.jsp?legacy=true",
  userId,
  embedded = false,
}) => {
  const [gadgets, setGadgets] = useState<DashboardWidget[]>(() => initialGadgets());
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Load dashboard configuration from server
  const { config, isLoading, error: configError, addWidget, removeWidget } = useDashboardConfig(
    userId,
    true // autoRefresh
  );

  // Session layout prefs from Widget Configuration gadget
  useEffect(() => {
    const onPreferred = (ev: Event) => {
      const detail = (ev as CustomEvent<{ ids?: string[] }>).detail;
      const ids = detail?.ids ?? loadPreferredGadgetIds();
      if (ids && ids.length > 0) {
        setGadgets(buildGadgetsFromIds(ids));
      }
    };
    window.addEventListener(PREFERRED_GADGETS_EVENT, onPreferred);
    return () => window.removeEventListener(PREFERRED_GADGETS_EVENT, onPreferred);
  }, []);

  // Convert WidgetConfig to DashboardWidget format
  const convertToDashboardWidget = useCallback(
    (widgetConfig: WidgetConfig): DashboardWidget | null => {
      const gadgetInfo = AVAILABLE_GADGETS.find((g) => g.id === widgetConfig.widgetKey);
      if (!gadgetInfo) return null;

      return {
        id: widgetConfig.widgetKey,
        name: gadgetInfo.name,
        component: gadgetInfo.component,
        props: widgetConfig.settings || {},
        position: widgetConfig.position,
      };
    },
    []
  );

  // Convert DashboardWidget to WidgetConfig format
  const convertToWidgetConfig = useCallback((dashboardWidget: DashboardWidget): WidgetConfig => {
    return {
      widgetKey: dashboardWidget.id,
      widgetType: dashboardWidget.id,
      position: dashboardWidget.position,
      settings: dashboardWidget.props,
    };
  }, []);

  // Load configuration from server on mount
  useEffect(() => {
    if (config && userId) {
      const dashboardWidgets = config.widgets
        .map(convertToDashboardWidget)
        .filter((w) => w !== null) as DashboardWidget[];

      if (dashboardWidgets.length > 0) {
        setGadgets(dashboardWidgets);
      } else {
        // If config exists but is empty, fallback to defaults and save
        setGadgets(DEFAULT_GADGETS);
      }
    } else if (!userId) {
      // No user ID — preferred session layout or product defaults
      setGadgets(initialGadgets());
    }
  }, [config, userId, convertToDashboardWidget]);

  // Get active gadget IDs for the modal
  const activeGadgetIds = useMemo(() => new Set(gadgets.map((g) => g.id)), [gadgets]);

  // Get available gadgets that aren't already active
  const availableGadgetsForModal: Parameters<typeof AddGadgetModal>[0]["availableGadgets"] = useMemo(
    () =>
      AVAILABLE_GADGETS.map((gadget) => ({
        id: gadget.id,
        name: gadget.name,
        description: gadget.description,
        category: gadget.category,
      })),
    []
  );

  // Check for feature flag to use legacy dashboard
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("legacyDashboard") === "true") {
      console.log("Legacy dashboard flag detected, redirecting...");
      window.location.href = legacyDashboardUrl;
    }
  }, [legacyDashboardUrl]);

  /**
   * Handle adding a new gadget to the dashboard.
   */
  const handleAddGadget = async (gadgetId: string) => {
    // Find the gadget info
    const gadgetInfo = AVAILABLE_GADGETS.find((g) => g.id === gadgetId);
    if (!gadgetInfo) return;

    try {
      setIsSaving(true);

      // Calculate next order for the left column
      const leftGadgets = gadgets.filter((g) => g.position.column === "left");
      const nextOrder = leftGadgets.length;

      // Create new gadget widget
      const newGadget: DashboardWidget = {
        id: gadgetId,
        name: gadgetInfo.name,
        component: gadgetInfo.component,
        props: {},
        position: { column: "left", order: nextOrder },
      };

      // Add to local state
      setGadgets([...gadgets, newGadget]);

      // Save to server if userId is present
      if (userId && addWidget) {
        const widgetConfig = convertToWidgetConfig(newGadget);
        await addWidget(widgetConfig);
      }

      // Close modal
      setIsModalOpen(false);
    } catch (err) {
      console.error("Failed to add gadget:", err);
      // TODO: Show user-friendly error message
    } finally {
      setIsSaving(false);
    }
  };

  /**
   * Handle removing a gadget from the dashboard.
   */
  const handleRemoveGadget = async (gadgetId: string) => {
    try {
      setIsSaving(true);

      // Remove from local state
      setGadgets(gadgets.filter((g) => g.id !== gadgetId));

      // Remove from server if userId is present
      if (userId && removeWidget) {
        await removeWidget(gadgetId);
      }
    } catch (err) {
      console.error("Failed to remove gadget:", err);
      // TODO: Show user-friendly error message
    } finally {
      setIsSaving(false);
    }
  };

  // Show loading state while fetching configuration (never block forever on error)
  if (isLoading && userId) {
    return (
      <div
        data-testid="dashboard-loading"
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          minHeight: embedded ? "12rem" : "100vh",
          color: "#999",
        }}
      >
        <p>Loading gadgets…</p>
      </div>
    );
  }

  return (
    <div style={{ width: "100%" }} data-testid="dashboard-root" data-embedded={embedded ? "1" : "0"}>
      {/* Soft config warning — still show default gadgets */}
      {configError && userId ? (
        <div
          data-testid="dashboard-config-warning"
          style={{
            padding: "10px 16px",
            color: "#664d03",
            backgroundColor: "#fff3cd",
            borderBottom: "1px solid #ffecb5",
            fontSize: "0.9rem",
          }}
        >
          Could not load saved gadget layout ({configError}). Showing defaults.{" "}
          <a href={legacyDashboardUrl}>Legacy dashboard</a>
        </div>
      ) : null}

      {/* Dashboard Header */}
      <div
        style={{
          padding: "16px 20px",
          borderBottom: "1px solid #e0e0e0",
          backgroundColor: "#f5f5f5",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <h1 style={{ margin: 0, fontSize: embedded ? "1.15em" : "1.5em", color: "#333" }}>
          {embedded ? "Gadgets" : "Dashboard"}
        </h1>
        <button
          type="button"
          data-testid="dashboard-add-gadget"
          onClick={() => setIsModalOpen(true)}
          style={{
            backgroundColor: "#2196f3",
            color: "white",
            border: "none",
            padding: "8px 16px",
            borderRadius: "4px",
            cursor: "pointer",
            fontSize: "0.95em",
          }}
        >
          + Add Gadget
        </button>
      </div>

      {/* Dashboard Layout */}
      <DashboardLayout widgets={gadgets} onRemoveGadget={handleRemoveGadget} />

      {/* Add Gadget Modal */}
      <AddGadgetModal
        isOpen={isModalOpen}
        availableGadgets={availableGadgetsForModal}
        activeGadgetIds={activeGadgetIds}
        onAdd={handleAddGadget}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
};
