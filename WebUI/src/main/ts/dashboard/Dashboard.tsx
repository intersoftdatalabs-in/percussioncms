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
  GADGET_CATALOG,
  loadPreferredGadgetIds,
  PREFERRED_GADGETS_EVENT,
  type GadgetCatalogEntry,
} from "./gadgetsCatalog";
import { message, MSG } from "../i18n/message";
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
 * <p>Names + descriptions are sourced from
 * {@link GADGET_CATALOG} and rendered through
 * {@link message} at use sites so locale switches re-render.</p>
 */
interface GadgetInfo {
  id: string;
  /** TMX key for the localized name. */
  nameKey: string;
  /** English fallback shown by tests / when TMX is unavailable. */
  name: string;
  component: React.ComponentType<any>;
  /** TMX key for the localized description. */
  descriptionKey: string;
  /** English fallback description. */
  description: string;
  category: string;
}

/**
 * Component lookup. Names + descriptions live in
 * {@link GADGET_CATALOG} so headers / descriptions stay in sync
 * with the gadgets catalog.
 */
const GADGET_COMPONENTS: Record<string, React.ComponentType<any>> = {
  welcome: WelcomeWidget,
  workflow: WorkflowStatusWidget,
  activity: ActivityWidget,
  "process-monitor": ProcessMonitorWidget,
  effectiveness: EffectivenessWidget,
  "assets-status": AssetsStatusWidget,
  "bulk-upload": BulkUploadWidget,
  reports: ReportsWidget,
  traffic: TrafficWidget,
  blogs: BlogsWidget,
  comments: CommentsWidget,
  "forms-tracker": FormsTrackerWidget,
  "cookie-consent": CookieConsentWidget,
  "seo-audit": SEOAuditWidget,
  "google-setup": GoogleSetupWidget,
  membership: MembershipWidget,
  "sitewide-framework": SitewideFrameworkWidget,
  siteimprove: SiteimproveWidget,
  iframe: IframeWidget,
  "global-variables": GlobalVariablesWidget,
  "widget-configuration": WidgetConfigurationWidget,
};

function catalogToGadgetInfo(entry: GadgetCatalogEntry): GadgetInfo {
  return {
    id: entry.id,
    nameKey: entry.nameKey,
    name: entry.name,
    component: GADGET_COMPONENTS[entry.id] ?? (() => null),
    descriptionKey: entry.descriptionKey,
    description: entry.description,
    category: entry.category,
  };
}

const AVAILABLE_GADGETS: GadgetInfo[] = GADGET_CATALOG.map(catalogToGadgetInfo);

/**
 * Default Home Gadgets layout — verified CMS/DTS APIs only.
 * Delivery-backed gadgets (Comments, Cookie Consent, Membership) and SEO /
 * Siteimprove are available via Add Gadget; shells without REST peers stay
 * honest "not available" placeholders.
 */
const DEFAULT_GADGET_IDS = [
  "welcome",
  "blogs",
  "workflow",
  "activity",
  "assets-status",
  "process-monitor",
  "google-setup",
  "traffic",
  "effectiveness",
  "forms-tracker",
  "comments",
  "global-variables",
  "cookie-consent",
];

const DEFAULT_GADGETS: DashboardWidget[] = (() => {
  const leftIds = new Set([
    "welcome",
    "blogs",
    "workflow",
    "google-setup",
    "forms-tracker",
    "global-variables",
  ]);
  let leftOrder = 0;
  let rightOrder = 0;
  return DEFAULT_GADGET_IDS.map((id) => {
    const info = AVAILABLE_GADGETS.find((g) => g.id === id);
    const isLeft = leftIds.has(id);
    const order = isLeft ? leftOrder++ : rightOrder++;
    return {
      id,
      name: info?.name ?? id,
      component: info?.component ?? (() => null),
      props: id === "welcome" ? { userName: "User" } : {},
      position: {
        column: (isLeft ? "left" : "right") as "left" | "right",
        order,
      },
    } satisfies DashboardWidget;
  });
})();

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
        nameKey: gadget.nameKey,
        description: gadget.description,
        descriptionKey: gadget.descriptionKey,
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
        <p>{message(MSG.DASHBOARD_LOADING)}</p>
      </div>
    );
  }

  return (
    <div style={{ width: "100%" }} data-testid="dashboard-root" data-embedded={embedded ? "1" : "0"}>
      {/* Soft config warning — still show default gadgets */}
      {configError && userId ? (
        <div
          data-testid="dashboard-config-warning"
          role="alert"
          style={{
            padding: "10px 16px",
            color: "#664d03",
            backgroundColor: "#fff3cd",
            borderBottom: "1px solid #ffecb5",
            fontSize: "0.9rem",
          }}
        >
          <span style={{ fontWeight: 600 }}>
            {message(MSG.DASHBOARD_LAYOUT_WARNING_TITLE)}:{" "}
          </span>
          {message(MSG.DASHBOARD_LAYOUT_WARNING_PREFIX)} ({configError}).{" "}
          <a href={legacyDashboardUrl}>{message(MSG.DASHBOARD_LEGACY_LINK)}</a>
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
          {message(embedded ? MSG.DASHBOARD_EMBEDDED_TITLE : MSG.DASHBOARD_TITLE)}
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
          + {message(MSG.DASHBOARD_ADD_GADGET)}
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
