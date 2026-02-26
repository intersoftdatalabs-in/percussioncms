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

import React, { useEffect, useState } from "react";
import { DashboardLayout, type DashboardWidget } from "./DashboardLayout";
import { AddGadgetModal, type AddGadgetModalProps } from "./AddGadgetModal";
import { WelcomeWidget } from "./WelcomeWidget";
import { ActivityWidget } from "./ActivityWidget";
import { WorkflowStatusWidget } from "./WorkflowStatusWidget";
import { ProcessMonitorWidget } from "./ProcessMonitorWidget";
import { EffectivenessWidget } from "./EffectivenessWidget";
import { AssetsStatusWidget } from "./AssetsStatusWidget";
import { BulkUploadWidget } from "./BulkUploadWidget";
import { ReportsWidget } from "./ReportsWidget";
import { TrafficWidget } from "./TrafficWidget";

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
    name: "Workflow Status",
    component: WorkflowStatusWidget,
    description: "Page workflow status overview",
    category: "Content Management",
  },
  {
    id: "activity",
    name: "Activity",
    component: ActivityWidget,
    description: "Recent content activity timeline",
    category: "Content Management",
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
    name: "Effectiveness",
    component: EffectivenessWidget,
    description: "Performance and effectiveness metrics",
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
    description: "Bulk file upload job tracking",
    category: "Content Management",
  },
  {
    id: "reports",
    name: "Reports",
    component: ReportsWidget,
    description: "Available reports list",
    category: "Analytics",
  },
  {
    id: "traffic",
    name: "Traffic",
    component: TrafficWidget,
    description: "Content traffic analytics with charts",
    category: "Analytics",
  },
];

/**
 * Default gadget configuration for initial dashboard.
 *
 * <p>This is the "home" configuration. In the future, users can customize
 * this via dashboard configuration endpoints.</p>
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
    id: "workflow",
    name: "Workflow Status",
    component: WorkflowStatusWidget,
    props: {},
    position: { column: "left", order: 1 },
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
];

export interface DashboardProps {
  legacyDashboardUrl?: string;
}

/**
 * Main dashboard component.
 *
 * @param legacyDashboardUrl - Optional URL to legacy dashboard for fallback
 */
export const Dashboard: React.FC<DashboardProps> = ({
  legacyDashboardUrl = "/cm/app/dashboard.jsp?legacy=true",
}) => {
  const [gadgets, setGadgets] = useState<DashboardWidget[]>(DEFAULT_GADGETS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Get active gadget IDs for the modal
  const activeGadgetIds = new Set(gadgets.map((g) => g.id));

  // Get available gadgets that aren't already active
  const availableGadgetsForModal: Parameters<typeof AddGadgetModal>[0]["availableGadgets"] =
    AVAILABLE_GADGETS.map((gadget) => ({
      id: gadget.id,
      name: gadget.name,
      description: gadget.description,
      category: gadget.category,
    }));

  // Check for feature flag to use legacy dashboard
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("legacyDashboard") === "true") {
      console.log("Legacy dashboard flag detected, redirecting...");
      window.location.href = legacyDashboardUrl;
    }
  }, [legacyDashboardUrl]);

  // TODO: Load user's saved dashboard configuration from REST API
  // useEffect(() => {
  //   setLoading(true);
  //   try {
  //     // const config = await getDashboardConfig(userId);
  //     // setGadgets(config.gadgets);
  //   } catch (err) {
  //     setError(err instanceof Error ? err.message : String(err));
  //   } finally {
  //     setLoading(false);
  //   }
  // }, []);

  /**
   * Handle adding a new gadget to the dashboard.
   */
  const handleAddGadget = (gadgetId: string) => {
    // Find the gadget info
    const gadgetInfo = AVAILABLE_GADGETS.find((g) => g.id === gadgetId);
    if (!gadgetInfo) return;

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

    // Add gadget and close modal
    setGadgets([...gadgets, newGadget]);
    setIsModalOpen(false);

    // TODO: Persist to REST API via useDashboardConfig hook
  };

  /**
   * Handle removing a gadget from the dashboard.
   */
  const handleRemoveGadget = (gadgetId: string) => {
    setGadgets(gadgets.filter((g) => g.id !== gadgetId));
    // TODO: Persist to REST API via useDashboardConfig hook
  };

  if (error) {
    return (
      <div
        style={{
          padding: "20px",
          color: "#c33",
          backgroundColor: "#fee",
          borderRadius: "4px",
          margin: "20px",
        }}
      >
        <strong>Error loading dashboard:</strong> {error}
        <p>
          <a href={legacyDashboardUrl}>Fall back to legacy dashboard</a>
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          height: "100vh",
          color: "#999",
        }}
      >
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div style={{ width: "100%" }}>
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
        <h1 style={{ margin: 0, fontSize: "1.5em", color: "#333" }}>Dashboard</h1>
        <button
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
